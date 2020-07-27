package com.bil.bilmobileads;

import android.content.Context;
import android.widget.FrameLayout;

import com.bil.bilmobileads.interfaces.ResultCallback;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.consentmanager.sdk.storage.CMPStorageV1;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.BannerSize;
import com.bil.bilmobileads.entity.TimerRecall;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.bil.bilmobileads.interfaces.TimerCompleteListener;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.BannerAdUnit;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.Signals;
import org.prebid.mobile.TargetingParams;
import org.prebid.mobile.VideoAdUnit;
import org.prebid.mobile.VideoBaseAdUnit;
import org.prebid.mobile.addendum.AdViewUtils;
import org.prebid.mobile.addendum.PbFindSizeError;

import java.lang.annotation.Target;
import java.util.Arrays;

import androidx.annotation.NonNull;

public class ADBanner {

    // MARK: - View
    private FrameLayout adView;
    private AdDelegate adDelegate;

    // MARK: - AD
    private PublisherAdRequest amRequest;
    private AdUnit adUnit;
    private PublisherAdView amBanner;
    // MARK: - AD Info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: - Properties
    private ADFormat adFormatDefault;
    private AdSize curBannerSize;
    private int timeAutoRefresh = Constants.BANNER_AUTO_REFRESH_DEFAULT;
    private boolean isLoadBannerSucc = false; // true => banner đang chạy
    private boolean isRecallingPreload = false; // Check đang đợi gọi lại preload
    private TimerRecall timerRecall; // Recall load func AD

    public ADBanner(FrameLayout adView, String placement) {
        if (adView == null || placement == null) {
            PBMobileAds.getInstance().log("FrameLayout and placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("AD Banner Init: " + placement);

        this.adView = adView;
        this.placement = placement;

        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();

        this.timerRecall = new TimerRecall(Constants.RECALL_CONFIGID_SERVER, 1000, new TimerCompleteListener() {
            @Override
            public void doWork() {
                isRecallingPreload = false;
                load();
            }
        });

        // Get AdUnit
        if (this.adUnitObj == null) {
            this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
            if (this.adUnitObj == null) {
                PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                    @Override
                    public void success(AdUnitObj data) {
                        adUnitObj = data;

                        final Context contextApp = PBMobileAds.getInstance().getContextApp();
                        if (PBMobileAds.getInstance().gdprConfirm && CMPConsentTool.needShowCMP(contextApp)) {
                            String appName = contextApp.getApplicationInfo().loadLabel(contextApp.getPackageManager()).toString();

                            CMPConfig cmpConfig = CMPConfig.createInstance(15029, "consentmanager.mgr.consensu.org", appName, "EN");
                            CMPConsentTool.createInstance(contextApp, cmpConfig, new OnCloseCallback() {
                                @Override
                                public void onWebViewClosed() {
                                    PBMobileAds.getInstance().log("ConsentString: " + CMPStorageConsentManager.getConsentString(contextApp));
                                    load();
                                }
                            });
                        } else {
                            load();
                        }
                    }

                    @Override
                    public void failure(Exception error) {
                        PBMobileAds.getInstance().log(error.getLocalizedMessage());
                    }
                });
            }
        }
    }

    // MARK: - Preload + Load
    void deplayCallPreload() {
        this.isRecallingPreload = true;
        this.timerRecall.start();
    }

    AdInfor getAdInfor(boolean isVideo) {
        for (AdInfor infor : this.adUnitObj.adInfor) {
            if (infor.isVideo == isVideo) {
                return infor;
            }
        }
        return null;
    }

    public boolean load() {
        PBMobileAds.getInstance().log(" | isRunning: " + this.isLoaded() + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isLoaded() == true || this.isRecallingPreload == true) {
            return false;
        }
        PBMobileAds.getInstance().log("Load Banner AD: " + this.placement);

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("Ad is not active or not exist");
            return false;
        }

        // Check and set default
        this.adFormatDefault = this.adFormatDefault == null ? this.adUnitObj.defaultType : this.adFormatDefault;
        // set adformat theo loại duy nhất có
        if (adUnitObj.adInfor.size() < 2) {
            this.adFormatDefault = this.adUnitObj.adInfor.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
        }

        // Remove Ad
        this.destroy();

        // Set GDPR
        if (PBMobileAds.getInstance().gdprConfirm) {
            TargetingParams.setSubjectToGDPR(true);
            TargetingParams.setGDPRConsentString(CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp()));
        }

        AdInfor adInfor;
        if (this.adFormatDefault.equals(ADFormat.VAST)) {
            AdInfor info = this.getAdInfor(true);
            if (info == null) {
                PBMobileAds.getInstance().log("AdInfor is not exist");
                return false;
            }
            adInfor = info;

            PBMobileAds.getInstance().setupPBS(adInfor.host);
            PBMobileAds.getInstance().log("[Banner Video] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

            VideoBaseAdUnit.Parameters parameters = new VideoBaseAdUnit.Parameters();
            parameters.setMimes(Arrays.asList("video/mp4"));
            parameters.setProtocols(Arrays.asList(Signals.Protocols.VAST_2_0));
            parameters.setPlaybackMethod(Arrays.asList(Signals.PlaybackMethod.AutoPlaySoundOff));
            parameters.setPlacement(Signals.Placement.InBanner);

            VideoAdUnit vAdUnit = new VideoAdUnit(adInfor.configId, this.curBannerSize.getWidth(), this.curBannerSize.getHeight());
            vAdUnit.setParameters(parameters);
            this.adUnit = vAdUnit;
        } else {
            AdInfor info = this.getAdInfor(false);
            if (info == null) {
                PBMobileAds.getInstance().log("AdInfor is not exist");
                return false;
            }
            adInfor = info;

            PBMobileAds.getInstance().setupPBS(adInfor.host);
            PBMobileAds.getInstance().log("[Banner Simple] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new BannerAdUnit(adInfor.configId, this.curBannerSize.getWidth(), this.curBannerSize.getHeight());
        }
        this.adUnit.setAutoRefreshPeriodMillis(this.timeAutoRefresh);

        this.amBanner = new PublisherAdView(PBMobileAds.getInstance().getContextApp().getApplicationContext());
        this.amBanner.setAdUnitId(adInfor.adUnitID);
        this.amBanner.setAdSizes(this.curBannerSize);

        this.adView.removeAllViews();
        this.adView.addView(this.amBanner);

        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch placement '" + placement + "' for DFP: " + resultCode.name());
                handerResult(resultCode);
            }
        });

        this.amBanner.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                AdViewUtils.findPrebidCreativeSize(amBanner, new AdViewUtils.PbFindSizeListener() {
                    @Override
                    public void success(int width, int height) {
                        isLoadBannerSucc = true;
                        amBanner.setAdSizes(new AdSize(width, height));

                        if (adDelegate == null) return;
                        adDelegate.onAdLoaded("onAdLoaded: " + placement);
                    }

                    @Override
                    public void failure(@NonNull PbFindSizeError error) {
                        if (adDelegate == null) return;
                        adDelegate.onAdFailedToLoad(error.getDescription());
                    }
                });
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                if (adDelegate == null) return;
                adDelegate.onAdClosed("onAdClosed");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                if (adDelegate == null) return;

                switch (i) {
                    case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                        adDelegate.onAdFailedToLoad("ERROR_CODE_INTERNAL_ERROR");
                        break;
                    case AdRequest.ERROR_CODE_INVALID_REQUEST:
                        adDelegate.onAdFailedToLoad("ERROR_CODE_INVALID_REQUEST");
                        break;
                    case AdRequest.ERROR_CODE_NETWORK_ERROR:
                        adDelegate.onAdFailedToLoad("ERROR_CODE_NETWORK_ERROR");
                        break;
                    case AdRequest.ERROR_CODE_NO_FILL:
                        adDelegate.onAdFailedToLoad("ERROR_CODE_NO_FILL");
                        break;
                }
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adDelegate == null) return;
                adDelegate.onAdImpression("onAdImpression");
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                if (adDelegate == null) return;
                adDelegate.onAdLeftApplication("onAdLeftApplication");
            }
        });

        return true;
    }

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amBanner.loadAd(this.amRequest);
        } else {
            this.isLoadBannerSucc = false;
            this.stopAutoRefresh();

            if (resultCode == ResultCode.NO_BIDS) {
                this.adFormatDefault = this.adFormatDefault == ADFormat.HTML ? ADFormat.VAST : ADFormat.HTML;
                this.deplayCallPreload();
            } else if (resultCode == ResultCode.TIMEOUT) {
                this.deplayCallPreload();
            }
        }
    }

    public void destroy() {
        if (this.isLoaded()) {
            PBMobileAds.getInstance().log("Destroy Placement: " + this.placement);
            this.stopAutoRefresh();
            this.isLoadBannerSucc = false;
            this.adView.removeAllViews();
            this.amBanner.destroy();

            this.timerRecall.cancel();
            this.timerRecall = null;
        }
    }

    // MARK: - Public FUNC
    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

    public void setAdSize(BannerSize size) {
        this.curBannerSize = this.getBannerSize(size);
    }

    public AdSize getAdSize() {
        return this.curBannerSize;
    }

    public void setAutoRefreshMillis(int timeMillis) {
        this.timeAutoRefresh = timeMillis;
        if (this.adUnit != null) {
            this.adUnit.setAutoRefreshPeriodMillis(timeMillis);
        }
    }

    public void stopAutoRefresh() {
        // run stopAutoRefresh to stop .fetchDemand
        this.adUnit.stopAutoRefresh();
    }

    public boolean isLoaded() {
        return this.isLoadBannerSucc;
//        if (this.amBanner == null) return false;
//
//        return this.amBanner.isLoading();
    }

    void addFirstPartyData(AdUnit adUnit) {
        //        Access Control List
        //        Targeting.shared.addBidderToAccessControlList(Prebid.bidderNameAppNexus)

        //global user data
        TargetingParams.addUserData("globalUserDataKey1", "globalUserDataValue1");

        //global context data
        TargetingParams.addContextData("globalContextDataKey1", "globalContextDataValue1");

        //adunit context data
        adUnit.addContextData("adunitContextDataKey1", "adunitContextDataValue1");

        //global context keywords
        TargetingParams.addContextKeyword("globalContextKeywordValue1");
        TargetingParams.addContextKeyword("globalContextKeywordValue2");

        //global user keywords
        TargetingParams.addUserKeyword("globalUserKeywordValue1");
        TargetingParams.addUserKeyword("globalUserKeywordValue2");

        //adunit context keywords
        adUnit.addContextKeyword("adunitContextKeywordValue1");
        adUnit.addContextKeyword("adunitContextKeywordValue2");
    }

    // MARK: - Private FUNC
    AdSize getBannerSize(BannerSize typeBanner) {
        AdSize adSize = null;
        switch (typeBanner) {
            case Banner320x50:
                adSize = new AdSize(320, 50);
                break;
            case Banner320x100:
                adSize = new AdSize(320, 100);
                break;
            case Banner300x250:
                adSize = new AdSize(300, 250);
                break;
            case Banner468x60:
                adSize = new AdSize(468, 60);
                break;
            case Banner728x90:
                adSize = new AdSize(728, 90);
                break;
        }
        return adSize;
    }

}


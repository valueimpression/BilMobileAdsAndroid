package com.bil.bilmobileads;

import android.content.Context;

import com.bil.bilmobileads.interfaces.ResultCallback;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.consentmanager.sdk.storage.CMPStorageV1;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.TimerRecall;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.bil.bilmobileads.interfaces.TimerCompleteListener;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.InterstitialAdUnit;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.Signals;
import org.prebid.mobile.TargetingParams;
import org.prebid.mobile.VideoBaseAdUnit;
import org.prebid.mobile.VideoInterstitialAdUnit;

import java.util.Arrays;

public class ADInterstitial {

    // MARK: - View
    private AdDelegate adDelegate;

    // MARK: - AD
    private PublisherAdRequest amRequest;
    private AdUnit adUnit;
    private PublisherInterstitialAd amInterstitial;
    // MARK: - AD info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: Properties
    private ADFormat adFormatDefault;
    private boolean isLoadAfterPreload = false; // Check user gọi load() chưa có AD -> preload và show AD luôn
    private boolean isFetchingAD = false; // Check có phải đang lấy AD ko
    private boolean isRecallingPreload = false; // Check đang đợi gọi lại preload
    private TimerRecall timerRecall; // Recall load func AD

    public ADInterstitial(String placement) {
        if (placement == null) {
            PBMobileAds.getInstance().log("Placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("AD Interstitial Init: " + placement);

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
                preLoad();
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
                                    preLoad();
                                }
                            });
                        } else {
                            preLoad();
                        }
                    }

                    @Override
                    public void failure(Exception error) {
                        PBMobileAds.getInstance().log(error.getLocalizedMessage());
                    }
                });
            } else {
                this.preLoad();
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

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amInterstitial.loadAd(this.amRequest);
        } else {
            if (resultCode == ResultCode.NO_BIDS) {
                this.adFormatDefault = this.adFormatDefault.equals(ADFormat.HTML) ? ADFormat.VAST : ADFormat.HTML;
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            } else if (resultCode == ResultCode.TIMEOUT) {
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            }

            this.isFetchingAD = false;
        }
    }

    public boolean preLoad() {
        PBMobileAds.getInstance().log(" | isReady: " + this.isReady() + " |  isFetchingAD: " + this.isFetchingAD + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isReady() == true || this.isFetchingAD == true || this.isRecallingPreload == true) {
            return false;
        }
        PBMobileAds.getInstance().log("Preload Interstitial AD: " + this.placement);

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("Ad is not active or not exist");
            return false;
        }

        // Check and set default
        this.adFormatDefault = this.adFormatDefault == null ? this.adUnitObj.defaultType : this.adFormatDefault;
        // set adFormat theo loại duy nhất có
        if (adUnitObj.adInfor.size() < 2) {
            this.adFormatDefault = this.adUnitObj.adInfor.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
        }

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
            PBMobileAds.getInstance().log("[Full Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

            VideoBaseAdUnit.Parameters parameters = new VideoBaseAdUnit.Parameters();
            parameters.setMimes(Arrays.asList("video/mp4"));
            parameters.setProtocols(Arrays.asList(Signals.Protocols.VAST_2_0));
            parameters.setPlaybackMethod(Arrays.asList(Signals.PlaybackMethod.AutoPlaySoundOn));
            parameters.setPlacement(Signals.Placement.InBanner);

            VideoInterstitialAdUnit vAdUnit = new VideoInterstitialAdUnit(adInfor.configId);
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
            PBMobileAds.getInstance().log("[Full Simple] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new InterstitialAdUnit(adInfor.configId);
        }

        this.amInterstitial = new PublisherInterstitialAd(PBMobileAds.getInstance().getContextApp().getApplicationContext());
        this.amInterstitial.setAdUnitId(adInfor.adUnitID);

        this.isFetchingAD = true;
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch placement '" + placement + "' for DFP: " + resultCode.name());
                handerResult(resultCode);
            }
        });

        this.amInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                // Đã gọi lên ad server succ
                isFetchingAD = false;
                if (amInterstitial.isLoaded() == true) {
                    if (isLoadAfterPreload) {
                        amInterstitial.show();
                        isLoadAfterPreload = false;
                    }

                    if (adDelegate == null) return;
                    adDelegate.onAdLoaded("Ad Interstitial Ready with placement " + placement);
                } else {
                    isLoadAfterPreload = false;

                    if (adDelegate == null) return;
                    adDelegate.onAdLoaded("onAdLoaded Ad Interstitial Not Ready");
                }
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();

                isFetchingAD = false;
                if (adDelegate == null) return;
                adDelegate.onAdClosed("onAdClosed");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);

                isFetchingAD = false;
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

                isFetchingAD = false;
                if (adDelegate == null) return;
                adDelegate.onAdImpression("onAdImpression");
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();

                isFetchingAD = false;
                if (adDelegate == null) return;
                adDelegate.onAdLeftApplication("onAdLeftApplication");
            }
        });

        return true;
    }

    public void load() {
        if (isReady()) {
            this.amInterstitial.show();
        } else {
            PBMobileAds.getInstance().log("Don't have AD");
            this.isLoadAfterPreload = true;
            this.preLoad();
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy Placement: " + this.placement);
        this.isLoadAfterPreload = false;
        this.adUnit.stopAutoRefresh();

        this.timerRecall.cancel();
        this.timerRecall = null;
    }

    public boolean isReady() {
        if (this.amInterstitial == null) return false;

        PBMobileAds.getInstance().log("IsReady: " + this.amInterstitial.isLoaded());
        return this.amInterstitial.isLoaded();
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

}

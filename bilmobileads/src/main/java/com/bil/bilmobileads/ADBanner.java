package com.bil.bilmobileads;

import android.content.Context;
import android.widget.FrameLayout;

import com.bil.bilmobileads.interfaces.ResultCallback;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.google.android.gms.ads.AdListener;
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
import org.prebid.mobile.TargetingParams;
import org.prebid.mobile.VideoAdUnit;
import org.prebid.mobile.addendum.AdViewUtils;
import org.prebid.mobile.addendum.PbFindSizeError;

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
    private AdSize curBannerSize;
    private ADFormat adFormatDefault;
    private int timeAutoRefresh = Constants.BANNER_AUTO_REFRESH_DEFAULT;
    private boolean isLoadBannerSucc = false; // true => banner đang chạy
    private boolean isRecallingPreload = false; // Check đang đợi gọi lại preload
    private TimerRecall timerRecall; // Recall load func AD

    public ADBanner(FrameLayout adView, final String placementStr) {
        if (adView == null || placementStr == null) {
            PBMobileAds.getInstance().log("FrameLayout and placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("ADBanner Init: " + placementStr);

        this.adView = adView;
        this.placement = placementStr;

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
                        PBMobileAds.getInstance().log("Get Config ADBanner placement: " + placementStr + " Success");
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
                        PBMobileAds.getInstance().log("Get Config ADBanner placement: " + placementStr + " Fail with Error: " + error.getLocalizedMessage());
                    }
                });
            }
        }
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
            case SmartBanner:
                adSize = AdSize.SMART_BANNER;
        }
        return adSize;
    }

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

    void resetAD() {
        if (this.adUnit == null || this.amBanner == null) return;

        this.isRecallingPreload = false;
        this.isLoadBannerSucc = false;
        this.adView.removeAllViews();

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amBanner.destroy();
        this.amBanner.setAdListener(null);
        this.amBanner = null;
    }

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amBanner.loadAd(this.amRequest);
        } else {
            if (resultCode == ResultCode.NO_BIDS) {
                this.deplayCallPreload();
            } else if (resultCode == ResultCode.TIMEOUT) {
                this.deplayCallPreload();
            }
        }
    }

    // MARK: - Public FUNC
    public boolean load() {
        PBMobileAds.getInstance().log("ADBanner Placement '" + this.placement + "' - isLoaded: " + this.isLoaded() + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isLoaded() == true || this.isRecallingPreload == true) {
            return false;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("ADBanner Placement '" + this.placement + "' is not active or not exist");
            return false;
        }

        // Check and set default
        this.adFormatDefault = this.adUnitObj.defaultFormat;
        // set adformat theo loại duy nhất có
        if (adUnitObj.adInfor.size() < 2) {
            this.adFormatDefault = this.adUnitObj.adInfor.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
        }

        // Get AdInfor
        boolean isVideo = this.adFormatDefault == ADFormat.VAST;
        AdInfor adInfor = this.getAdInfor(isVideo);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor of ADBanner Placement '" + this.placement + "' is not exist");
            return false;
        }

        PBMobileAds.getInstance().log("Load ADBanner Placement: " + this.placement);
        // Set GDPR
        if (PBMobileAds.getInstance().gdprConfirm) {
            String consentStr = CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp());
            if (consentStr != null && consentStr != "") {
                TargetingParams.setSubjectToGDPR(true);
                TargetingParams.setGDPRConsentString(CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp()));
            }
        }

        // Set AD Size
        this.setAdSize(this.adUnitObj.bannerSize);
        int w = this.curBannerSize.getWidth();
        int h = this.curBannerSize.getHeight();
        if (this.curBannerSize.toString() == AdSize.SMART_BANNER.toString()) {
            w = 1;
            h = 1;
        }

        PBMobileAds.getInstance().setupPBS(adInfor.host);
        if (isVideo) {
            PBMobileAds.getInstance().log("[ADBanner Video] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new VideoAdUnit(adInfor.configId, w, h);
        } else {
            PBMobileAds.getInstance().log("[ADBanner HTML] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new BannerAdUnit(adInfor.configId, w, h);
        }
        this.adUnit.setAutoRefreshPeriodMillis(this.timeAutoRefresh);

        this.amBanner = new PublisherAdView(PBMobileAds.getInstance().getContextApp());
        this.amBanner.setAdUnitId(adInfor.adUnitID);
        this.amBanner.setAdSizes(this.curBannerSize);

        this.adView.removeAllViews();
        this.adView.addView(this.amBanner);

        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch ADBanner placement '" + placement + "' for DFP: " + resultCode.name());
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
                        amBanner.setAdSizes(curBannerSize);

                        isLoadBannerSucc = true;
                        PBMobileAds.getInstance().log("onAdLoaded: ADBanner Placement '" + placement + "'");
                        if (adDelegate != null) adDelegate.onAdLoaded();
                    }

                    @Override
                    public void failure(@NonNull PbFindSizeError error) {
                        isLoadBannerSucc = true;
                        PBMobileAds.getInstance().log("onAdLoaded: ADBanner Placement '" + placement + "' - " + error.getDescription());
                        if (adDelegate != null) adDelegate.onAdLoaded();
                    }
                });
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();

                PBMobileAds.getInstance().log("onAdOpened: ADBanner Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdOpened();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();

                PBMobileAds.getInstance().log("onAdClosed: ADBanner Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClosed();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log("onAdClicked: ADBanner Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClicked();
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();

                PBMobileAds.getInstance().log("onAdLeftApplication: ADBanner Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdLeftApplication();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);

                isLoadBannerSucc = false;
                String messErr = PBMobileAds.getInstance().getADError(errorCode);
                PBMobileAds.getInstance().log("onAdFailedToLoad: ADBanner Placement '" + placement + "' with error: " + messErr);
                if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
            }
        });

        return true;
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADBanner Placement: " + this.placement);

        this.resetAD();
        if (this.timerRecall != null) {
            this.timerRecall.cancel();
            this.timerRecall = null;
        }
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

    private void setAdSize(BannerSize size) {
        this.curBannerSize = this.getBannerSize(size);
    }

    public int getWidth() {
        return this.curBannerSize.getWidth();
    }

    public int getHeight() {
        return this.curBannerSize.getHeight();
    }

    public void setAutoRefreshMillis(int timeMillis) {
        this.timeAutoRefresh = timeMillis;
        if (this.adUnit != null) {
            this.adUnit.setAutoRefreshPeriodMillis(timeMillis);
        }
    }

    public boolean isLoaded() {
        return this.isLoadBannerSucc;
    }

}


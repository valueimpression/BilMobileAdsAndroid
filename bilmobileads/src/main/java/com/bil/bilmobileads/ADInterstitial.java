package com.bil.bilmobileads;

import android.content.Context;

import com.bil.bilmobileads.interfaces.ResultCallback;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
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

    // MARK: - Private FUNC
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
        if (this.adUnit == null || this.amInterstitial == null) return;

        this.isRecallingPreload = false;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amInterstitial.setAdListener(null);
        this.amInterstitial = null;
    }

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amInterstitial.loadAd(this.amRequest);
        } else {
            if (resultCode == ResultCode.NO_BIDS) {
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            } else if (resultCode == ResultCode.TIMEOUT) {
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            }

            this.isFetchingAD = false;
        }
    }

    // MARK: - Public FUNC
    public boolean preLoad() {
        PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' - isReady: " + this.isReady() + " |  isFetchingAD: " + this.isFetchingAD + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isReady() == true || this.isFetchingAD == true || this.isRecallingPreload == true) {
            return false;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' is not active or not exist");
            return false;
        }

        // Check and set default
        this.adFormatDefault = this.adUnitObj.defaultFormat;
        // set adFormat theo loại duy nhất có
        if (adUnitObj.adInfor.size() < 2) {
            this.adFormatDefault = this.adUnitObj.adInfor.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
        }

        // Check AdInfor
        boolean isAdVideo = this.adFormatDefault == ADFormat.VAST;
        AdInfor adInfor = this.getAdInfor(isAdVideo);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor of ADInterstitial Placement '" + this.placement + "' is not exist");
            return false;
        }

        PBMobileAds.getInstance().log("Preload ADInterstitial Placement: " + this.placement);
        // Set GDPR
        if (PBMobileAds.getInstance().gdprConfirm) {
            String consentStr = CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp());
            if (consentStr != null && consentStr != "") {
                TargetingParams.setSubjectToGDPR(true);
                TargetingParams.setGDPRConsentString(CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp()));
            }
        }

        PBMobileAds.getInstance().setupPBS(adInfor.host);
        if (isAdVideo) {
            PBMobileAds.getInstance().log("[ADInterstitial Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new VideoInterstitialAdUnit(adInfor.configId);
        } else {
            PBMobileAds.getInstance().log("[ADInterstitial HTML] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new InterstitialAdUnit(adInfor.configId);
        }
        this.amInterstitial = new PublisherInterstitialAd(PBMobileAds.getInstance().getContextApp());
        this.amInterstitial.setAdUnitId(adInfor.adUnitID);

        this.isFetchingAD = true;
        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch ADInterstitial placement '" + placement + "' for DFP: " + resultCode.name());
                handerResult(resultCode);
            }
        });

        this.amInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                if (isLoadAfterPreload) {
                    amInterstitial.show();
                    isLoadAfterPreload = false;
                }

                isFetchingAD = false;
                PBMobileAds.getInstance().log("onAdLoaded: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();

                isFetchingAD = false;
                PBMobileAds.getInstance().log("onAdOpened: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdOpened();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();

                isFetchingAD = false;
                PBMobileAds.getInstance().log("onAdClosed: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClosed();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();

                isFetchingAD = false;
                PBMobileAds.getInstance().log("onAdClicked: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClicked();
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();

                isFetchingAD = false;
                PBMobileAds.getInstance().log("onAdLeftApplication: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdLeftApplication();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);

                String messErr = PBMobileAds.getInstance().getADError(errorCode);
                if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
                    adFormatDefault = adFormatDefault.equals(ADFormat.VAST) ? ADFormat.HTML : ADFormat.VAST;
                    if (!isLoadAfterPreload) deplayCallPreload();
                }

                isFetchingAD = false;
                PBMobileAds.getInstance().log("onAdFailedToLoad: ADInterstitial Placement '" + placement + "' with error: " + messErr);
                if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
            }
        });

        return true;
    }

    public void load() {
        if (isReady()) {
            this.amInterstitial.show();
        } else {
            PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' Don't have AD");
            this.isLoadAfterPreload = true;
            this.preLoad();
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADInterstitial Placement: " + this.placement);

        this.resetAD();
        if (this.timerRecall != null) {
            this.timerRecall.cancel();
            this.timerRecall = null;
        }
    }

    public boolean isReady() {
        if (this.amInterstitial == null) return false;

        PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' IsReady: " + this.amInterstitial.isLoaded());
        return this.amInterstitial.isLoaded();
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

}

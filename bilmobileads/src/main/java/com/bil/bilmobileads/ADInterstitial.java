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

    private boolean isFetchingAD = false;
    private boolean setDefaultBidType = true;

    public ADInterstitial(final String placementStr) {
        if (placementStr == null) {
            PBMobileAds.getInstance().log("Placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("ADInterstitial Init: " + placementStr);

        this.placement = placementStr;

        // Get AdUnit
        this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
        if (this.adUnitObj == null) {
            PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                @Override
                public void success(AdUnitObj data) {
                    PBMobileAds.getInstance().log("Get Config ADInterstitial placement: " + placementStr + " Success");
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
                    PBMobileAds.getInstance().log("Get Config ADInterstitial placement: " + placementStr + " Fail with Error: " + error.getLocalizedMessage());
                }
            });
        } else {
            this.preLoad();
        }
    }

    boolean processNoBids() {
        if (this.adUnitObj.adInfor.size() >= 2 && this.adFormatDefault == this.adUnitObj.defaultFormat) {
            this.setDefaultBidType = false;
            this.preLoad();

            return true;
        } else {
            // Both or .video, .html is no bids -> wait and preload.
            PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' No Bids.");
            return false;
        }
    }

    void resetAD() {
        if (this.adUnit == null || this.amInterstitial == null) return;

        this.isFetchingAD = false;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amInterstitial.setAdListener(null);
        this.amInterstitial = null;
    }

    void handlerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amInterstitial.loadAd(this.amRequest);
        } else {
            this.isFetchingAD = false;

            if (resultCode == ResultCode.NO_BIDS) {
                this.processNoBids();
            } else if (resultCode == ResultCode.TIMEOUT) {
                PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' Timeout. Please check your internet connect.");
            }
        }
    }

    // MARK: - Public FUNC
    public boolean preLoad() {
        PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' - isReady: " + this.isReady() + " | isFetchingAD: " + this.isFetchingAD);
        if (this.adUnitObj == null || this.isReady() || this.isFetchingAD) return false;
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' is not active or not exist.");
            return false;
        }

        // Check and Set Default
        this.adFormatDefault = this.adUnitObj.defaultFormat;
        if (!this.setDefaultBidType && this.adUnitObj.adInfor.size() >= 2) {
            this.adFormatDefault = this.adFormatDefault == ADFormat.VAST ? ADFormat.HTML : ADFormat.VAST;
            this.setDefaultBidType = true;
        }

        // Set GDPR
        PBMobileAds.getInstance().setGDPR();

        // Check AdInfor
        boolean isVideo = this.adFormatDefault == ADFormat.VAST;
        AdInfor adInfor = PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor of ADInterstitial Placement '" + this.placement + "' is not exist.");
            return false;
        }

        PBMobileAds.getInstance().log("Preload ADInterstitial Placement: " + this.placement);
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        if (isVideo) {
            PBMobileAds.getInstance().log("[ADInterstitial Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new VideoInterstitialAdUnit(adInfor.configId);
        } else {
            PBMobileAds.getInstance().log("[ADInterstitial HTML] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
            this.adUnit = new InterstitialAdUnit(adInfor.configId);
        }
        this.amInterstitial = new PublisherInterstitialAd(PBMobileAds.getInstance().getContextApp());
        this.amInterstitial.setAdUnitId(adInfor.adUnitID);
        this.amInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                isFetchingAD = false;
                PBMobileAds.getInstance().log("onAdLoaded: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();

                PBMobileAds.getInstance().log("onAdOpened: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdOpened();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();

                PBMobileAds.getInstance().log("onAdClosed: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClosed();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log("onAdClicked: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClicked();
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();

                PBMobileAds.getInstance().log("onAdLeftApplication: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdLeftApplication();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);

                if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
                    if (!processNoBids()) {
                        isFetchingAD = false;
                        if (adDelegate != null)
                            adDelegate.onAdFailedToLoad("onAdFailedToLoad: ADInterstitial Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode));
                    }
                } else {
                    isFetchingAD = false;
                    String messErr = "onAdFailedToLoad: ADInterstitial Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode);
                    PBMobileAds.getInstance().log(messErr);
                    if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
                }
            }
        });

        // Create Request PBS
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
                handlerResult(resultCode);
            }
        });

        return true;
    }

    public void show() {
        if (isReady()) {
            this.amInterstitial.show();
        } else {
            PBMobileAds.getInstance().log("ADInterstitial Placement '" + this.placement + "' currently unavailable, call preload() first.");
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADInterstitial Placement: " + this.placement);
        this.resetAD();
    }

    public boolean isReady() {
        if (this.adUnit == null || this.amInterstitial == null) return false;
        return this.amInterstitial.isLoaded();
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

}

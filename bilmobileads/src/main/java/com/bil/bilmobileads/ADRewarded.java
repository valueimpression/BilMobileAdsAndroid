package com.bil.bilmobileads;

import android.app.Activity;
import android.content.Context;

import com.bil.bilmobileads.entity.ADRewardItem;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.TimerRecall;
import com.bil.bilmobileads.interfaces.AdRewardedDelegate;
import com.bil.bilmobileads.interfaces.TimerCompleteListener;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.RewardedVideoAdUnit;
import org.prebid.mobile.TargetingParams;

import androidx.annotation.NonNull;

public class ADRewarded {

    // MARK: - View
    private Activity activityAd;
    private AdRewardedDelegate adDelegate;

    // MARK: - AD
    private PublisherAdRequest amRequest;
    private AdUnit adUnit;
    private RewardedAd amRewarded;
    // MARK: - AD info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: Properties
    private boolean isFetchingAD = false;

    public ADRewarded(Activity activity, final String placementStr) {
        if (activity == null || placementStr == null) {
            PBMobileAds.getInstance().log("Activity and Placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("AD Interstitial Init: " + placementStr);

        this.activityAd = activity;
        this.placement = placementStr;

        // Get AdUnit
        this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
        if (this.adUnitObj == null) {
            PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                @Override
                public void success(AdUnitObj data) {
                    PBMobileAds.getInstance().log("Get Config ADRewarded placement: " + placementStr + " Success");
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
                    PBMobileAds.getInstance().log("Get Config ADRewarded placement: " + placementStr + " Fail with Error: " + error.getLocalizedMessage());
                }
            });
        } else {
            this.preLoad();
        }
    }

    void resetAD() {
        if (this.adUnit == null || this.amRewarded == null) return;

        this.isFetchingAD = false;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amRewarded = null;
    }

    void handlerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amRewarded.loadAd(this.amRequest, new RewardedAdLoadCallback() {
                @Override
                public void onRewardedAdLoaded() {
                    isFetchingAD = false;

                    PBMobileAds.getInstance().log("onRewardedAdLoaded: ADRewarded Placement '" + placement + "' Loaded Success");
                    if (adDelegate != null) adDelegate.onRewardedAdLoaded();
                }

                @Override
                public void onRewardedAdFailedToLoad(int errorCode) {
                    isFetchingAD = false;

                    String messErr = "onRewardedAdFailedToLoad: ADRewarded Placement '" + placement + "' failed: " + PBMobileAds.getInstance().getAdRewardedError(errorCode);
                    PBMobileAds.getInstance().log(messErr);
                    if (adDelegate != null) adDelegate.onRewardedAdFailedToLoad(messErr);
                }
            });
        } else {
            this.isFetchingAD = false;

            if (resultCode == ResultCode.NO_BIDS) {
                PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' No Bids.");
            } else if (resultCode == ResultCode.TIMEOUT) {
                PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' Timeout. Please check your internet connect.");
            }
        }
    }

    // MARK: - Public FUNC
    public boolean preLoad() {
        PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' - isReady: " + this.isReady() + " | isFetchingAD: " + this.isFetchingAD);
        if (this.adUnitObj == null || this.isReady() || this.isFetchingAD) return false;
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' is not active or not exist.");
            return false;
        }

        // Set GDPR
        PBMobileAds.getInstance().setGDPR();

        // Check AdInfor
        boolean isVideo = this.adUnitObj.defaultFormat == ADFormat.VAST;
        AdInfor adInfor = PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor of ADRewarded Placement '" + this.placement + "' is not exist.");
            return false;
        }

        PBMobileAds.getInstance().log("Preload ADRewarded Placement: " + this.placement);
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log("[ADRewarded Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
        this.adUnit = new RewardedVideoAdUnit(adInfor.configId);
        this.amRewarded = new RewardedAd(PBMobileAds.getInstance().getContextApp(), adInfor.adUnitID);

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
                PBMobileAds.getInstance().log("Prebid demand fetch ADRewarded placement '" + placement + "' for DFP: " + resultCode.name());
                handlerResult(resultCode);
            }
        });

        return true;
    }

    public void show() {
        if (this.isReady()) {
            this.amRewarded.show(this.activityAd, new RewardedAdCallback() {
                @Override
                public void onRewardedAdOpened() {
                    PBMobileAds.getInstance().log("onRewardedAdOpened: ADRewarded Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onRewardedAdOpened();
                }

                @Override
                public void onRewardedAdClosed() {
                    PBMobileAds.getInstance().log("onRewardedAdClosed: ADRewarded Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onRewardedAdClosed();
                }

                @Override
                public void onUserEarnedReward(@NonNull RewardItem reward) {
                    String dataReward = "Type: " + reward.getType() + " | Amount: " + reward.getAmount();
                    PBMobileAds.getInstance().log("onUserEarnedReward: ADRewarded Placement '" + placement + "' with RewardItem - " + dataReward);

                    ADRewardItem rewardItem = new ADRewardItem(reward.getType(), reward.getAmount());
                    if (adDelegate != null) adDelegate.onUserEarnedReward(rewardItem);
                }

                @Override
                public void onRewardedAdFailedToShow(int errorCode) {
                    super.onRewardedAdFailedToShow(errorCode);

                    String messErr = "onRewardedAdFailedToShow: ADRewarded Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getAdRewardedError(errorCode);
                    PBMobileAds.getInstance().log(messErr);
                    if (adDelegate != null) adDelegate.onRewardedAdFailedToShow(messErr);
                }
            });
        } else {
            PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' currently unavailable, call preload() first.");
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADRewarded Placement: " + this.placement);
        this.resetAD();
    }

    public boolean isReady() {
        if (this.adUnit == null || this.amRewarded == null) return false;
        return this.amRewarded.isLoaded();
    }

    public void setListener(AdRewardedDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }
}

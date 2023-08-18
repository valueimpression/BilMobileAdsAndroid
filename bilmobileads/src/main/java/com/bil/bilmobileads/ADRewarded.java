package com.bil.bilmobileads;

import android.app.Activity;

import com.bil.bilmobileads.entity.ADRewardItem;
import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.interfaces.AdRewardedDelegate;

import androidx.annotation.NonNull;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.RewardedVideoAdUnit;
import org.prebid.mobile.api.data.AdUnitFormat;
import org.prebid.mobile.api.exceptions.AdException;
import org.prebid.mobile.api.rendering.RewardedAdUnit;
import org.prebid.mobile.api.rendering.listeners.InterstitialAdUnitListener;
import org.prebid.mobile.api.rendering.listeners.RewardedAdUnitListener;

import java.util.EnumSet;

public class ADRewarded {

    // MARK: - View
    private Activity activityAd;
    private AdRewardedDelegate adDelegate;

    // MARK: - AD
    private AdManagerAdRequest amRequest;
    private AdUnit adUnit;
    private RewardedAd amRewarded;
    private org.prebid.mobile.api.rendering.RewardedAdUnit rewardedAdUnit;

    // MARK: - AD info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: Properties
    private boolean isFetchingAD = false;
    private boolean isHaveAds = false;

    public ADRewarded(Activity activity, final String placementStr) {
        if (activity == null || placementStr == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "Activity or Placement is null");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded placement: " + placementStr + " Init");

        this.activityAd = activity;
        this.placement = placementStr;

        this.getConfigAD();
    }

    // MARK: - Handle AD
    void getConfigAD() {
        this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
        if (this.adUnitObj == null) {
            this.isFetchingAD = true;

            // Get AdUnit Info
            PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                @Override
                public void success(AdUnitObj data) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded placement: " + placement + " Init Success");
                    isFetchingAD = false;
                    adUnitObj = data;

                    PBMobileAds.getInstance().showCMP(new WorkCompleteDelegate() {
                        @Override
                        public void doWork() {
                            preLoad();
                        }
                    });
                }

                @Override
                public void failure(Exception error) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded placement: " + placement + " Init Failed with Error: " + error.getLocalizedMessage() + ". Please check your internet connect.");
                    isFetchingAD = false;
                }
            });
        } else {
            this.preLoad();
        }
    }

    void resetAD() {
        this.isHaveAds = false;
        this.isFetchingAD = false;
        this.amRequest = null;

        if (this.adUnit != null) {
            this.adUnit.stopAutoRefresh();
            this.adUnit = null;
        }

        if (this.amRewarded != null) {
            this.amRewarded.setFullScreenContentCallback(null);
            this.amRewarded = null;
        }

        if (this.rewardedAdUnit != null) {
            this.rewardedAdUnit.setRewardedAdUnitListener(null);
            this.rewardedAdUnit = null;
        }
    }

    // MARK: - Public FUNC
    public void preLoad() {
        PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded Placement '" + this.placement + "' - isReady: " + this.isReady() + " | isFetchingAD: " + this.isFetchingAD);
        if (this.adUnitObj == null || this.isReady() || this.isFetchingAD) {
            if (this.adUnitObj == null && !this.isFetchingAD) {
                PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded placement: " + this.placement + " is not ready to load.");
                this.getConfigAD();
                return;
            }
            return;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() == 0) {
            PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded Placement '" + this.placement + "' is not active or not exist.");
            return;
        }

        // Check AdInfor
        final AdInfor adInfor = this.adUnitObj.adInfor.get(0); // PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log(LogType.INFOR, "AdInfor of ADRewarded Placement '" + this.placement + "' is not exist.");
            return;
        }

        PBMobileAds.getInstance().log(LogType.INFOR, "Preload ADRewarded Placement: " + this.placement);
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log(LogType.INFOR, "[ADRewarded Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

        if (adInfor.adUnitID != null) {
            this.adUnit = new RewardedVideoAdUnit(adInfor.configId);

            // Create Request PBS
            this.isFetchingAD = true;
            this.amRequest = new AdManagerAdRequest.Builder().build();
            this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
                @Override
                public void onComplete(ResultCode resultCode) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "PBS demand fetch ADRewarded placement '" + placement + "' for DFP: " + resultCode.name());
                    RewardedAd.load(
                            activityAd,
                            adInfor.adUnitID,
                            amRequest,
                            new RewardedAdLoadCallback() {
                                @Override
                                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                                    super.onAdLoaded(rewardedAd);

                                    isHaveAds = true;
                                    isFetchingAD = false;
                                    amRewarded = rewardedAd;
                                    setAdsListener();

                                    PBMobileAds.getInstance().log(LogType.INFOR, "onRewardedAdLoaded: ADRewarded Placement '" + placement + "' Loaded Success");
                                    if (adDelegate != null) adDelegate.onRewardedAdLoaded();
                                }

                                @Override
                                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                    super.onAdFailedToLoad(loadAdError);

                                    isHaveAds = false;
                                    isFetchingAD = false;
                                    amRewarded = null;
                                    String messErr = "onRewardedAdFailedToLoad: ADRewarded Placement '" + placement + "' failed: " + loadAdError.getMessage(); // + PBMobileAds.getInstance().getAdRewardedError(errorCode);
                                    PBMobileAds.getInstance().log(LogType.INFOR, messErr);
                                    if (adDelegate != null)
                                        adDelegate.onRewardedAdFailedToLoad(messErr);
                                }
                            }
                    );
                }
            });
        } else {
            this.rewardedAdUnit = new org.prebid.mobile.api.rendering.RewardedAdUnit(this.activityAd, adInfor.configId);
            this.rewardedAdUnit.setRewardedAdUnitListener(new RewardedAdUnitListener() {
                @Override
                public void onAdLoaded(RewardedAdUnit rewardedAdUnit) {
                    isHaveAds = true;
                    isFetchingAD = false;
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdLoaded: ADRewarded Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onRewardedAdLoaded();
                }

                @Override
                public void onAdDisplayed(RewardedAdUnit rewardedAdUnit) {
                    isHaveAds = false;
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdDisplayed: ADRewarded Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onRewardedAdOpened();
                }

                @Override
                public void onAdFailed(RewardedAdUnit rewardedAdUnit, AdException exception) {
                    isHaveAds = false;
                    isFetchingAD = false;
                    String messErr = "onAdFailed: ADRewarded Placement '" + placement + "' with error: " + exception.getMessage();
                    PBMobileAds.getInstance().log(LogType.INFOR, messErr);
                    if (adDelegate != null) adDelegate.onRewardedAdFailedToLoad(messErr);
                }

                @Override
                public void onAdClicked(RewardedAdUnit rewardedAdUnit) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADRewarded Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onRewardedAdClicked();
                }

                @Override
                public void onAdClosed(RewardedAdUnit rewardedAdUnit) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdClosed: ADRewarded Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onRewardedAdClosed();
                }

                @Override
                public void onUserEarnedReward(RewardedAdUnit rewardedAdUnit) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "onUserEarnedReward: ADRewarded Placement '" + placement + "'");

//                    ADRewardItem rewardItem = new ADRewardItem("Reward", 1);
                    if (adDelegate != null) adDelegate.onUserEarnedReward();
                }
            });
            this.rewardedAdUnit.loadAd();
        }
    }

    void setAdsListener() {
        if (amRewarded == null) return;

        amRewarded.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADRewarded Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onRewardedAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdImpression: ADRewarded Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onRewardedAdOpened();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                amRewarded = null;

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdDismissedFullScreenContent: ADRewarded Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onRewardedAdClosed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                amRewarded = null;

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdFailedToShowFullScreenContent: ADRewarded Placement '" + placement + "' | " + adError.getMessage());
                if (adDelegate != null) adDelegate.onRewardedAdFailedToLoad(adError.getMessage());
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                PBMobileAds.getInstance().log(LogType.INFOR, "onAdShowedFullScreenContent: ADRewarded Placement '" + placement + "'");
            }
        });
    }

    public void show() {
        if (this.isReady()) {
            if(this.amRewarded != null) {
                this.amRewarded.show(this.activityAd, reward -> {
                    String dataReward = "Type: " + reward.getType() + " | Amount: " + reward.getAmount();
                    PBMobileAds.getInstance().log(LogType.INFOR, "onUserEarnedReward: ADRewarded Placement '" + placement + "' with RewardItem - " + dataReward);

//                    ADRewardItem rewardItem = new ADRewardItem(reward.getType(), reward.getAmount());
                    if (adDelegate != null) adDelegate.onUserEarnedReward();
                });
            }
            if(this.rewardedAdUnit != null) {
                this.rewardedAdUnit.show();
            }
        } else {
            PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded Placement '" + this.placement + "' currently unavailable, call preLoad() first.");
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log(LogType.INFOR, "Destroy ADRewarded Placement: " + this.placement);
        this.resetAD();
    }

    public boolean isReady() {
        if (this.amRewarded != null && this.isHaveAds) return true; // Render ads by GAM
        if (this.isHaveAds) return true; // Render ads by Prebid Rendering
        return false;
    }

    public void setListener(AdRewardedDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }
}

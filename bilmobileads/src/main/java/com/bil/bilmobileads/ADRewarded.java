package com.bil.bilmobileads;

import android.app.Activity;
import android.util.Log;

import com.bil.bilmobileads.entity.ADRewardItem;
import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
//import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
//import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.interfaces.AdRewardedDelegate;

import androidx.annotation.NonNull;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.RewardedVideoAdUnit;
import org.prebid.mobile.api.rendering.RewardedAdUnit;

public class ADRewarded {

    // MARK: - View
    private Activity activityAd;
    private AdRewardedDelegate adDelegate;

    // MARK: - AD
//    private PublisherAdRequest amRequest;
    private AdManagerAdRequest amRequest;
    private AdUnit adUnit;
    private RewardedAd amRewarded;
    // MARK: - AD info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: Properties
    private boolean isFetchingAD = false;

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
        if (this.adUnit == null || this.amRewarded == null) return;

        this.isFetchingAD = false;
        this.amRequest = null;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amRewarded = null;
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
//        boolean isVideo = this.adUnitObj.defaultFormat == ADFormat.VAST;
        final AdInfor adInfor = this.adUnitObj.adInfor.get(0); // PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log(LogType.INFOR, "AdInfor of ADRewarded Placement '" + this.placement + "' is not exist.");
            return;
        }

        PBMobileAds.getInstance().log(LogType.INFOR, "Preload ADRewarded Placement: " + this.placement);
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log(LogType.INFOR, "[ADRewarded Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
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

                                isFetchingAD = false;
                                amRewarded = rewardedAd;
                                setAdsListener();

                                PBMobileAds.getInstance().log(LogType.INFOR, "onRewardedAdLoaded: ADRewarded Placement '" + placement + "' Loaded Success");
                                if (adDelegate != null) adDelegate.onRewardedAdLoaded();
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                super.onAdFailedToLoad(loadAdError);

                                isFetchingAD = false;
                                amRewarded = null;
                                String messErr = "onRewardedAdFailedToLoad: ADRewarded Placement '" + placement + "' failed: " + loadAdError.getMessage(); // + PBMobileAds.getInstance().getAdRewardedError(errorCode);
                                PBMobileAds.getInstance().log(LogType.INFOR, messErr);
                                if (adDelegate != null)
                                    adDelegate.onRewardedAdFailedToLoad(messErr);
                            }
                        }
                );
//                if (resultCode == ResultCode.SUCCESS) {
//                } else {
//                    isFetchingAD = false;
//                    if (resultCode == ResultCode.NO_BIDS) {
//                        PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded Placement '" + placement + "' No Bids.");
//                    } else if (resultCode == ResultCode.TIMEOUT) {
//                        PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded Placement '" + placement + "' Timeout. Please check your internet connect.");
//                    }
//                }
            }
        });
    }

    void setAdsListener() {
        if (amRewarded == null) return;

        amRewarded.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADRewarded Placement '" + placement + "'");
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
            this.amRewarded.show(this.activityAd, reward -> {
                String dataReward = "Type: " + reward.getType() + " | Amount: " + reward.getAmount();
                PBMobileAds.getInstance().log(LogType.INFOR, "onUserEarnedReward: ADRewarded Placement '" + placement + "' with RewardItem - " + dataReward);

                ADRewardItem rewardItem = new ADRewardItem(reward.getType(), reward.getAmount());
                if (adDelegate != null) adDelegate.onUserEarnedReward(rewardItem);
            });
        } else {
            PBMobileAds.getInstance().log(LogType.INFOR, "ADRewarded Placement '" + this.placement + "' currently unavailable, call preLoad() first.");
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log(LogType.INFOR, "Destroy ADRewarded Placement: " + this.placement);
        this.resetAD();
    }

    public boolean isReady() {
        if (this.adUnit == null || this.amRewarded == null) return false;
        return true;
    }

    public void setListener(AdRewardedDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }
}

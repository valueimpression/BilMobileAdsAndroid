package com.bil.bilmobileads.interfaces;

import com.google.android.gms.ads.rewarded.RewardItem;

public class AdRewardedDelegate {

    public void onRewardedAdLoaded(String data){};

    public void onRewardedAdOpened(String data){};

    public void onRewardedAdClosed(String data){};

    public void onUserEarnedReward(RewardItem data){};

    public void onRewardedAdFailedToLoad(String err){};

    public void onRewardedAdFailedToShow(String err){};
}

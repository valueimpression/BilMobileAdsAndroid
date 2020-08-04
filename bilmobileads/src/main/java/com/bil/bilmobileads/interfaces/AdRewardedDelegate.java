package com.bil.bilmobileads.interfaces;

public interface AdRewardedDelegate {

    public void onRewardedAdLoaded(String data);

    public void onRewardedAdOpened(String data);

    public void onRewardedAdClosed(String data);

    public void onUserEarnedReward(String data);

    public void onRewardedAdFailedToLoad(String err);

    public void onRewardedAdFailedToShow(String err);
}

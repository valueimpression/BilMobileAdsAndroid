package com.bil.bilmobileads.interfaces;

public interface AdRewardedDelegate {
    public void onRewardedAdOpened(String data);

    public void onRewardedAdClosed(String data);

    public void onUserEarnedReward(String data);

    public void onRewardedAdFailedToShow(String err);
}

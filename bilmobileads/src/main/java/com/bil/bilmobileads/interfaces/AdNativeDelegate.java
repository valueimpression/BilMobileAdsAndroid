package com.bil.bilmobileads.interfaces;

import com.bil.bilmobileads.ADNativeViewBuilder;

public class AdNativeDelegate {

    /*
     * Called when an ad native custom view loaded. (ADNativeCustom Only)
     * */
    public void onNativeViewLoaded(ADNativeViewBuilder.Builder builder) {
    }

    /*
     * Called when an ad native request loaded an ad.
     * */
    public void onAdLoaded() {
    }

    public void onAdClicked() {
    }

    /*
     * Called when an ad request loaded fail.
     * */
    public void onAdFailedToLoad(String error) {
    }
}

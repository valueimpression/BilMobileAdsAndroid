package com.bil.bilmobileads.interfaces;

import com.bil.bilmobileads.ADNativeView;
import com.bil.bilmobileads.entity.AdData;

public class NativeAdLoaderCustomDelegate {

    public void onAdClicked() {
    }

    public void onAdImpression() {
    }

    public void onAdClosed() {
    }

    /*
     * Called when an ad native custom view loaded.
     * */
    public void onNativeViewLoaded(ADNativeView.Builder builder) {
    }

    /*
     * Called when a request native ad is loaded fail.
     * */
    public void onNativeFailedToLoad(String error) {
    }

    public void onNativePaidEvent(AdData data){

    }
}

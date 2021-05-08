package com.bil.bilmobileads.interfaces;

import com.bil.bilmobileads.ADNativeView;

public class NativeAdLoaderCustomDelegate {

    /*
     * Called when an ad native custom view loaded.
     * */
    public void onNativeViewLoaded(ADNativeView.Builder builder) { }

    /*
     * Called when a request native ad is loaded fail.
     * */
    public void onNativeFailedToLoad(String error) { }
}

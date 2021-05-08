package com.bil.bilmobileads.interfaces;

public class NativeAdCustomDelegate {
    /*
     * Called when an ad native did record Impression.
     * */
    public void onNativeAdDidRecordImpression(String data){}

    /*
     * Called just before the application will background or terminate because the user clicked on an
     * ad that will launch another application (such as the App Store).
     * The normal UIApplicationDelegate methods, like applicationDidEnterBackground:, will be called immediately before this.
     */
    public void onNativeAdDidRecordClick(String data){}

    /*
     * Called when an ad native did Expire.
     * */
    public void onNativeAdDidExpire(String data){}
}

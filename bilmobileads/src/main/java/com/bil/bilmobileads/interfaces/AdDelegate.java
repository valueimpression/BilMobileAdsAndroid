package com.bil.bilmobileads.interfaces;

public class AdDelegate {

    /*
    * Called when an ad request loaded an ad.
    * */
    public void onAdLoaded(String data){};

    /*
     * Called when AD Opened.
     * */
    public void onAdOpened(String data){};

    /*
     * Called when back to application after click ad.
     * */
    public void onAdClosed(String data){};

    /*
     * Called just before presenting the user a full screen view (ADInterstitial only), such as a browser, in response to clicking on an ad.
     * */
    public void onAdClicked(String data){};

    /*
    * Called just before the application will background or terminate because the user clicked on an ad
    *               that will launch another application (such as the App Store).
    * */
    public void onAdLeftApplication(String data){};

    /*
     * Called when an ad request loaded fail.
     * */
    public void onAdFailedToLoad(String err){};

}

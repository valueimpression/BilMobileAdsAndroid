package com.bil.bilmobileads;

import android.app.Activity;
import android.content.Context;


import androidx.annotation.NonNull;

import com.bil.bilmobileads.entity.AdData;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.Date;

public class ADAppOpen {

    private AdDelegate adDelegate;

    private AdUnitObj adUnitObj;
    //    private static final String AD_UNIT_ID = "/6499/example/app-open";
    private String placement = null;
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private boolean isFetchingAD = false;

    private long loadTime = 0;

    private String logSuffix = "[ADAppOpen] ";

    public ADAppOpen(Context context, String placementStr) {
        if (placementStr == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, logSuffix + "placement is null");
            throw new NullPointerException();
        }

        MobileAds.initialize(context, initializationStatus -> {
        });

        PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "placement: " + placementStr + " Init");
        this.placement = placementStr;
        this.getConfigAD(context);
    }

    void getConfigAD(Context context) {
        this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
        if (this.adUnitObj == null) {
            isFetchingAD = true;

            // Get AdUnit Info
            PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                @Override
                public void success(AdUnitObj data) {
                    PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "placement: " + placement + " Init Success");
                    isFetchingAD = false;
                    adUnitObj = data;
                    PBMobileAds.getInstance().showCMP(() -> preLoad(context));
                }

                @Override
                public void failure(Exception error) {
                    PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "placement: " + placement + " Init Failed with Error: " + error.getLocalizedMessage() + ". Please check your internet connect.");
                    isFetchingAD = false;
                }
            });
        } else {
            this.preLoad(context);
        }
    }

    public void preLoad(Context context) {
        // Do not load ad if there is an unused ad or one is already loading.
        if (isFetchingAD || isLoadingAd || isReady()) {
            return;
        }
        this.resetAD();

        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() == 0) {
            PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "placement '" + this.placement + "' is not active or not exist.");
            return;
        }

        final AdInfor adInfor = this.adUnitObj.adInfor.get(0); // PBMobileAds.getInstance().getAdInfor(true, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "AdInfor of placement '" + this.placement + "' is not exist.");
            return;
        }

        PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "placement '" + this.placement + "' call preload ads.");

        isLoadingAd = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(context, adInfor.adUnitID, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(AppOpenAd ad) {
                // Called when an app open ad has loaded.
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = (new Date()).getTime();
                setAdsListener();

                PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "onAdLoaded: placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                // Called when an app open ad has failed to load.
                String messErr = logSuffix + "onAdFailedToLoad: placement '" + placement + "' with error: " + loadAdError.getMessage();
                PBMobileAds.getInstance().log(LogType.INFOR, messErr);
                if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
                isLoadingAd = false;
            }
        });
    }

    void setAdsListener() {
        if (appOpenAd == null) return;

        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "onAdClicked: placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();

                PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "onAdImpression: placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdOpened();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                // Set the reference to null so isReady() returns false.
                PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "onAdDismissedFullScreenContent: placement '" + placement + "'");
                appOpenAd = null;
                isShowingAd = false;
                if (adDelegate != null) adDelegate.onAdClosed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when fullscreen content failed to show.
                // Set the reference to null so isReady() returns false.
                String errMess = "placement '" + placement + "' with error: " + adError.getMessage();
                PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "onAdFailedToShowFullScreenContent: " + errMess);
                appOpenAd = null;
                isShowingAd = false;
                if (adDelegate != null) adDelegate.onAdFailedToLoad(errMess);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "onAdShowedFullScreenContent: placement '" + placement + "'");
            }
        });
        appOpenAd.setOnPaidEventListener(adValue -> {
            PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "onPaidEvent: Placement '" + placement + "'");
            AdData adData = new AdData(adValue.getCurrencyCode(), adValue.getPrecisionType(), adValue.getValueMicros());
            if (adDelegate != null) adDelegate.onPaidEvent(adData);
        });
    }

    public void show(@NonNull final Activity activity) {
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "placement '" + placement + "' is already showing");
            return;
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isReady()) {
            PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "placement '" + placement + "' currently unavailable, call preLoad() first.");
            return;
        }

        isShowingAd = true;
        appOpenAd.show(activity);
    }

    /**
     * Check if ad exists and can be shown.
     */
    public boolean isReady() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

    /**
     * Utility method to check if ad was loaded more than n hours ago.
     */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public void destroy() {
        PBMobileAds.getInstance().log(LogType.INFOR, logSuffix + "Destroy placement: " + this.placement);
        this.resetAD();
    }

    void resetAD() {
        this.isLoadingAd = false;
        this.isShowingAd = false;
        this.isFetchingAD = false;

        if (this.appOpenAd != null) {
            this.appOpenAd.setFullScreenContentCallback(null);
            this.appOpenAd.setOnPaidEventListener(null);
            this.appOpenAd = null;
        }
    }
}

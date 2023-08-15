package com.bil.bilmobileads;

import static com.google.android.gms.common.util.CollectionUtils.listOf;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.ViewGroup;

import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
//import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
//import com.google.android.gms.ads.doubleclick.PublisherAdView;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.BannerSize;
import com.bil.bilmobileads.interfaces.AdDelegate;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.BannerAdUnit;
import org.prebid.mobile.BannerParameters;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.Signals;
import org.prebid.mobile.VideoAdUnit;
import org.prebid.mobile.VideoParameters;
import org.prebid.mobile.addendum.AdViewUtils;
import org.prebid.mobile.addendum.PbFindSizeError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ADBanner implements Application.ActivityLifecycleCallbacks {

    // MARK: - View
    private ViewGroup adView;
    private AdDelegate adDelegate;

    // MARK: - AD
    //    private PublisherAdRequest amRequest;
    private AdManagerAdRequest amRequest;
    //    private AdUnit adUnit;
    private BannerAdUnit adUnit;
    //    private PublisherAdView amBanner;
    private AdManagerAdView amBanner;
    // MARK: - AD Info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: - Properties
    private AdSize curBannerSize;
    private ADFormat adFormatDefault;

    private boolean isLoadBannerSucc = false;
    private boolean setDefaultBidType = true;
    private boolean isFetchingAD = false;

    public ADBanner(ViewGroup adView, final String placementStr) {
        if (adView == null || placementStr == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "AdView Placeholder or placement is null");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner placement: " + placementStr + " Init");

        this.adView = adView;
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
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner placement: " + placement + " Init Success");
                    isFetchingAD = false;
                    adUnitObj = data;

                    PBMobileAds.getInstance().showCMP(new WorkCompleteDelegate() {
                        @Override
                        public void doWork() {
                            // Setup Application Delegate
                            ((Activity) PBMobileAds.getInstance().getContextApp()).getApplication().registerActivityLifecycleCallbacks(ADBanner.this);

                            load();
                        }
                    });
                }

                @Override
                public void failure(Exception error) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner placement: " + placement + " Init Failed with Error: " + error.getLocalizedMessage() + ". Please check your internet connect.");
                    isFetchingAD = false;
                    destroy();
                }
            });
        } else {
            this.load();
        }
    }

    boolean processNoBids() {
        if (this.adUnitObj.adInfor.size() >= 2 && this.adFormatDefault == this.adUnitObj.defaultFormat) {
            this.setDefaultBidType = false;
            this.load();

            return true;
        } else {
            // Both or .video, .html is no bids -> wait and preload.
            PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner Placement '" + this.placement + "' No Bids.");
            return false;
        }
    }

    void resetAD() {
        if (this.adUnit == null || this.amBanner == null) return;

        this.isFetchingAD = false;
        this.isLoadBannerSucc = false;
        this.amRequest = null;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amBanner.destroy();
        this.amBanner.setAdListener(null);
        this.amBanner = null;
    }

    // MARK: - Public FUNC
    public void load() {
        PBMobileAds.getInstance().log(LogType.DEBUG, "ADBanner Placement '" + this.placement + "' - isLoaded: " + this.isLoaded() + " | isFetchingAD: " + this.isFetchingAD);
        if (this.adView == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "ADBanner placement: " + this.placement + ", AdView Placeholder is null.");
            return;
        }

        if (this.adUnitObj == null || this.isLoaded() || this.isFetchingAD) {
            if (this.adUnitObj == null && !this.isFetchingAD) {
                PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner placement: " + this.placement + " is not ready to load.");
                this.getConfigAD();
                return;
            }
            return;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() == 0) {
            PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner Placement '" + this.placement + "' is not active or not exist.");
            return;
        }

//        // Check and Set Default
//        this.adFormatDefault = this.adUnitObj.defaultFormat;
//        if (!this.setDefaultBidType && this.adUnitObj.adInfor.size() >= 2) {
//            this.adFormatDefault = this.adFormatDefault == ADFormat.VAST ? ADFormat.HTML : ADFormat.VAST;
//            this.setDefaultBidType = true;
//        }

        // Get AdInfor
//        boolean isVideo = this.adFormatDefault == ADFormat.VAST;
        AdInfor adInfor = this.adUnitObj.adInfor.get(0);  // PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log(LogType.INFOR, "AdInfor of ADBanner Placement '" + this.placement + "' is not exist.");
            return;
        }

        // Set AD Size
        this.setAdSize(this.adUnitObj.bannerSize);
        int w = this.curBannerSize.getWidth();
        int h = this.curBannerSize.getHeight();
        if (this.curBannerSize.toString() == AdSize.SMART_BANNER.toString()) {
            w = 1;
            h = 1;
        }

        PBMobileAds.getInstance().log(LogType.INFOR, "Load ADBanner Placement: " + this.placement);
        PBMobileAds.getInstance().setupPBS(adInfor.host);

        PBMobileAds.getInstance().log(LogType.DEBUG, "[ADBanner] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
        this.adUnit = new BannerAdUnit(adInfor.configId, w, h);

        BannerParameters parameters = new BannerParameters();
        parameters.setApi(listOf(Signals.Api.MRAID_3, Signals.Api.OMID_1));
        this.adUnit.setBannerParameters(parameters);
        VideoParameters videoParameters = new VideoParameters(listOf("video/x-flv", "video/mp4"));
//        videoParameters.setPlacement(Signals.Placement.InBanner);
//        videoParameters.setApi(listOf(Signals.Api.VPAID_1, Signals.Api.VPAID_2));
//        videoParameters.setProtocols(listOf(Signals.Protocols.VAST_2_0));
//        videoParameters.setPlaybackMethod(listOf(Signals.PlaybackMethod.AutoPlaySoundOn));
        this.adUnit.setVideoParameters(videoParameters);

        // Set auto refresh time | refreshTime default: sec
        this.startFetchData();

//        this.amBanner = new PublisherAdView(PBMobileAds.getInstance().getContextApp());
        this.amBanner = new AdManagerAdView(PBMobileAds.getInstance().getContextApp());
        this.amBanner.setAdUnitId(adInfor.adUnitID);
        this.amBanner.setAdSizes(this.curBannerSize);
        this.amBanner.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                isFetchingAD = false;
                AdViewUtils.findPrebidCreativeSize(amBanner, new AdViewUtils.PbFindSizeListener() {
                    @Override
                    public void success(int width, int height) {
                        isLoadBannerSucc = true;
                        amBanner.setAdSizes(curBannerSize);
                        PBMobileAds.getInstance().log(LogType.INFOR, "onAdLoaded: ADBanner Placement '" + placement + "'");
                        if (adDelegate != null) adDelegate.onAdLoaded();
                    }

                    @Override
                    public void failure(@NonNull PbFindSizeError error) {
                        isLoadBannerSucc = false;
                        PBMobileAds.getInstance().log(LogType.INFOR, "onAdLoaded: ADBanner Placement '" + placement + "' with error: " + error.getDescription());
                        if (adDelegate != null) adDelegate.onAdLoaded();
                    }
                });
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdOpened: ADBanner Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdOpened();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdClosed: ADBanner Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClosed();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADBanner Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                isLoadBannerSucc = false;
                isFetchingAD = false;
                int errorCode = loadAdError.getCode();
                String messErr = "onAdFailedToLoad: ADBanner Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode);
                PBMobileAds.getInstance().log(LogType.INFOR, messErr);
                if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);

//                if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
//                    if (!processNoBids()) {
//                        isFetchingAD = false;
//                        if (adDelegate != null)
//                            adDelegate.onAdFailedToLoad("onAdFailedToLoad: ADBanner Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode));
//                    }
//                } else {
//                isFetchingAD = false;
//                String messErr = "onAdFailedToLoad: ADBanner Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode);
//                PBMobileAds.getInstance().log(LogType.INFOR, messErr);
//                if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
//                }
            }
        });

        // Add AD to view
        this.adView.removeAllViews();
        this.adView.addView(this.amBanner);

        // Create Request PBS
        this.isFetchingAD = true;
        final AdManagerAdRequest.Builder builder = new AdManagerAdRequest.Builder();
        this.amRequest = builder.build();
        this.adUnit.fetchDemand(this.amRequest, resultCode -> {
            PBMobileAds.getInstance().log(LogType.DEBUG, "PBS demand fetch ADBanner placement '" + placement + "' for DFP: " + resultCode.name());

            amBanner.loadAd(amRequest);

//            if (resultCode == ResultCode.SUCCESS) {
//                amBanner.loadAd(amRequest);
//            } else {
//                isFetchingAD = false;
//                isLoadBannerSucc = false;
//
//                if (resultCode == ResultCode.NO_BIDS) {
//                    processNoBids();
//                } else if (resultCode == ResultCode.TIMEOUT) {
//                    PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner Placement '" + placement + "' Timeout. Please check your internet connect.");
//                } else {
//                    PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner Placement '" + placement + "' No Ads. Something went wrong");
//                }
//            }
        });
    }

    public void destroy() {
        ((Activity) PBMobileAds.getInstance().getContextApp()).getApplication().unregisterActivityLifecycleCallbacks(this);

        if (this.adUnit == null) return;
        PBMobileAds.getInstance().log(LogType.INFOR, "Destroy ADBanner Placement: " + this.placement);
        this.resetAD();
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

    public int getWidth() {
        if (this.adUnit == null) return 0;
        return this.curBannerSize.getWidth();
    }

    public int getHeight() {
        if (this.adUnit == null) return 0;
        return this.curBannerSize.getHeight();
    }

    public int getWidthInPixels() {
        if (this.adUnit == null) return 0;
        return this.curBannerSize.getWidthInPixels(PBMobileAds.getInstance().getContextApp());
    }

    public int getHeightInPixels() {
        if (this.adUnit == null) return 0;
        return this.curBannerSize.getHeightInPixels(PBMobileAds.getInstance().getContextApp());
    }

    public boolean isLoaded() {
        return this.isLoadBannerSucc;
    }

    public void startFetchData() {
        if (this.adUnit == null) return;
        if (this.adUnitObj.refreshTime > 0) {
//            int time = this.adUnitObj.refreshTime <= 120 ? this.adUnitObj.refreshTime : 120;
            this.adUnit.setAutoRefreshInterval(Math.max(this.adUnitObj.refreshTime, 120));
//            this.adUnit.setAutoRefreshPeriodMillis(this.adUnitObj.refreshTime); // convert sec to milisec
//            this.adUnit.setAutoRefreshInterval(this.adUnitObj.refreshTime); // convert sec to milisec
        }
    }

    public void stopFetchData() {
        if (this.adUnit == null) return;
//        this.adUnit.setAutoRefreshPeriodMillis(600000 * 1000);
//        this.adUnit.setAutoRefreshInterval(600000 * 1000);
        this.adUnit.setAutoRefreshInterval(600);
    }

    // MARK: - Private FUNC
    void setAdSize(BannerSize size) {
        this.curBannerSize = this.getBannerSize(size);
    }

    AdSize getBannerSize(BannerSize typeBanner) {
        AdSize adSize = null;
        switch (typeBanner) {
            case Banner320x50:
                adSize = new AdSize(320, 50);
                break;
            case Banner320x100:
                adSize = new AdSize(320, 100);
                break;
            case Banner300x250:
                adSize = new AdSize(300, 250);
                break;
            case Banner468x60:
                adSize = new AdSize(468, 60);
                break;
            case Banner728x90:
                adSize = new AdSize(728, 90);
                break;
            case SmartBanner:
                adSize = AdSize.SMART_BANNER;
        }
        return adSize;
    }

    //  MARK: - Application Delegate
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        if (activityName.equalsIgnoreCase("CMPConsentToolActivity")
                || activityName.equalsIgnoreCase("AdActivity"))
            return;

        this.startFetchData();
        if (this.adUnit != null)
            PBMobileAds.getInstance().log(LogType.DEBUG, "onForeground: ADBanner Placement '" + this.placement + "'");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        if (activityName.equalsIgnoreCase("CMPConsentToolActivity")
                || activityName.equalsIgnoreCase("AdActivity"))
            return;

        this.stopFetchData();
        if (this.adUnit != null)
            PBMobileAds.getInstance().log(LogType.DEBUG, "onBackground: ADBanner Placement '" + this.placement + "'");
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}


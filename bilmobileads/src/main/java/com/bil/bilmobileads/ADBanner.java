package com.bil.bilmobileads;

import static com.google.android.gms.common.util.CollectionUtils.listOf;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;

import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.BannerSize;
import com.bil.bilmobileads.interfaces.AdDelegate;

import org.prebid.mobile.BannerAdUnit;
import org.prebid.mobile.BannerParameters;
import org.prebid.mobile.Signals;
import org.prebid.mobile.VideoParameters;
import org.prebid.mobile.addendum.AdViewUtils;
import org.prebid.mobile.addendum.PbFindSizeError;
import org.prebid.mobile.api.data.BannerAdPosition;
import org.prebid.mobile.api.data.VideoPlacementType;
import org.prebid.mobile.api.exceptions.AdException;
import org.prebid.mobile.api.rendering.BannerView;
import org.prebid.mobile.api.rendering.listeners.BannerViewListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ADBanner implements Application.ActivityLifecycleCallbacks {

    // MARK: - View
    private Context context;
    private ViewGroup adView;
    private AdDelegate adDelegate;

    // MARK: - AD
    private AdManagerAdRequest amRequest;
    private BannerAdUnit adUnit;
    private AdManagerAdView amBanner;
    private BannerView bannerView;

    // MARK: - AD Info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: - Properties
    private AdSize curBannerSize;
    private org.prebid.mobile.AdSize curPrebidBannerSize;

    private boolean isLoadBannerSucc = false;
    private boolean isFetchingAD = false;

    public ADBanner(Context context, ViewGroup adView, final String placementStr) {
        if (context == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "Context is null");
            throw new NullPointerException();
        }
        if (adView == null || placementStr == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "AdView Placeholder is null");
            throw new NullPointerException();
        }
        if (placementStr == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "Placement is null");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log(LogType.INFOR, "ADBanner placement: " + placementStr + " Init");

        this.context = context;
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

    void resetAD() {
        this.isFetchingAD = false;
        this.isLoadBannerSucc = false;
        this.amRequest = null;

        if (this.adUnit != null) {
            this.adUnit.stopAutoRefresh();
            this.adUnit = null;
        }

        if (this.amBanner != null) {
            this.amBanner.destroy();
            this.amBanner.setAdListener(null);
            this.amBanner = null;
        }

        if (this.bannerView != null) {
            this.bannerView.setBannerListener(null);
            this.bannerView.stopRefresh();
            this.bannerView.destroy();
            this.bannerView = null;
        }
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

        // Get AdInfor
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

        adInfor.adUnitID = null;
        if (adInfor.adUnitID != null) {
            this.adUnit = new BannerAdUnit(adInfor.configId, w, h);
            this.startFetchData();

            BannerParameters parameters = new BannerParameters();
            parameters.setApi(listOf(Signals.Api.MRAID_3, Signals.Api.OMID_1));
            this.adUnit.setBannerParameters(parameters);
            VideoParameters videoParameters = new VideoParameters(listOf("video/x-flv", "video/mp4"));
            this.adUnit.setVideoParameters(videoParameters);

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
                            String mess = "onAdLoaded: ADBanner Placement '" + placement + "' with error: " + error.getDescription();
                            PBMobileAds.getInstance().log(LogType.INFOR, mess);
                            if (adDelegate != null) adDelegate.onAdFailedToLoad(mess);
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
            });
        } else {
            this.bannerView = new BannerView(this.context, adInfor.configId, this.curPrebidBannerSize);
            this.startFetchData();
            this.bannerView.setBannerListener(new BannerViewListener() {
                @Override
                public void onAdLoaded(BannerView bannerView) {
                    isFetchingAD = false;
                    isLoadBannerSucc = true;
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdLoaded: ADBanner Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onAdLoaded();
                }

                @Override
                public void onAdDisplayed(BannerView bannerView) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdDisplayed: ADBanner Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onAdOpened();
                }

                @Override
                public void onAdFailed(BannerView bannerView, AdException exception) {
                    isLoadBannerSucc = false;
                    String mess = "onAdFailed: ADBanner Placement '" + placement + "' with error: " + exception.getLocalizedMessage();
                    PBMobileAds.getInstance().log(LogType.INFOR, mess);
                    if (adDelegate != null) adDelegate.onAdFailedToLoad(mess);
                }

                @Override
                public void onAdClicked(BannerView bannerView) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADBanner Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onAdClicked();
                }

                @Override
                public void onAdClosed(BannerView bannerView) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "onAdClosed: ADBanner Placement '" + placement + "'");
                    if (adDelegate != null) adDelegate.onAdClosed();

                    isLoadBannerSucc = false;
                    load();
                }
            });

            // Create layout parameters for the FrameLayout
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, // Width
                    FrameLayout.LayoutParams.WRAP_CONTENT  // Height
            );
            layoutParams.gravity = Gravity.CENTER;
            this.bannerView.setLayoutParams(layoutParams);

            this.adView.removeAllViews();
            this.adView.addView(this.bannerView);

            this.isFetchingAD = true;
            this.bannerView.loadAd();
        }
    }

    public void destroy() {
        ((Activity) PBMobileAds.getInstance().getContextApp()).getApplication().unregisterActivityLifecycleCallbacks(this);

        PBMobileAds.getInstance().log(LogType.INFOR, "Destroy ADBanner Placement: " + this.placement);
        this.adView.removeAllViews();
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
        if (this.adUnitObj.refreshTime > 0) {
            int time = Math.min(this.adUnitObj.refreshTime, 120);
            if (this.adUnit != null) this.adUnit.setAutoRefreshInterval(time);
            if (this.bannerView != null) this.bannerView.setAutoRefreshDelay(time);
        }
    }

    public void stopFetchData() {
        if (this.adUnit != null) this.adUnit.stopAutoRefresh();
        if (this.bannerView != null) this.bannerView.stopRefresh();
    }

    // MARK: - Private FUNC
    void setAdSize(BannerSize size) {
        this.curBannerSize = this.getBannerSize(size);
        this.curPrebidBannerSize = this.getPrebidBannerSize(size);
    }

    org.prebid.mobile.AdSize getPrebidBannerSize(BannerSize typeBanner) {
        org.prebid.mobile.AdSize adSize = null;
        switch (typeBanner) {
            case Banner320x50:
                adSize = new org.prebid.mobile.AdSize(320, 50);
                break;
            case Banner320x100:
                adSize = new org.prebid.mobile.AdSize(320, 100);
                break;
            case Banner300x250:
                adSize = new org.prebid.mobile.AdSize(300, 250);
                break;
            case Banner468x60:
                adSize = new org.prebid.mobile.AdSize(468, 60);
                break;
            case Banner728x90:
                adSize = new org.prebid.mobile.AdSize(728, 90);
                break;
            case SmartBanner:
                adSize = new org.prebid.mobile.AdSize(1, 1);
        }
        return adSize;
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


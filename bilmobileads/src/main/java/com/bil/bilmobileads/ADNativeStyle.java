package com.bil.bilmobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.TimerRecall;
import com.bil.bilmobileads.interfaces.AdNativeDelegate;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.TimerCompleteListener;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import org.prebid.mobile.NativeAdUnit;
import org.prebid.mobile.NativeDataAsset;
import org.prebid.mobile.NativeEventTracker;
import org.prebid.mobile.NativeImageAsset;
import org.prebid.mobile.NativeTitleAsset;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.TargetingParams;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ADNativeStyle implements Application.ActivityLifecycleCallbacks {

    // MARK: - View
    private FrameLayout adViewFrame;
    private AdNativeDelegate adDelegate;

    // MARK: - AD
    private PublisherAdRequest amRequest;
    private NativeAdUnit adUnit;
    private PublisherAdView amNative;
    // MARK: - AD Info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: - Properties
    private ADFormat adFormatDefault;

    private boolean isLoadNativeSucc = false;
    private boolean setDefaultBidType = true;
    private boolean isFetchingAD = false;

    public ADNativeStyle(FrameLayout adViewFrame, final String placementStr) {
        if (adViewFrame == null || placementStr == null) {
            PBMobileAds.getInstance().log("FrameLayout and placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("ADNativeStyle Init: " + placement);

        this.adViewFrame = adViewFrame;
        this.placement = placementStr;

        // Setup Application Delegate
        ((Activity) PBMobileAds.getInstance().getContextApp()).getApplication().registerActivityLifecycleCallbacks(this);

        // Get AdUnit
        this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
        if (this.adUnitObj == null) {
            PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                @Override
                public void success(AdUnitObj data) {
                    PBMobileAds.getInstance().log("Get Config ADNativeStyle placement: " + placementStr + " Success");
                    adUnitObj = data;

                    final Context contextApp = PBMobileAds.getInstance().getContextApp();
                    if (PBMobileAds.getInstance().gdprConfirm && CMPConsentTool.needShowCMP(contextApp)) {
                        String appName = contextApp.getApplicationInfo().loadLabel(contextApp.getPackageManager()).toString();

                        CMPConfig cmpConfig = CMPConfig.createInstance(15029, "consentmanager.mgr.consensu.org", appName, "EN");
                        CMPConsentTool.createInstance(contextApp, cmpConfig, new OnCloseCallback() {
                            @Override
                            public void onWebViewClosed() {
                                PBMobileAds.getInstance().log("ConsentString: " + CMPStorageConsentManager.getConsentString(contextApp));
                                load();
                            }
                        });
                    } else {
                        load();
                    }
                }

                @Override
                public void failure(Exception error) {
                    PBMobileAds.getInstance().log("Get Config ADNativeStyle placement: " + placementStr + " Fail with Error: " + error.getLocalizedMessage());
                }
            });
        } else {
            this.load();
        }
    }

    // MARK: - Load AD
    void resetAD() {
        if (this.adUnit == null || this.amNative == null) return;

        this.isFetchingAD = false;
        this.isLoadNativeSucc = false;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amNative.destroy();
        this.amNative.setAdListener(null);
        this.amNative = null;
    }

    void handlerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amNative.loadAd(this.amRequest);
        } else {
            if (resultCode == ResultCode.NO_BIDS) {
                this.processNoBids();
            } else if (resultCode == ResultCode.TIMEOUT) {
                PBMobileAds.getInstance().log("ADNativeStyle Placement '" + this.placement + "' Timeout. Please check your internet connect.");
            }
        }
    }

    public boolean load() {
        PBMobileAds.getInstance().log("ADNativeStyle Placement '" + this.placement + "' - isLoaded: " + this.isLoaded() + " | isFetchingAD: " + this.isFetchingAD);
        if (this.adUnitObj == null || this.isLoaded() || this.isFetchingAD) {
            return false;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("AdNativeStyle Placement '" + this.placement + "' is not active or not exist.");
            return false;
        }

        // Check and Set Default
        this.adFormatDefault = this.adUnitObj.defaultFormat;
        if (!this.setDefaultBidType && this.adUnitObj.adInfor.size() >= 2) {
            this.adFormatDefault = this.adFormatDefault == ADFormat.VAST ? ADFormat.HTML : ADFormat.VAST;
            this.setDefaultBidType = true;
        }

        // Set GDPR
        PBMobileAds.getInstance().setGDPR();

        // Get AdInfor
        boolean isVideo = this.adFormatDefault == ADFormat.VAST;
        AdInfor adInfor = PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor of ADNativeStyle Placement '" + this.placement + "' is not exist.");
            return false;
        }

        PBMobileAds.getInstance().log("Load ADNativeStyle Placement: " + this.placement);
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log("[ADNativeStyle] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
        this.adUnit = new NativeAdUnit(adInfor.configId);
        this.adUnit.setContextType(NativeAdUnit.CONTEXT_TYPE.SOCIAL_CENTRIC);
        this.adUnit.setPlacementType(NativeAdUnit.PLACEMENTTYPE.CONTENT_FEED);
        this.adUnit.setContextSubType(NativeAdUnit.CONTEXTSUBTYPE.GENERAL_SOCIAL);

        // Set auto refresh time | refreshTime is -> sec
        this.setAutoRefreshMillis();

        setupNativeAsset();

        // Create AdView
        this.amNative = new PublisherAdView(PBMobileAds.getInstance().getContextApp());
        this.amNative.setAdUnitId(adInfor.adUnitID);
        this.amNative.setAdSizes(AdSize.FLUID);
        this.amNative.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                isFetchingAD = false;
                isLoadNativeSucc = true;
                PBMobileAds.getInstance().log("onAdLoaded: ADNativeStyle Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdLoaded();
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();

                PBMobileAds.getInstance().log("onAdClicked: ADNativeStyle Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);
                isLoadNativeSucc = false;

                if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
                    if (!processNoBids()) {
                        isFetchingAD = false;
                        if (adDelegate != null)
                            adDelegate.onAdFailedToLoad("onAdFailedToLoad: ADNativeStyle Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode));
                    }
                } else {
                    isFetchingAD = false;
                    String messErr = "onAdFailedToLoad: ADNativeStyle Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode);
                    PBMobileAds.getInstance().log(messErr);
                    if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
                }
            }
        });

        // Add AD to view
        this.adViewFrame.removeAllViews();
        this.adViewFrame.addView(this.amNative);

        // Create Request PBS
        this.isFetchingAD = true;
        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch ADNativeStyle placement '" + placement + "' for DFP: " + resultCode.name());
                handlerResult(resultCode);
            }
        });

        return true;
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADNativeStyle Placement: " + this.placement);
        this.resetAD();
        ((Activity) PBMobileAds.getInstance().getContextApp()).getApplication().unregisterActivityLifecycleCallbacks(this);
    }

    // MARK: - Public FUNC
    public void setListener(AdNativeDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

    public int getWidth() {
        if (this.adUnit == null) return 0;
        return this.amNative.getWidth();
    }

    public int getHeight() {
        if (this.adUnit == null) return 0;
        return this.amNative.getHeight();
    }

    public boolean isLoaded() {
        return this.isLoadNativeSucc;
    }

    // MARK: - Private FUNC
    void setupNativeAsset() {
        // Add event trackers requirements, this is required
        ArrayList<NativeEventTracker.EVENT_TRACKING_METHOD> methods = new ArrayList<>();
        methods.add(NativeEventTracker.EVENT_TRACKING_METHOD.IMAGE);
        methods.add(NativeEventTracker.EVENT_TRACKING_METHOD.JS);
        try {
            NativeEventTracker tracker = new NativeEventTracker(NativeEventTracker.EVENT_TYPE.IMPRESSION, methods);
            this.adUnit.addEventTracker(tracker);
        } catch (Exception e) {
            e.printStackTrace();
            PBMobileAds.getInstance().log(e.getLocalizedMessage());
        }
        // Require a title asset
        NativeTitleAsset title = new NativeTitleAsset();
        title.setLength(90);
        title.setRequired(true);
        this.adUnit.addAsset(title);
        // Require an icon asset
        NativeImageAsset icon = new NativeImageAsset();
        icon.setImageType(NativeImageAsset.IMAGE_TYPE.ICON);
        icon.setWMin(20);
        icon.setHMin(20);
        icon.setRequired(true);
        this.adUnit.addAsset(icon);
        // Require an main image asset
        NativeImageAsset image = new NativeImageAsset();
        image.setImageType(NativeImageAsset.IMAGE_TYPE.MAIN);
        image.setHMin(200);
        image.setWMin(200);
        image.setRequired(true);
        this.adUnit.addAsset(image);
        // Require sponsored text
        NativeDataAsset data = new NativeDataAsset();
        data.setLen(90);
        data.setDataType(NativeDataAsset.DATA_TYPE.SPONSORED);
        data.setRequired(true);
        this.adUnit.addAsset(data);
        // Require main description
        NativeDataAsset body = new NativeDataAsset();
        body.setRequired(true);
        body.setDataType(NativeDataAsset.DATA_TYPE.DESC);
        this.adUnit.addAsset(body);
        // Require call to action
        NativeDataAsset cta = new NativeDataAsset();
        cta.setRequired(true);
        cta.setDataType(NativeDataAsset.DATA_TYPE.CTATEXT);
        this.adUnit.addAsset(cta);
    }

    void setAutoRefreshMillis() {
        if (this.adUnitObj.refreshTime > 0) {
            this.adUnit.setAutoRefreshPeriodMillis(this.adUnitObj.refreshTime * 1000); // convert sec to milisec
        }
    }

    boolean processNoBids() {
        if (this.adUnitObj.adInfor.size() >= 2 && this.adFormatDefault == this.adUnitObj.defaultFormat) {
            this.setDefaultBidType = false;
            this.load();

            return true;
        } else {
            // Both or .video, .html is no bids -> wait and preload.
            PBMobileAds.getInstance().log("ADNativeStyle Placement '" + this.placement + "' No Bids.");
            return false;
        }
    }

    // MARK: - Application Delegate
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (this.adUnit == null) return;

        this.setAutoRefreshMillis();
        PBMobileAds.getInstance().log("onForeground: ADNativeStyle Placement '" + this.placement + "'");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (this.adUnit == null) return;

        this.adUnit.setAutoRefreshPeriodMillis(60000 * 1000);
        PBMobileAds.getInstance().log("onBackground: ADNativeStyle Placement '" + this.placement + "'");
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


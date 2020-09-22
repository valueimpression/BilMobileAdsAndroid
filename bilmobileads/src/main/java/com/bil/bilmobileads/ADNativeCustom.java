package com.bil.bilmobileads;

import android.content.Context;
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
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;

import org.prebid.mobile.NativeAdUnit;
import org.prebid.mobile.NativeDataAsset;
import org.prebid.mobile.NativeEventTracker;
import org.prebid.mobile.NativeImageAsset;
import org.prebid.mobile.NativeTitleAsset;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.TargetingParams;

import java.util.ArrayList;

public class ADNativeCustom {

    // MARK: - View
    private FrameLayout adViewFrame;
    private AdNativeDelegate adNativeDelegate;

    // MARK: - AD
    private PublisherAdRequest amRequest;
    private NativeAdUnit adUnit;
    private AdLoader amNativeDFP;
    private UnifiedNativeAd unifiedNativeAdObj;
    private ADNativeView.Builder builder;

    // MARK: - AD Info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: - Properties
    private ADFormat adFormatDefault;
    private int timeAutoRefresh = Constants.BANNER_AUTO_REFRESH_DEFAULT;
    private boolean isLoadNativeSucc = false; // true => Native đang chạy
    private boolean isRecallingPreload = false; // Check đang đợi gọi lại preload
    private TimerRecall timerRecall; // Recall load func AD

    public ADNativeCustom(FrameLayout adViewFrame, final String placementStr) {
        if (adViewFrame == null || placementStr == null) {
            PBMobileAds.getInstance().log("FrameLayout and placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("AD Native Init: " + placementStr);

        this.adViewFrame = adViewFrame;
        this.placement = placementStr;

        this.timerRecall = new TimerRecall(Constants.RECALL_CONFIGID_SERVER, 1000, new TimerCompleteListener() {
            @Override
            public void doWork() {
                isRecallingPreload = false;
                load();
            }
        });

        // Get AdUnit
        if (this.adUnitObj == null) {
            this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
            if (this.adUnitObj == null) {
                PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                    @Override
                    public void success(AdUnitObj data) {
                        PBMobileAds.getInstance().log("Get Config ADNativeCustom placement: " + placementStr + " Success");
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
                        PBMobileAds.getInstance().log("Get Config ADNativeCustom placement: " + placementStr + " Fail with Error: " + error.getLocalizedMessage());
                    }
                });
            }
        }
    }

    // MARK: - Private FUNC
    void deplayCallPreload() {
        this.isRecallingPreload = true;
        this.timerRecall.start();
    }

    AdInfor getAdInfor(boolean isVideo) {
        for (AdInfor infor : this.adUnitObj.adInfor) {
            if (infor.isVideo == isVideo) {
                return infor;
            }
        }
        return null;
    }

    void resetAD() {
        if (this.adUnit == null || this.amNativeDFP == null) return;

        this.isRecallingPreload = false;
        this.isLoadNativeSucc = false;
        this.adViewFrame.removeAllViews();

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amNativeDFP = null;
    }

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

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amNativeDFP.loadAd(this.amRequest);
        } else {
            if (resultCode == ResultCode.NO_BIDS) {
                this.deplayCallPreload();
            } else if (resultCode == ResultCode.TIMEOUT) {
                this.deplayCallPreload();
            }
        }
    }

    // MARK: - Public FUNC
    public boolean load() {
        PBMobileAds.getInstance().log("ADNativeCustom Placement '" + this.placement + "' - isLoaded: " + this.isLoaded() + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isLoaded() == true || this.isRecallingPreload == true) {
            return false;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("ADNativeCustom Placement '" + this.placement + "' is not active or not exist");
            return false;
        }

        // Check and set default
        this.adFormatDefault = this.adUnitObj.defaultFormat;
        // set adformat theo loại duy nhất có
        if (adUnitObj.adInfor.size() < 2) {
            this.adFormatDefault = this.adUnitObj.adInfor.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
        }

        // Get AdInfor
        boolean isVideo = this.adFormatDefault == ADFormat.VAST;
        AdInfor adInfor = this.getAdInfor(isVideo);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor of ADNativeCustom Placement '" + this.placement + "' is not exist");
            return false;
        }

        PBMobileAds.getInstance().log("Load ADNativeCustom Placement: " + this.placement);
        // Set GDPR
        if (PBMobileAds.getInstance().gdprConfirm) {
            String consentStr = CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp());
            if (consentStr != null && consentStr != "") {
                TargetingParams.setSubjectToGDPR(true);
                TargetingParams.setGDPRConsentString(CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp()));
            }
        }

        // Setup Host
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log("[ADNativeCustom] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

        // Create AdUnit
        this.adUnit = new NativeAdUnit(adInfor.configId);
        this.adUnit.setContextType(NativeAdUnit.CONTEXT_TYPE.SOCIAL_CENTRIC);
        this.adUnit.setPlacementType(NativeAdUnit.PLACEMENTTYPE.CONTENT_FEED);
        this.adUnit.setContextSubType(NativeAdUnit.CONTEXTSUBTYPE.GENERAL_SOCIAL);
        this.adUnit.setAutoRefreshPeriodMillis(this.timeAutoRefresh);

        setupNativeAsset();

        // Create AdLoadder
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();
        NativeAdOptions nativeOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        this.amNativeDFP = new AdLoader.Builder(PBMobileAds.getInstance().getContextApp(), adInfor.adUnitID)
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        PBMobileAds.getInstance().log("onNativeAdViewLoaded: ADNativeCustom Placement '" + placement + "'");

                        // You must call destroy on old ads when you are done with them,
                        // otherwise you will have a memory leak.
                        if (unifiedNativeAdObj != null) unifiedNativeAdObj.destroy();
                        unifiedNativeAdObj = unifiedNativeAd;

                        // Create AdNativeViewBuider
                        if (builder != null) {
                            builder.destroy();
                            builder = null;
                        }
                        builder = new ADNativeView.Builder(unifiedNativeAd);

                        // Add NativeAD to View
                        adViewFrame.removeAllViews();
                        adViewFrame.addView(builder.getNativeView());

                        if (adNativeDelegate != null)
                            adNativeDelegate.onNativeViewLoaded(builder);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();

                        isLoadNativeSucc = true;
                        PBMobileAds.getInstance().log("onAdLoaded: ADNativeCustom Placement '" + placement + "'");
                        if (adNativeDelegate != null) adNativeDelegate.onAdLoaded();
                    }

                    @Override
                    public void onAdLeftApplication() {
                        super.onAdLeftApplication();

                        PBMobileAds.getInstance().log("onAdClicked: ADNativeStyle Placement '" + placement + "'");
                        if (adNativeDelegate != null) adNativeDelegate.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        super.onAdFailedToLoad(errorCode);

                        String messErr = PBMobileAds.getInstance().getADError(errorCode);
                        PBMobileAds.getInstance().log("onAdFailedToLoad: ADNativeCustom Placement '" + placement + "' with error: " + messErr);
                        if (adNativeDelegate != null) adNativeDelegate.onAdFailedToLoad(messErr);
                    }
                })
                .withNativeAdOptions(nativeOptions)
                .build();

        // Create Request PBS
        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch ADNativeCustom placement '" + placement + "' for DFP: " + resultCode.name());
                handerResult(resultCode);
            }
        });

        return true;
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADNativeCustom Placement: " + this.placement);

        this.resetAD();

        if (unifiedNativeAdObj != null) {
            unifiedNativeAdObj.destroy();
            unifiedNativeAdObj = null;
        }

        if (this.timerRecall != null) {
            this.timerRecall.cancel();
            this.timerRecall = null;
        }
    }

    public void setListener(AdNativeDelegate adNativeDelegate) {
        this.adNativeDelegate = adNativeDelegate;
    }

    public void setAutoRefreshMillis(int timeMillis) {
        this.timeAutoRefresh = timeMillis;
        if (this.adUnit != null) {
            this.adUnit.setAutoRefreshPeriodMillis(timeMillis);
        }
    }

    public boolean isLoaded() {
        return this.isLoadNativeSucc;
    }

}


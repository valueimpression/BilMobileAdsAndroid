package com.bil.bilmobileads;

import android.content.Context;
import android.widget.FrameLayout;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.TimerRecall;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.TimerCompleteListener;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.facebook.ads.BidderTokenProvider;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.NativeAdUnit;
import org.prebid.mobile.NativeDataAsset;
import org.prebid.mobile.NativeEventTracker;
import org.prebid.mobile.NativeImageAsset;
import org.prebid.mobile.NativeTitleAsset;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.TargetingParams;
import org.prebid.mobile.addendum.AdViewUtils;
import org.prebid.mobile.addendum.PbFindSizeError;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class ADNative {

    // MARK: - View
    private FrameLayout adView;
    private AdDelegate adDelegate;

    // MARK: - AD
    private PublisherAdRequest amRequest;
    private NativeAdUnit adUnit;
    private PublisherAdView amNative;
    // MARK: - AD Info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: - Properties
    private ADFormat adFormatDefault;
    private int timeAutoRefresh = Constants.BANNER_AUTO_REFRESH_DEFAULT;
    private boolean isLoadNativeSucc = false; // true => banner đang chạy
    private boolean isRecallingPreload = false; // Check đang đợi gọi lại preload
    private TimerRecall timerRecall; // Recall load func AD

    public ADNative(FrameLayout adView, String placement) {
        if (adView == null || placement == null) {
            PBMobileAds.getInstance().log("FrameLayout and placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("AD Native Init: " + placement);

        this.adView = adView;
        this.placement = placement;

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
                        PBMobileAds.getInstance().log(error.getLocalizedMessage());
                    }
                });
            }
        }
    }

    // MARK: - Private FUNC
    void addFirstPartyData(AdUnit adUnit) {
        //        Access Control List
        //        Targeting.shared.addBidderToAccessControlList(Prebid.bidderNameAppNexus)

        //global user data
        TargetingParams.addUserData("globalUserDataKey1", "globalUserDataValue1");

        //global context data
        TargetingParams.addContextData("globalContextDataKey1", "globalContextDataValue1");

        //adunit context data
        adUnit.addContextData("adunitContextDataKey1", "adunitContextDataValue1");

        //global context keywords
        TargetingParams.addContextKeyword("globalContextKeywordValue1");
        TargetingParams.addContextKeyword("globalContextKeywordValue2");

        //global user keywords
        TargetingParams.addUserKeyword("globalUserKeywordValue1");
        TargetingParams.addUserKeyword("globalUserKeywordValue2");

        //adunit context keywords
        adUnit.addContextKeyword("adunitContextKeywordValue1");
        adUnit.addContextKeyword("adunitContextKeywordValue2");
    }

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
        if (this.adUnit == null || this.amNative == null) return;

        this.isRecallingPreload = false;
        this.isLoadNativeSucc = false;
        this.adView.removeAllViews();

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amNative.destroy();
        this.amNative.setAdListener(null);
        this.amNative = null;
    }

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amNative.loadAd(this.amRequest);
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
        PBMobileAds.getInstance().log("ADNative Placement '" + this.placement + "' - isLoaded: " + this.isLoaded() + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isLoaded() == true || this.isRecallingPreload == true) {
            return false;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("AdNative Placement '" + this.placement + "' is not active or not exist");
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
            PBMobileAds.getInstance().log("AdInfor of ADNative Placement '" + this.placement + "' is not exist");
            return false;
        }

        PBMobileAds.getInstance().log("Load ADNative Placement: " + this.placement);
        // Set GDPR
        if (PBMobileAds.getInstance().gdprConfirm) {
            String consentStr = CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp());
            if (consentStr != null && consentStr != "") {
                TargetingParams.setSubjectToGDPR(true);
                TargetingParams.setGDPRConsentString(CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp()));
            }
        }

        // Set FB Token
        String fbToken = BidderTokenProvider.getBidderToken(PBMobileAds.getInstance().getContextApp());
        TargetingParams.addUserData("fb_token", fbToken);

        // Setup Host
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log("[ADNative] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

        // Create AdUnit
        this.adUnit = new NativeAdUnit(adInfor.configId);
        this.adUnit.setContextType(NativeAdUnit.CONTEXT_TYPE.SOCIAL_CENTRIC);
        this.adUnit.setPlacementType(NativeAdUnit.PLACEMENTTYPE.CONTENT_FEED);
        this.adUnit.setContextSubType(NativeAdUnit.CONTEXTSUBTYPE.GENERAL_SOCIAL);
        this.adUnit.setAutoRefreshPeriodMillis(this.timeAutoRefresh);
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

        // Create AdView
        this.amNative = new PublisherAdView(PBMobileAds.getInstance().getContextApp());
        this.amNative.setAdUnitId(adInfor.adUnitID);
        this.amNative.setAdSizes(AdSize.FLUID);

        // Remove + Add Ad to view
        this.adView.removeAllViews();
        this.adView.addView(this.amNative);

        // Create Request
        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch ADNative placement '" + placement + "' for DFP: " + resultCode.name());
                handerResult(resultCode);
            }
        });

        this.amNative.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                AdViewUtils.findPrebidCreativeSize(amNative, new AdViewUtils.PbFindSizeListener() {
                    @Override
                    public void success(int width, int height) {
                        isLoadNativeSucc = true;
                        amNative.setAdSizes(new AdSize(width, height));

                        PBMobileAds.getInstance().log("onAdLoaded: ADNative Placement '" + placement + "'");
                        if (adDelegate == null) return;
                        adDelegate.onAdLoaded("onAdLoaded: ADNative Placement '" + placement + "'");
                    }

                    @Override
                    public void failure(@NonNull PbFindSizeError error) {
                        isLoadNativeSucc = true;
                        PBMobileAds.getInstance().log("onAdLoaded: ADNative Placement '" + placement + "' - " + error.getDescription());
                        if (adDelegate == null) return;
                        adDelegate.onAdFailedToLoad("onAdLoaded: ADNative Placement '" + placement + "' - " + error.getDescription());
                    }
                });
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();

                PBMobileAds.getInstance().log("onAdOpened: ADNative Placement '" + placement + "'");
                if (adDelegate == null) return;
                adDelegate.onAdOpened("onAdOpened: ADNative Placement '" + placement + "'");
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();

                PBMobileAds.getInstance().log("onAdClosed: ADNative Placement '" + placement + "'");
                if (adDelegate == null) return;
                adDelegate.onAdOpened("onAdClosed: ADNative Placement '" + placement + "'");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log("onAdClicked: ADNative Placement '" + placement + "'");
                if (adDelegate == null) return;
                adDelegate.onAdOpened("onAdClicked: ADNative Placement '" + placement + "'");
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();

                PBMobileAds.getInstance().log("onAdLeftApplication: ADNative Placement '" + placement + "'");
                if (adDelegate == null) return;
                adDelegate.onAdOpened("onAdLeftApplication: ADNative Placement '" + placement + "'");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);

                String messErr = PBMobileAds.getInstance().getADError(errorCode);
                PBMobileAds.getInstance().log("onAdFailedToLoad: ADNative Placement '" + placement + "' with error: " + messErr);
                if (adDelegate == null) return;
                adDelegate.onAdFailedToLoad("onAdFailedToLoad: ADNative Placement '" + placement + "' with error: " + messErr);
            }
        });

        return true;
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADNative Placement: " + this.placement);

        this.resetAD();
        if (this.timerRecall != null) {
            this.timerRecall.cancel();
            this.timerRecall = null;
        }
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

    public int getWidth() {
        return this.amNative.getWidth();
    }

    public int getHeight() {
        return this.amNative.getHeight();
    }

    public void setAutoRefreshMillis(int timeMillis) {
        this.timeAutoRefresh = timeMillis;
        if (this.adUnit != null) {
            this.adUnit.setAutoRefreshPeriodMillis(timeMillis);
        }
    }

    public boolean isLoaded() {
        return this.isLoadNativeSucc;
        //        if (this.amNative == null) return false;
        //        //
        //        //        return this.amNative.isLoading();
    }

}


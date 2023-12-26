package com.bil.bilmobileads;

import androidx.annotation.NonNull;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdData;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.NativeAdLoaderCustomDelegate;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
//import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.formats.NativeCustomTemplateAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeCustomFormatAd;

import org.prebid.mobile.NativeAdUnit;
import org.prebid.mobile.NativeDataAsset;
import org.prebid.mobile.NativeEventTracker;
import org.prebid.mobile.NativeImageAsset;
import org.prebid.mobile.NativeTitleAsset;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.PrebidNativeAd;
import org.prebid.mobile.PrebidNativeAdListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.addendum.AdViewUtils;
import org.prebid.mobile.rendering.bidding.data.bid.Bid;
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse;

import java.util.ArrayList;

public class ADNativeCustom {

    // MARK: - View
    private NativeAdLoaderCustomDelegate adNativeDelegate;

    // MARK: - AD
//    private PublisherAdRequest amRequest;
    private AdManagerAdRequest amRequest;
    private NativeAdUnit adUnit;
    private AdLoader amNativeDFP;

    // MARK: - AD Info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: - Properties
    private boolean isFetchingAD = false;

    public ADNativeCustom(final String placementStr) {
        if (placementStr == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "Placement is null");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log(LogType.INFOR, "ADNativeCustom placement: " + placementStr + " Init");

        this.placement = placementStr;

        this.getConfigAD();
    }

    // MARK: - Handle AD
    void getConfigAD() {
        this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
        if (this.adUnitObj == null) {
            this.isFetchingAD = true;

            // Get AdUnit
            PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
                @Override
                public void success(AdUnitObj data) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADNativeCustom placement: " + placement + " Init Success");
                    isFetchingAD = false;
                    adUnitObj = data;

                    PBMobileAds.getInstance().showCMP(() -> preload());
                }

                @Override
                public void failure(Exception error) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADNativeCustom placement: " + placement + " Init Failed with Error: " + error.getLocalizedMessage() + ". Please check your internet connect.");
                    isFetchingAD = false;
                }
            });
        } else {
            this.preload();
        }
    }

    void resetAD() {
        if (this.adUnit == null || this.amNativeDFP == null) return;

        this.isFetchingAD = false;
        this.amRequest = null;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amNativeDFP = null;
    }

    // MARK: - Public FUNC
    public void preload() {
        try {
            PBMobileAds.getInstance().log(LogType.DEBUG, "ADNativeCustom Placement '" + this.placement + "' - isFetchingAD: " + this.isFetchingAD);
            if (this.adUnitObj == null || this.isFetchingAD) {
                if (this.adUnitObj == null) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADNativeCustom placement: " + this.placement + " is not ready to load.");
                    this.getConfigAD();
                    return;
                }
                return;
            }
            this.resetAD();

            // Check Active
            if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() == 0) {
                PBMobileAds.getInstance().log(LogType.INFOR, "ADNativeCustom Placement '" + this.placement + "' is not active or not exist.");
                return;
            }

            // Get AdInfor
            AdInfor adInfor = this.adUnitObj.adInfor.get(0);
            if (adInfor == null) {
                PBMobileAds.getInstance().log(LogType.INFOR, "AdInfor of ADNativeCustom Placement '" + this.placement + "' is not exist.");
                return;
            }

            PBMobileAds.getInstance().log(LogType.INFOR, "Load ADNativeCustom Placement: " + this.placement);
            PBMobileAds.getInstance().setupPBS(adInfor.host);
            PBMobileAds.getInstance().log(LogType.DEBUG, "[ADNativeCustom] - configID: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

            this.adUnit = new NativeAdUnit(adInfor.configId);
            this.adUnit.setContextType(NativeAdUnit.CONTEXT_TYPE.SOCIAL_CENTRIC);
            this.adUnit.setPlacementType(NativeAdUnit.PLACEMENTTYPE.CONTENT_FEED);
            this.adUnit.setContextSubType(NativeAdUnit.CONTEXTSUBTYPE.GENERAL_SOCIAL);

            setupNativeAsset();

            // Create AdLoadder
            VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
            NativeAdOptions nativeOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
            this.amNativeDFP = new AdLoader.Builder(PBMobileAds.getInstance().getContextApp(), adInfor.adUnitID)
                    .forNativeAd(nativeAd -> {
                        isFetchingAD = false;

                        ADNativeView.Builder builder = new ADNativeView.Builder(placement, nativeAd);
                        PBMobileAds.getInstance().log(LogType.INFOR, "onNativeAdViewLoaded: ADNativeCustom Unified Placement '" + placement + "'");
                        if (adNativeDelegate != null) adNativeDelegate.onNativeViewLoaded(builder);

                        nativeAd.setOnPaidEventListener(adValue -> {
                            PBMobileAds.getInstance().log(LogType.INFOR, "onPaidEvent: ADNativeCustom Placement '" + placement + "'");
                            AdData adData = new AdData(adValue.getCurrencyCode(), adValue.getPrecisionType(), adValue.getValueMicros());
                            if (adNativeDelegate != null)
                                adNativeDelegate.onNativePaidEvent(adData);
                        });
                    })
                    .forCustomFormatAd(PBMobileAds.getInstance().nativeTemplateId, nativeCustomFormatAd -> AdViewUtils.findNative(nativeCustomFormatAd, new PrebidNativeAdListener() {
                        @Override
                        public void onPrebidNativeLoaded(PrebidNativeAd ad) {
                            isFetchingAD = false;

                            ADNativeView.Builder builder = new ADNativeView.Builder(placement, ad);
                            PBMobileAds.getInstance().log(LogType.INFOR, "onNativeAdViewLoaded: ADNativeCustom CustomTemplate Placement '" + placement + "'");
                            if (adNativeDelegate != null)
                                adNativeDelegate.onNativeViewLoaded(builder);
                        }

                        @Override
                        public void onPrebidNativeNotFound() {
                            PBMobileAds.getInstance().log(LogType.INFOR, "onPrebidNativeNotFound: ADNativeCustom CustomTemplate Placement '" + placement + "'");
                        }

                        @Override
                        public void onPrebidNativeNotValid() {
                            PBMobileAds.getInstance().log(LogType.INFOR, "onPrebidNativeNotValid: ADNativeCustom CustomTemplate Placement '" + placement + "'");
                        }
                    }), (nativeCustomFormatAd, s) -> PBMobileAds.getInstance().log(LogType.INFOR, "onCustomClick: ADNativeCustom CustomTemplate Placement '" + placement + "' " + s))
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADNativeCustom Placement '" + placement + "'");
                            if (adNativeDelegate != null) adNativeDelegate.onAdClicked();
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            PBMobileAds.getInstance().log(LogType.INFOR, "onAdImpression: ADNativeCustom Placement '" + placement + "'");
                            if (adNativeDelegate != null) adNativeDelegate.onAdImpression();
                        }

                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            PBMobileAds.getInstance().log(LogType.INFOR, "onAdImpression: ADNativeCustom Placement '" + placement + "'");
                            if (adNativeDelegate != null) adNativeDelegate.onAdClosed();
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);

                            isFetchingAD = false;
                            String messErr = "onAdFailedToLoad: ADNativeCustom Placement '" + placement + "' with error: " + loadAdError.getMessage(); // + PBMobileAds.getInstance().getADError(errorCode);
                            PBMobileAds.getInstance().log(LogType.INFOR, messErr);
                            if (adNativeDelegate != null)
                                adNativeDelegate.onNativeFailedToLoad(messErr);
                        }
                    })
                    .withNativeAdOptions(nativeOptions)
                    .build();

            // Create Request PBS
            this.isFetchingAD = true;
            this.amRequest = new AdManagerAdRequest.Builder().build();
            this.adUnit.fetchDemand(this.amRequest, resultCode -> {
                PBMobileAds.getInstance().log(LogType.INFOR, "PBS demand fetch ADNativeCustom placement '" + placement + "' for DFP: " + resultCode.name());
                if (amNativeDFP != null) amNativeDFP.loadAd(amRequest);
            });
        } catch (Exception e) {
            PBMobileAds.getInstance().log(LogType.ERROR, "ADNativeCustom Placement '" + this.placement + "' load failed with error: " + e.getLocalizedMessage());
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log(LogType.INFOR, "Destroy ADNativeCustom Placement: " + this.placement);
        this.resetAD();
    }

    public void setListener(NativeAdLoaderCustomDelegate adNativeDelegate) {
        this.adNativeDelegate = adNativeDelegate;
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
            PBMobileAds.getInstance().log(LogType.ERROR, e.getLocalizedMessage());
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
}


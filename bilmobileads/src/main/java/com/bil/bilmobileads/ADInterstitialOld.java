package com.bil.bilmobileads;

import android.app.Activity;

//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.FullScreenContentCallback;
//import com.google.android.gms.ads.LoadAdError;
//import com.google.android.gms.ads.OnPaidEventListener;
//import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
//import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
//import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AppEventListener;
//import com.google.android.gms.ads.admanager.AppEventListener;

//import org.prebid.mobile.AdUnit;
//import org.prebid.mobile.InterstitialAdUnit;
//import org.prebid.mobile.OnCompleteListener;
//import org.prebid.mobile.ResultCode;
//import org.prebid.mobile.VideoInterstitialAdUnit;

public class ADInterstitialOld {
//
//    // MARK: - View
//    private AdDelegate adDelegate;
//
//    // MARK: - AD
////    private PublisherAdRequest amRequest;
//    private AdManagerAdRequest amRequest;
//    private AdUnit adUnit;
//    //    private PublisherInterstitialAd amInterstitial;
//    private AdManagerInterstitialAd amInterstitial;
//    // MARK: - AD info
//    private String placement;
//    private AdUnitObj adUnitObj;
//
//    // MARK: Properties
//    private ADFormat adFormatDefault;
//
//    private boolean isFetchingAD = false;
//    private boolean setDefaultBidType = true;
//
//    public ADInterstitialOld(final String placementStr) {
//        if (placementStr == null) {
//            PBMobileAds.getInstance().log(LogType.ERROR, "Placement is null");
//            throw new NullPointerException();
//        }
//
//        PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + placementStr + " Init");
//        this.placement = placementStr;
//        this.getConfigAD();
//    }
//
//    // MARK: - Handle AD
//    void getConfigAD() {
//        this.adUnitObj = PBMobileAds.getInstance().getAdUnitObj(this.placement);
//        if (this.adUnitObj == null) {
//            this.isFetchingAD = true;
//
//            // Get AdUnit Info
//            PBMobileAds.getInstance().getADConfig(this.placement, new ResultCallback<AdUnitObj, Exception>() {
//                @Override
//                public void success(AdUnitObj data) {
//                    PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + placement + " Init Success");
//                    isFetchingAD = false;
//                    adUnitObj = data;
//
//                    PBMobileAds.getInstance().showCMP(new WorkCompleteDelegate() {
//                        @Override
//                        public void doWork() {
//                            preLoad();
//                        }
//                    });
//                }
//
//                @Override
//                public void failure(Exception error) {
//                    PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + placement + " Init Failed with Error: " + error.getLocalizedMessage() + ". Please check your internet connect.");
//                    isFetchingAD = false;
//                }
//            });
//        } else {
//            this.preLoad();
//        }
//    }
//
//    boolean processNoBids() {
//        if (this.adUnitObj.adInfor.size() >= 2 && this.adFormatDefault == this.adUnitObj.defaultFormat) {
//            this.setDefaultBidType = false;
//            this.preLoad();
//
//            return true;
//        } else {
//            // Both or .video, .html is no bids -> wait and preload.
//            PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial Placement '" + this.placement + "' No Bids.");
//            return false;
//        }
//    }
//
//    void resetAD() {
//        if (this.adUnit == null || this.amInterstitial == null) return;
//
//        this.isFetchingAD = false;
//        this.amRequest = null;
//
//        this.adUnit.stopAutoRefresh();
//        this.adUnit = null;
//
//        this.amInterstitial.setAppEventListener(null);
////        this.amInterstitial.setAdListener(null);
//        this.amInterstitial = null;
//    }
//
//    // MARK: - Public FUNC
//    public void preLoad() {
//        PBMobileAds.getInstance().log(LogType.DEBUG, "ADInterstitial Placement '" + this.placement + "' - isReady: " + this.isReady() + " | isFetchingAD: " + this.isFetchingAD);
//        if (this.adUnitObj == null || this.isReady() || this.isFetchingAD) {
//            if (this.adUnitObj == null && !this.isFetchingAD) {
//                PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + this.placement + " is not ready to load.");
//                this.getConfigAD();
//                return;
//            }
//            return;
//        }
//        this.resetAD();
//
//        // Check Active
//        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
//            PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial Placement '" + this.placement + "' is not active or not exist.");
//            return;
//        }
//
//        // Check and Set Default
//        this.adFormatDefault = this.adUnitObj.defaultFormat;
//        if (!this.setDefaultBidType && this.adUnitObj.adInfor.size() >= 2) {
//            this.adFormatDefault = this.adFormatDefault == ADFormat.VAST ? ADFormat.HTML : ADFormat.VAST;
//            this.setDefaultBidType = true;
//        }
//
//        // Check AdInfor
//        boolean isVideo = this.adFormatDefault == ADFormat.VAST;
//        AdInfor adInfor = PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
//        if (adInfor == null) {
//            PBMobileAds.getInstance().log(LogType.INFOR, "AdInfor of ADInterstitial Placement '" + this.placement + "' is not exist.");
//            return;
//        }
//
//        PBMobileAds.getInstance().log(LogType.INFOR, "Preload ADInterstitial Placement: " + this.placement);
//        PBMobileAds.getInstance().setupPBS(adInfor.host);
//        if (isVideo) {
//            PBMobileAds.getInstance().log(LogType.DEBUG, "[ADInterstitial Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
//            this.adUnit = new VideoInterstitialAdUnit(adInfor.configId);
//        } else {
//            PBMobileAds.getInstance().log(LogType.DEBUG, "[ADInterstitial HTML] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
//            this.adUnit = new InterstitialAdUnit(adInfor.configId);
//        }
//
////        this.amInterstitial = new PublisherInterstitialAd(PBMobileAds.getInstance().getContextApp());
//        this.amInterstitial = new AdManagerInterstitialAd() {
//            @Nullable
//            @Override
//            public FullScreenContentCallback getFullScreenContentCallback() {
//                return null;
//            }
//
//            @Nullable
//            @Override
//            public OnPaidEventListener getOnPaidEventListener() {
//                return null;
//            }
//
//            @NonNull
//            @Override
//            public ResponseInfo getResponseInfo() {
//                return null;
//            }
//
//            @NonNull
//            @Override
//            public String getAdUnitId() {
//                return null;
//            }
//
//            @Override
//            public void setFullScreenContentCallback(@Nullable FullScreenContentCallback fullScreenContentCallback) {
//
//            }
//
//            @Override
//            public void setImmersiveMode(boolean b) {
//
//            }
//
//            @Override
//            public void setOnPaidEventListener(@Nullable OnPaidEventListener onPaidEventListener) {
//
//            }
//
//            @Override
//            public void show(@NonNull Activity activity) {
//
//            }
//
//            @Nullable
//            @Override
//            public AppEventListener getAppEventListener() {
//                return null;
//            }
//
//            @Override
//            public void setAppEventListener(@Nullable AppEventListener appEventListener) {
//
//            }
//        };
////        this.amInterstitial.setAdUnitId(adInfor.adUnitID);
////        this.amInterstitial.setAdListener(new AdListener() {
//        this.amInterstitial.setAppEventListener(new AdListener() {
//            @Override
//            public void onAdLoaded() {
//                super.onAdLoaded();
//
//                isFetchingAD = false;
//                PBMobileAds.getInstance().log(LogType.INFOR, "onAdLoaded: ADInterstitial Placement '" + placement + "'");
//                if (adDelegate != null) adDelegate.onAdLoaded();
//            }
//
//            @Override
//            public void onAdOpened() {
//                super.onAdOpened();
//
//                PBMobileAds.getInstance().log(LogType.INFOR, "onAdOpened: ADInterstitial Placement '" + placement + "'");
//                if (adDelegate != null) adDelegate.onAdOpened();
//            }
////            @Override
////            public void onAdImpression() {
////                super.onAdImpression();
////
////                PBMobileAds.getInstance().log(LogType.INFOR, "onAdImpression: ADInterstitial Placement '" + placement + "'");
////                if (adDelegate != null) adDelegate.onAdOpened();
////            }
//
//            @Override
//            public void onAdClosed() {
//                super.onAdClosed();
//
//                PBMobileAds.getInstance().log(LogType.INFOR, "onAdClosed: ADInterstitial Placement '" + placement + "'");
//                if (adDelegate != null) adDelegate.onAdClosed();
//            }
//
//            @Override
//            public void onAdClicked() {
//                super.onAdClicked();
//
//                PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADInterstitial Placement '" + placement + "'");
//                if (adDelegate != null) adDelegate.onAdClicked();
//            }
//
//            @Override
//            public void onAdLeftApplication() {
//                super.onAdLeftApplication();
//
//                PBMobileAds.getInstance().log(LogType.INFOR, "onAdLeftApplication: ADInterstitial Placement '" + placement + "'");
//                if (adDelegate != null) adDelegate.onAdLeftApplication();
//            }
//
//            @Override
//            public void onAdFailedToLoad(int errorCode) {
//                super.onAdFailedToLoad(errorCode);
//                //            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
////                super.onAdFailedToLoad(loadAdError);
//
//                isFetchingAD = false;
////                int errorCode = i; // loadAdError.getCode();
//                if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
//                    if (!processNoBids()) {
//                        if (adDelegate != null)
//                            adDelegate.onAdFailedToLoad("onAdFailedToLoad: ADInterstitial Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode));
//                    }
//                } else {
//                    String messErr = "onAdFailedToLoad: ADInterstitial Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode);
//                    PBMobileAds.getInstance().log(LogType.INFOR, messErr);
//                    if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
//                }
//            }
//        });
//
//        // Create Request PBS
//        this.isFetchingAD = true;
////        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
//        final AdManagerAdRequest.Builder builder = new AdManagerAdRequest.Builder();
////        if (PBMobileAds.getInstance().isTestMode) {
////            builder.addTestDevice(Constants.DEVICE_ID_TEST);
////        }
//        this.amRequest = builder.build();
//        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
//            @Override
//            public void onComplete(ResultCode resultCode) {
//                PBMobileAds.getInstance().log(LogType.DEBUG, "PBS demand fetch ADInterstitial placement '" + placement + "' for DFP: " + resultCode.name());
//                if (resultCode == ResultCode.SUCCESS) {
////                    amInterstitial.loadAd(amRequest);
//                } else {
//                    isFetchingAD = false;
//                    if (resultCode == ResultCode.NO_BIDS) {
//                        processNoBids();
//                    } else if (resultCode == ResultCode.TIMEOUT) {
//                        PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial Placement '" + placement + "' Timeout. Please check your internet connect.");
//                    }
//                }
//            }
//        });
//    }
//
//    public void show() {
//        if (isReady()) {
////            this.amInterstitial.show();
////            this.amInterstitial.show((Activity) PBMobileAds.getInstance().getContextApp());
//        } else {
//            PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial Placement '" + this.placement + "' currently unavailable, call preload() first.");
//        }
//    }
//
//    public void destroy() {
//        PBMobileAds.getInstance().log(LogType.INFOR, "Destroy ADInterstitial Placement: " + this.placement);
//        this.resetAD();
//    }
//
//    public boolean isReady() {
//        if (this.adUnit == null || this.amInterstitial == null) return false;
////        return this.amInterstitial.isLoaded();
//        return true;
//    }
//
//    public void setListener(AdDelegate adDelegate) {
//        this.adDelegate = adDelegate;
//    }

}

package com.bil.bilmobileads;

import static com.google.android.gms.common.util.CollectionUtils.listOf;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;

import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;

import org.prebid.mobile.InterstitialAdUnit;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.Signals;
import org.prebid.mobile.VideoParameters;
import org.prebid.mobile.api.data.AdUnitFormat;

import java.util.EnumSet;
import java.util.List;

public class ADInterstitial {

    // MARK: - View
    private AdDelegate adDelegate;

    // MARK: - AD
    private AdManagerAdRequest amRequest;
    private InterstitialAdUnit adUnit;
    private AdManagerInterstitialAd amInterstitial;
    // MARK: - AD info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: Properties
    private boolean isFetchingAD = false;

    public ADInterstitial(final String placementStr) {
        if (placementStr == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "Placement is null");
            throw new NullPointerException();
        }

        PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + placementStr + " Init");
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
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + placement + " Init Success");
                    isFetchingAD = false;
                    adUnitObj = data;

                    PBMobileAds.getInstance().showCMP(new WorkCompleteDelegate() {
                        @Override
                        public void doWork() {
                            preLoad();
                        }
                    });
                }

                @Override
                public void failure(Exception error) {
                    PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + placement + " Init Failed with Error: " + error.getLocalizedMessage() + ". Please check your internet connect.");
                    isFetchingAD = false;
                }
            });
        } else {
            this.preLoad();
        }
    }

    void resetAD() {
        if (this.adUnit == null || this.amInterstitial == null) return;

        this.isFetchingAD = false;
        this.amRequest = null;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amInterstitial.setAppEventListener(null);
        this.amInterstitial = null;
    }

    // MARK: - Public FUNC
    public void preLoad() {
        PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial Placement '" + this.placement + "' - isReady: " + this.isReady() + " | isFetchingAD: " + this.isFetchingAD);
        if (this.adUnitObj == null || this.isReady() || this.isFetchingAD) {
            if (this.adUnitObj == null && !this.isFetchingAD) {
                PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial placement: " + this.placement + " is not ready to load.");
                this.getConfigAD();
                return;
            }
            return;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() == 0) {
            PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial Placement '" + this.placement + "' is not active or not exist.");
            return;
        }

        // Check AdInfor
        final AdInfor adInfor = this.adUnitObj.adInfor.get(0); // PBMobileAds.getInstance().getAdInfor(isVideo, this.adUnitObj);
        if (adInfor == null) {
            PBMobileAds.getInstance().log(LogType.INFOR, "AdInfor of ADInterstitial Placement '" + this.placement + "' is not exist.");
            return;
        }

        PBMobileAds.getInstance().log(LogType.INFOR, "Preload ADInterstitial Placement: " + this.placement);
        PBMobileAds.getInstance().setupPBS(adInfor.host);

        PBMobileAds.getInstance().log(LogType.DEBUG, "[ADInterstitial] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);
        this.adUnit = new InterstitialAdUnit(adInfor.configId, EnumSet.of(AdUnitFormat.BANNER, AdUnitFormat.VIDEO));
//        this.adUnit.setMinSizePercentage(80, 60);

        VideoParameters videoParameters = new VideoParameters(listOf("video/x-flv", "video/mp4"));
        videoParameters.setPlacement(Signals.Placement.Interstitial);
//        videoParameters.setApi(listOf(Signals.Api.VPAID_1, Signals.Api.VPAID_2));
//        videoParameters.setPlaybackMethod(listOf(Signals.PlaybackMethod.AutoPlaySoundOn));
//        videoParameters.setProtocols(listOf(Signals.Protocols.VAST_2_0));
        this.adUnit.setVideoParameters(videoParameters);

        // Create Request PBS
        this.isFetchingAD = true;
        this.amRequest = new AdManagerAdRequest.Builder().build();
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log(LogType.INFOR, "PBS demand fetch ADInterstitial placement '" + placement + "' for DFP: " + resultCode.name());

                AdManagerInterstitialAd.load(PBMobileAds.getInstance().getContextApp(), adInfor.adUnitID, amRequest, new AdManagerInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AdManagerInterstitialAd adManagerInterstitialAd) {
                        super.onAdLoaded(adManagerInterstitialAd);

                        isFetchingAD = false;
                        amInterstitial = adManagerInterstitialAd;
                        setAdsListener();

                        PBMobileAds.getInstance().log(LogType.INFOR, "onAdLoaded: ADInterstitial Placement '" + placement + "'");
                        if (adDelegate != null) adDelegate.onAdLoaded();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);

                        isFetchingAD = false;
                        amInterstitial = null;

                        int errorCode = loadAdError.getCode();
                        String messErr = "onAdFailedToLoad: ADInterstitial Placement '" + placement + "' with error: " + PBMobileAds.getInstance().getADError(errorCode);
                        PBMobileAds.getInstance().log(LogType.INFOR, messErr);
                        if (adDelegate != null) adDelegate.onAdFailedToLoad(messErr);
                    }
                });
            }
        });
    }

    void setAdsListener() {
        if (amInterstitial == null) return;

        amInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdClicked: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdImpression: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdOpened();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                amInterstitial = null;

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdDismissedFullScreenContent: ADInterstitial Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onAdClosed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                amInterstitial = null;

                PBMobileAds.getInstance().log(LogType.INFOR, "onAdFailedToShowFullScreenContent: ADInterstitial Placement '" + placement + "' | " + adError.getMessage());
                if (adDelegate != null) adDelegate.onAdFailedToLoad(adError.getMessage());
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                PBMobileAds.getInstance().log(LogType.INFOR, "onAdShowedFullScreenContent: ADInterstitial Placement '" + placement + "'");
            }
        });
    }

    public void show() {
        if (isReady()) {
            this.amInterstitial.show((Activity) PBMobileAds.getInstance().getContextApp());
        } else {
            PBMobileAds.getInstance().log(LogType.INFOR, "ADInterstitial Placement '" + this.placement + "' currently unavailable, call preLoad() first.");
        }
    }

    public void destroy() {
        PBMobileAds.getInstance().log(LogType.INFOR, "Destroy ADInterstitial Placement: " + this.placement);
        this.resetAD();
    }

    public boolean isReady() {
        if (this.adUnit == null || this.amInterstitial == null) return false;
        return true;
    }

    public void setListener(AdDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }

}

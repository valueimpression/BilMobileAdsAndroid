package com.bil.bilmobileads;

import android.app.Activity;
import android.content.Context;

import com.bil.bilmobileads.interfaces.ResultCallback;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.TimerRecall;
import com.bil.bilmobileads.interfaces.AdRewardedDelegate;
import com.bil.bilmobileads.interfaces.TimerCompleteListener;

import org.prebid.mobile.AdUnit;
import org.prebid.mobile.OnCompleteListener;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.RewardedVideoAdUnit;
import org.prebid.mobile.TargetingParams;

import androidx.annotation.NonNull;

public class ADRewarded {

    // MARK: - View
    private Activity activityAd;
    private AdRewardedDelegate adDelegate;

    // MARK: - AD
    private PublisherAdRequest amRequest;
    private AdUnit adUnit;
    private RewardedAd amRewarded;
    // MARK: - AD info
    private String placement;
    private AdUnitObj adUnitObj;

    // MARK: Properties
    private ADFormat adFormatDefault;
    private boolean isLoadAfterPreload = false; // Check user gọi load() chưa có AD -> preload và show AD luôn
    private boolean isFetchingAD = false; // Check có phải đang lấy AD ko
    private boolean isRecallingPreload = false; // Check đang đợi gọi lại preload
    private TimerRecall timerRecall; // Recall load func AD

    public ADRewarded(Activity activity, String placement) {
        if (activity == null || placement == null) {
            PBMobileAds.getInstance().log("Activity and Placement is not nullable");
            throw new NullPointerException();
        }
        PBMobileAds.getInstance().log("AD Interstitial Init: " + placement);

        this.activityAd = activity;
        this.placement = placement;

        this.timerRecall = new TimerRecall(Constants.RECALL_CONFIGID_SERVER, 1000, new TimerCompleteListener() {
            @Override
            public void doWork() {
                isRecallingPreload = false;
                preLoad();
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
                                    preLoad();
                                }
                            });
                        } else {
                            preLoad();
                        }
                    }

                    @Override
                    public void failure(Exception error) {
                        PBMobileAds.getInstance().log(error.getLocalizedMessage());
                    }
                });
            } else {
                this.preLoad();
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
        if (this.adUnit == null || this.amRewarded == null) return;

        this.isRecallingPreload = false;

        this.adUnit.stopAutoRefresh();
        this.adUnit = null;

        this.amRewarded = null;
    }

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amRewarded.loadAd(this.amRequest, new RewardedAdLoadCallback() {
                @Override
                public void onRewardedAdLoaded() {
                    isFetchingAD = false;

                    if (isLoadAfterPreload) {
                        isLoadAfterPreload = false;
                        if (isReady()) show();
                    }

                    PBMobileAds.getInstance().log("onRewardedAdLoaded: ADRewarded Placement '" + placement + "' Succ");
                    if (adDelegate != null) adDelegate.onRewardedAdLoaded();
                }

                @Override
                public void onRewardedAdFailedToLoad(int errorCode) {
                    isFetchingAD = false;

                    String messErr = PBMobileAds.getInstance().getAdRewardedError(errorCode);
                    PBMobileAds.getInstance().log("onRewardedAdFailedToLoad: ADRewarded Placement '" + placement + "' failed: " + messErr);
                    if (adDelegate != null) adDelegate.onRewardedAdFailedToLoad();
                    //  if (!isLoadAfterPreload) deplayCallPreload();
                }
            });
        } else {
            if (resultCode == ResultCode.NO_BIDS) {
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            } else if (resultCode == ResultCode.TIMEOUT) {
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            }

            this.isFetchingAD = false;
        }
    }

    // MARK: - Public FUNC
    public boolean preLoad() {
        PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' - isReady: " + this.isReady() + " |  isFetchingAD: " + this.isFetchingAD + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isReady() == true || this.isFetchingAD == true || this.isRecallingPreload == true) {
            return false;
        }
        this.resetAD();

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' is not active or not exist");
            return false;
        }

        // Check and set default
        this.adFormatDefault = this.adUnitObj.defaultFormat;
        // set adFormat theo loại duy nhất có
        if (adUnitObj.adInfor.size() < 2) {
            this.adFormatDefault = this.adUnitObj.adInfor.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
        }

        // Check AdInfor
        AdInfor adInfor = this.getAdInfor(true);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor of ADRewarded Placement '" + this.placement + "' is not exist");
            return false;
        }

        PBMobileAds.getInstance().log("Preload ADRewarded Placement: " + this.placement);
        // Set GDPR
        if (PBMobileAds.getInstance().gdprConfirm) {
            String consentStr = CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp());
            if (consentStr != null && consentStr != "") {
                TargetingParams.setSubjectToGDPR(true);
                TargetingParams.setGDPRConsentString(CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp()));
            }
        }

        // Set PBS Host
        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log("[ADRewarded Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

        this.adUnit = new RewardedVideoAdUnit(adInfor.configId);
        this.amRewarded = new RewardedAd(PBMobileAds.getInstance().getContextApp(), adInfor.adUnitID);

        this.isFetchingAD = true;
        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch ADRewarded placement '" + placement + "' for DFP: " + resultCode.name());
                handerResult(resultCode);
            }
        });

        return true;
    }

    public void load() {
        if (this.isReady()) {
            this.show();
        } else {
            PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' Don't have AD");
            this.isLoadAfterPreload = true;
            this.preLoad();
        }
    }

    void show() {
        this.amRewarded.show(this.activityAd, new RewardedAdCallback() {
            @Override
            public void onRewardedAdOpened() {
                isFetchingAD = false;

                PBMobileAds.getInstance().log("onRewardedAdOpened: ADRewarded Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onRewardedAdOpened();
            }

            @Override
            public void onRewardedAdClosed() {
                isFetchingAD = false;

                PBMobileAds.getInstance().log("onRewardedAdClosed: ADRewarded Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onRewardedAdClosed();
            }

            @Override
            public void onUserEarnedReward(@NonNull RewardItem reward) {
                isFetchingAD = false;

                PBMobileAds.getInstance().log("onUserEarnedReward: ADRewarded Placement '" + placement + "'");
                if (adDelegate != null) adDelegate.onUserEarnedReward(placement);
            }

            @Override
            public void onRewardedAdFailedToShow(int errorCode) {
                super.onRewardedAdFailedToShow(errorCode);

                isFetchingAD = false;
                String messErr = PBMobileAds.getInstance().getAdRewardedError(errorCode);
                PBMobileAds.getInstance().log("onRewardedAdFailedToShow: ADRewarded Placement '" + placement + "' with error: " + messErr);
                if (adDelegate != null) adDelegate.onRewardedAdFailedToShow();
            }
        });
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy ADRewarded Placement: " + this.placement);

        this.resetAD();
        if (this.timerRecall != null) {
            this.timerRecall.cancel();
            this.timerRecall = null;
        }
    }

    public boolean isReady() {
        if (this.amRewarded == null) return false;

        PBMobileAds.getInstance().log("ADRewarded Placement '" + this.placement + "' IsReady: " + this.amRewarded.isLoaded());

        return this.amRewarded.isLoaded();
    }

    public void setListener(AdRewardedDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }
}

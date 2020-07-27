package com.bil.bilmobileads;

import android.app.Activity;
import android.content.Context;

import com.bil.bilmobileads.interfaces.ResultCallback;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.consentmanager.sdk.storage.CMPStorageV1;
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
import org.prebid.mobile.Signals;
import org.prebid.mobile.TargetingParams;
import org.prebid.mobile.VideoBaseAdUnit;

import java.util.Arrays;

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
        final PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();
        if (PBMobileAds.getInstance().isTestMode) {
            builder.addTestDevice(Constants.DEVICE_ID_TEST);
        }
        this.amRequest = builder.build();

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

    // MARK: - Preload + Load
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

    void handerResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            this.amRewarded.loadAd(this.amRequest, new RewardedAdLoadCallback() {
                @Override
                public void onRewardedAdLoaded() {
                    PBMobileAds.getInstance().log("RewarededVideo load placement '" + placement + "': SUCC");
                    isFetchingAD = false;

                    if (isLoadAfterPreload) {
                        isLoadAfterPreload = false;
                        if (isReady()) show();
                    }
                }

                @Override
                public void onRewardedAdFailedToLoad(int errorCode) {
                    PBMobileAds.getInstance().log("RewardedVideo load placement '" + placement + "' failed: " + errorCode);
                    isFetchingAD = false;

                    if (!isLoadAfterPreload) deplayCallPreload();
                }
            });
        } else {
            if (resultCode == ResultCode.NO_BIDS) {
                this.adFormatDefault = this.adFormatDefault.equals(ADFormat.HTML) ? ADFormat.VAST : ADFormat.HTML;
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            } else if (resultCode == ResultCode.TIMEOUT) {
                // Ko gọi lại preload nếu user gọi load() đầu tiên
                if (!this.isLoadAfterPreload) this.deplayCallPreload();
            }

            this.isFetchingAD = false;
        }
    }

    public boolean preLoad() {
        PBMobileAds.getInstance().log(" | isReady: " + this.isReady() + " |  isFetchingAD: " + this.isFetchingAD + " |  isRecallingPreload: " + this.isRecallingPreload);
        if (this.adUnitObj == null || this.isReady() == true || this.isFetchingAD == true || this.isRecallingPreload == true) {
            return false;
        }
        PBMobileAds.getInstance().log("Preload Rewareded AD: " + this.placement);

        // Check Active
        if (!this.adUnitObj.isActive || this.adUnitObj.adInfor.size() <= 0) {
            PBMobileAds.getInstance().log("Ad is not active or not exist");
            return false;
        }

        // Check and set default
        this.adFormatDefault = this.adFormatDefault == null ? this.adUnitObj.defaultType : this.adFormatDefault;
        // set adFormat theo loại duy nhất có
        if (adUnitObj.adInfor.size() < 2) {
            this.adFormatDefault = this.adUnitObj.adInfor.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
        }

        // Set GDPR
        if (PBMobileAds.getInstance().gdprConfirm) {
            TargetingParams.setSubjectToGDPR(true);
            TargetingParams.setGDPRConsentString(CMPStorageConsentManager.getConsentString(PBMobileAds.getInstance().getContextApp()));
        }

        AdInfor adInfor = this.getAdInfor(true);
        if (adInfor == null) {
            PBMobileAds.getInstance().log("AdInfor is not exist");
            return false;
        }

        PBMobileAds.getInstance().setupPBS(adInfor.host);
        PBMobileAds.getInstance().log("[Full Video] - configId: " + adInfor.configId + " | adUnitID: " + adInfor.adUnitID);

        VideoBaseAdUnit.Parameters parameters = new VideoBaseAdUnit.Parameters();
        parameters.setMimes(Arrays.asList("video/mp4"));
        parameters.setProtocols(Arrays.asList(Signals.Protocols.VAST_2_0));
        parameters.setPlaybackMethod(Arrays.asList(Signals.PlaybackMethod.AutoPlaySoundOn));
        parameters.setPlacement(Signals.Placement.InBanner);

        RewardedVideoAdUnit rAdUnit = new RewardedVideoAdUnit(adInfor.configId);
        rAdUnit.setParameters(parameters);
        this.adUnit = rAdUnit;

        this.amRewarded = new RewardedAd(PBMobileAds.getInstance().getContextApp().getApplicationContext(), adInfor.adUnitID);

        this.isFetchingAD = true;
        this.adUnit.fetchDemand(this.amRequest, new OnCompleteListener() {
            @Override
            public void onComplete(ResultCode resultCode) {
                PBMobileAds.getInstance().log("Prebid demand fetch placement '" + placement + "' for DFP: " + resultCode.name());
                handerResult(resultCode);
            }
        });

        return true;
    }

    public void load() {
        if (this.isReady()) {
            this.show();
        } else {
            PBMobileAds.getInstance().log("Don't have AD");
            this.isLoadAfterPreload = true;
            this.preLoad();
        }
    }

    private void show() {
        this.amRewarded.show(this.activityAd, new RewardedAdCallback() {
            @Override
            public void onRewardedAdOpened() {
                isFetchingAD = false;
                if (adDelegate == null) return;
                adDelegate.onRewardedAdOpened("onRewardedAdOpened");
            }

            @Override
            public void onRewardedAdClosed() {
                isFetchingAD = false;
                if (adDelegate == null) return;
                adDelegate.onRewardedAdOpened("onRewardedAdClosed");
            }

            @Override
            public void onUserEarnedReward(@NonNull RewardItem reward) {
                isFetchingAD = false;
                if (adDelegate == null) return;
                adDelegate.onRewardedAdOpened("onUserEarnedReward");
            }

            @Override
            public void onRewardedAdFailedToShow(int errorCode) {
                isFetchingAD = false;
                if (adDelegate == null) return;
                switch (errorCode) {
                    case ERROR_CODE_INTERNAL_ERROR:
                        adDelegate.onRewardedAdOpened("INTERNAL_ERROR");
                        break;
                    case ERROR_CODE_AD_REUSED:
                        adDelegate.onRewardedAdOpened("AD_REUSED");
                        break;
                    case ERROR_CODE_NOT_READY:
                        adDelegate.onRewardedAdOpened("NOT_READY");
                        break;
                    case ERROR_CODE_APP_NOT_FOREGROUND:
                        adDelegate.onRewardedAdOpened("APP_NOT_FOREGROUND");
                        break;
                }
            }
        });
    }

    public void destroy() {
        PBMobileAds.getInstance().log("Destroy Placement: " + this.placement);
        this.isLoadAfterPreload = false;
        this.adUnit.stopAutoRefresh();

        this.timerRecall.cancel();
        this.timerRecall = null;
    }

    public boolean isReady() {
        if (this.amRewarded == null) return false;

        PBMobileAds.getInstance().log("AD IsReady: " + this.amRewarded.isLoaded());

        return this.amRewarded.isLoaded();
    }

    public void setListener(AdRewardedDelegate adDelegate) {
        this.adDelegate = adDelegate;
    }
}

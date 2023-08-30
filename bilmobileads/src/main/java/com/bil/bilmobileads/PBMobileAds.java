
package com.bil.bilmobileads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.ADType;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.BannerSize;
import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.entity.HostCustom;
import com.bil.bilmobileads.interfaces.ResultCallback;
import com.bil.bilmobileads.interfaces.WorkCompleteDelegate;
import com.consentmanager.sdk.CMPConsentTool;
import com.consentmanager.sdk.callbacks.OnCloseCallback;
import com.consentmanager.sdk.model.CMPConfig;
import com.consentmanager.sdk.storage.CMPStorageConsentManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
//import com.google.android.gms.ads.rewarded.RewardedAdCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.prebid.mobile.Host;
import org.prebid.mobile.PrebidMobile;
import org.prebid.mobile.TargetingParams;
import org.prebid.mobile.api.data.InitializationStatus;
import org.prebid.mobile.rendering.listeners.SdkInitializationListener;
import org.prebid.mobile.rendering.sdk.deviceData.managers.UserConsentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PBMobileAds {

    private static final PBMobileAds instance = new PBMobileAds();

    // MARK: Context app
    private Context contextApp;
    // MARK: List Config
    ArrayList<AdUnitObj> listAdUnitObj = new ArrayList<AdUnitObj>();
    // MARK: api
    private String pbServerEndPoint = "";
    String nativeTemplateId = "";
    boolean gdprConfirm = false;
    boolean isShowCMP = false;

    // LOG:
    private final boolean DEBUG_MODE = false;

    private PBMobileAds() {
    }

    public static PBMobileAds getInstance() {
        return instance;
    }

    // MARK: - initialize
    public void initialize(Context context, boolean testMode) {
        if (context == null) {
            PBMobileAds.getInstance().log(LogType.ERROR, "Context is null");
            throw new NullPointerException();
        }
        this.contextApp = context;

        // Declare in init to the user agent could be passed in first call
        PrebidMobile.initializeSdk(context, status -> PBMobileAds.getInstance().log(LogType.INFOR, "PBMobileAds Init"));
        PrebidMobile.setShareGeoLocation(true);
        PrebidMobile.setPbsDebug(testMode);
//        PrebidMobile.setApplicationContext(context.getApplicationContext());

        this.log(LogType.DEBUG, "Webview With SDK: " + Build.VERSION.SDK_INT);
        WebView webView = new WebView(Build.VERSION.SDK_INT > 21 ? context : context.getApplicationContext());
        webView.clearCache(true);
    }

    public void setTestDeviceIds(String testDeviceIds) {
        // Sample device ID:  33BE2250B43518CCDA7DE426D04EE231
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList(testDeviceIds)).build();
        MobileAds.setRequestConfiguration(configuration);
    }

    // MARK: - Call API AD
    void getADConfig(final String adUnit, final ResultCallback resultAD) {
        try {
            this.log(LogType.DEBUG, "Start Request Config adUnit: " + adUnit);

            // Get GAM App ID
            ApplicationInfo appInfo = this.contextApp.getPackageManager().getApplicationInfo(this.contextApp.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = appInfo.metaData;
            String gamAppID = bundle.getString("com.google.android.gms.ads.APPLICATION_ID");

            HttpApi httpApi = new HttpApi<JSONObject>(Constants.GET_DATA_CONFIG + adUnit + "&appID=" + gamAppID, new ResultCallback<JSONObject, Exception>() {
                @Override
                public void success(JSONObject dataJSON) {
                    try {
                        // Check AdUnitObj Exist if init new Ad > 2
                        AdUnitObj adUnitObjCheck = getAdUnitObj(adUnit);
                        if (adUnitObjCheck != null) {
                            resultAD.success(adUnitObjCheck);
                            return;
                        }

                        pbServerEndPoint = dataJSON.getString("pbServerEndPoint");
                        nativeTemplateId = dataJSON.getString("nativeTemplateId");
                        gdprConfirm = dataJSON.getBoolean("gdprConfirm");

                        // Set all ad type config
                        JSONObject adunitJsonObj = dataJSON.getJSONObject("adunit");

                        String placement = adunitJsonObj.getString("placement");
                        ADType type = getAdType(adunitJsonObj.getString("type"));
                        boolean isActive = adunitJsonObj.getBoolean("isActive");
                        int refreshTime = adunitJsonObj.getInt("refreshTime");

                        // Create AdInfor
                        ArrayList<AdInfor> adInforList = new ArrayList<AdInfor>();
                        JSONArray adInforArray = adunitJsonObj.getJSONArray("adInfor");
                        for (int j = 0; j < adInforArray.length(); j++) {
                            JSONObject adInforObj = adInforArray.getJSONObject(j);
                            boolean isVideo = adInforObj.getBoolean("isVideo");
                            String configId = adInforObj.getString("configId");
                            String adUnitID = adInforObj.getString("adUnitID");
                            // Create Host
                            JSONObject hostObj = adInforObj.getJSONObject("host");
                            String pbHost = hostObj.getString("pbHost");
                            String pbAccountId = hostObj.getString("pbAccountId");
                            String storedAuctionResponse = hostObj.getString("storedAuctionResponse");
                            HostCustom hostCustom = new HostCustom(pbHost, pbAccountId, storedAuctionResponse);

                            AdInfor adInfor = new AdInfor(isVideo, hostCustom, configId, adUnitID);
                            adInforList.add(adInfor);
                        }

                        // Validate defaultType Bid type
                        ADFormat defaultFormat = adunitJsonObj.getString("defaultType").equalsIgnoreCase(ADFormat.HTML.toString()) ? ADFormat.HTML : ADFormat.VAST;
                        if (adInforList.size() < 2) {
                            ADFormat bidType = adInforList.get(0).isVideo ? ADFormat.VAST : ADFormat.HTML;
                            defaultFormat = defaultFormat == bidType ? defaultFormat : bidType;
                        }

                        AdUnitObj adUnitObj;
                        // Set ad size if type is banner
                        if (type == ADType.Banner || type == ADType.SmartBanner) {
                            String width = adunitJsonObj.has("width") && !adunitJsonObj.getString("width").isEmpty() ? adunitJsonObj.getString("width") : "320";
                            String height = adunitJsonObj.has("height") && !adunitJsonObj.getString("height").isEmpty() ? adunitJsonObj.getString("height") : "50";
                            String size = width + "x" + height;

                            BannerSize bannerSize;
                            switch (size) {
                                case "320x50":
                                    bannerSize = BannerSize.Banner320x50;
                                    break;
                                case "320x100":
                                    bannerSize = BannerSize.Banner320x100;
                                    break;
                                case "300x250":
                                    bannerSize = BannerSize.Banner300x250;
                                    break;
                                case "468x60":
                                    bannerSize = BannerSize.Banner468x60;
                                    break;
                                case "728x90":
                                    bannerSize = BannerSize.Banner728x90;
                                    break;
                                default:
                                    bannerSize = BannerSize.SmartBanner;
                                    break;
                            }
                            adUnitObj = new AdUnitObj(placement, type, defaultFormat, isActive, adInforList, bannerSize, refreshTime);
                        } else {
                            adUnitObj = new AdUnitObj(placement, type, defaultFormat, isActive, adInforList, refreshTime);
                        }
                        listAdUnitObj.add(adUnitObj);

                        // Return result
                        resultAD.success(adUnitObj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        PBMobileAds.getInstance().log(LogType.ERROR, e.getLocalizedMessage());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        PBMobileAds.getInstance().log(LogType.ERROR, e.getLocalizedMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                        PBMobileAds.getInstance().log(LogType.ERROR, e.getLocalizedMessage());
                    }
                }

                @Override
                public void failure(Exception error) {
                    resultAD.failure(error);
                }
            });
            httpApi.execute();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // MARK: - Get Data Config
    AdUnitObj getAdUnitObj(String placement) {
        for (AdUnitObj config : this.listAdUnitObj) {
            if (config.placement.equalsIgnoreCase(placement)) {
                return config;
            }
        }
        return null;
    }

    AdInfor getAdInfor(boolean isVideo, AdUnitObj adUnitObj) {
        for (AdInfor infor : adUnitObj.adInfor) {
            if (infor.isVideo == isVideo) {
                return infor;
            }
        }
        return null;
    }

    // MARK: Setup PBS
    void setupPBS(HostCustom hostCus) {
        this.log(LogType.DEBUG, "Host: " + hostCus.pbHost + " | AccountId: " + hostCus.pbAccountId + " | storedAuctionResponse: " + hostCus.storedAuctionResponse);
        if (hostCus.pbHost.equalsIgnoreCase("Appnexus")) {
            PrebidMobile.setPrebidServerHost(Host.APPNEXUS);
        } else if (hostCus.pbHost.equalsIgnoreCase("Rubicon")) {
            PrebidMobile.setPrebidServerHost(Host.RUBICON);
        } else if (hostCus.pbHost.equalsIgnoreCase("Custom")) {
            this.log(LogType.DEBUG, "Custom URL: " + this.pbServerEndPoint);
            Host.CUSTOM.setHostUrl(this.pbServerEndPoint);
            PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        }

        PrebidMobile.setPrebidServerAccountId(hostCus.pbAccountId);
        PrebidMobile.setStoredAuctionResponse(hostCus.storedAuctionResponse);
    }

    // MARK: Show CMP + Setup GDPR
    void setGDPR() {
        String consentString = CMPStorageConsentManager.getConsentString(this.contextApp);
        if (!consentString.isEmpty()) {
            TargetingParams.setSubjectToGDPR(true);
            TargetingParams.setGDPRConsentString(consentString);
        }
    }

    void showCMP(final WorkCompleteDelegate resultWork) {
        if (this.isShowCMP) {
            resultWork.doWork();
            return;
        }

        if (this.gdprConfirm) {
            if (CMPConsentTool.needShowCMP(this.contextApp)) {
                PBMobileAds.getInstance().log(LogType.INFOR, "ConsentString Init");

                this.isShowCMP = true;
                String appName = this.contextApp.getApplicationInfo().loadLabel(this.contextApp.getPackageManager()).toString();
                CMPConfig cmpConfig = CMPConfig.createInstance(15029, "consentmanager.mgr.consensu.org", appName, "EN");
                CMPConsentTool.createInstance(contextApp, cmpConfig, new OnCloseCallback() {
                    @Override
                    public void onWebViewClosed() {
                        isShowCMP = false;
                        setGDPR();
                        resultWork.doWork();
                    }
                });
            } else {
                this.setGDPR();
                resultWork.doWork();
            }
        } else {
            resultWork.doWork();
        }
    }

    // MARK: Get Error
    String getADError(int errorCode) {
        String messErr = "";
        switch (errorCode) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                messErr = "ERROR_CODE_INTERNAL_ERROR";
                break;
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                messErr = "ERROR_CODE_INVALID_REQUEST";
                break;
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                messErr = "ERROR_CODE_NETWORK_ERROR";
                break;
            case AdRequest.ERROR_CODE_NO_FILL:
                messErr = "ERROR_CODE_NO_FILL";
                break;
        }

        return messErr;
    }

//    String getAdRewardedError(int errorCode) {
//        String messErr = "";
//        switch (errorCode) {
//            case RewardedAdCallback.ERROR_CODE_INTERNAL_ERROR:
//                messErr = "ERROR_CODE_INTERNAL_ERROR";
//                break;
//            case RewardedAdCallback.ERROR_CODE_AD_REUSED:
//                messErr = "ERROR_CODE_AD_REUSED";
//                break;
//            case RewardedAdCallback.ERROR_CODE_NOT_READY:
//                messErr = "ERROR_CODE_NOT_READY";
//                break;
//            case RewardedAdCallback.ERROR_CODE_APP_NOT_FOREGROUND:
//                messErr = "ERROR_CODE_APP_NOT_FOREGROUND";
//                break;
//        }
//
//        return messErr;
//    }

    ADType getAdType(String type) {
        switch (type) {
            case "banner":
                return ADType.Banner;
            case "smart_banner":
                return ADType.SmartBanner;
            case "interstitial":
                return ADType.Interstitial;
            case "rewarded":
                return ADType.Rewarded;
        }

        return null;
    }

    // MARK: - Public FUNC
    public void log(LogType errorType, String mess) {
        switch (errorType) {
            case DEBUG:
                if (DEBUG_MODE) Log.d("PBMobileAds", mess);
                break;
            case ERROR:
                Log.e("PBMobileAds", mess);
                break;
            case INFOR:
                Log.i("PBMobileAds", mess);
                break;
            case VERBOSE:
                Log.v("PBMobileAds", mess);
                break;
            case WARN:
                Log.w("PBMobileAds", mess);
                break;
        }
    }

    public Context getContextApp() {
        return this.contextApp;
    }

    public void enableCOPPA() {
        TargetingParams.setSubjectToCOPPA(true);
    }

    public void disableCOPPA() {
        TargetingParams.setSubjectToCOPPA(false);
    }

    public void setConsentString(String consentString) {
        if (consentString == null || consentString.isEmpty()) {
            log(LogType.ERROR, "Consent String is null or empty");
            return;
        }
        TargetingParams.setSubjectToGDPR(true);
        TargetingParams.setGDPRConsentString(consentString);
    }

    public void setPurposeConsents(String purposeConsents) {
        if (purposeConsents == null || purposeConsents.isEmpty()) {
            log(LogType.ERROR, "PurposeConsents is null or empty");
            return;
        }
        TargetingParams.setPurposeConsents(purposeConsents);
    }

    public void setCCPAString(String ccpaString) {
        if (ccpaString == null || ccpaString.isEmpty()) {
            log(LogType.ERROR, "CCPA is null or empty");
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.contextApp);
        preferences.edit().putString(UserConsentManager.US_PRIVACY_KEY, ccpaString).apply();
    }

    public void setGender(PBMobileAds.GENDER gender) {
        if (gender == GENDER.MALE) {
            TargetingParams.setGender(TargetingParams.GENDER.MALE);
        } else if (gender == GENDER.FEMALE) {
            TargetingParams.setGender(TargetingParams.GENDER.FEMALE);
        } else if (gender == GENDER.UNKNOWN) {
            TargetingParams.setGender(TargetingParams.GENDER.UNKNOWN);
        }
    }

    public void setYearOfBirth(int yearOfBirth) throws Exception {
        TargetingParams.setYearOfBirth(yearOfBirth);
    }

    public enum GENDER {
        FEMALE,
        MALE,
        UNKNOWN
    }

}



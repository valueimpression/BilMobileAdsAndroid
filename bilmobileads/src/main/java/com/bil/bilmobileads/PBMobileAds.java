
package com.bil.bilmobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.WebView;

import com.bil.bilmobileads.entity.ADFormat;
import com.bil.bilmobileads.entity.ADType;
import com.bil.bilmobileads.entity.AdInfor;
import com.bil.bilmobileads.entity.AdUnitObj;
import com.bil.bilmobileads.entity.HostCustom;
//import com.bil.bilmobileads.entity.TimerRecall;
import com.bil.bilmobileads.interfaces.ResultCallback;
//import com.bil.bilmobileads.interfaces.TimerCompleteListener;
import com.consentmanager.sdk.CMPConsentTool;
import com.google.android.gms.ads.AdSize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.prebid.mobile.Host;
import org.prebid.mobile.PrebidMobile;
import org.prebid.mobile.TargetingParams;

import java.util.ArrayList;

public class PBMobileAds {

    private static final PBMobileAds instance = new PBMobileAds();

    // MARK: Context app
    private Context contextApp;

    // MARK: List Config
    ArrayList<AdUnitObj> listAdUnitObj = new ArrayList<AdUnitObj>();

    // MARK: List AD
    ArrayList<ADBanner> listADBanner = new ArrayList<ADBanner>();
    ArrayList<ADInterstitial> listADIntersititial = new ArrayList<ADInterstitial>();
    ArrayList<ADRewarded> listADRewarded = new ArrayList<ADRewarded>();

    // MARK: api
    boolean isTestMode = false;
    boolean gdprConfirm = false;
    private String pbServerEndPoint = "";

    private PBMobileAds() {
        this.log("PBMobileAds Init");
    }

    public static PBMobileAds getInstance() {
        return instance;
    }

    // MARK: - initialize
    public void initialize(Context context, boolean testMode) {
        this.contextApp = context;
        this.isTestMode = testMode;

        // Declare in init to the user agent could be passed in first call
        PrebidMobile.setShareGeoLocation(true);
        PrebidMobile.setApplicationContext(context);
        WebView obj = new WebView(context);
        obj.clearCache(true);
    }

    public Context getContextApp() {
        return this.contextApp;
    }

    // MARK: - Call API AD
    void getADConfig(final String adUnit, final ResultCallback resultAD) {
        this.log("Start Request Config adUnit: " + adUnit);

        //        final TimerRecall timerRecall = new TimerRecall(Constants.RECALL_CONFIGID_SERVER, 1000);
        //        timerRecall.setListener(new TimerCompleteListener() {
        //            @Override
        //            public void doWork() {
        //                getADConfig(adUnit, resultAD);
        //            }
        //        });

        HttpApi httpApi = new HttpApi<JSONObject>(Constants.GET_DATA_CONFIG + adUnit, new ResultCallback<JSONObject, Exception>() {
            @Override
            public void success(JSONObject dataJSON) {
                //  timerRecall.cancel();
                try {
                    pbServerEndPoint = dataJSON.getString("pbServerEndPoint");
                    gdprConfirm = dataJSON.getBoolean("gdprConfirm");

                    // Set all ad type config
                    JSONObject adunitJsonObj = dataJSON.getJSONObject("adunit");

                    String placement = adunitJsonObj.getString("placement");
                    String type = adunitJsonObj.getString("type");
                    ADFormat defaultType = adunitJsonObj.getString("defaultType").equalsIgnoreCase(ADFormat.HTML.toString()) ? ADFormat.HTML : ADFormat.VAST;
                    boolean isActive = adunitJsonObj.getBoolean("isActive");

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

                    AdUnitObj adUnitObj;
                    // Set ad size if type is banner
                    if (type.equalsIgnoreCase(String.valueOf(ADType.Banner))) {
                        String width = adunitJsonObj.has("width") && !adunitJsonObj.getString("width").isEmpty() ? adunitJsonObj.getString("width") : "320";
                        String height = adunitJsonObj.has("height") && !adunitJsonObj.getString("height").isEmpty() ? adunitJsonObj.getString("height") : "50";
                        String bannerSize = width + "x" + height;

                        AdSize adSize;
                        switch (bannerSize) {
                            case "320x50":
                                adSize = new AdSize(320, 50);
                                break;
                            case "320x100":
                                adSize = new AdSize(320, 100);
                                break;
                            case "300x250":
                                adSize = new AdSize(300, 250);
                                break;
                            case "468x60":
                                adSize = new AdSize(468, 60);
                                break;
                            case "728x90":
                                adSize = new AdSize(728, 90);
                                break;
                            default:
                                DisplayMetrics displayMetrics = new DisplayMetrics();
                                ((Activity) getContextApp()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                                float heightDP = displayMetrics.heightPixels / displayMetrics.density;
                                if (heightDP <= 720) {
                                    adSize = new AdSize(320, 50);
                                } else {
                                    adSize = new AdSize(728, 90);
                                }
                                break;
                        }

                        adUnitObj = new AdUnitObj(placement, type, defaultType, isActive, adInforList, adSize);
                    } else {
                        adUnitObj = new AdUnitObj(placement, type, defaultType, isActive, adInforList);
                    }

                    listAdUnitObj.add(adUnitObj);

                    // Return result
                    resultAD.success(adUnitObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                    PBMobileAds.getInstance().log(e.getLocalizedMessage());
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    PBMobileAds.getInstance().log(e.getLocalizedMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    PBMobileAds.getInstance().log(e.getLocalizedMessage());
                }
            }

            @Override
            public void failure(Exception error) {
                resultAD.failure(error);
            }
        });
        httpApi.execute();
    }

    // MARK: - Call CMP / GDPR
    private void initGDPR() {
        Context contextApp = this.getContextApp();
        String appName = contextApp.getApplicationInfo().loadLabel(contextApp.getPackageManager()).toString();
        CMPConsentTool.createInstance(contextApp, 14327, "consentmanager.mgr.consensu.org", appName, "");
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

    // MARK: Setup PBS
    public void setupPBS(HostCustom hostCus) {
        this.log("Host: " + hostCus.pbHost + " | AccountId: " + hostCus.pbAccountId + " | storedAuctionResponse: " + hostCus.storedAuctionResponse);
        if (hostCus.pbHost.equalsIgnoreCase("Appnexus")) {
            PrebidMobile.setPrebidServerHost(Host.APPNEXUS);
        } else if (hostCus.pbHost.equalsIgnoreCase("Rubicon")) {
            PrebidMobile.setPrebidServerHost(Host.RUBICON);
        } else if (hostCus.pbHost.equalsIgnoreCase("Custom")) {
            this.log("Custom URL: " + this.pbServerEndPoint);
            Host.CUSTOM.setHostUrl(this.pbServerEndPoint);
            PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        }

        PrebidMobile.setPrebidServerAccountId(hostCus.pbAccountId);
        PrebidMobile.setStoredAuctionResponse(hostCus.storedAuctionResponse);
    }

    public void enableCOPPA() {
        TargetingParams.setSubjectToCOPPA(true);
    }

    public void disableCOPPA() {
        TargetingParams.setSubjectToCOPPA(false);
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

    public boolean log(String object) {
        Log.d("PBMobileAds", object);
        return false;
    }

    public static enum GENDER {
        FEMALE,
        MALE,
        UNKNOWN;

        private GENDER() {
        }
    }
}



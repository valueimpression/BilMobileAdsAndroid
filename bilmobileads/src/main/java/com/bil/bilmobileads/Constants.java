package com.bil.bilmobileads;

import org.prebid.mobile.Host;

public final class Constants {

    // MARK: - BANNER + INTERSTITIAL
    // AD Simple Test
    public static final Host PB_SERVER_HOST = Host.APPNEXUS;
    public static final String PB_SERVER_ACCOUNT_ID = "bfa84af2-bd16-4d35-96ad-31c6bb888df0";
    public static final String ADUNIT_CONFIG_ID = "625c6125-f19e-4d5b-95c5-55501526b2a4";
    // AD Video Test
    public static final Host V_PB_SERVER_HOST = Host.RUBICON;
    public static final String V_PB_SERVER_ACCOUNT_ID = "1001";
    public static final String V_ADUNIT_CONFIG_ID = "1001-1";

    public static final String PB_SERVER_CUSTOM = "https://pb-server.vliplatform.com/openrtb2/auction"; // "http://localhost:8000/openrtb2/auction" //

    // MARK: - Properties
    public static final String DEVICE_ID_TEST = "B3EEABB8EE11C2BE770B684D95219ECB";
    public static final long RECALL_CONFIGID_SERVER = 10000;
    public static final int BANNER_AUTO_REFRESH_DEFAULT = 30000; // MilliSec
    public static final double BANNER_RECALL_DEFAULT = 10000; // Sec
    public static final double INTERSTITIAL_RECALL_DEFAULT = 10000; // Sec

    // MARK: - URL Prefix
    public static final String URL_PREFIX = "https://app-services.vliplatform.com"; // "http://10.0.2.2:8000"; //
    // API
    public static final String GET_DATA_CONFIG = "/getAdunitConfig?adUnitId=";

    private Constants() {

    }
}

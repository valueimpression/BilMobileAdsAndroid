package com.bil.bilmobileads;

public final class Constants {

    // MARK: - Properties
    public static final String DEVICE_ID_TEST = "B3EEABB8EE11C2BE770B684D95219ECB";
    public static final int TIME_REQUEST_ADUNIT = 30000;

    // ADType:
    public static final String BANNER = "banner";
    public static final String SMART_BANNER = "smart_banner";
    public static final String INTERSTITIAL = "interstitial";
    public static final String REWARDED = "rewarded";

    // MARK: - URL Prefix
    public static final String URL_PREFIX = "https://app-services.vliplatform.com"; // "http://10.0.2.2:8000";
    // API
    public static final String GET_DATA_CONFIG = "/getAdunitConfig?adUnitId=";

    private Constants() {
    }
}

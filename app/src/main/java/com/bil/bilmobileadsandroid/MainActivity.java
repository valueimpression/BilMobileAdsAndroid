package com.bil.bilmobileadsandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bil.bilmobileads.ADAppOpen;
import com.bil.bilmobileads.ADBanner;
import com.bil.bilmobileads.ADInterstitial;
import com.bil.bilmobileads.ADNativeCustom;
import com.bil.bilmobileads.ADNativeStyle;
import com.bil.bilmobileads.ADNativeView;
import com.bil.bilmobileads.ADRewarded;
import com.bil.bilmobileads.PBMobileAds;
import com.bil.bilmobileads.entity.AdData;
import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.bil.bilmobileads.interfaces.AdRewardedDelegate;
import com.bil.bilmobileads.interfaces.NativeAdCustomDelegate;
import com.bil.bilmobileads.interfaces.NativeAdVideoDelegate;
import com.bil.bilmobileads.interfaces.NativeAdLoaderCustomDelegate;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Activity curActivity;

    ADBanner adBanner;
    ADNativeStyle adNativeStyle;
    ADNativeCustom adNativeCustom;
    ADInterstitial adInterstitial;
    ADRewarded adRewarded;

    ADAppOpen adAppOpen;

    ArrayList<ADNativeView.Builder> builderArrayList = new ArrayList<>();
    ADNativeView.Builder builderView;

    boolean isShow = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PBMobileAds.getInstance().initialize(this, true);

        final FrameLayout bannerView = findViewById(R.id.bannerView);
        final FrameLayout bannerView2 = findViewById(R.id.bannerView2);

//        this.curActivity = this;
//        this.adAppOpen = new ADAppOpen(this, "1005");

        this.adBanner = new ADBanner(this, bannerView, "1001");
        this.adBanner.setListener(new AdDelegate() {
            @Override
            public void onAdReturn(ViewGroup viewGroup) {
                super.onAdReturn(viewGroup);
                if (isShow) {
                    adBanner.setViewGroup(bannerView);
                    bannerView.addView(viewGroup);
                } else {
                    adBanner.setViewGroup(bannerView2);
                    bannerView2.addView(viewGroup);
                }
                isShow = !isShow;
            }
        });

//        this.adInterstitial = new ADInterstitial(this,"1002");

//        this.adRewarded = new ADRewarded(this, "1003");

//        this.adNativeStyle = new ADNativeStyle(bannerView, "1004");

//        this.adNativeCustom = new ADNativeCustom("1004");
//        this.adNativeCustom.setListener(new NativeAdLoaderCustomDelegate() {
//            @Override
//            public void onNativeViewLoaded(ADNativeView.Builder builder) {
//                super.onNativeViewLoaded(builder);
//                builderArrayList.add(builder);
//            }
//
//            @Override
//            public void onNativeFailedToLoad(String error) {
//                super.onNativeFailedToLoad(error);
//                PBMobileAds.getInstance().log(LogType.INFOR, error);
//            }
//        });

        Button btnLoadFull = (Button) findViewById(R.id.loadFull);
        btnLoadFull.setOnClickListener(view -> {
//                adNativeCustom.preload();
            if (adInterstitial != null) adInterstitial.preLoad();
            if (adRewarded != null) adRewarded.preLoad();
            if (adAppOpen != null) adAppOpen.preLoad(curActivity);
            if (adBanner != null) adBanner.destroy();
        });

        Button btnShowFull = (Button) findViewById(R.id.showFull);
        btnShowFull.setOnClickListener(view -> {
            if (adInterstitial != null) adInterstitial.show();
            if (adRewarded != null) adRewarded.show();
            if (adAppOpen != null) adAppOpen.show(curActivity);
            if (adBanner != null) adBanner.load();

//                if (builderArrayList.size() <= 0) {
//                    PBMobileAds.getInstance().log(LogType.INFOR, "Native unavailable, preload before call show ads");
//                    return;
//                }
//
//                if(builderView != null) builderView.destroy();
//
//                ADNativeView.Builder builder = builderArrayList.get(builderArrayList.size() - 1);
//                builderView = builder;
//                builderArrayList.remove(builder);
//
//                // Get View and setup content NativeAD
//                View nativeView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.native_ad_view, null);
//                builder.setNativeView(nativeView)
//                        .setHeadlineView((TextView) nativeView.findViewById(R.id.ad_headline))
//                        .setBodyView((TextView) nativeView.findViewById(R.id.ad_body))
//                        .setCallToActionView((Button) nativeView.findViewById(R.id.ad_call_to_action))
//                        .setIconView((ImageView) nativeView.findViewById(R.id.ad_app_icon))
//                        .setPriceView((TextView) nativeView.findViewById(R.id.ad_price))
//                        .setStarRatingView((RatingBar) nativeView.findViewById(R.id.ad_stars))
//                        .setStoreView((TextView) nativeView.findViewById(R.id.ad_store))
//                        .setAdvertiserView((TextView) nativeView.findViewById(R.id.ad_advertiser))
//                        .setMediaView((FrameLayout) nativeView.findViewById(R.id.ad_media_android))
//                        .build();
//                builder.setNativeAdDelegate(new NativeAdCustomDelegate() {
//                    @Override
//                    public void onNativeAdDidRecordImpression(String data) {
//                        super.onNativeAdDidRecordImpression(data);
//                        PBMobileAds.getInstance().log(LogType.INFOR, "onNativeAdDidRecordImpression: " + data);
//                    }
//
//                    @Override
//                    public void onNativeAdDidRecordClick(String data) {
//                        super.onNativeAdDidRecordClick(data);
//                        PBMobileAds.getInstance().log(LogType.INFOR, "onNativeAdDidRecordClick: " + data);
//                    }
//
//                    @Override
//                    public void onNativeAdDidExpire(String data) {
//                        super.onNativeAdDidExpire(data);
//                        PBMobileAds.getInstance().log(LogType.INFOR, "onNativeAdDidExpire: " + data);
//                    }
//                });
//                builder.setVideoListener(new NativeAdVideoDelegate() {
//                    @Override
//                    public void onVideoEnd() {
//                        super.onVideoEnd();
//                        PBMobileAds.getInstance().log(LogType.INFOR, "onVideoEnd");
//                    }
//                });
//
//                bannerView.removeAllViews();
//                bannerView.addView(builder.getNativeView());
        });

    }
}

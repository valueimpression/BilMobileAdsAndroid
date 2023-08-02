package com.bil.bilmobileadsandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bil.bilmobileads.ADBanner;
import com.bil.bilmobileads.ADInterstitial;
import com.bil.bilmobileads.ADNativeCustom;
import com.bil.bilmobileads.ADNativeStyle;
import com.bil.bilmobileads.ADNativeView;
import com.bil.bilmobileads.ADRewarded;
import com.bil.bilmobileads.PBMobileAds;
import com.bil.bilmobileads.entity.LogType;
import com.bil.bilmobileads.interfaces.NativeAdCustomDelegate;
import com.bil.bilmobileads.interfaces.NativeAdVideoDelegate;
import com.bil.bilmobileads.interfaces.NativeAdLoaderCustomDelegate;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ADBanner adBanner;
    ADNativeStyle adNativeStyle;
    ADNativeCustom adNativeCustom;
    ADInterstitial adInterstitial;
    ADRewarded adRewarded;

    ADBanner adBanner1;

    ArrayList<ADNativeView.Builder> builderArrayList = new ArrayList<>();
    ADNativeView.Builder builderView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PBMobileAds.getInstance().initialize(this, true);

        final FrameLayout bannerView = findViewById(R.id.bannerView);

        this.adBanner = new ADBanner(bannerView, "1001");
        this.adBanner.load();

        this.adInterstitial = new ADInterstitial("1002");
        this.adInterstitial.preLoad();

//        this.adRewarded = new ADRewarded(this, "1003");

//        this.adNativeStyle = new ADNativeStyle(bannerView, "1004");
//        this.adNativeStyle.load();

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
        btnLoadFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                adNativeCustom.preload();
                if (adInterstitial != null) adInterstitial.preLoad();
                if (adRewarded != null) adRewarded.preLoad();
//                if (adBanner != null)  adBanner.destroy();
            }
        });

        Button btnShowFull = (Button) findViewById(R.id.showFull);
        btnShowFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adInterstitial != null) adInterstitial.show();
                if (adRewarded != null) adRewarded.show();
//                if (adBanner != null) adBanner.load();

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
            }
        });

    }
}

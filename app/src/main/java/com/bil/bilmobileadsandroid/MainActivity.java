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
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.bil.bilmobileads.interfaces.AdNativeDelegate;
import com.bil.bilmobileads.interfaces.AdNativeVideoDelegate;
import com.bil.bilmobileads.interfaces.AdRewardedDelegate;
import com.bil.bilmobileads.unity.UADBanner;
import com.bil.bilmobileads.unity.Utils;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ADBanner adBanner;
    ADNativeStyle adNativeStyle;
    ADNativeCustom adNativeCustom;
    ADInterstitial adInterstitial;
    ADRewarded adRewarded;

    ADBanner adBanner1;

    ArrayList<ADNativeView.Builder> builderArrayList = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PBMobileAds.getInstance().initialize(this, true);

        final FrameLayout bannerView = findViewById(R.id.bannerView);

        this.adBanner = new ADBanner(bannerView, "13b7495e-1e87-414a-afcd-ef8a9034bd22");

//        UADBanner uadBanner = new UADBanner(this, null);
//        uadBanner.create("13b7e996-4aa3-40f5-b33e-54914d04bdcb", Utils.TOP_CENTER);
//        13b7e996-4aa3-40f5-b33e-54914d04bdcb
//        13b7495e-1e87-414a-afcd-ef8a9034bd22 -> smart

//        this.adInterstitial = new ADInterstitial("3bad632c-26f8-4137-ae27-05325ee1b30c");

//        this.adRewarded = new ADRewarded(this, "d4aa579a-1655-452b-9502-b16ed31d2a99");

//        this.adNativeStyle = new ADNativeStyle(bannerView, "b99a80a3-7a4d-4f32-bb70-0039fdb4fca3");

//        this.adNativeCustom = new ADNativeCustom("b99a80a3-7a4d-4f32-bb70-0039fdb4fca3");
//        this.adNativeCustom.setListener(new AdNativeDelegate() {
//            @Override
//            public void onNativeViewLoaded(ADNativeView.Builder builder) {
//                super.onNativeViewLoaded(builder);
//
//                // Preload native ads (Max 5 request)
//                builderArrayList.add(builder);
//                PBMobileAds.getInstance().log("Total current Ads stored: " + adNativeCustom.numOfAds());
//                // Preload 2 native ads
//                if (adNativeCustom.numOfAds() < 2) {
//                    adNativeCustom.load();
//                }
//            }
//        });

        Button btnLoadFull = (Button) findViewById(R.id.loadFull);
        btnLoadFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                adNativeCustom.load();
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
                if (adBanner != null)  adBanner.load();

//                if (builderArrayList.size() <= 0) {
//                    PBMobileAds.getInstance().log("Native unavailable, load ad before show");
//                    return;
//                }
//
//                ADNativeView.Builder builder = builderArrayList.get(builderArrayList.size() - 1);
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
//                builder.setVideoListener(new AdNativeVideoDelegate() {
//                    @Override
//                    public void onVideoEnd() {
//                        super.onVideoEnd();
//                        PBMobileAds.getInstance().log("onVideoEnd");
//                    }
//                });
//                bannerView.removeAllViews();
//                bannerView.addView(builder.getNativeView());
            }
        });

    }
}

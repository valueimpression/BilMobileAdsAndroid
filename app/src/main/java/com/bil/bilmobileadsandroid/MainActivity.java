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

public class MainActivity extends AppCompatActivity {

    ADBanner adBanner;
    ADNativeStyle adNativeStyle;
    ADNativeCustom adNativeCustom;
    ADInterstitial adInterstitial;
    ADRewarded adRewarded;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PBMobileAds.getInstance().initialize(this, true);

        final FrameLayout bannerView = findViewById(R.id.bannerView);

//        this.adBanner = new ADBanner(bannerView, "1001");

//        this.adNativeStyle = new ADNativeStyle(bannerView, "1004");

        this.adNativeCustom = new ADNativeCustom(bannerView, "1004");
        this.adNativeCustom.setListener(new AdNativeDelegate() {
            @Override
            public void onNativeViewLoaded(ADNativeView.Builder builder) {
                super.onNativeViewLoaded(builder);

                // Get View and setup content NativeAD
                View nativeView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.native_ad_view, null);
                builder.setNativeView(nativeView)
                        .setHeadlineView((TextView) nativeView.findViewById(R.id.ad_headline))
                        .setBodyView((TextView) nativeView.findViewById(R.id.ad_body))
                        .setCallToActionView((Button) nativeView.findViewById(R.id.ad_call_to_action))
                        .setIconView((ImageView) nativeView.findViewById(R.id.ad_app_icon))
                        .setPriceView((TextView) nativeView.findViewById(R.id.ad_price))
                        .setStarRatingView((RatingBar) nativeView.findViewById(R.id.ad_stars))
                        .setStoreView((TextView) nativeView.findViewById(R.id.ad_store))
                        .setAdvertiserView((TextView) nativeView.findViewById(R.id.ad_advertiser))
                        .setMediaView((FrameLayout) nativeView.findViewById(R.id.ad_media_android))
                        .build();
                builder.setVideoListener(new AdNativeVideoDelegate() {
                    @Override
                    public void onVideoEnd() {
                        super.onVideoEnd();

                        PBMobileAds.getInstance().log("onVideoEnd");
                    }
                });
            }
        });

//        this.adInterstitial = new ADInterstitial("1002");

//        this.adRewarded = new ADRewarded(this, "1003");

        Button btnShowFull = (Button) findViewById(R.id.showFull);
        btnShowFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adInterstitial != null) {
                    adInterstitial.load();
                }

                if (adRewarded != null) {
                    adRewarded.load();
                }
            }
        });

    }
}

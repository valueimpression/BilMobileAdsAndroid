package com.bil.bilmobileadsandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.bil.bilmobileads.ADBanner;
import com.bil.bilmobileads.ADInterstitial;
import com.bil.bilmobileads.ADRewarded;
import com.bil.bilmobileads.PBMobileAds;
import com.bil.bilmobileads.entity.BannerSize;
import com.bil.bilmobileads.interfaces.AdDelegate;
import com.bil.bilmobileads.interfaces.AdRewardedDelegate;

public class MainActivity extends AppCompatActivity {

    ADBanner adBanner;
    ADInterstitial adInterstitial;
    ADRewarded adRewarded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PBMobileAds.getInstance().initialize(this, true);

        FrameLayout bannerView = findViewById(R.id.bannerView);
        //  init banner  | ba87c06b-8fdf-4f19-9c63-4777e8957295
        this.adBanner = new ADBanner(bannerView, "1001");
        this.adBanner.setAdSize(BannerSize.Banner300x250);
        this.adBanner.setAutoRefreshMillis(30000);
        this.adBanner.load();

//        this.adInterstitial = new ADInterstitial("1002");
//        this.adInterstitial.preLoad();

//        this.adRewarded = new ADRewarded(this, "1003");
//        this.adRewarded.preLoad();

        Button btnShowFull = (Button) findViewById(R.id.showFull);
        btnShowFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                adInterstitial.load();

                adRewarded.load();
            }
        });

    }
}

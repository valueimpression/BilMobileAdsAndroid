package com.bil.bilmobileads;

import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bil.bilmobileads.interfaces.AdNativeVideoDelegate;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;

public final class ADNativeViewBuilder {

    private UnifiedNativeAd unifiedNativeAd;

    ADNativeViewBuilder(UnifiedNativeAd unifiedNativeAd) {
        this.unifiedNativeAd = unifiedNativeAd;
    }

    public static class Builder {

        private UnifiedNativeAd nativeAd;
        private UnifiedNativeAdView nativeAdView;

        public Builder(UnifiedNativeAd nativeAd) {
            this.nativeAd = nativeAd;
            this.nativeAdView = new UnifiedNativeAdView(PBMobileAds.getInstance().getContextApp());
        }

        public ADNativeViewBuilder.Builder setNativeView(View view) {
            this.nativeAdView.removeAllViews();
            this.nativeAdView.addView(view);

            return this;
        }

        public ADNativeViewBuilder.Builder setMediaView(FrameLayout frameLayout) {
            MediaView mediaView = new MediaView(PBMobileAds.getInstance().getContextApp());
            mediaView.setMediaContent(this.nativeAd.getMediaContent());

            frameLayout.addView(mediaView);

            this.nativeAdView.setMediaView(mediaView);

            return this;
        }

        public ADNativeViewBuilder.Builder setHeadlineView(TextView headLine) {
            if (this.nativeAd.getHeadline() == null) {
                headLine.setVisibility(View.INVISIBLE);
            } else {
                headLine.setVisibility(View.VISIBLE);
                headLine.setText(this.nativeAd.getHeadline());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setBodyView(TextView txtBody) {
            if (this.nativeAd.getBody() == null) {
                txtBody.setVisibility(View.INVISIBLE);
            } else {
                txtBody.setVisibility(View.VISIBLE);
                txtBody.setText(this.nativeAd.getBody());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setCallToActionView(Button btnAction) {
            if (this.nativeAd.getCallToAction() == null) {
                btnAction.setVisibility(View.INVISIBLE);
            } else {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText(this.nativeAd.getCallToAction());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setIconView(ImageView imgIcon) {
            if (this.nativeAd.getIcon() == null) {
                imgIcon.setVisibility(View.GONE);
            } else {
                imgIcon.setVisibility(View.VISIBLE);
                imgIcon.setImageDrawable(this.nativeAd.getIcon().getDrawable());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setPriceView(TextView txtprice) {
            if (this.nativeAd.getPrice() == null) {
                txtprice.setVisibility(View.INVISIBLE);
            } else {
                txtprice.setVisibility(View.VISIBLE);
                txtprice.setText(this.nativeAd.getPrice());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setStarRatingView(RatingBar rateBar) {
            if (this.nativeAd.getStarRating() == null) {
                rateBar.setVisibility(View.INVISIBLE);
            } else {
                rateBar.setVisibility(View.VISIBLE);
                rateBar.setRating(this.nativeAd.getStarRating().floatValue());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setStoreView(TextView txtStore) {
            if (this.nativeAd.getStore() == null) {
                txtStore.setVisibility(View.INVISIBLE);
            } else {
                txtStore.setVisibility(View.VISIBLE);
                txtStore.setText(this.nativeAd.getStore());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setAdvertiserView(TextView txtAdvertiser) {
            if (this.nativeAd.getAdvertiser() == null) {
                txtAdvertiser.setVisibility(View.INVISIBLE);
            } else {
                txtAdvertiser.setVisibility(View.VISIBLE);
                txtAdvertiser.setText(this.nativeAd.getAdvertiser());
            }
            return this;
        }

        public ADNativeViewBuilder.Builder setVideoListener(final AdNativeVideoDelegate videoDelegate) {
            VideoController vc = this.nativeAd.getVideoController();

            // Updates the UI to say whether or not this ad has a video asset.
            if (vc.hasVideoContent()) {
                // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
                // VideoController will call methods on this object when events occur in the video lifecycle.
                vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                    @Override
                    public void onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.

                        if (videoDelegate != null) videoDelegate.onVideoEnd();
                        super.onVideoEnd();
                    }

                    @Override
                    public void onVideoStart() {
                        if (videoDelegate != null) videoDelegate.onVideoStart();
                        super.onVideoStart();
                    }

                    @Override
                    public void onVideoPlay() {
                        if (videoDelegate != null) videoDelegate.onVideoPlay();
                        super.onVideoPlay();
                    }

                    @Override
                    public void onVideoPause() {
                        if (videoDelegate != null) videoDelegate.onVideoPause();
                        super.onVideoPause();
                    }

                    @Override
                    public void onVideoMute(boolean b) {
                        if (videoDelegate != null) videoDelegate.onVideoMute(b);
                        super.onVideoMute(b);
                    }
                });
            }

            return this;
        }

        public FrameLayout getNativeView() {
            return this.nativeAdView;
        }

        public FrameLayout build() {
            this.nativeAdView.setNativeAd(this.nativeAd);
            return this.nativeAdView;
        }

        public void destroy() {
            if (this.nativeAd != null) {
                this.nativeAd.destroy();
            }
            if (this.nativeAdView != null) {
                this.nativeAdView.removeAllViews();
                this.nativeAdView.destroy();
                this.nativeAdView = null;
            }
        }
    }
}

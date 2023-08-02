package com.bil.bilmobileads;

import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bil.bilmobileads.interfaces.NativeAdVideoDelegate;
import com.bil.bilmobileads.interfaces.NativeAdCustomDelegate;
import com.google.android.gms.ads.VideoController;
//import com.google.android.gms.ads.formats.MediaView;
//import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
//import com.google.android.gms.ads.formats.UnifiedNativeAdView;

import org.prebid.mobile.PrebidNativeAd;
import org.prebid.mobile.PrebidNativeAdEventListener;
import org.prebid.mobile.Util;

public final class ADNativeView {

    ADNativeView() {
    }

    public static class Builder {

        private String placement;
        private View viewTemplate;

        private PrebidNativeAd nativeAd;
        private NativeAdCustomDelegate nativeAdDelegate;

        private NativeAd gadNativeAd;
        private NativeAdView nativeAdView;

        public Builder(String placement, PrebidNativeAd nativeAd) {
            this.placement = placement;
            this.nativeAd = nativeAd;
            this.nativeAdView = new NativeAdView(PBMobileAds.getInstance().getContextApp());
        }

        public Builder(String placement, NativeAd gadNativeAd) {
            this.placement = placement;
            this.gadNativeAd = gadNativeAd;
            this.nativeAdView = new NativeAdView(PBMobileAds.getInstance().getContextApp());
        }

        public ADNativeView.Builder setNativeView(View view) {
            this.viewTemplate = view;

            TextView txtAd = new TextView(view.getContext());
            txtAd.setText("Ad");
            txtAd.setTextColor(Color.WHITE);
            txtAd.setPadding(0, 0, 5, 0);
            txtAd.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            txtAd.setBackgroundColor(Color.parseColor("#FFCD68"));
            txtAd.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

            this.nativeAdView.addView(view);
            this.nativeAdView.addView(txtAd);
            return this;
        }

        public ADNativeView.Builder setHeadlineView(TextView headLine) {
            String headLineStr = this.isGAD() ? this.gadNativeAd.getHeadline() : this.nativeAd.getTitle();
            if (headLineStr == null) {
                headLine.setVisibility(View.INVISIBLE);
            } else {
                headLine.setVisibility(View.VISIBLE);
                headLine.setText(headLineStr);
            }
            return this;
        }

        public ADNativeView.Builder setBodyView(TextView txtBody) {
            String txtBodyStr = this.isGAD() ? this.gadNativeAd.getBody() : this.nativeAd.getDescription();
            if (txtBodyStr == null) {
                txtBody.setVisibility(View.INVISIBLE);
            } else {
                txtBody.setVisibility(View.VISIBLE);
                txtBody.setText(txtBodyStr);
            }
            return this;
        }

        public ADNativeView.Builder setCallToActionView(Button btnAction) {
            String btnActionStr = this.isGAD() ? this.gadNativeAd.getCallToAction() : this.nativeAd.getCallToAction();
            if (btnActionStr == null) {
                btnAction.setVisibility(View.INVISIBLE);
            } else {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText(btnActionStr);
            }
            return this;
        }

        public ADNativeView.Builder setIconView(ImageView imgIcon) {
            if (this.isGAD()) {
                if (this.gadNativeAd.getIcon() == null) {
                    imgIcon.setVisibility(View.GONE);
                } else {
                    imgIcon.setVisibility(View.VISIBLE);
                    imgIcon.setImageDrawable(this.gadNativeAd.getIcon().getDrawable());
                }
            } else {
                if (this.nativeAd.getIconUrl() == null) {
                    imgIcon.setVisibility(View.GONE);
                } else {
                    imgIcon.setVisibility(View.VISIBLE);
                    imgIcon.setImageURI(Uri.parse(this.nativeAd.getIconUrl()));
//                    Util.loadImage(imgIcon, this.nativeAd.getIconUrl());
                }
            }
            return this;
        }

        public ADNativeView.Builder setMediaView(FrameLayout frameLayout) {
            if (this.isGAD()) {
                MediaView mediaView = new MediaView(PBMobileAds.getInstance().getContextApp());
                mediaView.setMediaContent(this.gadNativeAd.getMediaContent());
                frameLayout.addView(mediaView);
                this.nativeAdView.setMediaView(mediaView);
            } else {
                ImageView imageView = new ImageView(PBMobileAds.getInstance().getContextApp());
                imageView.setImageURI(Uri.parse(this.nativeAd.getImageUrl()));
//                Util.loadImage(imageView, this.nativeAd.getImageUrl());
                frameLayout.addView(imageView);
            }
            return this;
        }

        public ADNativeView.Builder setPriceView(TextView txtPrice) {
            String txtPriceStr = this.isGAD() ? this.gadNativeAd.getPrice() : null;
            if (txtPriceStr == null) {
                txtPrice.setVisibility(View.INVISIBLE);
            } else {
                txtPrice.setVisibility(View.VISIBLE);
                txtPrice.setText(txtPriceStr);
            }
            return this;
        }

        public ADNativeView.Builder setStarRatingView(RatingBar rateBar) {
            Double rateBarStr = this.isGAD() ? this.gadNativeAd.getStarRating() : null;
            if (rateBarStr == null) {
                rateBar.setVisibility(View.INVISIBLE);
            } else {
                rateBar.setVisibility(View.VISIBLE);
                rateBar.setRating(rateBarStr.floatValue());
            }
            return this;
        }

        public ADNativeView.Builder setStoreView(TextView txtStore) {
            String txtStoreStr = this.isGAD() ? this.gadNativeAd.getStore() : null;
            if (txtStoreStr == null) {
                txtStore.setVisibility(View.INVISIBLE);
            } else {
                txtStore.setVisibility(View.VISIBLE);
                txtStore.setText(txtStoreStr);
            }
            return this;
        }

        public ADNativeView.Builder setAdvertiserView(TextView txtAdvertiser) {
            String txtAdvertiserStr = this.isGAD() ? this.gadNativeAd.getAdvertiser() : this.nativeAd.getSponsoredBy();
            if (txtAdvertiserStr == null) {
                txtAdvertiser.setVisibility(View.INVISIBLE);
            } else {
                txtAdvertiser.setVisibility(View.VISIBLE);
                txtAdvertiser.setText(txtAdvertiserStr);
            }
            return this;
        }

        public ADNativeView.Builder setNativeAdDelegate(final NativeAdCustomDelegate nativeAdDelegate) {
            this.nativeAdDelegate = nativeAdDelegate;
            return this;
        }

        public ADNativeView.Builder setVideoListener(final NativeAdVideoDelegate videoDelegate) {
            if (this.gadNativeAd == null) return this;

            VideoController vc = this.gadNativeAd.getMediaContent().getVideoController();

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
            if (this.isGAD())
                this.nativeAdView.setNativeAd(this.gadNativeAd);
            else
                this.nativeAd.registerView(this.viewTemplate, new PrebidNativeAdEventListener() {
                    @Override
                    public void onAdExpired() {
                        if (nativeAdDelegate != null)
                            nativeAdDelegate.onNativeAdDidExpire("ADNativeCustom Placement " + placement + " did record Expire");
                    }

                    @Override
                    public void onAdClicked() {
                        if (nativeAdDelegate != null)
                            nativeAdDelegate.onNativeAdDidRecordClick("ADNativeCustom Placement " + placement + " did record Click");
                    }

                    @Override
                    public void onAdImpression() {
                        if (nativeAdDelegate != null)
                            nativeAdDelegate.onNativeAdDidRecordImpression("ADNativeCustom Placement " + placement + " did record Impression");
                    }
                });

            return this.nativeAdView;
        }

        private boolean isGAD() {
            return this.gadNativeAd != null;
        }

        public void destroy() {
            this.placement = null;
            if (this.nativeAd != null) {
                this.nativeAd = null;
            }
            if (this.gadNativeAd != null) {
                this.gadNativeAd.destroy();
                this.gadNativeAd = null;
            }
            if (this.nativeAdView != null) {
                this.nativeAdView.removeAllViews();
                this.nativeAdView.destroy();
                this.nativeAdView = null;
            }
            if (this.viewTemplate != null) this.viewTemplate = null;
        }
    }
}

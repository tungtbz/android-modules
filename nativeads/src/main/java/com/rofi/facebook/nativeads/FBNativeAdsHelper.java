package com.rofi.facebook.nativeads;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeAdView;

public class FBNativeAdsHelper implements NativeAdListener {
    private String placementId;
    private Activity _activity;
    private NativeAd nativeAd;
    private FrameLayout mNativeAdsContainer;
    private static FBNativeAdsHelper mInstace = null;
    private boolean isShow;
    private boolean isLoadingAds;

    private int topMargin;
    private int bottomMargin;

    public static FBNativeAdsHelper getInstance() {
        if (null == mInstace) {
            mInstace = new FBNativeAdsHelper();
        }
        return mInstace;
    }

    public void Init(Activity activity, String[] args) {
        placementId = args[0];
        _activity = activity;
        // Initialize the Audience Network SDK
        if (!AudienceNetworkAds.isInitialized(_activity.getApplicationContext())) {

            AudienceNetworkAds
                    .buildInitSettings(_activity.getApplicationContext())
                    .withInitListener(new AudienceNetworkAds.InitListener() {
                        @Override
                        public void onInitialized(AudienceNetworkAds.InitResult initResult) {
                            Log.d(AudienceNetworkAds.TAG, "Init " + initResult.getMessage());
                        }
                    })
                    .initialize();
        }
    }

    public void ShowAd(Activity activity, int topMargin, int bottomMargin) {
        isShow = true;
        this.topMargin = topMargin;
        this.bottomMargin = bottomMargin;
        Log.d(AudienceNetworkAds.TAG, "Show Ad");
        // Check if nativeAd has been loaded successfully
        if (nativeAd == null || !nativeAd.isAdLoaded()) {
            LoadAd(_activity);
            return;
        }
        // Check if ad is already expired or invalidated, and do not show ad if that is the case.
        // You will not get paid to show an invalidated ad.
        if (nativeAd.isAdInvalidated()) {
            LoadAd(_activity);
            return;
        }

        DisplayAds();
    }

    public void HideAds() {
        isShow = false;
        if (mNativeAdsContainer != null && mNativeAdsContainer.getVisibility() == View.VISIBLE)
            mNativeAdsContainer.setVisibility(View.GONE);
    }

    private void LoadAd(Activity activity) {
        if (isLoadingAds) return;
        nativeAd = new NativeAd(activity, placementId);
        isLoadingAds = true;
        // Initiate a request to load an ad.
        Log.d(AudienceNetworkAds.TAG, "Load Ad");
        nativeAd.loadAd(
                nativeAd.buildLoadAdConfig()
                        .withAdListener(this)
                        .withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
                        .build());
    }

    private void DisplayAds() {
        if (!isShow) return;
        Log.d(AudienceNetworkAds.TAG, "Display Ad");
        if (mNativeAdsContainer == null) {
            mNativeAdsContainer = new FrameLayout(_activity);
            int widthPx = _activity.getResources().getDimensionPixelSize(R.dimen.mrec_width);
            int heightPx = _activity.getResources().getDimensionPixelSize(R.dimen.mrec_height);

            int gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPx, heightPx, gravity);
            layoutParams.setMargins(0, topMargin, 0, bottomMargin);

            mNativeAdsContainer.setLayoutParams(layoutParams);

            ViewGroup rootView = _activity.findViewById(android.R.id.content);
            rootView.addView(mNativeAdsContainer);
        }

        mNativeAdsContainer.removeAllViews();

        if (mNativeAdsContainer.getVisibility() != View.VISIBLE) {
            mNativeAdsContainer.setVisibility(View.VISIBLE);
        }

        // Render the Native Ad Template
        View adView = NativeAdView.render(_activity, nativeAd);
        mNativeAdsContainer.addView(adView, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    @Override
    public void onMediaDownloaded(Ad ad) {

    }

    @Override
    public void onError(Ad ad, AdError adError) {
        Log.d(AudienceNetworkAds.TAG, "onError" + adError.getErrorMessage());
        isLoadingAds = false;
    }

    @Override
    public void onAdLoaded(Ad ad) {
        isLoadingAds = false;
        Log.d(AudienceNetworkAds.TAG, "onAdLoaded");
        DisplayAds();
    }

    @Override
    public void onAdClicked(Ad ad) {
    }

    @Override
    public void onLoggingImpression(Ad ad) {

    }
}

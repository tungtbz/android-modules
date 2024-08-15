package com.rofi.ironsourceads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdSize;
import com.unity3d.mediation.banner.LevelPlayBannerAdView;
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener;
import com.unity3d.player.UnityPlayer;

public class BannerAds {
    Activity mActivity;
    LevelPlayBannerAdView mBannerAdView;
    int mBannerAdViewVisibilityState = View.INVISIBLE;

    public BannerAds(String adUnitId, String sizeDescription, int sizeWidth, int sizeHeight, int customWidth, boolean displayOnLoad, int position, final IUnityBannerAdListener bannerListener) {
        this.mActivity = UnityPlayer.currentActivity;
        this.mBannerAdView = new LevelPlayBannerAdView(this.mActivity.getApplicationContext(), adUnitId);
        LevelPlayAdSize size = BannerUtils.getAdSize(sizeDescription, sizeWidth, sizeHeight, customWidth);

        if (size != null)
            this.mBannerAdView.setAdSize(size);

        this.mBannerAdView.setBackgroundColor(0);

        if (displayOnLoad) {
            this.mBannerAdView.setVisibility(View.VISIBLE);
            this.mBannerAdViewVisibilityState = 0;
        } else {
            this.mBannerAdView.setVisibility(View.GONE);
            this.mBannerAdViewVisibilityState = 8;
        }

        setPosition(position);
        this.mBannerAdView.setBannerListener(new LevelPlayBannerAdViewListener() {
            public void onAdLoaded(LevelPlayAdInfo levelPlayAdInfo) {
                if (bannerListener != null)
                    bannerListener.onAdLoaded(LevelPlayUtils.adInfoToString(levelPlayAdInfo));
            }

            public void onAdLoadFailed(LevelPlayAdError adError) {
                if (bannerListener != null)
                    bannerListener.onAdLoadFailed(LevelPlayUtils.adErrorToString(adError));
            }

            public void onAdDisplayed(LevelPlayAdInfo levelPlayAdInfo) {
                if (bannerListener != null)
                    bannerListener.onAdDisplayed(LevelPlayUtils.adInfoToString(levelPlayAdInfo));
            }

            public void onAdDisplayFailed(LevelPlayAdInfo levelPlayAdInfo, LevelPlayAdError adError) {
                if (bannerListener != null)
                    bannerListener.onAdDisplayFailed(LevelPlayUtils.adInfoToString(levelPlayAdInfo), LevelPlayUtils.adErrorToString(adError));
            }

            public void onAdClicked(LevelPlayAdInfo levelPlayAdInfo) {
                if (bannerListener != null)
                    bannerListener.onAdClicked(LevelPlayUtils.adInfoToString(levelPlayAdInfo));
            }

            public void onAdExpanded(LevelPlayAdInfo levelPlayAdInfo) {
                if (bannerListener != null)
                    bannerListener.onAdExpanded(LevelPlayUtils.adInfoToString(levelPlayAdInfo));
            }

            public void onAdCollapsed(LevelPlayAdInfo levelPlayAdInfo) {
                if (bannerListener != null)
                    bannerListener.onAdCollapsed(LevelPlayUtils.adInfoToString(levelPlayAdInfo));
            }

            public void onAdLeftApplication(LevelPlayAdInfo levelPlayAdInfo) {
                if (bannerListener != null)
                    bannerListener.onAdLeftApplication(LevelPlayUtils.adInfoToString(levelPlayAdInfo));
            }
        });
    }

    public void load() {
        this.mBannerAdView.loadAd();
    }

    public void destroy() {
        this.mBannerAdView.destroy();
    }

    public void showAd() {
        this.mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (BannerAds.this.mBannerAdView != null)
                    BannerAds.this.mBannerAdView.setVisibility(View.VISIBLE);
                BannerAds.this.mBannerAdViewVisibilityState = View.VISIBLE;
            }
        });
    }

    public void hideAd() {
        this.mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (BannerAds.this.mBannerAdView != null)
                    BannerAds.this.mBannerAdView.setVisibility(View.GONE);
                BannerAds.this.mBannerAdViewVisibilityState = View.GONE;
            }
        });
    }

    private void setPosition(final int position) {
        this.mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (BannerAds.this.mBannerAdView.getParent() == null)
                    BannerAds.this.mActivity.addContentView((View) BannerAds.this.mBannerAdView, (ViewGroup.LayoutParams) new FrameLayout.LayoutParams(-1, -2));
                BannerAds.this.setPositionInternal(position, 0, 0);
                BannerAds.this.mBannerAdView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                    public void onChildViewAdded(View parent, View child) {
                        BannerAds.this.mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                if (BannerAds.this.mBannerAdView != null)
                                    BannerAds.this.mBannerAdView.setVisibility(BannerAds.this.mBannerAdViewVisibilityState);
                                BannerAds.this.mBannerAdView.requestLayout();
                            }
                        });
                    }

                    public void onChildViewRemoved(View parent, View child) {
                    }
                });
            }
        });
    }

    private void setPositionInternal(int position, int offsetX, int offsetY) {
        FrameLayout.LayoutParams adLayoutParams = (FrameLayout.LayoutParams) this.mBannerAdView.getLayoutParams();
        if (adLayoutParams == null)
            return;
        adLayoutParams.gravity = (position == 1) ? 48 : 80;
        this.mBannerAdView.setLayoutParams((ViewGroup.LayoutParams) adLayoutParams);
    }

    public void resumeAutoRefresh() {
        this.mBannerAdView.resumeAutoRefresh();
    }

    public void pauseAutoRefresh() {
        this.mBannerAdView.pauseAutoRefresh();
    }
}

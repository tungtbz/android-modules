package com.rofi.maxads;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.rofi.ads.AdsEventListener;
import com.rofi.ads.IAdsService;
import com.rofi.base.Constants;
import com.rofi.base.ThreadUltils;
import com.rofi.remoteconfig.FirebaseRemoteConfigService;

import java.util.concurrent.TimeUnit;

public class MaxAdsService implements IAdsService {
    private final String TAG = "MaxAdsService";

    private MaxInterstitialAd mInterstitialAd;
    private int mRetryAttemptInterstitialAds;

    private MaxRewardedAd mRewardedAd;
    private int mRetryAttemptRewardAds;

    private MaxAdView bannerAdView;
    private MaxAdView rectAdView;

    private int mCurrentVideoRewardRequestCode;
    private int mCurrentInterRequestCode;
    private boolean mIsShowingAppOpenAd;
    //0 not load
    //1 call load ad
    //2 ad loaded
    private int mRectBannerState;
    //1 hide afterloaded
    //2 show afterloaded
    private int mRectShowFlag;

    private boolean isCoolDownShowInter;
    private boolean isInterAdClicked;
    FrameLayout mNativeAdsContainer;

    FrameLayout mNativeBannerAdsContainer;
    private int mRetryAttemptNativeAds;

    private int mRetryAttemptNativeBannerAds;
    //native ads
    private MaxNativeAdLoader nativeAdLoader;

    private MaxNativeAdLoader nativeBannerAdLoader;
    private MaxAd nativeAd, nativeBannerAd;
    private int mEndGameAdType;

    @Override
    public void Init(Activity activity) {
        mRectBannerState = 0;
        mRectShowFlag = 1;

        AppLovinSdk.getInstance(activity.getApplicationContext()).setMediationProvider("max");
        AppLovinSdk.getInstance(activity.getApplicationContext()).getSettings().setVerboseLogging(BuildConfig.DEBUG);
        AppLovinSdk.getInstance(activity.getApplicationContext()).getSettings().setCreativeDebuggerEnabled(BuildConfig.DEBUG);

        if (BuildConfig.DEBUG) {
            AppLovinSdk.getInstance(activity.getApplicationContext()).showMediationDebugger();
        }

        AppLovinSdk.initializeSdk(activity.getApplicationContext(), new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration configuration) {
                InitVideoRewardAds(activity);
                InitInterAds(activity);

                //cache MREC
                LoadMREC(activity);
            }
        });
    }

    //private
    void InitVideoRewardAds(Activity activity) {
        String videoRewardKey = activity.getResources().getString(R.string.applovin_videoreward_key);
        mRewardedAd = MaxRewardedAd.getInstance(videoRewardKey, activity);

        mRewardedAd.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        mRewardedAd.setListener(new MaxRewardedAdListener() {
            @Override
            public void onRewardedVideoStarted(MaxAd ad) {
                Log.d(TAG, "onRewardedVideoStarted: ============================");
            }

            @Override
            public void onRewardedVideoCompleted(MaxAd ad) {
                Log.d(TAG, "onRewardedVideoCompleted: =============================");
            }

            @Override
            public void onUserRewarded(final MaxAd maxAd, final MaxReward maxReward) {
                // Rewarded ad was displayed and user should receive the reward
//                LibraryBridge.SendMessageFromNativeToGame("OnVideoRewardedWithCode", String.valueOf(mCurrentVideoRewardRequestCode));
                _adsAdsEventListener.onVideoRewardUserRewarded(String.valueOf(mCurrentVideoRewardRequestCode));
            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'

                // Reset retry attempt
                mRetryAttemptRewardAds = 0;
//                AnalyticServices.getInstance().LogEvent(UnityPlayer.currentActivity, "af_rewarded_api_called", null);
                _adsAdsEventListener.onVideoRewardLoaded();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
//                AnalyticServices.getInstance().LogEvent(UnityPlayer.currentActivity, "af_rewarded_ad_displayed", null);
                _adsAdsEventListener.onVideoRewardDisplayed();
            }

            @Override
            public void onAdHidden(MaxAd ad) {
                // rewarded ad is hidden. Pre-load the next ad
                Log.d(TAG, "video reward onAdHidden: =============================");
                mRewardedAd.loadAd();
            }

            @Override
            public void onAdClicked(MaxAd ad) {
//                AnalyticServices.getInstance().LogEventAdClicked(UnityPlayer.currentActivity, ad.getFormat().getLabel());
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                // Rewarded ad failed to load
                // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)

                mRetryAttemptRewardAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptRewardAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRewardedAd.loadAd();
                    }
                }, delayMillis);
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                // Rewarded ad failed to display. We recommend loading the next ad
                mRewardedAd.loadAd();
            }
        });

        mRewardedAd.loadAd();
    }

    void InitInterAds(Activity activity) {
        String interKey = activity.getResources().getString(R.string.applovin_inter_key);
        Log.d(TAG, "createInterstitialAd: " + interKey);

        mInterstitialAd = new MaxInterstitialAd(interKey, activity);
        mInterstitialAd.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        mInterstitialAd.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {
                Log.d(TAG, "Inter: onAdExpanded");
            }

            @Override
            public void onAdCollapsed(MaxAd ad) {
                Log.d(TAG, "Inter: onAdCollapsed");
            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                Log.d(TAG, "Inter: onAdLoaded");
                // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'

                // Reset retry attempt
                mRetryAttemptInterstitialAds = 0;
                _adsAdsEventListener.onInterLoaded();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {

                Log.d(TAG, "Inter: onAdDisplayed");
                _adsAdsEventListener.onInterDisplayed();
            }

            @Override
            public void onAdHidden(MaxAd ad) {
                // Interstitial ad is hidden. Pre-load the next ad
                mInterstitialAd.loadAd();

                if (mIsShowingAppOpenAd) {
                    Log.d(TAG, "Inter: onAdHidden after show open app");
                    mIsShowingAppOpenAd = false;
                    return;
                }

                Log.d(TAG, "Inter: onAdHidden Normal");
                int coolDownShowInterInSencond = FirebaseRemoteConfigService.getInstance().GetInt(Constants.ADS_INTERVAL);
                ThreadUltils.startTask(() -> {
                    // doTask
                    isCoolDownShowInter = false;
                    mCurrentInterRequestCode = 0;
                    Log.d(TAG, "Inter: onAdHidden Reset Cooldown");
                }, coolDownShowInterInSencond * 1000L);
                _adsAdsEventListener.onInterHidden(String.valueOf(mCurrentInterRequestCode));
            }

            @Override
            public void onAdClicked(MaxAd ad) {
                Log.d(TAG, "Inter: onAdClicked");
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());
                isInterAdClicked = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                Log.d(TAG, "Inter: onAdLoadFailed");
                // Interstitial ad failed to load
                // AppLovin recommends that you retry with exponentially higher delays up to a maximum delay (in this case 64 seconds)

                mRetryAttemptInterstitialAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptInterstitialAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mInterstitialAd.loadAd();
                    }
                }, delayMillis);
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                // Interstitial ad failed to display. AppLovin recommends that you load the next ad.
                mInterstitialAd.loadAd();
            }
        });

        // Load the first ad
        mInterstitialAd.loadAd();
    }

    private void LoadMREC(Activity activity) {
        String mrecKey = activity.getResources().getString(R.string.applovin_mrec_ads_key);
        Log.d(TAG, "LoadRectBannerApplovin: bannerKey" + mrecKey);

        rectAdView = new MaxAdView(mrecKey, MaxAdFormat.MREC, activity.getApplicationContext());
        rectAdView.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });
        rectAdView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(MaxAd ad) {

            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                mRectBannerState = 2;
                if (mRectShowFlag == 1) {
                    HideMREC();
                }
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {

            }

            @Override
            public void onAdHidden(MaxAd ad) {

            }

            @Override
            public void onAdClicked(MaxAd ad) {
                Log.d(TAG, "onMRECAdClicked: ");
//                AnalyticManager.getInstance().ShowAds(2);
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
//                mRectBannerLoaded = false;
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {

            }
        });

        // MREC width and height are 300 and 250 respectively, on phones and tablets
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 300);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 250);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPx, heightPx, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        layoutParams.setMargins(0, 0, 0, 10);
        rectAdView.setLayoutParams(layoutParams);
        rectAdView.setVisibility(View.GONE);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(rectAdView);

        // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
        rectAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
        rectAdView.stopAutoRefresh();
        // Load the ad
        rectAdView.loadAd();
        mRectBannerState = 1;
    }

    private void LogRevenue(MaxAd ad) {
        double revenue = ad.getRevenue(); // In USD
        Log.d(TAG, "onAdRevenuePaid: revenue MRECT: " + revenue);
        String networkName = ad.getNetworkName(); // Display name of the network that showed the ad (e.g. "AdColony")
        String adUnitId = ad.getAdUnitId(); // The MAX Ad Unit ID
        String adFormatStr = ad.getFormat().getLabel();
        _adsAdsEventListener.onAdRevenuePaid(adFormatStr, adUnitId, networkName, revenue);
    }

    private void ShowNativeBanner(Activity activity) {
        if (nativeBannerAdLoader != null) {
            Log.d(TAG, "ShowNativeBanner: 111");
            mNativeBannerAdsContainer.setVisibility(View.VISIBLE);
            nativeBannerAdLoader.loadAd();
        } else {
            Log.d(TAG, "ShowNativeBanner: 222");
            InitNativeBannerAds(activity);
        }
    }

    private void InitNativeBannerAds(Activity activity) {
        //prepare container
        mNativeBannerAdsContainer = new FrameLayout(activity);
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 360);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 120);

        mNativeBannerAdsContainer.setLayoutParams(new FrameLayout.LayoutParams(widthPx, heightPx, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM));
//        mNativeBannerAdsContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM));

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(mNativeBannerAdsContainer);

        //init ads
        String adsKey = activity.getResources().getString(R.string.applovin_native_small);
        nativeBannerAdLoader = new MaxNativeAdLoader(adsKey, activity.getApplicationContext());
        nativeBannerAdLoader.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        nativeBannerAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad) {
                // Clean up any pre-existing native ad to prevent memory leaks.
                Log.d(TAG, "onNativeAdLoaded ");
                mRetryAttemptNativeAds = 0;
                if (nativeBannerAd != null) {
                    nativeBannerAdLoader.destroy(nativeBannerAd);
                }

                // Save ad for cleanup.
                nativeBannerAd = ad;

                // Add ad view to view.
                mNativeBannerAdsContainer.removeAllViews();
                mNativeBannerAdsContainer.addView(nativeAdView);
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error) {
                // We recommend retrying with exponentially higher delays up to a maximum delay
                Log.d(TAG, "onNativeAdLoadFailed ");
                mRetryAttemptNativeBannerAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptNativeBannerAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nativeBannerAdLoader.loadAd();
                    }
                }, delayMillis);

            }

            @Override
            public void onNativeAdClicked(final MaxAd ad) {
                // Optional click callback
            }
        });

        nativeBannerAdLoader.loadAd();
    }

    private void HideNativeBanner() {
        if (nativeBannerAd != null) {
            nativeBannerAdLoader.destroy(nativeBannerAd);
        }
        if (mNativeBannerAdsContainer != null) {
            mNativeBannerAdsContainer.setVisibility(View.GONE);
        }
    }

    private void ShowNormalBanner(Activity activity) {
        if (bannerAdView != null) {
            Log.d(TAG, "ShowBottomBannerAppLovin: 1");
            if (bannerAdView.getVisibility() != View.VISIBLE)
                bannerAdView.setVisibility(View.VISIBLE);

            bannerAdView.startAutoRefresh();
        } else {
            Log.d(TAG, "ShowBottomBannerAppLovin: 2");
            LoadNormalBanner(activity);
        }
    }

    private void LoadNormalBanner(Activity activity) {
        String bannerKey = activity.getResources().getString(R.string.applovin_banner_key);
        Log.d(TAG, "LoadApplovinBottomBanner: bannerKey" + bannerKey);

        bannerAdView = new MaxAdView(bannerKey, activity.getApplicationContext());

        bannerAdView.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        bannerAdView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(MaxAd ad) {

            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                Log.d(TAG, "BANNER onAdLoaded: ");
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
                Log.d(TAG, "onAdDisplayed: ");
            }

            @Override
            public void onAdHidden(MaxAd ad) {

            }

            @Override
            public void onAdClicked(MaxAd ad) {
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {

            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {

            }
        });

        // Stretch to the width of the screen for banners to be fully functional
        int width = ViewGroup.LayoutParams.MATCH_PARENT;

        // Banner height on phones and tablets is 50 and 90, respectively
//        int heightPx = activity.getResources().getDimensionPixelSize(R.dimen.banner_height);

//         Get the adaptive banner height.
        int heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight();
        int heightPx = AppLovinSdkUtils.dpToPx(activity, heightDp);
        bannerAdView.setExtraParameter("adaptive_banner", "true");

        bannerAdView.setBackgroundColor(Color.rgb(0, 0, 0));
        bannerAdView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(bannerAdView);

        // Load the ad
        bannerAdView.loadAd();
    }

    private void HideNormalBanner() {
        Log.d(TAG, "StopBottomBanner");
        if (bannerAdView != null) {
            // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
            bannerAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
            bannerAdView.stopAutoRefresh();

            bannerAdView.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean IsRewardReady() {
        if (mRewardedAd == null) return false;
        return mRewardedAd.isReady();
    }

    @Override
    public boolean IsInterReady() {
        if (mInterstitialAd == null) return false;
        return mInterstitialAd.isReady();
    }

    @Override
    public void ShowReward(int requestCode) {
        if (IsRewardReady()) {
            mCurrentVideoRewardRequestCode = requestCode;
            mRewardedAd.showAd();
        } else {
            Log.e(TAG, "ShowVideo Applovin: FAILEDDDDDDDDDDDDD");
        }
    }

    @Override
    public void ShowInter(int requestCode) {
        Log.d(TAG, "ShowInter: " + requestCode);
        //force show inter ads
        if (requestCode == 1) {
            if (IsInterReady()) {
                mInterstitialAd.showAd();
            } else {
                mIsShowingAppOpenAd = false;
            }
            return;
        }

        //show normal
        if (isCoolDownShowInter) return;

        if (IsInterReady()) {
            //reset flags
            isInterAdClicked = false;
            isCoolDownShowInter = true;

            mCurrentInterRequestCode = requestCode;
            mInterstitialAd.showAd();

        } else {
            Log.e(TAG, "ShowInter_Applovin: FAILEDDDDDDDDDDDDD");
        }
    }

    @Override
    public void ShowBanner(Activity activity) {
        int type = FirebaseRemoteConfigService.getInstance().GetInt(Constants.RK_BANNER_TYPE);
        Log.d(TAG, "ShowBanner: " + type);
        if (type == 0) {
            // Show Normal Banner
            ShowNormalBanner(activity);
        } else if (type == 1) {
            // Show Native Banner
            ShowNativeBanner(activity);
        }
    }

    @Override
    public void HideBanner() {
        int type = FirebaseRemoteConfigService.getInstance().GetInt(Constants.RK_BANNER_TYPE);
        if (type == 0) {
            HideNormalBanner();
        } else if (type == 1) {
            HideNativeBanner();
        }
    }

    @Override
    public void ShowMREC(Activity activity) {
        if (rectAdView == null) return;
        mRectShowFlag = 2;
        if (rectAdView.getVisibility() != View.VISIBLE) {
            rectAdView.setVisibility(View.VISIBLE);
            rectAdView.startAutoRefresh();
        }
    }

    @Override
    public void HideMREC() {
        if (mRectBannerState == 2 && rectAdView != null) {
            rectAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
            rectAdView.stopAutoRefresh();
        }

        if (rectAdView != null && rectAdView.getVisibility() == View.VISIBLE) {
            rectAdView.setVisibility(View.GONE);
        }

        mRectShowFlag = 1;
    }

    @Override
    public void OnPause() {

    }

    @Override
    public void Resume() {

    }

    AdsEventListener _adsAdsEventListener;

    @Override
    public void SetEventListener(AdsEventListener listener) {
        _adsAdsEventListener = listener;
    }
}

package com.rofi.maxads;

import static com.rofi.base.Constants.RESUME_INTER_ADS;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdResponse;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdReviewListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.applovin.sdk.AppLovinSdkUtils;
import com.rofi.ads.AdsEventListener;
import com.rofi.ads.IAdsService;
import com.rofi.base.Constants;
import com.rofi.base.ThreadUltils;
import com.rofi.remoteconfig.FirebaseRemoteConfigService;
import com.unity3d.player.UnityPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MaxAdsService implements IAdsService, MaxAdListener, MaxAdViewAdListener, MaxRewardedAdListener, MaxAdRevenueListener, MaxAdReviewListener {
    private final String TAG = "MaxAdsService";

    private MaxInterstitialAd mInterstitialAd;
    private int mRetryAttemptInterstitialAds;

    private MaxRewardedAd mRewardedAd;
    private int mRetryAttemptRewardAds;

    private MaxAdView bannerAdView;
    private MaxAdView rectAdView;

    private int mCurrentVideoRewardRequestCode;
    private int mCurrentInterRequestCode;
    //    private boolean mIsShowingAppOpenAd;
    //0 not load
    //1 call load ad
    //2 ad loaded
    private int mRectBannerState;
    //1 hide afterloaded
    //2 show afterloaded
    private int mRectShowFlag;

    private boolean isCoolDownShowInter;
    private boolean isClickToAds;

    private int mRetryAttemptNativeAds;

    private int mRetryAttemptNativeBannerAds;
    //native ads
    private FrameLayout mNativeRectAdsContainer;
    private FrameLayout mNativeBannerAdsContainer;

    private MaxNativeAdLoader nativeRectAdLoader;
    private MaxNativeAdLoader nativeBannerAdLoader;
    private MaxAd nativeRectAd, nativeBannerAd;

    private String _bannerAdId;
    private String _interAdId;
    private String _rewardAdId;
    private String _mrecAdId;
    private String _nativeRectAdId;
    private String _nativeSmallAdId;
    private String _openAdsId;

    private boolean _apsEnable;
    private String _apsAppId;
    private String _apsBannerId;
    private String _apsMRECId;
    private String _apsInterId;
    private String _apsVideoRewardId;

    private int _bannerPosition;
    private int _mrecPosition;

    private Activity _activity;
    private int blockAutoShowInterCount;


    private MaxAppOpenAd appOpenAd;
    private long _finishInterAdsTime = 0;
    int coolDownShowInterInSecond;
    boolean isFullscreenAdsShowing;
    boolean isMRECLoaded;
    boolean isMRECLoading;

    private Timer timer;

    private AmazonAdsService _maxAmazonAdsService;

    private AppLovinSdk sdk;
    private MaxAdView mFreeMrecAdViews;
    private String sdkKey;

    protected static class Insets {
        int left;

        int top;

        int right;

        int bottom;
    }

    @Override
    public void Init(Activity activity, String[] args) {
        if (args == null || args.length == 0) {
            Log.e(TAG, "args is empty!");
            return;
        }
        _activity = activity;
        sdkKey = args[0];
        _bannerAdId = args[1];
        _interAdId = args[2];
        _rewardAdId = args[3];
        _mrecAdId = args[4];
        _nativeRectAdId = args[5];
        _nativeSmallAdId = args[6];

        _bannerPosition = Integer.parseInt(args[7]);
        _mrecPosition = Integer.parseInt(args[8]);

        if (args.length >= 10) _openAdsId = args[9];

        _apsEnable = false;
        //aps
        if (args.length >= 13) {

            _apsAppId = args[10];
            _apsBannerId = args[11];
            _apsMRECId = args[12];
            _apsInterId = args[13];
            _apsVideoRewardId = args[14];

            if (_apsAppId != null && !_apsAppId.equals("")) {
                _apsEnable = true;
                Log.d(TAG, "APS _apsAppId:" + _apsAppId);

                Log.d(TAG, "APS _apsBannerId:" + _apsBannerId);
                Log.d(TAG, "APS _apsInterId:" + _apsInterId);
                Log.d(TAG, "APS _apsMRECId:" + _apsMRECId);
                Log.d(TAG, "APS _apsVideoRewardId:" + _apsVideoRewardId);

                _maxAmazonAdsService = new AmazonAdsService();
                _maxAmazonAdsService.Init(activity, _apsAppId);
            }
        }

        if (sdkKey == null || sdkKey.isEmpty()) {
            Log.d(TAG, "sdkKey is empty");
            return;
        }

        mRectBannerState = 0;
        mRectShowFlag = 1;

        Context context = activity.getApplicationContext();
        // Create the initialization configuration
        AppLovinSdkInitializationConfiguration initConfig = AppLovinSdkInitializationConfiguration.builder(sdkKey, context)
                .setMediationProvider(AppLovinMediationProvider.MAX)
                .setPluginVersion("Max-Unity-Custom")
                // Perform any additional configuration/setting changes
                .build();

        AppLovinPrivacySettings.setHasUserConsent(true, context);
        AppLovinPrivacySettings.setDoNotSell(false, context);
        this.sdk = AppLovinSdk.getInstance(context);

        this.sdk.getSettings().setVerboseLogging(BuildConfig.DEBUG);
        this.sdk.getSettings().setCreativeDebuggerEnabled(BuildConfig.DEBUG);

        //init sdk
        // Initialize the SDK with the configuration
        this.sdk.initialize(initConfig, new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration sdkConfig) {
                // Start loading ads
                Log.d(TAG, "onSdkInitialized");

                InitVideoRewardAds(_activity);
                InitInterAds(_activity);

//                //cache MREC
                LoadMREC(_activity, _mrecPosition);

                _adsAdsEventListener.onAdServiceLoaded();

                if (BuildConfig.DEBUG) {
                    sdk.showMediationDebugger();
                }
            }
        });

//        AppLovinSdk.initializeSdk(activity.getApplicationContext(), new AppLovinSdk.SdkInitializationListener() {
//            @Override
//            public void onSdkInitialized(final AppLovinSdkConfiguration configuration) {
//                Log.d(TAG, "onSdkInitialized");
//
//                InitVideoRewardAds(_activity);
//                InitInterAds(_activity);
//
////                //cache MREC
//                LoadMREC(_activity, _mrecPosition);
//
//                _adsAdsEventListener.onAdServiceLoaded();
//
//                if (BuildConfig.DEBUG) {
//                    sdk.showMediationDebugger();
//                }
//            }
//        });
    }

    static Activity getCurrentActivity() {
        return UnityPlayer.currentActivity;
    }

    static void runSafelyOnUiThread(Activity activity, final Runnable runner) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    runner.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    static boolean isMainThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

    public void ShowFreeMrec() {
        runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                if (mFreeMrecAdViews == null) {
                    return;
                }

                if (!_isFreeMrecLoaded) {
                    forceLoadFreeMrec();
                }

                mFreeMrecAdViews.setVisibility(View.VISIBLE);
                mFreeMrecAdViews.startAutoRefresh();
            }
        });
    }

    public void HideFreeMrec() {
        runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                if (mFreeMrecAdViews == null) {
                    return;
                }
                mFreeMrecAdViews.setVisibility(View.GONE);
                mFreeMrecAdViews.stopAutoRefresh();
            }
        });
    }

    private boolean _isFreeMrecLoading;
    private boolean _isFreeMrecLoaded;

    public void CreateFreeMrec(String adsId) {
        if (this._isFreeMrecLoading) return;
        _isFreeMrecLoaded = false;

        mFreeMrecAdViews = new MaxAdView(adsId, MaxAdFormat.MREC, getCurrentActivity().getApplicationContext());
        mFreeMrecAdViews.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        mFreeMrecAdViews.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(MaxAd ad) {

            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                Log.d(TAG, "Free MREC onAdLoaded: " + ad.getAdUnitId());
                _isFreeMrecLoading = false;
                _isFreeMrecLoaded = true;
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {

            }

            @Override
            public void onAdHidden(MaxAd ad) {

            }

            @Override
            public void onAdClicked(MaxAd ad) {
                Log.d(TAG, "Free MREC onMRECAdClicked: ");
//                AnalyticManager.getInstance().ShowAds(2);
                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
//                mRectBannerLoaded = false;
                Log.d(TAG, "Free MREC: onAdLoadFailed: ");
                _isFreeMrecLoading = false;
                _isFreeMrecLoaded = false;
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {

            }
        });

        mFreeMrecAdViews.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
        mFreeMrecAdViews.stopAutoRefresh();

        //add ads view to layout
        mFreeMrecAdViews.setVisibility(View.GONE);
        if (mFreeMrecAdViews.getParent() == null) {
            Activity currentActivity = MaxAdsService.getCurrentActivity();
            RelativeLayout relativeLayout = new RelativeLayout((Context) currentActivity);
            currentActivity.addContentView((View) relativeLayout, (ViewGroup.LayoutParams) new LinearLayout.LayoutParams(-1, -1));
            relativeLayout.addView((View) mFreeMrecAdViews);

            this.updatePositionMrecAdview("centered", 0);
        }

        forceLoadFreeMrec();
    }

    public void forceLoadFreeMrec() {
        runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            @Override
            public void run() {
                mFreeMrecAdViews.loadAd();
                _isFreeMrecLoading = true;
            }
        });
    }


    public void updatePositionMrecAdview(String adViewPosition, int adViewOffsetY) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                int adViewWidthDp = 0, adViewHeightDp = 0;

                MaxAdView adView = mFreeMrecAdViews;
                MaxAdFormat adFormat = MaxAdFormat.MREC;

                if (adView == null) {
                    MaxAdsService.e(adFormat.getLabel() + " does not exist");
                    return;
                }

                RelativeLayout relativeLayout = (RelativeLayout) adView.getParent();
                if (relativeLayout == null) {
                    MaxAdsService.e(adFormat.getLabel() + "'s parent does not exist");
                    return;
                }

                Rect windowRect = new Rect();
                relativeLayout.getWindowVisibleDisplayFrame(windowRect);

                //calculate w and h
                if ("top_center".equalsIgnoreCase(adViewPosition) || "bottom_center".equalsIgnoreCase(adViewPosition)) {
                    int adViewWidthPx = windowRect.width();
                    adViewWidthDp = AppLovinSdkUtils.pxToDp((Context) getCurrentActivity(), adViewWidthPx);
                } else {
                    adViewWidthDp = MaxAdFormat.MREC.getSize().getWidth();
                }
                adViewHeightDp = MaxAdFormat.MREC.getSize().getHeight();
                int widthPx = AppLovinSdkUtils.dpToPx((Context) getCurrentActivity(), adViewWidthDp);
                int heightPx = AppLovinSdkUtils.dpToPx((Context) getCurrentActivity(), adViewHeightDp);
                //=========

                int gravity = 0;
                adView.setRotation(0.0F);
                adView.setTranslationX(0.0F);

                if ("centered".equalsIgnoreCase(adViewPosition)) {
                    gravity = Gravity.CENTER;
                } else {
                    if (adViewPosition.contains("top")) {
                        gravity = Gravity.TOP;
                    } else if (adViewPosition.contains("bottom")) {
                        gravity = Gravity.BOTTOM;
                    }

                    if (adViewPosition.contains("center")) {
                        gravity |= Gravity.CENTER_HORIZONTAL;
                    }
                }

                MaxAdsService.Insets insets = MaxAdsService.getSafeInsets();
                int marginLeft = insets.left;
                int marginRight = insets.right;
                int marginTop = insets.top + adViewOffsetY;
                int marginBottom = insets.bottom;
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mFreeMrecAdViews.getLayoutParams();
                params.height = heightPx;
                params.width = widthPx;
                params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
                adView.setLayoutParams((ViewGroup.LayoutParams) params);


                relativeLayout.setGravity(gravity);
            }
        });
    }

    private static void d(String message) {
        String fullMessage = "[MaxUnityAdManager] " + message;
        Log.d("AppLovinSdk", fullMessage);
    }

    private static void e(String message) {
        String fullMessage = "[MaxUnityAdManager] " + message;
        Log.e("AppLovinSdk", fullMessage);
    }

    protected static Insets getSafeInsets() {
        Insets insets = new Insets();
        if (Build.VERSION.SDK_INT < 28)
            return insets;
        Window window = getCurrentActivity().getWindow();
        if (window == null)
            return insets;
        WindowInsets windowInsets = window.getDecorView().getRootWindowInsets();
        if (windowInsets == null)
            return insets;
        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        if (displayCutout == null)
            return insets;
        insets.left = displayCutout.getSafeInsetLeft();
        insets.top = displayCutout.getSafeInsetTop();
        insets.right = displayCutout.getSafeInsetRight();
        insets.bottom = displayCutout.getSafeInsetBottom();
        return insets;
    }

    @Override
    public void onResume(Activity activity) {
        ShowResumeAds();
        setBannerMrecToFront();
    }

    private void ShowResumeAds() {
        Log.d(TAG, "onResume blockAutoShowInterCount: " + blockAutoShowInterCount);
        if (blockAutoShowInterCount > 0) {
            DecreaseBlockAutoShowInter();
            return;
        }
        if (_isDisableResumeAds) return;

        boolean isShowResumeAds = FirebaseRemoteConfigService.getInstance().GetBoolean(Constants.RESUME_ADS_KEY);
        if (!isShowResumeAds) return;

        //resume from ads
        if (isClickToAds) {
            isClickToAds = false;
            return;
        }

        ShowInter(RESUME_INTER_ADS);
    }

    //private
    void InitVideoRewardAds(Activity activity) {
//        String videoRewardKey = activity.getResources().getString(R.string.applovin_videoreward_key);
        String videoRewardKey = _rewardAdId;
        mRewardedAd = MaxRewardedAd.getInstance(videoRewardKey, getCurrentActivity().getApplicationContext());

        mRewardedAd.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        mRewardedAd.setListener(new MaxRewardedAdListener() {
            @Override
            public void onUserRewarded(final MaxAd maxAd, final MaxReward maxReward) {
                Log.d(TAG, "video reward onUserRewarded: =============================");
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
                Log.d(TAG, "video reward onAdDisplayed: =============================");
                isFullscreenAdsShowing = true;

//                AnalyticServices.getInstance().LogEvent(UnityPlayer.currentActivity, "af_rewarded_ad_displayed", null);
                _adsAdsEventListener.onVideoRewardDisplayed();

                if (blockAutoShowInterCount <= 0) {
                    IncreaseBlockAutoShowInter();
                }
            }

            @Override
            public void onAdHidden(MaxAd ad) {
                // rewarded ad is hidden. Pre-load the next ad
                Log.d(TAG, "video reward onAdHidden: =============================");
                LoadVideoRewardAd(false);
                isFullscreenAdsShowing = false;

                //add some delay
                if (!isCoolDownShowInter) {
                    isCoolDownShowInter = true;
                    ThreadUltils.startTask(() -> {
                        // doTask
                        isCoolDownShowInter = false;
                        Log.d(TAG, "Inter Reset Cooldown");
                    }, 5 * 1000L);
                }
            }

            @Override
            public void onAdClicked(MaxAd ad) {
//                AnalyticServices.getInstance().LogEventAdClicked(UnityPlayer.currentActivity, ad.getFormat().getLabel());
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());

                isClickToAds = true;
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
                        LoadVideoRewardAd(false);
                    }
                }, delayMillis);
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                // Rewarded ad failed to display. We recommend loading the next ad
                LoadVideoRewardAd(false);
            }
        });

        LoadVideoRewardAd(true);
    }

    private void LoadVideoRewardAd(boolean isFirstLoad) {
        if (mRewardedAd == null) {
            Log.d(TAG, "Video reward: onAdLoadFailed xxx");
            return;
        }
        if (isFirstLoad && _apsVideoRewardId != null && !_apsVideoRewardId.equals("")) {
            _maxAmazonAdsService.loadRewardAd(_apsVideoRewardId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    Log.d(TAG, "APS load video reward onFailure : " + adError.getMessage());
                    mRewardedAd.setLocalExtraParameter("amazon_ad_error", adError);
                    mRewardedAd.loadAd();
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    Log.d(TAG, "APS load video reward onSuccess : " + dtbAdResponse.getImpressionUrl());
                    mRewardedAd.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    mRewardedAd.loadAd();
                }
            });
        } else {
            mRewardedAd.loadAd();
        }
    }

    void InitInterAds(Activity activity) {
//        String interKey = activity.getResources().getString(R.string.applovin_inter_key);
        Log.d(TAG, "createInterstitialAd: " + _interAdId);

        mInterstitialAd = new MaxInterstitialAd(_interAdId, activity);
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
                isFullscreenAdsShowing = true;

                _adsAdsEventListener.onInterDisplayed();

            }

            @Override
            public void onAdHidden(MaxAd ad) {
                isFullscreenAdsShowing = false;

                // Interstitial ad is hidden. Pre-load the next ad
                LoadInterAd(false);
                //check is ad resume
                if (mCurrentInterRequestCode == RESUME_INTER_ADS) {
                    Log.d(TAG, "Inter: ads resume Hidden ");
                    return;
                }
                Log.d(TAG, "Inter: onAdHidden Normal");

                coolDownShowInterInSecond =  FirebaseRemoteConfigService.getInstance().GetInt(Constants.ADS_INTERVAL);
//                FirebaseRemoteConfigService.getInstance().GetInt(Constants.ADS_INTERVAL);

//                ThreadUltils.startTask(() -> {
//                    // doTask
//                    isCoolDownShowInter = false;
//                    mCurrentInterRequestCode = 0;
//                    Log.d(TAG, "Inter: onAdHidden Reset Cooldown");
//                }, coolDownShowInterInSencond * 1000L);
                RunCountDownToShowInter();

                _adsAdsEventListener.onInterHidden(String.valueOf(mCurrentInterRequestCode));
            }

            @Override
            public void onAdClicked(MaxAd ad) {
                Log.d(TAG, "Inter: onAdClicked");
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());
                isClickToAds = true;
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

                        LoadInterAd(false);
                    }
                }, delayMillis);
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                // Interstitial ad failed to display. AppLovin recommends that you load the next ad.
                LoadInterAd(false);
            }
        });

        // Load the first ad
        LoadInterAd(true);
    }

    private void LoadInterAd(boolean isFirstLoad) {
        if (mInterstitialAd == null) {
            Log.d(TAG, "Inter: onAdLoadFailed xxx");
            return;
        }
        if (isFirstLoad && _apsInterId != null && !_apsInterId.equals("")) {
            _maxAmazonAdsService.loadInterAd(_apsInterId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    Log.d(TAG, "APS load inter onFailure : " + adError.getMessage());
                    mInterstitialAd.setLocalExtraParameter("amazon_ad_error", adError);
                    mInterstitialAd.loadAd();
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    Log.d(TAG, "APS load inter onSuccess : " + dtbAdResponse.getImpressionUrl());
                    mInterstitialAd.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    mInterstitialAd.loadAd();
                }
            });
        } else {
            mInterstitialAd.loadAd();
        }
    }

    private void RunCountDownToShowInter() {
        isCoolDownShowInter = true;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                // do your work
                if (coolDownShowInterInSecond <= 0) {
                    timer.cancel();

                    isCoolDownShowInter = false;
                    mCurrentInterRequestCode = 0;
                    Log.d(TAG, "RunCountDownToShowInter: Reset Cooldown");
                    return;
                }

                if (isFullscreenAdsShowing) {
                    Log.d(TAG, "RunCountDownToShowInter  isShowingFullscreenAds --> skip");
                    return;
                }

                Log.d(TAG, "RunCountDownToShowInter ");
                coolDownShowInterInSecond -= 1;

            }
        }, 0, 1000);
    }

    private void LoadMREC(Activity activity, int position) {
//        String mrecKey = activity.getResources().getString(R.string.applovin_mrec_ads_key);
        Log.d(TAG, "LoadRectBannerApplovin: bannerKey" + _mrecAdId);

        rectAdView = new MaxAdView(_mrecAdId, MaxAdFormat.MREC, activity.getApplicationContext());
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
                Log.d(TAG, "MREC onAdLoaded");
                isMRECLoaded = true;
                isMRECLoading = false;

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
                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
//                mRectBannerLoaded = false;
                Log.d(TAG, "MREC: onAdLoadFailed: ");
                isMRECLoading = false;
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {

            }
        });

        // MREC width and height are 300 and 250 respectively, on phones and tablets
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 300);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 250);
        int gravity = position == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPx, heightPx, gravity);
        layoutParams.setMargins(0, 0, 0, 0);
        rectAdView.setLayoutParams(layoutParams);
        rectAdView.setVisibility(View.GONE);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(rectAdView);

        // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
        rectAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
        rectAdView.stopAutoRefresh();

        if (_apsEnable && _apsMRECId != null && !_apsMRECId.equals("")) {
            _maxAmazonAdsService.loadMRECAd(_apsMRECId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    // 'adView' is your instance of MaxAdView
                    rectAdView.setLocalExtraParameter("amazon_ad_error", adError);
                    rectAdView.loadAd();
                    isMRECLoading = true;
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    // 'adView' is your instance of MaxAdView
                    rectAdView.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    rectAdView.loadAd();
                    isMRECLoading = true;
                }
            });
        } else {
            // Load the ad
            rectAdView.loadAd();
            mRectBannerState = 1;
            isMRECLoading = true;
        }
    }

    private void LogRevenue(MaxAd ad) {
        double revenue = ad.getRevenue(); // In USD
        Log.d(TAG, ad.getFormat() + " onAdRevenuePaid:  " + revenue);
        String networkName = ad.getNetworkName(); // Display name of the network that showed the ad (e.g. "AdColony")
        String adUnitId = ad.getAdUnitId(); // The MAX Ad Unit ID
        String adFormatStr = ad.getFormat().getLabel();
        _adsAdsEventListener.onAdRevenuePaid(adFormatStr, adUnitId, networkName, revenue);
    }

    boolean _isBannerLoading;

    private void LoadNormalBanner(Activity activity, int position) {
//        String bannerKey = activity.getResources().getString(R.string.applovin_banner_key);
        Log.d(TAG, "Load Banner: " + _bannerAdId);

        bannerAdView = new MaxAdView(_bannerAdId, activity.getApplicationContext());
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
                _isBannerLoading = false;
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
                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                _isBannerLoading = false;
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
        bannerAdView.setBackgroundColor(Color.TRANSPARENT);

        int gravity = 0;
        if (position == Constants.POSITION_CENTER_TOP)
            gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        else gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        bannerAdView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx, gravity));

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(bannerAdView);

        _LoadBannerInternal(activity);
    }

    private void _LoadBannerInternal(Activity activity) {
        if (_isBannerLoading) {
            Log.d(TAG, "_LoadBannerInternal IsLoading....");
            return;
        }

        if (_apsEnable && _apsBannerId != null && !_apsBannerId.equals("")) {
            _maxAmazonAdsService.loadBannerAd(activity, _apsBannerId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    Log.d(TAG, "APS onFailure " + adError.getMessage());
                    // 'adView' is your instance of MaxAdView
                    bannerAdView.setLocalExtraParameter("amazon_ad_error", adError);
                    bannerAdView.loadAd();
                    _isBannerLoading = true;
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    Log.d(TAG, "APS onSuccess " + dtbAdResponse.getImpressionUrl());
                    // 'adView' is your instance of MaxAdView
                    bannerAdView.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    bannerAdView.loadAd();
                    _isBannerLoading = true;
                }
            });
        } else {
            // Load the ad
            bannerAdView.loadAd();
            _isBannerLoading = true;
        }
    }

    private void ShowNormalBanner(Activity activity, int position) {
        if (bannerAdView != null) {
//            Log.d(TAG, "ShowBottomBannerAppLovin: 1");
            if (bannerAdView.getVisibility() != View.VISIBLE)
                bannerAdView.setVisibility(View.VISIBLE);

            bannerAdView.startAutoRefresh();
        } else {
//            Log.d(TAG, "ShowBottomBannerAppLovin: 2");
            LoadNormalBanner(activity, position);
        }
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
        if (_isDisableInterAds) {
            Log.d(TAG, "Inter Ads Is Disabled");
            return false;
        }
        if (isFullscreenAdsShowing) {
            Log.d(TAG, "isFullscreenAdsShowing");
            return false;
        }

        if (isCoolDownShowInter) {
            Log.d(TAG, "isCoolDownShowInter");
            return false;
        }

        if (mInterstitialAd == null) {
            return false;
        }
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

        if (_isDisableInterAds) {
            Log.d(TAG, "Inter Ads Is Disabled !!!!!!!");
            return;
        }

        if (isFullscreenAdsShowing) {
            Log.d(TAG, "Other full screen ads is showing, wait !!!!!!");
            return;
        }

        //show resume ads
        if (requestCode == RESUME_INTER_ADS) {
            if (IsInterReady()) {
                mInterstitialAd.showAd();
            }
            Log.d(TAG, "ShowInter: check 1 ");
            return;
        }

        //show normal
        if (isCoolDownShowInter) {
            Log.d(TAG, "ShowInter: check 2 ");
            return;
        }

        if (IsInterReady()) {
            //reset flags
            isClickToAds = false;
            Log.d(TAG, "ShowInter: check 3 ");
            mCurrentInterRequestCode = requestCode;
            mInterstitialAd.showAd();

        } else {
            Log.d(TAG, "ShowInter: check 4 ");
        }
    }

    @Override
    public void ShowBanner(Activity activity) {
        Log.d(TAG, "ShowBanner");
        ShowNormalBanner(activity, _bannerPosition);

//        int type = FirebaseRemoteConfigService.getInstance().GetInt(Constants.RK_BANNER_TYPE_OF_SCREEN + screenCode);
//        Log.d(TAG, "ShowBanner for screen: " + screenCode + " with position: " + _bannerPosition + ", type: " + type);
//
//        if (type == 0) {
//            // Show Normal Banner
//
//        } else if (type == 1) {
//            // Show Native Banner
//            ShowNativeBanner(activity, _bannerPosition);
//        }
    }

    @Override
    public void HideBanner() {
        HideNormalBanner();
        HideNativeBanner();
    }

    @Override
    public void ShowMREC(Activity activity) {
        Log.d(TAG, "ShowMREC");
        if (rectAdView == null) {
            LoadMREC(activity, _mrecPosition);
            return;
        }

        mRectShowFlag = 2;

        if (rectAdView.getVisibility() != View.VISIBLE) {
            rectAdView.setVisibility(View.VISIBLE);

            rectAdView.startAutoRefresh();

            if (!isMRECLoading && !isMRECLoaded) {
                rectAdView.loadAd();
            }
        }
    }

    @Override
    public void HideMREC() {
        Log.d(TAG, "HideMREC");
        if (mRectBannerState == 2 && rectAdView != null) {
            rectAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
            rectAdView.stopAutoRefresh();
        }

        if (rectAdView != null && rectAdView.getVisibility() == View.VISIBLE) {
            rectAdView.setVisibility(View.GONE);
        }

        mRectShowFlag = 1;
    }

    //fix bug for unity 2022.3.12
    private void setBannerMrecToFront() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (rectAdView != null && rectAdView.getVisibility() == View.VISIBLE) {
                    rectAdView.bringToFront();
                }

                if (bannerAdView != null && bannerAdView.getVisibility() == View.VISIBLE) {
                    bannerAdView.bringToFront();
                }
            }
        }, 500);


    }

    @Override
    public void ShowNativeMREC(Activity activity) {
        Log.d(TAG, "ShowNativeMREC");

        if (nativeRectAdLoader != null) {
            mNativeRectAdsContainer.setVisibility(View.VISIBLE);
            nativeRectAdLoader.loadAd();
        } else {
            LoadRectNativeAds(activity, _mrecPosition);
        }
    }

    @Override
    public void HideNativeMREC() {
        if (nativeRectAd != null) {
            nativeRectAdLoader.destroy(nativeRectAd);
        }
        if (mNativeRectAdsContainer != null) {
            mNativeRectAdsContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void ShowNativeBanner(Activity activity) {
        if (nativeBannerAdLoader != null) {
            Log.d(TAG, "ShowNativeBanner: 111");
            mNativeBannerAdsContainer.setVisibility(View.VISIBLE);
            nativeBannerAdLoader.loadAd();
        } else {
            Log.d(TAG, "ShowNativeBanner: 222");
            InitNativeBannerAds(activity, _bannerPosition);
        }
    }

    @Override
    public void HideNativeBanner() {
        //destroy
        if (nativeBannerAd != null) {
            nativeBannerAdLoader.destroy(nativeBannerAd);
        }
        //hide container
        if (mNativeBannerAdsContainer != null) {
            mNativeBannerAdsContainer.setVisibility(View.GONE);
        }
    }

    private void InitNativeBannerAds(Activity activity, int position) {
        //prepare container
        mNativeBannerAdsContainer = new FrameLayout(activity);
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 360);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 120);

        int gravity = position == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        mNativeBannerAdsContainer.setLayoutParams(new FrameLayout.LayoutParams(widthPx, heightPx, gravity));

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(mNativeBannerAdsContainer);

        nativeBannerAdLoader = new MaxNativeAdLoader(_nativeSmallAdId, activity.getApplicationContext());
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
                Log.d(TAG, "InitNativeBannerAds ");
                Log.d(TAG, "InitNativeBannerAds x: " + (nativeAdView == null));
                Log.d(TAG, "InitNativeBannerAds y: " + (mNativeBannerAdsContainer == null));
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

    //Native RECT
    private void LoadRectNativeAds(Activity activity, int position) {
        //prepare container
        mNativeRectAdsContainer = new FrameLayout(activity);
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 300);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 250);

        int gravity = 0;
        if (position == Constants.POSITION_CENTER_TOP)
            gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        else gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPx, heightPx, gravity);
        layoutParams.setMargins(0, 0, 0, 0);

        mNativeRectAdsContainer.setLayoutParams(layoutParams);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(mNativeRectAdsContainer);

        Log.d(TAG, "_nativeRectAdId " + _nativeRectAdId);
        nativeRectAdLoader = new MaxNativeAdLoader(_nativeRectAdId, activity.getApplicationContext());

        nativeRectAdLoader.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        nativeRectAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad) {
                // Clean up any pre-existing native ad to prevent memory leaks.
                Log.d(TAG, "LoadRectNativeAds ");
                Log.d(TAG, "LoadRectNativeAds x: " + (nativeAdView == null));
                Log.d(TAG, "LoadRectNativeAds z: " + (ad == null));
                Log.d(TAG, "LoadRectNativeAds y: " + (mNativeRectAdsContainer == null));

                mRetryAttemptNativeAds = 0;

                if (nativeRectAd != null) {
                    nativeRectAdLoader.destroy(nativeRectAd);
                }

                // Save ad for cleanup.
                nativeRectAd = ad;

                // Add ad view to view.
                mNativeRectAdsContainer.removeAllViews();
                mNativeRectAdsContainer.addView(nativeAdView);
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error) {
                // We recommend retrying with exponentially higher delays up to a maximum delay
                Log.d(TAG, "onNativeAdLoadFailed ");
                mRetryAttemptNativeAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptNativeAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nativeRectAdLoader.loadAd();
                    }
                }, delayMillis);

            }

            @Override
            public void onNativeAdClicked(final MaxAd ad) {
                // Optional click callback
            }
        });

        nativeRectAdLoader.loadAd();
    }

    @Override
    public void OnPause(Activity activity) {

    }

    AdsEventListener _adsAdsEventListener;

    @Override
    public void SetEventListener(AdsEventListener listener) {
        _adsAdsEventListener = listener;
    }

    @Override
    public void IncreaseBlockAutoShowInter() {
        Log.d(TAG, "IncreaseBlockAutoShowInter ");
        blockAutoShowInterCount += 1;
    }

    @Override
    public void DecreaseBlockAutoShowInter() {
        Log.d(TAG, "DecreaseBlockAutoShowInter ");
        blockAutoShowInterCount -= 1;
        if (blockAutoShowInterCount < 0) blockAutoShowInterCount = 0;
    }

    @Override
    public void LoadOpenAppAds(Activity activity) {
        Log.d(TAG, "OpenAppAds " + _openAdsId);
        appOpenAd = new MaxAppOpenAd(_openAdsId, activity.getApplicationContext());

        appOpenAd.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        appOpenAd.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd maxAd) {

            }

            @Override
            public void onAdCollapsed(MaxAd maxAd) {

            }

            @Override
            public void onAdLoaded(MaxAd maxAd) {
                Log.d(TAG, "Open App On Ad Loaded!");
            }

            @Override
            public void onAdDisplayed(MaxAd maxAd) {
                Log.d(TAG, "Open App onAdDisplayed!");
                isFullscreenAdsShowing = true;

            }

            @Override
            public void onAdHidden(MaxAd maxAd) {
                Log.d(TAG, "Open App onAdHidden!");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        _adsAdsEventListener.onAOAAdHidden();
                        isFullscreenAdsShowing = false;
                    }
                }, 2 * 1000);

            }

            @Override
            public void onAdClicked(MaxAd maxAd) {
                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {
                Log.d(TAG, "Open App On Ad LoadFailed!");
            }

            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                Log.d(TAG, "Open App On Ad Display Failed!");
            }
        });

        //
        appOpenAd.loadAd();
    }

    @Override
    public void ShowOpenAppAds(Activity activity) {
        if (appOpenAd == null || !AppLovinSdk.getInstance(activity.getApplicationContext()).isInitialized())
            return;

        if (appOpenAd.isReady()) {
            Log.d(TAG, "ShowOpenAppAds ");
            appOpenAd.showAd();
        }
    }

    @Override
    public boolean IsOpenAppAdsAvailable() {
        if (appOpenAd == null) return false;

        return appOpenAd.isReady();
    }

    private boolean _isDisableResumeAds;


    @Override
    public void DisableResumeAds() {
        _isDisableResumeAds = true;
    }

    @Override
    public void EnableResumeAds() {
        _isDisableResumeAds = false;
    }

    private boolean _isDisableInterAds;

    @Override
    public void DisableInterAds() {
        _isDisableInterAds = true;
    }

    @Override
    public void EnableInterAds() {
        _isDisableInterAds = false;
    }

    //ads callbacks
    @Override
    public void onAdExpanded(@NonNull MaxAd maxAd) {

    }

    @Override
    public void onAdCollapsed(@NonNull MaxAd maxAd) {

    }

    @Override
    public void onUserRewarded(@NonNull MaxAd maxAd, @NonNull MaxReward maxReward) {

    }

    @Override
    public void onAdLoaded(@NonNull MaxAd maxAd) {

    }

    @Override
    public void onAdDisplayed(@NonNull MaxAd maxAd) {

    }

    @Override
    public void onAdHidden(@NonNull MaxAd maxAd) {

    }

    @Override
    public void onAdClicked(@NonNull MaxAd maxAd) {

    }

    @Override
    public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {

    }

    @Override
    public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {

    }

    @Override
    public void onAdRevenuePaid(@NonNull MaxAd maxAd) {

    }

    @Override
    public void onCreativeIdGenerated(@NonNull String s, @NonNull MaxAd maxAd) {

    }
}

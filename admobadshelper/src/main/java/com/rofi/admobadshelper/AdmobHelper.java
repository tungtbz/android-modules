package com.rofi.admobadshelper;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdInspectorError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnAdInspectorClosedListener;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.ump.ConsentInformation;
import com.rofi.base.Constants;
import com.rofi.base.ThreadUltils;
import com.unity3d.player.UnityPlayer;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdmobHelper {
    // Constants for ad positions
    public static final int POSITION_TOP_CENTER = 0;
    public static final int POSITION_BOTTOM_CENTER = 1;
    public static final int POSITION_TOP_LEFT = 2;
    public static final int POSITION_TOP_RIGHT = 3;
    public static final int POSITION_BOTTOM_LEFT = 4;
    public static final int POSITION_BOTTOM_RIGHT = 5;
    public static final int POSITION_CENTER = 6;
    public static final int POSITION_CUSTOM = -1;
    
    private static volatile AdmobHelper mInstance = null;
    private static final Object sLock = new Object();
    private final String TAG = AdmobHelper.class.toString();
    
    // Thread-safe variables using volatile
    private volatile AppOpenAd _appOpenAd = null;
    private volatile boolean _isLoadingAd = false;
    private volatile boolean _isShowingAd = false;
    private volatile long loadTime = 0;
    private volatile int consentCode = -1;

    private volatile IAdmobAdListener adsEventCallback;

    // UI components - Use WeakReference to prevent memory leaks
    // These should be cleared when activity is destroyed
    private volatile WeakReference<AdView> mrecAdViewRef;
    private volatile WeakReference<AdView> cBannerViewRef;
    
    // Keep WeakReference to current activity to avoid memory leaks
    private volatile WeakReference<Activity> currentActivityRef;
    
    // Interstitial Ad - Use strong reference (single-use ad, cleared after show)
    private volatile InterstitialAd interstitialAd;
    private volatile boolean interstitialAdLoading = false;
    private volatile boolean interstitialAdLoaded = false;
    private volatile boolean interstitialAdsEnabled = true; // Control interstitial ad display
    private volatile long lastInterstitialDismissedTime = 0; // Track when last interstitial was dismissed
    private volatile long interstitialIntervalMs = 0; // Minimum interval between interstitials (0 = no limit)
    
    // Rewarded Ad - Use strong reference (single-use ad, cleared after show)
    private volatile RewardedAd rewardedAd;
    private volatile boolean rewardedAdLoading = false;
    private volatile boolean rewardedAdLoaded = false;
    private volatile int currentRewardCode = 0; // Store reward code for callback
    private volatile int currentInterstitialCode = 0; // Store interstitial code for callback
    
    // Auto-reload retry mechanism
    private static final long BASE_RETRY_DELAY_MS = 5000; // 5 seconds base delay
    private static final long MAX_RETRY_DELAY_MS = 300000; // 5 minutes max delay
    
    // Retry counters for each ad type
    private volatile int bannerRetryCount = 0;
    private volatile int mrecRetryCount = 0;
    private volatile int interstitialRetryCount = 0;
    private volatile int rewardedRetryCount = 0;
    
    // Handlers for scheduled retry tasks
    private final Handler bannerRetryHandler = new Handler(Looper.getMainLooper());
    private final Handler mrecRetryHandler = new Handler(Looper.getMainLooper());
    private final Handler interstitialRetryHandler = new Handler(Looper.getMainLooper());
    private final Handler rewardedRetryHandler = new Handler(Looper.getMainLooper());

    // Thread-safe singleton pattern
    public static AdmobHelper getInstance() {
        if (mInstance == null) {
            synchronized (sLock) {
                if (mInstance == null) {
                    mInstance = new AdmobHelper();
                }
            }
        }
        return mInstance;
    }

    String _appOpenAdsId;
    String _cBannerId;
    int bannerPosition;
    String _mrecAdsId;
    String _interstitialAdsId;
    String _rewardedAdsId;
    
    // Thread-safe boolean flags using volatile
    private volatile boolean mrecAdLoading;
    private volatile boolean bannerAdLoading;
    private volatile boolean mrecAdLoaded;
    private volatile boolean bannerAdLoaded;
    
    // Logical state tracking - desired visibility state
    private volatile boolean bannerLogicallyVisible = false;
    private volatile boolean mrecLogicallyVisible = false;
    
    // Thread-safe counters using volatile
    private volatile int blockAOACount;

    private volatile boolean _isDisableResumeAds;
    private volatile boolean aoaBlocker;
//    private volatile boolean m_isCollapsible;

    //    private IAdmobAdListener adListener;
    private ConsentInformation consentInformation;
    // Use an atomic boolean to initialize the Google Mobile Ads SDK and load ads once.
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;

    public void Init(Activity activity, IAdmobAdListener adListener, String[] args) {
        _appOpenAdsId = args[0];

//        this.adListener = adListener;
    }

    public AdmobHelper() {
        blockAOACount = 0;
        mrecAdViewRef = new WeakReference<>(null);
        cBannerViewRef = new WeakReference<>(null);
        currentActivityRef = new WeakReference<>(null);
        // interstitialAd and rewardedAd are now strong references, initialized as null by default
    }
    
    /**
     * Helper method to safely get banner AdView from WeakReference
     */
    @Nullable
    private AdView getBannerAdView() {
        return cBannerViewRef != null ? cBannerViewRef.get() : null;
    }
    
    /**
     * Helper method to safely set banner AdView with WeakReference
     */
    private void setBannerAdView(@Nullable AdView adView) {
        if (adView != null) {
            cBannerViewRef = new WeakReference<>(adView);
        } else {
            cBannerViewRef = new WeakReference<>(null);
        }
    }
    
    /**
     * Helper method to safely get MREC AdView from WeakReference
     */
    @Nullable
    private AdView getMrecAdView() {
        return mrecAdViewRef != null ? mrecAdViewRef.get() : null;
    }
    
    /**
     * Helper method to safely set MREC AdView with WeakReference
     */
    private void setMrecAdView(@Nullable AdView adView) {
        if (adView != null) {
            mrecAdViewRef = new WeakReference<>(adView);
        } else {
            mrecAdViewRef = new WeakReference<>(null);
        }
    }
    
    /**
     * Helper method to safely get Interstitial Ad with synchronization
     * Thread-safe access to interstitial ad instance
     */
    @Nullable
    private synchronized InterstitialAd getInterstitialAd() {
        return interstitialAd;
    }
    
    /**
     * Helper method to safely set Interstitial Ad with synchronization
     * Thread-safe setter that also updates the loaded flag
     */
    private synchronized void setInterstitialAd(@Nullable InterstitialAd ad) {
        this.interstitialAd = ad;
        this.interstitialAdLoaded = (ad != null);
    }
    
    /**
     * Helper method to safely get Rewarded Ad with synchronization
     * Thread-safe access to rewarded ad instance
     */
    @Nullable
    private synchronized RewardedAd getRewardedAd() {
        return rewardedAd;
    }
    
    /**
     * Helper method to safely set Rewarded Ad with synchronization
     * Thread-safe setter that also updates the loaded flag
     */
    private synchronized void setRewardedAd(@Nullable RewardedAd ad) {
        this.rewardedAd = ad;
        this.rewardedAdLoaded = (ad != null);
    }
    
    /**
     * Helper method to safely set current activity
     */
    private void setCurrentActivity(@Nullable Activity activity) {
        if (activity != null) {
            currentActivityRef = new WeakReference<>(activity);
        } else {
            currentActivityRef = new WeakReference<>(null);
        }
    }

    public void initBanner(Activity activity, String id, int position) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot init banner - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        _cBannerId = id;
        bannerPosition = position;
//        m_isCollapsible = isCollapsible;
        AdView bannerView = new AdView(activity);
        bannerView.setAdSize(getBannerAdSize(activity));
        bannerView.setAdUnitId(_cBannerId);
        bannerView.setVisibility(View.GONE);

        bannerView.setOnPaidEventListener(adValue -> {
            AdView banner = getBannerAdView();
            if (banner == null) return;
            
            ResponseInfo responseInfo = banner.getResponseInfo();
            String adSourceName = "admob";
            if (responseInfo != null) {
                AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();

                if (loadedAdapterResponseInfo != null) {
                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                    Log.d(TAG, "BANNER loadedAdapterResponseInfo" + "\nadSourceName" + adSourceName);

                }
            }

            onAdPaid("BANNER", adValue, _cBannerId, adSourceName);

        });

        bannerView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.d(TAG, "BANNER onAdFailedToLoad: " + loadAdError.getMessage());
                synchronized (AdmobHelper.this) {
                    bannerAdLoading = false;
                }
                
                // Schedule auto-reload with exponential backoff
                scheduleBannerReload();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                synchronized (AdmobHelper.this) {
                    bannerAdLoading = false;
                    bannerAdLoaded = true;
                }
                
                // Reset retry counter on successful load
                resetBannerRetryCounter();
                
                Log.d(TAG, "BANNER onAdLoaded");
                
                // Check logical state and apply visibility accordingly
                AdView banner = getBannerAdView();
                if (banner != null) {
                    if (bannerLogicallyVisible) {
                        Log.d(TAG, "BANNER onAdLoaded - logical state is SHOW, displaying ad");
                        banner.resume();
                        banner.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "BANNER onAdLoaded - logical state is HIDE, keeping ad hidden");
                        banner.setVisibility(View.GONE);
                    }
                }
                
                IAdmobAdListener callback = adsEventCallback;
                if (callback != null) {
                    callback.onBannerLoaded();
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                IAdmobAdListener callback = adsEventCallback;
                if (callback != null) {
                    callback.onAdClicked();
                }
            }

            @Override
            public void onAdOpened() {
                Log.d(TAG, "BANNER onAdOpened");
            }
        });

        int gravity = bannerPosition == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);

        layoutParams.setMargins(0, 0, 0, 0);
        bannerView.setLayoutParams(layoutParams);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(bannerView);
        
        // Store in WeakReference to prevent memory leak
        setBannerAdView(bannerView);
    }

    Handler handler = new Handler(Looper.getMainLooper()); // Thread-safe handler
    
    /**
     * Calculate exponential backoff delay for retry attempts
     * @param attemptCount Number of retry attempts (0-based)
     * @return Delay in milliseconds (5s, 10s, 20s, 40s, ..., capped at 5 minutes)
     */
    private long calculateRetryDelay(int attemptCount) {
        long delay = BASE_RETRY_DELAY_MS * (long) Math.pow(2, attemptCount);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }
    
    /**
     * Schedule banner ad reload with exponential backoff
     */
    private void scheduleBannerReload() {
        synchronized (this) {
            bannerRetryCount++;
        }
        
        long delay = calculateRetryDelay(bannerRetryCount);
        Log.d(TAG, "Scheduling banner reload. Attempt: " + bannerRetryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener callback = adsEventCallback;
        if (callback != null) {
            callback.onBannerRetrying(bannerRetryCount, delay);
        }
        
        bannerRetryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying banner load (attempt " + bannerRetryCount + ")");
            loadBanner(true, false);
        }, delay);
    }
    
    /**
     * Schedule MREC ad reload with exponential backoff
     */
    private void scheduleMrecReload() {
        synchronized (this) {
            mrecRetryCount++;
        }
        
        long delay = calculateRetryDelay(mrecRetryCount);
        Log.d(TAG, "Scheduling MREC reload. Attempt: " + mrecRetryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener callback = adsEventCallback;
        if (callback != null) {
            callback.onMrecRetrying(mrecRetryCount, delay);
        }
        
        mrecRetryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying MREC load (attempt " + mrecRetryCount + ")");
            loadMrec();
        }, delay);
    }
    
    /**
     * Schedule interstitial ad reload with exponential backoff
     */
    private void scheduleInterstitialReload() {
        synchronized (this) {
            interstitialRetryCount++;
        }
        
        long delay = calculateRetryDelay(interstitialRetryCount);
        Log.d(TAG, "Scheduling interstitial reload. Attempt: " + interstitialRetryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener callback = adsEventCallback;
        if (callback != null) {
            callback.onInterstitialRetrying(interstitialRetryCount, delay);
        }
        
        interstitialRetryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying interstitial load (attempt " + interstitialRetryCount + ")");
            loadInterstitial();
        }, delay);
    }
    
    /**
     * Schedule rewarded ad reload with exponential backoff
     */
    private void scheduleRewardedReload() {
        synchronized (this) {
            rewardedRetryCount++;
        }
        
        long delay = calculateRetryDelay(rewardedRetryCount);
        Log.d(TAG, "Scheduling rewarded reload. Attempt: " + rewardedRetryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener callback = adsEventCallback;
        if (callback != null) {
            callback.onRewardedRetrying(rewardedRetryCount, delay);
        }
        
        rewardedRetryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying rewarded load (attempt " + rewardedRetryCount + ")");
            loadRewarded();
        }, delay);
    }
    
    /**
     * Reset retry counter for specific ad type on successful load
     */
    private void resetBannerRetryCounter() {
        synchronized (this) {
            if (bannerRetryCount > 0) {
                Log.d(TAG, "Banner loaded successfully after " + bannerRetryCount + " retries");
            }
            bannerRetryCount = 0;
        }
        bannerRetryHandler.removeCallbacksAndMessages(null);
    }
    
    private void resetMrecRetryCounter() {
        synchronized (this) {
            if (mrecRetryCount > 0) {
                Log.d(TAG, "MREC loaded successfully after " + mrecRetryCount + " retries");
            }
            mrecRetryCount = 0;
        }
        mrecRetryHandler.removeCallbacksAndMessages(null);
    }
    
    private void resetInterstitialRetryCounter() {
        synchronized (this) {
            if (interstitialRetryCount > 0) {
                Log.d(TAG, "Interstitial loaded successfully after " + interstitialRetryCount + " retries");
            }
            interstitialRetryCount = 0;
        }
        interstitialRetryHandler.removeCallbacksAndMessages(null);
    }
    
    private void resetRewardedRetryCounter() {
        synchronized (this) {
            if (rewardedRetryCount > 0) {
                Log.d(TAG, "Rewarded loaded successfully after " + rewardedRetryCount + " retries");
            }
            rewardedRetryCount = 0;
        }
        rewardedRetryHandler.removeCallbacksAndMessages(null);
    }

    protected static class Insets {
        int left;

        int top;

        int right;

        int bottom;
    }

    static void runSafelyOnUiThread(Activity activity, final Runnable runner) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w("AdmobHelper", "Activity is null or finishing, skipping UI operation");
            return;
        }
        
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        runner.run();
                    }
                } catch (Exception e) {
                    Log.e("AdmobHelper", "Error in UI thread operation", e);
                }
            }
        });
    }

    static Activity getCurrentActivity() {
        Activity activity = UnityPlayer.currentActivity;
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w("AdmobHelper", "Current activity is null or invalid");
            return null;
        }
        return activity;
    }

    public void RunAutoRefreshBanner(int refreshTime) {
        handler.removeCallbacksAndMessages(null);

        Runnable r = new Runnable() {
            public void run() {
                Activity currentActivity = getCurrentActivity();
                if (currentActivity != null) {
                    runSafelyOnUiThread(currentActivity, new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Manual Load Banner refresh Time" + refreshTime);
                            loadBanner(false, false);
                        }
                    });
                } else {
                    Log.w(TAG, "Cannot refresh banner - no valid activity");
                }

                handler.postDelayed(this, refreshTime * 1000L);
            }
        };

        handler.postDelayed(r, 1);
    }

    public void ForceLoadBanner(boolean isCollapsible) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, new Runnable() {
                @Override
                public void run() {
                    loadBanner(true, isCollapsible);
                }
            });
        }
    }

    public void StopRefresh() {
        handler.removeCallbacksAndMessages(null);
    }

    private void loadBanner(boolean isForceLoad, boolean isCollapsible) {
        AdView bannerView = getBannerAdView();
        if (_cBannerId == null || bannerView == null) {
            synchronized (this) {
                bannerAdLoading = false;
            }
            return;
        }

        synchronized (this) {
            if (!isForceLoad && bannerAdLoading) return;
            if (!isForceLoad && bannerAdLoaded) return;
            bannerAdLoading = true;
        }

        // Ensure UI operations run on main thread
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView banner = getBannerAdView();
                if (banner == null) {
                    synchronized (this) {
                        bannerAdLoading = false;
                    }
                    Log.w(TAG, "Banner view was garbage collected");
                    return;
                }
                if(isCollapsible){
                    // Create an extra parameter that aligns the bottom of the expanded ad to
                    // the bottom of the bannerView.
                    Bundle extras = new Bundle();
                    extras.putString("collapsible", bannerPosition == Constants.POSITION_CENTER_TOP ? "top" : "bottom");
                    AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();

                    banner.loadAd(adRequest);
                }else {
                    AdRequest adRequest = new AdRequest.Builder().build();

                    banner.loadAd(adRequest);
                }

                Log.d(TAG, "Loading Banner");
            });
        } else {
            synchronized (this) {
                bannerAdLoading = false; // Reset flag if no valid activity
            }
        }
    }

    public void showBanner() {
        // Set logical state to visible
        bannerLogicallyVisible = true;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView banner = getBannerAdView();
                if (banner != null && bannerAdLoaded && banner.getVisibility() == View.GONE) {
                    Log.d(TAG, "showBanner - ad loaded, showing now");
                    banner.resume();
                    banner.setVisibility(View.VISIBLE);
                } else if (banner != null && !bannerAdLoaded) {
                    Log.d(TAG, "showBanner - ad not loaded yet, will auto-show when loaded");
                }
            });
        }
    }

    public void HideBanner() {
        // Set logical state to hidden
        bannerLogicallyVisible = false;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView banner = getBannerAdView();
                if (banner != null && banner.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "HideBanner - hiding now");
                    banner.pause();
                    banner.setVisibility(View.GONE);
                } else if (banner != null && !bannerAdLoaded) {
                    Log.d(TAG, "HideBanner - ad not loaded yet, will stay hidden when loaded");
                }
            });
        }
    }

    private AdSize getBannerAdSize(Activity activity) {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = outMetrics.widthPixels;

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity.getApplicationContext(), adWidth);
    }

//    private int mPositionCode;
//    private int mHorizontalOffset;
//    private int mVerticalOffset;

    private void updateMrecPosition(int positionCode, int topPadding) {
        AdView mrec = getMrecAdView();
        if (mrec == null) return;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, new Runnable() {
                public void run() {
                    AdView mrecView = getMrecAdView();
                    if (mrecView != null) { // Double check after UI thread switch
                        FrameLayout.LayoutParams layoutParams = getLayoutParams(positionCode, topPadding);
                        mrecView.setLayoutParams(layoutParams);
                    }
                }
            });
        }
    }

    protected FrameLayout.LayoutParams getLayoutParams(int positionCode, int topPadding) {
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        adParams.gravity = AdmobHelper.getLayoutGravityForPositionCode(positionCode);
        Insets insets = getSafeInsets();
        int safeInsetLeft = insets.left;
        int safeInsetTop = insets.top;

        adParams.bottomMargin = insets.bottom;
        adParams.rightMargin = insets.right;

        if (positionCode == -1) {
//            int leftOffset = (int) AdmobHelper.convertDpToPixel(this.mHorizontalOffset);
            int leftOffset = 0;
            if (leftOffset < safeInsetLeft)
                leftOffset = safeInsetLeft;
            int topOffset = (int) AdmobHelper.convertDpToPixel(topPadding);
            if (topOffset < safeInsetTop)
                topOffset = safeInsetTop;
            adParams.leftMargin = leftOffset;
            adParams.topMargin = topOffset;

        } else {
            adParams.leftMargin = safeInsetLeft;

            if (positionCode == 0
                    || positionCode == 2
                    || positionCode == 3
                    || positionCode == 6
            ) {
                int topOffsetPixel = (int) AdmobHelper.convertDpToPixel(topPadding);
                adParams.topMargin = safeInsetTop + topOffsetPixel;
            }

        }
        return adParams;
    }

    public static float convertPixelsToDp(float px) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return px / metrics.density;
    }

    public static float convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return dp * metrics.density;
    }

    private Insets getSafeInsets() {
        Insets insets = new Insets();
        if (Build.VERSION.SDK_INT < 28)
            return insets;
        Window window = AdmobHelper.getCurrentActivity().getWindow();
        if (window == null)
            return insets;
        WindowInsets windowInsets = window.getDecorView().getRootWindowInsets();
        if (windowInsets == null)
            return insets;
        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        if (displayCutout == null)
            return insets;
        insets.top = displayCutout.getSafeInsetTop();
        insets.left = displayCutout.getSafeInsetLeft();
        insets.bottom = displayCutout.getSafeInsetBottom();
        insets.right = displayCutout.getSafeInsetRight();
        return insets;
    }

    private static int getLayoutGravityForPositionCode(int positionCode) {
        switch (positionCode) {
            case POSITION_TOP_CENTER:
                return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            case POSITION_BOTTOM_CENTER:
                return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            case POSITION_TOP_LEFT:
                return Gravity.TOP | Gravity.START;
            case POSITION_TOP_RIGHT:
                return Gravity.TOP | Gravity.END;
            case POSITION_BOTTOM_LEFT:
                return Gravity.BOTTOM | Gravity.START;
            case POSITION_BOTTOM_RIGHT:
                return Gravity.BOTTOM | Gravity.END;
            case POSITION_CENTER:
                return Gravity.CENTER;
            case POSITION_CUSTOM:
                return Gravity.TOP | Gravity.START; // Default for custom positioning
            default:
                throw new IllegalArgumentException("Invalid ad position code: " + positionCode);
        }
    }

    public synchronized void SetAdsCallback(IAdmobAdListener callback) {
        adsEventCallback = callback;
    }

    public void initMrec(Activity activity, String adUnitId, String position) {
        _mrecAdsId = adUnitId;
        int positionCode = Integer.parseInt(position);
        CreateMrecAdView(activity, _mrecAdsId, positionCode);
    }

    public void setMrecPosition(int positionCode, int offsetY) {
        this.updateMrecPosition(positionCode, offsetY);
    }

    public void loadMrec() {
        synchronized (this) {
            if (mrecAdLoading || mrecAdLoaded) return;
            mrecAdLoading = true;
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView mrec = getMrecAdView();
                if (mrec != null) {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    mrec.loadAd(adRequest);
                } else {
                    Log.d(TAG, "loadMrec: mrecAdView is null");
                    synchronized (this) {
                        mrecAdLoading = false; // Reset flag if view is null
                    }
                }
            });
        } else {
            Log.w(TAG, "Cannot load MREC - no valid activity");
            synchronized (this) {
                mrecAdLoading = false; // Reset flag if no valid activity
            }
        }
    }

    public void ShowMrec() {
        // Set logical state to visible
        mrecLogicallyVisible = true;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView mrec = getMrecAdView();
                if (mrec != null && mrecAdLoaded && mrec.getVisibility() == View.GONE) {
                    Log.d(TAG, "ShowMrec - ad loaded, showing now");
                    mrec.setVisibility(View.VISIBLE);
                    mrec.resume();
                } else if (mrec != null && !mrecAdLoaded) {
                    Log.d(TAG, "ShowMrec - ad not loaded yet, will auto-show when loaded");
                }
            });
        }
    }

    public void HideMrec() {
        // Set logical state to hidden
        mrecLogicallyVisible = false;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView mrec = getMrecAdView();
                if (mrec != null && mrec.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "HideMrec - hiding now");
                    mrec.setVisibility(View.GONE);
                    mrec.pause();
                } else if (mrec != null && !mrecAdLoaded) {
                    Log.d(TAG, "HideMrec - ad not loaded yet, will stay hidden when loaded");
                }
            });
        }
    }

    /**
     * Check if Banner ad is loaded and ready to display
     * @return true if Banner ad is loaded, false otherwise
     */
    public boolean isBannerLoaded() {
        return bannerAdLoaded;
    }

    /**
     * Check if MREC ad is loaded and ready to display
     * @return true if MREC ad is loaded, false otherwise
     */
    public boolean isMrecLoaded() {
        return mrecAdLoaded;
    }

    // ==================== INTERSTITIAL AD METHODS ====================
    
    /**
     * Initialize Interstitial Ad with ad unit ID
     * @param activity Current activity context
     * @param adUnitId Ad unit ID for interstitial ad
     */
    public void initInterstitial(Activity activity, String adUnitId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot init interstitial - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        _interstitialAdsId = adUnitId;
        Log.d(TAG, "Interstitial initialized with ad unit ID: " + adUnitId);
    }
    
    /**
     * Load interstitial ad
     * Follows Google's best practice: check if ad is already loaded before loading
     */
    public void loadInterstitial() {
        if (_interstitialAdsId == null) {
            Log.w(TAG, "Interstitial ad unit ID is not set. Call initInterstitial() first.");
            return;
        }
        
        synchronized (this) {
            // Don't reload if already loaded or currently loading
            if (interstitialAdLoading || interstitialAdLoaded) {
                Log.d(TAG, "Interstitial ad is already loading or loaded");
                return;
            }
            interstitialAdLoading = true;
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot load interstitial - no valid activity");
            synchronized (this) {
                interstitialAdLoading = false;
            }
            return;
        }
        
        runSafelyOnUiThread(currentActivity, () -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            InterstitialAd.load(
                currentActivity,
                _interstitialAdsId,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.d(TAG, "Interstitial ad loaded");
                        synchronized (AdmobHelper.this) {
                            interstitialAdLoading = false;
                            interstitialAdLoaded = true;
                        }
                        setInterstitialAd(ad);
                        
                        // Reset retry counter on successful load
                        resetInterstitialRetryCounter();
                        
                        // Set OnPaidEventListener
                        ad.setOnPaidEventListener(adValue -> {
                            InterstitialAd interstitial = getInterstitialAd();
                            if (interstitial == null) return;
                            
                            ResponseInfo responseInfo = interstitial.getResponseInfo();
                            String adSourceName = "admob";
                            if (responseInfo != null) {
                                AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
                                if (loadedAdapterResponseInfo != null) {
                                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                                    Log.d(TAG, "INTERSTITIAL loadedAdapterResponseInfo\nadSourceName: " + adSourceName);
                                }
                            }
                            
                            onAdPaid("INTERSTITIAL", adValue, _interstitialAdsId, adSourceName);
                        });
                        
                        IAdmobAdListener callback = adsEventCallback;
                        if (callback != null) {
                            callback.onInterstitialLoaded();
                        }
                        
                        ResponseInfo responseInfo = ad.getResponseInfo();
                        if (responseInfo != null) {
                            Log.d(TAG, "INTERSTITIAL adapter class name: " + responseInfo.getMediationAdapterClassName());
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Failed to load interstitial: " + loadAdError.getMessage());
                        synchronized (AdmobHelper.this) {
                            interstitialAdLoading = false;
                            interstitialAdLoaded = false;
                        }
                        setInterstitialAd(null);
                        
                        // Schedule auto-reload with exponential backoff
                        scheduleInterstitialReload();
                    }
                }
            );
        });
    }
    
    /**
     * Show interstitial ad
     * Follows Google's best practice: check if ad is ready before showing
     * Thread-safe implementation with double-check pattern
     */
    public void showInterstitial(int interstitialCode) {
        if (!interstitialAdsEnabled) {
            Log.d(TAG, "Interstitial ads are disabled, cannot show");
            return;
        }
        
        // Check interval requirement
        if (interstitialIntervalMs > 0 && lastInterstitialDismissedTime > 0) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastDismissed = currentTime - lastInterstitialDismissedTime;
            
            if (timeSinceLastDismissed < interstitialIntervalMs) {
                long remainingMs = interstitialIntervalMs - timeSinceLastDismissed;
                Log.d(TAG, "Interstitial interval not met. Remaining time: " + (remainingMs / 1000) + " seconds");
                return;
            }
        }
        
        // Thread-safe check with synchronized block
        InterstitialAd ad;
        synchronized (this) {
            ad = interstitialAd;
            if (ad == null || !interstitialAdLoaded) {
                Log.d(TAG, "Interstitial ad is not ready yet");
                // Preload for next time
                loadInterstitial();
                return;
            }
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot show interstitial - no valid activity");
            return;
        }
        
        // Store interstitial code for callback
        currentInterstitialCode = interstitialCode;
        
        runSafelyOnUiThread(currentActivity, () -> {
            // Double-check in UI thread with synchronized access
            InterstitialAd interstitial;
            synchronized (AdmobHelper.this) {
                interstitial = interstitialAd;
                if (interstitial == null) {
                    Log.d(TAG, "Interstitial ad is null in UI thread");
                    return;
                }
            }
            
            // Set FullScreenContentCallback
            interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed");
                    IAdmobAdListener callback = adsEventCallback;
                    if (callback != null) {
                        callback.onAdDisplayFullScreenContent(1); // 1 for interstitial
                    }
                }
                
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed with code: " + currentInterstitialCode);
                    
                    // Record dismiss time for interval tracking
                    lastInterstitialDismissedTime = System.currentTimeMillis();
                    
                    // Clear reference in synchronized block
                    setInterstitialAd(null); // This also sets interstitialAdLoaded = false
                    
                    IAdmobAdListener callback = adsEventCallback;
                    if (callback != null) {
                        callback.onAdDismissedFullScreenContent(1); // 1 for interstitial
                        callback.onInterstitialDismissed(currentInterstitialCode); // Pass interstitial code
                    }
                    
                    // Preload next interstitial
                    loadInterstitial();
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Interstitial failed to show: " + adError.getMessage());
                    // Clear reference in synchronized block
                    setInterstitialAd(null); // This also sets interstitialAdLoaded = false
                    
                    // Try to load again
                    loadInterstitial();
                }
                
                @Override
                public void onAdImpression() {
                    Log.d(TAG, "Interstitial impression recorded");
                }
                
                @Override
                public void onAdClicked() {
                    Log.d(TAG, "Interstitial clicked");
                    IAdmobAdListener callback = adsEventCallback;
                    if (callback != null) {
                        callback.onAdClicked();
                    }
                }
            });
            
            // Show the ad
            interstitial.show(currentActivity);
        });
    }
    
    /**
     * Check if interstitial ad is ready to show
     * Thread-safe check with synchronized access
     * @return true if ad is loaded and ready
     */
    public synchronized boolean isInterstitialReady() {
        return interstitialAdLoaded && interstitialAd != null;
    }
    
    /**
     * Disable interstitial ads
     * When disabled, interstitial ads will not load or show
     */
    public void disableInterstitial() {
        interstitialAdsEnabled = false;
        Log.d(TAG, "Interstitial ads disabled");
    }
    
    /**
     * Enable interstitial ads
     * When enabled, interstitial ads can load and show normally
     */
    public void enableInterstitial() {
        interstitialAdsEnabled = true;
        Log.d(TAG, "Interstitial ads enabled");
    }
    
    /**
     * Check if interstitial ads are currently enabled
     * @return true if enabled, false if disabled
     */
    public boolean isInterstitialEnabled() {
        return interstitialAdsEnabled;
    }
    
    /**
     * Set minimum interval between interstitial ads
     * The interval is measured from when the previous ad was dismissed
     * @param intervalMs Minimum interval in milliseconds (0 = no interval limit)
     */
    public void setInterstitialInterval(long intervalMs) {
        interstitialIntervalMs = intervalMs;
        Log.d(TAG, "Interstitial interval set to: " + (intervalMs / 1000) + " seconds");
    }
    
    /**
     * Get current interstitial interval setting
     * @return Current interval in milliseconds
     */
    public long getInterstitialInterval() {
        return interstitialIntervalMs;
    }
    
    /**
     * Get remaining time until next interstitial can be shown
     * @return Remaining time in milliseconds (0 if can show now)
     */
    public long getRemainingInterstitialInterval() {
        if (interstitialIntervalMs <= 0 || lastInterstitialDismissedTime <= 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastDismissed = currentTime - lastInterstitialDismissedTime;
        long remaining = interstitialIntervalMs - timeSinceLastDismissed;
        
        return remaining > 0 ? remaining : 0;
    }
    
    /**
     * Check if interstitial can be shown based on interval
     * @return true if interval requirement is met or no interval is set
     */
    public boolean canShowInterstitialByInterval() {
        if (interstitialIntervalMs <= 0) {
            return true; // No interval limit
        }
        
        if (lastInterstitialDismissedTime <= 0) {
            return true; // No previous ad shown
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastDismissed = currentTime - lastInterstitialDismissedTime;
        
        return timeSinceLastDismissed >= interstitialIntervalMs;
    }
    
    /**
     * Reset interstitial interval timer
     * Useful when you want to manually reset the timer (e.g., after level complete)
     */
    public void resetInterstitialInterval() {
        lastInterstitialDismissedTime = 0;
        Log.d(TAG, "Interstitial interval timer reset");
    }

    // ==================== REWARDED AD METHODS ====================
    
    /**
     * Initialize Rewarded Ad with ad unit ID
     * @param activity Current activity context
     * @param adUnitId Ad unit ID for rewarded ad
     */
    public void initRewarded(Activity activity, String adUnitId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot init rewarded - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        _rewardedAdsId = adUnitId;
        Log.d(TAG, "Rewarded initialized with ad unit ID: " + adUnitId);
    }
    
    /**
     * Load rewarded ad
     * Follows Google's best practice: check if ad is already loaded before loading
     */
    public void loadRewarded() {
        if (_rewardedAdsId == null) {
            Log.w(TAG, "Rewarded ad unit ID is not set. Call initRewarded() first.");
            return;
        }
        
        synchronized (this) {
            // Don't reload if already loaded or currently loading
            if (rewardedAdLoading || rewardedAdLoaded) {
                Log.d(TAG, "Rewarded ad is already loading or loaded");
                return;
            }
            rewardedAdLoading = true;
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot load rewarded - no valid activity");
            synchronized (this) {
                rewardedAdLoading = false;
            }
            return;
        }
        
        runSafelyOnUiThread(currentActivity, () -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            RewardedAd.load(
                currentActivity,
                _rewardedAdsId,
                adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        Log.d(TAG, "Rewarded ad loaded");
                        synchronized (AdmobHelper.this) {
                            rewardedAdLoading = false;
                            rewardedAdLoaded = true;
                        }
                        setRewardedAd(ad);
                        
                        // Reset retry counter on successful load
                        resetRewardedRetryCounter();
                        
                        // Set OnPaidEventListener
                        ad.setOnPaidEventListener(adValue -> {
                            RewardedAd rewarded = getRewardedAd();
                            if (rewarded == null) return;
                            
                            ResponseInfo responseInfo = rewarded.getResponseInfo();
                            String adSourceName = "admob";
                            if (responseInfo != null) {
                                AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
                                if (loadedAdapterResponseInfo != null) {
                                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                                    Log.d(TAG, "REWARDED loadedAdapterResponseInfo\nadSourceName: " + adSourceName);
                                }
                            }
                            
                            onAdPaid("REWARDED", adValue, _rewardedAdsId, adSourceName);
                        });
                        
                        IAdmobAdListener callback = adsEventCallback;
                        if (callback != null) {
                            callback.onRewardedLoaded();
                        }
                        
                        ResponseInfo responseInfo = ad.getResponseInfo();
                        if (responseInfo != null) {
                            Log.d(TAG, "REWARDED adapter class name: " + responseInfo.getMediationAdapterClassName());
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Failed to load rewarded: " + loadAdError.getMessage());
                        synchronized (AdmobHelper.this) {
                            rewardedAdLoading = false;
                            rewardedAdLoaded = false;
                        }
                        setRewardedAd(null);
                        
                        // Schedule auto-reload with exponential backoff
                        scheduleRewardedReload();
                    }
                }
            );
        });
    }
    
    /**
     * Show rewarded ad with reward listener (backward compatibility)
     * User must complete the ad to receive reward
     * Follows Google's best practice: check if ad is ready before showing
     */
    public void showRewarded() {
        showRewarded(0); // Default reward code
    }
    
    /**
     * Show rewarded ad with reward listener and custom reward code
     * User must complete the ad to receive reward
     * Follows Google's best practice: check if ad is ready before showing
     * Thread-safe implementation with double-check pattern
     * @param rewardCode Custom code to identify reward type, will be passed back in onUserEarnedReward
     */
    public void showRewarded(int rewardCode) {
        // Thread-safe check with synchronized block
        RewardedAd ad;
        synchronized (this) {
            ad = rewardedAd;
            if (ad == null || !rewardedAdLoaded) {
                Log.d(TAG, "Rewarded ad is not ready yet");
                // Preload for next time
                loadRewarded();
                return;
            }
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot show rewarded - no valid activity");
            return;
        }
        
        // Store reward code for callback
        currentRewardCode = rewardCode;
        
        runSafelyOnUiThread(currentActivity, () -> {
            // Double-check in UI thread with synchronized access
            RewardedAd rewarded;
            synchronized (AdmobHelper.this) {
                rewarded = rewardedAd;
                if (rewarded == null) {
                    Log.d(TAG, "Rewarded ad is null in UI thread");
                    return;
                }
            }
            
            // Set FullScreenContentCallback
            rewarded.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed");
                    IAdmobAdListener callback = adsEventCallback;
                    if (callback != null) {
                        callback.onAdDisplayFullScreenContent(2); // 2 for rewarded
                    }
                }
                
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed");
                    // Clear reference in synchronized block
                    setRewardedAd(null); // Single-use: this also sets rewardedAdLoaded = false
                    
                    IAdmobAdListener callback = adsEventCallback;
                    if (callback != null) {
                        callback.onAdDismissedFullScreenContent(2); // 2 for rewarded
                    }
                    
                    // Preload next rewarded ad
                    loadRewarded();
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Rewarded failed to show: " + adError.getMessage());
                    // Clear reference in synchronized block
                    setRewardedAd(null); // This also sets rewardedAdLoaded = false
                    
                    // Try to load again
                    loadRewarded();
                }
                
                @Override
                public void onAdImpression() {
                    Log.d(TAG, "Rewarded impression recorded");
                }
                
                @Override
                public void onAdClicked() {
                    Log.d(TAG, "Rewarded clicked");
                    IAdmobAdListener callback = adsEventCallback;
                    if (callback != null) {
                        callback.onAdClicked();
                    }
                }
            });
            
            // Show the ad with reward listener
            rewarded.show(currentActivity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // User earned the reward
                    int amount = rewardItem.getAmount();
                    String type = rewardItem.getType();
                    Log.d(TAG, "User earned reward (code: " + currentRewardCode + "): " + amount + " " + type);
                    
                    IAdmobAdListener callback = adsEventCallback;
                    if (callback != null) {
                        callback.onUserEarnedReward(currentRewardCode, type, amount);
                    }
                }
            });
        });
    }
    
    /**
     * Check if rewarded ad is ready to show
     * Thread-safe check with synchronized access
     * @return true if ad is loaded and ready
     */
    public synchronized boolean isRewardedReady() {
        return rewardedAdLoaded && rewardedAd != null;
    }

    public void bypassConsentFlow(Activity activity) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext());
        googleMobileAdsConsentManager.bypassConsentFlow();
        consentCode = 0;

        //force init sdk
        initializeMobileAdsSdk(activity);
    }

    public int getConsentCode() {
        return consentCode;
    }

    public void startConsentFlow(Activity activity, IGoogleConsentCallback consentCallback) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext());
        googleMobileAdsConsentManager.gatherConsent(activity, consentError -> {
            if (consentError != null) {
                consentCode = 0;
                // Consent not obtained in current session.
                Log.w(TAG, String.format("%s: %s", consentError.getErrorCode(), consentError.getMessage()));
                if (consentCallback != null) {
                    consentCallback.onFinish(0);
                }
            } else {
                consentCode = 1;
                Log.d(TAG, "Consent Flow: FINISH----------------------");
                if (consentCallback != null) consentCallback.onFinish(1);
            }

            if (googleMobileAdsConsentManager.canRequestAds()) {
                initializeMobileAdsSdk(activity);
            }
        });

        // attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk(activity);
        }
    }

    // Show a privacy options button if required.
    public boolean isPrivacySettingsButtonEnabled() {
        return consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    private void CreateMrecAdView(Activity activity, String adUnitId, int positionCode) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot create MREC - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        
        AdView mrecView = new AdView(activity);
        mrecView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        mrecView.setAdUnitId(adUnitId);
        mrecView.setVisibility(View.GONE);
        mrecView.setDescendantFocusability(393216);
        activity.addContentView(mrecView, (ViewGroup.LayoutParams) getLayoutParams(positionCode, 0));

        mrecView.setOnPaidEventListener(adValue -> {
            AdView mrec = getMrecAdView();
            if (mrec == null) return;
            
            ResponseInfo responseInfo = mrec.getResponseInfo();
            if (responseInfo == null) return;
            
            // Get the ad unit ID.
            AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
            String adSourceName = "admob";
            if (loadedAdapterResponseInfo != null) {
                adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                Log.d(TAG, "MREC loadedAdapterResponseInfo" + "\nadSourceName" + adSourceName);

            }

            onAdPaid("MREC", adValue, _mrecAdsId, adSourceName);

        });

        mrecView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                synchronized (AdmobHelper.this) {
                    mrecAdLoading = false;
                    mrecAdLoaded = true;
                }
                
                // Reset retry counter on successful load
                resetMrecRetryCounter();

                AdView mrec = getMrecAdView();
                if (mrec != null && mrec.getResponseInfo() != null) {
                    Log.d(TAG, "MREC adapter class name: " + mrec.getResponseInfo().getMediationAdapterClassName());
                }
                
                // Check logical state and apply visibility accordingly
                if (mrec != null) {
                    if (mrecLogicallyVisible) {
                        Log.d(TAG, "MREC onAdLoaded - logical state is SHOW, displaying ad");
                        mrec.setVisibility(View.VISIBLE);
                        mrec.resume();
                    } else {
                        Log.d(TAG, "MREC onAdLoaded - logical state is HIDE, keeping ad hidden");
                        mrec.setVisibility(View.GONE);
                    }
                }
                
                IAdmobAdListener callback = adsEventCallback;
                if (callback != null) {
                    callback.onMrecLoaded();
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                IAdmobAdListener callback = adsEventCallback;
                if (callback != null) callback.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                synchronized (AdmobHelper.this) {
                    mrecAdLoading = false;
                }
                Log.d(TAG, "MREC onAdFailedToLoad" + "\nloadAdError: " + loadAdError.getMessage());

                // Schedule auto-reload with exponential backoff
                scheduleMrecReload();
            }
        });
        
        // Store in WeakReference to prevent memory leak
        setMrecAdView(mrecView);
    }

    private int dpToPx(Context var0, @Dimension(unit = 0) int var1) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) var1, var0.getResources().getDisplayMetrics());
    }

    private void onAdPaid(String adFormat, AdValue adValue, String adUnitId, String adSourceName) {
        // Extract the impression-level ad revenue data.
        double value = (double) adValue.getValueMicros() / 1000000;
        String currencyCode = adValue.getCurrencyCode();
        int precision = adValue.getPrecisionType();

        Log.d(TAG, "Ads on Paid Event " + "\nvalueMicros" + value + ", currencyCode: " + currencyCode + " ,precision: " + precision + " ,adUnitId: " + adUnitId + " ,adSourceName" + adSourceName);

        // Thread-safe callback invocation
        IAdmobAdListener callback = adsEventCallback;
        if (callback != null) {
            callback.onAdImpression(adFormat, adUnitId, adSourceName, value);
        }
    }

    public void loadAd(Activity activity) {
        // Thread-safe check with synchronization
        synchronized (this) {
            if (_appOpenAdsId == null || _isLoadingAd || isAdAvailable()) {
                return;
            }
            _isLoadingAd = true;
        }

        Log.d(TAG, "Start Load ads.");
        AdRequest request = new AdRequest.Builder().build();

        AppOpenAd.load(getCurrentActivity().getApplicationContext(), _appOpenAdsId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(AppOpenAd appOpenAd) {
                Log.d(TAG, "App Open Ads was loaded.");
                synchronized (AdmobHelper.this) {
                    _isLoadingAd = false;
                    _appOpenAd = appOpenAd;
                    loadTime = (new Date()).getTime();
                }
                
                _appOpenAd.setOnPaidEventListener(new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(AdValue adValue) {
                        // Get the ad unit ID.
                        String adUnitId = _appOpenAd.getAdUnitId();

                        AdapterResponseInfo loadedAdapterResponseInfo = _appOpenAd.getResponseInfo().getLoadedAdapterResponseInfo();
                        String adSourceName = "admob";
                        if (loadedAdapterResponseInfo != null) {
                            adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                            Log.d(TAG, "App Open Ads loadedAdapterResponseInfo" + "\nadSourceName" + adSourceName);

                        }

                        onAdPaid("App open", adValue, adUnitId, adSourceName);

                    }
                });

                Log.d(TAG, "Banner adapter class name: " + Objects.requireNonNull(_appOpenAd.getResponseInfo()).getMediationAdapterClassName());

                if (needShowAOAAfterLoad) {
                    showAppOpenAds(getCurrentActivity());
                    needShowAOAAfterLoad = false;
                }
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.d(TAG, "App open ad has failed to load.");
                _isLoadingAd = false;
                if (adsEventCallback != null) {
                    adsEventCallback.onAOAFailedToLoad();
                }
            }
        });
    }

    /**
     * Check if ad exists and can be shown.
     */
    public boolean isAdAvailable() {
        return _appOpenAd != null && wasLoadTimeLessThanNHoursAgo(1);
    }

    /**
     * Utility method to check if ad was loaded more than n hours ago.
     */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    private volatile boolean needShowAOAAfterLoad;

    public boolean showAppOpenAds(Activity activity) {
        if (_isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.");
            return false;
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready");
//            needShowAOAAfterLoad = true;
            loadAd(getCurrentActivity());
            return false;
        }

        if (aoaBlocker) {
            Log.d(TAG, "AOA IS BLOCKED!");
            return false;
        }

        if(blockAOACount > 0) {
            Log.d(TAG, "AOA IS BLOCKED! (COUNT SHOW AOA: " + blockAOACount + ")");
            blockAOACount -=1;
            return false;
        }

        _appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adsEventCallback != null)
                    adsEventCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                // Set the reference to null so isAdAvailable() returns false.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                _appOpenAd = null;
                _isShowingAd = false;
                adsEventCallback.onAdDismissedFullScreenContent(0);
                loadAd(getCurrentActivity());

                ThreadUltils.startTask(() -> {
                    Log.d(TAG, "Reset AOA Time-Block");
                    aoaBlocker = false;
                }, 1 * 1000L);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when fullscreen content failed to show.
                // Set the reference to null so isAdAvailable() returns false.
                Log.d(TAG, adError.getMessage());
                _appOpenAd = null;
                _isShowingAd = false;
                loadAd(getCurrentActivity());
            }

            @Override
            public void onAdImpression() {

            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
                aoaBlocker = true;
                if (adsEventCallback != null)
                    adsEventCallback.onAdDisplayFullScreenContent(0);
            }
        });

        _isShowingAd = true;
        _appOpenAd.show(activity);
        return true;
    }

    public boolean canShowAOA() {
        if (_isDisableResumeAds || blockAOACount > 0) return false;
        return true;
    }

    public void DisableAOA() {
        _isDisableResumeAds = true;
    }

    public void EnableAOA() {
        _isDisableResumeAds = false;
    }

    public synchronized void IncreaseBlockAOA() {
        blockAOACount += 1;
    }

    public synchronized void DecreaseBlockAOA() {
        blockAOACount -= 1;
        if (blockAOACount < 0) blockAOACount = 0;
    }

    private void initializeMobileAdsSdk(Activity activity) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(activity.getApplicationContext(), initializationStatus -> {
            if (BuildConfig.DEBUG) {
                Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                for (String adapterClass : statusMap.keySet()) {
                    AdapterStatus status = statusMap.get(adapterClass);
                    assert status != null;
                    Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d", adapterClass, status.getDescription(), status.getLatency()));
                }

                MobileAds.openAdInspector(activity.getApplicationContext(), new OnAdInspectorClosedListener() {
                    @Override
                    public void onAdInspectorClosed(@Nullable AdInspectorError adInspectorError) {

                    }
                });
            }
            loadMrec();
            loadBanner(true, false);
            loadInterstitial();
            loadRewarded();

            if (adsEventCallback != null) {
                adsEventCallback.onAdInitialized(null);
            }
        });
    }

    /**
     * Cleanup method to be called when the helper is no longer needed
     * Should be called in Activity's onDestroy()
     */
    public void cleanup() {
        synchronized (this) {
            // Stop any pending refresh operations
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
            
            // Cancel all pending retry tasks
            bannerRetryHandler.removeCallbacksAndMessages(null);
            mrecRetryHandler.removeCallbacksAndMessages(null);
            interstitialRetryHandler.removeCallbacksAndMessages(null);
            rewardedRetryHandler.removeCallbacksAndMessages(null);
            
            // Reset retry counters
            bannerRetryCount = 0;
            mrecRetryCount = 0;
            interstitialRetryCount = 0;
            rewardedRetryCount = 0;
            
            // Destroy AdViews to release resources
            AdView banner = getBannerAdView();
            if (banner != null) {
                banner.destroy();
            }
            
            AdView mrec = getMrecAdView();
            if (mrec != null) {
                mrec.destroy();
            }
            
            // Clear ad references to prevent memory leaks
            _appOpenAd = null;
            setBannerAdView(null);
            setMrecAdView(null);
            setInterstitialAd(null); // Clears strong reference
            setRewardedAd(null); // Clears strong reference
            setCurrentActivity(null);
            
            // Clear callbacks
            adsEventCallback = null;
            
            // Reset states
            _isLoadingAd = false;
            _isShowingAd = false;
            bannerAdLoading = false;
            bannerAdLoaded = false;
            mrecAdLoading = false;
            mrecAdLoaded = false;
            interstitialAdLoading = false;
            interstitialAdLoaded = false;
            rewardedAdLoading = false;
            rewardedAdLoaded = false;
            
            // Reset logical state variables
            bannerLogicallyVisible = false;
            mrecLogicallyVisible = false;
            
            Log.d(TAG, "AdmobHelper cleaned up");
        }
    }
    
    /**
     * Pause ads when activity goes to background
     */
    public void onPause() {
        AdView banner = getBannerAdView();
        if (banner != null) {
            banner.pause();
        }
        
        AdView mrec = getMrecAdView();
        if (mrec != null) {
            mrec.pause();
        }
    }
    
    /**
     * Resume ads when activity comes to foreground
     */
    public void onResume() {
        AdView banner = getBannerAdView();
        if (banner != null) {
            banner.resume();
        }
        
        AdView mrec = getMrecAdView();
        if (mrec != null) {
            mrec.resume();
        }
    }
}

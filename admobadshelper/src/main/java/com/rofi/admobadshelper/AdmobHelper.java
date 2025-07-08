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
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.ump.ConsentInformation;
import com.rofi.base.Constants;
import com.rofi.base.ThreadUltils;
import com.unity3d.player.UnityPlayer;

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

    // UI components - should only be accessed from UI thread
    private volatile AdView mrecAdView;
    private volatile AdView cBannerView;

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
    
    // Thread-safe boolean flags using volatile
    private volatile boolean mrecAdLoading;
    private volatile boolean bannerAdLoading;
    private volatile boolean mrecAdLoaded;
    private volatile boolean bannerAdLoaded;
    
    // Thread-safe counters using volatile
    private volatile int blockAOACount;

    private volatile boolean _isDisableResumeAds;
    private volatile boolean aoaBlocker;

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
    }

    public void initBanner(Activity activity, String id, int position) {
        _cBannerId = id;
        bannerPosition = position;

        cBannerView = new AdView(activity);
        cBannerView.setAdSize(getBannerAdSize(activity));
//        cBannerView.setAdSize(AdSize.BANNER);
        cBannerView.setAdUnitId(_cBannerId);
        cBannerView.setVisibility(View.GONE);

        cBannerView.setOnPaidEventListener(adValue -> {
            ResponseInfo responseInfo = cBannerView.getResponseInfo();
            String adSourceName = "admob";
            if (responseInfo != null) {
                AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();

                if (loadedAdapterResponseInfo != null) {
                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                    Log.d(TAG, "BANNER loadedAdapterResponseInfo" + "\nadSourceName" + adSourceName);

                }
            }
            // Get the ad unit ID.
            onAdPaid("COLLAPSIBLE_BANNER", adValue, _cBannerId, adSourceName);
        });

        cBannerView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                bannerAdLoading = false;
                bannerAdLoaded = true;
                Log.d(TAG, "BANNER onAdLoaded");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adsEventCallback != null) {
                    adsEventCallback.onAdClicked();
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
        cBannerView.setLayoutParams(layoutParams);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(cBannerView);
    }

    Handler handler = new Handler(Looper.getMainLooper()); // Thread-safe handler

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
                            loadBanner(false);
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

    public void ForceLoadBanner() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, new Runnable() {
                @Override
                public void run() {
                    loadBanner(true);
                }
            });
        }
    }

    public void StopRefresh() {
        handler.removeCallbacksAndMessages(null);
    }

    private void loadBanner(boolean isForceLoad) {
        if (_cBannerId == null || cBannerView == null) return;

        synchronized (this) {
            if (!isForceLoad && bannerAdLoading) return;
            if (!isForceLoad && bannerAdLoaded) return;
            bannerAdLoading = true;
        }

        // Ensure UI operations run on main thread
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                // Create an extra parameter that aligns the bottom of the expanded ad to
                // the bottom of the bannerView.
                Bundle extras = new Bundle();
                extras.putString("collapsible", bannerPosition == Constants.POSITION_CENTER_TOP ? "top" : "bottom");

                AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();

                if (cBannerView != null) {
                    cBannerView.loadAd(adRequest);
                    Log.d(TAG, "Loading Banner");
                } else {
                    bannerAdLoading = false; // Reset flag if view is null
                }
            });
        } else {
            bannerAdLoading = false; // Reset flag if no valid activity
        }
    }

    public void showBanner() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                if (bannerAdLoaded && cBannerView != null && cBannerView.getVisibility() == View.GONE) {
                    Log.d(TAG, "showBanner");
                    cBannerView.resume();
                    cBannerView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void HideBanner() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                if (bannerAdLoaded && cBannerView != null && cBannerView.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "HideBanner");
                    cBannerView.pause();
                    cBannerView.setVisibility(View.GONE);
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
        if (this.mrecAdView == null) return;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, new Runnable() {
                public void run() {
                    if (mrecAdView != null) { // Double check after UI thread switch
                        FrameLayout.LayoutParams layoutParams = getLayoutParams(positionCode, topPadding);
                        mrecAdView.setLayoutParams(layoutParams);
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
                if (mrecAdView != null) {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    mrecAdView.loadAd(adRequest);
                } else {
                    Log.d(TAG, "loadMrec: mrecAdView is null");
                    mrecAdLoading = false; // Reset flag if view is null
                }
            });
        } else {
            Log.w(TAG, "Cannot load MREC - no valid activity");
            mrecAdLoading = false; // Reset flag if no valid activity
        }
    }

    public void ShowMrec() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                if (mrecAdView != null && mrecAdLoaded && mrecAdView.getVisibility() == View.GONE) {
                    mrecAdView.setVisibility(View.VISIBLE);
                    mrecAdView.resume();
                }
            });
        }
    }

    public void HideMrec() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                if (mrecAdView != null && mrecAdView.getVisibility() == View.VISIBLE) {
                    mrecAdView.setVisibility(View.GONE);
                    mrecAdView.pause();
                }
            });
        }
    }

    public void bypassConsentFlow(Activity activity) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext());
        googleMobileAdsConsentManager.bypassConsentFlow();
        consentCode = 0;
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

        // This sample attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk(activity);
        }
    }

    // Show a privacy options button if required.
    public boolean isPrivacySettingsButtonEnabled() {
        return consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    private void CreateMrecAdView(Activity activity, String adUnitId, int positionCode) {
        mrecAdView = new AdView(activity);
        mrecAdView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        mrecAdView.setAdUnitId(adUnitId);
        mrecAdView.setVisibility(View.GONE);
        mrecAdView.setDescendantFocusability(393216);
        AdmobHelper.getCurrentActivity().addContentView(mrecAdView, (ViewGroup.LayoutParams) getLayoutParams(positionCode, 0));

        mrecAdView.setOnPaidEventListener(adValue -> {
            // Get the ad unit ID.
            AdapterResponseInfo loadedAdapterResponseInfo = mrecAdView.getResponseInfo().getLoadedAdapterResponseInfo();
            String adSourceName = "admob";
            if (loadedAdapterResponseInfo != null) {
                adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                Log.d(TAG, "MREC loadedAdapterResponseInfo" + "\nadSourceName" + adSourceName);

            }

            onAdPaid("MREC", adValue, _mrecAdsId, adSourceName);

        });

        mrecAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                mrecAdLoading = false;
                mrecAdLoaded = true;

                Log.d(TAG, "Banner adapter class name: " + Objects.requireNonNull(mrecAdView.getResponseInfo()).getMediationAdapterClassName());
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adsEventCallback != null) adsEventCallback.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d(TAG, "MREC onAdFailedToLoad" + "\nloadAdError: " + loadAdError.getMessage());

                // Tải lại quảng cáo sau 30 giây
                mrecAdView.postDelayed(() -> {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    mrecAdView.loadAd(adRequest);
                }, 30000);  // 30 giây
            }
        });

//        int gravity = position == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
//        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
//        layoutParams.setMargins(0, 0, 0, 0);
//        mrecAdView.setLayoutParams(layoutParams);
//
//        ViewGroup rootView = activity.findViewById(android.R.id.content);
//        rootView.addView(mrecAdView);

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

    public void showAppOpenAds(Activity activity) {
        if (_isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.");
            return;
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready");
//            needShowAOAAfterLoad = true;
            loadAd(getCurrentActivity());
            return;
        }

        if (aoaBlocker) {
            Log.d(TAG, "AOA IS BLOCKED!");
            return;
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

            loadBanner(true);
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
            
            // Clear ad references to prevent memory leaks
            _appOpenAd = null;
            
            // Clear callbacks
            adsEventCallback = null;
            
            // Reset states
            _isLoadingAd = false;
            _isShowingAd = false;
            bannerAdLoading = false;
            bannerAdLoaded = false;
            mrecAdLoading = false;
            mrecAdLoaded = false;
            
            Log.d(TAG, "AdmobHelper cleaned up");
        }
    }
    
    /**
     * Pause ads when activity goes to background
     */
    public void onPause() {
        if (cBannerView != null) {
            cBannerView.pause();
        }
        if (mrecAdView != null) {
            mrecAdView.pause();
        }
    }
    
    /**
     * Resume ads when activity comes to foreground
     */
    public void onResume() {
        if (cBannerView != null) {
            cBannerView.resume();
        }
        if (mrecAdView != null) {
            mrecAdView.resume();
        }
    }
}

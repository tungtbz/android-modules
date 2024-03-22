package com.rofi.admobadshelper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdmobHelper {
    private static AdmobHelper mInstance = null;
    private final String TAG = AdmobHelper.class.toString();
    private AppOpenAd _appOpenAd = null;
    private boolean _isLoadingAd = false;
    private boolean _isShowingAd = false;
    private long loadTime = 0;
    private int consentCode = -1;

    private IAdmobAdListener adsEventCallback;

    AdView mrecAdView;

    AdView cBannerView;

    public static AdmobHelper getInstance() {
        if (null == mInstance) {
            mInstance = new AdmobHelper();
        }
        return mInstance;
    }

    String _appOpenAdsId;
    String _cBannerId;
    int bannerPosition;
    String _mrecAdsId;
    boolean mrecAdLoading;
    boolean bannerAdLoading;
    boolean mrecAdLoaded;
    boolean bannerAdLoaded;
    //    private IAdmobAdListener adListener;
    private ConsentInformation consentInformation;
    // Use an atomic boolean to initialize the Google Mobile Ads SDK and load ads once.
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;

    public void Init(Activity activity, IAdmobAdListener adListener, String[] args) {
        _appOpenAdsId = args[0];

//        this.adListener = adListener;
    }

    public void initBanner(Activity activity, String id, int position) {
        _cBannerId = id;
        bannerPosition = position;

        cBannerView = new AdView(activity);
        cBannerView.setAdSize(AdSize.BANNER);
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
        });

        int gravity = bannerPosition == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);

        layoutParams.setMargins(0, 0, 0, 0);
        cBannerView.setLayoutParams(layoutParams);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(cBannerView);
    }

    private void loadBanner() {
        if (_cBannerId == null) return;
        if (cBannerView == null) return;
        if (bannerAdLoading) return;
        if (bannerAdLoaded) return;

        // Create an extra parameter that aligns the bottom of the expanded ad to
        // the bottom of the bannerView.
        Bundle extras = new Bundle();
        extras.putString("collapsible", bannerPosition == Constants.POSITION_CENTER_TOP ? "top" : "bottom");
        extras.putString("collapsible_request_id", UUID.randomUUID().toString());

        AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();

        cBannerView.loadAd(adRequest);
        bannerAdLoading = true;
    }

    public void showBanner() {
        if (bannerAdLoaded && cBannerView.getVisibility() == View.GONE) {
            Log.d(TAG, "showBanner");
            cBannerView.setVisibility(View.VISIBLE);
        }
    }

    public void HideBanner() {
        if (bannerAdLoaded && cBannerView.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "HideBanner");
            cBannerView.setVisibility(View.GONE);
        }
    }

    private AdSize getAdSize(Activity activity, View view) {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = view.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity.getApplicationContext(), adWidth);
    }

    public void SetAdsCallback(IAdmobAdListener callback) {
        adsEventCallback = callback;
    }

    public void initMrec(Activity activity, String adUnitId, String position) {
        _mrecAdsId = adUnitId;
        CreateMrecAdView(activity, _mrecAdsId, Integer.parseInt(position));
    }

    public void loadMrec() {
        if (mrecAdView != null) {
            if (mrecAdLoading) return;
            if (mrecAdLoaded) return;

            mrecAdLoading = true;

            AdRequest adRequest = new AdRequest.Builder().build();
            mrecAdView.loadAd(adRequest);
        } else {
            Log.d(TAG, "loadMrec: NULLLLLLL");
        }
    }

    public void ShowMrec() {
        if (mrecAdLoaded && mrecAdView.getVisibility() == View.GONE) {
            mrecAdView.setVisibility(View.VISIBLE);
        }
    }

    public void HideMrec() {
        if (mrecAdView.getVisibility() == View.VISIBLE) {
            mrecAdView.setVisibility(View.GONE);
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

    private void CreateMrecAdView(Activity activity, String adUnitId, int position) {
        mrecAdView = new AdView(activity);
        mrecAdView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        mrecAdView.setAdUnitId(adUnitId);
        mrecAdView.setVisibility(View.GONE);

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
            }
        });

        int gravity = position == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
        layoutParams.setMargins(0, 0, 0, 0);
        mrecAdView.setLayoutParams(layoutParams);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(mrecAdView);

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

        adsEventCallback.onAdImpression(adFormat, adUnitId, adSourceName, value);
    }


    /**
     * Request an ad.
     */
    public void loadAd(Activity activity) {
        // Do not load ad if there is an unused ad or one is already loading.
        if (_isLoadingAd || isAdAvailable()) {
            return;
        }

        _isLoadingAd = true;
        Log.d(TAG, "Start Load ads.");
        AdRequest request = new AdRequest.Builder().build();

        AppOpenAd.load(activity.getApplicationContext(), _appOpenAdsId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(AppOpenAd appOpenAd) {
                Log.d(TAG, "App Open Ads was loaded.");
                _isLoadingAd = false;
                _appOpenAd = appOpenAd;
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

                loadTime = (new Date()).getTime();

                Log.d(TAG, "Banner adapter class name: " + Objects.requireNonNull(_appOpenAd.getResponseInfo()).getMediationAdapterClassName());
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.d(TAG, "App open ad has failed to load.");
                _isLoadingAd = false;
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

    public void showAppOpenAds(Activity activity) {
        if (_isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.");
            return;
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready");
            return;
        }

        _appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adsEventCallback != null) adsEventCallback.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                // Set the reference to null so isAdAvailable() returns false.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                _appOpenAd = null;
                _isShowingAd = false;
                adsEventCallback.onAdDismissedFullScreenContent(0);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when fullscreen content failed to show.
                // Set the reference to null so isAdAvailable() returns false.
                Log.d(TAG, adError.getMessage());
                _appOpenAd = null;
                _isShowingAd = false;
            }

            @Override
            public void onAdImpression() {

            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
            }
        });

        _isShowingAd = true;
        _appOpenAd.show(activity);
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

            loadBanner();
        });
    }
}

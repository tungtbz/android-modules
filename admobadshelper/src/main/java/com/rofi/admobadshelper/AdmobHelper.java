package com.rofi.admobadshelper;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

public class AdmobHelper {
    private static AdmobHelper mInstance = null;
    private final String TAG = AdmobHelper.class.toString();
    private AppOpenAd _appOpenAd = null;
    private boolean _isLoadingAd = false;
    private boolean _isShowingAd = false;
    private long loadTime = 0;

    public static AdmobHelper getInstance() {
        if (null == mInstance) {
            mInstance = new AdmobHelper();
        }
        return mInstance;
    }

    String _appOpenAdsId;

    public void Init(Activity activity, String[] args) {
        _appOpenAdsId = args[0];
    }

    /**
     * Request an ad.
     */
    public void loadAd(Activity activity, IAdmobAdListener adListener) {
        // Do not load ad if there is an unused ad or one is already loading.
        if (_isLoadingAd || isAdAvailable()) {
            return;
        }

        _isLoadingAd = true;
        Log.d(TAG, "Start Load ads.");
        AdRequest request = new AdRequest.Builder().build();

        AppOpenAd.load(activity.getApplicationContext(),
                _appOpenAdsId,
                request,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd appOpenAd) {
                        Log.d(TAG, "App Open Ads was loaded.");
                        _isLoadingAd = false;
                        _appOpenAd = appOpenAd;
                        _appOpenAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                // Extract the impression-level ad revenue data.
                                double value = (double) adValue.getValueMicros() / 1000000;
                                String currencyCode = adValue.getCurrencyCode();
                                int precision = adValue.getPrecisionType();

                                // Get the ad unit ID.
                                String adUnitId = _appOpenAd.getAdUnitId();
                                Log.d(TAG, "App Open Ads on Paid Event " +
                                        "\nvalueMicros" + value + ", currencyCode: " + currencyCode + " ,precision: " + precision + " ,adUnitId: " + adUnitId);

                                AdapterResponseInfo loadedAdapterResponseInfo = _appOpenAd.getResponseInfo().
                                        getLoadedAdapterResponseInfo();
                                String adSourceName = "admob";
                                if (loadedAdapterResponseInfo != null) {
                                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                                    String adSourceId = loadedAdapterResponseInfo.getAdSourceId();
                                    String adSourceInstanceName = loadedAdapterResponseInfo.getAdSourceInstanceName();
                                    String adSourceInstanceId = loadedAdapterResponseInfo.getAdSourceInstanceId();

                                    Bundle extras = _appOpenAd.getResponseInfo().getResponseExtras();
                                    String mediationGroupName = extras.getString("mediation_group_name");
                                    String mediationABTestName = extras.getString("mediation_ab_test_name");
                                    String mediationABTestVariant = extras.getString("mediation_ab_test_variant");

                                    Log.d(TAG, "App Open Ads loadedAdapterResponseInfo" +
                                            "\nadSourceName" + adSourceName + ", adSourceId: " + adSourceId + " ,precision: " + precision + " ,adUnitId: " + adUnitId);

                                }
                                adListener.onAdImpression(adUnitId, adSourceName, value);
                            }
                        });

                        loadTime = (new Date()).getTime();
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
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                // Set the reference to null so isAdAvailable() returns false.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                _appOpenAd = null;
                _isShowingAd = false;
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
}

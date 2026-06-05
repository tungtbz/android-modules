package com.rofi.analytic;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.rofi.base.Constants;

import java.util.ArrayList;
import java.util.List;

public class AnalyticServices {
    private static final String TAG = "AnalyticServices";
    private static AnalyticServices mInstace = null;
    private Activity _activityCached;

    public static AnalyticServices getInstance() {
        if (null == mInstace) {
            mInstace = new AnalyticServices();
        }
        return mInstace;
    }

    private List<IAnalytic> analyticList;
    private boolean _isInit;

    public void Init(Activity activity, String[] args) {
        if (_isInit) return;
        _activityCached = activity;

        analyticList = new ArrayList<>();
        analyticList.add(new FirebaseAnalytic());
        analyticList.add(new AppflyerAcnalytic());

        for (IAnalytic analytic : analyticList) {
            analytic.Init(activity, args);
        }

        LoadLocalData(activity);
        _isInit = true;
    }

    public void LogEvent(Activity activity, String eventName, String eventData) {
        for (IAnalytic analytic : analyticList) {
            analytic.LogEvent(activity, eventName, eventData);
        }
    }

    public void LogEventAdClicked(Activity activity, String adUnitId) {
        for (IAnalytic analytic : analyticList) {
            analytic.ClickToAd(activity, adUnitId);
        }
    }

    public void RevenueTracking(Activity activity, String adPlatform, String adFormat, String adUnitId, String adNetwork, double value) {
        for (IAnalytic analytic : analyticList) {
            analytic.RevenueTracking(activity, adPlatform, adFormat, adUnitId, adNetwork, value);
        }
    }

    int _showInterAdsCount = 0;

    private void LoadLocalData(Activity activity) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        _showInterAdsCount = sharedPref.getInt(Constants.SHOW_INTER_ADS_COUNT, 0);
        Log.d(TAG, "LoadLocalData: _showInterAdsCount: " + _showInterAdsCount);
    }

    public void AdmobAppOpenAdsRevenueTracking(Activity activity, String adSourceName, String adUnitId, double value) {
        for (IAnalytic analytic : analyticList) {
            analytic.AdmobAppOpenAdsRevenueTracking(activity, adSourceName, adUnitId, value);
        }
    }

    public void AdmobAdsRevenueTracking(Activity activity, String adFormat, String adSourceName, String adUnitId, double value) {
        for (IAnalytic analytic : analyticList) {
            analytic.AdmobAdsRevenueTracking(activity, adFormat, adSourceName, adUnitId, value);
        }
    }

    public void LogIAPRevenue(Context context, String publicKey, String signature, String originalJson, String price, String currency) {
        for (IAnalytic analytic : analyticList) {
            analytic.LogIAPRevenue(context, publicKey, signature, originalJson, price, currency);
        }
    }

    public void OnShowInter() {
        _showInterAdsCount += 1;
        Log.d(TAG, "OnShowInter: " + _showInterAdsCount);

        SharedPreferences sharedPreferences = _activityCached.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.SHOW_INTER_ADS_COUNT, _showInterAdsCount);
        editor.apply();

        if (_showInterAdsCount >= 1 && _showInterAdsCount <= 20) {
            String eventName = String.format(Constants.SHOW_INTER_ADS_EVENT_NAME, _showInterAdsCount);
            AnalyticServices.getInstance().LogEvent(_activityCached, eventName, null);
//            Log.d(TAG, "LogEvent: " + eventName);
        }
    }

    private static final String PREF_UA_LOGGED = "ua_logged";
    public void LogUserAcquisition(Activity activity, String acquisitionMediaSource, String acquisitionCampaign) {
        if (activity == null) return;

        // Check if already fired successfully in a previous session
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_UA_LOGGED, false)) {
            Log.d(TAG, "LogUserAcquisition: already logged, skipping.");
            return;
        }

        String mediaSource = acquisitionMediaSource;
        String campaign = acquisitionCampaign;

        if (mediaSource.isEmpty() && campaign.isEmpty()) {
            Log.w(TAG, "LogUserAcquisition: No acquisition data available from AppsFlyer yet.");
            return;
        }

        try {
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(activity.getApplicationContext());

            // Event name embeds the value directly — visible immediately in Firebase Console without param registration
            // e.g. "ua_source_moloco_int", "ua_campaign_summer_2024"
            // Firebase event name: max 40 chars, only letters/digits/underscore, must start with letter
            String safeSource = sanitizeEventName(mediaSource);
            String safeCampaign = sanitizeEventName(campaign);

            firebaseAnalytics.logEvent("ua_source_" + safeSource, null);
            firebaseAnalytics.logEvent("ua_camp_" + safeCampaign, null);

            // Mark as successfully logged — never fire again
            prefs.edit().putBoolean(PREF_UA_LOGGED, true).apply();

            Log.d(TAG, "LogUserAcquisition: ua_source_" + safeSource + ", ua_camp_" + safeCampaign);
        } catch (Exception e) {
            Log.e(TAG, "LogUserAcquisition failed: " + e.getMessage());
        }
    }

    private String sanitizeEventName(String raw) {
        if (raw == null || raw.isEmpty()) return "unknown";
        String result = raw.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")  // replace invalid chars with underscore
                .replaceAll("_+", "_")            // collapse multiple underscores
                .replaceAll("^_+|_+$", "");       // trim leading/trailing underscores
        if (result.isEmpty() || !Character.isLetter(result.charAt(0))) {
            result = "x_" + result;
        }
        return result.length() > 32 ? result.substring(0, 32) : result; // reserve 8 chars for prefix "ua_camp_" (longest prefix)
    }
}

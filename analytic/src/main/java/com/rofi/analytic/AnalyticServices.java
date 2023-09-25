package com.rofi.analytic;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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

    public void RevenueTracking(Activity activity, String adFormat, String adUnitId, String adNetwork, double value) {
        for (IAnalytic analytic : analyticList) {
            analytic.RevenueTracking(activity, adFormat, adUnitId, adNetwork, value);
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
}

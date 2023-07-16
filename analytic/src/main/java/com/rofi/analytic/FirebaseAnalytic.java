package com.rofi.analytic;

import android.app.Activity;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAnalytic implements IAnalytic {
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void Init(Activity activity) {
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity.getApplicationContext());
    }

    @Override
    public void LogEvent(Activity activity, String eventName, String eventData) {
        Bundle bundle = new Bundle();

        if(eventData != null){
            Map<String, Object> mapEventData = new Gson().fromJson(
                    eventData, new TypeToken<HashMap<String, Object>>() {
                    }.getType()
            );

            for (Map.Entry<String, Object> pair : mapEventData.entrySet()) {
                System.out.format("key: %s, value: %s", pair.getKey(), pair.getValue().toString());
                bundle.putString(pair.getKey(), pair.getValue().toString());
            }
        }
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }

    @Override
    public void RevenueTracking(Activity activity, String adFormat, String adUnitId, String adSource, double value) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "appLovin");
        params.putString(FirebaseAnalytics.Param.AD_SOURCE, adSource);
        params.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
        params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
        params.putDouble(FirebaseAnalytics.Param.VALUE, value);
        params.putString(FirebaseAnalytics.Param.CURRENCY, "USD"); // All Applovin revenue is sent in USD
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
    }

    @Override
    public void ClickToAd(Activity activity, String adUnitId) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
        mFirebaseAnalytics.logEvent("ad_click", params);
    }
}
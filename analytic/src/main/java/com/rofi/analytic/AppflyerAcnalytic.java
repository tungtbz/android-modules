package com.rofi.analytic;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.appsflyer.AppsFlyerLib;
import com.appsflyer.adrevenue.AppsFlyerAdRevenue;
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork;
import com.appsflyer.adrevenue.adnetworks.generic.Scheme;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AppflyerAcnalytic implements IAnalytic {
    private final String TAG = AppflyerAcnalytic.class.toString();
    String _mainNetwork;

    @Override
    public void Init(Activity activity, String[] args) {

        String af_dev_key = args[0];
        _mainNetwork = args[1];

        AppsFlyerLib.getInstance().init(af_dev_key, null, activity.getApplicationContext());
        AppsFlyerLib.getInstance().start(activity.getApplicationContext());

        AppsFlyerAdRevenue.Builder afRevenueBuilder = new AppsFlyerAdRevenue.Builder(activity.getApplication());
        AppsFlyerAdRevenue.initialize(afRevenueBuilder.build());
    }

    @Override
    public void LogEvent(Activity activity, String eventName, String eventData) {
        Map<String, Object> appflyerEventData = new HashMap<String, Object>();
        if (eventData != null) {
            appflyerEventData = new Gson().fromJson(
                    eventData, new TypeToken<HashMap<String, Object>>() {
                    }.getType()
            );
        }

        AppsFlyerLib.getInstance().logEvent(activity.getApplicationContext(), eventName, appflyerEventData);
    }

    @Override
    public void RevenueTracking(Activity activity, String adFormat, String adUnitId, String adNetwork, double value) {
        MediationNetwork mediationNetwork = GetMediationNetwork(_mainNetwork);
        Map<String, String> customParams = new HashMap<>();
        customParams.put(Scheme.AD_UNIT, adUnitId);
        customParams.put(Scheme.AD_TYPE, adFormat);

        AppsFlyerAdRevenue.logAdRevenue(adNetwork, mediationNetwork, Currency.getInstance(Locale.US), value, customParams);

        Log.d(TAG, "RevenueTracking, monetization_network: " + adNetwork + " ,mediation_network: " + mediationNetwork + " ad type: " + adFormat);
    }

    private MediationNetwork GetMediationNetwork(String networkName) {
        if (networkName.contains("Google")) return MediationNetwork.googleadmob;
        if (networkName.contains("Yandex")) return MediationNetwork.Yandex;
        if (networkName.contains("Unity")) return MediationNetwork.Unity;
        if (networkName.contains("Char")) return MediationNetwork.chartboost;
        if (networkName.contains("App")) return MediationNetwork.applovinmax;
        if (networkName.contains("Fyber")) return MediationNetwork.fyber;
        if (networkName.contains("applovin")) return MediationNetwork.applovinmax;
        if (networkName.contains("iron")) return MediationNetwork.ironsource;

        return MediationNetwork.customMediation;
    }

    @Override
    public void ClickToAd(Activity activity, String adUnitId) {
        Map<String, Object> eventValues = new HashMap<String, Object>();
        eventValues.put("adUnitId", adUnitId);
        AppsFlyerLib.getInstance().logEvent(activity.getApplicationContext(), "ad_click", eventValues);
    }

    @Override
    public void AdmobAppOpenAdsRevenueTracking(Activity activity, String adSourceName, String adUnitId, double value) {
        Map<String, String> customParams = new HashMap<>();
        customParams.put("ad_platform", "Admob");
        customParams.put("ad_source", adSourceName);
        customParams.put("ad_unit_name", "");
        customParams.put(Scheme.AD_UNIT, adUnitId);
        customParams.put(Scheme.AD_TYPE, "AppOpenAds");

        AppsFlyerAdRevenue.logAdRevenue(adSourceName, MediationNetwork.googleadmob, Currency.getInstance(Locale.US), value, customParams);

    }
}

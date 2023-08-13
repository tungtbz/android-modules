package com.rofi.analytic;

import android.app.Activity;

import com.appsflyer.AppsFlyerLib;
import com.appsflyer.adrevenue.AppsFlyerAdRevenue;
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork;
import com.appsflyer.adrevenue.adnetworks.generic.Scheme;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AppflyerAcnalytic implements IAnalytic {
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
        MediationNetwork mediationNetwork = GetMediationNetwork(adNetwork);
        Map<String, String> customParams = new HashMap<>();
        customParams.put(Scheme.AD_UNIT, adUnitId);
        customParams.put(Scheme.AD_TYPE, adFormat);
        AppsFlyerAdRevenue.logAdRevenue(_mainNetwork, mediationNetwork, Currency.getInstance(Locale.US), value, customParams);
    }

    private MediationNetwork GetMediationNetwork(String networkName) {
        if (networkName.contains("Google")) return MediationNetwork.googleadmob;
        if (networkName.contains("Yandex")) return MediationNetwork.Yandex;
        if (networkName.contains("Unity")) return MediationNetwork.Unity;
        if (networkName.contains("Char")) return MediationNetwork.chartboost;
        if (networkName.contains("App")) return MediationNetwork.applovinmax;
        if (networkName.contains("Fyber")) return MediationNetwork.fyber;
        return MediationNetwork.customMediation;
    }

    @Override
    public void ClickToAd(Activity activity, String adUnitId) {
        Map<String, Object> eventValues = new HashMap<String, Object>();
        eventValues.put("adUnitId", adUnitId);
        AppsFlyerLib.getInstance().logEvent(activity.getApplicationContext(), "ad_click", eventValues);
    }
}

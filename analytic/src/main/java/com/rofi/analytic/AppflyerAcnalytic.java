package com.rofi.analytic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerInAppPurchaseValidatorListener;
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

    // WeakReference to avoid memory leak — Activity may be destroyed before callback fires
    private java.lang.ref.WeakReference<Activity> _weakActivity;

    // Reuse a single Gson instance to avoid repeated GC alloc
    private final Gson _gson = new Gson();

    @Override
    public void Init(Activity activity, String[] args) {

        String af_dev_key = args[0];
        _mainNetwork = args[1];

        // Store weak reference — used to dispatch UA event to main thread after conversion data arrives
        _weakActivity = new java.lang.ref.WeakReference<>(activity);
        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onConversionDataSuccess(Map<String, Object> conversionData) {
                if (conversionData == null) return;

                Log.d(TAG, "onConversionDataSuccess: Attribution data received.");

                // Print full raw data as JSON for debugging (debug builds only, never in production)
                if (BuildConfig.DEBUG) {
                    try {
                        String rawJson = _gson.toJson(conversionData);
                        Log.w(TAG, "====== RAW DATA START ======");
                        Log.w(TAG, "[APPSFLYER RAW JSON]: " + rawJson);
                        Log.w(TAG, "====== RAW DATA END ========");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to print raw data: " + e.getMessage());
                    }
                }

                // Only process on first install launch
                if (!conversionData.containsKey("is_first_launch")) return;

                String isFirstLaunch = String.valueOf(conversionData.get("is_first_launch"));
                if (!isFirstLaunch.equalsIgnoreCase("true")) {
                    Log.d(TAG, "[ATTRIBUTION] Regular re-open, skipping.");
                    return;
                }

                // Extract media source
                String mediaSource = conversionData.containsKey("media_source")
                        ? String.valueOf(conversionData.get("media_source"))
                        : "Organic";

                // Extract campaign name
                String campaign = conversionData.containsKey("campaign")
                        ? String.valueOf(conversionData.get("campaign"))
                        : "unknown";

                Log.w(TAG, "[ATTRIBUTION] media_source: " + mediaSource);
                Log.w(TAG, "[ATTRIBUTION] campaign: " + campaign);

                if (mediaSource.toLowerCase().contains("moloco")) {
                    Log.w(TAG, "[ATTRIBUTION] => Confirmed: User acquired from MOLOCO!");
                }

                Log.w(TAG, "==========================================");

                Activity act = _weakActivity != null ? _weakActivity.get() : null;
                if (act != null && !act.isFinishing()) {
                    act.runOnUiThread(() -> {
                        try {
                            AnalyticServices.getInstance().LogUserAcquisition(act, mediaSource, campaign);
                        } catch (Exception e) {
                            Log.e(TAG, "Auto LogUserAcquisition failed: " + e.getMessage());
                        }
                    });
                } else {
                    Log.w(TAG, "Auto LogUserAcquisition skipped: Activity is null or finishing.");
                }
            }

            @Override
            public void onConversionDataFail(String errorMessage) {
                Log.e(TAG, "[ATTRIBUTION] Failed to get conversion data: " + errorMessage);
            }

            @Override
            public void onAppOpenAttribution(Map<String, String> attributionData) {
                // Deep link re-engagement attribution, no additional handling needed
                Log.d(TAG, "onAppOpenAttribution: " + (attributionData != null ? attributionData.toString() : "null"));
            }

            @Override
            public void onAttributionFailure(String errorMessage) {
                Log.e(TAG, "onAttributionFailure: " + errorMessage);
            }
        };

        AppsFlyerLib.getInstance().init(af_dev_key, conversionListener, activity.getApplicationContext());
        AppsFlyerLib.getInstance().start(activity.getApplicationContext());
        AppsFlyerLib.getInstance().registerValidatorListener(activity.getApplicationContext(), new AppsFlyerInAppPurchaseValidatorListener() {
            @Override
            public void onValidateInApp() {
                Log.d(TAG, "Purchase validated successfully");
            }

            @Override
            public void onValidateInAppFailure(String error) {
                Log.e(TAG, "onValidateInAppFailure called: " + error);
            }
        });

        if (BuildConfig.DEBUG) AppsFlyerLib.getInstance().setDebugLog(true);

        AppsFlyerAdRevenue.Builder afRevenueBuilder = new AppsFlyerAdRevenue.Builder(activity.getApplication());
        AppsFlyerAdRevenue.initialize(afRevenueBuilder.build());
    }

    public void LogIAPRevenue(Context context, String publicKey, String signature, String originalJson, String price, String currency) {
        Log.d(TAG, "ValidatePurchase publicKey: " + publicKey + " signature: " + signature + " originalJson: " + originalJson + " price: " + price + " currency: " + currency);
        Map<String, String> eventValues = new HashMap<>();
        AppsFlyerLib.getInstance().validateAndLogInAppPurchase(context, publicKey, signature, originalJson, price, currency, eventValues);
    }

    @Override
    public void LogEvent(Activity activity, String eventName, String eventData) {
        Map<String, Object> appflyerEventData = new HashMap<String, Object>();
        if (eventData != null) {
            appflyerEventData = new Gson().fromJson(eventData, new TypeToken<HashMap<String, Object>>() {
            }.getType());
        }

        AppsFlyerLib.getInstance().logEvent(activity.getApplicationContext(), eventName, appflyerEventData);
    }

    @Override
    public void RevenueTracking(Activity activity, String adPlatform, String adFormat, String adUnitId, String adNetwork, double value) {
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

    @Override
    public void AdmobAdsRevenueTracking(Activity activity, String adFormat, String adSourceName, String adUnitId, double value) {
        Map<String, String> customParams = new HashMap<>();
        customParams.put("ad_platform", "Admob");
        customParams.put("ad_source", adSourceName);
        customParams.put("ad_unit_name", "");
        customParams.put(Scheme.AD_UNIT, adUnitId);
        customParams.put(Scheme.AD_TYPE, adFormat);

        AppsFlyerAdRevenue.logAdRevenue(adSourceName, MediationNetwork.googleadmob, Currency.getInstance(Locale.US), value, customParams);
    }
}

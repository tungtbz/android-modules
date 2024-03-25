package com.tbase.maxads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.impl.sdk.utils.JsonUtils;
import com.applovin.impl.sdk.utils.StringUtils;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdReviewListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxAdWaterfallInfo;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxMediatedNetworkInfo;
import com.applovin.mediation.MaxNetworkResponseInfo;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.ads.MaxRewardedInterstitialAd;
import com.applovin.sdk.AppLovinCmpError;
import com.applovin.sdk.AppLovinCmpService;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.applovin.sdk.AppLovinSdkUtils;
import com.applovin.sdk.AppLovinUserService;
import com.unity3d.player.UnityPlayer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class MaxUnityAdManager implements
        MaxAdListener,
        MaxAdViewAdListener,
        MaxRewardedAdListener,
        MaxAdRevenueListener,
        MaxAdReviewListener,
        AppLovinUserService.OnConsentDialogDismissListener,
        AppLovinCmpService.OnCompletedListener {
    private static final String SDK_TAG = "AppLovinSdk";

    private static final String TAG = "MaxUnityAdManager";

    private static final String VERSION = "6.3.1";

    private static final String DEFAULT_AD_VIEW_POSITION = "top_left";

    private static final Point DEFAULT_AD_VIEW_OFFSET = new Point(0, 0);

    private static final ScheduledThreadPoolExecutor sThreadPoolExecutor = new ScheduledThreadPoolExecutor(3, new SdkThreadFactory());

    private static MaxUnityAdManager instance;

    private static WeakReference<Activity> currentActivity;

    private static BackgroundCallback backgroundCallback;

    private AppLovinSdk sdk;

    private final Map<String, MaxInterstitialAd> mInterstitials;

    private final Map<String, MaxAppOpenAd> mAppOpenAds;

    private final Map<String, MaxRewardedAd> mRewardedAds;

    private final Map<String, MaxRewardedInterstitialAd> mRewardedInterstitialAds;

    private final Map<String, MaxAdView> mAdViews;

    private final Map<String, MaxAdFormat> mAdViewAdFormats;

    private final Map<String, String> mAdViewPositions;

    private final Map<String, Point> mAdViewOffsets;

    private final Map<String, Integer> mAdViewWidths;

    private final Map<String, Map<String, String>> mAdViewExtraParametersToSetAfterCreate;

    private final Map<String, Map<String, Object>> mAdViewLocalExtraParametersToSetAfterCreate;

    private final Map<String, String> mAdViewCustomDataToSetAfterCreate;

    private final List<String> mAdUnitIdsToShowAfterCreate;

    private final Set<String> mDisabledAdaptiveBannerAdUnitIds;

    private final Set<String> mDisabledAutoRefreshAdViewAdUnitIds;

    private View mSafeAreaBackground;

    protected static class Insets {
        int left;

        int top;

        int right;

        int bottom;
    }

    private Integer mPublisherBannerBackgroundColor = null;

    private final Map<String, MaxAd> mAdInfoMap;

    private final Object mAdInfoMapLock;

    public MaxUnityAdManager() {
        this(null);
    }

    private MaxUnityAdManager(Activity currentActivity) {
        MaxUnityAdManager.currentActivity = new WeakReference<>(currentActivity);
        this.mInterstitials = new HashMap<>(2);
        this.mAppOpenAds = new HashMap<>(2);
        this.mRewardedAds = new HashMap<>(2);
        this.mRewardedInterstitialAds = new HashMap<>(2);
        this.mAdViews = new HashMap<>(2);
        this.mAdViewAdFormats = new HashMap<>(2);
        this.mAdViewPositions = new HashMap<>(2);
        this.mAdViewOffsets = new HashMap<>(2);
        this.mAdViewWidths = new HashMap<>(2);
        this.mAdInfoMap = new HashMap<>();
        this.mAdInfoMapLock = new Object();
        this.mAdViewExtraParametersToSetAfterCreate = new HashMap<>(1);
        this.mAdViewLocalExtraParametersToSetAfterCreate = new HashMap<>(1);
        this.mAdViewCustomDataToSetAfterCreate = new HashMap<>(1);
        this.mAdUnitIdsToShowAfterCreate = new ArrayList<>(2);
        this.mDisabledAdaptiveBannerAdUnitIds = new HashSet<>(2);
        this.mDisabledAutoRefreshAdViewAdUnitIds = new HashSet<>(2);
        AppLovinSdkUtils.runOnUiThread(true, new Runnable() {
            public void run() {
                MaxUnityAdManager.this.mSafeAreaBackground = new View((Context)MaxUnityAdManager.getCurrentActivity());
                MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(View.GONE);
                MaxUnityAdManager.this.mSafeAreaBackground.setBackgroundColor(0);
                MaxUnityAdManager.this.mSafeAreaBackground.setClickable(false);
                FrameLayout layout = new FrameLayout((Context)MaxUnityAdManager.getCurrentActivity());
                layout.addView(MaxUnityAdManager.this.mSafeAreaBackground, (ViewGroup.LayoutParams)new FrameLayout.LayoutParams(0, 0));
                MaxUnityAdManager.getCurrentActivity().addContentView((View)layout, (ViewGroup.LayoutParams)new LinearLayout.LayoutParams(-1, -1));
                ViewParent parent = layout.getParent();
                if (parent instanceof View)
                    ((View)parent).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                        public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                            for (MaxAdView adView : MaxUnityAdManager.this.mAdViews.values()) {
                                ViewParent parent = adView.getParent();
                                if (parent instanceof View)
                                    ((View)parent).bringToFront();
                            }
                        }
                    });
            }
        });
        getCurrentActivity().getWindow().getDecorView().getRootView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                boolean viewBoundsChanged = (left != oldLeft || right != oldRight || bottom != oldBottom || top != oldTop);
                if (!viewBoundsChanged)
                    return;
                for (Map.Entry<String, MaxAdFormat> adUnitFormats : (Iterable<Map.Entry<String, MaxAdFormat>>)MaxUnityAdManager.this.mAdViewAdFormats.entrySet())
                    MaxUnityAdManager.this.positionAdView(adUnitFormats.getKey(), adUnitFormats.getValue());
            }
        });
    }

    public static MaxUnityAdManager getInstance(Activity currentActivity) {
        if (instance == null) {
            instance = new MaxUnityAdManager(currentActivity);
        } else {
            MaxUnityAdManager.currentActivity = new WeakReference<>(currentActivity);
        }
        return instance;
    }

    public AppLovinSdk initializeSdkWithCompletionHandler(String sdkKey, AppLovinSdkSettings settings, BackgroundCallback backgroundCallback, final Listener listener) {
        MaxUnityAdManager.backgroundCallback = backgroundCallback;
        Activity currentActivity = getCurrentActivity();
        if (StringUtils.isValidString(sdkKey)) {
            this.sdk = AppLovinSdk.getInstance(sdkKey, settings, (Context)currentActivity);
        } else {
            this.sdk = AppLovinSdk.getInstance(settings, (Context)currentActivity);
        }
        this.sdk.setPluginVersion("Max-Unity-6.3.1");
        this.sdk.setMediationProvider("max");
        this.sdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
            public void onSdkInitialized(AppLovinSdkConfiguration config) {
                listener.onSdkInitializationComplete(config);
                JSONObject args = new JSONObject();
                JsonUtils.putString(args, "name", "OnSdkInitializedEvent");
                JsonUtils.putString(args, "consentFlowUserGeography", Integer.toString(config.getConsentFlowUserGeography().ordinal()));
                JsonUtils.putString(args, "consentDialogState", Integer.toString(config.getConsentDialogState().ordinal()));
                JsonUtils.putString(args, "countryCode", config.getCountryCode());
                JsonUtils.putString(args, "isSuccessfullyInitialized", String.valueOf(MaxUnityAdManager.this.sdk.isInitialized()));
                JsonUtils.putBoolean(args, "isTestModeEnabled", config.isTestModeEnabled());
                MaxUnityAdManager.forwardUnityEvent(args);
            }
        });
        return this.sdk;
    }

    public void createBanner(String adUnitId, String bannerPosition) {
        createAdView(adUnitId, getAdViewAdFormat(adUnitId), bannerPosition, DEFAULT_AD_VIEW_OFFSET);
    }

    public void createBanner(String adUnitId, float x, float y) {
        createAdView(adUnitId, getAdViewAdFormat(adUnitId), "top_left", getOffsetPixels(x, y, (Context)getCurrentActivity()));
    }

    public void loadBanner(String adUnitId) {
        loadAdView(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public void setBannerPlacement(String adUnitId, String placement) {
        setAdViewPlacement(adUnitId, getAdViewAdFormat(adUnitId), placement);
    }

    public void startBannerAutoRefresh(String adUnitId) {
        startAdViewAutoRefresh(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public void stopBannerAutoRefresh(String adUnitId) {
        stopAdViewAutoRefresh(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public void setBannerWidth(String adUnitId, int widthDp) {
        setAdViewWidth(adUnitId, widthDp, getAdViewAdFormat(adUnitId));
    }

    public void updateBannerPosition(String adUnitId, String bannerPosition) {
        updateAdViewPosition(adUnitId, bannerPosition, DEFAULT_AD_VIEW_OFFSET, getAdViewAdFormat(adUnitId));
    }

    public void updateBannerPosition(String adUnitId, float x, float y) {
        updateAdViewPosition(adUnitId, "top_left", getOffsetPixels(x, y, (Context)getCurrentActivity()), getAdViewAdFormat(adUnitId));
    }

    public void showBanner(String adUnitId) {
        showAdView(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public void hideBanner(String adUnitId) {
        hideAdView(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public void destroyBanner(String adUnitId) {
        destroyAdView(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public void setBannerBackgroundColor(String adUnitId, String hexColorCode) {
        setAdViewBackgroundColor(adUnitId, getAdViewAdFormat(adUnitId), hexColorCode);
    }

    public void setBannerExtraParameter(String adUnitId, String key, String value) {
        setAdViewExtraParameter(adUnitId, getAdViewAdFormat(adUnitId), key, value);
    }

    public void setBannerLocalExtraParameter(String adUnitId, String key, Object value) {
        if (key == null) {
            e("Failed to set local extra parameter: No key specified");
            return;
        }
        setAdViewLocalExtraParameter(adUnitId, getAdViewAdFormat(adUnitId), key, value);
    }

    public void setBannerCustomData(String adUnitId, String customData) {
        setAdViewCustomData(adUnitId, getAdViewAdFormat(adUnitId), customData);
    }

    public String getBannerLayout(String adUnitId) {
        return getAdViewLayout(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public static float getAdaptiveBannerHeight(float width) {
        return getDeviceSpecificAdViewAdFormat().getAdaptiveSize((int)width, (Context)getCurrentActivity()).getHeight();
    }

    public void createMRec(String adUnitId, String mrecPosition) {
        createAdView(adUnitId, MaxAdFormat.MREC, mrecPosition, DEFAULT_AD_VIEW_OFFSET);
    }

    public void createMRec(String adUnitId, float x, float y) {
        createAdView(adUnitId, MaxAdFormat.MREC, "top_left", getOffsetPixels(x, y, (Context)getCurrentActivity()));
    }

    public void loadMRec(String adUnitId) {
        loadAdView(adUnitId, MaxAdFormat.MREC);
    }

    public void setMRecPlacement(String adUnitId, String placement) {
        setAdViewPlacement(adUnitId, MaxAdFormat.MREC, placement);
    }

    public void startMRecAutoRefresh(String adUnitId) {
        startAdViewAutoRefresh(adUnitId, MaxAdFormat.MREC);
    }

    public void stopMRecAutoRefresh(String adUnitId) {
        stopAdViewAutoRefresh(adUnitId, MaxAdFormat.MREC);
    }

    public void updateMRecPosition(String adUnitId, String mrecPosition) {
        updateAdViewPosition(adUnitId, mrecPosition, DEFAULT_AD_VIEW_OFFSET, MaxAdFormat.MREC);
    }

    public void updateMRecPosition(String adUnitId, float x, float y) {
        updateAdViewPosition(adUnitId, "top_left", getOffsetPixels(x, y, (Context)getCurrentActivity()), MaxAdFormat.MREC);
    }

    public void showMRec(String adUnitId) {
        showAdView(adUnitId, MaxAdFormat.MREC);
    }

    public void hideMRec(String adUnitId) {
        hideAdView(adUnitId, MaxAdFormat.MREC);
    }

    public void setMRecExtraParameter(String adUnitId, String key, String value) {
        setAdViewExtraParameter(adUnitId, MaxAdFormat.MREC, key, value);
    }

    public void setMRecLocalExtraParameter(String adUnitId, String key, Object value) {
        if (key == null) {
            e("Failed to set local extra parameter: No key specified");
            return;
        }
        setAdViewLocalExtraParameter(adUnitId, MaxAdFormat.MREC, key, value);
    }

    public void setMRecCustomData(String adUnitId, String customData) {
        setAdViewCustomData(adUnitId, MaxAdFormat.MREC, customData);
    }

    public String getMRecLayout(String adUnitId) {
        return getAdViewLayout(adUnitId, MaxAdFormat.MREC);
    }

    public void destroyMRec(String adUnitId) {
        destroyAdView(adUnitId, MaxAdFormat.MREC);
    }

    public void loadInterstitial(String adUnitId) {
        MaxInterstitialAd interstitial = retrieveInterstitial(adUnitId);
        interstitial.loadAd();
    }

    public boolean isInterstitialReady(String adUnitId) {
        MaxInterstitialAd interstitial = retrieveInterstitial(adUnitId);
        return interstitial.isReady();
    }

    public void showInterstitial(String adUnitId, String placement, String customData) {
        MaxInterstitialAd interstitial = retrieveInterstitial(adUnitId);
        interstitial.showAd(placement, customData);
    }

    public void setInterstitialExtraParameter(String adUnitId, String key, String value) {
        MaxInterstitialAd interstitial = retrieveInterstitial(adUnitId);
        interstitial.setExtraParameter(key, value);
    }

    public void setInterstitialLocalExtraParameter(String adUnitId, String key, Object value) {
        if (key == null) {
            e("Failed to set local extra parameter: No key specified");
            return;
        }
        MaxInterstitialAd interstitial = retrieveInterstitial(adUnitId);
        interstitial.setLocalExtraParameter(key, value);
    }

    public void loadAppOpenAd(String adUnitId) {
        MaxAppOpenAd appOpenAd = retrieveAppOpenAd(adUnitId);
        appOpenAd.loadAd();
    }

    public boolean isAppOpenAdReady(String adUnitId) {
        MaxAppOpenAd appOpenAd = retrieveAppOpenAd(adUnitId);
        return appOpenAd.isReady();
    }

    public void showAppOpenAd(String adUnitId, String placement, String customData) {
        MaxAppOpenAd appOpenAd = retrieveAppOpenAd(adUnitId);
        appOpenAd.showAd(placement, customData);
    }

    public void setAppOpenAdExtraParameter(String adUnitId, String key, String value) {
        MaxAppOpenAd appOpenAd = retrieveAppOpenAd(adUnitId);
        appOpenAd.setExtraParameter(key, value);
    }

    public void setAppOpenAdLocalExtraParameter(String adUnitId, String key, Object value) {
        if (key == null) {
            e("Failed to set local extra parameter: No key specified");
            return;
        }
        MaxAppOpenAd appOpenAd = retrieveAppOpenAd(adUnitId);
        appOpenAd.setLocalExtraParameter(key, value);
    }

    public void loadRewardedAd(String adUnitId) {
        MaxRewardedAd rewardedAd = retrieveRewardedAd(adUnitId);
        rewardedAd.loadAd();
    }

    public boolean isRewardedAdReady(String adUnitId) {
        MaxRewardedAd rewardedAd = retrieveRewardedAd(adUnitId);
        return rewardedAd.isReady();
    }

    public void showRewardedAd(String adUnitId, String placement, String customData) {
        MaxRewardedAd rewardedAd = retrieveRewardedAd(adUnitId);
        rewardedAd.showAd(placement, customData);
    }

    public void setRewardedAdExtraParameter(String adUnitId, String key, String value) {
        MaxRewardedAd rewardedAd = retrieveRewardedAd(adUnitId);
        rewardedAd.setExtraParameter(key, value);
    }

    public void setRewardedAdLocalExtraParameter(String adUnitId, String key, Object value) {
        if (key == null) {
            e("Failed to set local extra parameter: No key specified");
            return;
        }
        MaxRewardedAd rewardedAd = retrieveRewardedAd(adUnitId);
        rewardedAd.setLocalExtraParameter(key, value);
    }

    public void loadRewardedInterstitialAd(String adUnitId) {
        MaxRewardedInterstitialAd rewardedInterstitialAd = retrieveRewardedInterstitialAd(adUnitId);
        rewardedInterstitialAd.loadAd();
    }

    public boolean isRewardedInterstitialAdReady(String adUnitId) {
        MaxRewardedInterstitialAd rewardedInterstitialAd = retrieveRewardedInterstitialAd(adUnitId);
        return rewardedInterstitialAd.isReady();
    }

    public void showRewardedInterstitialAd(String adUnitId, String placement, String customData) {
        MaxRewardedInterstitialAd rewardedInterstitialAd = retrieveRewardedInterstitialAd(adUnitId);
        rewardedInterstitialAd.showAd(placement, customData);
    }

    public void setRewardedInterstitialAdExtraParameter(String adUnitId, String key, String value) {
        MaxRewardedInterstitialAd rewardedInterstitialAd = retrieveRewardedInterstitialAd(adUnitId);
        rewardedInterstitialAd.setExtraParameter(key, value);
    }

    public void setRewardedInterstitialAdLocalExtraParameter(String adUnitId, String key, Object value) {
        if (key == null) {
            e("Failed to set local extra parameter: No key specified");
            return;
        }
        MaxRewardedInterstitialAd rewardedInterstitialAd = retrieveRewardedInterstitialAd(adUnitId);
        rewardedInterstitialAd.setLocalExtraParameter(key, value);
    }

    public void trackEvent(String event, String parameters) {
        if (this.sdk == null)
            return;
        Map<String, String> deserialized = deserializeParameters(parameters);
        this.sdk.getEventService().trackEvent(event, deserialized);
    }

    public void onDismiss() {
        JSONObject args = new JSONObject();
        JsonUtils.putString(args, "name", "OnSdkConsentDialogDismissedEvent");
        forwardUnityEvent(args);
    }

    public void showCmpForExistingUser() {
        this.sdk.getCmpService().showCmpForExistingUser(getCurrentActivity(), this);
    }

    public void onCompleted(AppLovinCmpError error) {
        JSONObject completionArgs = new JSONObject();
        JsonUtils.putString(completionArgs, "name", "OnCmpCompletedEvent");
        if (error != null) {
            JSONObject errorArgs = new JSONObject();
            JsonUtils.putInt(errorArgs, "code", error.getCode().getValue());
            JsonUtils.putString(errorArgs, "message", error.getMessage());
            JsonUtils.putInt(errorArgs, "cmpCode", error.getCmpCode());
            JsonUtils.putString(errorArgs, "cmpMessage", error.getCmpMessage());
            JsonUtils.putJSONObject(completionArgs, "error", errorArgs);
        }
        forwardUnityEvent(completionArgs, true);
    }

    public String getAdInfo(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId))
            return "";
        MaxAd ad = getAd(adUnitId);
        if (ad == null)
            return "";
        JSONObject adInfo = getAdInfo(ad);
        return adInfo.toString();
    }

    private JSONObject getAdInfo(MaxAd ad) {
        JSONObject adInfo = new JSONObject();
        JsonUtils.putString(adInfo, "adUnitId", ad.getAdUnitId());
        JsonUtils.putString(adInfo, "adFormat", ad.getFormat().getLabel());
        JsonUtils.putString(adInfo, "networkName", ad.getNetworkName());
        JsonUtils.putString(adInfo, "networkPlacement", ad.getNetworkPlacement());
        JsonUtils.putString(adInfo, "creativeId", !TextUtils.isEmpty(ad.getCreativeId()) ? ad.getCreativeId() : "");
        JsonUtils.putString(adInfo, "placement", !TextUtils.isEmpty(ad.getPlacement()) ? ad.getPlacement() : "");
        JsonUtils.putString(adInfo, "revenue", String.valueOf(ad.getRevenue()));
        JsonUtils.putString(adInfo, "revenuePrecision", ad.getRevenuePrecision());
        JsonUtils.putJSONObject(adInfo, "waterfallInfo", createAdWaterfallInfo(ad.getWaterfall()));
        JsonUtils.putString(adInfo, "latencyMillis", String.valueOf(ad.getRequestLatencyMillis()));
        JsonUtils.putString(adInfo, "dspName", !TextUtils.isEmpty(ad.getDspName()) ? ad.getDspName() : "");
        return adInfo;
    }

    private JSONObject createAdWaterfallInfo(MaxAdWaterfallInfo waterfallInfo) {
        JSONObject waterfallInfoObject = new JSONObject();
        if (waterfallInfo == null)
            return waterfallInfoObject;
        JsonUtils.putString(waterfallInfoObject, "name", waterfallInfo.getName());
        JsonUtils.putString(waterfallInfoObject, "testName", waterfallInfo.getTestName());
        JSONArray networkResponsesArray = new JSONArray();
        for (MaxNetworkResponseInfo response : waterfallInfo.getNetworkResponses())
            networkResponsesArray.put(createNetworkResponseInfo(response));
        JsonUtils.putJsonArray(waterfallInfoObject, "networkResponses", networkResponsesArray);
        JsonUtils.putString(waterfallInfoObject, "latencyMillis", String.valueOf(waterfallInfo.getLatencyMillis()));
        return waterfallInfoObject;
    }

    private JSONObject createNetworkResponseInfo(MaxNetworkResponseInfo response) {
        JSONObject networkResponseObject = new JSONObject();
        JsonUtils.putString(networkResponseObject, "adLoadState", Integer.toString(response.getAdLoadState().ordinal()));
        MaxMediatedNetworkInfo mediatedNetworkInfo = response.getMediatedNetwork();
        if (mediatedNetworkInfo != null) {
            JSONObject networkInfoObject = new JSONObject();
            JsonUtils.putString(networkInfoObject, "name", response.getMediatedNetwork().getName());
            JsonUtils.putString(networkInfoObject, "adapterClassName", response.getMediatedNetwork().getAdapterClassName());
            JsonUtils.putString(networkInfoObject, "adapterVersion", response.getMediatedNetwork().getAdapterVersion());
            JsonUtils.putString(networkInfoObject, "sdkVersion", response.getMediatedNetwork().getSdkVersion());
            JsonUtils.putJSONObject(networkResponseObject, "mediatedNetwork", networkInfoObject);
        }
        JsonUtils.putJSONObject(networkResponseObject, "credentials", BundleUtils.toJSONObject(response.getCredentials()));
        JsonUtils.putBoolean(networkResponseObject, "isBidding", response.isBidding());
        MaxError error = response.getError();
        if (error != null) {
            JSONObject errorObject = new JSONObject();
            JsonUtils.putString(errorObject, "errorMessage", error.getMessage());
            JsonUtils.putString(errorObject, "adLoadFailureInfo", error.getAdLoadFailureInfo());
            JsonUtils.putString(errorObject, "errorCode", Integer.toString(error.getCode()));
            JsonUtils.putString(errorObject, "latencyMillis", String.valueOf(error.getRequestLatencyMillis()));
            JsonUtils.putJSONObject(networkResponseObject, "error", errorObject);
        }
        JsonUtils.putString(networkResponseObject, "latencyMillis", String.valueOf(response.getLatencyMillis()));
        return networkResponseObject;
    }

    public String getAdValue(String adUnitId, String key) {
        if (TextUtils.isEmpty(adUnitId))
            return "";
        MaxAd ad = getAd(adUnitId);
        if (ad == null)
            return "";
        return ad.getAdValue(key);
    }

    public void onAdLoaded(MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (adFormat.isAdViewAd()) {
            if (MaxAdFormat.MREC == adFormat) {
                name = "OnMRecAdLoadedEvent";
            } else {
                name = "OnBannerAdLoadedEvent";
            }
            positionAdView(ad);
            MaxAdView adView = retrieveAdView(ad.getAdUnitId(), adFormat);
            if (adView != null && adView.getVisibility() != View.VISIBLE)
                adView.stopAutoRefresh();
        } else if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialLoadedEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdLoadedEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdLoadedEvent";
        } else if (MaxAdFormat.REWARDED_INTERSTITIAL == adFormat) {
            name = "OnRewardedInterstitialAdLoadedEvent";
        } else {
            logInvalidAdFormat(adFormat);
            return;
        }
        synchronized (this.mAdInfoMapLock) {
            this.mAdInfoMap.put(ad.getAdUnitId(), ad);
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        forwardUnityEvent(args);
    }

    public void onAdLoadFailed(String adUnitId, MaxError error) {
        String name;
        if (TextUtils.isEmpty(adUnitId)) {
            logStackTrace(new IllegalArgumentException("adUnitId cannot be null"));
            return;
        }
        if (this.mAdViews.containsKey(adUnitId)) {
            MaxAdFormat adViewAdFormat = this.mAdViewAdFormats.get(adUnitId);
            if (MaxAdFormat.MREC == adViewAdFormat) {
                name = "OnMRecAdLoadFailedEvent";
            } else {
                name = "OnBannerAdLoadFailedEvent";
            }
        } else if (this.mInterstitials.containsKey(adUnitId)) {
            name = "OnInterstitialLoadFailedEvent";
        } else if (this.mAppOpenAds.containsKey(adUnitId)) {
            name = "OnAppOpenAdLoadFailedEvent";
        } else if (this.mRewardedAds.containsKey(adUnitId)) {
            name = "OnRewardedAdLoadFailedEvent";
        } else if (this.mRewardedInterstitialAds.containsKey(adUnitId)) {
            name = "OnRewardedInterstitialAdLoadFailedEvent";
        } else {
            logStackTrace(new IllegalStateException("invalid adUnitId: " + adUnitId));
            return;
        }
        synchronized (this.mAdInfoMapLock) {
            this.mAdInfoMap.remove(adUnitId);
        }
        JSONObject args = new JSONObject();
        JsonUtils.putString(args, "name", name);
        JsonUtils.putString(args, "adUnitId", adUnitId);
        JsonUtils.putString(args, "errorCode", Integer.toString(error.getCode()));
        JsonUtils.putString(args, "errorMessage", error.getMessage());
        JsonUtils.putJSONObject(args, "waterfallInfo", createAdWaterfallInfo(error.getWaterfall()));
        String adLoadFailureInfo = error.getAdLoadFailureInfo();
        JsonUtils.putString(args, "adLoadFailureInfo", !TextUtils.isEmpty(adLoadFailureInfo) ? adLoadFailureInfo : "");
        JsonUtils.putString(args, "latencyMillis", String.valueOf(error.getRequestLatencyMillis()));
        forwardUnityEvent(args);
    }

    public void onAdClicked(MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (MaxAdFormat.BANNER == adFormat || MaxAdFormat.LEADER == adFormat) {
            name = "OnBannerAdClickedEvent";
        } else if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdClickedEvent";
        } else if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialClickedEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdClickedEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdClickedEvent";
        } else if (MaxAdFormat.REWARDED_INTERSTITIAL == adFormat) {
            name = "OnRewardedInterstitialAdClickedEvent";
        } else {
            logInvalidAdFormat(adFormat);
            return;
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        forwardUnityEvent(args);
    }

    public void onAdDisplayed(MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (!adFormat.isFullscreenAd())
            return;
        if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialDisplayedEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdDisplayedEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdDisplayedEvent";
        } else {
            name = "OnRewardedInterstitialAdDisplayedEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        forwardUnityEvent(args);
    }

    public void onAdDisplayFailed(MaxAd ad, MaxError error) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (!adFormat.isFullscreenAd())
            return;
        if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialAdFailedToDisplayEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdFailedToDisplayEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdFailedToDisplayEvent";
        } else {
            name = "OnRewardedInterstitialAdFailedToDisplayEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        JsonUtils.putString(args, "errorCode", Integer.toString(error.getCode()));
        JsonUtils.putString(args, "errorMessage", error.getMessage());
        JsonUtils.putString(args, "mediatedNetworkErrorCode", Integer.toString(error.getMediatedNetworkErrorCode()));
        JsonUtils.putString(args, "mediatedNetworkErrorMessage", error.getMediatedNetworkErrorMessage());
        JsonUtils.putJSONObject(args, "waterfallInfo", createAdWaterfallInfo(error.getWaterfall()));
        JsonUtils.putString(args, "latencyMillis", String.valueOf(error.getRequestLatencyMillis()));
        forwardUnityEvent(args);
    }

    public void onAdHidden(MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (!adFormat.isFullscreenAd())
            return;
        if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialHiddenEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdHiddenEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdHiddenEvent";
        } else {
            name = "OnRewardedInterstitialAdHiddenEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        forwardUnityEvent(args);
    }

    public void onAdCollapsed(MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (!adFormat.isAdViewAd()) {
            logInvalidAdFormat(adFormat);
            return;
        }
        if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdCollapsedEvent";
        } else {
            name = "OnBannerAdCollapsedEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        forwardUnityEvent(args);
    }

    public void onAdExpanded(MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (!adFormat.isAdViewAd()) {
            logInvalidAdFormat(adFormat);
            return;
        }
        if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdExpandedEvent";
        } else {
            name = "OnBannerAdExpandedEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        forwardUnityEvent(args);
    }

    public void onRewardedVideoCompleted(MaxAd ad) {}

    public void onRewardedVideoStarted(MaxAd ad) {}

    public void onUserRewarded(MaxAd ad, MaxReward reward) {
        MaxAdFormat adFormat = ad.getFormat();
        if (adFormat != MaxAdFormat.REWARDED && adFormat != MaxAdFormat.REWARDED_INTERSTITIAL) {
            logInvalidAdFormat(adFormat);
            return;
        }
        String rewardLabel = (reward != null) ? reward.getLabel() : "";
        int rewardAmountInt = (reward != null) ? reward.getAmount() : 0;
        String rewardAmount = Integer.toString(rewardAmountInt);
        String name = (adFormat == MaxAdFormat.REWARDED) ? "OnRewardedAdReceivedRewardEvent" : "OnRewardedInterstitialAdReceivedRewardEvent";
        JSONObject args = getDefaultAdEventParameters(name, ad);
        JsonUtils.putString(args, "rewardLabel", rewardLabel);
        JsonUtils.putString(args, "rewardAmount", rewardAmount);
        forwardUnityEvent(args);
    }

    public void onAdRevenuePaid(MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (MaxAdFormat.BANNER == adFormat || MaxAdFormat.LEADER == adFormat) {
            name = "OnBannerAdRevenuePaidEvent";
        } else if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdRevenuePaidEvent";
        } else if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialAdRevenuePaidEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdRevenuePaidEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdRevenuePaidEvent";
        } else if (MaxAdFormat.REWARDED_INTERSTITIAL == adFormat) {
            name = "OnRewardedInterstitialAdRevenuePaidEvent";
        } else {
            logInvalidAdFormat(adFormat);
            return;
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        forwardUnityEvent(args, adFormat.isFullscreenAd());
    }

    public void onCreativeIdGenerated(String creativeId, MaxAd ad) {
        String name;
        MaxAdFormat adFormat = ad.getFormat();
        if (MaxAdFormat.BANNER == adFormat || MaxAdFormat.LEADER == adFormat) {
            name = "OnBannerAdReviewCreativeIdGeneratedEvent";
        } else if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdReviewCreativeIdGeneratedEvent";
        } else if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialAdReviewCreativeIdGeneratedEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdReviewCreativeIdGeneratedEvent";
        } else if (MaxAdFormat.REWARDED_INTERSTITIAL == adFormat) {
            name = "OnRewardedInterstitialAdReviewCreativeIdGeneratedEvent";
        } else {
            logInvalidAdFormat(adFormat);
            return;
        }
        JSONObject args = getDefaultAdEventParameters(name, ad);
        JsonUtils.putString(args, "adReviewCreativeId", creativeId);
        forwardUnityEvent(args, adFormat.isFullscreenAd());
    }

    private JSONObject getDefaultAdEventParameters(String name, MaxAd ad) {
        JSONObject args = getAdInfo(ad);
        JsonUtils.putString(args, "name", name);
        return args;
    }

    private void createAdView(final String adUnitId, final MaxAdFormat adFormat, final String adViewPosition, final Point adViewOffsetPixels) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Creating " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\" and position: \"" + adViewPosition + "\"");
                if (MaxUnityAdManager.this.mAdViews.get(adUnitId) != null)
                    Log.w("MaxUnityAdManager", "Trying to create a " + adFormat.getLabel() + " that was already created. This will cause the current ad to be hidden.");
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat, adViewPosition, adViewOffsetPixels);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(View.GONE);
                adView.setVisibility(View.GONE);
                if (adView.getParent() == null) {
                    Activity currentActivity = MaxUnityAdManager.getCurrentActivity();
                    RelativeLayout relativeLayout = new RelativeLayout((Context)currentActivity);
                    currentActivity.addContentView((View)relativeLayout, (ViewGroup.LayoutParams)new LinearLayout.LayoutParams(-1, -1));
                    relativeLayout.addView((View)adView);
                    MaxUnityAdManager.this.mAdViewAdFormats.put(adUnitId, adFormat);
                    MaxUnityAdManager.this.positionAdView(adUnitId, adFormat);
                }
                Map<String, String> extraParameters = (Map<String, String>)MaxUnityAdManager.this.mAdViewExtraParametersToSetAfterCreate.get(adUnitId);
                if (adFormat.isBannerOrLeaderAd())
                    if (extraParameters == null || !extraParameters.containsKey("adaptive_banner"))
                        adView.setExtraParameter("adaptive_banner", "true");
                if (extraParameters != null) {
                    for (Map.Entry<String, String> extraParameter : extraParameters.entrySet()) {
                        adView.setExtraParameter(extraParameter.getKey(), extraParameter.getValue());
                        MaxUnityAdManager.this.maybeHandleExtraParameterChanges(adUnitId, adFormat, extraParameter.getKey(), extraParameter.getValue());
                    }
                    MaxUnityAdManager.this.mAdViewExtraParametersToSetAfterCreate.remove(adUnitId);
                }
                if (MaxUnityAdManager.this.mAdViewLocalExtraParametersToSetAfterCreate.containsKey(adUnitId)) {
                    Map<String, Object> localExtraParameters = (Map<String, Object>)MaxUnityAdManager.this.mAdViewLocalExtraParametersToSetAfterCreate.get(adUnitId);
                    if (localExtraParameters != null) {
                        for (Map.Entry<String, Object> localExtraParameter : localExtraParameters.entrySet())
                            adView.setLocalExtraParameter(localExtraParameter.getKey(), localExtraParameter.getValue());
                        MaxUnityAdManager.this.mAdViewLocalExtraParametersToSetAfterCreate.remove(adUnitId);
                    }
                }
                if (MaxUnityAdManager.this.mAdViewCustomDataToSetAfterCreate.containsKey(adUnitId)) {
                    String customData = (String)MaxUnityAdManager.this.mAdViewCustomDataToSetAfterCreate.get(adUnitId);
                    adView.setCustomData(customData);
                    MaxUnityAdManager.this.mAdViewCustomDataToSetAfterCreate.remove(adUnitId);
                }
                adView.loadAd();
                if (MaxUnityAdManager.this.mDisabledAutoRefreshAdViewAdUnitIds.contains(adUnitId))
                    adView.stopAutoRefresh();
                if (MaxUnityAdManager.this.mAdUnitIdsToShowAfterCreate.contains(adUnitId)) {
                    MaxUnityAdManager.this.showAdView(adUnitId, adFormat);
                    MaxUnityAdManager.this.mAdUnitIdsToShowAfterCreate.remove(adUnitId);
                }
            }
        });
    }

    private void loadAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                if (!MaxUnityAdManager.this.mDisabledAutoRefreshAdViewAdUnitIds.contains(adUnitId)) {
                    if (adView.getVisibility() != View.VISIBLE) {
                        MaxUnityAdManager.e("Auto-refresh will resume when the " + adFormat.getLabel() + " ad is shown. You should only call LoadBanner() or LoadMRec() if you explicitly pause auto-refresh and want to manually load an ad.");
                        return;
                    }
                    MaxUnityAdManager.e("You must stop auto-refresh if you want to manually load " + adFormat.getLabel() + " ads.");
                    return;
                }
                adView.loadAd();
            }
        });
    }

    private void setAdViewPlacement(final String adUnitId, final MaxAdFormat adFormat, final String placement) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Setting placement \"" + placement + "\" for " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\"");
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                adView.setPlacement(placement);
            }
        });
    }

    private void startAdViewAutoRefresh(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Starting " + adFormat.getLabel() + " auto refresh for ad unit identifier \"" + adUnitId + "\"");
                MaxUnityAdManager.this.mDisabledAutoRefreshAdViewAdUnitIds.remove(adUnitId);
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist for ad unit identifier \"" + adUnitId + "\"");
                    return;
                }
                adView.startAutoRefresh();
            }
        });
    }

    private void stopAdViewAutoRefresh(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Stopping " + adFormat.getLabel() + " auto refresh for ad unit identifier \"" + adUnitId + "\"");
                MaxUnityAdManager.this.mDisabledAutoRefreshAdViewAdUnitIds.add(adUnitId);
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist for ad unit identifier \"" + adUnitId + "\"");
                    return;
                }
                adView.stopAutoRefresh();
            }
        });
    }

    private void setAdViewWidth(final String adUnitId, final int widthDp, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Setting width " + widthDp + " for \"" + adFormat + "\" with ad unit identifier \"" + adUnitId + "\"");
                boolean isBannerOrLeader = adFormat.isBannerOrLeaderAd();
                int minWidthDp = isBannerOrLeader ? MaxAdFormat.BANNER.getSize().getWidth() : adFormat.getSize().getWidth();
                if (widthDp < minWidthDp)
                    MaxUnityAdManager.e("The provided width: " + widthDp + "dp is smaller than the minimum required width: " + minWidthDp + "dp for ad format: " + adFormat + ". Automatically setting width to " + minWidthDp + ".");
                int widthToSet = Math.max(minWidthDp, widthDp);
                MaxUnityAdManager.this.mAdViewWidths.put(adUnitId, Integer.valueOf(widthToSet));
                MaxUnityAdManager.this.positionAdView(adUnitId, adFormat);
            }
        });
    }

    private void updateAdViewPosition(final String adUnitId, final String adViewPosition, final Point offsetPixels, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Updating " + adFormat.getLabel() + " position to \"" + adViewPosition + "\" for ad unit id \"" + adUnitId + "\"");
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                MaxUnityAdManager.this.mAdViewPositions.put(adUnitId, adViewPosition);
                MaxUnityAdManager.this.mAdViewOffsets.put(adUnitId, offsetPixels);
                MaxUnityAdManager.this.positionAdView(adUnitId, adFormat);
            }
        });
    }

    private void showAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Showing " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\"");
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist for ad unit id \"" + adUnitId + "\"");
                    MaxUnityAdManager.this.mAdUnitIdsToShowAfterCreate.add(adUnitId);
                    return;
                }
                MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(View.VISIBLE);
                adView.setVisibility(View.VISIBLE);
                if (!MaxUnityAdManager.this.mDisabledAutoRefreshAdViewAdUnitIds.contains(adUnitId))
                    adView.startAutoRefresh();
            }
        });
    }

    private void hideAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Hiding " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\"");
                MaxUnityAdManager.this.mAdUnitIdsToShowAfterCreate.remove(adUnitId);
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(View.GONE);
                adView.setVisibility(View.GONE);
                adView.stopAutoRefresh();
            }
        });
    }

    private String getAdViewLayout(String adUnitId, MaxAdFormat adFormat) {
        d("Getting " + adFormat.getLabel() + " absolute position with ad unit id \"" + adUnitId + "\"");
        MaxAdView adView = retrieveAdView(adUnitId, adFormat);
        if (adView == null) {
            e(adFormat.getLabel() + " does not exist");
            return "";
        }
        int[] location = new int[2];
        adView.getLocationOnScreen(location);
        int originX = AppLovinSdkUtils.pxToDp((Context)getCurrentActivity(), location[0]);
        int originY = AppLovinSdkUtils.pxToDp((Context)getCurrentActivity(), location[1]);
        int width = AppLovinSdkUtils.pxToDp((Context)getCurrentActivity(), adView.getWidth());
        int height = AppLovinSdkUtils.pxToDp((Context)getCurrentActivity(), adView.getHeight());
        JSONObject rectMap = new JSONObject();
        JsonUtils.putString(rectMap, "origin_x", String.valueOf(originX));
        JsonUtils.putString(rectMap, "origin_y", String.valueOf(originY));
        JsonUtils.putString(rectMap, "width", String.valueOf(width));
        JsonUtils.putString(rectMap, "height", String.valueOf(height));
        return rectMap.toString();
    }

    private void destroyAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Destroying " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\"");
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                ViewParent parent = adView.getParent();
                if (parent instanceof ViewGroup)
                    ((ViewGroup)parent).removeView((View)adView);
                adView.setListener(null);
                adView.setRevenueListener(null);
                adView.setAdReviewListener(null);
                adView.destroy();
                MaxUnityAdManager.this.mAdViews.remove(adUnitId);
                MaxUnityAdManager.this.mAdViewAdFormats.remove(adUnitId);
                MaxUnityAdManager.this.mAdViewPositions.remove(adUnitId);
                MaxUnityAdManager.this.mAdViewOffsets.remove(adUnitId);
                MaxUnityAdManager.this.mAdViewWidths.remove(adUnitId);
                MaxUnityAdManager.this.mDisabledAdaptiveBannerAdUnitIds.remove(adUnitId);
            }
        });
    }

    private void setAdViewBackgroundColor(final String adUnitId, final MaxAdFormat adFormat, final String hexColorCode) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Setting " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\" to color: " + hexColorCode);
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                int backgroundColor = Color.parseColor(hexColorCode);
                MaxUnityAdManager.this.mPublisherBannerBackgroundColor = Integer.valueOf(backgroundColor);
                MaxUnityAdManager.this.mSafeAreaBackground.setBackgroundColor(backgroundColor);
                adView.setBackgroundColor(backgroundColor);
            }
        });
    }

    private void setAdViewExtraParameter(final String adUnitId, final MaxAdFormat adFormat, final String key, final String value) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Setting " + adFormat.getLabel() + " extra with key: \"" + key + "\" value: " + value);
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView != null) {
                    adView.setExtraParameter(key, value);
                } else {
                    MaxUnityAdManager.d(adFormat.getLabel() + " does not exist for ad unit ID \"" + adUnitId + "\". Saving extra parameter to be set when it is created.");
                    Map<String, String> extraParameters = (Map<String, String>)MaxUnityAdManager.this.mAdViewExtraParametersToSetAfterCreate.get(adUnitId);
                    if (extraParameters == null) {
                        extraParameters = new HashMap<>(1);
                        MaxUnityAdManager.this.mAdViewExtraParametersToSetAfterCreate.put(adUnitId, extraParameters);
                    }
                    extraParameters.put(key, value);
                }
                MaxUnityAdManager.this.maybeHandleExtraParameterChanges(adUnitId, adFormat, key, value);
            }
        });
    }

    private void setAdViewLocalExtraParameter(final String adUnitId, final MaxAdFormat adFormat, final String key, final Object value) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxUnityAdManager.d("Setting " + adFormat.getLabel() + " local extra with key: \"" + key + "\" value: " + value);
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView != null) {
                    adView.setLocalExtraParameter(key, value);
                } else {
                    MaxUnityAdManager.d(adFormat.getLabel() + " does not exist for ad unit ID \"" + adUnitId + "\". Saving local extra parameter to be set when it is created.");
                    Map<String, Object> localExtraParameters = (Map<String, Object>)MaxUnityAdManager.this.mAdViewLocalExtraParametersToSetAfterCreate.get(adUnitId);
                    if (localExtraParameters == null) {
                        localExtraParameters = new HashMap<>(1);
                        MaxUnityAdManager.this.mAdViewLocalExtraParametersToSetAfterCreate.put(adUnitId, localExtraParameters);
                    }
                    localExtraParameters.put(key, value);
                }
            }
        });
    }

    private void maybeHandleExtraParameterChanges(String adUnitId, MaxAdFormat adFormat, String key, String value) {
        if (MaxAdFormat.MREC != adFormat)
            if ("force_banner".equalsIgnoreCase(key)) {
                boolean shouldForceBanner = Boolean.parseBoolean(value);
                MaxAdFormat forcedAdFormat = shouldForceBanner ? MaxAdFormat.BANNER : getDeviceSpecificAdViewAdFormat();
                this.mAdViewAdFormats.put(adUnitId, forcedAdFormat);
                positionAdView(adUnitId, forcedAdFormat);
            } else if ("adaptive_banner".equalsIgnoreCase(key)) {
                boolean useAdaptiveBannerAdSize = Boolean.parseBoolean(value);
                if (useAdaptiveBannerAdSize) {
                    this.mDisabledAdaptiveBannerAdUnitIds.remove(adUnitId);
                } else {
                    this.mDisabledAdaptiveBannerAdUnitIds.add(adUnitId);
                }
                positionAdView(adUnitId, adFormat);
            }
    }

    private void setAdViewCustomData(final String adUnitId, final MaxAdFormat adFormat, final String customData) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView != null) {
                    adView.setCustomData(customData);
                } else {
                    MaxUnityAdManager.d(adFormat.getLabel() + " does not exist for ad unit ID \"" + adUnitId + "\". Saving custom data to be set when it is created.");
                    MaxUnityAdManager.this.mAdViewCustomDataToSetAfterCreate.put(adUnitId, customData);
                }
            }
        });
    }

    private void logInvalidAdFormat(MaxAdFormat adFormat) {
        logStackTrace(new IllegalStateException("invalid ad format: " + adFormat));
    }

    private void logStackTrace(Exception e) {
        e(Log.getStackTraceString(e));
    }

    private static void d(String message) {
        String fullMessage = "[MaxUnityAdManager] " + message;
        Log.d("AppLovinSdk", fullMessage);
    }

    private static void e(String message) {
        String fullMessage = "[MaxUnityAdManager] " + message;
        Log.e("AppLovinSdk", fullMessage);
    }

    private MaxInterstitialAd retrieveInterstitial(String adUnitId) {
        MaxInterstitialAd result = this.mInterstitials.get(adUnitId);
        if (result == null) {
            result = new MaxInterstitialAd(adUnitId, this.sdk, getCurrentActivity());
            result.setListener(this);
            result.setRevenueListener(this);
            result.setAdReviewListener(this);
            this.mInterstitials.put(adUnitId, result);
        }
        return result;
    }

    private MaxAppOpenAd retrieveAppOpenAd(String adUnitId) {
        MaxAppOpenAd result = this.mAppOpenAds.get(adUnitId);
        if (result == null) {
            result = new MaxAppOpenAd(adUnitId, this.sdk);
            result.setListener(this);
            result.setRevenueListener(this);
            this.mAppOpenAds.put(adUnitId, result);
        }
        return result;
    }

    private MaxRewardedAd retrieveRewardedAd(String adUnitId) {
        MaxRewardedAd result = this.mRewardedAds.get(adUnitId);
        if (result == null) {
            result = MaxRewardedAd.getInstance(adUnitId, this.sdk, getCurrentActivity());
            result.setListener(this);
            result.setRevenueListener(this);
            result.setAdReviewListener(this);
            this.mRewardedAds.put(adUnitId, result);
        }
        return result;
    }

    private MaxRewardedInterstitialAd retrieveRewardedInterstitialAd(String adUnitId) {
        MaxRewardedInterstitialAd result = this.mRewardedInterstitialAds.get(adUnitId);
        if (result == null) {
            result = new MaxRewardedInterstitialAd(adUnitId, this.sdk, getCurrentActivity());
            result.setListener(this);
            result.setRevenueListener(this);
            result.setAdReviewListener(this);
            this.mRewardedInterstitialAds.put(adUnitId, result);
        }
        return result;
    }

    private MaxAdView retrieveAdView(String adUnitId, MaxAdFormat adFormat) {
        return retrieveAdView(adUnitId, adFormat, null, null);
    }

    private MaxAdView retrieveAdView(String adUnitId, MaxAdFormat adFormat, String adViewPosition, Point adViewOffset) {
        MaxAdView result = this.mAdViews.get(adUnitId);
        if (result == null && adViewPosition != null && adViewOffset != null) {
            result = new MaxAdView(adUnitId, adFormat, this.sdk, (Context)getCurrentActivity());
            result.setListener(this);
            result.setRevenueListener(this);
            result.setAdReviewListener(this);
            this.mAdViews.put(adUnitId, result);
            this.mAdViewPositions.put(adUnitId, adViewPosition);
            this.mAdViewOffsets.put(adUnitId, adViewOffset);
            result.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
        }
        return result;
    }

    private void positionAdView(MaxAd ad) {
        positionAdView(ad.getAdUnitId(), ad.getFormat());
    }

    private void positionAdView(final String adUnitId, final MaxAdFormat adFormat) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                int adViewWidthDp, adViewHeightDp;
                MaxAdView adView = MaxUnityAdManager.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                RelativeLayout relativeLayout = (RelativeLayout)adView.getParent();
                if (relativeLayout == null) {
                    MaxUnityAdManager.e(adFormat.getLabel() + "'s parent does not exist");
                    return;
                }
                Rect windowRect = new Rect();
                relativeLayout.getWindowVisibleDisplayFrame(windowRect);
                String adViewPosition = (String)MaxUnityAdManager.this.mAdViewPositions.get(adUnitId);
                Point adViewOffset = (Point)MaxUnityAdManager.this.mAdViewOffsets.get(adUnitId);
                MaxUnityAdManager.Insets insets = MaxUnityAdManager.getSafeInsets();
                boolean isAdaptiveBannerDisabled = MaxUnityAdManager.this.mDisabledAdaptiveBannerAdUnitIds.contains(adUnitId);
                boolean isWidthDpOverridden = MaxUnityAdManager.this.mAdViewWidths.containsKey(adUnitId);
                if (isWidthDpOverridden) {
                    adViewWidthDp = ((Integer)MaxUnityAdManager.this.mAdViewWidths.get(adUnitId)).intValue();
                } else if ("top_center".equalsIgnoreCase(adViewPosition) || "bottom_center".equalsIgnoreCase(adViewPosition)) {
                    int adViewWidthPx = windowRect.width();
                    adViewWidthDp = AppLovinSdkUtils.pxToDp((Context)MaxUnityAdManager.getCurrentActivity(), adViewWidthPx);
                } else {
                    adViewWidthDp = adFormat.getSize().getWidth();
                }
                if ((adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER) && !isAdaptiveBannerDisabled) {
                    adViewHeightDp = adFormat.getAdaptiveSize(adViewWidthDp, (Context)MaxUnityAdManager.getCurrentActivity()).getHeight();
                } else {
                    adViewHeightDp = adFormat.getSize().getHeight();
                }
                int widthPx = AppLovinSdkUtils.dpToPx((Context)MaxUnityAdManager.getCurrentActivity(), adViewWidthDp);
                int heightPx = AppLovinSdkUtils.dpToPx((Context)MaxUnityAdManager.getCurrentActivity(), adViewHeightDp);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)adView.getLayoutParams();
                params.height = heightPx;
                adView.setLayoutParams((ViewGroup.LayoutParams)params);
                int gravity = 0;
                adView.setRotation(0.0F);
                adView.setTranslationX(0.0F);
                params.setMargins(0, 0, 0, 0);
                int marginLeft = insets.left + adViewOffset.x;
                int marginTop = insets.top + adViewOffset.y;
                int marginRight = insets.right;
                int marginBottom = insets.bottom;
                if ("centered".equalsIgnoreCase(adViewPosition)) {
                    gravity = 17;
                    if (MaxAdFormat.MREC == adFormat || isWidthDpOverridden) {
                        params.width = widthPx;
                    } else {
                        params.width = -1;
                    }
                } else {
                    if (adViewPosition.contains("top")) {
                        gravity = 48;
                    } else if (adViewPosition.contains("bottom")) {
                        gravity = 80;
                    }
                    if (adViewPosition.contains("center")) {
                        gravity |= 0x1;
                        if (MaxAdFormat.MREC == adFormat || isWidthDpOverridden) {
                            params.width = widthPx;
                        } else {
                            params.width = -1;
                        }
                        boolean containsLeft = adViewPosition.contains("left");
                        boolean containsRight = adViewPosition.contains("right");
                        if (containsLeft || containsRight) {
                            gravity |= 0x10;
                            if (MaxAdFormat.MREC == adFormat) {
                                gravity |= adViewPosition.contains("left") ? 3 : 5;
                            } else {
                                int windowWidth = windowRect.width() - insets.left - insets.right;
                                int windowHeight = windowRect.height() - insets.top - insets.bottom;
                                int longSide = Math.max(windowWidth, windowHeight);
                                int shortSide = Math.min(windowWidth, windowHeight);
                                int marginSign = (windowHeight > windowWidth) ? -1 : 1;
                                int margin = marginSign * (longSide - shortSide) / 2;
                                marginLeft += margin;
                                marginRight += margin;
                                int translationRaw = windowWidth / 2 - heightPx / 2;
                                int translationX = containsLeft ? -translationRaw : translationRaw;
                                adView.setTranslationX(translationX);
                                adView.setRotation(90.0F);
                            }
                            relativeLayout.setBackgroundColor(0);
                        }
                    } else {
                        params.width = widthPx;
                        if (adViewPosition.contains("left")) {
                            gravity |= 0x3;
                        } else if (adViewPosition.contains("right")) {
                            gravity |= 0x5;
                        }
                    }
                }
                if (MaxAdFormat.BANNER == adFormat || MaxAdFormat.LEADER == adFormat)
                    if (MaxUnityAdManager.this.mPublisherBannerBackgroundColor != null) {
                        FrameLayout.LayoutParams safeAreaLayoutParams = (FrameLayout.LayoutParams)MaxUnityAdManager.this.mSafeAreaBackground.getLayoutParams();
                        int safeAreaBackgroundGravity = 1;
                        if ("top_center".equals(adViewPosition)) {
                            safeAreaBackgroundGravity |= 0x30;
                            safeAreaLayoutParams.height = insets.top;
                            safeAreaLayoutParams.width = -1;
                            MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(adView.getVisibility());
                            marginLeft -= insets.left;
                            marginRight -= insets.right;
                        } else if ("bottom_center".equals(adViewPosition)) {
                            safeAreaBackgroundGravity |= 0x50;
                            safeAreaLayoutParams.height = insets.bottom;
                            safeAreaLayoutParams.width = -1;
                            MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(adView.getVisibility());
                            marginLeft -= insets.left;
                            marginRight -= insets.right;
                        } else {
                            MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(View.GONE);
                        }
                        safeAreaLayoutParams.gravity = safeAreaBackgroundGravity;
                        MaxUnityAdManager.this.mSafeAreaBackground.requestLayout();
                    } else {
                        MaxUnityAdManager.this.mSafeAreaBackground.setVisibility(View.GONE);
                    }
                params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
                relativeLayout.setGravity(gravity);
            }
        });
    }

    protected static Insets getSafeInsets() {
        Insets insets = new Insets();
        if (Build.VERSION.SDK_INT < 28)
            return insets;
        Window window = getCurrentActivity().getWindow();
        if (window == null)
            return insets;
        WindowInsets windowInsets = window.getDecorView().getRootWindowInsets();
        if (windowInsets == null)
            return insets;
        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        if (displayCutout == null)
            return insets;
        insets.left = displayCutout.getSafeInsetLeft();
        insets.top = displayCutout.getSafeInsetTop();
        insets.right = displayCutout.getSafeInsetRight();
        insets.bottom = displayCutout.getSafeInsetBottom();
        return insets;
    }

    private static void forwardUnityEvent(JSONObject args) {
        forwardUnityEvent(args, false);
    }

    private static void forwardUnityEvent(final JSONObject args, final boolean forwardInBackground) {
        sThreadPoolExecutor.execute(new Runnable() {
            public void run() {
                String serializedParameters = args.toString();
                if (forwardInBackground) {
                    MaxUnityAdManager.backgroundCallback.onEvent(serializedParameters);
                } else {
                    UnityPlayer.UnitySendMessage("MaxSdkCallbacks", "ForwardEvent", serializedParameters);
                }
            }
        });
    }

    protected static Map<String, String> deserializeParameters(String serialized) {
        if (!TextUtils.isEmpty(serialized))
            try {
                return JsonUtils.toStringMap(JsonUtils.jsonObjectFromJsonString(serialized, new JSONObject()));
            } catch (Throwable th) {
                e("Failed to deserialize: (" + serialized + ") with exception: " + th);
                return Collections.emptyMap();
            }
        return Collections.emptyMap();
    }

    private MaxAdFormat getAdViewAdFormat(String adUnitId) {
        if (this.mAdViewAdFormats.containsKey(adUnitId))
            return this.mAdViewAdFormats.get(adUnitId);
        return getDeviceSpecificAdViewAdFormat();
    }

    private static MaxAdFormat getDeviceSpecificAdViewAdFormat() {
        return AppLovinSdkUtils.isTablet((Context)getCurrentActivity()) ? MaxAdFormat.LEADER : MaxAdFormat.BANNER;
    }

    private static Activity getCurrentActivity() {
        return Utils.getCurrentActivity();
    }

    private static Point getOffsetPixels(float xDp, float yDp, Context context) {
        return new Point(AppLovinSdkUtils.dpToPx(context, (int)xDp), AppLovinSdkUtils.dpToPx(context, (int)yDp));
    }

    private MaxAd getAd(String adUnitId) {
        synchronized (this.mAdInfoMapLock) {
            return this.mAdInfoMap.get(adUnitId);
        }
    }

    private static class SdkThreadFactory implements ThreadFactory {
        private SdkThreadFactory() {}

        public Thread newThread(Runnable r) {
            Thread result = new Thread(r, "AppLovinSdk:Max-Unity-Plugin:shared");
            result.setDaemon(true);
            result.setPriority(5);
            result.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable th) {
                    Log.e("MaxUnityAdManager", "Caught unhandled exception", th);
                }
            });
            return result;
        }
    }

    public static interface BackgroundCallback {
        void onEvent(String param1String);
    }

    public static interface Listener {
        void onSdkInitializationComplete(AppLovinSdkConfiguration param1AppLovinSdkConfiguration);
    }
}


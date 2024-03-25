package com.tbase.maxads;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.applovin.impl.sdk.utils.JsonUtils;
import com.applovin.mediation.MaxMediatedNetworkInfo;
import com.applovin.sdk.AppLovinAdContentRating;
import com.applovin.sdk.AppLovinGender;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.applovin.sdk.AppLovinSdkUtils;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MaxUnityPlugin {
    private static final String TAG = "MaxUnityPlugin";

    private static final String SDK_TAG = "AppLovinSdk";

    private static AppLovinSdk sSdk;

    private static MaxUnityAdManager sAdManager;

    private static boolean sIsPluginInitialized = false;

    private static boolean sIsSdkInitialized = false;

    private static String sSdkKey;

    private static AppLovinSdkConfiguration sSdkConfiguration;

    private static String sUserIdToSet;

    private static String sUserSegmentNameToSet;

    private static List<String> sTestDeviceAdvertisingIds;

    private static Boolean sMutedToSet;

    private static Boolean sVerboseLogging;

    private static Boolean sCreativeDebuggerEnabled;

    private static Boolean sExceptionHandlerEnabled;

    private static Boolean sLocationCollectionEnabled;

    private static Integer sTargetingYearOfBirth;

    private static String sTargetingGender;

    private static Integer sTargetingMaximumAdContentRating;

    private static String sTargetingEmail;

    private static String sTargetingPhoneNumber;

    private static List<String> sTargetingKeywords;

    private static List<String> sTargetingInterests;

    private static final Map<String, String> sExtraParametersToSet = new HashMap<>();

    private static final Object sExtraParametersToSetLock = new Object();

    private static boolean isPluginInitialized() {
        return sIsPluginInitialized;
    }

    private static boolean isReadyToInteractWithSdk() {
        return (isPluginInitialized() && sSdk != null);
    }

    private static void maybeInitializePlugin() {
        if (isPluginInitialized())
            return;
        sAdManager = new MaxUnityAdManager();
        sIsPluginInitialized = true;
        sSdkKey = Utils.retrieveSdkKey();
    }

    public static void setSdkKey(String sdkKey) {
        if (TextUtils.isEmpty(sdkKey))
            return;
        maybeInitializePlugin();
        sSdkKey = sdkKey;
    }

    public static void initializeSdk(String serializedAdUnitIds, String serializedMetaData, MaxUnityAdManager.BackgroundCallback backgroundCallback) {
        JSONObject object = new JSONObject();
        JsonUtils.putString(object, "name", "OnInitialCallbackEvent");
        backgroundCallback.onEvent(object.toString());
        maybeInitializePlugin();
        sSdk = sAdManager.initializeSdkWithCompletionHandler(sSdkKey, generateSdkSettings(serializedAdUnitIds, serializedMetaData), backgroundCallback, new MaxUnityAdManager.Listener() {
            public void onSdkInitializationComplete(AppLovinSdkConfiguration sdkConfiguration) {
                MaxUnityPlugin.sSdkConfiguration = sdkConfiguration;
                MaxUnityPlugin.sIsSdkInitialized = true;
            }
        });
        if (!TextUtils.isEmpty(sUserIdToSet)) {
            sSdk.setUserIdentifier(sUserIdToSet);
            sUserIdToSet = null;
        }
        if (!TextUtils.isEmpty(sUserSegmentNameToSet)) {
            sSdk.getUserSegment().setName(sUserSegmentNameToSet);
            sUserSegmentNameToSet = null;
        }
        if (sTargetingYearOfBirth != null) {
            sSdk.getTargetingData().setYearOfBirth((sTargetingYearOfBirth.intValue() <= 0) ? null : sTargetingYearOfBirth);
            sTargetingYearOfBirth = null;
        }
        if (sTargetingGender != null) {
            sSdk.getTargetingData().setGender(getAppLovinGender(sTargetingGender));
            sTargetingGender = null;
        }
        if (sTargetingMaximumAdContentRating != null) {
            sSdk.getTargetingData().setMaximumAdContentRating(getAppLovinAdContentRating(sTargetingMaximumAdContentRating.intValue()));
            sTargetingMaximumAdContentRating = null;
        }
        if (sTargetingEmail != null) {
            sSdk.getTargetingData().setEmail(sTargetingEmail);
            sTargetingEmail = null;
        }
        if (sTargetingPhoneNumber != null) {
            sSdk.getTargetingData().setPhoneNumber(sTargetingPhoneNumber);
            sTargetingPhoneNumber = null;
        }
        if (sTargetingKeywords != null) {
            sSdk.getTargetingData().setKeywords(sTargetingKeywords);
            sTargetingKeywords = null;
        }
        if (sTargetingInterests != null) {
            sSdk.getTargetingData().setInterests(sTargetingInterests);
            sTargetingInterests = null;
        }
    }

    public static boolean isInitialized() {
        return (sIsPluginInitialized && sIsSdkInitialized);
    }

    public static void setUserId(String userId) {
        if (sSdk != null) {
            sSdk.setUserIdentifier(userId);
            sUserIdToSet = null;
        } else {
            sUserIdToSet = userId;
        }
    }

    public static void setUserSegmentField(String key, String value) {
        if (sSdk != null) {
            sSdk.getUserSegment().setName(value);
        } else {
            sUserSegmentNameToSet = value;
        }
    }

    public static void setTargetingDataYearOfBirth(int yearOfBirth) {
        if (sSdk == null) {
            sTargetingYearOfBirth = Integer.valueOf(yearOfBirth);
            return;
        }
        sSdk.getTargetingData().setYearOfBirth((yearOfBirth <= 0) ? null : Integer.valueOf(yearOfBirth));
    }

    public static void setTargetingDataGender(String gender) {
        if (sSdk == null) {
            sTargetingGender = gender;
            return;
        }
        sSdk.getTargetingData().setGender(getAppLovinGender(gender));
    }

    public static void setTargetingDataMaximumAdContentRating(int maximumAdContentRating) {
        if (sSdk == null) {
            sTargetingMaximumAdContentRating = Integer.valueOf(maximumAdContentRating);
            return;
        }
        sSdk.getTargetingData().setMaximumAdContentRating(getAppLovinAdContentRating(maximumAdContentRating));
    }

    public static void setTargetingDataEmail(String email) {
        if (sSdk == null) {
            sTargetingEmail = email;
            return;
        }
        sSdk.getTargetingData().setEmail(email);
    }

    public static void setTargetingDataPhoneNumber(String phoneNumber) {
        if (sSdk == null) {
            sTargetingPhoneNumber = phoneNumber;
            return;
        }
        sSdk.getTargetingData().setPhoneNumber(phoneNumber);
    }

    public static void setTargetingDataKeywords(String[] keywords) {
        List<String> keywordsList = (keywords != null) ? Arrays.<String>asList(keywords) : null;
        if (sSdk == null) {
            sTargetingKeywords = keywordsList;
            return;
        }
        sSdk.getTargetingData().setKeywords(keywordsList);
    }

    public static void setTargetingDataInterests(String[] interests) {
        List<String> interestsList = (interests != null) ? Arrays.<String>asList(interests) : null;
        if (sSdk == null) {
            sTargetingInterests = interestsList;
            return;
        }
        sSdk.getTargetingData().setInterests(interestsList);
    }

    public static void clearAllTargetingData() {
        if (sSdk == null) {
            sTargetingYearOfBirth = null;
            sTargetingGender = null;
            sTargetingMaximumAdContentRating = null;
            sTargetingEmail = null;
            sTargetingPhoneNumber = null;
            sTargetingKeywords = null;
            sTargetingInterests = null;
            return;
        }
        sSdk.getTargetingData().clearAll();
    }

    public static String getAvailableMediatedNetworks() {
        if (sSdk == null) {
            Log.d("[MaxUnityPlugin]", "Failed to get available mediated networks - please ensure the AppLovin MAX Unity Plugin has been initialized by calling 'MaxSdk.InitializeSdk();'!");
            return "";
        }
        List<MaxMediatedNetworkInfo> availableMediatedNetworks = sSdk.getAvailableMediatedNetworks();
        JSONArray serializedNetworks = new JSONArray();
        for (MaxMediatedNetworkInfo mediatedNetwork : availableMediatedNetworks) {
            JSONObject mediatedNetworkObject = new JSONObject();
            JsonUtils.putString(mediatedNetworkObject, "name", mediatedNetwork.getName());
            JsonUtils.putString(mediatedNetworkObject, "adapterClassName", mediatedNetwork.getAdapterClassName());
            JsonUtils.putString(mediatedNetworkObject, "adapterVersion", mediatedNetwork.getAdapterVersion());
            JsonUtils.putString(mediatedNetworkObject, "sdkVersion", mediatedNetwork.getSdkVersion());
            serializedNetworks.put(mediatedNetworkObject);
        }
        return serializedNetworks.toString();
    }

    public static void showMediationDebugger() {
        if (sSdk == null) {
            Log.d("[MaxUnityPlugin]", "Failed to show mediation debugger - please ensure the AppLovin MAX Unity Plugin has been initialized by calling 'MaxSdk.InitializeSdk();'!");
            return;
        }
        sSdk.showMediationDebugger();
    }

    public static void showCreativeDebugger() {
        if (sSdk == null) {
            Log.d("[MaxUnityPlugin]", "Failed to show creative debugger - please ensure the AppLovin MAX Unity Plugin has been initialized by calling 'MaxSdk.InitializeSdk();'!");
            return;
        }
        sSdk.showCreativeDebugger();
    }

    public static void preloadConsentDialog() {
        if (sSdk == null) {
            Log.e("[MaxUnityPlugin]", "Failed to preload consent dialog - please ensure the AppLovin MAX Unity Plugin has been initialized by calling 'MaxSdk.InitializeSdk();'!");
            return;
        }
        sSdk.getUserService().preloadConsentDialog();
    }

    public static void showConsentDialog() {
        if (sSdk == null) {
            Log.e("[MaxUnityPlugin]", "Failed to show consent dialog - please ensure the AppLovin MAX Unity Plugin has been initialized by calling 'MaxSdk.InitializeSdk();'!");
            return;
        }
        sSdk.getUserService().showConsentDialog(Utils.getCurrentActivity(), sAdManager);
    }

    public static int getConsentDialogState() {
        if (!isPluginInitialized())
            return AppLovinSdkConfiguration.ConsentDialogState.UNKNOWN.ordinal();
        return sSdkConfiguration.getConsentDialogState().ordinal();
    }

    public static String getSdkConfiguration() {
        if (sSdk == null) {
            Log.e("[MaxUnityPlugin]", "Failed to get SDK configuration - please ensure the AppLovin MAX Unity Plugin has been initialized by calling 'MaxSdk.InitializeSdk();'!");
            return "";
        }
        JSONObject args = new JSONObject();
        AppLovinSdkConfiguration sdkConfiguration = sSdk.getConfiguration();
        JsonUtils.putString(args, "consentFlowUserGeography", Integer.toString(sdkConfiguration.getConsentFlowUserGeography().ordinal()));
        JsonUtils.putString(args, "consentDialogState", Integer.toString(sdkConfiguration.getConsentDialogState().ordinal()));
        JsonUtils.putString(args, "countryCode", sdkConfiguration.getCountryCode());
        JsonUtils.putString(args, "isSuccessfullyInitialized", String.valueOf(sSdk.isInitialized()));
        JsonUtils.putBoolean(args, "isTestModeEnabled", sdkConfiguration.isTestModeEnabled());
        return args.toString();
    }

    public static void setHasUserConsent(boolean hasUserConsent) {
        AppLovinPrivacySettings.setHasUserConsent(hasUserConsent, (Context)Utils.getCurrentActivity());
    }

    public static boolean hasUserConsent() {
        return AppLovinPrivacySettings.hasUserConsent((Context)Utils.getCurrentActivity());
    }

    public static boolean isUserConsentSet() {
        return AppLovinPrivacySettings.isUserConsentSet((Context)Utils.getCurrentActivity());
    }

    public static void setIsAgeRestrictedUser(boolean isAgeRestrictedUser) {
        AppLovinPrivacySettings.setIsAgeRestrictedUser(isAgeRestrictedUser, (Context)Utils.getCurrentActivity());
    }

    public static boolean isAgeRestrictedUser() {
        return AppLovinPrivacySettings.isAgeRestrictedUser((Context)Utils.getCurrentActivity());
    }

    public static boolean isAgeRestrictedUserSet() {
        return AppLovinPrivacySettings.isAgeRestrictedUserSet((Context)Utils.getCurrentActivity());
    }

    public static void setDoNotSell(boolean doNotSell) {
        AppLovinPrivacySettings.setDoNotSell(doNotSell, (Context)Utils.getCurrentActivity());
    }

    public static boolean isDoNotSell() {
        return AppLovinPrivacySettings.isDoNotSell((Context)Utils.getCurrentActivity());
    }

    public static boolean isDoNotSellSet() {
        return AppLovinPrivacySettings.isDoNotSellSet((Context)Utils.getCurrentActivity());
    }

    public static void createBanner(String adUnitId, String bannerPosition) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("CreateBanner");
            return;
        }
        sAdManager.createBanner(adUnitId.trim(), bannerPosition);
    }

    public static void createBanner(String adUnitId, float x, float y) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("CreateBannerXY");
            return;
        }
        sAdManager.createBanner(adUnitId.trim(), x, y);
    }

    public static void loadBanner(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("LoadBanner");
            return;
        }
        sAdManager.loadBanner(adUnitId.trim());
    }

    public static void setBannerExtraParameter(String adUnitId, String key, String value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetBannerExtraParameter");
            return;
        }
        sAdManager.setBannerExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setBannerLocalExtraParameter(String adUnitId, String key, Object value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetBannerLocalExtraParameter");
            return;
        }
        sAdManager.setBannerLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setBannerLocalExtraParameterJson(String adUnitId, String key, String json) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetBannerLocalExtraParameter");
            return;
        }
        JSONObject jsonData = JsonUtils.deserialize(json);
        Object value = getLocalExtraParameterValue(jsonData);
        sAdManager.setBannerLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setBannerCustomData(String adUnitId, String customData) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("setBannerCustomData");
            return;
        }
        sAdManager.setBannerCustomData(adUnitId.trim(), customData);
    }

    public static void setBannerPlacement(String adUnitId, String placement) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetBannerPlacement");
            return;
        }
        sAdManager.setBannerPlacement(adUnitId.trim(), placement);
    }

    public static void startBannerAutoRefresh(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("StartBannerAutoRefresh");
            return;
        }
        sAdManager.startBannerAutoRefresh(adUnitId.trim());
    }

    public static void stopBannerAutoRefresh(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("StopBannerAutoRefresh");
            return;
        }
        sAdManager.stopBannerAutoRefresh(adUnitId.trim());
    }

    public static void setBannerWidth(String adUnitId, float widthDp) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetBannerWidth");
            return;
        }
        sAdManager.setBannerWidth(adUnitId.trim(), (int)widthDp);
    }

    public static void updateBannerPosition(String adUnitId, String bannerPosition) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("UpdateBannerPosition");
            return;
        }
        sAdManager.updateBannerPosition(adUnitId.trim(), bannerPosition);
    }

    public static void updateBannerPosition(String adUnitId, float x, float y) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("UpdateBannerPositionXY");
            return;
        }
        sAdManager.updateBannerPosition(adUnitId.trim(), x, y);
    }

    public static void showBanner(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("ShowBanner");
            return;
        }
        sAdManager.showBanner(adUnitId.trim());
    }

    public static void destroyBanner(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("DestroyBanner");
            return;
        }
        sAdManager.destroyBanner(adUnitId.trim());
    }

    public static void hideBanner(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("HideBanner");
            return;
        }
        sAdManager.hideBanner(adUnitId.trim());
    }

    public static String getBannerLayout(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("GetBannerLayout");
            return "";
        }
        return sAdManager.getBannerLayout(adUnitId.trim());
    }

    public static void setBannerBackgroundColor(String adUnitId, String hexColorCode) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetBannerBackgroundColor");
            return;
        }
        sAdManager.setBannerBackgroundColor(adUnitId.trim(), hexColorCode);
    }

    public static void createMRec(String adUnitId, String mrecPosition) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("CreateMRec");
            return;
        }
        sAdManager.createMRec(adUnitId.trim(), mrecPosition);
    }

    public static void createMRec(String adUnitId, float x, float y) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("CreateMRecXY");
            return;
        }
        sAdManager.createMRec(adUnitId.trim(), x, y);
    }

    public static void loadMRec(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("LoadMRec");
            return;
        }
        sAdManager.loadMRec(adUnitId.trim());
    }

    public static void setMRecPlacement(String adUnitId, String placement) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetMRecPlacement");
            return;
        }
        sAdManager.setMRecPlacement(adUnitId.trim(), placement);
    }

    public static void startMRecAutoRefresh(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("StartMRecAutoRefresh");
            return;
        }
        sAdManager.startMRecAutoRefresh(adUnitId.trim());
    }

    public static void stopMRecAutoRefresh(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("StopMRecAutoRefresh");
            return;
        }
        sAdManager.stopMRecAutoRefresh(adUnitId.trim());
    }

    public static void updateMRecPosition(String adUnitId, String placement) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("UpdateMRecPosition");
            return;
        }
        sAdManager.updateMRecPosition(adUnitId.trim(), placement);
    }

    public static void updateMRecPosition(String adUnitId, float x, float y) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("UpdateMRecPositionXY");
            return;
        }
        sAdManager.updateMRecPosition(adUnitId.trim(), x, y);
    }

    public static void showMRec(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("ShowMRec");
            return;
        }
        sAdManager.showMRec(adUnitId.trim());
    }

    public static void destroyMRec(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("DestroyMRec");
            return;
        }
        sAdManager.destroyMRec(adUnitId.trim());
    }

    public static void hideMRec(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("HideMRec");
            return;
        }
        sAdManager.hideMRec(adUnitId.trim());
    }

    public static void setMRecExtraParameter(String adUnitId, String key, String value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetMRecExtraParameter");
            return;
        }
        sAdManager.setMRecExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setMRecLocalExtraParameter(String adUnitId, String key, Object value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetMRecLocalExtraParameter");
            return;
        }
        sAdManager.setMRecLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setMRecLocalExtraParameterJson(String adUnitId, String key, String json) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetMRecLocalExtraParameter");
            return;
        }
        JSONObject jsonData = JsonUtils.deserialize(json);
        Object value = getLocalExtraParameterValue(jsonData);
        sAdManager.setMRecLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setMRecCustomData(String adUnitId, String customData) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("setMRecCustomData");
            return;
        }
        sAdManager.setMRecCustomData(adUnitId.trim(), customData);
    }

    public static String getMRecLayout(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("GetMRecLayout");
            return "";
        }
        return sAdManager.getMRecLayout(adUnitId.trim());
    }

    public static void loadInterstitial(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("LoadInterstitial");
            return;
        }
        sAdManager.loadInterstitial(adUnitId.trim());
    }

    public static boolean isInterstitialReady(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("IsInterstitialReady");
            return false;
        }
        return sAdManager.isInterstitialReady(adUnitId.trim());
    }

    public static void showInterstitial(String adUnitId, String placement, String customData) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("ShowInterstitial");
            return;
        }
        sAdManager.showInterstitial(adUnitId.trim(), placement, customData);
    }

    public static void setInterstitialExtraParameter(String adUnitId, String key, String value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetInterstitialExtraParameter");
            return;
        }
        sAdManager.setInterstitialExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setInterstitialLocalExtraParameter(String adUnitId, String key, Object value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetInterstitialLocalExtraParameter");
            return;
        }
        sAdManager.setInterstitialLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setInterstitialLocalExtraParameterJson(String adUnitId, String key, String json) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetInterstitialLocalExtraParameter");
            return;
        }
        JSONObject jsonData = JsonUtils.deserialize(json);
        Object value = getLocalExtraParameterValue(jsonData);
        sAdManager.setInterstitialLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void loadAppOpenAd(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("LoadAppOpenAd");
            return;
        }
        sAdManager.loadAppOpenAd(adUnitId.trim());
    }

    public static boolean isAppOpenAdReady(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("IsAppOpenAdReady");
            return false;
        }
        return sAdManager.isAppOpenAdReady(adUnitId.trim());
    }

    public static void showAppOpenAd(String adUnitId, String placement, String customData) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("ShowAppOpenAd");
            return;
        }
        sAdManager.showAppOpenAd(adUnitId.trim(), placement, customData);
    }

    public static void setAppOpenAdExtraParameter(String adUnitId, String key, String value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetAppOpenAdExtraParameter");
            return;
        }
        sAdManager.setAppOpenAdExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setAppOpenAdLocalExtraParameter(String adUnitId, String key, Object value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetAppOpenAdLocalExtraParameter");
            return;
        }
        sAdManager.setAppOpenAdLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setAppOpenAdLocalExtraParameterJson(String adUnitId, String key, String json) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetAppOpenAdLocalExtraParameter");
            return;
        }
        JSONObject jsonData = JsonUtils.deserialize(json);
        Object value = getLocalExtraParameterValue(jsonData);
        sAdManager.setAppOpenAdLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void loadRewardedAd(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("LoadRewardedAd");
            return;
        }
        sAdManager.loadRewardedAd(adUnitId.trim());
    }

    public static boolean isRewardedAdReady(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("IsRewardedAdReady");
            return false;
        }
        return sAdManager.isRewardedAdReady(adUnitId.trim());
    }

    public static void showRewardedAd(String adUnitId, String placement, String customData) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("ShowRewardedAd");
            return;
        }
        sAdManager.showRewardedAd(adUnitId.trim(), placement, customData);
    }

    public static void setRewardedAdExtraParameter(String adUnitId, String key, String value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetRewardedAdExtraParameter");
            return;
        }
        sAdManager.setRewardedAdExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setRewardedAdLocalExtraParameter(String adUnitId, String key, Object value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetRewardedAdLocalExtraParameter");
            return;
        }
        sAdManager.setRewardedAdLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setRewardedAdLocalExtraParameterJson(String adUnitId, String key, String json) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetRewardedAdLocalExtraParameter");
            return;
        }
        JSONObject jsonData = JsonUtils.deserialize(json);
        Object value = getLocalExtraParameterValue(jsonData);
        sAdManager.setRewardedAdLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void loadRewardedInterstitialAd(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("LoadRewardedInterstitialAd");
            return;
        }
        sAdManager.loadRewardedInterstitialAd(adUnitId.trim());
    }

    public static boolean isRewardedInterstitialAdReady(String adUnitId) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("IsRewardedInterstitialAdReady");
            return false;
        }
        return sAdManager.isRewardedInterstitialAdReady(adUnitId.trim());
    }

    public static void showRewardedInterstitialAd(String adUnitId, String placement, String customData) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("ShowRewardedInterstitialAd");
            return;
        }
        sAdManager.showRewardedInterstitialAd(adUnitId.trim(), placement, customData);
    }

    public static void setRewardedInterstitialAdExtraParameter(String adUnitId, String key, String value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetRewardedInterstitialAdExtraParameter");
            return;
        }
        sAdManager.setRewardedInterstitialAdExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setRewardedInterstitialAdLocalExtraParameter(String adUnitId, String key, Object value) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetRewardedInterstitialAdLocalExtraParameter");
            return;
        }
        sAdManager.setRewardedInterstitialAdLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void setRewardedInterstitialAdLocalExtraParameterJson(String adUnitId, String key, String json) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("SetRewardedInterstitialAdLocalExtraParameter");
            return;
        }
        JSONObject jsonData = JsonUtils.deserialize(json);
        Object value = getLocalExtraParameterValue(jsonData);
        sAdManager.setRewardedInterstitialAdLocalExtraParameter(adUnitId.trim(), key, value);
    }

    public static void trackEvent(String event, String parameters) {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("TrackEvent");
            return;
        }
        sAdManager.trackEvent(event, parameters);
    }

    public static boolean isTablet() {
        return AppLovinSdkUtils.isTablet((Context)Utils.getCurrentActivity());
    }

    public static boolean isPhysicalDevice() {
        return !AppLovinSdkUtils.isEmulator();
    }

    public static int getTcfVendorConsentStatus(int vendorId) {
        Boolean consentStatus = AppLovinPrivacySettings.getTcfVendorConsentStatus(vendorId);
        return getConsentStatusValue(consentStatus);
    }

    public static int getAdditionalConsentStatus(int atpId) {
        Boolean consentStatus = AppLovinPrivacySettings.getAdditionalConsentStatus(atpId);
        return getConsentStatusValue(consentStatus);
    }

    public static int getPurposeConsentStatus(int purposeId) {
        Boolean consentStatus = AppLovinPrivacySettings.getPurposeConsentStatus(purposeId);
        return getConsentStatusValue(consentStatus);
    }

    public static int getSpecialFeatureOptInStatus(int specialFeatureId) {
        Boolean consentStatus = AppLovinPrivacySettings.getSpecialFeatureOptInStatus(specialFeatureId);
        return getConsentStatusValue(consentStatus);
    }

    public static boolean isMuted() {
        if (sSdk != null)
            return sSdk.getSettings().isMuted();
        if (sMutedToSet != null)
            return sMutedToSet.booleanValue();
        return false;
    }

    public static void setMuted(boolean muted) {
        if (sSdk != null) {
            sSdk.getSettings().setMuted(muted);
            sMutedToSet = null;
        } else {
            sMutedToSet = Boolean.valueOf(muted);
        }
    }

    public static float getScreenDensity() {
        return (Utils.getCurrentActivity().getResources().getDisplayMetrics()).density;
    }

    public static String getAdInfo(String adUnitId) {
        return sAdManager.getAdInfo(adUnitId.trim());
    }

    public static String getAdValue(String adUnitId, String key) {
        return sAdManager.getAdValue(adUnitId.trim(), key);
    }

    public static void setVerboseLogging(boolean enabled) {
        if (sSdk != null) {
            sSdk.getSettings().setVerboseLogging(enabled);
            sVerboseLogging = null;
        } else {
            sVerboseLogging = Boolean.valueOf(enabled);
        }
    }

    public static boolean isVerboseLoggingEnabled() {
        if (sSdk != null)
            return sSdk.getSettings().isVerboseLoggingEnabled();
        if (sVerboseLogging != null)
            return sVerboseLogging.booleanValue();
        return false;
    }

    public static void setCreativeDebuggerEnabled(boolean enabled) {
        if (sSdk != null) {
            sSdk.getSettings().setCreativeDebuggerEnabled(enabled);
            sCreativeDebuggerEnabled = null;
        } else {
            sCreativeDebuggerEnabled = Boolean.valueOf(enabled);
        }
    }

    public static void setExceptionHandlerEnabled(boolean enabled) {
        if (sSdk != null) {
            sSdk.getSettings().setExceptionHandlerEnabled(enabled);
            sExceptionHandlerEnabled = null;
        } else {
            sExceptionHandlerEnabled = Boolean.valueOf(enabled);
        }
    }

    public static void setLocationCollectionEnabled(boolean enabled) {
        if (sSdk != null) {
            sSdk.getSettings().setLocationCollectionEnabled(enabled);
            sLocationCollectionEnabled = null;
        } else {
            sLocationCollectionEnabled = Boolean.valueOf(enabled);
        }
    }

    public static void setExtraParameter(String key, String value) {
        if (TextUtils.isEmpty(key)) {
            Log.e("AppLovinSdk", "[MaxUnityPlugin] ERROR: Failed to set extra parameter for null or empty key: " + key);
            return;
        }
        if (sSdk != null) {
            AppLovinSdkSettings settings = sSdk.getSettings();
            settings.setExtraParameter(key, value);
            setPendingExtraParametersIfNeeded(settings);
        } else {
            synchronized (sExtraParametersToSetLock) {
                sExtraParametersToSet.put(key, value);
            }
        }
    }

    public static int[] getSafeAreaInsets() {
        MaxUnityAdManager.Insets insets = MaxUnityAdManager.getSafeInsets();
        return new int[] { insets.left, insets.top, insets.right, insets.bottom };
    }

    public static void showCmpForExistingUser() {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("showCmpForExistingUser");
            return;
        }
        sAdManager.showCmpForExistingUser();
    }

    public static boolean hasSupportedCmp() {
        if (!isReadyToInteractWithSdk()) {
            logUninitializedAccessError("hasSupportedCmp");
            return false;
        }
        return sSdk.getCmpService().hasSupportedCmp();
    }

    public static void setTestDeviceAdvertisingIds(String[] advertisingIds) {
        sTestDeviceAdvertisingIds = Arrays.asList(advertisingIds);
    }

    public static float getAdaptiveBannerHeight(float width) {
        return MaxUnityAdManager.getAdaptiveBannerHeight(width);
    }

    private static void setPendingExtraParametersIfNeeded(AppLovinSdkSettings settings) {
        Map<String, String> extraParameters;
        synchronized (sExtraParametersToSetLock) {
            if (sExtraParametersToSet.size() <= 0)
                return;
            extraParameters = new HashMap<>(sExtraParametersToSet);
            sExtraParametersToSet.clear();
        }
        for (String key : extraParameters.keySet())
            settings.setExtraParameter(key, extraParameters.get(key));
    }

    private static AppLovinSdkSettings generateSdkSettings(String serializedAdUnitIds, String serializedMetaData) {
        AppLovinSdkSettings settings = new AppLovinSdkSettings((Context)Utils.getCurrentActivity());
        setPendingExtraParametersIfNeeded(settings);
        if (sTestDeviceAdvertisingIds != null && !sTestDeviceAdvertisingIds.isEmpty()) {
            settings.setTestDeviceAdvertisingIds(sTestDeviceAdvertisingIds);
            sTestDeviceAdvertisingIds = null;
        }
        if (sMutedToSet != null) {
            settings.setMuted(sMutedToSet.booleanValue());
            sMutedToSet = null;
        }
        if (sVerboseLogging != null) {
            settings.setVerboseLogging(sVerboseLogging.booleanValue());
            sVerboseLogging = null;
        }
        if (sCreativeDebuggerEnabled != null) {
            settings.setCreativeDebuggerEnabled(sCreativeDebuggerEnabled.booleanValue());
            sCreativeDebuggerEnabled = null;
        }
        if (sExceptionHandlerEnabled != null) {
            settings.setExceptionHandlerEnabled(sExceptionHandlerEnabled.booleanValue());
            sExceptionHandlerEnabled = null;
        }
        if (sLocationCollectionEnabled != null) {
            settings.setLocationCollectionEnabled(sLocationCollectionEnabled.booleanValue());
            sLocationCollectionEnabled = null;
        }
        List<String> adUnitIds = Arrays.asList(serializedAdUnitIds.split(","));
        settings.setInitializationAdUnitIds(adUnitIds);
        Map<String, String> unityMetaDataMap = MaxUnityAdManager.deserializeParameters(serializedMetaData);
        try {
            Field metaDataField = AppLovinSdkSettings.class.getDeclaredField("metaData");
            metaDataField.setAccessible(true);
            Map<String, String> metaDataMap = (Map<String, String>)metaDataField.get(settings);
            for (Map.Entry<String, String> metaDataEntry : unityMetaDataMap.entrySet())
                metaDataMap.put(metaDataEntry.getKey(), metaDataEntry.getValue());
        } catch (Exception exception) {}
        return settings;
    }

    private static AppLovinGender getAppLovinGender(String gender) {
        if ("F".equalsIgnoreCase(gender))
            return AppLovinGender.FEMALE;
        if ("M".equalsIgnoreCase(gender))
            return AppLovinGender.MALE;
        if ("O".equalsIgnoreCase(gender))
            return AppLovinGender.OTHER;
        return AppLovinGender.UNKNOWN;
    }

    private static AppLovinAdContentRating getAppLovinAdContentRating(int maximumAdContentRating) {
        if (maximumAdContentRating == 1)
            return AppLovinAdContentRating.ALL_AUDIENCES;
        if (maximumAdContentRating == 2)
            return AppLovinAdContentRating.EVERYONE_OVER_TWELVE;
        if (maximumAdContentRating == 3)
            return AppLovinAdContentRating.MATURE_AUDIENCES;
        return AppLovinAdContentRating.NONE;
    }

    private static Object getLocalExtraParameterValue(JSONObject jsonData) {
        Object value = JsonUtils.getObject(jsonData, "value", null);
        if (value instanceof JSONArray)
            return JsonUtils.optList((JSONArray)value, null);
        if (value instanceof JSONObject)
            try {
                return JsonUtils.toStringObjectMap((JSONObject)value);
            } catch (JSONException jsonException) {
                Log.e("AppLovinSdk", "Failed to create map from local extra parameter data: " + jsonData, (Throwable)jsonException);
                return null;
            }
        return value;
    }

    private static int getConsentStatusValue(Boolean consentStatus) {
        if (consentStatus != null)
            return consentStatus.booleanValue() ? 1 : 0;
        return -1;
    }

    private static void logUninitializedAccessError(String callingMethod) {
        Log.e("AppLovinSdk", "[MaxUnityPlugin] ERROR: Failed to execute " + callingMethod + "() - please ensure the AppLovin MAX Unity Plugin has been initialized by calling 'MaxSdk.InitializeSdk();'!");
    }
}


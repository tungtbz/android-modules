package com.tungt.unitybridge;

import android.app.Activity;
import android.util.Log;

import com.rofi.admobadshelper.AdmobHelper;
import com.rofi.ads.AdsManager;
import com.rofi.analytic.AnalyticServices;
import com.rofi.remoteconfig.FirebaseRemoteConfigService;
import com.unity3d.player.UnityPlayer;

public class UnityPluginManager {
    private static final String UNITY_CLASS_TO_GET_MESSAGE = "NativePluginBridge";

    public static void Init(){
        
    }

    public static boolean IsRewardedVideoAvailable() {
        return AdsManager.getInstance().GetService().IsRewardReady();
    }

    public static void ShowVideoAds(int requestCode) {
        Activity activity = UnityPlayer.currentActivity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AdsManager.getInstance().GetService().ShowReward(requestCode);
            }
        });
    }

    public static boolean IsInterAdsAvailable() {
        return AdsManager.getInstance().GetService().IsInterReady();
    }

    public static void ShowInterAds(int requestCode) {
        Activity activity = UnityPlayer.currentActivity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AdsManager.getInstance().GetService().ShowInter(requestCode);
            }
        });
    }

    public static void ShowBanner(int bannerType) {
        Activity activity = UnityPlayer.currentActivity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bannerType == 0)
                    AdsManager.getInstance().GetService().ShowBanner(UnityPlayer.currentActivity);
                else AdmobHelper.getInstance().showBanner();
            }
        });
    }

    public static void HideBanner() {
        Activity activity = UnityPlayer.currentActivity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AdmobHelper.getInstance().HideBanner();
                AdsManager.getInstance().GetService().HideBanner();
            }
        });
    }

    public static void ShowRECTAds() {
        Activity activity = UnityPlayer.currentActivity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AdsManager.getInstance().GetService().ShowMREC(UnityPlayer.currentActivity);
            }
        });
    }

    public static void HideMRECT() {
        Activity activity = UnityPlayer.currentActivity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AdsManager.getInstance().GetService().HideMREC();
            }
        });
    }

    public static void LogEvent(String eventName, String eventData) {
        AnalyticServices.getInstance().LogEvent(UnityPlayer.currentActivity, eventName, eventData);
    }

    public static int GetRemoteConfigIntValue(String key) {
        return FirebaseRemoteConfigService.getInstance().GetInt(key);
    }

    public static String GetRemoteConfigStringValue(String key) {
        return FirebaseRemoteConfigService.getInstance().GetString(key);
    }

    public static boolean GetRemoteConfigBoolValue(String key) {
        return FirebaseRemoteConfigService.getInstance().GetBoolean(key);
    }

    //0: open ads
    //1: admob
    public static boolean IsOpenAppAdsAvailable(int openAdsType) {
        if (openAdsType == 1) {
            return AdmobHelper.getInstance().isAdAvailable();
        } else {
            return AdsManager.getInstance().GetService().IsOpenAppAdsAvailable();
        }
    }

    //0: open ads
    //1: admob
    public static void LoadOpenAppAds(int openAdsType) {
        Activity activity = UnityPlayer.currentActivity;

        if (openAdsType == 1) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AdmobHelper.getInstance().loadAd(activity);
                }
            });
        } else AdsManager.getInstance().GetService().LoadOpenAppAds(activity);
    }
}

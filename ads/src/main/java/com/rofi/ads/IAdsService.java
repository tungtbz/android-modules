package com.rofi.ads;

import android.app.Activity;

public interface IAdsService {
    void Init(Activity activity, String[] args);

    //reward
    boolean IsRewardReady();

    void ShowReward(int requestCode);

    //inter
    boolean isInterstitialReady();

    void showInterstitial(int requestCode);

    //banner
    void ShowBanner(Activity activity);

    void HideBanner();

    //mrec
    void showMRec(Activity activity);

    void hideMRec();

    //native mrec
    void ShowNativeMREC(Activity activity);

    void HideNativeMREC();

    //native banner
    void ShowNativeBanner(Activity activity);

    void HideNativeBanner();

    void OnPause(Activity activity);

    void onResume(Activity activity);

    void SetEventListener(AdsEventListener listener);

    void IncreaseBlockAutoShowInter();

    void DecreaseBlockAutoShowInter();

    void loadAppOpenAd(Activity activity);

    void showAppOpenAd(Activity activity);

    boolean isAppOpenAdReady();

    void DisableResumeAds();
    void EnableResumeAds();

    void DisableInterAds();
    void EnableInterAds();
}

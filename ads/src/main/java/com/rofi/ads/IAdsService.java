package com.rofi.ads;

import android.app.Activity;

public interface IAdsService {
    void init(Activity activity, String[] args);

    void setBackgroundCallback(AdsManager.BackgroundCallback backgroundCallback);
    //reward
    boolean isRewardedAdReady();

    void showRewardedAd(int requestCode);

    //inter
    boolean isInterstitialReady();

    void showInterstitial(int requestCode);

    //banner
    void showBanner(String position);

    void hideBanner();

    //mrec
    void showMRec(Activity activity);

    void hideMRec();

    //native mrec
    void ShowNativeMREC(Activity activity);

    void HideNativeMREC();

    //native banner
    void ShowNativeBanner(Activity activity);

    void HideNativeBanner();

    void loadAppOpenAd(Activity activity);

    void showAppOpenAd(Activity activity);

    boolean isAppOpenAdReady();

    void OnPause(Activity activity);

    void onResume(Activity activity);

    void SetEventListener(AdsEventListener listener);

    void IncreaseBlockAutoShowInter();

    void DecreaseBlockAutoShowInter();

    void DisableResumeAds();

    void EnableResumeAds();

    void DisableInterAds();

    void EnableInterAds();
}

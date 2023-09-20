package com.rofi.ads;

import android.app.Activity;

public interface IAdsService {
    void Init(Activity activity, String[] args);

    //reward
    boolean IsRewardReady();

    void ShowReward(int requestCode);

    //inter
    boolean IsInterReady();

    void ShowInter(int requestCode);

    //banner
    void ShowBanner(Activity activity);

    void HideBanner();

    //mrec
    void ShowMREC(Activity activity);

    void HideMREC();

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

    void LoadOpenAppAds(Activity activity);

    void ShowOpenAppAds(Activity activity);

    boolean IsOpenAppAdsAvailable();
}

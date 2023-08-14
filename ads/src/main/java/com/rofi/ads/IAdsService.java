package com.rofi.ads;

import android.app.Activity;

public interface IAdsService {
    void Init(Activity activity, String[] args);

    boolean IsRewardReady();

    boolean IsInterReady();

    void ShowReward(int requestCode);

    void ShowInter(int requestCode);

    void ShowBanner(Activity activity, int screenCode);

    void HideBanner(int screenCode);

    void ShowMREC(Activity activity);

    void HideMREC();

    void OnPause(Activity activity);

    void onResume(Activity activity);

    void SetEventListener(AdsEventListener listener);
}

package com.rofi.ads;

import android.app.Activity;

public interface IAdsService {
    void Init(Activity activity);

    boolean IsRewardReady();
    boolean IsInterReady();

    void ShowReward(int requestCode);

    void ShowInter(int requestCode);

    void ShowBanner(Activity activity);

    void HideBanner();

    void ShowMREC(Activity activity);

    void HideMREC();

    void OnPause();

    void onResume();

    void SetEventListener(AdsEventListener listener);
}

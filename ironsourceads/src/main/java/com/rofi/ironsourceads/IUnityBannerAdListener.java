package com.rofi.ironsourceads;

public interface IUnityBannerAdListener {
    void onAdLoaded(String paramString);

    void onAdLoadFailed(String paramString);

    void onAdDisplayed(String paramString);

    void onAdDisplayFailed(String paramString1, String paramString2);

    void onAdClicked(String paramString);

    void onAdExpanded(String paramString);

    void onAdCollapsed(String paramString);

    void onAdLeftApplication(String paramString);
}

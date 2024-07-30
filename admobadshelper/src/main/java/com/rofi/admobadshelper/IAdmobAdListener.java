package com.rofi.admobadshelper;

public interface IAdmobAdListener {
    void onAdImpression(String adFormat, String adUnitId, String adNetwork, double value);

    void onAdDisplayFullScreenContent(int type);
    void onAdDismissedFullScreenContent(int type);

    void onAdClicked();

    void onBannerLoaded();
    void onBannerCollapDisplay();
}

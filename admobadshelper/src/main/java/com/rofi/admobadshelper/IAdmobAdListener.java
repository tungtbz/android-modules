package com.rofi.admobadshelper;

public interface IAdmobAdListener {
    void onAdImpression(String adFormat, String adUnitId, String adNetwork, double value);

    void onAdDismissedFullScreenContent(int type);

    void onAdClicked();
}

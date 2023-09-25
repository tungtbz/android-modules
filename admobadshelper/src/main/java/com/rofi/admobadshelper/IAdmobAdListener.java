package com.rofi.admobadshelper;

public interface IAdmobAdListener {
    void onShowAdComplete();
    void onAdImpression(String adUnitId, String adNetwork, double value);
}

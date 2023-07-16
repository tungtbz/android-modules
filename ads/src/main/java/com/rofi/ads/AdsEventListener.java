package com.rofi.ads;

public interface AdsEventListener {
    void onVideoRewardLoaded();

    void onVideoRewardDisplayed();

    void onVideoRewardRewarded();

    void onVideoRewardUserRewarded(String requestCode);


    void onInterLoaded();

    void onInterDisplayed();

    void onInterHidden(String code);

    void onAdClicked(String adFormat);

    void onAdRevenuePaid(String adFormat, String adUnitId, String adNetwork, double value);

}

package com.rofi.admobadshelper;

public interface IAdmobAdListener {
    void onAdImpression(String adFormat, String adUnitId, String adNetwork, double value);

    void onAdDisplayFullScreenContent(int type);
    void onAdDismissedFullScreenContent(int type);
    void onAOAFailedToLoad();

    void onAdClicked();

    void onBannerLoaded();
    void onBannerCollapDisplay();
    
    /**
     * Called when user earns reward from Rewarded Ad
     * @param type Reward type (e.g., "coins", "lives", etc.)
     * @param amount Reward amount
     */
    void onUserEarnedReward(String type, int amount);
}

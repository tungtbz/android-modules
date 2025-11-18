package com.rofi.admobadshelper;

public interface IAdmobAdListener {
    void onAdImpression(String adFormat, String adUnitId, String adNetwork, double value);

    void onAdDisplayFullScreenContent(int type);
    void onAdDismissedFullScreenContent(int type);
    
    /**
     * Called when Interstitial ad is dismissed
     * @param interstitialCode Custom code passed when showing interstitial ad
     */
    void onInterstitialDismissed(int interstitialCode);
    
    void onAOAFailedToLoad();

    void onAdClicked();

    void onBannerLoaded();
    void onBannerCollapDisplay();
    
    void onMrecLoaded();
    void onInterstitialLoaded();
    void onRewardedLoaded();
    
    /**
     * Called when user earns reward from Rewarded Ad
     * @param rewardCode Custom reward code passed when showing ad
     * @param type Reward type (e.g., "coins", "lives", etc.)
     * @param amount Reward amount
     */
    void onUserEarnedReward(int rewardCode, String type, int amount);
}

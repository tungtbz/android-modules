package com.rofi.admobadshelper;

public interface IAdmobAdListener {
    void onAdInitialized(String failedMessage);
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
    
    /**
     * Auto-reload retry callbacks
     * Called when ad fails to load and is scheduling a retry
     */
    
    /**
     * Called when Banner ad is retrying to load after failure
     * @param attemptCount Number of retry attempts (1-based)
     * @param delayMs Delay in milliseconds before next retry
     */
    void onBannerRetrying(int attemptCount, long delayMs);
    
    /**
     * Called when MREC ad is retrying to load after failure
     * @param attemptCount Number of retry attempts (1-based)
     * @param delayMs Delay in milliseconds before next retry
     */
    void onMrecRetrying(int attemptCount, long delayMs);
    
    /**
     * Called when Interstitial ad is retrying to load after failure
     * @param attemptCount Number of retry attempts (1-based)
     * @param delayMs Delay in milliseconds before next retry
     */
    void onInterstitialRetrying(int attemptCount, long delayMs);
    
    /**
     * Called when Rewarded ad is retrying to load after failure
     * @param attemptCount Number of retry attempts (1-based)
     * @param delayMs Delay in milliseconds before next retry
     */
    void onRewardedRetrying(int attemptCount, long delayMs);
}

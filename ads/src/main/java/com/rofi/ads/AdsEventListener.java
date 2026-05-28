package com.rofi.ads;

public interface AdsEventListener {
    void onAOADisplayed();
    void onAOAAdHidden();
    void onAOAFailedToLoad();

    void onAdServiceLoaded();

    void onVideoRewardLoaded();

    void onVideoRewardDisplayed();

    /**
     * Called when a rewarded ad fails to display after {@code showAd()} was invoked.
     * Implementations MUST unblock the user here (e.g., grant the reward anyway or
     * show a fallback) because {@code onVideoRewardUserRewarded} will NOT be called.
     *
     * @param requestCode The request code originally passed to {@code ShowReward()}.
     */
    void onVideoRewardDisplayFailed(String requestCode);

    void onVideoRewardClosed();

    void onVideoRewardUserRewarded(String requestCode);

    void onInterLoaded();

    void onInterDisplayed();

    /**
     * Called when an interstitial ad fails to display after {@code showAd()} was invoked.
     * Implementations should treat this the same as a closed ad — resume game flow,
     * unlock UI, etc. — so the user is never left stuck waiting.
     *
     * @param code The request code originally passed to {@code ShowInter()}.
     */
    void onInterDisplayFailed(String code);

    void onInterHidden(String code);

    void onAdClicked(String adFormat);

    void onAdRevenuePaid(String adFormat, String adUnitId, String adNetwork, double value);

}

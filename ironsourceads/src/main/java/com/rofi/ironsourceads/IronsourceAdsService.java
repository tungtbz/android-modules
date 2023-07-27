package com.rofi.ironsourceads;

import android.app.Activity;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.InitializationListener;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.rofi.ads.AdsEventListener;
import com.rofi.ads.IAdsService;
import com.rofi.base.Constants;
import com.rofi.base.ThreadUltils;
import com.rofi.remoteconfig.FirebaseRemoteConfigService;

public class IronsourceAdsService implements IAdsService {
    private final String TAG = "AdsService";

    AdsEventListener _adsEventListener;

    private int _currentVideoRewardRequestCode;
    private boolean _isShowingRewardAds;

    private int mCurrentInterRequestCode;
    private boolean _isCoolDownShowInter;
    private boolean isInterAdClicked;
    private boolean mIsShowingAppOpenAd;

    @Override
    public void Init(Activity activity) {
        IronSource.addImpressionDataListener(new ImpressionDataListener() {
            @Override
            public void onImpressionSuccess(ImpressionData impressionData) {
                LogRevenue(impressionData);
            }
        });

        IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
            // Indicates that there's an available ad.
            // The adInfo object includes information about the ad that was loaded successfully
            // Use this callback instead of onRewardedVideoAvailabilityChanged(true)
            @Override
            public void onAdAvailable(AdInfo adInfo) {
                Log.d(TAG, "Reward: onAdAvailable");
                _adsEventListener.onVideoRewardLoaded();
            }

            // Indicates that no ads are available to be displayed
            // Use this callback instead of onRewardedVideoAvailabilityChanged(false)
            @Override
            public void onAdUnavailable() {

            }

            // The Rewarded Video ad view has opened. Your activity will loose focus
            @Override
            public void onAdOpened(AdInfo adInfo) {
                Log.d(TAG, "Reward: onAdOpened");
                _isShowingRewardAds = true;
                _adsEventListener.onVideoRewardDisplayed();
            }

            // The Rewarded Video ad view is about to be closed. Your activity will regain its focus
            @Override
            public void onAdClosed(AdInfo adInfo) {
                Log.d(TAG, "Reward: onAdClosed");

                _isCoolDownShowInter = true;
                int coolDownShowInterInSencond = 10;
                ThreadUltils.startTask(() -> {
                    // doTask
                    _isCoolDownShowInter = false;
                    _isShowingRewardAds = false;
                }, coolDownShowInterInSencond * 1000L);
            }

            // The user completed to watch the video, and should be rewarded.
            // The placement parameter will include the reward data.
            // When using server-to-server callbacks, you may ignore this event and wait for the ironSource server callback
            @Override
            public void onAdRewarded(Placement placement, AdInfo adInfo) {
                Log.d(TAG, "Reward: onAdRewarded");
                _adsEventListener.onVideoRewardUserRewarded(String.valueOf(_currentVideoRewardRequestCode));
            }

            // The rewarded video ad was failed to show
            @Override
            public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {

            }

            // Invoked when the video ad was clicked.
            // This callback is not supported by all networks, and we recommend using it
            // only if it's supported by all networks you included in your build
            @Override
            public void onAdClicked(Placement placement, AdInfo adInfo) {
                Log.d(TAG, "Reward: onAdClicked");
//                Log.d(TAG, "onAdClicked: " + adInfo.);
                _adsEventListener.onAdClicked(adInfo.getAdUnit());
            }
        });

        IronSource.setLevelPlayInterstitialListener(new LevelPlayInterstitialListener() {
            // Invoked when the interstitial ad was loaded successfully.
            // AdInfo parameter includes information about the loaded ad
            @Override
            public void onAdReady(AdInfo adInfo) {
                Log.d(TAG, "Inter: onAdReady");
                _adsEventListener.onInterLoaded();
            }

            // Indicates that the ad failed to be loaded
            @Override
            public void onAdLoadFailed(IronSourceError error) {
            }

            // Invoked when the Interstitial Ad Unit has opened, and user left the application screen.
            // This is the impression indication.
            @Override
            public void onAdOpened(AdInfo adInfo) {
                Log.d(TAG, "Inter: onAdOpened");
                _adsEventListener.onInterDisplayed();
            }

            // Invoked when the interstitial ad closed and the user went back to the application screen.
            @Override
            public void onAdClosed(AdInfo adInfo) {
                IronSource.loadInterstitial();

                if (mIsShowingAppOpenAd) {
                    Log.d(TAG, "Inter: onAdHidden after show open app");
                    mIsShowingAppOpenAd = false;
                    return;
                }

                Log.d(TAG, "Inter: onAdHidden Normal");
                int coolDownShowInterInSencond = FirebaseRemoteConfigService.getInstance().GetInt(Constants.ADS_INTERVAL);
                ThreadUltils.startTask(() -> {
                    // doTask
                    _isCoolDownShowInter = false;
                    mCurrentInterRequestCode = 0;
                    Log.d(TAG, "Inter: onAdHidden Reset Cooldown");
                }, coolDownShowInterInSencond * 1000L);

                _adsEventListener.onInterHidden(String.valueOf(mCurrentInterRequestCode));
            }

            // Invoked when the ad failed to show
            @Override
            public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
            }

            // Invoked when end user clicked on the interstitial ad
            @Override
            public void onAdClicked(AdInfo adInfo) {
                Log.d(TAG, "Inter: onAdClicked");
                _adsEventListener.onAdClicked(adInfo.getAdUnit());
                isInterAdClicked = true;
            }

            // Invoked before the interstitial ad was opened, and before the InterstitialOnAdOpenedEvent is reported.
            // This callback is not supported by all networks, and we recommend using it only if
            // it's supported by all networks you included in your build.
            @Override
            public void onAdShowSucceeded(AdInfo adInfo) {
            }
        });

        if (BuildConfig.DEBUG) {
            IronSource.setMetaData("is_test_suite", "enable");
        }
        IronSource.setConsent(true);
        IronSource.setMetaData("do_not_sell", "false");
        IronSource.setMetaData("is_child_directed", "false");

        String appKey = activity.getResources().getString(R.string.ironsource_app_key);
        IronSource.init(activity, appKey, new InitializationListener() {
                    @Override
                    public void onInitializationComplete() {
                        if (BuildConfig.DEBUG)
                            IronSource.launchTestSuite(activity);
                        if (BuildConfig.DEBUG)
                            IntegrationHelper.validateIntegration(activity);

                        Log.d(TAG, "onInitializationComplete: ");
                        IronSource.loadInterstitial();
                    }
                },
                IronSource.AD_UNIT.INTERSTITIAL,
                IronSource.AD_UNIT.REWARDED_VIDEO,
                IronSource.AD_UNIT.BANNER);
    }

    @Override
    public boolean IsRewardReady() {
        return false;
    }

    @Override
    public boolean IsInterReady() {
        return false;
    }

    @Override
    public void ShowReward(int requestCode) {

    }

    @Override
    public void ShowInter(int requestCode) {

    }

    @Override
    public void ShowBanner(Activity activity) {

    }

    @Override
    public void HideBanner() {

    }

    @Override
    public void ShowMREC(Activity activity) {

    }

    @Override
    public void HideMREC() {

    }

    @Override
    public void OnPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void SetEventListener(AdsEventListener listener) {
        _adsEventListener = listener;
    }

    private void LogRevenue(ImpressionData impressionData) {
        double revenue = impressionData.getRevenue();
        String adFormatStr = impressionData.getAdUnit();
        String networkName = impressionData.getAdNetwork();
        String adUnitId = impressionData.getInstanceName();

        _adsEventListener.onAdRevenuePaid(adFormatStr, adUnitId, networkName, revenue);
    }
}

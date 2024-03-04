package com.rofi.ironsourceads;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.InitializationListener;
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.rofi.ads.AdsEventListener;
import com.rofi.ads.IAdsService;
import com.rofi.base.Constants;
import com.rofi.base.ThreadUltils;
import com.rofi.remoteconfig.FirebaseRemoteConfigService;

import java.util.Timer;
import java.util.TimerTask;

public class IronsourceAdsService implements IAdsService {
    private final String TAG = "IronsourceAdsService";

    AdsEventListener _adsEventListener;

    private int mCurrentVideoRewardRequestCode;
    private boolean _isShowingRewardAds;

    private int mCurrentInterRequestCode;
    private boolean isCoolDownShowInter;
    private boolean isAdClicked;
    private boolean mIsShowingAppOpenAd;

    private FrameLayout mBannerContainer;
    private IronSourceBannerLayout mIronSourceBannerLayout;

    private FrameLayout mRECParentContainer;
    private IronSourceBannerLayout mIronSourceRECBannerLayout;
    private String _appKey;

    private int blockAutoShowInterCount;
    private int _bannerPosition;
    private int _mrecPosition;

    @Override
    public void Init(Activity activity, String[] args) {
        if (args == null || args.length == 0) {
            Log.e(TAG, "args is empty!");
            return;
        }
        //set keys
        _appKey = args[0];
        _bannerPosition = Integer.parseInt(args[6]);
        _mrecPosition = Integer.parseInt(args[7]);

        setISListener();

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

        IronSource.shouldTrackNetworkState(activity.getApplicationContext(), true);
    }

    private void setISListener() {
        //set listeners
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
                isFullscreenAdsShowing = true;

                _adsEventListener.onVideoRewardDisplayed();
                IncreaseBlockAutoShowInter();
            }

            // The Rewarded Video ad view is about to be closed. Your activity will regain its focus
            @Override
            public void onAdClosed(AdInfo adInfo) {
                Log.d(TAG, "Reward: onAdClosed");

                isFullscreenAdsShowing = false;

                if (!isCoolDownShowInter) {
                    isCoolDownShowInter = true;
                    int coolDownShowInterInSencond = 5;
                    ThreadUltils.startTask(() -> {
                        // doTask
                        isCoolDownShowInter = false;
                        _isShowingRewardAds = false;
                    }, coolDownShowInterInSencond * 1000L);
                }
            }

            // The user completed to watch the video, and should be rewarded.
            // The placement parameter will include the reward data.
            // When using server-to-server callbacks, you may ignore this event and wait for the ironSource server callback
            @Override
            public void onAdRewarded(Placement placement, AdInfo adInfo) {
                Log.d(TAG, "Reward: onAdRewarded");
                _adsEventListener.onVideoRewardUserRewarded(String.valueOf(mCurrentVideoRewardRequestCode));
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
                isAdClicked = true;
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
                isFullscreenAdsShowing = true;
                _adsEventListener.onInterDisplayed();
            }

            // Invoked when the interstitial ad closed and the user went back to the application screen.
            @Override
            public void onAdClosed(AdInfo adInfo) {
                IronSource.loadInterstitial();
                isFullscreenAdsShowing = false;

                if (mIsShowingAppOpenAd) {
                    Log.d(TAG, "Inter: onAdHidden after show open app");
                    mIsShowingAppOpenAd = false;
                    return;
                }

                Log.d(TAG, "Inter: onAdHidden Normal");
                coolDownShowInterInSecond = FirebaseRemoteConfigService.getInstance().GetInt(Constants.ADS_INTERVAL);
                RunCountDownToShowInter();

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
                isAdClicked = true;
            }

            // Invoked before the interstitial ad was opened, and before the InterstitialOnAdOpenedEvent is reported.
            // This callback is not supported by all networks, and we recommend using it only if
            // it's supported by all networks you included in your build.
            @Override
            public void onAdShowSucceeded(AdInfo adInfo) {
            }
        });
    }

    @Override
    public boolean IsRewardReady() {
        return IronSource.isRewardedVideoAvailable();
    }

    @Override
    public boolean IsInterReady() {
        return IronSource.isInterstitialReady();
    }

    @Override
    public void ShowReward(int requestCode) {
        if (IsRewardReady()) {
            mCurrentVideoRewardRequestCode = requestCode;
            IronSource.showRewardedVideo();
        } else {
            Log.e(TAG, "ShowVideo Applovin: FAILEDDDDDDDDDDDDD");
        }
    }

    @Override
    public void ShowInter(int requestCode) {
        if (_isDisableInterAds) {
            Log.e(TAG, "Failed To Show Inter: _isDisableInterAds");
            return;
        }

        //force show inter ads
        if (requestCode == 1) {
            if (IsInterReady()) {
                Log.d(TAG, "ShowInter when resume");
                IronSource.showInterstitial();
            } else {
                mIsShowingAppOpenAd = false;
            }
            return;
        }

        //show normal
        if (isCoolDownShowInter) {
            Log.e(TAG, "Failed To Show Inter: isCoolDownShowInter");
            return;
        }

        if (IsInterReady()) {
            //reset flags
            isAdClicked = false;
            isCoolDownShowInter = true;

            mCurrentInterRequestCode = requestCode;
            Log.d(TAG, "ShowInter: normal");
            IronSource.showInterstitial();

        } else {
            Log.e(TAG, "ShowInter_Applovin: FAILEDDDDDDDDDDDDD");
        }
    }

    private Timer timer;
    int coolDownShowInterInSecond;
    boolean isFullscreenAdsShowing;

    private void RunCountDownToShowInter() {
        isCoolDownShowInter = true;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                // do your work
                if (coolDownShowInterInSecond <= 0) {
                    timer.cancel();

                    isCoolDownShowInter = false;
                    mCurrentInterRequestCode = 0;
                    Log.d(TAG, "RunCountDownToShowInter: Reset Cooldown");
                    return;
                }

                if (isFullscreenAdsShowing) {
                    Log.d(TAG, "RunCountDownToShowInter  isShowingFullscreenAds --> skip");
                    return;
                }

                Log.d(TAG, "RunCountDownToShowInter ");
                coolDownShowInterInSecond -= 1;

            }
        }, 0, 1000);
    }

    @Override
    public void ShowBanner(Activity activity) {
        LoadNormalBanner(activity);
    }

    @Override
    public void HideBanner() {
        Log.d(TAG, "StopBottomBanner");
        if (mBannerContainer != null && mIronSourceBannerLayout != null) {
            mBannerContainer.setVisibility(View.GONE);
            IronSource.destroyBanner(mIronSourceBannerLayout);
            mBannerContainer.removeAllViews();

            mIronSourceBannerLayout = null;
        }
    }

    @Override
    public void ShowMREC(Activity activity) {
        Log.d(TAG, "ShowMREC ~~~~~~~~");
        CreateAndLoadRectBanner(activity);
    }

    @Override
    public void HideMREC() {
        Log.d(TAG, "HideMREC");
        if (mRECParentContainer != null && mIronSourceRECBannerLayout != null) {
            mRECParentContainer.setVisibility(View.GONE);
            IronSource.destroyBanner(mIronSourceRECBannerLayout);
            mRECParentContainer.removeAllViews();
            mIronSourceRECBannerLayout = null;
        }
    }

    @Override
    public void ShowNativeMREC(Activity activity) {

    }

    @Override
    public void HideNativeMREC() {

    }

    @Override
    public void ShowNativeBanner(Activity activity) {

    }

    @Override
    public void HideNativeBanner() {

    }

    @Override
    public void OnPause(Activity activity) {
        IronSource.onPause(activity);
    }

    @Override
    public void onResume(Activity activity) {
        IronSource.onResume(activity);

        Log.d(TAG, "onResume blockAutoShowInterCount: " + blockAutoShowInterCount);


        showResumeApp();
    }

    @Override
    public void SetEventListener(AdsEventListener listener) {
        _adsEventListener = listener;
    }

    @Override
    public void IncreaseBlockAutoShowInter() {
        Log.d(TAG, "IncreaseBlockAutoShowInter ");
        blockAutoShowInterCount += 1;
    }

    @Override
    public void DecreaseBlockAutoShowInter() {
        Log.d(TAG, "DecreaseBlockAutoShowInter ");
        blockAutoShowInterCount -= 1;
        if (blockAutoShowInterCount < 0) blockAutoShowInterCount = 0;
    }

    @Override
    public void LoadOpenAppAds(Activity activity) {

    }

    @Override
    public void ShowOpenAppAds(Activity activity) {

    }

    @Override
    public boolean IsOpenAppAdsAvailable() {
        return false;
    }

    private boolean _isDisableResumeAds;

    @Override
    public void DisableResumeAds() {
        Log.d(TAG, "DisableResumeAds");
        _isDisableResumeAds = true;
    }

    @Override
    public void EnableResumeAds() {
        Log.d(TAG, "EnableResumeAds");
        _isDisableResumeAds = false;
    }

    private void LoadNormalBanner(Activity activity) {

        if (mBannerContainer == null) {
            //create banner's container
            // Stretch to the width of the screen for banners to be fully functional
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            // wrap banner height
            // int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = activity.getResources().getDimensionPixelSize(R.dimen.banner_height);
            int gravity = _bannerPosition == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            mBannerContainer = new FrameLayout(activity.getApplicationContext());
            mBannerContainer.setLayoutParams(new FrameLayout.LayoutParams(width, height, gravity));
            mBannerContainer.setVisibility(View.GONE);

            ViewGroup rootView = activity.findViewById(android.R.id.content);
            rootView.addView(mBannerContainer);
        }

        ISBannerSize size = ISBannerSize.SMART;
        size.setAdaptive(true);
        mIronSourceBannerLayout = IronSource.createBanner(activity, size);

        if (mIronSourceBannerLayout != null) {
            LevelPlayBannerListener levelPlayBannerListener = new LevelPlayBannerListener() {
                @Override
                public void onAdLoaded(AdInfo adInfo) {
                    Log.d(TAG, "onBannerAdLoaded");
                    // since banner container was "gone" by default, we need to make it visible as soon as the banner is ready
                    mBannerContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdLoadFailed(IronSourceError ironSourceError) {
                    Log.d(TAG, "onBannerAdLoadFailed" + " " + ironSourceError);
                }

                @Override
                public void onAdClicked(AdInfo adInfo) {
                    Log.d(TAG, "onBannerAdClicked");
                    _adsEventListener.onAdClicked("BANNER");
                    isAdClicked = true;
                }

                @Override
                public void onAdLeftApplication(AdInfo adInfo) {

                }

                @Override
                public void onAdScreenPresented(AdInfo adInfo) {

                }

                @Override
                public void onAdScreenDismissed(AdInfo adInfo) {

                }
            };

            // set the banner listener
            mIronSourceBannerLayout.setLevelPlayBannerListener(levelPlayBannerListener);

            // add IronSourceBanner to your container
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            mBannerContainer.addView(mIronSourceBannerLayout, 0, layoutParams);

            // Load the ad
            IronSource.loadBanner(mIronSourceBannerLayout);
            Log.d(TAG, "Start Load Banner");
        }
    }

    private void CreateAndLoadRectBanner(Activity activity) {
        LevelPlayBannerListener levelPlayBannerListener = new LevelPlayBannerListener() {
            @Override
            public void onAdLoaded(AdInfo adInfo) {
                Log.d(TAG, "on MREC AdLoaded");
                mRECParentContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdLoadFailed(IronSourceError ironSourceError) {

            }

            @Override
            public void onAdClicked(AdInfo adInfo) {

                _adsEventListener.onAdClicked("MREC");
                isAdClicked = true;
            }

            @Override
            public void onAdLeftApplication(AdInfo adInfo) {

            }

            @Override
            public void onAdScreenPresented(AdInfo adInfo) {

            }

            @Override
            public void onAdScreenDismissed(AdInfo adInfo) {

            }
        };

        if (mRECParentContainer == null) {
            mRECParentContainer = new FrameLayout(activity.getApplicationContext());
            int gravity = _mrecPosition == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, gravity);
            layoutParams.setMargins(0, 0, 0, 0);
            mRECParentContainer.setLayoutParams(layoutParams);
            mRECParentContainer.setVisibility(View.GONE);
            ViewGroup rootView = activity.findViewById(android.R.id.content);
            rootView.addView(mRECParentContainer);
        }

        mIronSourceRECBannerLayout = IronSource.createBanner(activity, ISBannerSize.RECTANGLE);
        mIronSourceRECBannerLayout.setLevelPlayBannerListener(levelPlayBannerListener);

        // add IronSourceBanner to your container
        mRECParentContainer.addView(mIronSourceRECBannerLayout, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        Log.d(TAG, "CreateAndLoadRectBanner: LoadBanner");
        IronSource.loadBanner(mIronSourceRECBannerLayout);

    }

    private void showResumeApp() {
        if (blockAutoShowInterCount > 0) {
            DecreaseBlockAutoShowInter();
            return;
        }
        if (_isDisableResumeAds) {
            Log.e(TAG, "showOpenAppAdIfReady: _isDisableResumeAds");
            return;
        }

        boolean showOpenAppAds = FirebaseRemoteConfigService.getInstance().GetBoolean(Constants.RESUME_ADS_KEY);
        if (!showOpenAppAds) {
            Log.e(TAG, "showOpenAppAdIfReady: RESUME_ADS_KEY = FALSE");
            return;
        }
        //resume from ads
        if (isAdClicked) {
            isAdClicked = false;
            Log.e(TAG, "showOpenAppAdIfReady: isInterAdClicked");
            return;
        }

        if (_isShowingRewardAds) {
            Log.e(TAG, "showOpenAppAdIfReady: _isShowingRewardAds");
            return;
        }

        mIsShowingAppOpenAd = true;

        ShowInter(1);
    }

    private void LogRevenue(ImpressionData impressionData) {
        double revenue = impressionData.getRevenue();
        String adFormatStr = impressionData.getAdUnit();
        String networkName = impressionData.getAdNetwork();
        String adUnitId = impressionData.getInstanceName();

        _adsEventListener.onAdRevenuePaid(adFormatStr, adUnitId, networkName, revenue);
    }

    private boolean _isDisableInterAds;

    @Override
    public void DisableInterAds() {
        _isDisableInterAds = true;
    }

    @Override
    public void EnableInterAds() {
        _isDisableInterAds = false;
    }
}

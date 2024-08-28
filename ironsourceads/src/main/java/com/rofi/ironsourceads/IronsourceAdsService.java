package com.rofi.ironsourceads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.ironsource.adqualitysdk.sdk.ISAdQualityConfig;
import com.ironsource.adqualitysdk.sdk.ISAdQualityInitError;
import com.ironsource.adqualitysdk.sdk.ISAdQualityInitListener;
import com.ironsource.adqualitysdk.sdk.ISAdQualityLogLevel;
import com.ironsource.adqualitysdk.sdk.IronSourceAdQuality;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.ISContainerParams;
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
import com.unity3d.player.UnityPlayer;


import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class IronsourceAdsService implements IAdsService {
    private final String TAG = "IronsourceAdsService";
    AdsEventListener _adsEventListener;
    private Handler mUIHandler;

    private int mCurrentVideoRewardRequestCode;
    private boolean _isShowingRewardAds;

    private int mCurrentInterRequestCode;
    private boolean isCoolDownShowInter;
    private boolean isAdClicked;
    private boolean mIsShowingResumeAds;

    private FrameLayout mBannerContainer;
    private IronSourceBannerLayout mBanner;
    //    private IronSourceBannerLayout mBanner;
    private int mBannerVisibilityState;

    private FrameLayout mRECParentContainer;
    private IronSourceBannerLayout mIronSourceRECBannerLayout;
    private String _appKey;

    private int blockAutoShowInterCount;
    private int _bannerPosition;
    private int _mrecPosition;
    private boolean _useMRECAdmob;
    private boolean _bannerLoaded;
    private boolean _needShowBanner;

    public IronsourceAdsService() {
        this.mUIHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void Init(Activity activity, String[] args) {
        if (args == null || args.length == 0) {
            Log.e(TAG, "args is empty!");
            return;
        }

        //set keys
        _appKey = args[0];
        _useMRECAdmob = Objects.equals(args[1], "mrec_admob");

        _bannerPosition = Integer.parseInt(args[6]);
        _mrecPosition = Integer.parseInt(args[7]);

        setISListener();

        if (BuildConfig.DEBUG) {
            IronSource.setMetaData("is_test_suite", "enable");
        }
        IronSource.setConsent(true);
        IronSource.setMetaData("do_not_sell", "false");
        IronSource.setMetaData("is_child_directed", "false");
        _needShowBanner = false;

        ISAdQualityConfig.Builder adQualityConfigBuilder = new ISAdQualityConfig.Builder().setAdQualityInitListener(new ISAdQualityInitListener() {
            @Override
            public void adQualitySdkInitSuccess() {
                Log.d(TAG, "adQualitySdkInitSuccess");

                IronSource.init(activity, _appKey, new InitializationListener() {
                    @Override
                    public void onInitializationComplete() {
                        if (BuildConfig.DEBUG) IronSource.launchTestSuite(activity);
                        if (BuildConfig.DEBUG) IntegrationHelper.validateIntegration(activity);

                        Log.d(TAG, "onInitializationComplete");

                        IronSource.loadInterstitial();

                        if (_useMRECAdmob) PreloadBanner(activity);
                    }
                }, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.REWARDED_VIDEO, IronSource.AD_UNIT.BANNER);
            }

            @Override
            public void adQualitySdkInitFailed(ISAdQualityInitError error, String message) {
                Log.d(TAG, "adQualitySdkInitFailed " + error + " message: " + message);
            }
        });
        adQualityConfigBuilder.setTestMode(BuildConfig.DEBUG);

        if (BuildConfig.DEBUG) adQualityConfigBuilder.setLogLevel(ISAdQualityLogLevel.VERBOSE);
        ISAdQualityConfig adQualityConfig = adQualityConfigBuilder.build();

        // Initialize ad quality
        IronSourceAdQuality.getInstance().initialize(activity.getApplicationContext(), _appKey, adQualityConfig);

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

                if (mIsShowingResumeAds) {
                    Log.d(TAG, "Inter: onAdHidden after show open app");
                    mIsShowingResumeAds = false;
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
                mIsShowingResumeAds = false;
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

    public void PreloadBanner(Activity activity) {
        IronsourceAdsService.this.mBannerVisibilityState = View.INVISIBLE;
        LoadNormalBanner(activity);
    }

    @Override
    public void ShowBanner(Activity activity) {
        _needShowBanner = true;
        if (_useMRECAdmob) {
            Log.d(TAG, "SHOW IRONSOURCE Banner");
            //Show IS Banner
            if (mBannerContainer != null && mBannerContainer.getVisibility() != View.VISIBLE && mBanner != null) {
                mBannerContainer.setVisibility(View.VISIBLE);
            }
        } else {
            Log.d(TAG, "RELOAD Banner");
            //Load IS Banner
            IronsourceAdsService.this.mBannerVisibilityState = View.VISIBLE;
            LoadNormalBanner(activity);
        }
    }

    @Override
    public void HideBanner() {
        _needShowBanner = false;
        if (_useMRECAdmob) {
            Log.d(TAG, "Hide IRONSOURCE Banner to SHOW Admob MREC");
            if (mBannerContainer != null && mBannerContainer.getVisibility() != View.GONE && mBanner != null) {
                mBannerContainer.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "DESTROY Banner to show MREC");
            if (mBannerContainer != null && mBanner != null) {
                mBannerContainer.setVisibility(View.GONE);

                IronSource.destroyBanner(mBanner);
                mBannerContainer.removeAllViews();

                mBanner = null;
            }
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
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onResume blockAutoShowInterCount: " + blockAutoShowInterCount);

                IronSource.onResume(activity);

                showResumeApp();
            }
        });

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

    private ISBannerSize getBannerSize(String description, int width, int height) {
        if (description.equals("CUSTOM")) return new ISBannerSize(width, height);
        if (description.equals("SMART")) return ISBannerSize.SMART;
        if (description.equals("RECTANGLE")) return ISBannerSize.RECTANGLE;
        if (description.equals("LARGE")) return ISBannerSize.LARGE;
        return ISBannerSize.BANNER;
    }

    public float getMaximalAdaptiveHeight(float width) {
        int widthInt = (int) width;
        return ISBannerSize.getMaximalAdaptiveHeight(widthInt);
    }

    public float getDeviceScreenWidth() {
        Activity activity = getUnityActivity();
        if (activity != null) {
            WindowManager windowManager = activity.getWindowManager();
            if (windowManager != null) {
                Display display = windowManager.getDefaultDisplay();
                if (display != null) {
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    display.getMetrics(displayMetrics);
                    int widthPixels = displayMetrics.widthPixels;
                    float density = displayMetrics.density;
                    return widthPixels / density;
                }
            }
        }
        return 0.0F;
    }

    public Activity getUnityActivity() {
        return UnityPlayer.currentActivity;
    }

    private void LoadNormalBanner(Activity activity) {
        this.mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (IronsourceAdsService.this) {
                    try {
                        //create container
                        if (IronsourceAdsService.this.mBannerContainer == null) {
                            IronsourceAdsService.this.mBannerContainer = new FrameLayout((Context) UnityPlayer.currentActivity);
                            IronsourceAdsService.this.mBannerContainer.setBackgroundColor(Color.TRANSPARENT);
                            IronsourceAdsService.this.mBannerContainer.setVisibility(IronsourceAdsService.this.mBannerVisibilityState);

                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2);
                            params.gravity = _bannerPosition == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                            UnityPlayer.currentActivity.addContentView((View) IronsourceAdsService.this.mBannerContainer, (ViewGroup.LayoutParams) params);
                        }
                        //create banner size
                        ISBannerSize size = ISBannerSize.BANNER;
                        size.setAdaptive(true);
                        float widthx = IronsourceAdsService.this.getDeviceScreenWidth();
                        float heightx = IronsourceAdsService.this.getMaximalAdaptiveHeight(widthx);
                        ISContainerParams isContainerParams = new ISContainerParams((int) widthx, (int) heightx);
                        size.setContainerParams(isContainerParams);

//                        if (Build.VERSION.SDK_INT >= 28) {
//                            IronsourceAdsService.this.mBannerContainer.setFitsSystemWindows(true);
//                            IronsourceAdsService.this.mBannerContainer.setSystemUiVisibility(1280);
//                        }

                        IronsourceAdsService.this.mBanner = IronSource.createBanner(IronsourceAdsService.this.getUnityActivity(), size);
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.gravity = _bannerPosition == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                        IronsourceAdsService.this.mBannerContainer.addView((View) IronsourceAdsService.this.mBanner, (ViewGroup.LayoutParams) layoutParams);

                        if (mBanner != null) {
                            LevelPlayBannerListener levelPlayBannerListener = new LevelPlayBannerListener() {
                                @Override
                                public void onAdLoaded(AdInfo adInfo) {
                                    Log.d(TAG, "onBannerAdLoaded");
                                    _bannerLoaded = true;
                                    // since banner container was "gone" by default, we need to make it visible as soon as the banner is ready
                                    if (_needShowBanner && mBannerContainer.getVisibility() == View.GONE)
                                        mBannerContainer.setVisibility(View.VISIBLE);
                                    if (!_needShowBanner && mBannerContainer.getVisibility() == View.VISIBLE)
                                        mBannerContainer.setVisibility(View.GONE);
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
                            mBanner.setLevelPlayBannerListener(levelPlayBannerListener);

                            // Load the ad
                            IronSource.loadBanner(IronsourceAdsService.this.mBanner);
                            Log.d(TAG, "Start Load Banner");
                        }

                    } catch (Exception e) {
                        Log.d(TAG, "Load  failed: " + e.getMessage());
                    }
                }
            }
        });
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
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, gravity);
            layoutParams.setMargins(0, 0, 0, 0);
            mRECParentContainer.setLayoutParams(layoutParams);
            mRECParentContainer.setVisibility(View.GONE);
            ViewGroup rootView = activity.findViewById(android.R.id.content);
            rootView.addView(mRECParentContainer);
        }

        mIronSourceRECBannerLayout = IronSource.createBanner(activity, ISBannerSize.RECTANGLE);
        mIronSourceRECBannerLayout.setLevelPlayBannerListener(levelPlayBannerListener);

        // add IronSourceBanner to your container
        mRECParentContainer.addView(mIronSourceRECBannerLayout, 0, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

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

        if (mIsShowingResumeAds) {
            Log.e(TAG, "mIsShowingResumeAds");
            return;
        }

        mIsShowingResumeAds = true;

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

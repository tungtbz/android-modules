package com.rofi.maxads;

import static com.rofi.base.Constants.RESUME_INTER_ADS;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdResponse;
import com.applovin.impl.sdk.utils.JsonUtils;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdReviewListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.rofi.ads.AdsEventListener;
import com.rofi.ads.IAdsService;
import com.rofi.base.Constants;
import com.rofi.base.ThreadUltils;
import com.rofi.remoteconfig.FirebaseRemoteConfigService;
import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MaxAdsService implements IAdsService,
        MaxAdViewAdListener,
        MaxRewardedAdListener,
        MaxAdRevenueListener,
        MaxAdReviewListener {

    @Override
    public void onAdExpanded(@NonNull MaxAd maxAd) {
        MaxAdsService.d("onAdExpanded");
        String name;
        MaxAdFormat adFormat = maxAd.getFormat();
        if (!adFormat.isAdViewAd()) {
            MaxAdsService.d("onAdExpanded " + adFormat);
            return;
        }
        if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdExpandedEvent";
        } else {
            name = "OnBannerAdExpandedEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, maxAd);
        forwardUnityEvent(args);
    }

    @Override
    public void onAdCollapsed(@NonNull MaxAd maxAd) {
        MaxAdsService.d("onAdCollapsed");
        String name;
        MaxAdFormat adFormat = maxAd.getFormat();
        if (!adFormat.isAdViewAd()) {
            MaxAdsService.d("onAdCollapsed " + adFormat);
            return;
        }
        if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdCollapsedEvent";
        } else {
            name = "OnBannerAdCollapsedEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, maxAd);
        forwardUnityEvent(args);
    }

    @Override
    public void onAdLoaded(@NonNull MaxAd maxAd) {
        String name;
        MaxAdFormat adFormat = maxAd.getFormat();
        if (adFormat.isAdViewAd()) {
            if (MaxAdFormat.MREC == adFormat) {
                name = "OnMRecAdLoadedEvent";
            } else {
                name = "OnBannerAdLoadedEvent";
            }
            positionAdView(maxAd);
            MaxAdView adView = retrieveAdView(maxAd.getAdUnitId(), adFormat);
            if (adView != null && adView.getVisibility() != View.VISIBLE)
                adView.stopAutoRefresh();
        } else if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialLoadedEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdLoadedEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdLoadedEvent";
        } else if (MaxAdFormat.REWARDED_INTERSTITIAL == adFormat) {
            name = "OnRewardedInterstitialAdLoadedEvent";
        } else {
//            logInvalidAdFormat(adFormat);
            return;
        }
        synchronized (this.mAdInfoMapLock) {
            this.mAdInfoMap.put(maxAd.getAdUnitId(), maxAd);
        }

        JSONObject args = getDefaultAdEventParameters(name, maxAd);
        forwardUnityEvent(args);
    }

    @Override
    public void onAdDisplayed(@NonNull MaxAd maxAd) {
        String name;
        MaxAdFormat adFormat = maxAd.getFormat();
        if (!adFormat.isFullscreenAd())
            return;
        if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialDisplayedEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdDisplayedEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdDisplayedEvent";
        } else {
            name = "OnRewardedInterstitialAdDisplayedEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, maxAd);
        forwardUnityEvent(args);
    }

    @Override
    public void onAdHidden(@NonNull MaxAd maxAd) {
        String name;
        MaxAdFormat adFormat = maxAd.getFormat();
        if (!adFormat.isFullscreenAd())
            return;
        if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialHiddenEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdHiddenEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdHiddenEvent";
        } else {
            name = "OnRewardedInterstitialAdHiddenEvent";
        }
        JSONObject args = getDefaultAdEventParameters(name, maxAd);
        forwardUnityEvent(args);
    }

    @Override
    public void onAdClicked(@NonNull MaxAd maxAd) {
        String name;
        MaxAdFormat adFormat = maxAd.getFormat();
        if (MaxAdFormat.BANNER == adFormat || MaxAdFormat.LEADER == adFormat) {
            name = "OnBannerAdClickedEvent";
        } else if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdClickedEvent";
        } else if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialClickedEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdClickedEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdClickedEvent";
        } else if (MaxAdFormat.REWARDED_INTERSTITIAL == adFormat) {
            name = "OnRewardedInterstitialAdClickedEvent";
        } else {
//            logInvalidAdFormat(adFormat);
            return;
        }
        JSONObject args = getDefaultAdEventParameters(name, maxAd);
        forwardUnityEvent(args);
    }

    @Override
    public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError maxError) {
        String name;
        if (TextUtils.isEmpty(adUnitId)) {
//            logStackTrace(new IllegalArgumentException("adUnitId cannot be null"));
            e("adUnitId cannot be null");
            return;
        }
        if (this.mAdViews.containsKey(adUnitId)) {
            MaxAdFormat adViewAdFormat = this.mAdViewAdFormats.get(adUnitId);
            if (MaxAdFormat.MREC == adViewAdFormat) {
                name = "OnMRecAdLoadFailedEvent";
            } else {
                name = "OnBannerAdLoadFailedEvent";
            }
        } else if (this.mInterstitials.containsKey(adUnitId)) {
            name = "OnInterstitialLoadFailedEvent";
        } else if (this.mAppOpenAds.containsKey(adUnitId)) {
            name = "OnAppOpenAdLoadFailedEvent";
        } else if (this.mRewardedAds.containsKey(adUnitId)) {
            name = "OnRewardedAdLoadFailedEvent";
        }
//        else if (this.mRewardedInterstitialAds.containsKey(adUnitId)) {
//            name = "OnRewardedInterstitialAdLoadFailedEvent";
//        }
        else {
//            logStackTrace(new IllegalStateException("invalid adUnitId: " + adUnitId));
            return;
        }
        synchronized (this.mAdInfoMapLock) {
            this.mAdInfoMap.remove(adUnitId);
        }
        JSONObject args = new JSONObject();
        JsonUtils.putString(args, "name", name);
        JsonUtils.putString(args, "adUnitId", adUnitId);
        JsonUtils.putString(args, "errorCode", Integer.toString(maxError.getCode()));
        JsonUtils.putString(args, "errorMessage", maxError.getMessage());
        String adLoadFailureInfo = maxError.getAdLoadFailureInfo();
        JsonUtils.putString(args, "adLoadFailureInfo", !TextUtils.isEmpty(adLoadFailureInfo) ? adLoadFailureInfo : "");
        JsonUtils.putString(args, "latencyMillis", String.valueOf(maxError.getRequestLatencyMillis()));
        forwardUnityEvent(args);
    }

    @Override
    public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {

    }

    @Override
    public void onAdRevenuePaid(@NonNull MaxAd maxAd) {
        String name;
        MaxAdFormat adFormat = maxAd.getFormat();
        if (MaxAdFormat.BANNER == adFormat || MaxAdFormat.LEADER == adFormat) {
            name = "OnBannerAdRevenuePaidEvent";
        } else if (MaxAdFormat.MREC == adFormat) {
            name = "OnMRecAdRevenuePaidEvent";
        } else if (MaxAdFormat.INTERSTITIAL == adFormat) {
            name = "OnInterstitialAdRevenuePaidEvent";
        } else if (MaxAdFormat.APP_OPEN == adFormat) {
            name = "OnAppOpenAdRevenuePaidEvent";
        } else if (MaxAdFormat.REWARDED == adFormat) {
            name = "OnRewardedAdRevenuePaidEvent";
        } else if (MaxAdFormat.REWARDED_INTERSTITIAL == adFormat) {
            name = "OnRewardedInterstitialAdRevenuePaidEvent";
        } else {
//            logInvalidAdFormat(adFormat);
            return;
        }

        JSONObject args = getDefaultAdEventParameters(name, maxAd);
        forwardUnityEvent(args, adFormat.isFullscreenAd());
    }

    @Override
    public void onCreativeIdGenerated(@NonNull String s, @NonNull MaxAd maxAd) {

    }

    @Override
    public void onUserRewarded(MaxAd ad, MaxReward reward) {
        MaxAdFormat adFormat = ad.getFormat();
        if (adFormat != MaxAdFormat.REWARDED && adFormat != MaxAdFormat.REWARDED_INTERSTITIAL) {
//            logInvalidAdFormat(adFormat);
            return;
        }
        String rewardLabel = (reward != null) ? reward.getLabel() : "";
        int rewardAmountInt = (reward != null) ? reward.getAmount() : 0;
        String rewardAmount = Integer.toString(rewardAmountInt);
        String name = (adFormat == MaxAdFormat.REWARDED) ? "OnRewardedAdReceivedRewardEvent" : "OnRewardedInterstitialAdReceivedRewardEvent";
        JSONObject args = getDefaultAdEventParameters(name, ad);
        JsonUtils.putString(args, "rewardLabel", rewardLabel);
        JsonUtils.putString(args, "rewardAmount", rewardAmount);
        forwardUnityEvent(args);
    }

    @Override
    public void onRewardedVideoStarted(@NonNull MaxAd maxAd) {
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull MaxAd maxAd) {
    }

    protected static class Insets {
        int left;

        int top;

        int right;

        int bottom;
    }

    private final String TAG = "MaxAdsService";
    private static final ScheduledThreadPoolExecutor sThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(3, new SdkThreadFactory());

    private MaxInterstitialAd mInterstitialAd;
    private int mRetryAttemptInterstitialAds;

    private MaxRewardedAd mRewardedAd;
    private int mRetryAttemptRewardAds;

    private MaxAdView bannerAdView;
    private MaxAdView rectAdView;

    private int mCurrentVideoRewardRequestCode;
    private int mCurrentInterRequestCode;
    //    private boolean mIsShowingAppOpenAd;
    //0 not load
    //1 call load ad
    //2 ad loaded
    private int mRectBannerState;
    //1 hide afterloaded
    //2 show afterloaded
    private int mRectShowFlag;

    private boolean isCoolDownShowInter;
    private boolean isClickToAds;

    private int mRetryAttemptNativeAds;

    private int mRetryAttemptNativeBannerAds;
    //native ads
    private FrameLayout mNativeRectAdsContainer;
    private FrameLayout mNativeBannerAdsContainer;

    private MaxNativeAdLoader nativeRectAdLoader;
    private MaxNativeAdLoader nativeBannerAdLoader;
    private MaxAd nativeRectAd, nativeBannerAd;

    private String _bannerAdId;
    private String _interAdId;
    private String _rewardAdId;
    private String _mrecAdId;
    private String _nativeRectAdId;
    private String _nativeSmallAdId;
    private String _appOpenAdId;

    private boolean _apsEnable;
    private String _apsAppId;
    private String _apsBannerId;
    private String _apsMRECId;
    private String _apsInterId;
    private String _apsVideoRewardId;

    private int _bannerPosition;
    private int _mrecPosition;

    private Activity _activity;
    private int blockAutoShowInterCount;

    private MaxAppOpenAd appOpenAd;
    private long _finishInterAdsTime = 0;
    int coolDownShowInterInSecond;
    boolean isFullscreenAdsShowing;
    boolean isMRECLoaded;
    boolean isMRECLoading;
    private Timer timer;
    private AmazonAdsService _maxAmazonAdsService;
    private final Map<String, MaxAdView> mAdViews;
    private final Map<String, MaxInterstitialAd> mInterstitials;
    private final Map<String, MaxAppOpenAd> mAppOpenAds;
    private final Map<String, MaxRewardedAd> mRewardedAds;

    private final Map<String, MaxAdFormat> mAdViewAdFormats;
    private final Map<String, String> mAdViewPositions;
    private final Map<String, Point> mAdViewOffsets;
    private final Set<String> mDisabledAdaptiveBannerAdUnitIds;
    private final Map<String, Integer> mAdViewWidths;
    private final List<String> mAdUnitIdsToShowAfterCreate;
    private final Set<String> mDisabledAutoRefreshAdViewAdUnitIds;

    private final Map<String, MaxAd> mAdInfoMap;

    private final Object mAdInfoMapLock;

    private final Map<String, Map<String, String>> mAdViewExtraParametersToSetAfterCreate;
    private AppLovinSdk sdk;
    private Integer mPublisherBannerBackgroundColor = null;
    private View mSafeAreaBackground;
    private static final Point DEFAULT_AD_VIEW_OFFSET = new Point(0, 0);
    private static BackgroundCallback backgroundCallback;

    public MaxAdsService() {
        this.mAdViews = new HashMap<>(2);
        this.mInterstitials = new HashMap<>(2);
        this.mAppOpenAds = new HashMap<>(2);
        this.mRewardedAds = new HashMap<>(2);

        this.mAdViewAdFormats = new HashMap<>(2);
        this.mAdViewPositions = new HashMap<>(2);
        this.mAdViewOffsets = new HashMap<>(2);
        this.mAdViewWidths = new HashMap<>(2);
        this.mAdInfoMap = new HashMap<>();
        this.mAdInfoMapLock = new Object();

        this.mDisabledAdaptiveBannerAdUnitIds = new HashSet<>(2);
        this.mDisabledAutoRefreshAdViewAdUnitIds = new HashSet<>(2);

        this.mAdViewExtraParametersToSetAfterCreate = new HashMap<>(1);

        this.mAdUnitIdsToShowAfterCreate = new ArrayList<>(2);
        AppLovinSdkUtils.runOnUiThread(true, new Runnable() {
            public void run() {
                MaxAdsService.this.mSafeAreaBackground = new View((Context) MaxAdsService.getCurrentActivity());
                MaxAdsService.this.mSafeAreaBackground.setVisibility(View.GONE);
                MaxAdsService.this.mSafeAreaBackground.setBackgroundColor(0);
                MaxAdsService.this.mSafeAreaBackground.setClickable(false);
                FrameLayout layout = new FrameLayout((Context) MaxAdsService.getCurrentActivity());
                layout.addView(MaxAdsService.this.mSafeAreaBackground, (ViewGroup.LayoutParams) new FrameLayout.LayoutParams(0, 0));
                MaxAdsService.getCurrentActivity().addContentView((View) layout, (ViewGroup.LayoutParams) new LinearLayout.LayoutParams(-1, -1));
                ViewParent parent = layout.getParent();
                if (parent instanceof View)
                    ((View) parent).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                        public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                            for (MaxAdView adView : MaxAdsService.this.mAdViews.values()) {
                                ViewParent parent = adView.getParent();
                                if (parent instanceof View)
                                    ((View) parent).bringToFront();
                            }
                        }
                    });
            }
        });

        getCurrentActivity().getWindow().getDecorView().getRootView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                boolean viewBoundsChanged = (left != oldLeft || right != oldRight || bottom != oldBottom || top != oldTop);
                if (!viewBoundsChanged)
                    return;
                for (Map.Entry<String, MaxAdFormat> adUnitFormats : (Iterable<Map.Entry<String, MaxAdFormat>>) MaxAdsService.this.mAdViewAdFormats.entrySet())
                    MaxAdsService.this.positionAdView(adUnitFormats.getKey(), adUnitFormats.getValue());
            }
        });
    }

    private MaxAdView retrieveAdView(String adUnitId, MaxAdFormat adFormat) {
        return retrieveAdView(adUnitId, adFormat, null, null);
    }

    private MaxAdView retrieveAdView(String adUnitId, MaxAdFormat adFormat, String adViewPosition, Point adViewOffset) {
        MaxAdView result = this.mAdViews.get(adUnitId);
        if (result == null && adViewPosition != null && adViewOffset != null) {
            result = new MaxAdView(adUnitId, adFormat, this.sdk, (Context) getCurrentActivity());
            result.setListener(this);
            result.setRevenueListener(this);
            result.setAdReviewListener(this);
            this.mAdViews.put(adUnitId, result);
            this.mAdViewPositions.put(adUnitId, adViewPosition);
            this.mAdViewOffsets.put(adUnitId, adViewOffset);
            result.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
        }
        return result;
    }

    private JSONObject getDefaultAdEventParameters(String name, MaxAd ad) {
        JSONObject args = getAdInfo(ad);
        JsonUtils.putString(args, "name", name);
        return args;
    }

    private static void forwardUnityEvent(JSONObject args) {
        forwardUnityEvent(args, false);
    }

    private static void forwardUnityEvent(final JSONObject args, final boolean forwardInBackground) {
        sThreadPoolExecutor.execute(new Runnable() {
            public void run() {
                String serializedParameters = args.toString();
                if (forwardInBackground && MaxAdsService.backgroundCallback != null) {
                    MaxAdsService.backgroundCallback.onEvent(serializedParameters);
                } else {
                    UnityPlayer.UnitySendMessage("MaxSdkCallbacks", "ForwardEvent", serializedParameters);
                }
            }
        });
    }

    public String getAdInfo(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId))
            return "";
        MaxAd ad = getAd(adUnitId);
        if (ad == null)
            return "";
        JSONObject adInfo = getAdInfo(ad);
        return adInfo.toString();
    }

    private JSONObject getAdInfo(MaxAd ad) {
        JSONObject adInfo = new JSONObject();
        JsonUtils.putString(adInfo, "adUnitId", ad.getAdUnitId());
        JsonUtils.putString(adInfo, "adFormat", ad.getFormat().getLabel());
        JsonUtils.putString(adInfo, "networkName", ad.getNetworkName());
        JsonUtils.putString(adInfo, "networkPlacement", ad.getNetworkPlacement());
        JsonUtils.putString(adInfo, "creativeId", !TextUtils.isEmpty(ad.getCreativeId()) ? ad.getCreativeId() : "");
        JsonUtils.putString(adInfo, "placement", !TextUtils.isEmpty(ad.getPlacement()) ? ad.getPlacement() : "");
        JsonUtils.putString(adInfo, "revenue", String.valueOf(ad.getRevenue()));
        JsonUtils.putString(adInfo, "revenuePrecision", ad.getRevenuePrecision());
        JsonUtils.putString(adInfo, "latencyMillis", String.valueOf(ad.getRequestLatencyMillis()));
        JsonUtils.putString(adInfo, "dspName", !TextUtils.isEmpty(ad.getDspName()) ? ad.getDspName() : "");
        return adInfo;
    }

    private MaxAd getAd(String adUnitId) {
        synchronized (this.mAdInfoMapLock) {
            return this.mAdInfoMap.get(adUnitId);
        }
    }

    private void positionAdView(MaxAd ad) {
        positionAdView(ad.getAdUnitId(), ad.getFormat());
    }

    private void positionAdView(final String adUnitId, final MaxAdFormat adFormat) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                int adViewWidthDp, adViewHeightDp;
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    Log.e(TAG, adFormat.getLabel() + " does not exist");
                    return;
                }
                RelativeLayout relativeLayout = (RelativeLayout) adView.getParent();
                if (relativeLayout == null) {
                    Log.e(TAG, adFormat.getLabel() + "'s parent does not exist");
                    return;
                }
                Rect windowRect = new Rect();
                relativeLayout.getWindowVisibleDisplayFrame(windowRect);
                String adViewPosition = (String) MaxAdsService.this.mAdViewPositions.get(adUnitId);
                Point adViewOffset = (Point) MaxAdsService.this.mAdViewOffsets.get(adUnitId);
                MaxAdsService.Insets insets = MaxAdsService.getSafeInsets();
                boolean isAdaptiveBannerDisabled = MaxAdsService.this.mDisabledAdaptiveBannerAdUnitIds.contains(adUnitId);
                boolean isWidthDpOverridden = MaxAdsService.this.mAdViewWidths.containsKey(adUnitId);
                if (isWidthDpOverridden) {
                    adViewWidthDp = ((Integer) MaxAdsService.this.mAdViewWidths.get(adUnitId)).intValue();
                } else if ("top_center".equalsIgnoreCase(adViewPosition) || "bottom_center".equalsIgnoreCase(adViewPosition)) {
                    int adViewWidthPx = windowRect.width();
                    adViewWidthDp = AppLovinSdkUtils.pxToDp((Context) MaxAdsService.getCurrentActivity(), adViewWidthPx);
                } else {
                    adViewWidthDp = adFormat.getSize().getWidth();
                }
                if ((adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER) && !isAdaptiveBannerDisabled) {
                    adViewHeightDp = adFormat.getAdaptiveSize(adViewWidthDp, (Context) MaxAdsService.getCurrentActivity()).getHeight();
                } else {
                    adViewHeightDp = adFormat.getSize().getHeight();
                }
                int widthPx = AppLovinSdkUtils.dpToPx((Context) MaxAdsService.getCurrentActivity(), adViewWidthDp);
                int heightPx = AppLovinSdkUtils.dpToPx((Context) MaxAdsService.getCurrentActivity(), adViewHeightDp);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) adView.getLayoutParams();
                params.height = heightPx;
                adView.setLayoutParams((ViewGroup.LayoutParams) params);
                int gravity = 0;
                adView.setRotation(0.0F);
                adView.setTranslationX(0.0F);
                params.setMargins(0, 0, 0, 0);
                int marginLeft = insets.left + adViewOffset.x;
                int marginTop = insets.top + adViewOffset.y;
                int marginRight = insets.right;
                int marginBottom = insets.bottom;
                if ("centered".equalsIgnoreCase(adViewPosition)) {
                    gravity = 17;
                    if (MaxAdFormat.MREC == adFormat || isWidthDpOverridden) {
                        params.width = widthPx;
                    } else {
                        params.width = -1;
                    }
                } else {
                    if (adViewPosition.contains("top")) {
                        gravity = 48;
                    } else if (adViewPosition.contains("bottom")) {
                        gravity = 80;
                    }
                    if (adViewPosition.contains("center")) {
                        gravity |= 0x1;
                        if (MaxAdFormat.MREC == adFormat || isWidthDpOverridden) {
                            params.width = widthPx;
                        } else {
                            params.width = -1;
                        }
                        boolean containsLeft = adViewPosition.contains("left");
                        boolean containsRight = adViewPosition.contains("right");
                        if (containsLeft || containsRight) {
                            gravity |= 0x10;
                            if (MaxAdFormat.MREC == adFormat) {
                                gravity |= adViewPosition.contains("left") ? 3 : 5;
                            } else {
                                int windowWidth = windowRect.width() - insets.left - insets.right;
                                int windowHeight = windowRect.height() - insets.top - insets.bottom;
                                int longSide = Math.max(windowWidth, windowHeight);
                                int shortSide = Math.min(windowWidth, windowHeight);
                                int marginSign = (windowHeight > windowWidth) ? -1 : 1;
                                int margin = marginSign * (longSide - shortSide) / 2;
                                marginLeft += margin;
                                marginRight += margin;
                                int translationRaw = windowWidth / 2 - heightPx / 2;
                                int translationX = containsLeft ? -translationRaw : translationRaw;
                                adView.setTranslationX(translationX);
                                adView.setRotation(90.0F);
                            }
                            relativeLayout.setBackgroundColor(0);
                        }
                    } else {
                        params.width = widthPx;
                        if (adViewPosition.contains("left")) {
                            gravity |= 0x3;
                        } else if (adViewPosition.contains("right")) {
                            gravity |= 0x5;
                        }
                    }
                }
                if (MaxAdFormat.BANNER == adFormat || MaxAdFormat.LEADER == adFormat)
                    if (MaxAdsService.this.mPublisherBannerBackgroundColor != null) {
                        FrameLayout.LayoutParams safeAreaLayoutParams = (FrameLayout.LayoutParams) MaxAdsService.this.mSafeAreaBackground.getLayoutParams();
                        int safeAreaBackgroundGravity = 1;
                        if ("top_center".equals(adViewPosition)) {
                            safeAreaBackgroundGravity |= 0x30;
                            safeAreaLayoutParams.height = insets.top;
                            safeAreaLayoutParams.width = -1;
                            MaxAdsService.this.mSafeAreaBackground.setVisibility(adView.getVisibility());
                            marginLeft -= insets.left;
                            marginRight -= insets.right;
                        } else if ("bottom_center".equals(adViewPosition)) {
                            safeAreaBackgroundGravity |= 0x50;
                            safeAreaLayoutParams.height = insets.bottom;
                            safeAreaLayoutParams.width = -1;
                            MaxAdsService.this.mSafeAreaBackground.setVisibility(adView.getVisibility());
                            marginLeft -= insets.left;
                            marginRight -= insets.right;
                        } else {
                            MaxAdsService.this.mSafeAreaBackground.setVisibility(View.GONE);
                        }
                        safeAreaLayoutParams.gravity = safeAreaBackgroundGravity;
                        MaxAdsService.this.mSafeAreaBackground.requestLayout();
                    } else {
                        MaxAdsService.this.mSafeAreaBackground.setVisibility(View.GONE);
                    }
                params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
                relativeLayout.setGravity(gravity);
            }
        });
    }

    protected static Insets getSafeInsets() {
        Insets insets = new Insets();
        if (Build.VERSION.SDK_INT < 28)
            return insets;
        Window window = getCurrentActivity().getWindow();
        if (window == null)
            return insets;
        WindowInsets windowInsets = window.getDecorView().getRootWindowInsets();
        if (windowInsets == null)
            return insets;
        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        if (displayCutout == null)
            return insets;
        insets.left = displayCutout.getSafeInsetLeft();
        insets.top = displayCutout.getSafeInsetTop();
        insets.right = displayCutout.getSafeInsetRight();
        insets.bottom = displayCutout.getSafeInsetBottom();
        return insets;
    }

    private MaxInterstitialAd retrieveInterstitial(String adUnitId) {
        MaxInterstitialAd result = this.mInterstitials.get(adUnitId);
        if (result == null) {
            result = new MaxInterstitialAd(adUnitId, this.sdk, getCurrentActivity());
            result.setListener(this);
            result.setRevenueListener(this);
            result.setAdReviewListener(this);
            this.mInterstitials.put(adUnitId, result);
        }
        return result;
    }

    private MaxAppOpenAd retrieveAppOpenAd(String adUnitId) {
        MaxAppOpenAd result = this.mAppOpenAds.get(adUnitId);
        if (result == null) {
            result = new MaxAppOpenAd(adUnitId, this.sdk);
            result.setListener(this);
            result.setRevenueListener(this);
            this.mAppOpenAds.put(adUnitId, result);
        }
        return result;
    }

    private MaxRewardedAd retrieveRewardedAd(String adUnitId) {
        MaxRewardedAd result = this.mRewardedAds.get(adUnitId);
        if (result == null) {
            result = MaxRewardedAd.getInstance(adUnitId, this.sdk, getCurrentActivity());
            result.setListener(this);
            result.setRevenueListener(this);
            result.setAdReviewListener(this);
            this.mRewardedAds.put(adUnitId, result);
        }
        return result;
    }

    @Override
    public void Init(Activity activity, String[] args) {
        if (args == null || args.length == 0) {
            Log.e(TAG, "args is empty!");
            return;
        }
        _activity = activity;

        _bannerAdId = args[0];
        _interAdId = args[1];
        _rewardAdId = args[2];
        _mrecAdId = args[3];
        _nativeRectAdId = args[4];
        _nativeSmallAdId = args[5];

        _bannerPosition = Integer.parseInt(args[6]);
        _mrecPosition = Integer.parseInt(args[7]);

        if (args.length >= 9) _appOpenAdId = args[8];
        _apsEnable = false;
        //aps
        if (args.length >= 12) {

            _apsAppId = args[9];
            _apsBannerId = args[10];
            _apsMRECId = args[11];
            _apsInterId = args[12];
            _apsVideoRewardId = args[13];

            if (_apsAppId != null && !_apsAppId.equals("")) {
                _apsEnable = true;
                Log.d(TAG, "APS _apsAppId:" + _apsAppId);

                Log.d(TAG, "APS _apsBannerId:" + _apsBannerId);
                Log.d(TAG, "APS _apsInterId:" + _apsInterId);
                Log.d(TAG, "APS _apsMRECId:" + _apsMRECId);
                Log.d(TAG, "APS _apsVideoRewardId:" + _apsVideoRewardId);

                _maxAmazonAdsService = new AmazonAdsService();
                _maxAmazonAdsService.Init(activity, _apsAppId);
            }
        }

        mRectBannerState = 0;
        mRectShowFlag = 1;

        Context context = activity.getApplicationContext();

        AppLovinPrivacySettings.setHasUserConsent(true, context);
        AppLovinPrivacySettings.setIsAgeRestrictedUser(false, context);
        AppLovinPrivacySettings.setDoNotSell(false, context);

        this.sdk = AppLovinSdk.getInstance(getCurrentActivity().getApplicationContext());
        this.sdk.setMediationProvider("max");
        this.sdk.getSettings().setVerboseLogging(BuildConfig.DEBUG);
        this.sdk.getSettings().setCreativeDebuggerEnabled(BuildConfig.DEBUG);

        this.sdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                Log.d(TAG, "onSdkInitialized");

                if (BuildConfig.DEBUG) {
                    AppLovinSdk.getInstance(activity.getApplicationContext()).showMediationDebugger();
                }
            }
        });
    }

    @Override
    public void onResume(Activity activity) {
        ShowResumeAds();
        setBannerMrecToFront();
    }

    private void ShowResumeAds() {
        Log.d(TAG, "onResume blockAutoShowInterCount: " + blockAutoShowInterCount);
        if (blockAutoShowInterCount > 0) {
            DecreaseBlockAutoShowInter();
            return;
        }
        if (_isDisableResumeAds) return;

        boolean isShowResumeAds = FirebaseRemoteConfigService.getInstance().GetBoolean(Constants.RESUME_ADS_KEY);
        if (!isShowResumeAds) return;

        //resume from ads
        if (isClickToAds) {
            isClickToAds = false;
            return;
        }

        showInterstitial(RESUME_INTER_ADS);
    }

    //private
    void InitVideoRewardAds(Activity activity) {
//        String videoRewardKey = activity.getResources().getString(R.string.applovin_videoreward_key);
        String videoRewardKey = _rewardAdId;
        mRewardedAd = MaxRewardedAd.getInstance(videoRewardKey, activity);

        mRewardedAd.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        mRewardedAd.setListener(new MaxRewardedAdListener() {
            @Override
            public void onRewardedVideoStarted(MaxAd ad) {
                Log.d(TAG, "onRewardedVideoStarted: ============================");
                isFullscreenAdsShowing = true;

                if (blockAutoShowInterCount <= 0) {
                    IncreaseBlockAutoShowInter();
                }
            }

            @Override
            public void onRewardedVideoCompleted(MaxAd ad) {
                Log.d(TAG, "onRewardedVideoCompleted: =============================");
            }

            @Override
            public void onUserRewarded(final MaxAd maxAd, final MaxReward maxReward) {
                Log.d(TAG, "video reward onUserRewarded: =============================");
                // Rewarded ad was displayed and user should receive the reward
//                LibraryBridge.SendMessageFromNativeToGame("OnVideoRewardedWithCode", String.valueOf(mCurrentVideoRewardRequestCode));
                _adsAdsEventListener.onVideoRewardUserRewarded(String.valueOf(mCurrentVideoRewardRequestCode));
            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'

                // Reset retry attempt
                mRetryAttemptRewardAds = 0;
//                AnalyticServices.getInstance().LogEvent(UnityPlayer.currentActivity, "af_rewarded_api_called", null);
                _adsAdsEventListener.onVideoRewardLoaded();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
//                AnalyticServices.getInstance().LogEvent(UnityPlayer.currentActivity, "af_rewarded_ad_displayed", null);
                _adsAdsEventListener.onVideoRewardDisplayed();
                Log.d(TAG, "video reward onAdDisplayed: =============================");
                IncreaseBlockAutoShowInter();
            }

            @Override
            public void onAdHidden(MaxAd ad) {
                // rewarded ad is hidden. Pre-load the next ad
                Log.d(TAG, "video reward onAdHidden: =============================");
                LoadVideoRewardAd(false);
                isFullscreenAdsShowing = false;

                //add some delay
                if (!isCoolDownShowInter) {
                    isCoolDownShowInter = true;
                    ThreadUltils.startTask(() -> {
                        // doTask
                        isCoolDownShowInter = false;
                        Log.d(TAG, "Inter Reset Cooldown");
                    }, 5 * 1000L);
                }
            }

            @Override
            public void onAdClicked(MaxAd ad) {
//                AnalyticServices.getInstance().LogEventAdClicked(UnityPlayer.currentActivity, ad.getFormat().getLabel());
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());

                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                // Rewarded ad failed to load
                // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)

                mRetryAttemptRewardAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptRewardAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LoadVideoRewardAd(false);
                    }
                }, delayMillis);
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                // Rewarded ad failed to display. We recommend loading the next ad
                LoadVideoRewardAd(false);
            }
        });

        LoadVideoRewardAd(true);
    }

    private void LoadVideoRewardAd(boolean isFirstLoad) {
        if (mRewardedAd == null) {
            Log.d(TAG, "Video reward: onAdLoadFailed xxx");
            return;
        }
        if (isFirstLoad && _apsVideoRewardId != null && !_apsVideoRewardId.equals("")) {
            _maxAmazonAdsService.loadRewardAd(_apsVideoRewardId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    Log.d(TAG, "APS load video reward onFailure : " + adError.getMessage());
                    mRewardedAd.setLocalExtraParameter("amazon_ad_error", adError);
                    mRewardedAd.loadAd();
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    Log.d(TAG, "APS load video reward onSuccess : " + dtbAdResponse.getImpressionUrl());
                    mRewardedAd.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    mRewardedAd.loadAd();
                }
            });
        } else {
            mRewardedAd.loadAd();
        }
    }

    void InitInterAds(Activity activity) {
//        String interKey = activity.getResources().getString(R.string.applovin_inter_key);
        Log.d(TAG, "createInterstitialAd: " + _interAdId);

        mInterstitialAd = new MaxInterstitialAd(_interAdId, activity);
        mInterstitialAd.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        mInterstitialAd.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {
                Log.d(TAG, "Inter: onAdExpanded");
            }

            @Override
            public void onAdCollapsed(MaxAd ad) {
                Log.d(TAG, "Inter: onAdCollapsed");
            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                Log.d(TAG, "Inter: onAdLoaded");
                // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'

                // Reset retry attempt
                mRetryAttemptInterstitialAds = 0;
                _adsAdsEventListener.onInterLoaded();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {

                Log.d(TAG, "Inter: onAdDisplayed");
                isFullscreenAdsShowing = true;

                _adsAdsEventListener.onInterDisplayed();

            }

            @Override
            public void onAdHidden(MaxAd ad) {
                isFullscreenAdsShowing = false;

                // Interstitial ad is hidden. Pre-load the next ad
                LoadInterAd(false);
                //check is ad resume
                if (mCurrentInterRequestCode == RESUME_INTER_ADS) {
                    Log.d(TAG, "Inter: ads resume Hidden ");
                    return;
                }
                Log.d(TAG, "Inter: onAdHidden Normal");

                coolDownShowInterInSecond = FirebaseRemoteConfigService.getInstance().GetInt(Constants.ADS_INTERVAL);

//                ThreadUltils.startTask(() -> {
//                    // doTask
//                    isCoolDownShowInter = false;
//                    mCurrentInterRequestCode = 0;
//                    Log.d(TAG, "Inter: onAdHidden Reset Cooldown");
//                }, coolDownShowInterInSencond * 1000L);
                RunCountDownToShowInter();

                _adsAdsEventListener.onInterHidden(String.valueOf(mCurrentInterRequestCode));
            }

            @Override
            public void onAdClicked(MaxAd ad) {
                Log.d(TAG, "Inter: onAdClicked");
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());
                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                Log.d(TAG, "Inter: onAdLoadFailed");
                // Interstitial ad failed to load
                // AppLovin recommends that you retry with exponentially higher delays up to a maximum delay (in this case 64 seconds)

                mRetryAttemptInterstitialAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptInterstitialAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        LoadInterAd(false);
                    }
                }, delayMillis);
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                // Interstitial ad failed to display. AppLovin recommends that you load the next ad.
                LoadInterAd(false);
            }
        });

        // Load the first ad
        LoadInterAd(true);
    }

    private void LoadInterAd(boolean isFirstLoad) {
        if (mInterstitialAd == null) {
            Log.d(TAG, "Inter: onAdLoadFailed xxx");
            return;
        }
        if (isFirstLoad && _apsInterId != null && !_apsInterId.equals("")) {
            _maxAmazonAdsService.loadInterAd(_apsInterId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    Log.d(TAG, "APS load inter onFailure : " + adError.getMessage());
                    mInterstitialAd.setLocalExtraParameter("amazon_ad_error", adError);
                    mInterstitialAd.loadAd();
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    Log.d(TAG, "APS load inter onSuccess : " + dtbAdResponse.getImpressionUrl());
                    mInterstitialAd.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    mInterstitialAd.loadAd();
                }
            });
        } else {
            mInterstitialAd.loadAd();
        }
    }

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

    private void LoadMREC(Activity activity, int position) {
//        String mrecKey = activity.getResources().getString(R.string.applovin_mrec_ads_key);
        Log.d(TAG, "LoadRectBannerApplovin: bannerKey" + _mrecAdId);

        rectAdView = new MaxAdView(_mrecAdId, MaxAdFormat.MREC, activity.getApplicationContext());
        rectAdView.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });
        rectAdView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(MaxAd ad) {

            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                Log.d(TAG, "MREC onAdLoaded");
                isMRECLoaded = true;
                isMRECLoading = false;

                mRectBannerState = 2;
                if (mRectShowFlag == 1) {
                    hideMRec();
                }
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {

            }

            @Override
            public void onAdHidden(MaxAd ad) {

            }

            @Override
            public void onAdClicked(MaxAd ad) {
                Log.d(TAG, "onMRECAdClicked: ");
//                AnalyticManager.getInstance().ShowAds(2);
                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
//                mRectBannerLoaded = false;
                Log.d(TAG, "MREC: onAdLoadFailed: ");
                isMRECLoading = false;
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {

            }
        });

        // MREC width and height are 300 and 250 respectively, on phones and tablets
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 300);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 250);
        int gravity = position == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPx, heightPx, gravity);
        layoutParams.setMargins(0, 0, 0, 0);
        rectAdView.setLayoutParams(layoutParams);
        rectAdView.setVisibility(View.GONE);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(rectAdView);

        // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
        rectAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
        rectAdView.stopAutoRefresh();

        if (_apsEnable && _apsMRECId != null && !_apsMRECId.equals("")) {
            _maxAmazonAdsService.loadMRECAd(_apsMRECId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    // 'adView' is your instance of MaxAdView
                    rectAdView.setLocalExtraParameter("amazon_ad_error", adError);
                    rectAdView.loadAd();
                    isMRECLoading = true;
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    // 'adView' is your instance of MaxAdView
                    rectAdView.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    rectAdView.loadAd();
                    isMRECLoading = true;
                }
            });
        } else {
            // Load the ad
            rectAdView.loadAd();
            mRectBannerState = 1;
            isMRECLoading = true;
        }
    }

    private void LogRevenue(MaxAd ad) {
        double revenue = ad.getRevenue(); // In USD
        MaxAdsService.d("onAdRevenuePaid: revenue: " + revenue);

        String networkName = ad.getNetworkName(); // Display name of the network that showed the ad (e.g. "AdColony")
        String adUnitId = ad.getAdUnitId(); // The MAX Ad Unit ID
        String adFormatStr = ad.getFormat().getLabel();
        _adsAdsEventListener.onAdRevenuePaid(adFormatStr, adUnitId, networkName, revenue);
    }

    boolean _isBannerLoading;

    private MaxAdFormat getAdViewAdFormat(String adUnitId) {
        if (this.mAdViewAdFormats.containsKey(adUnitId))
            return this.mAdViewAdFormats.get(adUnitId);
        return getDeviceSpecificAdViewAdFormat();
    }

    private static MaxAdFormat getDeviceSpecificAdViewAdFormat() {
        return AppLovinSdkUtils.isTablet((Context) getCurrentActivity()) ? MaxAdFormat.LEADER : MaxAdFormat.BANNER;
    }

    private static Activity getCurrentActivity() {
        return Utils.getCurrentActivity();
    }

    private void CreateBanner(String adUnitId, String bannerPosition) {
        createAdView(adUnitId, getAdViewAdFormat(adUnitId), bannerPosition, DEFAULT_AD_VIEW_OFFSET);
    }

    public void createMRec(String adUnitId, String mrecPosition) {
        createAdView(adUnitId, MaxAdFormat.MREC, mrecPosition, DEFAULT_AD_VIEW_OFFSET);
    }

    public void loadMRec(String adUnitId) {
        loadAdView(adUnitId, MaxAdFormat.MREC);
    }

    private void loadAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxAdsService.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                if (!MaxAdsService.this.mDisabledAutoRefreshAdViewAdUnitIds.contains(adUnitId)) {
                    if (adView.getVisibility() != View.VISIBLE) {
                        MaxAdsService.e("Auto-refresh will resume when the " + adFormat.getLabel() + " ad is shown. You should only call LoadBanner() or LoadMRec() if you explicitly pause auto-refresh and want to manually load an ad.");
                        return;
                    }
                    MaxAdsService.e("You must stop auto-refresh if you want to manually load " + adFormat.getLabel() + " ads.");
                    return;
                }
                adView.loadAd();
            }
        });
    }

    private void createAdView(final String adUnitId, final MaxAdFormat adFormat, final String adViewPosition, final Point adViewOffsetPixels) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                Log.d(TAG, "Creating " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\" and position: \"" + adViewPosition + "\"");
                if (MaxAdsService.this.mAdViews.get(adUnitId) != null)
                    Log.w("MaxUnityAdManager", "Trying to create a " + adFormat.getLabel() + " that was already created. This will cause the current ad to be hidden.");
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat, adViewPosition, adViewOffsetPixels);
                if (adView == null) {
                    Log.e(TAG, "adFormat.getLabel()" + "does not exist");
                    return;
                }

                MaxAdsService.this.mSafeAreaBackground.setVisibility(View.GONE);
                adView.setVisibility(View.GONE);

                //add to current
                if (adView.getParent() == null) {
                    Activity currentActivity = MaxAdsService.getCurrentActivity();
                    RelativeLayout relativeLayout = new RelativeLayout((Context) currentActivity);
                    currentActivity.addContentView((View) relativeLayout, (ViewGroup.LayoutParams) new LinearLayout.LayoutParams(-1, -1));
                    relativeLayout.addView((View) adView);
                    MaxAdsService.this.mAdViewAdFormats.put(adUnitId, adFormat);
                    MaxAdsService.this.positionAdView(adUnitId, adFormat);
                }
                Map<String, String> extraParameters = (Map<String, String>) MaxAdsService.this.mAdViewExtraParametersToSetAfterCreate.get(adUnitId);
                if (adFormat.isBannerOrLeaderAd())
                    if (extraParameters == null || !extraParameters.containsKey("adaptive_banner"))
                        adView.setExtraParameter("adaptive_banner", "true");

//                if (extraParameters != null) {
//                    for (Map.Entry<String, String> extraParameter : extraParameters.entrySet()) {
//                        adView.setExtraParameter(extraParameter.getKey(), extraParameter.getValue());
//                        MaxAdsService.this.maybeHandleExtraParameterChanges(adUnitId, adFormat, extraParameter.getKey(), extraParameter.getValue());
//                    }
//                    MaxAdsService.this.mAdViewExtraParametersToSetAfterCreate.remove(adUnitId);
//                }
//                if (MaxAdsService.this.mAdViewLocalExtraParametersToSetAfterCreate.containsKey(adUnitId)) {
//                    Map<String, Object> localExtraParameters = (Map<String, Object>) MaxAdsService.this.mAdViewLocalExtraParametersToSetAfterCreate.get(adUnitId);
//                    if (localExtraParameters != null) {
//                        for (Map.Entry<String, Object> localExtraParameter : localExtraParameters.entrySet())
//                            adView.setLocalExtraParameter(localExtraParameter.getKey(), localExtraParameter.getValue());
//                        MaxAdsService.this.mAdViewLocalExtraParametersToSetAfterCreate.remove(adUnitId);
//                    }
//                }
//                if (MaxAdsService.this.mAdViewCustomDataToSetAfterCreate.containsKey(adUnitId)) {
//                    String customData = (String) MaxAdsService.this.mAdViewCustomDataToSetAfterCreate.get(adUnitId);
//                    adView.setCustomData(customData);
//                    MaxAdsService.this.mAdViewCustomDataToSetAfterCreate.remove(adUnitId);
//                }

                adView.loadAd();

                if (MaxAdsService.this.mDisabledAutoRefreshAdViewAdUnitIds.contains(adUnitId))
                    adView.stopAutoRefresh();
                if (MaxAdsService.this.mAdUnitIdsToShowAfterCreate.contains(adUnitId)) {
                    MaxAdsService.this.showAdView(adUnitId, adFormat);
                    MaxAdsService.this.mAdUnitIdsToShowAfterCreate.remove(adUnitId);
                }
            }
        });
    }

    private void showAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                Log.d(TAG, "Showing " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\"");
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    Log.e(TAG, adFormat.getLabel() + " does not exist for ad unit id \"" + adUnitId + "\"");
                    MaxAdsService.this.mAdUnitIdsToShowAfterCreate.add(adUnitId);
                    return;
                }
                MaxAdsService.this.mSafeAreaBackground.setVisibility(View.VISIBLE);
                adView.setVisibility(View.VISIBLE);
                if (!MaxAdsService.this.mDisabledAutoRefreshAdViewAdUnitIds.contains(adUnitId))
                    adView.startAutoRefresh();
            }
        });
    }

    private void startAdViewAutoRefresh(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                Log.d(TAG, "Starting " + adFormat.getLabel() + " auto refresh for ad unit identifier \"" + adUnitId + "\"");
                MaxAdsService.this.mDisabledAutoRefreshAdViewAdUnitIds.remove(adUnitId);
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    Log.e(TAG, adFormat.getLabel() + " does not exist for ad unit identifier \"" + adUnitId + "\"");
                    return;
                }
                adView.startAutoRefresh();
            }
        });
    }

    private void stopAdViewAutoRefresh(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                Log.d(TAG, "Stopping " + adFormat.getLabel() + " auto refresh for ad unit identifier \"" + adUnitId + "\"");
                MaxAdsService.this.mDisabledAutoRefreshAdViewAdUnitIds.add(adUnitId);
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    Log.e(TAG, adFormat.getLabel() + " does not exist for ad unit identifier \"" + adUnitId + "\"");
                    return;
                }
                adView.stopAutoRefresh();
            }
        });
    }

    private void updateAdViewPosition(final String adUnitId, final String adViewPosition, final Point offsetPixels, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                Log.d(TAG, "Updating " + adFormat.getLabel() + " position to \"" + adViewPosition + "\" for ad unit id \"" + adUnitId + "\"");
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    Log.e(TAG, adFormat.getLabel() + " does not exist");
                    return;
                }
                MaxAdsService.this.mAdViewPositions.put(adUnitId, adViewPosition);
                MaxAdsService.this.mAdViewOffsets.put(adUnitId, offsetPixels);
                MaxAdsService.this.positionAdView(adUnitId, adFormat);
            }
        });
    }

    private void hideAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                Log.d(TAG, "Hiding " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\"");
                MaxAdsService.this.mAdUnitIdsToShowAfterCreate.remove(adUnitId);
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    Log.e(TAG, adFormat.getLabel() + " does not exist");
                    return;
                }
                MaxAdsService.this.mSafeAreaBackground.setVisibility(View.GONE);
                adView.setVisibility(View.GONE);
                adView.stopAutoRefresh();
            }
        });
    }

    private String getAdViewLayout(String adUnitId, MaxAdFormat adFormat) {
        Log.d(TAG, "Getting " + adFormat.getLabel() + " absolute position with ad unit id \"" + adUnitId + "\"");
        MaxAdView adView = retrieveAdView(adUnitId, adFormat);
        if (adView == null) {
            Log.e(TAG, adFormat.getLabel() + " does not exist");
            return "";
        }
        int[] location = new int[2];
        adView.getLocationOnScreen(location);
        int originX = AppLovinSdkUtils.pxToDp((Context) getCurrentActivity(), location[0]);
        int originY = AppLovinSdkUtils.pxToDp((Context) getCurrentActivity(), location[1]);
        int width = AppLovinSdkUtils.pxToDp((Context) getCurrentActivity(), adView.getWidth());
        int height = AppLovinSdkUtils.pxToDp((Context) getCurrentActivity(), adView.getHeight());
        JSONObject rectMap = new JSONObject();
        JsonUtils.putString(rectMap, "origin_x", String.valueOf(originX));
        JsonUtils.putString(rectMap, "origin_y", String.valueOf(originY));
        JsonUtils.putString(rectMap, "width", String.valueOf(width));
        JsonUtils.putString(rectMap, "height", String.valueOf(height));
        return rectMap.toString();
    }

    private void setAdViewWidth(final String adUnitId, final int widthDp, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxAdsService.d("Setting width " + widthDp + " for \"" + adFormat + "\" with ad unit identifier \"" + adUnitId + "\"");
                boolean isBannerOrLeader = adFormat.isBannerOrLeaderAd();
                int minWidthDp = isBannerOrLeader ? MaxAdFormat.BANNER.getSize().getWidth() : adFormat.getSize().getWidth();
                if (widthDp < minWidthDp)
                    MaxAdsService.e("The provided width: " + widthDp + "dp is smaller than the minimum required width: " + minWidthDp + "dp for ad format: " + adFormat + ". Automatically setting width to " + minWidthDp + ".");
                int widthToSet = Math.max(minWidthDp, widthDp);
                MaxAdsService.this.mAdViewWidths.put(adUnitId, Integer.valueOf(widthToSet));
                MaxAdsService.this.positionAdView(adUnitId, adFormat);
            }
        });
    }

    private void setAdViewBackgroundColor(final String adUnitId, final MaxAdFormat adFormat, final String hexColorCode) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxAdsService.d("Setting " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\" to color: " + hexColorCode);
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxAdsService.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                int backgroundColor = Color.parseColor(hexColorCode);
                MaxAdsService.this.mPublisherBannerBackgroundColor = Integer.valueOf(backgroundColor);
                MaxAdsService.this.mSafeAreaBackground.setBackgroundColor(backgroundColor);
                adView.setBackgroundColor(backgroundColor);
            }
        });
    }

    private void destroyAdView(final String adUnitId, final MaxAdFormat adFormat) {
        Utils.runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
            public void run() {
                MaxAdsService.d("Destroying " + adFormat.getLabel() + " with ad unit id \"" + adUnitId + "\"");
                MaxAdView adView = MaxAdsService.this.retrieveAdView(adUnitId, adFormat);
                if (adView == null) {
                    MaxAdsService.e(adFormat.getLabel() + " does not exist");
                    return;
                }
                ViewParent parent = adView.getParent();
                if (parent instanceof ViewGroup)
                    ((ViewGroup) parent).removeView((View) adView);
                adView.setListener(null);
                adView.setRevenueListener(null);
                adView.setAdReviewListener(null);
                adView.destroy();

                MaxAdsService.this.mAdViews.remove(adUnitId);
                MaxAdsService.this.mAdViewAdFormats.remove(adUnitId);
                MaxAdsService.this.mAdViewPositions.remove(adUnitId);
                MaxAdsService.this.mAdViewOffsets.remove(adUnitId);
                MaxAdsService.this.mAdViewWidths.remove(adUnitId);
                MaxAdsService.this.mDisabledAdaptiveBannerAdUnitIds.remove(adUnitId);
            }
        });
    }

    public String getBannerLayout(String adUnitId) {
        return getAdViewLayout(adUnitId, getAdViewAdFormat(adUnitId));
    }

    public String getMRecLayout(String adUnitId) {
        return getAdViewLayout(adUnitId, MaxAdFormat.MREC);
    }

    private void LoadNormalBanner(Activity activity, int position) {
//        String bannerKey = activity.getResources().getString(R.string.applovin_banner_key);
        Log.d(TAG, "Load Banner: " + _bannerAdId);

        bannerAdView = new MaxAdView(_bannerAdId, activity.getApplicationContext());
        bannerAdView.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        bannerAdView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(MaxAd ad) {

            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                _isBannerLoading = false;
                Log.d(TAG, "BANNER onAdLoaded: ");
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
                Log.d(TAG, "onAdDisplayed: ");
            }

            @Override
            public void onAdHidden(MaxAd ad) {

            }

            @Override
            public void onAdClicked(MaxAd ad) {
                _adsAdsEventListener.onAdClicked(ad.getFormat().getLabel());
                isClickToAds = true;
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                _isBannerLoading = false;
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {

            }
        });

        // Stretch to the width of the screen for banners to be fully functional
        int width = ViewGroup.LayoutParams.MATCH_PARENT;

        // Banner height on phones and tablets is 50 and 90, respectively
//        int heightPx = activity.getResources().getDimensionPixelSize(R.dimen.banner_height);

//         Get the adaptive banner height.
        int heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight();
        int heightPx = AppLovinSdkUtils.dpToPx(activity, heightDp);
        bannerAdView.setExtraParameter("adaptive_banner", "true");
        bannerAdView.setBackgroundColor(Color.rgb(0, 0, 0));

        int gravity = 0;
        if (position == Constants.POSITION_CENTER_TOP)
            gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        else gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        bannerAdView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx, gravity));

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(bannerAdView);

        _LoadBannerInternal(activity);
    }

    private void _LoadBannerInternal(Activity activity) {
        if (_isBannerLoading) {
            Log.d(TAG, "_LoadBannerInternal IsLoading....");
            return;
        }

        if (_apsEnable && _apsBannerId != null && !_apsBannerId.equals("")) {
            _maxAmazonAdsService.loadBannerAd(activity, _apsBannerId, new DTBAdCallback() {
                @Override
                public void onFailure(@NonNull AdError adError) {
                    Log.d(TAG, "APS onFailure " + adError.getMessage());
                    // 'adView' is your instance of MaxAdView
                    bannerAdView.setLocalExtraParameter("amazon_ad_error", adError);
                    bannerAdView.loadAd();
                    _isBannerLoading = true;
                }

                @Override
                public void onSuccess(@NonNull DTBAdResponse dtbAdResponse) {
                    Log.d(TAG, "APS onSuccess " + dtbAdResponse.getImpressionUrl());
                    // 'adView' is your instance of MaxAdView
                    bannerAdView.setLocalExtraParameter("amazon_ad_response", dtbAdResponse);
                    bannerAdView.loadAd();
                    _isBannerLoading = true;
                }
            });
        } else {
            // Load the ad
            bannerAdView.loadAd();
            _isBannerLoading = true;
        }
    }

    private void ShowNormalBanner(Activity activity, int position) {
        if (bannerAdView != null) {
//            Log.d(TAG, "ShowBottomBannerAppLovin: 1");
            if (bannerAdView.getVisibility() != View.VISIBLE)
                bannerAdView.setVisibility(View.VISIBLE);

            bannerAdView.startAutoRefresh();
        } else {
//            Log.d(TAG, "ShowBottomBannerAppLovin: 2");
            LoadNormalBanner(activity, position);
        }
    }

    private static void d(String message) {
        String fullMessage = "[MaxUnityAdManager] " + message;
        Log.d("AppLovinSdk", fullMessage);
    }

    private static void e(String message) {
        String fullMessage = "[MaxUnityAdManager] " + message;
        Log.e("AppLovinSdk", fullMessage);
    }

    private void HideNormalBanner() {
        Log.d(TAG, "StopBottomBanner");
        if (bannerAdView != null) {

            // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
            bannerAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
            bannerAdView.stopAutoRefresh();

            bannerAdView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean IsRewardReady() {
        MaxRewardedAd interstitial = retrieveRewardedAd(_rewardAdId);
        return interstitial.isReady();
    }

    @Override
    public boolean isInterstitialReady() {
        MaxInterstitialAd interstitial = retrieveInterstitial(_interAdId);
        return interstitial.isReady();
    }

    @Override
    public void ShowReward(int requestCode) {
        if (IsRewardReady()) {
            mCurrentVideoRewardRequestCode = requestCode;
            mRewardedAd.showAd();
        } else {
            Log.e(TAG, "ShowVideo Applovin: FAILEDDDDDDDDDDDDD");
        }
    }

    @Override
    public void showInterstitial(int requestCode) {
        Log.d(TAG, "ShowInter: " + requestCode);

        if (_isDisableInterAds) {
            Log.d(TAG, "Inter Ads Is Disabled !!!!!!!");
            return;
        }

        if (isFullscreenAdsShowing) {
            Log.d(TAG, "Other full screen ads is showing, wait !!!!!!");
            return;
        }

        //show resume ads
        if (requestCode == RESUME_INTER_ADS) {
            if (isInterstitialReady()) {
                mInterstitialAd.showAd();
            }
            Log.d(TAG, "ShowInter: check 1 ");
            return;
        }

        //show normal
        if (isCoolDownShowInter) {
            Log.d(TAG, "ShowInter: check 2 ");
            return;
        }

        if (isInterstitialReady()) {
            //reset flags
            isClickToAds = false;
            Log.d(TAG, "ShowInter: check 3 ");
            mCurrentInterRequestCode = requestCode;
            mInterstitialAd.showAd();

        } else {
            Log.d(TAG, "ShowInter: check 4 ");
        }
    }

    @Override
    public void ShowBanner(Activity activity) {
        Log.d(TAG, "ShowBanner");
        ShowNormalBanner(activity, _bannerPosition);

//        int type = FirebaseRemoteConfigService.getInstance().GetInt(Constants.RK_BANNER_TYPE_OF_SCREEN + screenCode);
//        Log.d(TAG, "ShowBanner for screen: " + screenCode + " with position: " + _bannerPosition + ", type: " + type);
//
//        if (type == 0) {
//            // Show Normal Banner
//
//        } else if (type == 1) {
//            // Show Native Banner
//            ShowNativeBanner(activity, _bannerPosition);
//        }
    }

    @Override
    public void HideBanner() {
        HideNormalBanner();
        HideNativeBanner();
    }

    @Override
    public void showMRec(Activity activity) {
        Log.d(TAG, "ShowMREC");
        if (rectAdView == null) {
            LoadMREC(activity, _mrecPosition);
            return;
        }

        mRectShowFlag = 2;

        if (rectAdView.getVisibility() != View.VISIBLE) {
            rectAdView.setVisibility(View.VISIBLE);

            rectAdView.startAutoRefresh();

            if (!isMRECLoading && !isMRECLoaded) {
                rectAdView.loadAd();
            }
        }
    }

    @Override
    public void hideMRec() {
        Log.d(TAG, "HideMREC");
        if (mRectBannerState == 2 && rectAdView != null) {
            rectAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
            rectAdView.stopAutoRefresh();
        }

        if (rectAdView != null && rectAdView.getVisibility() == View.VISIBLE) {
            rectAdView.setVisibility(View.GONE);
        }

        mRectShowFlag = 1;
    }

    //fix bug for unity 2022.3.12
    private void setBannerMrecToFront() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (rectAdView != null && rectAdView.getVisibility() == View.VISIBLE) {
                    rectAdView.bringToFront();
                }

                if (bannerAdView != null && bannerAdView.getVisibility() == View.VISIBLE) {
                    bannerAdView.bringToFront();
                }
            }
        }, 500);


    }

    @Override
    public void ShowNativeMREC(Activity activity) {
        Log.d(TAG, "ShowNativeMREC");

        if (nativeRectAdLoader != null) {
            mNativeRectAdsContainer.setVisibility(View.VISIBLE);
            nativeRectAdLoader.loadAd();
        } else {
            LoadRectNativeAds(activity, _mrecPosition);
        }
    }

    @Override
    public void HideNativeMREC() {
        if (nativeRectAd != null) {
            nativeRectAdLoader.destroy(nativeRectAd);
        }
        if (mNativeRectAdsContainer != null) {
            mNativeRectAdsContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void ShowNativeBanner(Activity activity) {
        if (nativeBannerAdLoader != null) {
            Log.d(TAG, "ShowNativeBanner: 111");
            mNativeBannerAdsContainer.setVisibility(View.VISIBLE);
            nativeBannerAdLoader.loadAd();
        } else {
            Log.d(TAG, "ShowNativeBanner: 222");
            InitNativeBannerAds(activity, _bannerPosition);
        }
    }

    @Override
    public void HideNativeBanner() {
        //destroy
        if (nativeBannerAd != null) {
            nativeBannerAdLoader.destroy(nativeBannerAd);
        }
        //hide container
        if (mNativeBannerAdsContainer != null) {
            mNativeBannerAdsContainer.setVisibility(View.GONE);
        }
    }

    private void InitNativeBannerAds(Activity activity, int position) {
        //prepare container
        mNativeBannerAdsContainer = new FrameLayout(activity);
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 360);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 120);

        int gravity = position == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        mNativeBannerAdsContainer.setLayoutParams(new FrameLayout.LayoutParams(widthPx, heightPx, gravity));

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(mNativeBannerAdsContainer);

        nativeBannerAdLoader = new MaxNativeAdLoader(_nativeSmallAdId, activity.getApplicationContext());
        nativeBannerAdLoader.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        nativeBannerAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad) {
                // Clean up any pre-existing native ad to prevent memory leaks.
                Log.d(TAG, "InitNativeBannerAds ");
                Log.d(TAG, "InitNativeBannerAds x: " + (nativeAdView == null));
                Log.d(TAG, "InitNativeBannerAds y: " + (mNativeBannerAdsContainer == null));
                mRetryAttemptNativeAds = 0;
                if (nativeBannerAd != null) {
                    nativeBannerAdLoader.destroy(nativeBannerAd);
                }

                // Save ad for cleanup.
                nativeBannerAd = ad;

                // Add ad view to view.
                mNativeBannerAdsContainer.removeAllViews();
                mNativeBannerAdsContainer.addView(nativeAdView);
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error) {
                // We recommend retrying with exponentially higher delays up to a maximum delay
                Log.d(TAG, "onNativeAdLoadFailed ");
                mRetryAttemptNativeBannerAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptNativeBannerAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nativeBannerAdLoader.loadAd();
                    }
                }, delayMillis);

            }

            @Override
            public void onNativeAdClicked(final MaxAd ad) {
                // Optional click callback
            }
        });

        nativeBannerAdLoader.loadAd();
    }

    //Native RECT
    private void LoadRectNativeAds(Activity activity, int position) {
        //prepare container
        mNativeRectAdsContainer = new FrameLayout(activity);
        int widthPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 300);
        int heightPx = AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 250);

        int gravity = 0;
        if (position == Constants.POSITION_CENTER_TOP)
            gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        else gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPx, heightPx, gravity);
        layoutParams.setMargins(0, 0, 0, 0);

        mNativeRectAdsContainer.setLayoutParams(layoutParams);

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(mNativeRectAdsContainer);

        Log.d(TAG, "_nativeRectAdId " + _nativeRectAdId);
        nativeRectAdLoader = new MaxNativeAdLoader(_nativeRectAdId, activity.getApplicationContext());

        nativeRectAdLoader.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd ad) {
                LogRevenue(ad);
            }
        });

        nativeRectAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad) {
                // Clean up any pre-existing native ad to prevent memory leaks.
                Log.d(TAG, "LoadRectNativeAds ");
                Log.d(TAG, "LoadRectNativeAds x: " + (nativeAdView == null));
                Log.d(TAG, "LoadRectNativeAds z: " + (ad == null));
                Log.d(TAG, "LoadRectNativeAds y: " + (mNativeRectAdsContainer == null));

                mRetryAttemptNativeAds = 0;

                if (nativeRectAd != null) {
                    nativeRectAdLoader.destroy(nativeRectAd);
                }

                // Save ad for cleanup.
                nativeRectAd = ad;

                // Add ad view to view.
                mNativeRectAdsContainer.removeAllViews();
                mNativeRectAdsContainer.addView(nativeAdView);
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error) {
                // We recommend retrying with exponentially higher delays up to a maximum delay
                Log.d(TAG, "onNativeAdLoadFailed ");
                mRetryAttemptNativeAds++;
                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, mRetryAttemptNativeAds)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nativeRectAdLoader.loadAd();
                    }
                }, delayMillis);

            }

            @Override
            public void onNativeAdClicked(final MaxAd ad) {
                // Optional click callback
            }
        });

        nativeRectAdLoader.loadAd();
    }

    @Override
    public void loadAppOpenAd(Activity activity) {
        MaxAppOpenAd appOpenAd = retrieveAppOpenAd(_appOpenAdId);
        appOpenAd.loadAd();
    }

    @Override
    public void showAppOpenAd(Activity activity) {
        if (appOpenAd == null || !sdk.isInitialized())
            return;

        if (appOpenAd.isReady()) {
            MaxAdsService.d("ShowOpenAppAds ");
            appOpenAd.showAd();
        }
    }

    @Override
    public boolean isAppOpenAdReady() {
        MaxAppOpenAd appOpenAd = retrieveAppOpenAd(_appOpenAdId);
        return appOpenAd.isReady();
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
    public void OnPause(Activity activity) {

    }

    private boolean _isDisableResumeAds;

    @Override
    public void DisableResumeAds() {
        _isDisableResumeAds = true;
    }

    @Override
    public void EnableResumeAds() {
        _isDisableResumeAds = false;
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

    AdsEventListener _adsAdsEventListener;

    @Override
    public void SetEventListener(AdsEventListener listener) {
        _adsAdsEventListener = listener;
    }

    private static class SdkThreadFactory implements ThreadFactory {
        private SdkThreadFactory() {
        }

        public Thread newThread(Runnable r) {
            Thread result = new Thread(r, "AppLovinSdk:Max-Unity-Plugin:shared");
            result.setDaemon(true);
            result.setPriority(5);
            result.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable th) {
                    Log.e("MaxUnityAdManager", "Caught unhandled exception", th);
                }
            });
            return result;
        }
    }

    public static interface BackgroundCallback {
        void onEvent(String param1String);
    }
}

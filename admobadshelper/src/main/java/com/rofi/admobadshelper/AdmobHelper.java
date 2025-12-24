package com.rofi.admobadshelper;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnAdInspectorClosedListener;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.AdInspectorError;
import com.google.android.ump.ConsentInformation;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AdmobHelper - Facade for managing all ad types
 * 
 * This class has been refactored to use the Facade pattern.
 * All ad-specific logic has been moved to dedicated manager classes:
 * - BannerAdManager: Banner/Collapsible Banner ads
 * - MrecAdManager: MREC (Medium Rectangle) ads
 * - InterstitialAdManager: Interstitial ads
 * - RewardedAdManager: Rewarded ads
 * - AppOpenAdManager: App Open ads
 * 
 * This class maintains backward compatibility by delegating all calls
 * to the appropriate manager instances.
 */
public class AdmobHelper {
    private static volatile AdmobHelper mInstance = null;
    private static final Object sLock = new Object();
    private final String TAG = AdmobHelper.class.toString();
    
    // Ad Managers
    private final BannerAdManager bannerManager;
    private final MrecAdManager mrecManager;
    private final InterstitialAdManager interstitialManager;
    private final RewardedAdManager rewardedManager;
    private final AppOpenAdManager appOpenManager;
    
    // Shared callback
    private volatile IAdmobAdListener adsEventCallback;
    
    // Consent management
    private ConsentInformation consentInformation;
    private volatile int consentCode = -1;
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
    
    // Thread-safe singleton pattern
    public static AdmobHelper getInstance() {
        if (mInstance == null) {
            synchronized (sLock) {
                if (mInstance == null) {
                    mInstance = new AdmobHelper();
                }
            }
        }
        return mInstance;
    }
    
    private AdmobHelper() {
        // Initialize all managers
        bannerManager = new BannerAdManager();
        mrecManager = new MrecAdManager();
        interstitialManager = new InterstitialAdManager();
        rewardedManager = new RewardedAdManager();
        appOpenManager = new AppOpenAdManager();
    }
    
    /**
     * Initialize with app open ad ID (legacy method)
     * @param activity Current activity
     * @param adListener Ad event listener
     * @param args Array containing app open ad ID
     */
    public void init(Activity activity, IAdmobAdListener adListener, String[] args) {
        if (args != null && args.length > 0) {
            appOpenManager.init(activity, args[0]);
        }
    }
    
    /**
     * Set callback for all ad events
     * @param callback Ad event listener
     */
    public synchronized void setAdsCallback(IAdmobAdListener callback) {
        this.adsEventCallback = callback;
        
        // Set callback for all managers
        bannerManager.setCallback(callback);
        mrecManager.setCallback(callback);
        interstitialManager.setCallback(callback);
        rewardedManager.setCallback(callback);
        appOpenManager.setCallback(callback);
    }
    
    // ==================== BANNER AD METHODS ====================
    
    public void initBanner(Activity activity, String id, int position) {
        bannerManager.init(activity, id, position);
    }
    
    public void runAutoRefreshBanner(int refreshTime) {
        bannerManager.runAutoRefresh(refreshTime);
    }
    
    public void forceLoadBanner() {
        bannerManager.load(true);
    }
    
    public void stopRefresh() {
        bannerManager.stopRefresh();
    }
    
    public void showBanner() {
        bannerManager.show();
    }
    
    public void hideBanner() {
        bannerManager.hide();
    }
    
    public boolean isBannerLoaded() {
        return bannerManager.isLoaded();
    }
    
    // ==================== MREC AD METHODS ====================
    
    public void initMrec(Activity activity, String adUnitId, String position) {
        try {
            int positionCode = Integer.parseInt(position);
            mrecManager.init(activity, adUnitId, positionCode);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid position code: " + position, e);
        }
    }
    
    public void setMrecPosition(int positionCode, int offsetY) {
        mrecManager.setPosition(positionCode, offsetY);
    }
    
    public void loadMrec() {
        mrecManager.load();
    }
    
    public void showMrec() {
        mrecManager.show();
    }
    
    public void hideMrec() {
        mrecManager.hide();
    }
    
    public boolean isMrecLoaded() {
        return mrecManager.isLoaded();
    }
    
    // ==================== INTERSTITIAL AD METHODS ====================
    
    public void initInterstitial(Activity activity, String adUnitId) {
        interstitialManager.init(activity, adUnitId);
    }
    
    public void loadInterstitial() {
        interstitialManager.load();
    }
    
    public void showInterstitial(int interstitialCode) {
        interstitialManager.show(interstitialCode);
    }
    
    public boolean isInterstitialReady() {
        return interstitialManager.isReady();
    }
    
    public void disableInterstitial() {
        interstitialManager.disable();
    }
    
    public void enableInterstitial() {
        interstitialManager.enable();
    }
    
    public boolean isInterstitialEnabled() {
        return interstitialManager.isEnabled();
    }
    
    public void setInterstitialInterval(long intervalMs) {
        interstitialManager.setInterval(intervalMs);
    }
    
    public long getInterstitialInterval() {
        return interstitialManager.getInterval();
    }
    
    public long getRemainingInterstitialInterval() {
        return interstitialManager.getRemainingInterval();
    }
    
    public boolean canShowInterstitialByInterval() {
        return interstitialManager.canShowByInterval();
    }
    
    public void resetInterstitialInterval() {
        interstitialManager.resetInterval();
    }
    
    // ==================== REWARDED AD METHODS ====================
    
    public void initRewarded(Activity activity, String adUnitId) {
        rewardedManager.init(activity, adUnitId);
    }
    
    public void loadRewarded() {
        rewardedManager.load();
    }
    
    public void showRewarded() {
        rewardedManager.show();
    }
    
    public void showRewarded(int rewardCode) {
        rewardedManager.show(rewardCode);
    }
    
    public boolean isRewardedReady() {
        return rewardedManager.isReady();
    }
    
    // ==================== APP OPEN AD METHODS ====================
    
    public void loadAd(Activity activity) {
        appOpenManager.load();
    }
    
    public boolean isAdAvailable() {
        return appOpenManager.isAvailable();
    }
    
    public void showAppOpenAds(Activity activity) {
        appOpenManager.show();
    }
    
    public boolean canShowAOA() {
        return appOpenManager.canShow();
    }
    
    public void disableAOA() {
        appOpenManager.disable();
    }
    
    public void enableAOA() {
        appOpenManager.enable();
    }
    
    public void increaseBlockAOA() {
        appOpenManager.increaseBlock();
    }
    
    public void decreaseBlockAOA() {
        appOpenManager.decreaseBlock();
    }
    
    // ==================== CONSENT MANAGEMENT ====================
    
    public void bypassConsentFlow(Activity activity) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext());
        googleMobileAdsConsentManager.bypassConsentFlow();
        consentCode = 0;
        
        // Force init SDK
        initializeMobileAdsSdk(activity);
    }
    
    public int getConsentCode() {
        return consentCode;
    }
    
    public void startConsentFlow(Activity activity, IGoogleConsentCallback consentCallback) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext());
        googleMobileAdsConsentManager.gatherConsent(activity, consentError -> {
            if (consentError != null) {
                consentCode = 0;
                // Consent not obtained in current session.
                Log.w(TAG, String.format("%s: %s", consentError.getErrorCode(), consentError.getMessage()));
                if (consentCallback != null) {
                    consentCallback.onFinish(0);
                }
            } else {
                consentCode = 1;
                Log.d(TAG, "Consent Flow: FINISH----------------------");
                if (consentCallback != null) consentCallback.onFinish(1);
            }
            
            if (googleMobileAdsConsentManager.canRequestAds()) {
                initializeMobileAdsSdk(activity);
            }
        });
        
        // Attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk(activity);
        }
    }
    
    public boolean isPrivacySettingsButtonEnabled() {
        return consentInformation.getPrivacyOptionsRequirementStatus() == 
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }
    
    private void initializeMobileAdsSdk(Activity activity) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }
        
        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(activity.getApplicationContext(), initializationStatus -> {
            if (BuildConfig.DEBUG) {
                Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                for (String adapterClass : statusMap.keySet()) {
                    AdapterStatus status = statusMap.get(adapterClass);
                    assert status != null;
                    Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d", 
                        adapterClass, status.getDescription(), status.getLatency()));
                }
                
                MobileAds.openAdInspector(activity.getApplicationContext(), new OnAdInspectorClosedListener() {
                    @Override
                    public void onAdInspectorClosed(@Nullable AdInspectorError adInspectorError) {
                        // Ad inspector closed
                    }
                });
            }
            
            // Auto-load all ad types
            loadMrec();
            forceLoadBanner();
            loadInterstitial();
            loadRewarded();
            
            if (adsEventCallback != null) {
                adsEventCallback.onAdInitialized(null);
            }
        });
    }
    
    // ==================== LIFECYCLE METHODS ====================
    
    /**
     * Cleanup method to be called when the helper is no longer needed
     * Should be called in Activity's onDestroy()
     */
    public void cleanup() {
        synchronized (this) {
            // Cleanup all managers
            bannerManager.cleanup();
            mrecManager.cleanup();
            interstitialManager.cleanup();
            rewardedManager.cleanup();
            appOpenManager.cleanup();
            
            // Clear callback
            adsEventCallback = null;
            
            Log.d(TAG, "AdmobHelper cleaned up");
        }
    }
    
    /**
     * Pause ads when activity goes to background
     */
    public void onPause() {
        bannerManager.onPause();
        mrecManager.onPause();
    }
    
    /**
     * Resume ads when activity comes to foreground
     */
    public void onResume() {
        bannerManager.onResume();
        mrecManager.onResume();
    }
}

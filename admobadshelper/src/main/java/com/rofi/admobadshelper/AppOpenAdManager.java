package com.rofi.admobadshelper;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.appopen.AppOpenAd;

import com.rofi.base.ThreadUltils;

import java.util.Date;
import java.util.Objects;

/**
 * Manager for App Open Ads
 * Handles initialization, loading, showing with blocking mechanism
 */
public class AppOpenAdManager extends BaseAdManager {
    
    // App Open Ad instance
    private volatile AppOpenAd ad = null;
    
    // Configuration
    private String adUnitId;
    
    // State flags
    private volatile boolean isLoadingAd = false;
    private volatile boolean isShowingAd = false;
    private volatile long loadTime = 0;
    
    // Blocking mechanism
    private volatile boolean isDisableResumeAds = false;
    private volatile boolean aoaBlocker = false;
    private volatile int blockAOACount = 0;
    
    // Auto-show flag
    private volatile boolean needShowAOAAfterLoad = false;
    
    public AppOpenAdManager() {
        super("AppOpenAdManager");
    }
    
    /**
     * Initialize App Open Ad
     * @param activity Current activity context
     * @param adUnitId Ad unit ID for app open ad
     */
    public void init(Activity activity, String adUnitId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot init app open ad - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        this.adUnitId = adUnitId;
        Log.d(TAG, "App Open Ad initialized with ad unit ID: " + adUnitId);
    }
    
    /**
     * Load app open ad
     */
    public void load() {
        // Thread-safe check with synchronization
        synchronized (this) {
            if (adUnitId == null || isLoadingAd || isAvailable()) {
                return;
            }
            isLoadingAd = true;
        }
        
        Log.d(TAG, "Start Load ads.");
        AdRequest request = new AdRequest.Builder().build();
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            synchronized (this) {
                isLoadingAd = false;
            }
            return;
        }
        
        AppOpenAd.load(currentActivity.getApplicationContext(), adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(AppOpenAd ads) {
                Log.d(TAG, "App Open Ads was loaded.");
                synchronized (AppOpenAdManager.this) {
                    isLoadingAd = false;
                    ad = ads;
                    loadTime = (new Date()).getTime();
                }
                
                ad.setOnPaidEventListener(new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(AdValue adValue) {
                        // Get the ad unit ID.
                        String adUnitId = ad.getAdUnitId();
                        
                        AdapterResponseInfo loadedAdapterResponseInfo = ad.getResponseInfo().getLoadedAdapterResponseInfo();
                        String adSourceName = "admob";
                        if (loadedAdapterResponseInfo != null) {
                            adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                            Log.d(TAG, "App Open Ads loadedAdapterResponseInfo\nadSourceName: " + adSourceName);
                        }
                        
                        IAdmobAdListener cb = callback;
                        if (cb != null) {
                            cb.onAdImpression("App open", adUnitId, adSourceName, 
                                (double) adValue.getValueMicros() / 1000000);
                        }
                    }
                });
                
                Log.d(TAG, "Banner adapter class name: " + Objects.requireNonNull(ad.getResponseInfo()).getMediationAdapterClassName());
                
                if (needShowAOAAfterLoad) {
                    show();
                    needShowAOAAfterLoad = false;
                }
            }
            
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.d(TAG, "App open ad has failed to load.");
                isLoadingAd = false;
                IAdmobAdListener cb = callback;
                if (cb != null) {
                    cb.onAOAFailedToLoad();
                }
            }
        });
    }
    
    /**
     * Check if ad exists and can be shown.
     */
    public boolean isAvailable() {
        return ad != null && wasLoadTimeLessThanNHoursAgo(1);
    }
    
    /**
     * Utility method to check if ad was loaded more than n hours ago.
     */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }
    
    /**
     * Show app open ad
     */
    public void show() {
        if (isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.");
            return;
        }
        
        if (!isAvailable()) {
            Log.d(TAG, "The app open ad is not ready");
            load();
            return;
        }
        
        if (aoaBlocker) {
            Log.d(TAG, "AOA IS BLOCKED!");
            return;
        }
        
        if (blockAOACount > 0) {
            Log.d(TAG, "AOA IS BLOCKED! (COUNT SHOW AOA: " + blockAOACount + ")");
            blockAOACount -= 1;
            return;
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot show app open ad - no valid activity");
            return;
        }
        
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                IAdmobAdListener cb = callback;
                if (cb != null) {
                    cb.onAdClicked();
                }
            }
            
            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                // Set the reference to null so isAvailable() returns false.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                ad = null;
                isShowingAd = false;
                IAdmobAdListener cb = callback;
                if (cb != null) {
                    cb.onAdDismissedFullScreenContent(0);
                }
                load();
                
                ThreadUltils.startTask(() -> {
                    Log.d(TAG, "Reset AOA Time-Block");
                    aoaBlocker = false;
                }, 1 * 1000L);
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when fullscreen content failed to show.
                // Set the reference to null so isAvailable() returns false.
                Log.d(TAG, adError.getMessage());
                ad = null;
                isShowingAd = false;
                load();
            }
            
            @Override
            public void onAdImpression() {
                // Impression recorded
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
                aoaBlocker = true;
                IAdmobAdListener cb = callback;
                if (cb != null) {
                    cb.onAdDisplayFullScreenContent(0);
                }
            }
        });
        
        isShowingAd = true;
        ad.show(currentActivity);
    }
    
    /**
     * Check if AOA can be shown (not disabled and not blocked)
     */
    public boolean canShow() {
        if (isDisableResumeAds || blockAOACount > 0) return false;
        return true;
    }
    
    /**
     * Disable AOA
     */
    public void disable() {
        isDisableResumeAds = true;
    }
    
    /**
     * Enable AOA
     */
    public void enable() {
        isDisableResumeAds = false;
    }
    
    /**
     * Increase block counter
     */
    public synchronized void increaseBlock() {
        blockAOACount += 1;
    }
    
    /**
     * Decrease block counter
     */
    public synchronized void decreaseBlock() {
        blockAOACount -= 1;
        if (blockAOACount < 0) blockAOACount = 0;
    }
    
    @Override
    protected void scheduleReload() {
        // App Open Ads don't use auto-reload with exponential backoff
        // They are loaded on-demand
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        synchronized (this) {
            // Clear ad reference
            ad = null;
            
            // Reset states
            isLoadingAd = false;
            isShowingAd = false;
            isDisableResumeAds = false;
            aoaBlocker = false;
            blockAOACount = 0;
            
            Log.d(TAG, "App Open Ad manager cleaned up");
        }
    }
}

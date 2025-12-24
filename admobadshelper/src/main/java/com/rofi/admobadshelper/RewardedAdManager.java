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
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Manager for Rewarded Ads
 * Handles initialization, loading, showing with reward callbacks
 */
public class RewardedAdManager extends BaseAdManager {
    
    // Rewarded Ad instance
    private volatile RewardedAd ad;
    
    // Configuration
    private String adUnitId;
    
    // State flags
    private volatile boolean isLoading = false;
    private volatile boolean isLoaded = false;
    
    // Current reward code for callback
    private volatile int currentRewardCode = 0;
    
    public RewardedAdManager() {
        super("RewardedAdManager");
    }
    
    /**
     * Initialize Rewarded Ad
     * @param activity Current activity context
     * @param adUnitId Ad unit ID for rewarded ad
     */
    public void init(Activity activity, String adUnitId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot init rewarded - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        this.adUnitId = adUnitId;
        Log.d(TAG, "Rewarded initialized with ad unit ID: " + adUnitId);
    }
    
    /**
     * Load rewarded ad
     * Follows Google's best practice: check if ad is already loaded before loading
     */
    public void load() {
        if (adUnitId == null) {
            Log.w(TAG, "Rewarded ad unit ID is not set. Call init() first.");
            return;
        }
        
        synchronized (this) {
            // Don't reload if already loaded or currently loading
            if (isLoading || isLoaded) {
                Log.d(TAG, "Rewarded ad is already loading or loaded");
                return;
            }
            isLoading = true;
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot load rewarded - no valid activity");
            synchronized (this) {
                isLoading = false;
            }
            return;
        }
        
        runSafelyOnUiThread(currentActivity, () -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            RewardedAd.load(
                currentActivity,
                adUnitId,
                adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd loadedAd) {
                        Log.d(TAG, "Rewarded ad loaded");
                        synchronized (RewardedAdManager.this) {
                            isLoading = false;
                            isLoaded = true;
                        }
                        setAd(loadedAd);
                        
                        // Reset retry counter on successful load
                        resetRetryCounter();
                        
                        // Set OnPaidEventListener
                        loadedAd.setOnPaidEventListener(adValue -> {
                            RewardedAd rewarded = getAd();
                            if (rewarded == null) return;
                            
                            ResponseInfo responseInfo = rewarded.getResponseInfo();
                            String adSourceName = "admob";
                            if (responseInfo != null) {
                                AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
                                if (loadedAdapterResponseInfo != null) {
                                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                                    Log.d(TAG, "REWARDED loadedAdapterResponseInfo\nadSourceName: " + adSourceName);
                                }
                            }
                            
                            IAdmobAdListener cb = callback;
                            if (cb != null) {
                                cb.onAdImpression("REWARDED", adUnitId, adSourceName, 
                                    (double) adValue.getValueMicros() / 1000000);
                            }
                        });
                        
                        IAdmobAdListener cb = callback;
                        if (cb != null) {
                            cb.onRewardedLoaded();
                        }
                        
                        ResponseInfo responseInfo = loadedAd.getResponseInfo();
                        if (responseInfo != null) {
                            Log.d(TAG, "REWARDED adapter class name: " + responseInfo.getMediationAdapterClassName());
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Failed to load rewarded: " + loadAdError.getMessage());
                        synchronized (RewardedAdManager.this) {
                            isLoading = false;
                            isLoaded = false;
                        }
                        setAd(null);
                        
                        // Schedule auto-reload with exponential backoff
                        scheduleReload();
                    }
                }
            );
        });
    }
    
    /**
     * Show rewarded ad with reward listener (backward compatibility)
     * User must complete the ad to receive reward
     * Follows Google's best practice: check if ad is ready before showing
     */
    public void show() {
        show(0); // Default reward code
    }
    
    /**
     * Show rewarded ad with reward listener and custom reward code
     * User must complete the ad to receive reward
     * Follows Google's best practice: check if ad is ready before showing
     * Thread-safe implementation with double-check pattern
     * @param rewardCode Custom code to identify reward type, will be passed back in onUserEarnedReward
     */
    public void show(int rewardCode) {
        // Thread-safe check with synchronized block
        RewardedAd rewarded;
        synchronized (this) {
            rewarded = ad;
            if (rewarded == null || !isLoaded) {
                Log.d(TAG, "Rewarded ad is not ready yet");
                // Preload for next time
                load();
                return;
            }
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot show rewarded - no valid activity");
            return;
        }
        
        // Store reward code for callback
        currentRewardCode = rewardCode;
        
        runSafelyOnUiThread(currentActivity, () -> {
            // Double-check in UI thread with synchronized access
            RewardedAd rewardedAd;
            synchronized (RewardedAdManager.this) {
                rewardedAd = ad;
                if (rewardedAd == null) {
                    Log.d(TAG, "Rewarded ad is null in UI thread");
                    return;
                }
            }
            
            // Set FullScreenContentCallback
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed");
                    IAdmobAdListener cb = callback;
                    if (cb != null) {
                        cb.onAdDisplayFullScreenContent(2); // 2 for rewarded
                    }
                }
                
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed");
                    // Clear reference in synchronized block
                    setAd(null); // Single-use: this also sets isLoaded = false
                    
                    IAdmobAdListener cb = callback;
                    if (cb != null) {
                        cb.onAdDismissedFullScreenContent(2); // 2 for rewarded
                    }
                    
                    // Preload next rewarded ad
                    load();
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Rewarded failed to show: " + adError.getMessage());
                    // Clear reference in synchronized block
                    setAd(null); // This also sets isLoaded = false
                    
                    // Try to load again
                    load();
                }
                
                @Override
                public void onAdImpression() {
                    Log.d(TAG, "Rewarded impression recorded");
                }
                
                @Override
                public void onAdClicked() {
                    Log.d(TAG, "Rewarded clicked");
                    IAdmobAdListener cb = callback;
                    if (cb != null) {
                        cb.onAdClicked();
                    }
                }
            });
            
            // Show the ad with reward listener
            rewardedAd.show(currentActivity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // User earned the reward
                    int amount = rewardItem.getAmount();
                    String type = rewardItem.getType();
                    Log.d(TAG, "User earned reward (code: " + currentRewardCode + "): " + amount + " " + type);
                    
                    IAdmobAdListener cb = callback;
                    if (cb != null) {
                        cb.onUserEarnedReward(currentRewardCode, type, amount);
                    }
                }
            });
        });
    }
    
    /**
     * Check if rewarded ad is ready to show
     * Thread-safe check with synchronized access
     * @return true if ad is loaded and ready
     */
    public synchronized boolean isReady() {
        return isLoaded && ad != null;
    }
    
    @Override
    protected void scheduleReload() {
        synchronized (this) {
            retryCount++;
        }
        
        long delay = calculateRetryDelay(retryCount);
        Log.d(TAG, "Scheduling rewarded reload. Attempt: " + retryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener cb = callback;
        if (cb != null) {
            cb.onRewardedRetrying(retryCount, delay);
        }
        
        retryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying rewarded load (attempt " + retryCount + ")");
            load();
        }, delay);
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        synchronized (this) {
            // Clear ad reference
            setAd(null);
            
            // Reset states
            isLoading = false;
            isLoaded = false;
            
            Log.d(TAG, "Rewarded manager cleaned up");
        }
    }
    
    // Helper methods
    
    private synchronized RewardedAd getAd() {
        return ad;
    }
    
    private synchronized void setAd(RewardedAd ad) {
        this.ad = ad;
        this.isLoaded = (ad != null);
    }
}

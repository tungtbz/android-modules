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
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Manager for Interstitial Ads
 * Handles initialization, loading, showing with interval control
 */
public class InterstitialAdManager extends BaseAdManager {
    
    // Interstitial Ad instance
    private volatile InterstitialAd ad;
    
    // Configuration
    private String adUnitId;
    
    // State flags
    private volatile boolean isLoading = false;
    private volatile boolean isLoaded = false;
    private volatile boolean isEnabled = true; // Control interstitial ad display
    
    // Interval control
    private volatile long lastDismissedTime = 0; // Track when last interstitial was dismissed
    private volatile long intervalMs = 0; // Minimum interval between interstitials (0 = no limit)
    
    // Current code for callback
    private volatile int currentCode = 0;
    
    public InterstitialAdManager() {
        super("InterstitialAdManager");
    }
    
    /**
     * Initialize Interstitial Ad
     * @param activity Current activity context
     * @param adUnitId Ad unit ID for interstitial ad
     */
    public void init(Activity activity, String adUnitId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot init interstitial - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        this.adUnitId = adUnitId;
        Log.d(TAG, "Interstitial initialized with ad unit ID: " + adUnitId);
    }
    
    /**
     * Load interstitial ad
     * Follows Google's best practice: check if ad is already loaded before loading
     */
    public void load() {
        if (adUnitId == null) {
            Log.w(TAG, "Interstitial ad unit ID is not set. Call init() first.");
            return;
        }
        
        synchronized (this) {
            // Don't reload if already loaded or currently loading
            if (isLoading || isLoaded) {
                Log.d(TAG, "Interstitial ad is already loading or loaded");
                return;
            }
            isLoading = true;
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot load interstitial - no valid activity");
            synchronized (this) {
                isLoading = false;
            }
            return;
        }
        
        runSafelyOnUiThread(currentActivity, () -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            InterstitialAd.load(
                currentActivity,
                adUnitId,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd loadedAd) {
                        Log.d(TAG, "Interstitial ad loaded");
                        synchronized (InterstitialAdManager.this) {
                            isLoading = false;
                            isLoaded = true;
                        }
                        setAd(loadedAd);
                        
                        // Reset retry counter on successful load
                        resetRetryCounter();
                        
                        // Set OnPaidEventListener
                        loadedAd.setOnPaidEventListener(adValue -> {
                            InterstitialAd interstitial = getAd();
                            if (interstitial == null) return;
                            
                            ResponseInfo responseInfo = interstitial.getResponseInfo();
                            String adSourceName = "admob";
                            if (responseInfo != null) {
                                AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
                                if (loadedAdapterResponseInfo != null) {
                                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                                    Log.d(TAG, "INTERSTITIAL loadedAdapterResponseInfo\nadSourceName: " + adSourceName);
                                }
                            }
                            
                            IAdmobAdListener cb = callback;
                            if (cb != null) {
                                cb.onAdImpression("INTERSTITIAL", adUnitId, adSourceName, 
                                    (double) adValue.getValueMicros() / 1000000);
                            }
                        });
                        
                        IAdmobAdListener cb = callback;
                        if (cb != null) {
                            cb.onInterstitialLoaded();
                        }
                        
                        ResponseInfo responseInfo = loadedAd.getResponseInfo();
                        if (responseInfo != null) {
                            Log.d(TAG, "INTERSTITIAL adapter class name: " + responseInfo.getMediationAdapterClassName());
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Failed to load interstitial: " + loadAdError.getMessage());
                        synchronized (InterstitialAdManager.this) {
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
     * Show interstitial ad
     * Follows Google's best practice: check if ad is ready before showing
     * Thread-safe implementation with double-check pattern
     * @param code Custom code to identify this interstitial, will be passed back in callback
     */
    public void show(int code) {
        if (!isEnabled) {
            Log.d(TAG, "Interstitial ads are disabled, cannot show");
            return;
        }
        
        // Check interval requirement
        if (intervalMs > 0 && lastDismissedTime > 0) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastDismissed = currentTime - lastDismissedTime;
            
            if (timeSinceLastDismissed < intervalMs) {
                long remainingMs = intervalMs - timeSinceLastDismissed;
                Log.d(TAG, "Interstitial interval not met. Remaining time: " + (remainingMs / 1000) + " seconds");
                return;
            }
        }
        
        // Thread-safe check with synchronized block
        InterstitialAd interstitial;
        synchronized (this) {
            interstitial = ad;
            if (interstitial == null || !isLoaded) {
                Log.d(TAG, "Interstitial ad is not ready yet");
                // Preload for next time
                load();
                return;
            }
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(TAG, "Cannot show interstitial - no valid activity");
            return;
        }
        
        // Store code for callback
        currentCode = code;
        
        runSafelyOnUiThread(currentActivity, () -> {
            // Double-check in UI thread with synchronized access
            InterstitialAd interstitialAd;
            synchronized (InterstitialAdManager.this) {
                interstitialAd = ad;
                if (interstitialAd == null) {
                    Log.d(TAG, "Interstitial ad is null in UI thread");
                    return;
                }
            }
            
            // Set FullScreenContentCallback
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed");
                    IAdmobAdListener cb = callback;
                    if (cb != null) {
                        cb.onAdDisplayFullScreenContent(1); // 1 for interstitial
                    }
                }
                
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed with code: " + currentCode);
                    
                    // Record dismiss time for interval tracking
                    lastDismissedTime = System.currentTimeMillis();
                    
                    // Clear reference in synchronized block
                    setAd(null); // This also sets isLoaded = false
                    
                    IAdmobAdListener cb = callback;
                    if (cb != null) {
                        cb.onAdDismissedFullScreenContent(1); // 1 for interstitial
                        cb.onInterstitialDismissed(currentCode); // Pass interstitial code
                    }
                    
                    // Preload next interstitial
                    load();
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Interstitial failed to show: " + adError.getMessage());
                    // Clear reference in synchronized block
                    setAd(null); // This also sets isLoaded = false
                    
                    // Try to load again
                    load();
                }
                
                @Override
                public void onAdImpression() {
                    Log.d(TAG, "Interstitial impression recorded");
                }
                
                @Override
                public void onAdClicked() {
                    Log.d(TAG, "Interstitial clicked");
                    IAdmobAdListener cb = callback;
                    if (cb != null) {
                        cb.onAdClicked();
                    }
                }
            });
            
            // Show the ad
            interstitialAd.show(currentActivity);
        });
    }
    
    /**
     * Check if interstitial ad is ready to show
     * Thread-safe check with synchronized access
     * @return true if ad is loaded and ready
     */
    public synchronized boolean isReady() {
        return isLoaded && ad != null;
    }
    
    /**
     * Disable interstitial ads
     * When disabled, interstitial ads will not load or show
     */
    public void disable() {
        isEnabled = false;
        Log.d(TAG, "Interstitial ads disabled");
    }
    
    /**
     * Enable interstitial ads
     * When enabled, interstitial ads can load and show normally
     */
    public void enable() {
        isEnabled = true;
        Log.d(TAG, "Interstitial ads enabled");
    }
    
    /**
     * Check if interstitial ads are currently enabled
     * @return true if enabled, false if disabled
     */
    public boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * Set minimum interval between interstitial ads
     * The interval is measured from when the previous ad was dismissed
     * @param intervalMs Minimum interval in milliseconds (0 = no interval limit)
     */
    public void setInterval(long intervalMs) {
        this.intervalMs = intervalMs;
        Log.d(TAG, "Interstitial interval set to: " + (intervalMs / 1000) + " seconds");
    }
    
    /**
     * Get current interstitial interval setting
     * @return Current interval in milliseconds
     */
    public long getInterval() {
        return intervalMs;
    }
    
    /**
     * Get remaining time until next interstitial can be shown
     * @return Remaining time in milliseconds (0 if can show now)
     */
    public long getRemainingInterval() {
        if (intervalMs <= 0 || lastDismissedTime <= 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastDismissed = currentTime - lastDismissedTime;
        long remaining = intervalMs - timeSinceLastDismissed;
        
        return remaining > 0 ? remaining : 0;
    }
    
    /**
     * Check if interstitial can be shown based on interval
     * @return true if interval requirement is met or no interval is set
     */
    public boolean canShowByInterval() {
        if (intervalMs <= 0) {
            return true; // No interval limit
        }
        
        if (lastDismissedTime <= 0) {
            return true; // No previous ad shown
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastDismissed = currentTime - lastDismissedTime;
        
        return timeSinceLastDismissed >= intervalMs;
    }
    
    /**
     * Reset interstitial interval timer
     * Useful when you want to manually reset the timer (e.g., after level complete)
     */
    public void resetInterval() {
        lastDismissedTime = 0;
        Log.d(TAG, "Interstitial interval timer reset");
    }
    
    @Override
    protected void scheduleReload() {
        synchronized (this) {
            retryCount++;
        }
        
        long delay = calculateRetryDelay(retryCount);
        Log.d(TAG, "Scheduling interstitial reload. Attempt: " + retryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener cb = callback;
        if (cb != null) {
            cb.onInterstitialRetrying(retryCount, delay);
        }
        
        retryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying interstitial load (attempt " + retryCount + ")");
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
            
            Log.d(TAG, "Interstitial manager cleaned up");
        }
    }
    
    // Helper methods
    
    private synchronized InterstitialAd getAd() {
        return ad;
    }
    
    private synchronized void setAd(InterstitialAd ad) {
        this.ad = ad;
        this.isLoaded = (ad != null);
    }
}

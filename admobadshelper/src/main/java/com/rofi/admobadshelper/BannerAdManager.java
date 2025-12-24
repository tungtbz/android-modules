package com.rofi.admobadshelper;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;
import com.rofi.base.Constants;

import java.lang.ref.WeakReference;

/**
 * Manager for Banner/Collapsible Banner Ads
 * Handles initialization, loading, showing/hiding, and auto-refresh
 */
public class BannerAdManager extends BaseAdManager {
    
    // Banner AdView reference
    private volatile WeakReference<AdView> adViewRef;
    
    // Banner configuration
    private String adUnitId;
    private int position;
    
    // State flags
    private volatile boolean isLoading = false;
    private volatile boolean isLoaded = false;
    private volatile boolean logicallyVisible = false;
    
    // Auto-refresh handler
    private final Handler refreshHandler;
    
    public BannerAdManager() {
        super("BannerAdManager");
        this.adViewRef = new WeakReference<>(null);
        this.refreshHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Initialize banner ad
     * @param activity Current activity
     * @param adUnitId Ad unit ID
     * @param position Banner position (Constants.POSITION_*)
     */
    public void init(Activity activity, String adUnitId, int position) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot init banner - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        this.adUnitId = adUnitId;
        this.position = position;
        
        AdView bannerView = new AdView(activity);
        bannerView.setAdSize(getBannerAdSize(activity));
        bannerView.setAdUnitId(adUnitId);
        bannerView.setVisibility(View.GONE);
        
        // Set paid event listener
        bannerView.setOnPaidEventListener(adValue -> {
            AdView banner = getAdView();
            if (banner == null) return;
            
            ResponseInfo responseInfo = banner.getResponseInfo();
            String adSourceName = "admob";
            if (responseInfo != null) {
                AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
                if (loadedAdapterResponseInfo != null) {
                    adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                    Log.d(TAG, "BANNER loadedAdapterResponseInfo\nadSourceName: " + adSourceName);
                }
            }
            
            // Notify callback
            IAdmobAdListener cb = callback;
            if (cb != null) {
                cb.onAdImpression("COLLAPSIBLE_BANNER", adUnitId, adSourceName, 
                    (double) adValue.getValueMicros() / 1000000);
            }
        });
        
        // Set ad listener
        bannerView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.d(TAG, "BANNER onAdFailedToLoad: " + loadAdError.getMessage());
                synchronized (BannerAdManager.this) {
                    isLoading = false;
                }
                
                // Schedule auto-reload with exponential backoff
                scheduleReload();
            }
            
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                synchronized (BannerAdManager.this) {
                    isLoading = false;
                    isLoaded = true;
                }
                
                // Reset retry counter on successful load
                resetRetryCounter();
                
                Log.d(TAG, "BANNER onAdLoaded");
                
                // Check logical state and apply visibility accordingly
                AdView banner = getAdView();
                if (banner != null) {
                    if (logicallyVisible) {
                        Log.d(TAG, "BANNER onAdLoaded - logical state is SHOW, displaying ad");
                        banner.resume();
                        banner.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "BANNER onAdLoaded - logical state is HIDE, keeping ad hidden");
                        banner.setVisibility(View.GONE);
                    }
                }
                
                IAdmobAdListener cb = callback;
                if (cb != null) {
                    cb.onBannerLoaded();
                }
            }
            
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                IAdmobAdListener cb = callback;
                if (cb != null) {
                    cb.onAdClicked();
                }
            }
            
            @Override
            public void onAdOpened() {
                Log.d(TAG, "BANNER onAdOpened");
            }
        });
        
        // Set layout params and add to view hierarchy
        int gravity = position == Constants.POSITION_CENTER_TOP ? 
            Gravity.CENTER_HORIZONTAL | Gravity.TOP : 
            Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            gravity);
        layoutParams.setMargins(0, 0, 0, 0);
        bannerView.setLayoutParams(layoutParams);
        
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(bannerView);
        
        // Store in WeakReference to prevent memory leak
        setAdView(bannerView);
    }
    
    /**
     * Load banner ad
     * @param forceLoad Force load even if already loading/loaded
     */
    public void load(boolean forceLoad) {
        AdView bannerView = getAdView();
        if (adUnitId == null || bannerView == null) {
            synchronized (this) {
                isLoading = false;
            }
            return;
        }
        
        synchronized (this) {
            if (!forceLoad && isLoading) return;
            if (!forceLoad && isLoaded) return;
            isLoading = true;
        }
        
        // Ensure UI operations run on main thread
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView banner = getAdView();
                if (banner == null) {
                    synchronized (this) {
                        isLoading = false;
                    }
                    Log.w(TAG, "Banner view was garbage collected");
                    return;
                }
                
                // Create collapsible banner extras
                Bundle extras = new Bundle();
                extras.putString("collapsible", 
                    position == Constants.POSITION_CENTER_TOP ? "top" : "bottom");
                
                AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();
                
                banner.loadAd(adRequest);
                Log.d(TAG, "Loading Banner");
            });
        } else {
            synchronized (this) {
                isLoading = false;
            }
        }
    }
    
    /**
     * Show banner ad
     */
    public void show() {
        // Set logical state to visible
        logicallyVisible = true;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView banner = getAdView();
                if (banner != null && isLoaded && banner.getVisibility() == View.GONE) {
                    Log.d(TAG, "showBanner - ad loaded, showing now");
                    banner.resume();
                    banner.setVisibility(View.VISIBLE);
                } else if (banner != null && !isLoaded) {
                    Log.d(TAG, "showBanner - ad not loaded yet, will auto-show when loaded");
                }
            });
        }
    }
    
    /**
     * Hide banner ad
     */
    public void hide() {
        // Set logical state to hidden
        logicallyVisible = false;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView banner = getAdView();
                if (banner != null && banner.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "hideBanner - hiding now");
                    banner.pause();
                    banner.setVisibility(View.GONE);
                } else if (banner != null && !isLoaded) {
                    Log.d(TAG, "hideBanner - ad not loaded yet, will stay hidden when loaded");
                }
            });
        }
    }
    
    /**
     * Start auto-refresh banner
     * @param refreshTimeSeconds Refresh interval in seconds
     */
    public void runAutoRefresh(int refreshTimeSeconds) {
        refreshHandler.removeCallbacksAndMessages(null);
        
        Runnable refreshRunnable = new Runnable() {
            public void run() {
                Activity currentActivity = getCurrentActivity();
                if (currentActivity != null) {
                    runSafelyOnUiThread(currentActivity, () -> {
                        Log.d(TAG, "Auto-refresh banner, interval: " + refreshTimeSeconds + "s");
                        load(false);
                    });
                } else {
                    Log.w(TAG, "Cannot refresh banner - no valid activity");
                }
                
                refreshHandler.postDelayed(this, refreshTimeSeconds * 1000L);
            }
        };
        
        refreshHandler.postDelayed(refreshRunnable, 1);
    }
    
    /**
     * Stop auto-refresh
     */
    public void stopRefresh() {
        refreshHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Check if banner is loaded
     * @return true if loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    @Override
    protected void scheduleReload() {
        synchronized (this) {
            retryCount++;
        }
        
        long delay = calculateRetryDelay(retryCount);
        Log.d(TAG, "Scheduling banner reload. Attempt: " + retryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener cb = callback;
        if (cb != null) {
            cb.onBannerRetrying(retryCount, delay);
        }
        
        retryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying banner load (attempt " + retryCount + ")");
            load(true);
        }, delay);
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        synchronized (this) {
            // Stop refresh
            refreshHandler.removeCallbacksAndMessages(null);
            
            // Destroy AdView
            AdView banner = getAdView();
            if (banner != null) {
                banner.destroy();
            }
            
            // Clear references
            setAdView(null);
            
            // Reset states
            isLoading = false;
            isLoaded = false;
            logicallyVisible = false;
            
            Log.d(TAG, "Banner manager cleaned up");
        }
    }
    
    @Override
    public void onPause() {
        AdView banner = getAdView();
        if (banner != null) {
            banner.pause();
        }
    }
    
    @Override
    public void onResume() {
        AdView banner = getAdView();
        if (banner != null) {
            banner.resume();
        }
    }
    
    // Helper methods
    
    private AdView getAdView() {
        return adViewRef != null ? adViewRef.get() : null;
    }
    
    private void setAdView(AdView adView) {
        if (adView != null) {
            adViewRef = new WeakReference<>(adView);
        } else {
            adViewRef = new WeakReference<>(null);
        }
    }
    
    @SuppressWarnings("deprecation")
    private AdSize getBannerAdSize(Activity activity) {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        
        float density = outMetrics.density;
        float adWidthPixels = outMetrics.widthPixels;
        int adWidth = (int) (adWidthPixels / density);
        
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            activity.getApplicationContext(), adWidth);
    }
}

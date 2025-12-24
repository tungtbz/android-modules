package com.rofi.admobadshelper;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Insets;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;

import java.lang.ref.WeakReference;

/**
 * Manager for MREC (Medium Rectangle) Ads
 * Handles initialization, loading, showing/hiding, and positioning
 */
public class MrecAdManager extends BaseAdManager {
    
    // MREC AdView reference
    private volatile WeakReference<AdView> adViewRef;
    
    // MREC configuration
    private String adUnitId;
    
    // State flags
    private volatile boolean isLoading = false;
    private volatile boolean isLoaded = false;
    private volatile boolean logicallyVisible = false;
    
    public MrecAdManager() {
        super("MrecAdManager");
        this.adViewRef = new WeakReference<>(null);
    }
    
    /**
     * Initialize MREC ad
     * @param activity Current activity
     * @param adUnitId Ad unit ID
     * @param positionCode Position code (AdmobHelper.POSITION_*)
     */
    public void init(Activity activity, String adUnitId, int positionCode) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Cannot create MREC - invalid activity");
            return;
        }
        
        setCurrentActivity(activity);
        this.adUnitId = adUnitId;
        
        AdView mrecView = new AdView(activity);
        mrecView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        mrecView.setAdUnitId(adUnitId);
        mrecView.setVisibility(View.GONE);
        mrecView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        activity.addContentView(mrecView, getLayoutParams(positionCode, 0));
        
        // Set paid event listener
        mrecView.setOnPaidEventListener(adValue -> {
            AdView mrec = getAdView();
            if (mrec == null) return;
            
            ResponseInfo responseInfo = mrec.getResponseInfo();
            if (responseInfo == null) return;
            
            AdapterResponseInfo loadedAdapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
            String adSourceName = "admob";
            if (loadedAdapterResponseInfo != null) {
                adSourceName = loadedAdapterResponseInfo.getAdSourceName();
                Log.d(TAG, "MREC loadedAdapterResponseInfo\nadSourceName: " + adSourceName);
            }
            
            // Notify callback
            IAdmobAdListener cb = callback;
            if (cb != null) {
                cb.onAdImpression("MREC", adUnitId, adSourceName, 
                    (double) adValue.getValueMicros() / 1000000);
            }
        });
        
        // Set ad listener
        mrecView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                synchronized (MrecAdManager.this) {
                    isLoading = false;
                    isLoaded = true;
                }
                
                // Reset retry counter on successful load
                resetRetryCounter();
                
                AdView mrec = getAdView();
                if (mrec != null && mrec.getResponseInfo() != null) {
                    Log.d(TAG, "MREC adapter class name: " + 
                        mrec.getResponseInfo().getMediationAdapterClassName());
                }
                
                // Check logical state and apply visibility accordingly
                if (mrec != null) {
                    if (logicallyVisible) {
                        Log.d(TAG, "MREC onAdLoaded - logical state is SHOW, displaying ad");
                        mrec.setVisibility(View.VISIBLE);
                        mrec.resume();
                    } else {
                        Log.d(TAG, "MREC onAdLoaded - logical state is HIDE, keeping ad hidden");
                        mrec.setVisibility(View.GONE);
                    }
                }
                
                IAdmobAdListener cb = callback;
                if (cb != null) {
                    cb.onMrecLoaded();
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
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                synchronized (MrecAdManager.this) {
                    isLoading = false;
                }
                Log.d(TAG, "MREC onAdFailedToLoad\nloadAdError: " + loadAdError.getMessage());
                
                // Schedule auto-reload with exponential backoff
                scheduleReload();
            }
        });
        
        // Store in WeakReference to prevent memory leak
        setAdView(mrecView);
    }
    
    /**
     * Load MREC ad
     */
    public void load() {
        synchronized (this) {
            if (isLoading || isLoaded) return;
            isLoading = true;
        }
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView mrec = getAdView();
                if (mrec != null) {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    mrec.loadAd(adRequest);
                } else {
                    Log.d(TAG, "loadMrec: mrecAdView is null");
                    synchronized (this) {
                        isLoading = false;
                    }
                }
            });
        } else {
            Log.w(TAG, "Cannot load MREC - no valid activity");
            synchronized (this) {
                isLoading = false;
            }
        }
    }
    
    /**
     * Show MREC ad
     */
    public void show() {
        // Set logical state to visible
        logicallyVisible = true;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView mrec = getAdView();
                if (mrec != null && isLoaded && mrec.getVisibility() == View.GONE) {
                    Log.d(TAG, "showMrec - ad loaded, showing now");
                    mrec.setVisibility(View.VISIBLE);
                    mrec.resume();
                } else if (mrec != null && !isLoaded) {
                    Log.d(TAG, "showMrec - ad not loaded yet, will auto-show when loaded");
                }
            });
        }
    }
    
    /**
     * Hide MREC ad
     */
    public void hide() {
        // Set logical state to hidden
        logicallyVisible = false;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView mrec = getAdView();
                if (mrec != null && mrec.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "hideMrec - hiding now");
                    mrec.setVisibility(View.GONE);
                    mrec.pause();
                } else if (mrec != null && !isLoaded) {
                    Log.d(TAG, "hideMrec - ad not loaded yet, will stay hidden when loaded");
                }
            });
        }
    }
    
    /**
     * Update MREC position
     * @param positionCode Position code
     * @param topPadding Top padding in dp
     */
    public void setPosition(int positionCode, int topPadding) {
        AdView mrec = getAdView();
        if (mrec == null) return;
        
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            runSafelyOnUiThread(currentActivity, () -> {
                AdView mrecView = getAdView();
                if (mrecView != null) {
                    FrameLayout.LayoutParams layoutParams = getLayoutParams(positionCode, topPadding);
                    mrecView.setLayoutParams(layoutParams);
                }
            });
        }
    }
    
    /**
     * Check if MREC is loaded
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
        Log.d(TAG, "Scheduling MREC reload. Attempt: " + retryCount + ", Delay: " + (delay / 1000) + "s");
        
        // Notify callback about retry
        IAdmobAdListener cb = callback;
        if (cb != null) {
            cb.onMrecRetrying(retryCount, delay);
        }
        
        retryHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying MREC load (attempt " + retryCount + ")");
            load();
        }, delay);
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        synchronized (this) {
            // Destroy AdView
            AdView mrec = getAdView();
            if (mrec != null) {
                mrec.destroy();
            }
            
            // Clear references
            setAdView(null);
            
            // Reset states
            isLoading = false;
            isLoaded = false;
            logicallyVisible = false;
            
            Log.d(TAG, "MREC manager cleaned up");
        }
    }
    
    @Override
    public void onPause() {
        AdView mrec = getAdView();
        if (mrec != null) {
            mrec.pause();
        }
    }
    
    @Override
    public void onResume() {
        AdView mrec = getAdView();
        if (mrec != null) {
            mrec.resume();
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
    
    protected static class Insets {
        int left;
        int top;
        int right;
        int bottom;
    }
    
    protected FrameLayout.LayoutParams getLayoutParams(int positionCode, int topPadding) {
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT);
        adParams.gravity = getLayoutGravityForPositionCode(positionCode);
        Insets insets = getSafeInsets();
        int safeInsetLeft = insets.left;
        int safeInsetTop = insets.top;
        
        adParams.bottomMargin = insets.bottom;
        adParams.rightMargin = insets.right;
        
        if (positionCode == -1) {
            int leftOffset = 0;
            if (leftOffset < safeInsetLeft)
                leftOffset = safeInsetLeft;
            int topOffset = (int) convertDpToPixel(topPadding);
            if (topOffset < safeInsetTop)
                topOffset = safeInsetTop;
            adParams.leftMargin = leftOffset;
            adParams.topMargin = topOffset;
        } else {
            adParams.leftMargin = safeInsetLeft;
            
            if (positionCode == 0 || positionCode == 2 || positionCode == 3 || positionCode == 6) {
                int topOffsetPixel = (int) convertDpToPixel(topPadding);
                adParams.topMargin = safeInsetTop + topOffsetPixel;
            }
        }
        return adParams;
    }
    
    private static int getLayoutGravityForPositionCode(int positionCode) {
        switch (positionCode) {
            case 0: // POSITION_TOP_CENTER
                return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            case 1: // POSITION_BOTTOM_CENTER
                return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            case 2: // POSITION_TOP_LEFT
                return Gravity.TOP | Gravity.START;
            case 3: // POSITION_TOP_RIGHT
                return Gravity.TOP | Gravity.END;
            case 4: // POSITION_BOTTOM_LEFT
                return Gravity.BOTTOM | Gravity.START;
            case 5: // POSITION_BOTTOM_RIGHT
                return Gravity.BOTTOM | Gravity.END;
            case 6: // POSITION_CENTER
                return Gravity.CENTER;
            case -1: // POSITION_CUSTOM
                return Gravity.TOP | Gravity.START;
            default:
                throw new IllegalArgumentException("Invalid ad position code: " + positionCode);
        }
    }
    
    private Insets getSafeInsets() {
        Insets insets = new Insets();
        if (Build.VERSION.SDK_INT < 28)
            return insets;
        Activity activity = getCurrentActivity();
        if (activity == null)
            return insets;
        Window window = activity.getWindow();
        if (window == null)
            return insets;
        WindowInsets windowInsets = window.getDecorView().getRootWindowInsets();
        if (windowInsets == null)
            return insets;
        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        if (displayCutout == null)
            return insets;
        insets.top = displayCutout.getSafeInsetTop();
        insets.left = displayCutout.getSafeInsetLeft();
        insets.bottom = displayCutout.getSafeInsetBottom();
        insets.right = displayCutout.getSafeInsetRight();
        return insets;
    }
    
    private static float convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return dp * metrics.density;
    }
}

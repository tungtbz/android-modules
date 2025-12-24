package com.rofi.admobadshelper;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.lang.ref.WeakReference;

/**
 * Base class for all Ad Managers
 * Provides common functionality:
 * - Thread-safe activity reference management
 * - Retry mechanism with exponential backoff
 * - Thread-safe UI operations
 * - Common logging
 */
public abstract class BaseAdManager {
    protected final String TAG;
    
    // Thread-safe activity reference to prevent memory leaks
    protected volatile WeakReference<Activity> activityRef;
    
    // Callback for ad events
    protected volatile IAdmobAdListener callback;
    
    // Auto-reload retry mechanism
    protected static final long BASE_RETRY_DELAY_MS = 5000; // 5 seconds
    protected static final long MAX_RETRY_DELAY_MS = 300000; // 5 minutes
    
    // Retry handler and counter
    protected final Handler retryHandler;
    protected volatile int retryCount = 0;
    
    /**
     * Constructor
     * @param tag Log tag for this manager
     */
    protected BaseAdManager(String tag) {
        this.TAG = tag;
        this.activityRef = new WeakReference<>(null);
        this.retryHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Set the callback for ad events
     * @param callback Ad event listener
     */
    public synchronized void setCallback(IAdmobAdListener callback) {
        this.callback = callback;
    }
    
    /**
     * Set current activity reference
     * @param activity Current activity
     */
    protected void setCurrentActivity(Activity activity) {
        if (activity != null) {
            activityRef = new WeakReference<>(activity);
        } else {
            activityRef = new WeakReference<>(null);
        }
    }
    
    /**
     * Get current activity from WeakReference
     * @return Current activity or null if not available
     */
    protected Activity getCurrentActivity() {
        Activity activity = activityRef != null ? activityRef.get() : null;
        
        // Fallback to UnityPlayer.currentActivity if WeakReference is null
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            activity = UnityPlayer.currentActivity;
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                Log.w(TAG, "Current activity is null or invalid");
                return null;
            }
        }
        return activity;
    }
    
    /**
     * Run code safely on UI thread with activity validation
     * @param activity Activity context
     * @param runner Runnable to execute
     */
    protected void runSafelyOnUiThread(Activity activity, final Runnable runner) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is null or finishing, skipping UI operation");
            return;
        }
        
        activity.runOnUiThread(() -> {
            try {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    runner.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in UI thread operation", e);
            }
        });
    }
    
    /**
     * Calculate exponential backoff delay for retry attempts
     * @param attemptCount Number of retry attempts (0-based)
     * @return Delay in milliseconds (5s, 10s, 20s, 40s, ..., capped at 5 minutes)
     */
    protected long calculateRetryDelay(int attemptCount) {
        long delay = BASE_RETRY_DELAY_MS * (long) Math.pow(2, attemptCount);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }
    
    /**
     * Reset retry counter on successful load
     */
    protected void resetRetryCounter() {
        synchronized (this) {
            if (retryCount > 0) {
                Log.d(TAG, "Ad loaded successfully after " + retryCount + " retries");
            }
            retryCount = 0;
        }
        retryHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Schedule ad reload with exponential backoff
     * Subclasses should override this to implement specific reload logic
     */
    protected abstract void scheduleReload();
    
    /**
     * Cleanup resources
     * Should be called when the manager is no longer needed
     */
    public void cleanup() {
        synchronized (this) {
            // Cancel pending retry tasks
            retryHandler.removeCallbacksAndMessages(null);
            
            // Reset retry counter
            retryCount = 0;
            
            // Clear references
            activityRef = new WeakReference<>(null);
            callback = null;
            
            Log.d(TAG, "Manager cleaned up");
        }
    }
    
    /**
     * Pause ad (called when activity goes to background)
     * Subclasses should override if needed
     */
    public void onPause() {
        // Default: do nothing
    }
    
    /**
     * Resume ad (called when activity comes to foreground)
     * Subclasses should override if needed
     */
    public void onResume() {
        // Default: do nothing
    }
}

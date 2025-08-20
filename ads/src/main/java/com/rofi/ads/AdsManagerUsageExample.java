package com.rofi.ads;

import android.app.Activity;
import android.util.Log;

/**
 * Example class showing how to properly use the improved AdsManager
 */
public class AdsManagerUsageExample {
    private static final String TAG = "AdsUsageExample";

    public void demonstrateProperUsage(Activity activity, IAdsService adsService) {
        // Get singleton instance
        AdsManager adsManager = AdsManager.getInstance();
        
        // Initialize with service
        if (adsManager.init(adsService)) {
            Log.d(TAG, "AdsManager initialized with service: " + adsManager.getServiceType());
            
            // Check if initialized before using
            if (adsManager.isInitialized()) {
                // Get service safely
                IAdsService service = adsManager.getSafeService();
                if (service != null) {
                    // Use the service
                    service.ShowBanner(activity);
                    
                    // Mark ready to show interstitial when appropriate
                    adsManager.setReadyToShowInter();
                    
                    // Check if ready before showing inter
                    if (adsManager.isReadyToShowInter() && service.IsInterReady()) {
                        service.ShowInter(1);
                        // Reset state after showing
                        adsManager.resetReadyState();
                    }
                }
            }
        } else {
            Log.e(TAG, "Failed to initialize AdsManager");
        }
    }
    
    public void demonstrateLifecycleManagement() {
        AdsManager adsManager = AdsManager.getInstance();
        
        // In Activity onDestroy or when app is closing
        adsManager.cleanup();
        
        Log.d(TAG, "AdsManager cleaned up");
    }
    
    public void demonstrateErrorHandling(IAdsService adsService) {
        AdsManager adsManager = AdsManager.getInstance();
        
        // Try to use before initialization
        if (!adsManager.isInitialized()) {
            Log.w(TAG, "AdsManager not initialized");
            // Will return null and log warning
            IAdsService service = adsManager.getService();
        }
        
        // Try to initialize with null service
        if (!adsManager.init(null)) {
            Log.e(TAG, "Cannot initialize with null service");
        }
        
        // Proper initialization
        if (adsManager.init(adsService)) {
            Log.d(TAG, "Successfully initialized");
        }
    }
}

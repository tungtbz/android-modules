package com.example;

import android.app.Activity;
import android.os.Bundle;
import com.rofi.admobadshelper.AdmobHelper;
import com.rofi.admobadshelper.IAdmobAdListener;
import com.rofi.admobadshelper.IGoogleConsentCallback;

/**
 * Example Activity showing proper usage of AdmobHelper
 * with correct lifecycle management to prevent memory leaks
 */
public class ExampleActivity extends Activity {
    
    private static final String APP_OPEN_AD_ID = "ca-app-pub-xxxxx/xxxxx";
    private static final String BANNER_AD_ID = "ca-app-pub-xxxxx/xxxxx";
    private static final String MREC_AD_ID = "ca-app-pub-xxxxx/xxxxx";
    
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Get singleton instance
        adHelper = AdmobHelper.getInstance();
        
        // Set up ad event callbacks
        setupAdCallbacks();
        
        // Initialize ads with consent flow
        initializeAds();
    }
    
    private void setupAdCallbacks() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onAdImpression(String adFormat, String adUnitId, String adNetwork, double value) {
                // Handle ad impression for analytics/revenue tracking
                logAdRevenue(adFormat, adNetwork, value);
            }
            
            @Override
            public void onAdDisplayFullScreenContent(int type) {
                // App Open Ad is showing
                // Pause game/music if needed
            }
            
            @Override
            public void onAdDismissedFullScreenContent(int type) {
                // App Open Ad dismissed
                // Resume game/music if needed
            }
            
            @Override
            public void onAOAFailedToLoad() {
                // App Open Ad failed to load
                // Continue with app flow
            }
            
            @Override
            public void onAdClicked() {
                // User clicked on an ad
            }
            
            @Override
            public void onBannerLoaded() {
                // Banner loaded successfully
            }
            
            @Override
            public void onBannerCollapDisplay() {
                // Collapsible banner collapsed
            }
        });
    }
    
    private void initializeAds() {
        // Start consent flow (GDPR/UMP)
        adHelper.startConsentFlow(this, new IGoogleConsentCallback() {
            @Override
            public void onFinish(int reason) {
                if (reason == 1) {
                    // Consent obtained successfully
                    setupAllAds();
                } else {
                    // Consent not obtained or error
                    // You might still be able to show ads depending on region
                    setupAllAds();
                }
            }
        });
        
        // Alternative: Bypass consent flow for testing or non-GDPR regions
        // adHelper.bypassConsentFlow(this);
        // setupAllAds();
    }
    
    private void setupAllAds() {
        // Initialize App Open Ads
        String[] aoaArgs = {APP_OPEN_AD_ID};
        adHelper.Init(this, null, aoaArgs);
        adHelper.loadAd(this);
        
        // Initialize Banner
        adHelper.initBanner(this, BANNER_AD_ID, AdmobHelper.POSITION_BOTTOM_CENTER);
        adHelper.RunAutoRefreshBanner(60); // Refresh every 60 seconds
        adHelper.showBanner();
        
        // Initialize MREC
        adHelper.initMrec(this, MREC_AD_ID, String.valueOf(AdmobHelper.POSITION_TOP_CENTER));
        adHelper.setMrecPosition(AdmobHelper.POSITION_TOP_CENTER, 100); // 100dp from top
        adHelper.loadMrec();
        adHelper.ShowMrec();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Resume ads when activity comes to foreground
        if (adHelper != null) {
            adHelper.onResume();
            
            // Show App Open Ad if conditions are met
            if (adHelper.canShowAOA()) {
                adHelper.showAppOpenAds(this);
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Pause ads when activity goes to background
        if (adHelper != null) {
            adHelper.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // IMPORTANT: Clean up to prevent memory leaks
        if (adHelper != null) {
            adHelper.StopRefresh(); // Stop banner auto-refresh
            adHelper.cleanup();     // Clean up all resources
        }
    }
    
    // Example: Hide banner temporarily (e.g., during gameplay)
    private void hideBannerForGameplay() {
        if (adHelper != null) {
            adHelper.HideBanner();
        }
    }
    
    // Example: Show banner again
    private void showBannerAfterGameplay() {
        if (adHelper != null) {
            adHelper.showBanner();
        }
    }
    
    // Example: Control App Open Ads
    private void disableAppOpenAdsForPurchase() {
        if (adHelper != null) {
            adHelper.DisableAOA();
        }
    }
    
    private void enableAppOpenAdsAfterPurchase() {
        if (adHelper != null) {
            adHelper.EnableAOA();
        }
    }
    
    // Example: Block App Open Ads temporarily (e.g., during tutorial)
    private void blockAOADuringTutorial() {
        if (adHelper != null) {
            adHelper.IncreaseBlockAOA();
        }
    }
    
    private void unblockAOAAfterTutorial() {
        if (adHelper != null) {
            adHelper.DecreaseBlockAOA();
        }
    }
    
    // Example: Force reload banner
    private void forceReloadBanner() {
        if (adHelper != null) {
            adHelper.ForceLoadBanner();
        }
    }
    
    // Helper method for logging ad revenue
    private void logAdRevenue(String adFormat, String adNetwork, double value) {
        // Send to your analytics service
        // Example: Firebase Analytics, Adjust, AppsFlyer, etc.
        System.out.println("Ad Revenue: " + adFormat + " from " + adNetwork + " = $" + value);
    }
}

/**
 * IMPORTANT NOTES:
 * 
 * 1. ALWAYS call cleanup() in onDestroy() to prevent memory leaks
 * 2. ALWAYS call onPause() and onResume() for proper ad lifecycle
 * 3. Initialize ads AFTER consent is obtained (if in GDPR region)
 * 4. Use singleton pattern - don't create new instances
 * 5. Handle ad callbacks gracefully - they might be called on background threads
 * 6. Stop auto-refresh when not needed to save battery
 * 7. Test with Android Studio Memory Profiler to verify no leaks
 * 
 * AD POSITIONS:
 * - POSITION_TOP_CENTER = 0
 * - POSITION_BOTTOM_CENTER = 1
 * - POSITION_TOP_LEFT = 2
 * - POSITION_TOP_RIGHT = 3
 * - POSITION_BOTTOM_LEFT = 4
 * - POSITION_BOTTOM_RIGHT = 5
 * - POSITION_CENTER = 6
 * - POSITION_CUSTOM = -1 (use with custom offsets)
 * 
 * LIFECYCLE BEST PRACTICES:
 * 
 * onCreate():
 *   - Initialize AdmobHelper
 *   - Set callbacks
 *   - Start consent flow
 * 
 * onResume():
 *   - Resume ads
 *   - Show App Open Ads (if appropriate)
 * 
 * onPause():
 *   - Pause ads to save resources
 * 
 * onDestroy():
 *   - Stop auto-refresh
 *   - Call cleanup() - THIS IS CRITICAL!
 * 
 * TESTING FOR MEMORY LEAKS:
 * 
 * 1. Open Android Studio Memory Profiler
 * 2. Create activity and load ads
 * 3. Rotate device multiple times
 * 4. Force garbage collection
 * 5. Check if old activity instances are collected
 * 6. Verify AdView instances are released
 */



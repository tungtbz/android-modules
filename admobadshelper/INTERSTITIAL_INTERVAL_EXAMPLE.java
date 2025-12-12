// Example: Using Interstitial Ad Interval Control

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.rofi.admobadshelper.AdmobHelper;
import com.rofi.admobadshelper.IAdmobAdListener;

/**
 * Example demonstrating how to use Interstitial Ad interval control
 * to limit ad frequency and improve user experience
 */
public class InterstitialIntervalExample extends AppCompatActivity {
    
    private static final String TAG = "InterstitialInterval";
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Test ID
    
    // Common interval values
    private static final long INTERVAL_30_SECONDS = 30 * 1000;
    private static final long INTERVAL_1_MINUTE = 60 * 1000;
    private static final long INTERVAL_2_MINUTES = 2 * 60 * 1000;
    private static final long INTERVAL_5_MINUTES = 5 * 60 * 1000;
    
    private AdmobHelper adHelper;
    private TextView tvRemainingTime;
    private Button btnShowAd;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        adHelper = AdmobHelper.getInstance();
        
        // Initialize interstitial
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        
        // Set interval to 2 minutes between ads
        adHelper.setInterstitialInterval(INTERVAL_2_MINUTES);
        
        // Load first ad
        adHelper.loadInterstitial();
        
        // Setup UI
        setupUI();
        
        // Setup callbacks
        setupCallbacks();
        
        // Start timer to update UI
        startRemainingTimeUpdater();
    }
    
    private void setupUI() {
        tvRemainingTime = findViewById(R.id.tv_remaining_time);
        btnShowAd = findViewById(R.id.btn_show_ad);
        
        btnShowAd.setOnClickListener(v -> attemptToShowAd());
        
        Button btnSetInterval30s = findViewById(R.id.btn_set_30s);
        btnSetInterval30s.setOnClickListener(v -> {
            adHelper.setInterstitialInterval(INTERVAL_30_SECONDS);
            Toast.makeText(this, "Interval set to 30 seconds", Toast.LENGTH_SHORT).show();
        });
        
        Button btnSetInterval2m = findViewById(R.id.btn_set_2m);
        btnSetInterval2m.setOnClickListener(v -> {
            adHelper.setInterstitialInterval(INTERVAL_2_MINUTES);
            Toast.makeText(this, "Interval set to 2 minutes", Toast.LENGTH_SHORT).show();
        });
        
        Button btnResetInterval = findViewById(R.id.btn_reset_interval);
        btnResetInterval.setOnClickListener(v -> {
            adHelper.resetInterstitialInterval();
            Toast.makeText(this, "Interval timer reset", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupCallbacks() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onAdDisplayFullScreenContent(int adType) {
                if (adType == 1) { // Interstitial
                    Log.d(TAG, "Interstitial showing");
                }
            }
            
            @Override
            public void onAdDismissedFullScreenContent(int adType) {
                if (adType == 1) { // Interstitial
                    Log.d(TAG, "Interstitial dismissed");
                    
                    // Show remaining time
                    long intervalMs = adHelper.getInterstitialInterval();
                    String message = "Next ad available in " + (intervalMs / 1000) + " seconds";
                    Toast.makeText(InterstitialIntervalExample.this, message, 
                                 Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onAdClicked() {
                Log.d(TAG, "Ad clicked");
            }
            
            @Override
            public void onAdImpression(String adFormat, String adUnitId, 
                                      String adSourceName, double revenue) {
                Log.d(TAG, "Ad impression: " + adFormat + ", revenue: " + revenue);
            }
            
            @Override
            public void onUserEarnedReward(int rewardCode, String type, int amount) {
                // Not used for interstitial
            }
            
            @Override
            public void onAOAFailedToLoad() {
                // Not used for interstitial
            }
        });
    }
    
    private void attemptToShowAd() {
        // Check if ad can be shown based on interval
        if (!adHelper.canShowInterstitialByInterval()) {
            long remainingMs = adHelper.getRemainingInterstitialInterval();
            long remainingSec = remainingMs / 1000;
            
            String message = "Please wait " + remainingSec + " seconds before next ad";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Interval not met. Remaining: " + remainingSec + "s");
            return;
        }
        
        // Check if ad is ready
        if (!adHelper.isInterstitialReady()) {
            Toast.makeText(this, "Ad is not ready yet", Toast.LENGTH_SHORT).show();
            adHelper.loadInterstitial(); // Try to load
            return;
        }
        
        // Show the ad
        adHelper.showInterstitial(0);
    }
    
    private void startRemainingTimeUpdater() {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateRemainingTimeUI();
                tvRemainingTime.postDelayed(this, 1000); // Update every second
            }
        };
        
        tvRemainingTime.post(updateRunnable);
    }
    
    private void updateRemainingTimeUI() {
        long remainingMs = adHelper.getRemainingInterstitialInterval();
        
        if (remainingMs > 0) {
            long seconds = remainingMs / 1000;
            long minutes = seconds / 60;
            long secs = seconds % 60;
            
            String timeText = String.format("Next ad in: %02d:%02d", minutes, secs);
            tvRemainingTime.setText(timeText);
            btnShowAd.setEnabled(false);
        } else {
            tvRemainingTime.setText("Ad available now");
            btnShowAd.setEnabled(adHelper.isInterstitialReady());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        adHelper.cleanup();
    }
}

// ==================== GAME EXAMPLE ====================

/**
 * Example: Game with level-based ads and interval control
 */
class GameWithIntervalExample extends AppCompatActivity {
    
    private static final String TAG = "GameInterval";
    private static final long INTERVAL_BETWEEN_ADS = 3 * 60 * 1000; // 3 minutes
    private int levelCount = 0;
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        
        // Set 3 minute interval
        adHelper.setInterstitialInterval(INTERVAL_BETWEEN_ADS);
        
        // Preload
        adHelper.loadInterstitial();
    }
    
    void onLevelCompleted() {
        levelCount++;
        
        // Try to show ad every 2 levels, but respect interval
        if (levelCount % 2 == 0) {
            showInterstitialIfPossible();
        } else {
            startNextLevel();
        }
    }
    
    private void showInterstitialIfPossible() {
        if (adHelper.canShowInterstitialByInterval() && 
            adHelper.isInterstitialReady()) {
            
            // Show ad
            adHelper.showInterstitial(0);
            
        } else {
            // Skip ad this time, proceed to next level
            Log.d(TAG, "Skipping ad due to interval or not ready");
            startNextLevel();
        }
    }
    
    private void startNextLevel() {
        // Start next level logic
        Log.d(TAG, "Starting level " + (levelCount + 1));
    }
}

// ==================== UTILITY APP EXAMPLE ====================

/**
 * Example: Photo editor with action-based ads
 */
class PhotoEditorIntervalExample extends AppCompatActivity {
    
    private static final long INTERVAL_BETWEEN_ADS = 5 * 60 * 1000; // 5 minutes
    private int actionsCount = 0;
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        
        // Set 5 minute interval
        adHelper.setInterstitialInterval(INTERVAL_BETWEEN_ADS);
        adHelper.loadInterstitial();
    }
    
    void onPhotoSaved() {
        actionsCount++;
        
        // Try to show ad every 3 saves
        if (actionsCount % 3 == 0) {
            showInterstitialWithInterval();
        }
    }
    
    void onPhotoShared() {
        showInterstitialWithInterval();
    }
    
    private void showInterstitialWithInterval() {
        // Check interval and show if possible
        if (adHelper.canShowInterstitialByInterval()) {
            if (adHelper.isInterstitialReady()) {
                adHelper.showInterstitial(0);
            } else {
                Log.d("PhotoEditor", "Ad not ready, loading...");
                adHelper.loadInterstitial();
            }
        } else {
            long remainingSec = adHelper.getRemainingInterstitialInterval() / 1000;
            Log.d("PhotoEditor", "Interval not met. Wait " + remainingSec + "s more");
        }
    }
}

// ==================== ADVANCED EXAMPLE WITH DYNAMIC INTERVAL ====================

/**
 * Example: Dynamic interval based on user engagement
 */
class DynamicIntervalExample extends AppCompatActivity {
    
    private AdmobHelper adHelper;
    private static final long BASE_INTERVAL = 2 * 60 * 1000; // 2 minutes
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        
        // Start with base interval
        adHelper.setInterstitialInterval(BASE_INTERVAL);
        adHelper.loadInterstitial();
        
        // Adjust interval based on user behavior
        adjustIntervalBasedOnEngagement();
    }
    
    private void adjustIntervalBasedOnEngagement() {
        // Get user engagement level from analytics
        int engagementLevel = getUserEngagementLevel(); // 1-5
        
        long interval;
        switch (engagementLevel) {
            case 5: // Very engaged - show less ads
                interval = 5 * 60 * 1000; // 5 minutes
                break;
            case 4: // Engaged
                interval = 4 * 60 * 1000; // 4 minutes
                break;
            case 3: // Normal
                interval = 3 * 60 * 1000; // 3 minutes
                break;
            case 2: // Low engagement
                interval = 2 * 60 * 1000; // 2 minutes
                break;
            case 1: // Very low
                interval = 1 * 60 * 1000; // 1 minute
                break;
            default:
                interval = BASE_INTERVAL;
        }
        
        adHelper.setInterstitialInterval(interval);
        Log.d("DynamicInterval", "Interval set to " + (interval / 1000) + "s for engagement level " + engagementLevel);
    }
    
    private int getUserEngagementLevel() {
        // Calculate based on session time, actions, etc.
        // This is a placeholder
        return 3; // Normal engagement
    }
    
    void onUserAction() {
        // Show ad after certain actions, respecting interval
        if (adHelper.canShowInterstitialByInterval() && 
            adHelper.isInterstitialReady()) {
            adHelper.showInterstitial(0);
        }
    }
}

// ==================== EXAMPLE WITH FIREBASE REMOTE CONFIG ====================

/**
 * Example: Control interval via Firebase Remote Config
 */
class RemoteConfigIntervalExample extends AppCompatActivity {
    
    private AdmobHelper adHelper;
    private FirebaseRemoteConfig remoteConfig;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        
        // Setup Remote Config
        remoteConfig = FirebaseRemoteConfig.getInstance();
        
        // Set defaults
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("interstitial_interval_seconds", 120L); // 2 minutes default
        remoteConfig.setDefaultsAsync(defaults);
        
        // Fetch and apply
        fetchIntervalFromRemoteConfig();
    }
    
    private void fetchIntervalFromRemoteConfig() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    applyRemoteInterval();
                }
            });
    }
    
    private void applyRemoteInterval() {
        long intervalSeconds = remoteConfig.getLong("interstitial_interval_seconds");
        long intervalMs = intervalSeconds * 1000;
        
        adHelper.setInterstitialInterval(intervalMs);
        Log.d("RemoteConfig", "Interval set from remote config: " + intervalSeconds + "s");
        
        // Load ad
        adHelper.loadInterstitial();
    }
}







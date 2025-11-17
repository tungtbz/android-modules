/**
 * REWARDED AD USAGE EXAMPLE
 * 
 * Hướng dẫn tích hợp và sử dụng Rewarded Ad với AdmobHelper
 * Dựa trên tài liệu chính thức của Google AdMob
 * 
 * Reference: https://developers.google.com/admob/android/rewarded
 */

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.rofi.admobadshelper.AdmobHelper;
import com.rofi.admobadshelper.IAdmobAdListener;

public class RewardedUsageExample extends AppCompatActivity {
    
    // Test Ad Unit ID cho Rewarded (dùng trong development)
    // Test ID từ Google: ca-app-pub-3940256099942544/5224354917
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    
    // Production Ad Unit ID (thay thế khi release)
    // private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ";
    
    private AdmobHelper adHelper;
    private int userCoins = 100;
    private TextView coinsTextView;
    private Button watchAdButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        coinsTextView = findViewById(R.id.textViewCoins);
        watchAdButton = findViewById(R.id.buttonWatchAd);
        
        // Lấy instance của AdmobHelper (Singleton)
        adHelper = AdmobHelper.getInstance();
        
        // ===== BƯỚC 1: Setup Callbacks =====
        setupAdCallbacks();
        
        // ===== BƯỚC 2: Khởi tạo Rewarded Ad =====
        adHelper.initRewarded(this, REWARDED_AD_UNIT_ID);
        
        // ===== BƯỚC 3: Preload Rewarded Ad =====
        // Load sớm để khi user bấm nút không phải chờ
        adHelper.loadRewarded();
        
        // ===== BƯỚC 4: Setup UI =====
        updateCoinsDisplay();
        setupWatchAdButton();
    }
    
    /**
     * PHƯƠNG THỨC 1: Setup callbacks để nhận reward
     */
    private void setupAdCallbacks() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onUserEarnedReward(String type, int amount) {
                // User đã xem đủ ad và nhận được thưởng
                runOnUiThread(() -> {
                    userCoins += amount;
                    updateCoinsDisplay();
                    showRewardMessage("You earned " + amount + " " + type + "!");
                });
            }
            
            @Override
            public void onAdDisplayFullScreenContent(int adType) {
                if (adType == 2) { // 2 = Rewarded
                    // Ad đang hiển thị
                    pauseGame();
                    disableWatchAdButton();
                }
            }
            
            @Override
            public void onAdDismissedFullScreenContent(int adType) {
                if (adType == 2) { // 2 = Rewarded
                    // User đã đóng ad (có thể đã nhận thưởng hoặc không)
                    resumeGame();
                    enableWatchAdButton();
                    // Ad đã auto-reload trong AdmobHelper
                }
            }
            
            @Override
            public void onAdClicked() {
                // User clicked on ad
            }
            
            @Override
            public void onAdImpression(String adFormat, String adUnitId, 
                                       String adSourceName, double revenue) {
                if (adFormat.equals("REWARDED")) {
                    // Track revenue
                    trackAdRevenue(revenue, adSourceName);
                }
            }
            
            @Override
            public void onAOAFailedToLoad() {
                // Not relevant for Rewarded
            }
            
            @Override
            public void onBannerLoaded() {
                // Not relevant for Rewarded
            }
            
            @Override
            public void onBannerCollapDisplay() {
                // Not relevant for Rewarded
            }
        });
    }
    
    /**
     * PHƯƠNG THỨC 2: Setup Watch Ad Button
     * User chủ động bấm để xem ad và nhận thưởng
     */
    private void setupWatchAdButton() {
        watchAdButton.setOnClickListener(v -> {
            // Show rewarded ad
            showRewardedAd();
        });
        
        // Update button state based on ad availability
        updateWatchAdButtonState();
    }
    
    /**
     * PHƯƠNG THỨC 3: Show Rewarded Ad
     */
    private void showRewardedAd() {
        if (adHelper.isRewardedReady()) {
            // Ad sẵn sàng, hiển thị ngay
            adHelper.showRewarded();
        } else {
            // Ad chưa sẵn sàng, load và thông báo user
            showMessage("Ad is loading, please wait...");
            adHelper.loadRewarded();
            
            // Check again after a delay
            watchAdButton.postDelayed(() -> {
                updateWatchAdButtonState();
            }, 2000);
        }
    }
    
    /**
     * PHƯƠNG THỨC 4: Update button state
     */
    private void updateWatchAdButtonState() {
        if (adHelper.isRewardedReady()) {
            watchAdButton.setEnabled(true);
            watchAdButton.setText("Watch Ad to Earn 50 Coins");
        } else {
            watchAdButton.setEnabled(false);
            watchAdButton.setText("Loading Ad...");
        }
    }
    
    /**
     * EXAMPLE: Game with Rewarded Ads
     */
    private void gameExample() {
        // Scenario 1: Continue game after game over
        findViewById(R.id.buttonContinue).setOnClickListener(v -> {
            if (adHelper.isRewardedReady()) {
                showMessage("Watch ad to continue?");
                adHelper.showRewarded();
                // Continue game in onUserEarnedReward callback
            } else {
                showMessage("Ad not ready, please restart");
            }
        });
        
        // Scenario 2: Get extra lives
        findViewById(R.id.buttonExtraLives).setOnClickListener(v -> {
            if (adHelper.isRewardedReady()) {
                adHelper.showRewarded();
                // Add lives in onUserEarnedReward callback
            } else {
                adHelper.loadRewarded();
                showMessage("Please wait...");
            }
        });
        
        // Scenario 3: Unlock premium item
        findViewById(R.id.buttonUnlockItem).setOnClickListener(v -> {
            if (adHelper.isRewardedReady()) {
                showConfirmDialog("Watch ad to unlock this item?", () -> {
                    adHelper.showRewarded();
                    // Unlock in onUserEarnedReward callback
                });
            }
        });
    }
    
    /**
     * EXAMPLE: Utility App with Rewarded Ads
     */
    private void utilityAppExample() {
        // Scenario: Remove watermark by watching ad
        findViewById(R.id.buttonRemoveWatermark).setOnClickListener(v -> {
            if (adHelper.isRewardedReady()) {
                showConfirmDialog(
                    "Watch a short video to remove watermark?",
                    () -> {
                        adHelper.showRewarded();
                        // Remove watermark in onUserEarnedReward
                    }
                );
            } else {
                // Offer alternative (purchase, etc.)
                showPurchaseDialog();
            }
        });
    }
    
    /**
     * EXAMPLE: Reward Types Handling
     */
    private void handleDifferentRewardTypes() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onUserEarnedReward(String type, int amount) {
                // Handle different reward types
                switch (type.toLowerCase()) {
                    case "coins":
                    case "coin":
                        userCoins += amount;
                        showMessage("+" + amount + " coins!");
                        break;
                        
                    case "lives":
                    case "life":
                        addLives(amount);
                        showMessage("+" + amount + " lives!");
                        break;
                        
                    case "unlock":
                        unlockPremiumFeature();
                        showMessage("Premium feature unlocked!");
                        break;
                        
                    case "continue":
                        continueGame();
                        showMessage("Continue playing!");
                        break;
                        
                    default:
                        // Generic reward
                        grantGenericReward(type, amount);
                        break;
                }
            }
            
            // ... other callbacks ...
            @Override public void onAdDisplayFullScreenContent(int adType) {}
            @Override public void onAdDismissedFullScreenContent(int adType) {}
            @Override public void onAdClicked() {}
            @Override public void onAdImpression(String adFormat, String adUnitId, String adSourceName, double revenue) {}
            @Override public void onAOAFailedToLoad() {}
            @Override public void onBannerLoaded() {}
            @Override public void onBannerCollapDisplay() {}
        });
    }
    
    /**
     * EXAMPLE: Frequency Control
     */
    private static final long REWARD_COOLDOWN_MS = 300000; // 5 minutes
    private long lastRewardTime = 0;
    
    private void showRewardedWithCooldown() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastReward = currentTime - lastRewardTime;
        
        if (timeSinceLastReward < REWARD_COOLDOWN_MS) {
            long remainingSeconds = (REWARD_COOLDOWN_MS - timeSinceLastReward) / 1000;
            showMessage("Please wait " + remainingSeconds + " seconds");
            return;
        }
        
        if (adHelper.isRewardedReady()) {
            adHelper.showRewarded();
        } else {
            showMessage("Ad not available");
            adHelper.loadRewarded();
        }
    }
    
    private void onRewardEarned(String type, int amount) {
        lastRewardTime = System.currentTimeMillis();
        userCoins += amount;
        updateCoinsDisplay();
    }
    
    /**
     * EXAMPLE: Daily Reward Limit
     */
    private static final int MAX_DAILY_REWARDS = 10;
    private int dailyRewardsCount = 0;
    
    private void showRewardedWithDailyLimit() {
        if (dailyRewardsCount >= MAX_DAILY_REWARDS) {
            showMessage("You've reached the daily limit!");
            showPurchaseDialog(); // Offer alternative
            return;
        }
        
        if (adHelper.isRewardedReady()) {
            adHelper.showRewarded();
        } else {
            showMessage("Loading ad...");
            adHelper.loadRewarded();
        }
    }
    
    private void onRewardEarnedWithLimit(String type, int amount) {
        dailyRewardsCount++;
        userCoins += amount;
        
        int remaining = MAX_DAILY_REWARDS - dailyRewardsCount;
        if (remaining > 0) {
            showMessage("Earned " + amount + " coins! (" + remaining + " left today)");
        } else {
            showMessage("Daily limit reached! Come back tomorrow.");
        }
        
        updateCoinsDisplay();
    }
    
    /**
     * EXAMPLE: Preloading Strategy
     */
    private void preloadingStrategies() {
        // Strategy 1: Load in onCreate (already done above)
        
        // Strategy 2: Load after showing ad (auto by AdmobHelper)
        
        // Strategy 3: Periodic check and reload
        watchAdButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!adHelper.isRewardedReady()) {
                    adHelper.loadRewarded();
                }
                // Check again in 5 minutes
                watchAdButton.postDelayed(this, 300000);
            }
        }, 300000);
        
        // Strategy 4: Load when entering specific screen
        // Call loadRewarded() in onResume() of reward-offering screen
    }
    
    /**
     * BEST PRACTICES CHECKLIST
     */
    private void bestPracticesChecklist() {
        // ✓ 1. User opt-in: Chỉ show khi user chủ động bấm nút
        // ✓ 2. Clear messaging: Nói rõ user sẽ nhận được gì
        // ✓ 3. Preload: Load sớm để không phải chờ
        // ✓ 4. Don't force: Không ép buộc phải xem ad
        // ✓ 5. Frequency limit: Giới hạn số lần show per day
        // ✓ 6. Alternative option: Cung cấp lựa chọn khác (IAP)
        // ✓ 7. Pause game: Tạm dừng game khi show ad
        // ✓ 8. Single-use: Một ad chỉ dùng 1 lần
        // ✓ 9. Test ad ID: Dùng test ID khi development
        // ✓ 10. Grant reward: Chỉ cộng thưởng trong onUserEarnedReward
    }
    
    /**
     * LIFECYCLE MANAGEMENT
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Update button state when returning to screen
        updateWatchAdButtonState();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup để tránh memory leaks
        if (adHelper != null) {
            adHelper.cleanup();
        }
    }
    
    // ===== HELPER METHODS =====
    
    private void updateCoinsDisplay() {
        coinsTextView.setText("Coins: " + userCoins);
    }
    
    private void pauseGame() {
        // Pause game logic
    }
    
    private void resumeGame() {
        // Resume game logic
    }
    
    private void continueGame() {
        // Continue game after game over
    }
    
    private void addLives(int count) {
        // Add lives to player
    }
    
    private void unlockPremiumFeature() {
        // Unlock premium feature
    }
    
    private void grantGenericReward(String type, int amount) {
        // Handle generic reward
    }
    
    private void disableWatchAdButton() {
        watchAdButton.setEnabled(false);
    }
    
    private void enableWatchAdButton() {
        watchAdButton.setEnabled(true);
        updateWatchAdButtonState();
    }
    
    private void showMessage(String message) {
        // Show toast or snackbar
    }
    
    private void showRewardMessage(String message) {
        // Show reward earned message
    }
    
    private void showConfirmDialog(String message, Runnable onConfirm) {
        // Show confirmation dialog
    }
    
    private void showPurchaseDialog() {
        // Show IAP dialog as alternative
    }
    
    private void trackAdRevenue(double revenue, String source) {
        // Track ad revenue in analytics
    }
}

/**
 * ===== COMMON USE CASES =====
 * 
 * 1. GAME APP - Extra Lives/Continue:
 *    - Button "Continue" after game over
 *    - Button "Get Extra Lives"
 *    - Button "Double Rewards"
 *    - Unlock special items/characters
 * 
 * 2. UTILITY APP - Premium Features:
 *    - Remove watermark
 *    - Unlock premium filter
 *    - Export in high quality
 *    - Remove ads for 24h
 * 
 * 3. CONTENT APP - Access Content:
 *    - Read premium article
 *    - Watch premium video
 *    - Download content
 *    - Unlock next chapter
 * 
 * 4. EDUCATIONAL APP - Unlock Lessons:
 *    - Access premium course
 *    - Get hint in quiz
 *    - Unlock next level
 *    - Get study materials
 * 
 * ===== MESSAGING BEST PRACTICES =====
 * 
 * ✅ GOOD Messaging:
 * - "Watch a short video to earn 50 coins"
 * - "Get 3 extra lives by watching an ad"
 * - "Unlock this item with a video ad"
 * - "Continue playing after watching an ad"
 * 
 * ❌ BAD Messaging:
 * - "Click here" (không rõ ràng)
 * - "Free coins" (không nói về ad)
 * - "Must watch" (ép buộc)
 * - No message at all
 * 
 * ===== REWARD AMOUNTS =====
 * 
 * Balanced Rewards:
 * - Not too high: Không làm mất cân bằng game
 * - Not too low: Phải đủ hấp dẫn để user xem
 * - Fair value: Tương đương với IAP value
 * 
 * Examples:
 * - Coins: 50-100 coins (if 1000 = $1)
 * - Lives: 1-3 lives
 * - Time: 24h ad-free
 * - Content: 1 premium item
 * 
 * ===== FREQUENCY RECOMMENDATIONS =====
 * 
 * - Minimum interval: 5 minutes between ads
 * - Daily limit: 5-10 ads per day
 * - Session limit: 3-5 ads per session
 * - Don't spam user with offers
 * 
 * ===== ALTERNATIVE OPTIONS =====
 * 
 * Always provide alternatives:
 * - In-App Purchase (buy coins/lives)
 * - Earn through gameplay
 * - Wait timer
 * - Share to unlock
 * 
 * ===== TROUBLESHOOTING =====
 * 
 * Q: User watched ad but didn't receive reward?
 * A: - Check onUserEarnedReward is called
 *    - Verify reward is granted in callback
 *    - Check for race conditions
 *    - Test with test ad ID
 * 
 * Q: Ad not loading?
 * A: - Check internet connection
 *    - Verify ad unit ID correct
 *    - Check fill rate in AdMob Console
 *    - Use test ad ID to verify integration
 * 
 * Q: Button stays disabled?
 * A: - Check isRewardedReady() returns true
 *    - Verify loadRewarded() was called
 *    - Check logcat for errors
 *    - Update UI in onAdLoaded (via callback)
 * 
 * ===== INTEGRATION CHECKLIST =====
 * 
 * Before Release:
 * [ ] Replace test ad unit ID with production ID
 * [ ] Test reward granting
 * [ ] Test frequency limits
 * [ ] Test user can decline (have alternative)
 * [ ] Verify clear messaging
 * [ ] Test with no internet
 * [ ] Test lifecycle (rotation, background)
 * [ ] Verify no forced watching
 * [ ] Check AdMob policy compliance
 * [ ] Test analytics tracking
 */


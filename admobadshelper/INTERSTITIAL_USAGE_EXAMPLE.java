/**
 * INTERSTITIAL AD USAGE EXAMPLE
 * 
 * Hướng dẫn tích hợp và sử dụng Interstitial Ad với AdmobHelper
 * Dựa trên tài liệu chính thức của Google AdMob
 * 
 * Reference: https://developers.google.com/admob/android/interstitial
 */

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.rofi.admobadshelper.AdmobHelper;

public class InterstitialUsageExample extends AppCompatActivity {
    
    // Test Ad Unit ID cho Interstitial (dùng trong development)
    // Test ID từ Google: ca-app-pub-3940256099942544/1033173712
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    
    // Production Ad Unit ID (thay thế khi release)
    // private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY";
    
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Lấy instance của AdmobHelper (Singleton)
        adHelper = AdmobHelper.getInstance();
        
        // ===== BƯỚC 1: Khởi tạo Interstitial Ad =====
        // Gọi sau khi hoàn thành consent flow và SDK initialization
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        
        // ===== BƯỚC 2: Preload Interstitial Ad =====
        // Best practice: Load trước khi cần hiển thị
        // Thường gọi trong onCreate hoặc khi bắt đầu game/level
        adHelper.loadInterstitial();
        
        // ===== BƯỚC 3: Hiển thị Interstitial Ad =====
        // Hiển thị tại các điểm chuyển cảnh tự nhiên trong app
        // Ví dụ: sau khi hoàn thành level, task, hoặc khi share xong
        
        // Example: Show after completing a level
        findViewById(R.id.buttonCompleteLevel).setOnClickListener(v -> {
            // User completed level
            onLevelCompleted();
            
            // Show interstitial at natural break point
            showInterstitialAd();
        });
    }
    
    /**
     * PHƯƠNG THỨC 1: Show Interstitial đơn giản
     * AdmobHelper tự động kiểm tra ad đã sẵn sàng chưa
     */
    private void showInterstitialAd() {
        adHelper.showInterstitial();
        // Nếu ad chưa sẵn sàng, showInterstitial() sẽ tự động load cho lần sau
    }
    
    /**
     * PHƯƠNG THỨC 2: Show Interstitial với kiểm tra trước
     * Kiểm tra xem ad đã ready chưa trước khi show
     */
    private void showInterstitialWithCheck() {
        if (adHelper.isInterstitialReady()) {
            // Ad đã sẵn sàng, hiển thị ngay
            adHelper.showInterstitial();
        } else {
            // Ad chưa sẵn sàng, load và bỏ qua lần này
            adHelper.loadInterstitial();
            // Hoặc: thực hiện hành động backup
            proceedToNextScreen();
        }
    }
    
    /**
     * BEST PRACTICE: Tạm dừng app khi hiển thị interstitial
     * Dừng game logic, animation, audio khi show ad
     */
    private void onLevelCompleted() {
        // Pause game/app logic
        pauseGameplay();
        pauseAudio();
        
        // Show ad
        showInterstitialAd();
        
        // Note: Resume game logic sẽ được gọi trong onAdDismissedFullScreenContent callback
    }
    
    /**
     * EXAMPLE: Preload interstitial ở nhiều điểm trong app
     */
    private void preloadInterstitialStrategies() {
        // Strategy 1: Load trong onCreate
        // (đã implement ở trên)
        
        // Strategy 2: Load sau khi ad hiện tại bị dismiss
        // (AdmobHelper tự động làm điều này)
        
        // Strategy 3: Load khi bắt đầu một màn chơi mới
        adHelper.loadInterstitial();
        
        // Strategy 4: Load định kỳ nếu user ở lại app lâu
        // (có thể dùng Handler hoặc Timer)
    }
    
    /**
     * EXAMPLE: Sử dụng với Ad Callbacks
     * Đăng ký callback để nhận events từ ads
     */
    private void setupAdCallbacks() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onAdDisplayFullScreenContent(int adType) {
                // adType = 1 cho Interstitial
                if (adType == 1) {
                    // Interstitial đang hiển thị
                    pauseGameplay();
                    pauseAudio();
                }
            }
            
            @Override
            public void onAdDismissedFullScreenContent(int adType) {
                // adType = 1 cho Interstitial
                if (adType == 1) {
                    // User đã đóng interstitial
                    resumeGameplay();
                    resumeAudio();
                    proceedToNextScreen();
                    
                    // AdmobHelper đã tự động load ad mới cho lần sau
                }
            }
            
            @Override
            public void onAdClicked() {
                // User đã click vào ad
                // Track analytics nếu cần
            }
            
            @Override
            public void onAdImpression(String adFormat, String adUnitId, String adSourceName, double revenue) {
                // Impression được ghi nhận
                if (adFormat.equals("INTERSTITIAL")) {
                    // Track revenue
                    trackAdRevenue(adFormat, revenue, adSourceName);
                }
            }
            
            @Override
            public void onAOAFailedToLoad() {
                // Not relevant for Interstitial
            }
        });
    }
    
    /**
     * BEST PRACTICES CHECKLIST
     */
    private void bestPracticesChecklist() {
        // ✓ 1. Luôn dùng test ad unit ID khi phát triển
        // ✓ 2. Chỉ hiển thị interstitial tại các điểm chuyển cảnh tự nhiên
        // ✓ 3. Preload ad trước khi cần hiển thị
        // ✓ 4. Tạm dừng gameplay/audio khi hiển thị ad
        // ✓ 5. Tiếp tục gameplay/audio sau khi ad dismissed
        // ✓ 6. Không spam user bằng quảng cáo liên tục
        // ✓ 7. Kiểm tra ad availability trước khi show (optional)
        // ✓ 8. Ads hết hạn sau ~1 giờ, reload định kỳ nếu cần
    }
    
    /**
     * LIFECYCLE MANAGEMENT
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup để tránh memory leaks
        if (adHelper != null) {
            adHelper.cleanup();
        }
    }
    
    // ===== HELPER METHODS =====
    
    private void pauseGameplay() {
        // Implement game pause logic
    }
    
    private void pauseAudio() {
        // Implement audio pause logic
    }
    
    private void resumeGameplay() {
        // Implement game resume logic
    }
    
    private void resumeAudio() {
        // Implement audio resume logic
    }
    
    private void proceedToNextScreen() {
        // Navigate to next screen/level
    }
    
    private void trackAdRevenue(String adFormat, double revenue, String adSource) {
        // Track ad revenue in analytics
        // Example: Firebase Analytics, Adjust, AppsFlyer, etc.
    }
}

/**
 * ===== COMMON USE CASES =====
 * 
 * 1. GAME APP - Show between levels:
 *    - Load interstitial when level starts
 *    - Show when level completed
 *    - Resume game in onAdDismissedFullScreenContent
 * 
 * 2. UTILITY APP - Show after completing task:
 *    - Load when app starts
 *    - Show after user completes an action (share, save, etc.)
 *    - Don't interrupt critical workflows
 * 
 * 3. NEWS APP - Show between articles:
 *    - Load when article opens
 *    - Show when navigating to next article
 *    - Respect user reading flow
 * 
 * 4. SHOPPING APP - Show after checkout:
 *    - NEVER show during checkout process
 *    - Show after successful purchase
 *    - Only at natural break points
 * 
 * ===== FREQUENCY MANAGEMENT =====
 * 
 * Không show quá nhiều ads:
 * - Tối đa 1 interstitial mỗi 2-3 minutes
 * - Tối đa 1 interstitial mỗi 2-3 screens/levels
 * - Đừng show liên tục nếu user từ chối
 * - Cân bằng giữa revenue và user experience
 * 
 * ===== TROUBLESHOOTING =====
 * 
 * Q: Ad không hiển thị?
 * A: - Kiểm tra đã gọi initInterstitial() chưa
 *    - Kiểm tra đã load ad chưa (loadInterstitial())
 *    - Kiểm tra ad unit ID có đúng không
 *    - Kiểm tra consent flow đã hoàn thành chưa
 *    - Xem logs để biết lỗi cụ thể
 * 
 * Q: Ad load chậm?
 * A: - Preload càng sớm càng tốt
 *    - Load ngay sau khi dismiss ad trước
 *    - Kiểm tra network connectivity
 *    - Ad có thể không có inventory (test với test ad ID)
 * 
 * Q: Memory leak?
 * A: - AdmobHelper đã dùng WeakReference
 *    - Nhớ gọi cleanup() trong onDestroy()
 *    - Không hold strong reference đến Activity
 * 
 * ===== AD UNIT ID SETUP =====
 * 
 * 1. Development (Test):
 *    - Use: ca-app-pub-3940256099942544/1033173712
 *    - Safe to test, won't affect account
 * 
 * 2. Production (Real):
 *    - Create in AdMob Console
 *    - Format: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
 *    - Replace before release
 *    - NEVER use test ID in production
 * 
 * ===== INTEGRATION CHECKLIST =====
 * 
 * Before Release:
 * [ ] Replace test ad unit ID with production ID
 * [ ] Test ad loading and showing
 * [ ] Test ad callbacks
 * [ ] Test lifecycle (pause/resume/destroy)
 * [ ] Test memory usage
 * [ ] Verify frequency capping
 * [ ] Test on different devices/Android versions
 * [ ] Verify analytics tracking
 * [ ] Check AdMob policy compliance
 * [ ] Test in production mode (not debug)
 */


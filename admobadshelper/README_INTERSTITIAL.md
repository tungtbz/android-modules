# Interstitial Ad Integration Guide

## Tổng quan

Interstitial Ad là quảng cáo toàn màn hình hiển thị tại các điểm chuyển cảnh tự nhiên trong ứng dụng. AdmobHelper đã tích hợp đầy đủ Interstitial Ad theo best practices của Google AdMob.

## Tính năng

✅ **Thread-safe**: Sử dụng synchronized và volatile  
✅ **Memory-safe**: Sử dụng WeakReference để tránh memory leaks  
✅ **Auto-reload**: Tự động load ad mới sau khi dismissed  
✅ **Lifecycle-aware**: Xử lý đúng lifecycle của Activity  
✅ **Revenue tracking**: Tích hợp OnPaidEventListener  
✅ **Full callbacks**: Hỗ trợ đầy đủ FullScreenContentCallback  

## Cài đặt nhanh

### 1. Khởi tạo

```java
AdmobHelper adHelper = AdmobHelper.getInstance();

// Test Ad Unit ID (development)
String testAdUnitId = "ca-app-pub-3940256099942544/1033173712";

// Initialize interstitial
adHelper.initInterstitial(this, testAdUnitId);
```

### 2. Load Ad

```java
// Preload ad (best practice: load sớm nhất có thể)
adHelper.loadInterstitial();
```

### 3. Show Ad

```java
// Show at natural break point
adHelper.showInterstitial();
```

## API Reference

### `initInterstitial(Activity activity, String adUnitId)`

Khởi tạo Interstitial Ad với ad unit ID.

**Parameters:**
- `activity`: Current activity context
- `adUnitId`: Ad unit ID từ AdMob console

**Example:**
```java
adHelper.initInterstitial(this, "ca-app-pub-3940256099942544/1033173712");
```

---

### `loadInterstitial()`

Load interstitial ad. Tự động kiểm tra nếu ad đã loaded hoặc đang loading.

**Best Practice:**
- Gọi càng sớm càng tốt (trong onCreate hoặc khi bắt đầu level)
- Gọi lại sau khi ad dismissed (auto-reload)
- Gọi định kỳ nếu user ở trong app lâu

**Example:**
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adHelper.initInterstitial(this, adUnitId);
    adHelper.loadInterstitial(); // Preload
}
```

---

### `showInterstitial()`

Hiển thị interstitial ad. Tự động kiểm tra ad availability.

**Behavior:**
- Nếu ad ready: Hiển thị ngay
- Nếu ad chưa ready: Log warning và load cho lần sau
- Auto-reload sau khi dismissed

**Example:**
```java
// Show after completing level
void onLevelCompleted() {
    pauseGame();
    adHelper.showInterstitial();
    // Resume game in callback
}
```

---

### `isInterstitialReady()`

Kiểm tra xem interstitial ad có sẵn sàng hiển thị không.

**Returns:** `boolean` - true nếu ad đã loaded và ready

**Example:**
```java
if (adHelper.isInterstitialReady()) {
    adHelper.showInterstitial();
} else {
    // Proceed without ad or load for next time
    adHelper.loadInterstitial();
}
```

---

## Callbacks

Đăng ký callback để nhận events từ Interstitial Ad:

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onAdDisplayFullScreenContent(int adType) {
        if (adType == 1) { // 1 = Interstitial
            // Ad is showing
            pauseGame();
            pauseAudio();
        }
    }
    
    @Override
    public void onAdDismissedFullScreenContent(int adType) {
        if (adType == 1) { // 1 = Interstitial
            // User dismissed ad
            resumeGame();
            resumeAudio();
            // Auto-reloaded by AdmobHelper
        }
    }
    
    @Override
    public void onAdClicked() {
        // User clicked on ad
        trackAdClick();
    }
    
    @Override
    public void onAdImpression(String adFormat, String adUnitId, 
                               String adSourceName, double revenue) {
        if (adFormat.equals("INTERSTITIAL")) {
            // Track revenue
            trackAdRevenue(revenue, adSourceName);
        }
    }
    
    @Override
    public void onAOAFailedToLoad() {
        // Not relevant for Interstitial
    }
});
```

### Callback Parameters

**adType:**
- `0` = App Open Ad
- `1` = Interstitial Ad

**adFormat:**
- `"INTERSTITIAL"` = Interstitial Ad
- `"COLLAPSIBLE_BANNER"` = Banner Ad
- `"MREC"` = Medium Rectangle
- `"App open"` = App Open Ad

---

## Best Practices

### 1. Timing - Khi nào show ad?

✅ **ĐÚNG - Các điểm chuyển cảnh tự nhiên:**
- Sau khi hoàn thành level/task
- Khi chuyển giữa các màn hình chính
- Sau khi share/save thành công
- Khi tạm dừng game (pause menu)

❌ **SAI - Không show khi:**
- Đang trong critical workflow (checkout, login)
- Ngay khi mở app (dùng App Open Ad thay thế)
- Liên tục mỗi vài giây
- Đang loading dữ liệu quan trọng

### 2. Frequency - Tần suất hiển thị

```java
private static final int MIN_INTERVAL_MS = 120000; // 2 minutes
private long lastInterstitialShownTime = 0;

private void showInterstitialWithCapping() {
    long currentTime = System.currentTimeMillis();
    long timeSinceLastAd = currentTime - lastInterstitialShownTime;
    
    if (timeSinceLastAd >= MIN_INTERVAL_MS) {
        adHelper.showInterstitial();
        lastInterstitialShownTime = currentTime;
    } else {
        // Skip this time, show next time
        Log.d(TAG, "Interstitial shown too recently, skipping");
    }
}
```

**Khuyến nghị:**
- Tối đa 1 ad mỗi 2-3 phút
- Tối đa 1 ad mỗi 2-3 màn hình/level
- Cân bằng giữa revenue và user experience

### 3. Preloading - Load trước

```java
// Strategy 1: Load trong onCreate
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adHelper.initInterstitial(this, adUnitId);
    adHelper.loadInterstitial(); // Load ngay
}

// Strategy 2: Load khi bắt đầu level
void startLevel(int levelNumber) {
    setupLevel(levelNumber);
    adHelper.loadInterstitial(); // Sẵn sàng cho khi hoàn thành
}

// Strategy 3: Auto-reload sau khi dismissed
// (AdmobHelper tự động làm điều này)
```

### 4. Pause/Resume - Tạm dừng app

```java
void showInterstitialWithPause() {
    // Before showing
    pauseGameLogic();
    pauseAnimations();
    pauseAudio();
    
    // Show ad
    adHelper.showInterstitial();
    
    // Resume in callback
    adHelper.SetAdsCallback(new IAdmobAdListener() {
        @Override
        public void onAdDismissedFullScreenContent(int adType) {
            if (adType == 1) {
                resumeGameLogic();
                resumeAnimations();
                resumeAudio();
            }
        }
    });
}
```

---

## Use Cases

### Game App

```java
public class GameActivity extends AppCompatActivity {
    private int levelCount = 0;
    private static final int SHOW_AD_EVERY_N_LEVELS = 3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AdmobHelper adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        adHelper.loadInterstitial(); // Preload
    }
    
    void onLevelCompleted() {
        levelCount++;
        
        // Show ad every 3 levels
        if (levelCount % SHOW_AD_EVERY_N_LEVELS == 0) {
            pauseGame();
            adHelper.showInterstitial();
            // Game will resume in callback
        } else {
            startNextLevel();
        }
    }
}
```

### Utility App

```java
public class PhotoEditorActivity extends AppCompatActivity {
    
    void onSaveCompleted() {
        // User saved edited photo
        showSuccessMessage();
        
        // Show interstitial after save
        adHelper.showInterstitial();
        
        // Navigate back after ad dismissed (in callback)
    }
}
```

### News/Content App

```java
public class ArticleActivity extends AppCompatActivity {
    private int articlesRead = 0;
    
    void onArticleCompleted() {
        articlesRead++;
        
        // Show ad every 2 articles
        if (articlesRead % 2 == 0) {
            adHelper.showInterstitial();
            // Load next article in callback
        } else {
            loadNextArticle();
        }
    }
}
```

---

## Ad Unit IDs

### Development (Test)

**LUÔN dùng test ad unit ID khi phát triển:**

```java
// Test Interstitial Ad Unit ID
private static final String TEST_INTERSTITIAL_ID = 
    "ca-app-pub-3940256099942544/1033173712";
```

### Production (Real)

**Tạo ad unit trong AdMob Console:**

1. Đăng nhập [AdMob Console](https://apps.admob.com)
2. Chọn app của bạn
3. Click "Ad units" → "Add ad unit"
4. Chọn "Interstitial"
5. Cấu hình và lấy ad unit ID

```java
// Production Interstitial Ad Unit ID
private static final String PROD_INTERSTITIAL_ID = 
    "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY";
```

**⚠️ QUAN TRỌNG:**
- KHÔNG dùng test ID trong production
- KHÔNG dùng real ID khi development (có thể bị ban)
- Thay đổi ID trước khi release

---

## Lifecycle Management

```java
public class MainActivity extends AppCompatActivity {
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, adUnitId);
        adHelper.loadInterstitial();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup để tránh memory leaks
        if (adHelper != null) {
            adHelper.cleanup();
        }
    }
}
```

---

## Troubleshooting

### Ad không hiển thị

**Kiểm tra:**
1. ✅ Đã gọi `initInterstitial()` chưa?
2. ✅ Đã gọi `loadInterstitial()` chưa?
3. ✅ Ad unit ID có đúng không?
4. ✅ Consent flow đã hoàn thành chưa?
5. ✅ Có kết nối internet không?
6. ✅ Xem logs để biết lỗi cụ thể

**Debug:**
```java
// Check if ad is ready
if (adHelper.isInterstitialReady()) {
    Log.d(TAG, "Interstitial is ready");
} else {
    Log.d(TAG, "Interstitial is NOT ready");
}

// Show ad với debug logs
adHelper.showInterstitial(); // Check logcat for details
```

### Ad load chậm

**Giải pháp:**
1. Preload càng sớm càng tốt
2. Load ngay sau khi dismiss ad trước
3. Kiểm tra network speed
4. Test với test ad unit ID (luôn có inventory)
5. Kiểm tra có fill rate thấp không (trong AdMob Console)

### Memory leaks

**Prevention:**
1. ✅ AdmobHelper đã dùng `WeakReference`
2. ✅ Gọi `cleanup()` trong `onDestroy()`
3. ✅ Không hold strong reference đến Activity
4. ✅ Clear callbacks khi destroy

---

## Advanced Features

### Custom frequency capping

```java
public class AdFrequencyManager {
    private static final int MAX_ADS_PER_HOUR = 5;
    private static final long ONE_HOUR_MS = 3600000;
    private Queue<Long> adShowTimes = new LinkedList<>();
    
    public boolean canShowAd() {
        long currentTime = System.currentTimeMillis();
        
        // Remove old timestamps (older than 1 hour)
        while (!adShowTimes.isEmpty() && 
               currentTime - adShowTimes.peek() > ONE_HOUR_MS) {
            adShowTimes.poll();
        }
        
        // Check if under limit
        return adShowTimes.size() < MAX_ADS_PER_HOUR;
    }
    
    public void recordAdShown() {
        adShowTimes.offer(System.currentTimeMillis());
    }
}

// Usage
if (frequencyManager.canShowAd()) {
    adHelper.showInterstitial();
    frequencyManager.recordAdShown();
}
```

### A/B Testing different frequencies

```java
// Remote config
int adFrequency = FirebaseRemoteConfig.getInstance()
    .getLong("interstitial_frequency");

if (levelCount % adFrequency == 0) {
    adHelper.showInterstitial();
}
```

### Analytics tracking

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onAdImpression(String adFormat, String adUnitId, 
                               String adSourceName, double revenue) {
        if (adFormat.equals("INTERSTITIAL")) {
            // Firebase Analytics
            Bundle bundle = new Bundle();
            bundle.putString("ad_format", adFormat);
            bundle.putString("ad_source", adSourceName);
            bundle.putDouble("ad_revenue", revenue);
            FirebaseAnalytics.getInstance(context)
                .logEvent("ad_impression", bundle);
            
            // Adjust
            AdjustAdRevenue adjustAdRevenue = new AdjustAdRevenue(
                AdjustConfig.AD_REVENUE_ADMOB);
            adjustAdRevenue.setRevenue(revenue, "USD");
            adjustAdRevenue.setAdRevenueNetwork(adSourceName);
            Adjust.trackAdRevenue(adjustAdRevenue);
        }
    }
});
```

---

## Complete Example

Xem file `INTERSTITIAL_USAGE_EXAMPLE.java` để có ví dụ đầy đủ về cách sử dụng Interstitial Ad trong các tình huống thực tế.

---

## Related Documentation

- [AdMob Interstitial Official Docs](https://developers.google.com/admob/android/interstitial)
- [AdMob Policy](https://support.google.com/admob/answer/6128543)
- [Banner Ad Guide](README.md)
- [MREC Ad Guide](README_MREC.md)
- [App Open Ad Guide](README_AOA.md)

---

## Changelog

### v1.0.0 (Current)
- ✅ Initial Interstitial Ad integration
- ✅ Thread-safe implementation
- ✅ Memory leak prevention với WeakReference
- ✅ Auto-reload after dismissed
- ✅ Full callback support
- ✅ Revenue tracking với OnPaidEventListener

---

## Support

Nếu gặp vấn đề hoặc có câu hỏi, vui lòng:
1. Kiểm tra phần Troubleshooting ở trên
2. Xem logs trong Logcat với filter "AdmobHelper"
3. Kiểm tra AdMob Console để xem ad performance
4. Liên hệ team để được hỗ trợ

---

**Happy Coding! 🚀**


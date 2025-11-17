# Rewarded Ad Integration Guide

## Tổng quan

Rewarded Ad là quảng cáo có thưởng, user chủ động xem để nhận phần thưởng (coins, lives, unlock items, v.v.). Đây là định dạng ad ít invasive nhất và thường mang lại revenue cao nhất vì user engagement tốt.

## Tính năng

✅ **User opt-in**: User chủ động bấm nút để xem  
✅ **Memory-safe**: Sử dụng WeakReference để tránh memory leaks  
✅ **Thread-safe**: Synchronized và volatile flags  
✅ **Auto-reload**: Tự động load ad mới sau khi dismissed  
✅ **Revenue tracking**: OnPaidEventListener integration  
✅ **Full callbacks**: FullScreenContentCallback + OnUserEarnedRewardListener  
✅ **Single-use**: Mỗi ad chỉ dùng 1 lần, đảm bảo fairness  

## Cài đặt nhanh

### 1. Khởi tạo

```java
AdmobHelper adHelper = AdmobHelper.getInstance();

// Test Ad Unit ID (development)
String testAdUnitId = "ca-app-pub-3940256099942544/5224354917";

// Initialize rewarded
adHelper.initRewarded(this, testAdUnitId);
```

### 2. Load Ad (Preload)

```java
// Load sớm để khi user bấm nút không phải chờ
adHelper.loadRewarded();
```

### 3. Setup Callback để nhận thưởng

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onUserEarnedReward(String type, int amount) {
        // User đã xem đủ ad, cộng thưởng
        userCoins += amount;
        showMessage("You earned " + amount + " " + type);
    }
    
    // ... other callbacks
});
```

### 4. Show Ad (User bấm nút)

```java
// Show when user clicks button
button.setOnClickListener(v -> {
    if (adHelper.isRewardedReady()) {
        adHelper.showRewarded();
    } else {
        Toast.makeText(this, "Ad is loading...", Toast.LENGTH_SHORT).show();
        adHelper.loadRewarded();
    }
});
```

---

## API Reference

### `initRewarded(Activity activity, String adUnitId)`

Khởi tạo Rewarded Ad với ad unit ID.

**Parameters:**
- `activity`: Current activity context
- `adUnitId`: Ad unit ID từ AdMob console

**Example:**
```java
adHelper.initRewarded(this, "ca-app-pub-3940256099942544/5224354917");
```

---

### `loadRewarded()`

Load rewarded ad. Tự động kiểm tra nếu ad đã loaded hoặc đang loading.

**Best Practice:**
- Gọi càng sớm càng tốt (onCreate hoặc khi vào màn hình)
- Gọi lại sau khi ad dismissed (auto-reload)
- Không cần gọi nhiều lần (có auto-check)

**Example:**
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adHelper.initRewarded(this, adUnitId);
    adHelper.loadRewarded(); // Preload
}
```

---

### `showRewarded()`

Hiển thị rewarded ad. Tự động kiểm tra ad availability.

**Behavior:**
- Nếu ad ready: Hiển thị ngay
- Nếu ad chưa ready: Log warning và load cho lần sau
- Auto-reload sau khi dismissed
- Chỉ cộng thưởng nếu user xem đủ ad (trong `onUserEarnedReward`)

**Example:**
```java
// Show when user clicks "Watch Ad" button
buttonWatchAd.setOnClickListener(v -> {
    adHelper.showRewarded();
});
```

---

### `isRewardedReady()`

Kiểm tra xem rewarded ad có sẵn sàng hiển thị không.

**Returns:** `boolean` - true nếu ad đã loaded và ready

**Example:**
```java
if (adHelper.isRewardedReady()) {
    buttonWatchAd.setEnabled(true);
    buttonWatchAd.setText("Watch Ad to Earn 50 Coins");
} else {
    buttonWatchAd.setEnabled(false);
    buttonWatchAd.setText("Loading Ad...");
}
```

---

## Callbacks

### Reward Callback (QUAN TRỌNG!)

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onUserEarnedReward(String type, int amount) {
        // CHỈ cộng thưởng trong callback này!
        // Callback này CHỈ được gọi khi user xem đủ ad
        
        userCoins += amount;
        updateUI();
        showRewardMessage("You earned " + amount + " " + type);
        
        // Save to database/preferences
        saveUserCoins();
    }
    
    // ... other callbacks
});
```

### Full Callbacks Example

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onUserEarnedReward(String type, int amount) {
        // User earned reward (xem đủ ad)
        grantReward(type, amount);
    }
    
    @Override
    public void onAdDisplayFullScreenContent(int adType) {
        if (adType == 2) { // 2 = Rewarded
            // Ad is showing
            pauseGame();
        }
    }
    
    @Override
    public void onAdDismissedFullScreenContent(int adType) {
        if (adType == 2) { // 2 = Rewarded
            // User dismissed ad
            resumeGame();
            // Auto-reloaded by AdmobHelper
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
```

### Callback Parameters

**adType:**
- `0` = App Open Ad
- `1` = Interstitial Ad
- `2` = Rewarded Ad

**adFormat:**
- `"REWARDED"` = Rewarded Ad
- `"INTERSTITIAL"` = Interstitial Ad
- `"COLLAPSIBLE_BANNER"` = Banner Ad
- `"MREC"` = Medium Rectangle
- `"App open"` = App Open Ad

**Reward Parameters:**
- `type`: String - Loại thưởng (e.g., "coins", "lives")
- `amount`: int - Số lượng thưởng

---

## Best Practices

### 1. User Opt-In - Luôn là lựa chọn của user

✅ **ĐÚNG:**
```java
// Clear button với messaging rõ ràng
button.setText("Watch Ad to Earn 50 Coins");
button.setOnClickListener(v -> {
    if (adHelper.isRewardedReady()) {
        adHelper.showRewarded();
    }
});
```

❌ **SAI:**
```java
// Auto-show ad mà không hỏi user
@Override
protected void onResume() {
    super.onResume();
    adHelper.showRewarded(); // WRONG! User phải opt-in
}
```

### 2. Clear Messaging - Nói rõ user sẽ nhận được gì

✅ **ĐÚNG:**
- "Watch a short video to earn 50 coins"
- "Get 3 extra lives by watching an ad"
- "Unlock this premium feature with a video ad"
- "Continue playing after watching an ad"

❌ **SAI:**
- "Click here"
- "Free stuff"
- "Watch video" (không nói được gì)

### 3. Don't Force - Luôn có lựa chọn khác

```java
// Example: Game Over screen
void showGameOverOptions() {
    // Option 1: Watch ad to continue
    buttonContinue.setText("Watch Ad to Continue");
    buttonContinue.setOnClickListener(v -> {
        if (adHelper.isRewardedReady()) {
            adHelper.showRewarded();
        }
    });
    
    // Option 2: Restart without ad (free)
    buttonRestart.setText("Restart Level (Free)");
    buttonRestart.setOnClickListener(v -> {
        restartLevel();
    });
    
    // Option 3: Purchase (IAP)
    buttonPurchase.setText("Buy Unlimited Continues");
    buttonPurchase.setOnClickListener(v -> {
        showPurchaseDialog();
    });
}
```

### 4. Preload Strategy

```java
// Load sớm nhất có thể
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    adHelper.initRewarded(this, adUnitId);
    adHelper.loadRewarded(); // Load ngay
}

// Load khi vào màn hình cần show ad
@Override
protected void onResume() {
    super.onResume();
    
    if (!adHelper.isRewardedReady()) {
        adHelper.loadRewarded();
    }
}

// Auto-reload được handle bởi AdmobHelper sau khi dismissed
```

### 5. Frequency Limits

```java
// Giới hạn số lần show per day
private static final int MAX_DAILY_REWARDS = 10;
private int dailyRewardsCount = 0;

private void showRewardedWithLimit() {
    if (dailyRewardsCount >= MAX_DAILY_REWARDS) {
        showMessage("Daily limit reached! Come back tomorrow.");
        showPurchaseDialog(); // Offer alternative
        return;
    }
    
    if (adHelper.isRewardedReady()) {
        adHelper.showRewarded();
    }
}

// Trong callback
@Override
public void onUserEarnedReward(String type, int amount) {
    dailyRewardsCount++;
    grantReward(type, amount);
    
    int remaining = MAX_DAILY_REWARDS - dailyRewardsCount;
    showMessage("Earned! " + remaining + " ads left today");
}
```

### 6. Cooldown Between Ads

```java
private static final long COOLDOWN_MS = 300000; // 5 minutes
private long lastRewardTime = 0;

private void showRewardedWithCooldown() {
    long currentTime = System.currentTimeMillis();
    long timeSince = currentTime - lastRewardTime;
    
    if (timeSince < COOLDOWN_MS) {
        long remainingSec = (COOLDOWN_MS - timeSince) / 1000;
        showMessage("Please wait " + remainingSec + " seconds");
        return;
    }
    
    if (adHelper.isRewardedReady()) {
        adHelper.showRewarded();
        lastRewardTime = currentTime;
    }
}
```

---

## Use Cases

### Game App

```java
public class GameActivity extends AppCompatActivity {
    private int userLives = 3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AdmobHelper adHelper = AdmobHelper.getInstance();
        adHelper.initRewarded(this, REWARDED_AD_UNIT_ID);
        adHelper.loadRewarded();
        
        setupRewardCallbacks();
    }
    
    void onGameOver() {
        // Show option to continue
        showContinueDialog();
    }
    
    void showContinueDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("Watch ad to continue or restart?")
            .setPositiveButton("Watch Ad", (dialog, which) -> {
                if (adHelper.isRewardedReady()) {
                    adHelper.showRewarded();
                } else {
                    Toast.makeText(this, "Ad not ready", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Restart", (dialog, which) -> {
                restartLevel();
            })
            .show();
    }
    
    void setupRewardCallbacks() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onUserEarnedReward(String type, int amount) {
                // Continue game
                userLives += amount;
                continueGame();
            }
            
            // ... other callbacks
        });
    }
}
```

### Utility App

```java
public class EditorActivity extends AppCompatActivity {
    private boolean watermarkRemoved = false;
    
    void setupRemoveWatermarkButton() {
        buttonRemoveWatermark.setOnClickListener(v -> {
            if (adHelper.isRewardedReady()) {
                new AlertDialog.Builder(this)
                    .setTitle("Remove Watermark")
                    .setMessage("Watch a short video to remove watermark?")
                    .setPositiveButton("Watch", (dialog, which) -> {
                        adHelper.showRewarded();
                    })
                    .setNegativeButton("Buy Premium", (dialog, which) -> {
                        showPurchaseDialog();
                    })
                    .show();
            } else {
                showPurchaseDialog(); // Only IAP option
            }
        });
    }
    
    void setupRewardCallback() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onUserEarnedReward(String type, int amount) {
                watermarkRemoved = true;
                updatePreview();
                showMessage("Watermark removed!");
            }
            
            // ... other callbacks
        });
    }
}
```

### Content App

```java
public class ArticleActivity extends AppCompatActivity {
    private boolean premiumUnlocked = false;
    
    void showPremiumContent() {
        if (premiumUnlocked) {
            displayFullArticle();
            return;
        }
        
        // Show paywall with options
        new AlertDialog.Builder(this)
            .setTitle("Premium Content")
            .setMessage("Watch ad to unlock or subscribe?")
            .setPositiveButton("Watch Ad", (dialog, which) -> {
                if (adHelper.isRewardedReady()) {
                    adHelper.showRewarded();
                }
            })
            .setNeutralButton("Subscribe", (dialog, which) -> {
                showSubscriptionDialog();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    void setupRewardCallback() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onUserEarnedReward(String type, int amount) {
                premiumUnlocked = true;
                displayFullArticle();
            }
            
            // ... other callbacks
        });
    }
}
```

---

## Ad Unit IDs

### Development (Test)

**LUÔN dùng test ad unit ID khi phát triển:**

```java
// Test Rewarded Ad Unit ID
private static final String TEST_REWARDED_ID = 
    "ca-app-pub-3940256099942544/5224354917";
```

### Production (Real)

**Tạo ad unit trong AdMob Console:**

1. Đăng nhập [AdMob Console](https://apps.admob.com)
2. Chọn app của bạn
3. Click "Ad units" → "Add ad unit"
4. Chọn "Rewarded"
5. Configure reward (type và amount)
6. Lấy ad unit ID

```java
// Production Rewarded Ad Unit ID
private static final String PROD_REWARDED_ID = 
    "ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ";
```

**⚠️ QUAN TRỌNG:**
- KHÔNG dùng test ID trong production
- KHÔNG dùng real ID khi development
- Thay đổi ID trước khi release

---

## Reward Configuration

### In AdMob Console

Khi tạo ad unit, bạn cần configure:

1. **Reward Type**: String (e.g., "coins", "lives", "unlock")
2. **Reward Amount**: Integer (e.g., 50, 100, 1)

Example configurations:
- Type: "coins", Amount: 50
- Type: "lives", Amount: 3
- Type: "unlock", Amount: 1
- Type: "continue", Amount: 1

### Handle Different Reward Types

```java
@Override
public void onUserEarnedReward(String type, int amount) {
    switch (type.toLowerCase()) {
        case "coins":
        case "coin":
            userCoins += amount;
            showMessage("+" + amount + " coins!");
            break;
            
        case "lives":
        case "life":
            userLives += amount;
            showMessage("+" + amount + " lives!");
            break;
            
        case "unlock":
            unlockPremiumFeature();
            showMessage("Premium unlocked!");
            break;
            
        case "continue":
            continueGame();
            showMessage("Continue playing!");
            break;
            
        default:
            Log.w(TAG, "Unknown reward type: " + type);
            // Handle as generic reward
            grantGenericReward(type, amount);
            break;
    }
}
```

---

## Lifecycle Management

```java
public class MainActivity extends AppCompatActivity {
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adHelper = AdmobHelper.getInstance();
        adHelper.initRewarded(this, adUnitId);
        adHelper.loadRewarded();
        
        setupRewardCallbacks();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Update UI based on ad availability
        updateWatchAdButtonState();
        
        // Preload if not ready
        if (!adHelper.isRewardedReady()) {
            adHelper.loadRewarded();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup
        if (adHelper != null) {
            adHelper.cleanup();
        }
    }
    
    private void updateWatchAdButtonState() {
        if (adHelper.isRewardedReady()) {
            buttonWatchAd.setEnabled(true);
            buttonWatchAd.setText("Watch Ad to Earn Coins");
        } else {
            buttonWatchAd.setEnabled(false);
            buttonWatchAd.setText("Loading Ad...");
        }
    }
}
```

---

## Troubleshooting

### Ad không hiển thị

**Kiểm tra:**
1. ✅ Đã gọi `initRewarded()` chưa?
2. ✅ Đã gọi `loadRewarded()` chưa?
3. ✅ Ad unit ID có đúng không?
4. ✅ Consent flow đã hoàn thành chưa?
5. ✅ Có kết nối internet không?
6. ✅ Xem logs để biết lỗi cụ thể

**Debug:**
```java
if (adHelper.isRewardedReady()) {
    Log.d(TAG, "Rewarded is ready");
    adHelper.showRewarded();
} else {
    Log.d(TAG, "Rewarded is NOT ready");
    adHelper.loadRewarded();
}
```

### User không nhận được thưởng

**Nguyên nhân:**
- User không xem đủ ad (thoát sớm)
- Không implement `onUserEarnedReward` callback
- Logic cộng thưởng bị lỗi
- Race condition

**Giải pháp:**
```java
@Override
public void onUserEarnedReward(String type, int amount) {
    // CRITICAL: CHỈ cộng thưởng ở đây
    Log.d(TAG, "User earned: " + amount + " " + type);
    
    // Cộng thưởng
    userCoins += amount;
    
    // Save ngay
    saveUserCoins();
    
    // Update UI on main thread
    runOnUiThread(() -> {
        updateCoinsDisplay();
        showRewardMessage();
    });
}
```

### Button disabled mãi

**Nguyên nhân:**
- Ad không load được
- Không update UI sau khi load

**Giải pháp:**
```java
// Check ad ready state periodically
private void startAdReadyChecker() {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.postDelayed(new Runnable() {
        @Override
        public void run() {
            updateWatchAdButtonState();
            handler.postDelayed(this, 2000); // Check every 2s
        }
    }, 2000);
}
```

---

## Advanced Features

### Server-Side Verification (SSV)

Để chống gian lận, bạn có thể enable SSV:

1. Setup callback URL in AdMob Console
2. Verify reward on your server
3. Grant reward sau khi server confirm

**Note:** AdmobHelper chưa implement SSV callback. Bạn cần implement custom nếu cần.

### Multiple Reward Types

```java
// Configure different buttons cho different rewards
buttonEarnCoins.setOnClickListener(v -> {
    // Show ad với reward type "coins"
    adHelper.showRewarded();
});

buttonEarnLives.setOnClickListener(v -> {
    // Show ad với reward type "lives"
    adHelper.showRewarded();
});

// Handle in callback
@Override
public void onUserEarnedReward(String type, int amount) {
    if (type.equals("coins")) {
        userCoins += amount;
    } else if (type.equals("lives")) {
        userLives += amount;
    }
}
```

### A/B Testing Rewards

```java
// Remote config for reward amount
int rewardAmount = FirebaseRemoteConfig.getInstance()
    .getLong("reward_coins_amount");

// Display in UI
button.setText("Watch Ad to Earn " + rewardAmount + " Coins");
```

---

## Complete Example

Xem file `REWARDED_USAGE_EXAMPLE.java` để có ví dụ đầy đủ về cách sử dụng Rewarded Ad trong các tình huống thực tế.

---

## Related Documentation

- [AdMob Rewarded Official Docs](https://developers.google.com/admob/android/rewarded)
- [AdMob Policy - Rewarded](https://support.google.com/admob/answer/9452652)
- [Interstitial Ad Guide](README_INTERSTITIAL.md)
- [Banner Ad Guide](README.md)

---

## Changelog

### v2.2 (Current)
- ✅ Initial Rewarded Ad integration
- ✅ Thread-safe implementation
- ✅ Memory leak prevention với WeakReference
- ✅ Auto-reload after dismissed
- ✅ Full callback support
- ✅ Revenue tracking với OnPaidEventListener
- ✅ OnUserEarnedRewardListener integration

---

## Support

Nếu gặp vấn đề hoặc có câu hỏi:
1. Kiểm tra phần Troubleshooting ở trên
2. Xem logs trong Logcat với filter "AdmobHelper"
3. Kiểm tra AdMob Console để xem ad performance
4. Review `REWARDED_USAGE_EXAMPLE.java` cho examples

---

**Happy Rewarding! 🎁💰**


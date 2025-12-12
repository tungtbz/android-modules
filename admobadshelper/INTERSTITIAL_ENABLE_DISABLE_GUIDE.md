# Interstitial Enable/Disable Feature Guide

## Tổng quan

Tính năng Enable/Disable cho phép bạn kiểm soát việc hiển thị Interstitial Ads một cách linh hoạt trong ứng dụng của mình. Tính năng này hữu ích cho:

- 🎖️ **Premium Users**: Tắt ads cho người dùng trả phí
- 📚 **Tutorial/Onboarding**: Tắt ads trong quá trình hướng dẫn
- 💳 **Critical Flows**: Tắt ads trong checkout, payment
- 🎮 **Game Modes**: Tắt ads trong competitive mode
- ⏰ **Time-based**: Tắt ads tạm thời
- 🔧 **Remote Control**: Điều khiển từ xa qua Firebase Remote Config

---

## API Methods

### `disableInterstitial()`

Tắt Interstitial Ads. Khi disabled:
- ❌ `loadInterstitial()` sẽ không load ad
- ❌ `showInterstitial()` sẽ không hiển thị ad
- ✅ Log message: "Interstitial ads are disabled"

```java
AdmobHelper.getInstance().disableInterstitial();
```

---

### `enableInterstitial()`

Bật Interstitial Ads. Khi enabled:
- ✅ `loadInterstitial()` hoạt động bình thường
- ✅ `showInterstitial()` hiển thị ad nếu ready
- ✅ Log message: "Interstitial ads enabled"

```java
AdmobHelper.getInstance().enableInterstitial();
```

---

### `isInterstitialEnabled()`

Kiểm tra trạng thái enable/disable.

**Returns:** `boolean` - true nếu enabled, false nếu disabled

```java
if (adHelper.isInterstitialEnabled()) {
    // Ads are enabled
} else {
    // Ads are disabled
}
```

---

## Use Cases & Examples

### 1. Premium Users (Ad-Free)

```java
public class MainActivity extends AppCompatActivity {
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, INTERSTITIAL_AD_UNIT_ID);
        
        // Check user's premium status
        if (isPremiumUser()) {
            adHelper.disableInterstitial();
            Log.d(TAG, "Premium user - ads disabled");
        } else {
            adHelper.enableInterstitial();
            adHelper.loadInterstitial(); // Preload for free users
        }
    }
    
    // When user purchases premium
    private void onPurchaseSuccess() {
        adHelper.disableInterstitial();
        Toast.makeText(this, "Thank you for going premium! Ads removed.", 
                      Toast.LENGTH_LONG).show();
        
        // Save premium status
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("is_premium", true)
            .apply();
    }
    
    // When subscription expires
    private void onSubscriptionExpired() {
        adHelper.enableInterstitial();
        adHelper.loadInterstitial();
        
        Toast.makeText(this, "Your subscription has expired. Ads will resume.", 
                      Toast.LENGTH_LONG).show();
        
        // Update status
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("is_premium", false)
            .apply();
    }
    
    private boolean isPremiumUser() {
        return getSharedPreferences("prefs", MODE_PRIVATE)
                .getBoolean("is_premium", false);
    }
}
```

---

### 2. Tutorial/Onboarding Flow

```java
public class TutorialActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        
        // Disable ads during tutorial
        AdmobHelper.getInstance().disableInterstitial();
        
        startTutorialSteps();
    }
    
    private void onTutorialCompleted() {
        // Mark tutorial as completed
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("tutorial_completed", true)
            .apply();
        
        // Re-enable ads
        AdmobHelper adHelper = AdmobHelper.getInstance();
        adHelper.enableInterstitial();
        adHelper.loadInterstitial(); // Start loading
        
        // Navigate to main screen
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure ads are enabled when leaving
        AdmobHelper.getInstance().enableInterstitial();
    }
}
```

---

### 3. Critical User Flows (Checkout/Payment)

```java
public class CheckoutActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        
        // CRITICAL: Disable ads during payment flow
        AdmobHelper.getInstance().disableInterstitial();
        
        setupPaymentForm();
    }
    
    private void onPaymentCompleted() {
        // Process payment
        processOrder();
        
        // Show success message
        showSuccessDialog();
        
        // Navigate back to main
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Re-enable ads when leaving checkout
        AdmobHelper.getInstance().enableInterstitial();
    }
}
```

---

### 4. Remote Config Control

```java
public class MainActivity extends AppCompatActivity {
    private FirebaseRemoteConfig remoteConfig;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Setup Firebase Remote Config
        remoteConfig = FirebaseRemoteConfig.getInstance();
        
        // Set defaults
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("interstitial_ads_enabled", true);
        remoteConfig.setDefaultsAsync(defaults);
        
        // Fetch and apply
        fetchRemoteConfig();
    }
    
    private void fetchRemoteConfig() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    applyRemoteConfig();
                }
            });
    }
    
    private void applyRemoteConfig() {
        boolean adsEnabled = remoteConfig.getBoolean("interstitial_ads_enabled");
        
        AdmobHelper adHelper = AdmobHelper.getInstance();
        
        if (adsEnabled) {
            adHelper.enableInterstitial();
            adHelper.loadInterstitial();
            Log.d(TAG, "Interstitial ads enabled via remote config");
        } else {
            adHelper.disableInterstitial();
            Log.d(TAG, "Interstitial ads disabled via remote config");
        }
    }
}
```

---

### 5. Time-based Control

```java
public class RewardActivity extends AppCompatActivity {
    private static final long AD_FREE_DURATION_MS = 1800000; // 30 minutes
    
    private void grantAdFreeReward() {
        AdmobHelper adHelper = AdmobHelper.getInstance();
        
        // Disable ads temporarily
        adHelper.disableInterstitial();
        
        Toast.makeText(this, "Ads disabled for 30 minutes!", 
                      Toast.LENGTH_LONG).show();
        
        // Save timestamp
        long disabledUntil = System.currentTimeMillis() + AD_FREE_DURATION_MS;
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putLong("ads_disabled_until", disabledUntil)
            .apply();
        
        // Schedule re-enable
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            adHelper.enableInterstitial();
            adHelper.loadInterstitial();
            
            Toast.makeText(this, "Ad-free period expired. Ads resumed.", 
                          Toast.LENGTH_SHORT).show();
        }, AD_FREE_DURATION_MS);
    }
    
    private void checkAdFreeStatus() {
        long disabledUntil = getSharedPreferences("prefs", MODE_PRIVATE)
                .getLong("ads_disabled_until", 0);
        
        long currentTime = System.currentTimeMillis();
        
        if (currentTime < disabledUntil) {
            // Still in ad-free period
            AdmobHelper.getInstance().disableInterstitial();
            
            long remainingMs = disabledUntil - currentTime;
            scheduleReEnable(remainingMs);
        } else {
            // Ad-free period expired
            AdmobHelper.getInstance().enableInterstitial();
        }
    }
    
    private void scheduleReEnable(long delayMs) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AdmobHelper.getInstance().enableInterstitial();
            AdmobHelper.getInstance().loadInterstitial();
        }, delayMs);
    }
}
```

---

### 6. Game Mode Control

```java
public class GameActivity extends AppCompatActivity {
    private enum GameMode {
        CASUAL,      // Ads enabled
        COMPETITIVE  // Ads disabled
    }
    
    private GameMode currentMode = GameMode.CASUAL;
    
    private void setGameMode(GameMode mode) {
        currentMode = mode;
        AdmobHelper adHelper = AdmobHelper.getInstance();
        
        switch (mode) {
            case CASUAL:
                adHelper.enableInterstitial();
                adHelper.loadInterstitial();
                Log.d(TAG, "Casual mode - ads enabled");
                break;
                
            case COMPETITIVE:
                adHelper.disableInterstitial();
                Log.d(TAG, "Competitive mode - ads disabled");
                break;
        }
    }
    
    private void onModeSelected(GameMode mode) {
        setGameMode(mode);
        startGame();
    }
}
```

---

### 7. IAP Integration (Watch Ad or Pay)

```java
public class UnlockActivity extends AppCompatActivity {
    
    private void showUnlockOptions() {
        AdmobHelper adHelper = AdmobHelper.getInstance();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unlock Premium Feature");
        builder.setMessage("Choose how to unlock:");
        
        // Option 1: Watch ad (if enabled and ready)
        if (adHelper.isInterstitialEnabled() && adHelper.isInterstitialReady()) {
            builder.setPositiveButton("Watch Ad (Free)", (dialog, which) -> {
                adHelper.showInterstitial();
                
                // Grant unlock in callback
                adHelper.SetAdsCallback(new IAdmobAdListener() {
                    @Override
                    public void onAdDismissedFullScreenContent(int adType) {
                        if (adType == 1) {
                            unlockFeature();
                        }
                    }
                    
                    // ... other callback methods
                });
            });
        }
        
        // Option 2: Pay with IAP
        builder.setNeutralButton("Purchase ($0.99)", (dialog, which) -> {
            launchPurchaseFlow();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void unlockFeature() {
        Toast.makeText(this, "Feature unlocked!", Toast.LENGTH_SHORT).show();
        // Grant the feature
    }
    
    private void launchPurchaseFlow() {
        // Google Play Billing implementation
    }
}
```

---

## Best Practices

### ✅ DO

1. **Always re-enable** ads khi rời khỏi critical flows:
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    AdmobHelper.getInstance().enableInterstitial();
}
```

2. **Check status** trước khi show:
```java
if (adHelper.isInterstitialEnabled() && adHelper.isInterstitialReady()) {
    adHelper.showInterstitial();
}
```

3. **Save state** trong SharedPreferences:
```java
SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
prefs.edit().putBoolean("ads_disabled", true).apply();
```

4. **Log actions** để debug:
```java
Log.d(TAG, "Interstitial ads disabled - premium user");
```

---

### ❌ DON'T

1. **Không quên re-enable** sau critical flows
2. **Không disable vĩnh viễn** mà không có lý do (premium, etc.)
3. **Không disable** trong background/foreground lifecycle
4. **Không rely** hoàn toàn vào remote config (có fallback)

---

## Testing Checklist

### Premium Flow
- [ ] Ads disabled khi user purchases
- [ ] Ads re-enabled khi subscription expires
- [ ] State được persist qua app restarts
- [ ] UI cập nhật phù hợp (remove ad buttons)

### Tutorial Flow
- [ ] Ads disabled trong tutorial
- [ ] Ads enabled sau tutorial complete
- [ ] Ads enabled nếu user skip tutorial
- [ ] State restore đúng khi rotate device

### Critical Flows
- [ ] Ads disabled trong checkout
- [ ] Ads re-enabled khi back/cancel
- [ ] Ads re-enabled trong onDestroy()
- [ ] No ads interrupt payment flow

### Remote Config
- [ ] Default value hoạt động
- [ ] Remote value được apply
- [ ] Fallback khi fetch fails
- [ ] Update without app restart

---

## Thread Safety

Tính năng này là **thread-safe**:

```java
// In AdmobHelper.java
private volatile boolean interstitialAdsEnabled = true;
```

- `volatile` đảm bảo visibility across threads
- Safe to call từ bất kỳ thread nào
- No synchronization needed cho read/write

---

## Troubleshooting

### Ads vẫn show sau khi disable

**Nguyên nhân:** Ad đã được load trước khi disable

**Giải pháp:** Disable trước khi init/load:
```java
adHelper.disableInterstitial();
adHelper.initInterstitial(this, adUnitId);
// Ad sẽ không load
```

---

### State không persist

**Nguyên nhân:** Singleton bị reset

**Giải pháp:** Save vào SharedPreferences:
```java
// Save
getSharedPreferences("prefs", MODE_PRIVATE)
    .edit()
    .putBoolean("ads_enabled", false)
    .apply();

// Restore in onCreate
boolean adsEnabled = getSharedPreferences("prefs", MODE_PRIVATE)
    .getBoolean("ads_enabled", true);
    
if (!adsEnabled) {
    adHelper.disableInterstitial();
}
```

---

### Remote config không apply

**Nguyên nhân:** Fetch chưa complete

**Giải pháp:** Set defaults và check callback:
```java
remoteConfig.setDefaultsAsync(defaults);
remoteConfig.fetchAndActivate()
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            applyRemoteConfig();
        }
    });
```

---

## Performance Impact

- ✅ **Minimal overhead**: Chỉ check boolean flag
- ✅ **No memory impact**: Không tạo object mới
- ✅ **Thread-safe**: Không cần synchronization
- ✅ **Fast**: Instant enable/disable

---

## Comparison với AOA

| Feature | Interstitial | App Open Ad |
|---------|-------------|-------------|
| Disable method | `disableInterstitial()` | `DisableAOA()` |
| Enable method | `enableInterstitial()` | `EnableAOA()` |
| Check method | `isInterstitialEnabled()` | `canShowAOA()` |
| Default state | Enabled | Enabled |
| Thread-safe | ✅ Yes | ✅ Yes |

---

## Related Documentation

- [README_INTERSTITIAL.md](README_INTERSTITIAL.md) - Complete integration guide
- [INTERSTITIAL_USAGE_EXAMPLE.java](INTERSTITIAL_USAGE_EXAMPLE.java) - Usage examples
- [CHANGELOG.md](CHANGELOG.md) - Version history

---

## Support

Nếu có vấn đề hoặc câu hỏi:
1. Xem phần Troubleshooting ở trên
2. Check logs với filter "AdmobHelper"
3. Verify thread-safe implementation
4. Liên hệ team support

---

**Version:** 2.1.1  
**Last Updated:** November 17, 2025

**Happy Coding! 🚀**







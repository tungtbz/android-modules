# Rewarded Ad - Quick Start Guide

## 4 Bước Đơn Giản

### 1️⃣ Initialize (onCreate)

```java
AdmobHelper adHelper = AdmobHelper.getInstance();

// Test Ad Unit ID (development)
String adUnitId = "ca-app-pub-3940256099942544/5224354917";

adHelper.initRewarded(this, adUnitId);
adHelper.loadRewarded(); // Preload
```

### 2️⃣ Setup Callback (nhận thưởng)

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onUserEarnedReward(int rewardCode, String type, int amount) {
        // CHỈ cộng thưởng ở đây!
        // rewardCode: custom code passed when showing ad
        userCoins += amount;
        updateUI();
        showMessage("Earned " + amount + " " + type);
    }
    
    // ... other callback methods (implement tất cả)
});
```

### 3️⃣ Show Ad (User bấm nút)

```java
// Setup button
button.setText("Watch Ad to Earn 50 Coins");
button.setOnClickListener(v -> {
    if (adHelper.isRewardedReady()) {
        // Method 1: Simple (backward compatible)
        adHelper.showRewarded();
        
        // Method 2: With reward code to identify reward type
        // adHelper.showRewarded(REWARD_CODE_COINS);
    } else {
        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
        adHelper.loadRewarded();
    }
});
```

### 4️⃣ Cleanup (onDestroy)

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    adHelper.cleanup(); // Prevent memory leaks
}
```

---

## Complete Example

```java
public class MainActivity extends AppCompatActivity {
    private AdmobHelper adHelper;
    private int userCoins = 100;
    private TextView textViewCoins;
    private Button buttonWatchAd;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        textViewCoins = findViewById(R.id.textViewCoins);
        buttonWatchAd = findViewById(R.id.buttonWatchAd);
        
        // Initialize
        adHelper = AdmobHelper.getInstance();
        adHelper.initRewarded(this, "ca-app-pub-3940256099942544/5224354917");
        adHelper.loadRewarded();
        
        // Setup callbacks
        setupCallbacks();
        
        // Setup button
        setupButton();
        
        // Update UI
        updateCoinsDisplay();
    }
    
    private void setupCallbacks() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onUserEarnedReward(int rewardCode, String type, int amount) {
                // User earned reward!
                // rewardCode: to identify which reward was given
                userCoins += amount;
                updateCoinsDisplay();
                Toast.makeText(MainActivity.this, 
                    "You earned " + amount + " " + type, 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAdDisplayFullScreenContent(int adType) {
                if (adType == 2) { // Rewarded
                    buttonWatchAd.setEnabled(false);
                }
            }
            
            @Override
            public void onAdDismissedFullScreenContent(int adType) {
                if (adType == 2) { // Rewarded
                    buttonWatchAd.setEnabled(true);
                    updateButtonState();
                }
            }
            
            @Override
            public void onAdClicked() {}
            
            @Override
            public void onAdImpression(String f, String u, String n, double r) {}
            
            @Override
            public void onAOAFailedToLoad() {}
            
            @Override
            public void onBannerLoaded() {}
            
            @Override
            public void onBannerCollapDisplay() {}
        });
    }
    
    private void setupButton() {
        buttonWatchAd.setOnClickListener(v -> {
            if (adHelper.isRewardedReady()) {
                adHelper.showRewarded();
            } else {
                Toast.makeText(this, "Ad is loading...", Toast.LENGTH_SHORT).show();
                adHelper.loadRewarded();
            }
        });
        
        updateButtonState();
    }
    
    private void updateButtonState() {
        if (adHelper.isRewardedReady()) {
            buttonWatchAd.setEnabled(true);
            buttonWatchAd.setText("Watch Ad to Earn 50 Coins");
        } else {
            buttonWatchAd.setEnabled(false);
            buttonWatchAd.setText("Loading Ad...");
        }
    }
    
    private void updateCoinsDisplay() {
        textViewCoins.setText("Coins: " + userCoins);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        adHelper.cleanup();
    }
}
```

---

## API Methods

| Method | Description |
|--------|-------------|
| `initRewarded(Activity, String)` | Initialize với ad unit ID |
| `loadRewarded()` | Load ad (với auto-loading prevention) |
| `showRewarded()` | Show ad (với availability check) |
| `isRewardedReady()` | Check if ad is ready |

---

## Callbacks - Implement Interface Đầy Đủ

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onUserEarnedReward(int rewardCode, String type, int amount) {
        // QUAN TRỌNG: Cộng thưởng ở đây!
        // rewardCode: custom code to identify reward type
        userCoins += amount;
    }
    
    @Override
    public void onAdDisplayFullScreenContent(int adType) {
        // Ad showing (adType: 0=AOA, 1=Interstitial, 2=Rewarded)
    }
    
    @Override
    public void onAdDismissedFullScreenContent(int adType) {
        // Ad dismissed
    }
    
    @Override
    public void onAdClicked() {
        // Ad clicked
    }
    
    @Override
    public void onAdImpression(String adFormat, String adUnitId, 
                               String adSourceName, double revenue) {
        // Ad impression (for revenue tracking)
    }
    
    @Override
    public void onAOAFailedToLoad() {
        // Not used for Rewarded
    }
    
    @Override
    public void onBannerLoaded() {
        // Not used for Rewarded
    }
    
    @Override
    public void onBannerCollapDisplay() {
        // Not used for Rewarded
    }
});
```

---

## Best Practices ✅

### ✅ DO
- ✅ User chủ động bấm nút (opt-in)
- ✅ Nói rõ user sẽ nhận được gì ("Earn 50 coins")
- ✅ Preload ad càng sớm càng tốt
- ✅ CHỈ cộng thưởng trong `onUserEarnedReward`
- ✅ Provide lựa chọn khác (IAP, gameplay)
- ✅ Giới hạn số lần per day
- ✅ Dùng test ad ID khi development
- ✅ Gọi cleanup() trong onDestroy()

### ❌ DON'T
- ❌ Auto-show ad mà không hỏi user
- ❌ Ép buộc phải xem ad
- ❌ Messaging không rõ ràng
- ❌ Cộng thưởng trước khi xem đủ ad
- ❌ Show quá nhiều ads
- ❌ Không có lựa chọn khác
- ❌ Dùng test ad ID trong production

---

## When to Show? ⏰

**Good Times (User opt-in):**
- ✅ Button "Earn Extra Coins"
- ✅ Button "Get Extra Lives"
- ✅ Button "Continue" after game over
- ✅ Button "Unlock Premium Item"
- ✅ Button "Remove Watermark"

**Bad Times:**
- ❌ Auto-show khi mở app
- ❌ Auto-show every X minutes
- ❌ Ép buộc để tiếp tục
- ❌ Không có option khác

---

## Messaging Examples 💬

### ✅ Good Messaging

```java
button.setText("Watch Ad to Earn 50 Coins");
button.setText("Get 3 Extra Lives (Video Ad)");
button.setText("Continue Playing (Watch Ad)");
button.setText("Unlock Premium (Free with Ad)");
```

### ❌ Bad Messaging

```java
button.setText("Click Here");  // Không rõ
button.setText("Free Coins");  // Không nói về ad
button.setText("Continue");    // Không nói phải xem ad
```

---

## Ad Unit ID Setup 🔑

```java
// TEST (development) - Always use this when testing
String TEST_ID = "ca-app-pub-3940256099942544/5224354917";

// PRODUCTION (release) - Create in AdMob Console
String PROD_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ";

// Use BuildConfig to switch
String adUnitId = BuildConfig.DEBUG ? TEST_ID : PROD_ID;
```

⚠️ **IMPORTANT:** 
- ALWAYS use TEST_ID during development!
- Create production ID in AdMob Console
- Configure reward type và amount in console

---

## Reward Configuration in AdMob

When creating ad unit in AdMob Console:
1. **Reward Type**: String (e.g., "coins", "lives")
2. **Reward Amount**: Integer (e.g., 50, 3, 1)

Handle in code:
```java
@Override
public void onUserEarnedReward(String type, int amount) {
    switch (type) {
        case "coins":
            userCoins += amount;
            break;
        case "lives":
            userLives += amount;
            break;
        // ... more types
    }
}
```

---

## Troubleshooting 🔧

### Button disabled mãi?

```java
// Check ad ready state
if (adHelper.isRewardedReady()) {
    Log.d(TAG, "Rewarded ready!");
} else {
    Log.d(TAG, "Rewarded NOT ready");
    adHelper.loadRewarded();
}

// Update UI periodically
Handler handler = new Handler(Looper.getMainLooper());
handler.postDelayed(new Runnable() {
    @Override
    public void run() {
        updateButtonState();
        handler.postDelayed(this, 2000);
    }
}, 2000);
```

### User không nhận thưởng?

```java
// CRITICAL: Chỉ cộng thưởng trong callback này!
@Override
public void onUserEarnedReward(String type, int amount) {
    Log.d(TAG, "User earned: " + amount + " " + type);
    
    // Cộng thưởng
    userCoins += amount;
    
    // Save immediately
    SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
    prefs.edit().putInt("coins", userCoins).apply();
    
    // Update UI
    runOnUiThread(() -> {
        updateCoinsDisplay();
    });
}
```

### Ad không load?

**Common Issues:**
1. ✅ Internet connected?
2. ✅ Ad unit ID correct?
3. ✅ Called initRewarded()?
4. ✅ Called loadRewarded()?
5. ✅ Consent flow completed?
6. ✅ Check logcat with filter "AdmobHelper"

---

## Frequency Control 🎚️

```java
// Limit to 10 ads per day
private static final int MAX_DAILY = 10;
private int dailyCount = 0;

private void showRewardedWithLimit() {
    if (dailyCount >= MAX_DAILY) {
        Toast.makeText(this, "Daily limit reached!", Toast.LENGTH_SHORT).show();
        showPurchaseDialog(); // Offer IAP
        return;
    }
    
    if (adHelper.isRewardedReady()) {
        adHelper.showRewarded();
    }
}

// In callback
@Override
public void onUserEarnedReward(String type, int amount) {
    dailyCount++;
    userCoins += amount;
    
    int remaining = MAX_DAILY - dailyCount;
    Toast.makeText(this, 
        "Earned! " + remaining + " ads left today", 
        Toast.LENGTH_SHORT).show();
}
```

---

## Common Use Cases 🎮

### Game: Extra Lives

```java
buttonExtraLives.setText("Watch Ad for 3 Lives");
buttonExtraLives.setOnClickListener(v -> {
    if (adHelper.isRewardedReady()) {
        adHelper.showRewarded();
    }
});

// In callback
@Override
public void onUserEarnedReward(String type, int amount) {
    if (type.equals("lives")) {
        playerLives += amount;
        updateLivesDisplay();
    }
}
```

### Game: Continue after Game Over

```java
void showGameOverDialog() {
    new AlertDialog.Builder(this)
        .setTitle("Game Over")
        .setMessage("Watch ad to continue?")
        .setPositiveButton("Watch Ad", (d, w) -> {
            if (adHelper.isRewardedReady()) {
                adHelper.showRewarded();
            }
        })
        .setNegativeButton("Restart", (d, w) -> {
            restartGame();
        })
        .show();
}

// In callback
@Override
public void onUserEarnedReward(String type, int amount) {
    if (type.equals("continue")) {
        continueGame();
    }
}
```

### Utility: Remove Watermark

```java
buttonRemoveWatermark.setText("Watch Ad to Remove Watermark");
buttonRemoveWatermark.setOnClickListener(v -> {
    if (adHelper.isRewardedReady()) {
        adHelper.showRewarded();
    }
});

// In callback
@Override
public void onUserEarnedReward(String type, int amount) {
    if (type.equals("unlock")) {
        watermarkRemoved = true;
        updatePreview();
    }
}
```

---

## More Info 📚

- 📖 **Full Documentation**: `README_REWARDED.md`
- 💻 **Complete Example**: `REWARDED_USAGE_EXAMPLE.java`
- 🌐 **Official Guide**: [Google AdMob Docs](https://developers.google.com/admob/android/rewarded)
- 📝 **All Ad Formats**: `README_AD_FORMATS.md`

---

## Checklist Before Release ✓

- [ ] Replace test ad unit ID with production ID
- [ ] Configure reward type & amount in AdMob Console
- [ ] Test reward granting works correctly
- [ ] Test with no internet
- [ ] Test frequency limits
- [ ] Verify user can decline (have alternative)
- [ ] Check messaging is clear
- [ ] Test lifecycle (rotation, background)
- [ ] Verify not forcing users
- [ ] Test analytics tracking

---

**That's it! You're ready to reward your users! 🎁🚀**


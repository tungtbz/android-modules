# Interstitial Ad - Quick Start Guide

## 3 Bước Đơn Giản

### 1️⃣ Initialize (onCreate)

```java
AdmobHelper adHelper = AdmobHelper.getInstance();

// Test Ad Unit ID (development)
String adUnitId = "ca-app-pub-3940256099942544/1033173712";

adHelper.initInterstitial(this, adUnitId);
adHelper.loadInterstitial(); // Preload
```

### 2️⃣ Show (at natural break point)

```java
// Example: After completing level
void onLevelCompleted() {
    adHelper.showInterstitial();
    // Auto-reload sau khi dismissed
}
```

### 3️⃣ Cleanup (onDestroy)

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
public class GameActivity extends AppCompatActivity {
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        
        // Initialize
        adHelper = AdmobHelper.getInstance();
        adHelper.initInterstitial(this, "ca-app-pub-3940256099942544/1033173712");
        adHelper.loadInterstitial();
    }
    
    void onLevelCompleted() {
        // Show ad at natural break point
        adHelper.showInterstitial();
        // Ad will auto-reload after dismissed
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        adHelper.cleanup();
    }
}
```

---

## With Callbacks (Optional)

```java
adHelper.SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onAdDisplayFullScreenContent(int adType) {
        if (adType == 1) { // Interstitial
            pauseGame();
        }
    }
    
    @Override
    public void onAdDismissedFullScreenContent(int adType) {
        if (adType == 1) { // Interstitial
            resumeGame();
            goToNextLevel();
        }
    }
    
    @Override
    public void onAdClicked() {
        // Ad clicked
    }
    
    @Override
    public void onAdImpression(String adFormat, String adUnitId, 
                               String adSourceName, double revenue) {
        if (adFormat.equals("INTERSTITIAL")) {
            // Track revenue
        }
    }
    
    @Override
    public void onAOAFailedToLoad() {
        // Not used for interstitial
    }
});
```

---

## API Methods

| Method | Description |
|--------|-------------|
| `initInterstitial(Activity, String)` | Initialize với ad unit ID |
| `loadInterstitial()` | Load ad (với auto-loading prevention) |
| `showInterstitial()` | Show ad (với availability check) |
| `isInterstitialReady()` | Check if ad is ready |

---

## Best Practices ✅

### ✅ DO
- ✅ Preload ad càng sớm càng tốt
- ✅ Show tại các điểm chuyển cảnh tự nhiên
- ✅ Pause game/audio khi show ad
- ✅ Gọi cleanup() trong onDestroy()
- ✅ Dùng test ad unit ID khi development

### ❌ DON'T
- ❌ Show quá nhiều ads (max 1 ad / 2-3 phút)
- ❌ Show trong critical workflow (checkout, login)
- ❌ Show ngay khi mở app
- ❌ Dùng test ad ID trong production

---

## When to Show? ⏰

**Good Times (Natural Break Points):**
- ✅ After completing level/task
- ✅ Between major screen transitions
- ✅ After successful save/share
- ✅ In pause menu

**Bad Times:**
- ❌ During gameplay
- ❌ During checkout/payment
- ❌ Every few seconds
- ❌ App launch

---

## Ad Unit IDs 🔑

```java
// TEST (development) - Always use this when testing
String TEST_ID = "ca-app-pub-3940256099942544/1033173712";

// PRODUCTION (release) - Create in AdMob Console
String PROD_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY";
```

⚠️ **IMPORTANT:** Always use TEST_ID during development!

---

## Troubleshooting 🔧

### Ad not showing?

```java
// Check if ad is ready
if (adHelper.isInterstitialReady()) {
    Log.d(TAG, "Interstitial ready!");
    adHelper.showInterstitial();
} else {
    Log.d(TAG, "Interstitial NOT ready, loading...");
    adHelper.loadInterstitial();
}
```

**Common Issues:**
1. ✅ Did you call `initInterstitial()`?
2. ✅ Did you call `loadInterstitial()`?
3. ✅ Is ad unit ID correct?
4. ✅ Is internet connected?
5. ✅ Check logcat for errors

---

## Frequency Control 🎚️

```java
private static final int MIN_INTERVAL_MS = 120000; // 2 minutes
private long lastAdTime = 0;

void showInterstitialWithCapping() {
    long now = System.currentTimeMillis();
    if (now - lastAdTime >= MIN_INTERVAL_MS) {
        adHelper.showInterstitial();
        lastAdTime = now;
    } else {
        Log.d(TAG, "Too soon, skip this time");
    }
}
```

---

## More Info 📚

- 📖 **Full Documentation**: `README_INTERSTITIAL.md`
- 💻 **Complete Example**: `INTERSTITIAL_USAGE_EXAMPLE.java`
- 📝 **Changelog**: `CHANGELOG.md` (v2.1)
- 🌐 **Official Guide**: [Google AdMob Docs](https://developers.google.com/admob/android/interstitial)

---

## Questions? 🤔

1. Check `README_INTERSTITIAL.md` for detailed guide
2. See `INTERSTITIAL_USAGE_EXAMPLE.java` for examples
3. Look at logcat with filter "AdmobHelper"
4. Check AdMob Console for ad performance

---

**That's it! You're ready to show Interstitial Ads! 🚀**


# AdmobHelper - Supported Ad Formats

Hướng dẫn nhanh về tất cả các định dạng quảng cáo được hỗ trợ trong AdmobHelper.

---

## 📋 Danh Sách Ad Formats

| Ad Format | Status | Documentation | Quick Start |
|-----------|--------|---------------|-------------|
| **App Open Ad (AOA)** | ✅ Supported | - | - |
| **Banner Ad (Collapsible)** | ✅ Supported | - | - |
| **MREC (Medium Rectangle)** | ✅ Supported | - | - |
| **Interstitial Ad** | ✅ Supported | [README_INTERSTITIAL.md](README_INTERSTITIAL.md) | [INTERSTITIAL_QUICK_START.md](INTERSTITIAL_QUICK_START.md) |
| **Rewarded Ad** | ✅ Supported | [README_REWARDED.md](README_REWARDED.md) | [REWARDED_QUICK_START.md](REWARDED_QUICK_START.md) |
| **Rewarded Interstitial** | 🚧 Planned | - | - |
| **Native Ad** | 🚧 Planned | - | - |

---

## 🆕 Interstitial Ad (Mới!)

### Tổng quan
Quảng cáo toàn màn hình hiển thị tại các điểm chuyển cảnh tự nhiên trong ứng dụng.

### Quick Start

```java
// 1. Initialize
AdmobHelper.getInstance().initInterstitial(this, adUnitId);

// 2. Load
AdmobHelper.getInstance().loadInterstitial();

// 3. Show at natural break point
AdmobHelper.getInstance().showInterstitial();
```

### Tài liệu
- 📖 **Full Guide**: [README_INTERSTITIAL.md](README_INTERSTITIAL.md)
- 💻 **Examples**: [INTERSTITIAL_USAGE_EXAMPLE.java](INTERSTITIAL_USAGE_EXAMPLE.java)
- 🚀 **Quick Start**: [INTERSTITIAL_QUICK_START.md](INTERSTITIAL_QUICK_START.md)
- 📝 **Summary**: [INTERSTITIAL_INTEGRATION_SUMMARY.md](INTERSTITIAL_INTEGRATION_SUMMARY.md)

### Features
- ✅ Memory-safe (WeakReference)
- ✅ Thread-safe (synchronized + volatile)
- ✅ Auto-reload after dismissed
- ✅ Revenue tracking
- ✅ Full callbacks
- ✅ Best practices compliant

### Test Ad Unit ID
```java
String TEST_ID = "ca-app-pub-3940256099942544/1033173712";
```

---

## 🎯 App Open Ad (AOA)

### Tổng quan
Quảng cáo hiển thị khi người dùng mở hoặc quay lại ứng dụng.

### Quick Start

```java
// Initialize
AdmobHelper.getInstance().Init(this, listener, new String[]{aoaAdUnitId});

// Load
AdmobHelper.getInstance().loadAd(this);

// Show (usually called in onResume)
if (AdmobHelper.getInstance().canShowAOA()) {
    AdmobHelper.getInstance().showAppOpenAds(this);
}
```

### Features
- ✅ Auto-show on app resume
- ✅ Frequency control
- ✅ Block mechanism (IncreaseBlockAOA/DecreaseBlockAOA)
- ✅ Enable/Disable support

### Control Methods
```java
// Enable/Disable
AdmobHelper.getInstance().EnableAOA();
AdmobHelper.getInstance().DisableAOA();

// Block/Unblock
AdmobHelper.getInstance().IncreaseBlockAOA();
AdmobHelper.getInstance().DecreaseBlockAOA();
```

---

## 📰 Banner Ad (Collapsible)

### Tổng quan
Quảng cáo banner thông minh có thể thu gọn/mở rộng, hiển thị ở top hoặc bottom màn hình.

### Quick Start

```java
// Initialize
AdmobHelper.getInstance().initBanner(
    this, 
    bannerId, 
    Constants.POSITION_CENTER_BOTTOM
);

// Start auto-refresh (every 60 seconds)
AdmobHelper.getInstance().RunAutoRefreshBanner(60);

// Show/Hide
AdmobHelper.getInstance().showBanner();
AdmobHelper.getInstance().HideBanner();

// Stop refresh
AdmobHelper.getInstance().StopRefresh();
```

### Positions
```java
Constants.POSITION_CENTER_TOP    // Top center
Constants.POSITION_CENTER_BOTTOM // Bottom center
```

### Features
- ✅ Auto-refresh với configurable interval
- ✅ Collapsible support (top/bottom)
- ✅ Adaptive banner size
- ✅ Show/Hide control

---

## 📐 MREC (Medium Rectangle)

### Tổng quan
Quảng cáo hình chữ nhật 300x250, thích hợp cho nội dung hoặc trong feed.

### Quick Start

```java
// Initialize with position
AdmobHelper.getInstance().initMrec(
    this,
    mrecAdUnitId,
    String.valueOf(AdmobHelper.POSITION_TOP_CENTER)
);

// Load
AdmobHelper.getInstance().loadMrec();

// Show/Hide
AdmobHelper.getInstance().ShowMrec();
AdmobHelper.getInstance().HideMrec();

// Update position
AdmobHelper.getInstance().setMrecPosition(
    AdmobHelper.POSITION_BOTTOM_CENTER,
    offsetY
);
```

### Positions
```java
POSITION_TOP_CENTER     // 0
POSITION_BOTTOM_CENTER  // 1
POSITION_TOP_LEFT       // 2
POSITION_TOP_RIGHT      // 3
POSITION_BOTTOM_LEFT    // 4
POSITION_BOTTOM_RIGHT   // 5
POSITION_CENTER         // 6
POSITION_CUSTOM         // -1
```

### Features
- ✅ Flexible positioning
- ✅ Custom offset support
- ✅ Auto-retry on failure (30s)
- ✅ Safe area handling

---

## 🎁 Rewarded Ad (Mới!)

### Tổng quan
Quảng cáo có thưởng - user chủ động xem để nhận phần thưởng (coins, lives, unlock items).

### Quick Start

```java
// 1. Initialize
AdmobHelper.getInstance().initRewarded(this, adUnitId);

// 2. Load
AdmobHelper.getInstance().loadRewarded();

// 3. Setup callback
AdmobHelper.getInstance().SetAdsCallback(new IAdmobAdListener() {
    @Override
    public void onUserEarnedReward(String type, int amount) {
        userCoins += amount; // Grant reward!
    }
    // ... other callbacks
});

// 4. Show when user clicks button
buttonWatchAd.setOnClickListener(v -> {
    AdmobHelper.getInstance().showRewarded();
});
```

### Tài liệu
- 📖 **Full Guide**: [README_REWARDED.md](README_REWARDED.md)
- 💻 **Examples**: [REWARDED_USAGE_EXAMPLE.java](REWARDED_USAGE_EXAMPLE.java)
- 🚀 **Quick Start**: [REWARDED_QUICK_START.md](REWARDED_QUICK_START.md)

### Features
- ✅ User opt-in (user chủ động)
- ✅ Single-use (mỗi ad dùng 1 lần)
- ✅ Memory-safe (WeakReference)
- ✅ Thread-safe (synchronized + volatile)
- ✅ Auto-reload after dismissed
- ✅ Revenue tracking
- ✅ Full callbacks + reward listener

### Test Ad Unit ID
```java
String TEST_ID = "ca-app-pub-3940256099942544/5224354917";
```

### Reward Configuration
Configure in AdMob Console:
- **Reward Type**: String (e.g., "coins", "lives", "unlock")
- **Reward Amount**: Integer (e.g., 50, 3, 1)

---

## 🎬 Rewarded Interstitial (Coming Soon)

### Status
🚧 **Planned for future release**

### Expected Features
- ⏳ Combined interstitial + reward
- ⏳ Skip option after timer
- ⏳ Reward on completion

---

## 🖼️ Native Ad (Coming Soon)

### Status
🚧 **Planned for future release**

### Expected Features
- ⏳ Customizable native ad layouts
- ⏳ In-feed integration
- ⏳ Multiple templates

---

## 🎯 General Features (All Ad Formats)

### Memory Safety
- ✅ WeakReference pattern for UI components
- ✅ Proper cleanup in onDestroy
- ✅ No Activity leaks
- ✅ Safe garbage collection handling

### Thread Safety
- ✅ Synchronized loading states
- ✅ Volatile flags for thread-safe checks
- ✅ UI operations on main thread
- ✅ No race conditions

### Revenue Tracking
- ✅ OnPaidEventListener integration
- ✅ Ad source tracking
- ✅ Revenue per impression
- ✅ Currency code support

### Lifecycle Management
- ✅ onCreate initialization
- ✅ onPause/onResume handling
- ✅ onDestroy cleanup
- ✅ Activity state validation

### Callbacks
```java
interface IAdmobAdListener {
    void onAdDisplayFullScreenContent(int adType);
    void onAdDismissedFullScreenContent(int adType);
    void onAdClicked();
    void onAdImpression(String adFormat, String adUnitId, 
                        String adSourceName, double revenue);
    void onAOAFailedToLoad();
}
```

**Ad Types:**
- `0` = App Open Ad
- `1` = Interstitial Ad
- `2` = Rewarded Ad

**Ad Formats:**
- `"App open"` = App Open Ad
- `"INTERSTITIAL"` = Interstitial Ad
- `"REWARDED"` = Rewarded Ad
- `"COLLAPSIBLE_BANNER"` = Banner Ad
- `"MREC"` = Medium Rectangle

---

## 🎨 Consent Management

### UMP (User Messaging Platform)
```java
// Start consent flow
AdmobHelper.getInstance().startConsentFlow(this, new IGoogleConsentCallback() {
    @Override
    public void onFinish(int consentCode) {
        // 0 = error/not obtained
        // 1 = consent obtained
    }
});

// Or bypass for testing
AdmobHelper.getInstance().bypassConsentFlow(this);

// Check consent code
int code = AdmobHelper.getInstance().getConsentCode();

// Check privacy settings button
boolean show = AdmobHelper.getInstance().isPrivacySettingsButtonEnabled();
```

---

## 🔧 Lifecycle Integration

### Complete Activity Example

```java
public class MainActivity extends AppCompatActivity {
    private AdmobHelper adHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        adHelper = AdmobHelper.getInstance();
        
        // Consent flow
        adHelper.startConsentFlow(this, consentCode -> {
            // Initialize ads after consent
            initAds();
        });
    }
    
    private void initAds() {
        // Initialize all ad formats you need
        
        // App Open Ad
        adHelper.Init(this, listener, new String[]{aoaId});
        adHelper.loadAd(this);
        
        // Banner
        adHelper.initBanner(this, bannerId, Constants.POSITION_CENTER_BOTTOM);
        adHelper.RunAutoRefreshBanner(60);
        
        // MREC
        adHelper.initMrec(this, mrecId, "0");
        adHelper.loadMrec();
        
        // Interstitial
        adHelper.initInterstitial(this, interstitialId);
        adHelper.loadInterstitial();
        
        // Rewarded
        adHelper.initRewarded(this, rewardedId);
        adHelper.loadRewarded();
        
        // Setup reward callback
        setupRewardCallbacks();
    }
    
    private void setupRewardCallbacks() {
        adHelper.SetAdsCallback(new IAdmobAdListener() {
            @Override
            public void onUserEarnedReward(String type, int amount) {
                // Grant reward
                userCoins += amount;
                updateUI();
            }
            // ... other callbacks
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        adHelper.onResume();
        
        // Show AOA if allowed
        if (adHelper.canShowAOA()) {
            adHelper.showAppOpenAds(this);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        adHelper.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        adHelper.cleanup(); // CRITICAL: Prevent memory leaks
    }
}
```

---

## 📊 Test Ad Unit IDs

### For Development (Always use these!)

```java
// App Open Ad
String AOA_TEST_ID = "ca-app-pub-3940256099942544/9257395921";

// Interstitial
String INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712";

// Banner
String BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111";

// MREC (use Banner test ID)
String MREC_TEST_ID = "ca-app-pub-3940256099942544/6300978111";

// Rewarded
String REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917";
```

⚠️ **NEVER** use test IDs in production!

---

## 📚 Documentation Index

### General
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [MEMORY_LEAK_FIX.md](MEMORY_LEAK_FIX.md) - Memory leak fixes (v2.0)
- [README_MEMORY_FIX.md](README_MEMORY_FIX.md) - Memory fix guide
- [USAGE_EXAMPLE.java](USAGE_EXAMPLE.java) - General usage examples

### Interstitial Ad (v2.1)
- [README_INTERSTITIAL.md](README_INTERSTITIAL.md) - Full documentation
- [INTERSTITIAL_USAGE_EXAMPLE.java](INTERSTITIAL_USAGE_EXAMPLE.java) - Code examples
- [INTERSTITIAL_QUICK_START.md](INTERSTITIAL_QUICK_START.md) - Quick start
- [INTERSTITIAL_INTEGRATION_SUMMARY.md](INTERSTITIAL_INTEGRATION_SUMMARY.md) - Integration summary

### Rewarded Ad (v2.2)
- [README_REWARDED.md](README_REWARDED.md) - Full documentation
- [REWARDED_USAGE_EXAMPLE.java](REWARDED_USAGE_EXAMPLE.java) - Code examples
- [REWARDED_QUICK_START.md](REWARDED_QUICK_START.md) - Quick start

### Reference
- [admob_android_interstitial.md](admob_android_interstitial.md) - AdMob interstitial reference

---

## 🆘 Troubleshooting

### Common Issues

**Ads not showing?**
1. ✅ Check internet connection
2. ✅ Use test ad unit IDs
3. ✅ Complete consent flow
4. ✅ Check logcat for errors
5. ✅ Verify ad unit IDs

**Memory leaks?**
1. ✅ Call `cleanup()` in `onDestroy()`
2. ✅ Use Memory Profiler to verify
3. ✅ Check Activity is not held after destroy

**Crashes?**
1. ✅ Initialize SDK before showing ads
2. ✅ Check Activity is valid
3. ✅ Don't show ads during Activity transitions

---

## 🎓 Best Practices Summary

### Do ✅
- ✅ Always use test ad IDs during development
- ✅ Call cleanup() in onDestroy()
- ✅ Complete consent flow before showing ads
- ✅ Show ads at natural break points
- ✅ Implement frequency capping
- ✅ Handle callbacks properly
- ✅ Test on real devices

### Don't ❌
- ❌ Use test IDs in production
- ❌ Show too many ads
- ❌ Interrupt critical user flows
- ❌ Forget to cleanup
- ❌ Show ads immediately on app launch
- ❌ Block UI with ads

---

## 📈 Version History

| Version | Date | Features |
|---------|------|----------|
| **v2.2** | Nov 17, 2025 | 🎁 Rewarded Ad integration |
| **v2.1** | Nov 17, 2025 | ➕ Interstitial Ad integration |
| **v2.0** | Nov 17, 2025 | 🔧 Memory leak fixes, thread safety |
| **v1.0** | - | 🎉 Initial release (AOA, Banner, MREC) |

---

## 🔮 Roadmap

### Next Release (v2.3)
- [ ] Rewarded Interstitial Ad
- [ ] Improved error handling
- [ ] More callback options
- [ ] Ad loading state callbacks

### Future (v3.0)
- [ ] Rewarded Interstitial
- [ ] Native Ad support
- [ ] Ad mediation helpers
- [ ] Analytics integration
- [ ] Remote config support

---

## 🤝 Support

### Need Help?
1. Check specific ad format documentation
2. Review usage examples
3. Look at troubleshooting sections
4. Check logcat with filter "AdmobHelper"
5. Verify in AdMob Console

### External Resources
- [Google AdMob Documentation](https://developers.google.com/admob/android)
- [AdMob Policy Center](https://support.google.com/admob/answer/6128543)
- [Google Mobile Ads SDK](https://developers.google.com/admob/android/quick-start)

---

**Happy Monetizing! 💰🚀**


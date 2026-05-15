# Debug Banner Placeholder - Learnings

## Task Summary
Implemented debug banner placeholder in MaxAdsService.java — a solid black View that mimics the exact size and position of the real banner ad, enabling rapid testing of SetBannerPosition / SetBannerPositionAbsolute without waiting for banner ad load.

## Changes Made

### Edit 1 - Field Added
- Added `private volatile View mDebugBannerPlaceholder;` after `rectAdView` field
- Location: Line 79

### Edit 2 - Three Methods Added
Added after `HideBanner()` method:
1. `ShowDebugBannerPlaceholder()` - Creates solid black View, positions it, makes visible
2. `HideDebugBannerPlaceholder()` - Hides the placeholder
3. `positionDebugBannerPlaceholder(int heightPx)` - Mirrors applyAdViewPosition gravity+margins logic

### Edit 3 - SetBannerPosition() Updated
- Removed early return when bannerAdView is null
- Added placeholder sync logic that runs regardless of bannerAdView existence
- Uses MaxAdFormat.BANNER.getAdaptiveSize() to get height in dp, converts to px

### Edit 4 - SetBannerPositionAbsolute() Updated
- Added placeholder sync after bannerAdView positioning
- Uses same TOP|START gravity and absolute margin calculation as bannerAdView

## Key Patterns Used
- `runSafelyOnUiThread()` for UI thread operations
- `MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight()` for adaptive height
- `AppLovinSdkUtils.dpToPx()` for dp to px conversion
- `getSafeInsets()` for safe area margins
- `ViewGroup.LayoutParams.MATCH_PARENT` for banner width

## Verification
- All 4 edits applied successfully
- Code structure verified via read tool
- Gradle build failed due to Java version mismatch (environment has Java 8, project requires Java 11) — not a code issue

## Notes
- All required imports (View, Color, Gravity, FrameLayout, ViewGroup, Log) already existed in the file
- No BuildConfig.DEBUG wrapping as per requirements
- Placeholder uses plain View (not MaxAdView) as specified
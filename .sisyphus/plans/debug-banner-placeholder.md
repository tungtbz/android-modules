# Debug Banner Placeholder (Black View)

## TL;DR

> **Quick Summary**: Thêm một `View` màu đen vào `MaxAdsService` có cùng kích thước và vị trí chính xác với banner ad, cho phép test nhanh `SetBannerPosition` / `SetBannerPositionAbsolute` mà không cần đợi banner load xong.
>
> **Deliverables**:
> - Field `mDebugBannerPlaceholder` (plain `View`)
> - Method `ShowDebugBannerPlaceholder()` — tạo + định vị view đen
> - Method `HideDebugBannerPlaceholder()` — ẩn view
> - Private helper `positionDebugBannerPlaceholder(int heightPx)` — logic tái sử dụng
> - `SetBannerPosition()` cập nhật: đồng bộ placeholder nếu đang hiển thị
> - `SetBannerPositionAbsolute()` cập nhật: đồng bộ placeholder nếu đang hiển thị
>
> **Estimated Effort**: Quick (single file, ~80 lines)
> **Parallel Execution**: NO — sequential, một task duy nhất
> **Critical Path**: Task 1 → Done

---

## Context

### Original Request
Tạo một view màu đen có đúng kích thước và vị trí banner để test nhanh hàm `SetBannerPosition` mà không cần đợi Banner Load xong.

### Research Findings
- Banner dùng `FrameLayout.LayoutParams` với `gravity` + `margins` dựa trên `_bannerPosition` / `_bannerCustomPosition`
- Chiều cao tính bằng `MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight()` (adaptive, thay đổi theo màn hình)
- Chiều rộng luôn là `MATCH_PARENT`
- Logic định vị nằm hoàn toàn trong `applyAdViewPosition()` (private, nhận `MaxAdView`) và `SetBannerPositionAbsolute()`
- Safe-insets được tính bởi `getSafeInsets()` (static, tái sử dụng được)
- `MaxAdView extends View` → `FrameLayout.LayoutParams` áp dụng hoàn toàn giống nhau cho plain `View`

---

## Work Objectives

### Core Objective
Thêm debug view "black banner ghost" vào `MaxAdsService.java` — **chỉ thêm, không sửa logic hiện có** (ngoại trừ 2 chỗ sync placeholder trong `SetBannerPosition` và `SetBannerPositionAbsolute`).

### Concrete Deliverables
- `T:\android-modules\maxads\src\main\java\com\rofi\maxads\MaxAdsService.java` (modified)

### Must Have
- View màu `Color.BLACK`, `MATCH_PARENT` width, adaptive height (giống banner thật)
- Tái sử dụng đúng `getSafeInsets()`, `_bannerCustomPosition`, `_bannerCustomOffsetX/Y`, `_bannerPosition` để tính vị trí
- `SetBannerPosition()` và `SetBannerPositionAbsolute()` tự động cập nhật placeholder khi nó đang `VISIBLE`
- Method public được gọi từ Unity bridge (`ShowDebugBannerPlaceholder`, `HideDebugBannerPlaceholder`)

### Must NOT Have (Guardrails)
- **KHÔNG** thay đổi bất kỳ logic banner thật nào (load, show, hide, position)
- **KHÔNG** thêm guard `BuildConfig.DEBUG` — để Unity gọi tùy ý
- **KHÔNG** dùng Kotlin
- **KHÔNG** dùng `MaxAdView` cho placeholder — dùng plain `View`
- **KHÔNG** gọi `MaxAdView` API (loadAd, startAutoRefresh...) trên placeholder
- **KHÔNG** duplicate `applyAdViewPosition` — hãy tự implement logic tương đương cho plain `View` trong `positionDebugBannerPlaceholder`

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: NO (0% test coverage — scaffold only)
- **Automated tests**: NO
- **Agent-Executed QA**: YES (log inspection + visual verification)

---

## Execution Strategy

Sequential — 1 task duy nhất.

---

## TODOs

- [ ] 1. Thêm debug banner placeholder vào MaxAdsService

  **File**: `T:\android-modules\maxads\src\main\java\com\rofi\maxads\MaxAdsService.java`

  **What to do**:

  **Bước 1 — Thêm field** (sau `private volatile MaxAdView rectAdView;`, dòng ~75):
  ```java
  // Debug placeholder — solid black view at the exact position/size of the banner.
  // Used to test SetBannerPosition / SetBannerPositionAbsolute without waiting for ad load.
  private volatile View mDebugBannerPlaceholder;
  ```

  **Bước 2 — Thêm 3 method mới** ngay sau method `HideBanner()` (khoảng dòng 1483):

  ```java
  /**
   * Shows a solid black placeholder view at the exact position and adaptive height
   * of the real banner. Use this to visually verify SetBannerPosition() and
   * SetBannerPositionAbsolute() without waiting for the banner ad to load.
   * <p>
   * SetBannerPosition() and SetBannerPositionAbsolute() automatically keep the
   * placeholder in sync while it is visible.
   */
  public void ShowDebugBannerPlaceholder() {
      Activity activity = getCurrentActivity();
      if (activity == null) return;
      runSafelyOnUiThread(activity, new Runnable() {
          @Override
          public void run() {
              Activity act = getCurrentActivity();
              if (act == null) return;

              int heightDp = MaxAdFormat.BANNER.getAdaptiveSize(act).getHeight();
              int heightPx = AppLovinSdkUtils.dpToPx(act, heightDp);

              if (mDebugBannerPlaceholder == null) {
                  mDebugBannerPlaceholder = new View(act);
                  mDebugBannerPlaceholder.setBackgroundColor(Color.BLACK);
                  ViewGroup rootView = act.findViewById(android.R.id.content);
                  rootView.addView(mDebugBannerPlaceholder);
              }

              mDebugBannerPlaceholder.setVisibility(View.VISIBLE);
              positionDebugBannerPlaceholder(heightPx);
              mDebugBannerPlaceholder.bringToFront();
              Log.d(TAG, "ShowDebugBannerPlaceholder: heightPx=" + heightPx);
          }
      });
  }

  /**
   * Hides the debug banner placeholder created by {@link #ShowDebugBannerPlaceholder()}.
   */
  public void HideDebugBannerPlaceholder() {
      runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
          @Override
          public void run() {
              if (mDebugBannerPlaceholder != null) {
                  mDebugBannerPlaceholder.setVisibility(View.GONE);
                  Log.d(TAG, "HideDebugBannerPlaceholder");
              }
          }
      });
  }

  /**
   * Repositions the debug banner placeholder to match the current banner position state.
   * Mirrors the gravity + margin logic of applyAdViewPosition (isBanner=true).
   * Must be called on the UI thread.
   *
   * @param heightPx Adaptive banner height in pixels.
   */
  private void positionDebugBannerPlaceholder(int heightPx) {
      if (mDebugBannerPlaceholder == null) return;
      Activity act = getCurrentActivity();
      if (act == null) return;

      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, heightPx);

      if (_bannerCustomPosition != null) {
          // Mirror applyAdViewPosition logic (isBanner = true, MATCH_PARENT)
          String pos = _bannerCustomPosition.toLowerCase().trim();
          if ("centered".equals(pos)) pos = "bottom_center";

          int gravity;
          if (pos.contains("top")) {
              gravity = Gravity.TOP;
          } else {
              gravity = Gravity.BOTTOM;
          }
          if (pos.contains("left")) {
              gravity |= Gravity.START;
          } else if (pos.contains("right")) {
              gravity |= Gravity.END;
          } else {
              gravity |= Gravity.CENTER_HORIZONTAL;
          }

          Insets insets = getSafeInsets();
          int marginLeft   = insets.left;
          int marginRight  = insets.right;
          int marginTop    = insets.top;
          int marginBottom = insets.bottom;

          // For MATCH_PARENT banner: offsetX only applies to START/END edges
          int hGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
          if (hGravity == Gravity.START) {
              marginLeft += _bannerCustomOffsetX;
          } else if (hGravity == Gravity.END) {
              marginRight += _bannerCustomOffsetX;
          }

          int vGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
          if (vGravity == Gravity.TOP) {
              marginTop += _bannerCustomOffsetY;
          } else {
              marginBottom += _bannerCustomOffsetY;
          }

          params.gravity = gravity;
          params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
      } else {
          // Default gravity — mirrors LoadNormalBanner behaviour
          params.gravity = (_bannerPosition == Constants.POSITION_CENTER_TOP)
                  ? Gravity.TOP | Gravity.CENTER_HORIZONTAL
                  : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
      }

      mDebugBannerPlaceholder.setLayoutParams(params);
      mDebugBannerPlaceholder.requestLayout();
  }
  ```

  **Bước 3 — Cập nhật `SetBannerPosition()`** (khoảng dòng 1498-1513):

  Thay đổi phần bên trong `runSafelyOnUiThread` từ:
  ```java
  if (bannerAdView == null) {
      Log.w(TAG, "SetBannerPosition: bannerAdView not yet initialized; "
              + "position saved and will be applied on next load.");
      return;
  }
  applyAdViewPosition(bannerAdView, position, offsetX, offsetY, true);
  ```
  Thành:
  ```java
  if (bannerAdView == null) {
      Log.w(TAG, "SetBannerPosition: bannerAdView not yet initialized; "
              + "position saved and will be applied on next load.");
  } else {
      applyAdViewPosition(bannerAdView, position, offsetX, offsetY, true);
  }
  // Sync debug placeholder regardless of whether bannerAdView exists
  if (mDebugBannerPlaceholder != null
          && mDebugBannerPlaceholder.getVisibility() == View.VISIBLE) {
      Activity act = getCurrentActivity();
      if (act != null) {
          int hDp = MaxAdFormat.BANNER.getAdaptiveSize(act).getHeight();
          positionDebugBannerPlaceholder(AppLovinSdkUtils.dpToPx(act, hDp));
      }
  }
  ```

  **Bước 4 — Cập nhật `SetBannerPositionAbsolute()`** (khoảng dòng 652-685):

  Thêm vào ngay SAU dòng `bannerAdView.requestLayout();` (và LOG) bên trong runnable:
  ```java
  // Sync debug placeholder with absolute position
  if (mDebugBannerPlaceholder != null
          && mDebugBannerPlaceholder.getVisibility() == View.VISIBLE) {
      FrameLayout.LayoutParams debugParams = new FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
      debugParams.gravity     = Gravity.TOP | Gravity.START;
      debugParams.topMargin   = centerYPx - heightPx / 2;
      debugParams.leftMargin  = 0;
      debugParams.rightMargin = 0;
      debugParams.bottomMargin = 0;
      mDebugBannerPlaceholder.setLayoutParams(debugParams);
      mDebugBannerPlaceholder.requestLayout();
  }
  ```
  > **Lưu ý**: `SetBannerPositionAbsolute` dùng `topMargin = centerYPx + heightPx / 2` (Unity Y-axis convention). Placeholder phải dùng đúng cùng công thức đó để khớp vị trí thật.
  > Hãy kiểm tra lại công thức hiện tại trong code (`centerYPx + heightPx / 2` hay `centerYPx - heightPx / 2`) và dùng **đúng** công thức đó cho placeholder.

  **Must NOT do**:
  - Không thay đổi return type hay signature của bất kỳ method public nào
  - Không thêm import mới (tất cả đã có: `View`, `Color`, `Gravity`, `FrameLayout`, `ViewGroup`, `Log`)
  - Không wrap trong `BuildConfig.DEBUG`

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single file, well-defined changes (~80 lines added, 2 small modifications)
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (only task)
  - **Blocks**: Nothing
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `MaxAdsService.java:490-594` — `applyAdViewPosition()`: logic gravity + margin cần mirror trong `positionDebugBannerPlaceholder`
  - `MaxAdsService.java:652-685` — `SetBannerPositionAbsolute()`: xem công thức `topMargin` để mirror đúng
  - `MaxAdsService.java:606-624` — `getSafeInsets()`: gọi để lấy insets (đã static)
  - `MaxAdsService.java:1144-1267` — `LoadNormalBanner()`: cách tính `heightPx` và default gravity

  **API/Type References**:
  - `MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight()` — adaptive banner height (int dp)
  - `AppLovinSdkUtils.dpToPx(context, dp)` — convert dp → px
  - `FrameLayout.LayoutParams(MATCH_PARENT, heightPx)` — layout params cho plain View

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Placeholder xuất hiện đúng vị trí bottom (mặc định)
    Tool: Bash (adb logcat)
    Preconditions: App đang chạy, chưa gọi SetBannerPosition
    Steps:
      1. Gọi ShowDebugBannerPlaceholder() qua Unity/ADB
      2. Kiểm tra logcat: D/MaxAdsService: ShowDebugBannerPlaceholder: heightPx=XXX
      3. Quan sát màn hình: black bar xuất hiện ở BOTTOM của màn hình
    Expected Result: Black view rộng toàn màn hình, cao đúng adaptive height, nằm ở dưới cùng
    Evidence: .sisyphus/evidence/task-1-placeholder-default.txt (logcat snippet)

  Scenario: SetBannerPosition("top_center", 0, 0) cập nhật placeholder
    Tool: Bash (adb logcat)
    Preconditions: ShowDebugBannerPlaceholder() đã được gọi trước
    Steps:
      1. Gọi SetBannerPosition("top_center", 0, 0)
      2. Quan sát màn hình ngay lập tức (không cần đợi ad load)
      3. Kiểm tra logcat: applyAdViewPosition: Banner pos=top_center
    Expected Result: Black view chuyển lên TOP của màn hình ngay lập tức
    Evidence: .sisyphus/evidence/task-1-placeholder-top.txt (logcat snippet)

  Scenario: HideDebugBannerPlaceholder ẩn view
    Tool: Bash (adb logcat)
    Preconditions: Placeholder đang VISIBLE
    Steps:
      1. Gọi HideDebugBannerPlaceholder()
      2. Kiểm tra logcat: HideDebugBannerPlaceholder
    Expected Result: Black view biến mất khỏi màn hình
    Evidence: .sisyphus/evidence/task-1-placeholder-hide.txt (logcat snippet)

  Scenario: Không crash khi gọi ShowDebugBannerPlaceholder() trước khi banner load
    Tool: Bash (adb logcat grep -i "exception\|crash\|fatal")
    Preconditions: App vừa khởi động, banner chưa load xong
    Steps:
      1. Gọi ShowDebugBannerPlaceholder() ngay sau Init()
      2. Kiểm tra logcat không có exception
    Expected Result: Không có crash, không có NullPointerException
    Evidence: .sisyphus/evidence/task-1-placeholder-no-crash.txt
  ```

  **Commit**: YES
  - Message: `feat(maxads): add debug banner placeholder for position testing`
  - Files: `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`
  - Pre-commit: `./gradlew :maxads:compileDebugJavaWithJavac`

---

## Final Verification Wave

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Đọc diff cuối, kiểm tra: field đã thêm, 3 method public/private đã thêm, `SetBannerPosition` và `SetBannerPositionAbsolute` đã sync placeholder, không có logic banner thật nào bị thay đổi, build clean.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | VERDICT: APPROVE/REJECT`

---

## Commit Strategy

- `feat(maxads): add debug banner placeholder for position testing`
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`
  - Pre-commit: `./gradlew :maxads:compileDebugJavaWithJavac`

---

## Success Criteria

### Verification Commands
```bash
# Build phải sạch
./gradlew :maxads:compileDebugJavaWithJavac
# Expected: BUILD SUCCESSFUL
```

### Final Checklist
- [ ] Field `mDebugBannerPlaceholder` tồn tại
- [ ] `ShowDebugBannerPlaceholder()` và `HideDebugBannerPlaceholder()` là public
- [ ] `positionDebugBannerPlaceholder()` là private
- [ ] `SetBannerPosition()` sync placeholder (không break early return cũ)
- [ ] `SetBannerPositionAbsolute()` sync placeholder
- [ ] Build `:maxads:compileDebugJavaWithJavac` SUCCESS
- [ ] Không import mới nào cần thêm

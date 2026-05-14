# Custom Position for Banner & MREC Ads

## TL;DR

> **Quick Summary**: Add `SetBannerPosition` and `SetMRECPosition` public methods to `MaxAdsService` so callers (Unity, Java) can reposition the main banner and MREC ad views to any gravity-based position with dp offsets, matching the existing `updatePositionMrecAdview` pattern for FreeMrec.
>
> **Deliverables**:
> - `MaxAdsService.java` — 2 new public methods + 1 private helper + 6 new volatile state fields + minor edits in `LoadNormalBanner` and `LoadMREC`
>
> **Estimated Effort**: Quick (1 file, < 120 lines net-new code)
> **Parallel Execution**: NO — single file, single wave
> **Critical Path**: Task 1 → done

---

## Context

### Original Request
"tôi muốn có thêm chức năng custom vị trí cho banner, mrec" — add custom position functionality for banner and mrec ads in `MaxAdsService.java`.

### Interview Summary
**Key Discussions**:
- Current banner/MREC only support 2 positions: `Constants.POSITION_CENTER_TOP` (0) or `Constants.POSITION_CENTER_BOTTOM` (1)
- `updatePositionMrecAdview(String, int)` already exists for the free/secondary MREC (`mFreeMrecAdViews`) — new methods follow the same string-based position pattern
- `bannerAdView` and `rectAdView` are direct children of the root `FrameLayout` (content view), not a `RelativeLayout` like FreeMrec — LayoutParams type differs

**Research Findings**:
- `IAdsService` interface follows a pattern where implementation-specific extended methods are NOT added to the interface (`updatePositionMrecAdview` is not in IAdsService) — new methods follow the same pattern (MaxAdsService-only)
- `AdsManager` is a thin holder; Unity callers call directly on the concrete class via `AndroidJavaObject.Call()` by method name — interface membership is irrelevant for Unity callers
- `getSafeInsets()` returns pixel values (not dp) — must convert offsetX/offsetY via `AppLovinSdkUtils.dpToPx()` before adding to insets

### Metis Review
**Identified Gaps (addressed)**:
- Gap: `_bannerCustomPosition` state not persisted → recreating the banner (after `cleanup()` + re-`Init()`) silently reverts to int-based position. **Resolved**: Add volatile String/int fields for custom position; apply in `LoadNormalBanner` and `LoadMREC` when non-null.
- Gap: `getLayoutParams()` unsafe cast risk. **Resolved**: `instanceof FrameLayout.LayoutParams` guard before cast.
- Gap: Unit mismatch (insets = px, offsets = dp). **Resolved**: explicit `AppLovinSdkUtils.dpToPx()` conversion before arithmetic.
- Gap: `"centered"` for banner (MATCH_PARENT width + vertical center) is undefined for AppLovin SDK. **Resolved**: Explicitly excluded from supported banner positions; maps to fallback `"bottom_center"` with `Log.w`.
- Gap: offsetX direction undefined for right-aligned positions. **Resolved**: See table below — `offsetX` always adds to the *edge margin* matching the alignment (`marginLeft` for START, `marginRight` for END, ignored for CENTER_HORIZONTAL banner; adds to `marginLeft` for MREC CENTER_HORIZONTAL).

---

## Work Objectives

### Core Objective
Add two public methods to `MaxAdsService` that allow callers to reposition the main banner and MREC ad views at runtime, with position surviving ad view recreation.

### Concrete Deliverables
- `MaxAdsService.java` modified with: 6 new volatile fields, `SetBannerPosition()`, `SetMRECPosition()`, `applyAdViewPosition()`, and updated `LoadNormalBanner` / `LoadMREC`

### Definition of Done
- [ ] `./gradlew :maxads:assembleDebug` exits 0 with no new errors
- [ ] `./gradlew :maxads:lint` reports no new warnings in changed file
- [ ] Calling `SetBannerPosition("top_center", 0, 0)` logs a confirmation and does not crash
- [ ] Calling `SetBannerPosition("bottom_center", 0, 0)` when `bannerAdView == null` logs `W/MaxAdsService` and does not crash

### Must Have
- `SetBannerPosition(String position, int offsetX, int offsetY)` public method on `MaxAdsService`
- `SetMRECPosition(String position, int offsetX, int offsetY)` public method on `MaxAdsService`
- Thread safety: both methods dispatch to UI thread via `runSafelyOnUiThread()`
- Null guard: if ad view not yet created, log.w + early return (no crash, no pending-state logic needed)
- Position persistence: stored in volatile fields, applied in `LoadNormalBanner` / `LoadMREC` so position survives cleanup+reinit
- Unit correctness: dp offsets converted via `AppLovinSdkUtils.dpToPx()` before adding to pixel-valued safe insets
- Height preservation: banner height read from existing LayoutParams (not recomputed) inside position update

### Must NOT Have (Guardrails)
- **DO NOT** add `SetBannerPosition` / `SetMRECPosition` to `IAdsService` — matches existing pattern where `updatePositionMrecAdview` is not in the interface
- **DO NOT** modify or refactor `updatePositionMrecAdview` or anything touching `mFreeMrecAdViews`
- **DO NOT** change `mNativeBannerAdsContainer` or `mNativeRectAdsContainer` positioning
- **DO NOT** remove `_bannerPosition` / `_mrecPosition` int fields — still read by `LoadNormalBanner`, `LoadMREC`, `ShowNativeBanner`, `ShowNativeMREC`
- **DO NOT** support `"centered"` for banner (MATCH_PARENT width + vertical center undefined) — fallback to `"bottom_center"` with warning
- **DO NOT** recompute adaptive banner height inside the position update method — read from existing params
- **DO NOT** touch `IronsourceAdsService`, `AdmobHelper`, or any other IAdsService implementation

---

## Position-to-Gravity Lookup Table (Spec — Implementation Must Match)

| Position String | Gravity Flags | offsetX applied to | offsetY applied to |
|---|---|---|---|
| `"top_center"` | `TOP \| CENTER_HORIZONTAL` | ignored (MATCH_PARENT banner); `marginLeft` (MREC) | `marginTop += offsetYPx` |
| `"top_left"` | `TOP \| START` | `marginLeft += offsetXPx` | `marginTop += offsetYPx` |
| `"top_right"` | `TOP \| END` | `marginRight += offsetXPx` | `marginTop += offsetYPx` |
| `"bottom_center"` *(default)* | `BOTTOM \| CENTER_HORIZONTAL` | ignored (MATCH_PARENT banner); `marginLeft` (MREC) | `marginBottom += offsetYPx` |
| `"bottom_left"` | `BOTTOM \| START` | `marginLeft += offsetXPx` | `marginBottom += offsetYPx` |
| `"bottom_right"` | `BOTTOM \| END` | `marginRight += offsetXPx` | `marginBottom += offsetYPx` |
| anything else / null | same as `"bottom_center"` + `Log.w` | same as bottom_center | same as bottom_center |
| `"centered"` for banner | redirect to `"bottom_center"` + `Log.w` | same as bottom_center | same as bottom_center |

**Note**: All pixel values = `getSafeInsets().<field> + AppLovinSdkUtils.dpToPx(ctx, offset)`. Units must never be mixed.

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: NO (only `ExampleInstrumentedTest` scaffolds)
- **Automated tests**: None
- **Framework**: N/A
- **Agent-Executed QA**: YES (build + logcat analysis)

---

## Execution Strategy

### Single Task — No Parallelism Needed
All changes are in one file (`MaxAdsService.java`). One task, one agent, sequential.

```
Wave 1 (only wave):
└── Task 1: Add custom position fields + methods + helper + wire into LoadNormalBanner/LoadMREC [quick]

Wave FINAL:
├── F1: Plan compliance + build verification (oracle)
├── F2: Code quality review (unspecified-high)
```

---

## TODOs

- [x] 1. Add custom position state fields, `SetBannerPosition`, `SetMRECPosition`, `applyAdViewPosition` helper, and wire into `LoadNormalBanner` / `LoadMREC`

  **What to do**:

  **Step A — Add 6 new volatile fields** (immediately after line 116 `private volatile int _mrecBgColor;`):
  ```java
  // Custom position state — persists across ad view recreation
  private volatile String _bannerCustomPosition;  // null = use _bannerPosition int
  private volatile int    _bannerCustomOffsetX;
  private volatile int    _bannerCustomOffsetY;
  private volatile String _mrecCustomPosition;    // null = use _mrecPosition int
  private volatile int    _mrecCustomOffsetX;
  private volatile int    _mrecCustomOffsetY;
  ```

  **Step B — Add private helper `applyAdViewPosition`** (after `updatePositionMrecAdview` at ~line 470, before `private static void d(...)`):
  ```java
  /**
   * Applies a gravity-based position + dp offsets to a MaxAdView that is a direct
   * child of a FrameLayout (the root content view). Preserves existing height.
   *
   * @param adView    The ad view to reposition (bannerAdView or rectAdView)
   * @param position  Position keyword — see position table in plan
   * @param offsetXDp Horizontal offset in dp (applied to the edge matching alignment)
   * @param offsetYDp Vertical offset in dp (applied to the vertical edge)
   * @param isBanner  true = banner (MATCH_PARENT width); false = MREC (300×250dp)
   */
  private void applyAdViewPosition(MaxAdView adView, String position,
                                   int offsetXDp, int offsetYDp, boolean isBanner) {
      Activity activity = getCurrentActivity();
      if (activity == null) return;

      // --- Resolve dimensions ---
      int widthPx;
      int heightPx;
      ViewGroup.LayoutParams existingLp = adView.getLayoutParams();
      if (isBanner) {
          widthPx = ViewGroup.LayoutParams.MATCH_PARENT;
          // Preserve existing height — do NOT recompute adaptive height here
          heightPx = (existingLp != null) ? existingLp.height
                  : AppLovinSdkUtils.dpToPx(activity,
                        MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight());
      } else {
          widthPx  = AppLovinSdkUtils.dpToPx(activity, MaxAdFormat.MREC.getSize().getWidth());
          heightPx = AppLovinSdkUtils.dpToPx(activity, MaxAdFormat.MREC.getSize().getHeight());
      }

      // --- Normalize position string ---
      String pos = (position != null) ? position.toLowerCase().trim() : "";
      // "centered" is invalid for banner (MATCH_PARENT); fall back to bottom_center
      if (isBanner && "centered".equals(pos)) {
          Log.w(TAG, "applyAdViewPosition: 'centered' is not supported for Banner "
                  + "(MATCH_PARENT width). Falling back to 'bottom_center'.");
          pos = "bottom_center";
      }

      // --- Resolve gravity ---
      int gravity;
      if (pos.contains("top")) {
          gravity = Gravity.TOP;
      } else if (pos.contains("bottom")) {
          gravity = Gravity.BOTTOM;
      } else {
          if (!pos.isEmpty() && !"centered".equals(pos)) {
              Log.w(TAG, "applyAdViewPosition: unknown position '" + position
                      + "'. Falling back to 'bottom_center'.");
          }
          gravity = Gravity.BOTTOM;
          pos = "bottom_center"; // normalize for margin resolution below
      }
      if (pos.contains("left")) {
          gravity |= Gravity.START;
      } else if (pos.contains("right")) {
          gravity |= Gravity.END;
      } else {
          gravity |= Gravity.CENTER_HORIZONTAL;
      }

      // --- Convert offsets: dp → px (safe insets are already in px) ---
      Insets insets = getSafeInsets();
      int offsetXPx = AppLovinSdkUtils.dpToPx(activity, offsetXDp);
      int offsetYPx = AppLovinSdkUtils.dpToPx(activity, offsetYDp);

      int marginLeft   = insets.left;
      int marginRight  = insets.right;
      int marginTop    = insets.top;
      int marginBottom = insets.bottom;

      // Apply offsetX to the edge matching horizontal alignment
      int hGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
      if (hGravity == Gravity.START) {
          marginLeft += offsetXPx;
      } else if (hGravity == Gravity.END) {
          marginRight += offsetXPx;
      } else {
          // CENTER_HORIZONTAL: offsetX shifts MREC; for MATCH_PARENT banner it has no effect
          if (!isBanner) {
              marginLeft += offsetXPx;
          }
      }

      // Apply offsetY to the vertical edge
      int vGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
      if (vGravity == Gravity.TOP) {
          marginTop += offsetYPx;
      } else {
          marginBottom += offsetYPx;
      }

      // --- Apply LayoutParams ---
      FrameLayout.LayoutParams params;
      if (existingLp instanceof FrameLayout.LayoutParams) {
          params = (FrameLayout.LayoutParams) existingLp;
      } else {
          params = new FrameLayout.LayoutParams(widthPx, heightPx);
      }
      params.width   = widthPx;
      params.height  = heightPx;
      params.gravity = gravity;
      params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
      adView.setLayoutParams(params);
      adView.requestLayout();

      Log.d(TAG, "applyAdViewPosition: " + (isBanner ? "Banner" : "MREC")
              + " pos=" + position + " gravity=" + gravity
              + " offsetX=" + offsetXDp + "dp offsetY=" + offsetYDp + "dp"
              + " margins=[L=" + marginLeft + " T=" + marginTop
              + " R=" + marginRight + " B=" + marginBottom + "]px");
  }
  ```

  **Step C — Add `SetBannerPosition` public method** (after `ShowBanner` / `HideBanner` block, ~after line 1242):
  ```java
  /**
   * Sets a custom position for the main banner ad view.
   * Safe to call at any time; position is persisted and re-applied if the banner
   * is recreated (e.g., after cleanup() + Init()).
   *
   * Supported position values: "top_center", "top_left", "top_right",
   *   "bottom_center" (default), "bottom_left", "bottom_right".
   * Note: "centered" is not supported for banner and falls back to "bottom_center".
   *
   * @param position  Position string (case-insensitive)
   * @param offsetX   Horizontal offset in dp (positive = inward from aligned edge)
   * @param offsetY   Vertical offset in dp (positive = inward from aligned edge)
   */
  public void SetBannerPosition(String position, int offsetX, int offsetY) {
      _bannerCustomPosition = position;
      _bannerCustomOffsetX  = offsetX;
      _bannerCustomOffsetY  = offsetY;
      runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
          @Override
          public void run() {
              if (bannerAdView == null) {
                  Log.w(TAG, "SetBannerPosition: bannerAdView not yet initialized; "
                          + "position saved and will be applied on next load.");
                  return;
              }
              applyAdViewPosition(bannerAdView, position, offsetX, offsetY, true);
          }
      });
  }
  ```

  **Step D — Add `SetMRECPosition` public method** (after `ShowMREC` / `HideMREC` block, ~after line 1305):
  ```java
  /**
   * Sets a custom position for the main MREC ad view.
   * Safe to call at any time; position is persisted and re-applied if the MREC
   * is recreated (e.g., after cleanup() + Init()).
   *
   * Supported position values: "top_center", "top_left", "top_right",
   *   "bottom_center", "bottom_left", "bottom_right", "centered".
   *
   * @param position  Position string (case-insensitive)
   * @param offsetX   Horizontal offset in dp (positive = inward from aligned edge)
   * @param offsetY   Vertical offset in dp (positive = inward from aligned edge)
   */
  public void SetMRECPosition(String position, int offsetX, int offsetY) {
      _mrecCustomPosition = position;
      _mrecCustomOffsetX  = offsetX;
      _mrecCustomOffsetY  = offsetY;
      runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
          @Override
          public void run() {
              if (rectAdView == null) {
                  Log.w(TAG, "SetMRECPosition: rectAdView not yet initialized; "
                          + "position saved and will be applied on next load.");
                  return;
              }
              applyAdViewPosition(rectAdView, position, offsetX, offsetY, false);
          }
      });
  }
  ```

  **Step E — Wire custom position into `LoadNormalBanner`** (after the `bannerAdView.setLayoutParams(layoutParams)` call at ~line 1080, before `ViewGroup rootView = activity.findViewById(...)`):
  ```java
  // Apply custom position if one has been set via SetBannerPosition()
  if (_bannerCustomPosition != null) {
      applyAdViewPosition(bannerAdView, _bannerCustomPosition,
              _bannerCustomOffsetX, _bannerCustomOffsetY, true);
  }
  ```

  **Step F — Wire custom position into `LoadMREC`** (after `rectAdView.setLayoutParams(layoutParams)` at ~line 929, before `rectAdView.setVisibility(View.GONE)`):
  ```java
  // Apply custom position if one has been set via SetMRECPosition()
  if (_mrecCustomPosition != null) {
      applyAdViewPosition(rectAdView, _mrecCustomPosition,
              _mrecCustomOffsetX, _mrecCustomOffsetY, false);
  }
  ```

  **Must NOT do**:
  - Do NOT modify `updatePositionMrecAdview` or touch `mFreeMrecAdViews`
  - Do NOT modify `mNativeBannerAdsContainer` / `mNativeRectAdsContainer`
  - Do NOT add to `IAdsService` interface
  - Do NOT remove `_bannerPosition` / `_mrecPosition` int fields
  - Do NOT recompute adaptive banner height (preserve `existingLp.height`)

  **Recommended Agent Profile**:
  > Single file Java edit, straightforward field additions and method insertions.
  - **Category**: `quick`
    - Reason: All changes in one file, no architectural decisions left open, code is directly specified
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 (only task)
  - **Blocks**: F1, F2
  - **Blocked By**: None

  **References**:

  **Pattern References** (existing code to follow):
  - `MaxAdsService.java:399-470` — `updatePositionMrecAdview()`: position string parsing style and gravity logic pattern
  - `MaxAdsService.java:254-271` — `runSafelyOnUiThread()`: thread-dispatch wrapper pattern
  - `MaxAdsService.java:978-1095` — `LoadNormalBanner()`: where banner LayoutParams are created (lines ~1064-1080); wire custom position after `bannerAdView.setLayoutParams(layoutParams)`
  - `MaxAdsService.java:856-964` — `LoadMREC()`: where MREC LayoutParams are created (lines ~926-929); wire custom position after `rectAdView.setLayoutParams(layoutParams)`
  - `MaxAdsService.java:64-134` — volatile field declarations block: add 6 new fields after `_mrecBgColor` (line 117)

  **API/Type References**:
  - `AppLovinSdkUtils.dpToPx(Context, int)` — dp-to-px conversion (used throughout file)
  - `MaxAdFormat.MREC.getSize().getWidth()` / `.getHeight()` — standard MREC dimensions (300×250 dp)
  - `Gravity.TOP`, `Gravity.BOTTOM`, `Gravity.START`, `Gravity.END`, `Gravity.CENTER_HORIZONTAL` — Android gravity flags
  - `FrameLayout.LayoutParams` — the LayoutParams type for both `bannerAdView` and `rectAdView`
  - `MaxAdsService.getSafeInsets()` — returns pixel-valued insets for device cutout

  **Acceptance Criteria**:

  **Build verification:**
  - [ ] `./gradlew :maxads:assembleDebug` exits 0 — no compilation errors

  **Lint:**
  - [ ] `./gradlew :maxads:lint` exits 0 — no new warnings on changed file

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: SetBannerPosition null guard — called before Init()
    Tool: Bash (adb logcat)
    Preconditions: MaxAdsService instance created, Init() NOT yet called, bannerAdView == null
    Steps:
      1. Call maxAdsService.SetBannerPosition("top_center", 0, 0)
      2. Capture Logcat: adb logcat -s MaxAdsService:W *:S
    Expected Result: Log line contains "bannerAdView not yet initialized" at W level, no NullPointerException, no crash
    Failure Indicators: NullPointerException in stack trace, or no W log line emitted
    Evidence: .sisyphus/evidence/task-1-banner-null-guard.txt

  Scenario: SetMRECPosition null guard — called before Init()
    Tool: Bash (adb logcat)
    Preconditions: MaxAdsService instance created, Init() NOT yet called, rectAdView == null
    Steps:
      1. Call maxAdsService.SetMRECPosition("top_center", 0, 0)
      2. Capture Logcat: adb logcat -s MaxAdsService:W *:S
    Expected Result: Log line contains "rectAdView not yet initialized" at W level, no crash
    Failure Indicators: NullPointerException, no log line
    Evidence: .sisyphus/evidence/task-1-mrec-null-guard.txt

  Scenario: SetBannerPosition "top_center" — gravity applied correctly
    Tool: Bash (adb shell + logcat)
    Preconditions: Init() completed, SDK initialized, bannerAdView != null
    Steps:
      1. Call maxAdsService.SetBannerPosition("top_center", 0, 0)
      2. adb logcat -s MaxAdsService:D *:S | grep "applyAdViewPosition"
    Expected Result: Log line contains "Banner pos=top_center gravity=" and a value equal to (Gravity.TOP | Gravity.CENTER_HORIZONTAL) = 17 in decimal; margins contain insets values
    Failure Indicators: ClassCastException, gravity value != 17, no D log line
    Evidence: .sisyphus/evidence/task-1-banner-top-center.txt

  Scenario: SetMRECPosition "bottom_right" with offset — gravity + margin applied correctly
    Tool: Bash (adb logcat)
    Preconditions: Init() completed, rectAdView != null, device density = 3.0 (xxhdpi for verification math)
    Steps:
      1. Call maxAdsService.SetMRECPosition("bottom_right", 16, 8)  // 16dp offsetX, 8dp offsetY
      2. adb logcat -s MaxAdsService:D *:S | grep "applyAdViewPosition"
    Expected Result: Log shows "MREC pos=bottom_right", marginRight = insets.right + 48 (16dp×3), marginBottom = insets.bottom + 24 (8dp×3)
    Failure Indicators: gravity missing END flag, margins show raw dp values (not converted), ClassCastException
    Evidence: .sisyphus/evidence/task-1-mrec-bottom-right-offset.txt

  Scenario: "centered" position for banner — fallback to bottom_center
    Tool: Bash (adb logcat)
    Preconditions: Init() completed, bannerAdView != null
    Steps:
      1. Call maxAdsService.SetBannerPosition("centered", 0, 0)
      2. adb logcat -s MaxAdsService:W *:S | grep "applyAdViewPosition"
    Expected Result: W log contains "'centered' is not supported for Banner" and fallback to 'bottom_center'
    Failure Indicators: No warning logged, or crash, or banner positioned at vertical center
    Evidence: .sisyphus/evidence/task-1-banner-centered-fallback.txt

  Scenario: Position persists after cleanup() + Init() (reinit cycle)
    Tool: Bash (adb logcat)
    Preconditions: Init() completed, bannerAdView created
    Steps:
      1. Call maxAdsService.SetBannerPosition("top_center", 0, 16)
      2. Call maxAdsService.cleanup()
      3. Call maxAdsService.Init(activity, args)  — triggers PreloadBanner → LoadNormalBanner
      4. adb logcat -s MaxAdsService:D *:S | grep "applyAdViewPosition"
    Expected Result: After step 3, logcat shows applyAdViewPosition called with "top_center" offsetY=16 (custom position was re-applied from stored volatile fields)
    Failure Indicators: No applyAdViewPosition log after reinit, gravity reverts to bottom
    Evidence: .sisyphus/evidence/task-1-banner-persist-reinit.txt
  ```

  **Evidence to Capture:**
  - [ ] task-1-banner-null-guard.txt — logcat output showing W log, no NPE
  - [ ] task-1-mrec-null-guard.txt — logcat output showing W log, no NPE
  - [ ] task-1-banner-top-center.txt — logcat showing correct gravity=17
  - [ ] task-1-mrec-bottom-right-offset.txt — logcat showing correct margins in px
  - [ ] task-1-banner-centered-fallback.txt — logcat showing W warning + fallback
  - [ ] task-1-banner-persist-reinit.txt — logcat showing re-applied custom position

  **Commit**: YES
  - Message: `feat(maxads): add SetBannerPosition and SetMRECPosition for custom ad positioning`
  - Files: `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`
  - Pre-commit: `./gradlew :maxads:assembleDebug`

---

## Final Verification Wave

- [x] F1. **Plan Compliance Audit** — `oracle` — **APPROVE**
  Must Have [7/7] | Must NOT Have [7/7] | Tasks [1/1] | VERDICT: APPROVE
  All requirements verified: fields, methods, helper, wire-ins, thread safety, null guards, dp→px conversion, height preservation, persistence.
  All guardrails respected: IAdsService untouched, updatePositionMrecAdview untouched, native containers untouched, int fields preserved.

- [x] F2. **Code Quality Review** — `unspecified-high` — **PASS (after fix)**
  Build [N/A — env lacks Java 11+] | Lint [N/A — env lacks Java 11+]
  Issues found & fixed:
  1. 🔴 BUG (fixed): `"centered"` for MREC silently fell to `bottom_center` instead of `Gravity.CENTER`. Fixed by adding `"centered".equals(pos)` → `Gravity.CENTER` check in gravity resolution block, and `CENTER_VERTICAL` → `marginTop += offsetYPx` in offsetY block (matches existing `updatePositionMrecAdview` behavior).
  2. ⚠️ NPE risk in `getSafeInsets()` (pre-existing, out of scope): `getCurrentActivity()` called without null check. Protected by caller's null guard in `applyAdViewPosition`.
  3. ℹ️ MINOR: `requestLayout()` called before view attached (wire-ins in LoadNormalBanner/LoadMREC). Semantically noisy but behaviorally correct — LayoutParams are set before `addView`.
  VERDICT: PASS after centered-gravity fix

---

## Commit Strategy

- **1**: `feat(maxads): add SetBannerPosition and SetMRECPosition for custom ad positioning`
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`
  - Pre-commit: `./gradlew :maxads:assembleDebug`

---

## Success Criteria

### Verification Commands
```bash
./gradlew :maxads:assembleDebug   # Expected: BUILD SUCCESSFUL
./gradlew :maxads:lint            # Expected: Lint found 0 errors, 0 warnings (in changed file)
```

### Final Checklist
- [ ] `SetBannerPosition(String, int, int)` public method present
- [ ] `SetMRECPosition(String, int, int)` public method present
- [ ] Private `applyAdViewPosition(MaxAdView, String, int, int, boolean)` helper present
- [ ] 6 new volatile fields for custom position state declared
- [ ] `LoadNormalBanner` applies `_bannerCustomPosition` when non-null
- [ ] `LoadMREC` applies `_mrecCustomPosition` when non-null
- [ ] `IAdsService.java` NOT modified
- [ ] `updatePositionMrecAdview` NOT modified
- [ ] `./gradlew :maxads:assembleDebug` exits 0

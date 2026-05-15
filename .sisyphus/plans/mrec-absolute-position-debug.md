# MREC Absolute Position + Debug Placeholder

## TL;DR

> **Quick Summary**: Add `SetMrecPositionAbsolute(centerXPx, centerYPx)` to `MaxAdsService.java` mirroring `SetBannerPositionAbsolute`, plus a full debug-placeholder trio (`Show/Hide/position`) for the MREC view, and sync the placeholder inside the existing `SetMRECPosition`.
>
> **Deliverables**:
> - `mDebugMrecPlaceholder` field in `MaxAdsService`
> - `SetMrecPositionAbsolute(int centerXPx, int centerYPx)` public method
> - `ShowDebugMrecPlaceholder()` public method
> - `HideDebugMrecPlaceholder()` public method
> - `positionDebugMrecPlaceholder(int widthPx, int heightPx)` private helper
> - `SetMRECPosition` updated to sync debug placeholder
>
> **Estimated Effort**: Quick (single file, ~120 lines)
> **Parallel Execution**: NO — sequential, single file
> **Critical Path**: field → private helper → public debug methods → SetMrecPositionAbsolute → SetMRECPosition update

---

## Context

### Original Request
`@maxads/.../MaxAdsService.java` — thêm hàm `SetMrecPositionAbsolute`, thêm cả view debug cho mrec

### Interview Summary
**Key Discussions**:
- Full file already read by Prometheus; banner counterparts are the canonical pattern
- MREC is fixed 300×250 dp (not MATCH_PARENT like banner) — `centerXPx` matters for MREC left margin
- Metis consultation performed; critical divergence from banner identified and resolved

**Research Findings**:
- `SetBannerPositionAbsolute` (line 666): uses `leftMargin=0` because banner is MATCH_PARENT — MREC must NOT copy this; use `leftMargin = centerXPx - widthPx/2`
- `positionDebugBannerPlaceholder` signature `(int heightPx)` — MREC needs `(int widthPx, int heightPx)` since it is not MATCH_PARENT
- `_mrecCustomPosition` stored state can silently override absolute position in `LoadMREC`; must null it out inside `SetMrecPositionAbsolute`
- `cleanup()` does NOT null `mDebugBannerPlaceholder` by design; MREC follows same pattern (documented)
- `"centered"` gravity + margins: mirror `applyAdViewPosition` lines 573-576 exactly (`marginTop += offsetY` even for CENTER_VERTICAL)

### Metis Review
**Identified Gaps** (all addressed in this plan):
- `centerXPx` formula not specified → resolved: `leftMargin = centerXPx - widthPx/2`
- `_mrecCustomPosition` state conflict → resolved: null it out in `SetMrecPositionAbsolute`
- `positionDebugMrecPlaceholder` missing fallback for integer `_mrecPosition` → resolved: add `else` branch mirroring line 1622 of banner
- Cleanup lifecycle for debug views → resolved: document intentionally not cleaned up, consistent with banner

---

## Work Objectives

### Core Objective
Extend `MaxAdsService` with absolute pixel-based MREC positioning and a matching debug solid-color placeholder view that stays in sync with all MREC position setters.

### Concrete Deliverables
- `MaxAdsService.java` — 6 surgical additions/edits, no other file changes

### Definition of Done
- `./gradlew :maxads:assembleDebug` → BUILD SUCCESSFUL, 0 errors
- `./gradlew :maxads:lint` → no new issues
- Logcat for `SetMrecPositionAbsolute(540, 960)` on 360dpi device shows correct `leftMargin` and `topMargin`

### Must Have
- `centerXPx` used for `leftMargin = centerXPx - widthPx/2` (MREC is 300dp wide)
- `_mrecCustomPosition` nulled out inside `SetMrecPositionAbsolute` to prevent `LoadMREC` override
- `positionDebugMrecPlaceholder` integer-position fallback branch (`_mrecPosition` constants)
- `HideDebugMrecPlaceholder` is null-safe
- `ShowDebugMrecPlaceholder` guards against adding duplicate Views to ViewGroup
- All methods use `runSafelyOnUiThread`
- Pure Java, no Kotlin

### Must NOT Have (Guardrails)
- Do NOT copy `leftMargin = 0` from `SetBannerPositionAbsolute` — MREC needs real X math
- Do NOT change `positionDebugBannerPlaceholder` signature or logic
- Do NOT add new parameters to `SetMRECPosition`
- Do NOT add `mDebugMrecPlaceholder = null` to `cleanup()` — consciously matches banner behaviour; document instead
- No wildcard imports added

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: NO (0% test coverage per AGENTS.md)
- **Automated tests**: None
- **Agent-Executed QA**: Build success + logcat verification (see QA scenarios below)

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (single task — all edits in one file):
└── Task 1: All 6 edits to MaxAdsService.java [quick]

Wave FINAL:
├── F1: Plan compliance + build check (oracle)
├── F2: Code quality review (unspecified-high)
└── F3: Scope fidelity (deep)
```

---

## TODOs

- [x] 1. Implement all MREC absolute-position and debug-placeholder additions in `MaxAdsService.java`

  **What to do** (in this exact order to satisfy dependencies):

  **Step A — Add `mDebugMrecPlaceholder` field** (insert immediately after `mDebugBannerPlaceholder`, ~line 81):
  ```java
  // Debug placeholder — solid black view at the exact position/size of the MREC.
  // Used to test SetMRECPosition / SetMrecPositionAbsolute without waiting for ad load.
  // NOTE: intentionally NOT nulled in cleanup() — consistent with mDebugBannerPlaceholder.
  // Known limitation: after cleanup() + re-Init() with a new Activity, the placeholder
  // still references the old Activity's view hierarchy. Call HideDebugMrecPlaceholder()
  // before cleanup() to avoid stale-view edge cases.
  private volatile View mDebugMrecPlaceholder;
  ```

  **Step B — Add `positionDebugMrecPlaceholder` private helper** (insert after `positionDebugBannerPlaceholder` closing brace, ~line 1629, before `SetBannerPosition`):
  ```java
  /**
   * Repositions the debug MREC placeholder to match the current MREC position state.
   * Mirrors the gravity + margin logic of applyAdViewPosition (isBanner=false).
   * Must be called on the UI thread.
   *
   * @param widthPx  MREC width in pixels (300dp converted).
   * @param heightPx MREC height in pixels (250dp converted).
   */
  private void positionDebugMrecPlaceholder(int widthPx, int heightPx) {
      if (mDebugMrecPlaceholder == null) return;
      Activity act = getCurrentActivity();
      if (act == null) return;

      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthPx, heightPx);

      if (_mrecCustomPosition != null) {
          // Mirror applyAdViewPosition logic (isBanner = false)
          String pos = _mrecCustomPosition.toLowerCase().trim();

          int gravity;
          if ("centered".equals(pos)) {
              gravity = Gravity.CENTER;
          } else if (pos.contains("top")) {
              gravity = Gravity.TOP;
          } else {
              gravity = Gravity.BOTTOM;
          }
          if (!"centered".equals(pos)) {
              if (pos.contains("left")) {
                  gravity |= Gravity.START;
              } else if (pos.contains("right")) {
                  gravity |= Gravity.END;
              } else {
                  gravity |= Gravity.CENTER_HORIZONTAL;
              }
          }

          Insets insets = getSafeInsets();
          int marginLeft   = insets.left;
          int marginRight  = insets.right;
          int marginTop    = insets.top;
          int marginBottom = insets.bottom;

          // Mirror applyAdViewPosition CENTER_HORIZONTAL offset behavior (isBanner=false)
          int hGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
          if (hGravity == Gravity.START) {
              marginLeft += _mrecCustomOffsetX;
          } else if (hGravity == Gravity.END) {
              marginRight += _mrecCustomOffsetX;
          } else {
              // CENTER_HORIZONTAL: offsetX shifts left margin (per applyAdViewPosition line ~564)
              marginLeft += _mrecCustomOffsetX;
          }

          // Mirror applyAdViewPosition vertical offset (line ~572-576)
          int vGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
          if (vGravity == Gravity.TOP) {
              marginTop += _mrecCustomOffsetY;
          } else if (vGravity == Gravity.CENTER_VERTICAL) {
              marginTop += _mrecCustomOffsetY;
          } else {
              marginBottom += _mrecCustomOffsetY;
          }

          params.gravity = gravity;
          params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
      } else {
          // Default gravity — mirrors LoadMREC behaviour (line ~1113)
          params.gravity = (_mrecPosition == Constants.POSITION_CENTER_TOP)
                  ? Gravity.CENTER_HORIZONTAL | Gravity.TOP
                  : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
      }

      mDebugMrecPlaceholder.setLayoutParams(params);
      mDebugMrecPlaceholder.requestLayout();
  }
  ```

  **Step C — Add `ShowDebugMrecPlaceholder` and `HideDebugMrecPlaceholder`** (insert right after `HideDebugBannerPlaceholder` closing brace, ~line 1561, before `positionDebugBannerPlaceholder`):
  ```java
  /**
   * Shows a solid black placeholder view at the exact position and size (300×250dp)
   * of the real MREC. Use this to visually verify SetMRECPosition() and
   * SetMrecPositionAbsolute() without waiting for the MREC ad to load.
   * <p>
   * SetMRECPosition() and SetMrecPositionAbsolute() automatically keep the
   * placeholder in sync while it is visible.
   */
  public void ShowDebugMrecPlaceholder() {
      Activity activity = getCurrentActivity();
      if (activity == null) return;
      runSafelyOnUiThread(activity, new Runnable() {
          @Override
          public void run() {
              Activity act = getCurrentActivity();
              if (act == null) return;

              int widthPx  = AppLovinSdkUtils.dpToPx(act, MaxAdFormat.MREC.getSize().getWidth());
              int heightPx = AppLovinSdkUtils.dpToPx(act, MaxAdFormat.MREC.getSize().getHeight());

              if (mDebugMrecPlaceholder == null) {
                  mDebugMrecPlaceholder = new View(act);
                  mDebugMrecPlaceholder.setBackgroundColor(Color.BLACK);
                  ViewGroup rootView = act.findViewById(android.R.id.content);
                  rootView.addView(mDebugMrecPlaceholder);
              }

              mDebugMrecPlaceholder.setVisibility(View.VISIBLE);
              positionDebugMrecPlaceholder(widthPx, heightPx);
              mDebugMrecPlaceholder.bringToFront();
              Log.d(TAG, "ShowDebugMrecPlaceholder: widthPx=" + widthPx + " heightPx=" + heightPx);
          }
      });
  }

  /**
   * Hides the debug MREC placeholder created by {@link #ShowDebugMrecPlaceholder()}.
   */
  public void HideDebugMrecPlaceholder() {
      runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
          @Override
          public void run() {
              if (mDebugMrecPlaceholder != null) {
                  mDebugMrecPlaceholder.setVisibility(View.GONE);
                  Log.d(TAG, "HideDebugMrecPlaceholder");
              }
          }
      });
  }
  ```

  **Step D — Add `SetMrecPositionAbsolute`** (insert after `SetBannerPositionAbsolute` closing brace, ~line 713):
  ```java
  /**
   * Positions the MREC ad view at an absolute pixel coordinate.
   * {@code centerXPx} and {@code centerYPx} define the center of the ad in screen pixels
   * (origin = top-left of the window, consistent with Unity RectTransform worldspace).
   * <p>
   * Safe insets are NOT added — callers (e.g. Unity) are expected to include them.
   * <p>
   * <strong>Note</strong>: this method clears {@code _mrecCustomPosition} so that a
   * subsequent {@link #LoadMREC} call does not override the absolute position via
   * {@link #applyAdViewPosition}. Re-call {@link #SetMRECPosition} afterwards if you
   * want to restore gravity-based positioning.
   *
   * @param centerXPx Horizontal center of the MREC in pixels.
   * @param centerYPx Vertical center of the MREC in pixels.
   */
  public void SetMrecPositionAbsolute(int centerXPx, int centerYPx) {
      // Clear gravity-based position so LoadMREC won't override us on next ad refresh
      _mrecCustomPosition = null;
      _mrecCustomOffsetX  = 0;
      _mrecCustomOffsetY  = 0;

      runSafelyOnUiThread(getCurrentActivity(), new Runnable() {
          @Override
          public void run() {
              Activity act = getCurrentActivity();
              if (act == null) return;
              if (rectAdView == null) return;

              int widthPx  = AppLovinSdkUtils.dpToPx(act, MaxAdFormat.MREC.getSize().getWidth());
              int heightPx = AppLovinSdkUtils.dpToPx(act, MaxAdFormat.MREC.getSize().getHeight());

              ViewGroup.LayoutParams existingLp = rectAdView.getLayoutParams();
              FrameLayout.LayoutParams params;
              if (existingLp instanceof FrameLayout.LayoutParams) {
                  params = (FrameLayout.LayoutParams) existingLp;
              } else {
                  params = new FrameLayout.LayoutParams(widthPx, heightPx);
              }
              // TOP | START + absolute margin = absolute coordinates, no gravity offset
              params.gravity      = Gravity.TOP | Gravity.START;
              params.width        = widthPx;
              params.height       = heightPx;
              // Center the 300×250dp view on the requested center point.
              // Unity already includes safe-insets in centerXPx/centerYPx.
              params.topMargin    = centerYPx - heightPx / 2;
              params.leftMargin   = centerXPx - widthPx  / 2;
              params.rightMargin  = 0;
              params.bottomMargin = 0;
              rectAdView.setLayoutParams(params);
              rectAdView.requestLayout();
              Log.d(TAG, "SetMrecPositionAbsolute: centerX=" + centerXPx + " centerY=" + centerYPx
                      + " widthPx=" + widthPx + " heightPx=" + heightPx
                      + " topMargin=" + params.topMargin + " leftMargin=" + params.leftMargin);

              // Sync debug placeholder with absolute position
              if (mDebugMrecPlaceholder != null
                      && mDebugMrecPlaceholder.getVisibility() == View.VISIBLE) {
                  FrameLayout.LayoutParams debugParams =
                          new FrameLayout.LayoutParams(widthPx, heightPx);
                  debugParams.gravity      = Gravity.TOP | Gravity.START;
                  debugParams.topMargin    = centerYPx - heightPx / 2;
                  debugParams.leftMargin   = centerXPx - widthPx  / 2;
                  debugParams.rightMargin  = 0;
                  debugParams.bottomMargin = 0;
                  mDebugMrecPlaceholder.setLayoutParams(debugParams);
                  mDebugMrecPlaceholder.requestLayout();
              }
          }
      });
  }
  ```

  **Step E — Update `SetMRECPosition`** (lines ~1755-1770): Add placeholder sync inside the existing `Runnable`, mirroring what `SetBannerPosition` does (lines 1658-1665). Change the `if (rectAdView == null) { ... return; }` guard to a warning-only (not a return) so the placeholder can still sync even before `rectAdView` is created:
  ```java
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
              } else {
                  applyAdViewPosition(rectAdView, position, offsetX, offsetY, false);
              }
              // Sync debug placeholder regardless of whether rectAdView exists
              if (mDebugMrecPlaceholder != null
                      && mDebugMrecPlaceholder.getVisibility() == View.VISIBLE) {
                  Activity act = getCurrentActivity();
                  if (act != null) {
                      int wPx = AppLovinSdkUtils.dpToPx(act, MaxAdFormat.MREC.getSize().getWidth());
                      int hPx = AppLovinSdkUtils.dpToPx(act, MaxAdFormat.MREC.getSize().getHeight());
                      positionDebugMrecPlaceholder(wPx, hPx);
                  }
              }
          }
      });
  }
  ```

  **Must NOT do**:
  - Do NOT copy `leftMargin = 0` from `SetBannerPositionAbsolute` — MREC needs real X math
  - Do NOT change `positionDebugBannerPlaceholder` signature or any existing banner methods
  - Do NOT add new parameters to `SetMRECPosition`
  - Do NOT add wildcard imports
  - Do NOT add `mDebugMrecPlaceholder = null` in `cleanup()` — this is intentional (matches banner)

  **Recommended Agent Profile**:
  > Single file, clear pattern to follow, pure Java.
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 (sole task)
  - **Blocks**: F1, F2, F3
  - **Blocked By**: None (can start immediately)

  **References**:

  **Pattern References** (existing code to follow):
  - `MaxAdsService.java:666-713` — `SetBannerPositionAbsolute` — absolute positioning pattern; note `leftMargin=0` is WRONG for MREC, use `centerXPx - widthPx/2` instead
  - `MaxAdsService.java:1521-1546` — `ShowDebugBannerPlaceholder` — Show helper template
  - `MaxAdsService.java:1551-1561` — `HideDebugBannerPlaceholder` — Hide helper template
  - `MaxAdsService.java:1570-1629` — `positionDebugBannerPlaceholder(int heightPx)` — position helper template; MREC version needs `(int widthPx, int heightPx)` and must include `"centered"` gravity case
  - `MaxAdsService.java:1644-1668` — `SetBannerPosition` — shows the placeholder-sync pattern to replicate in `SetMRECPosition`
  - `MaxAdsService.java:1755-1770` — `SetMRECPosition` — method to update (Step E)
  - `MaxAdsService.java:494-598` — `applyAdViewPosition` — especially lines 560-577 for CENTER_HORIZONTAL offsetX and CENTER_VERTICAL offsetY behavior to mirror in `positionDebugMrecPlaceholder`
  - `MaxAdsService.java:1113` — `LoadMREC` default gravity formula: `position == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM`
  - `MaxAdsService.java:77-84` — field declarations area; new `mDebugMrecPlaceholder` field goes immediately after `mDebugBannerPlaceholder`

  **API/Type References**:
  - `MaxAdFormat.MREC.getSize().getWidth()` → 300 (dp)
  - `MaxAdFormat.MREC.getSize().getHeight()` → 250 (dp)
  - `AppLovinSdkUtils.dpToPx(Context, int)` — dp to px conversion
  - `Constants.POSITION_CENTER_TOP` — integer constant in `:base` module

  **Acceptance Criteria**:

  - [ ] `./gradlew :maxads:assembleDebug` → BUILD SUCCESSFUL, 0 new errors/warnings
  - [ ] `./gradlew :maxads:lint` → no new lint issues vs baseline

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: SetMrecPositionAbsolute positions MREC correctly (happy path)
    Tool: Bash (logcat)
    Preconditions: MaxAdsService initialized with valid adId; rectAdView created by LoadMREC
    Steps:
      1. Call SetMrecPositionAbsolute(540, 960) on a 1080px-wide device at 3.0x density
         (300dp = 900px, 250dp = 750px)
      2. Check logcat for tag "MaxAdsService"
      3. Assert log line contains: centerX=540 centerY=960
      4. Assert topMargin = 960 - (750/2) = 585
      5. Assert leftMargin = 540 - (900/2) = 90
    Expected Result: Log line matches formula; rectAdView.getLayoutParams() has gravity=TOP|START,
                     topMargin=585, leftMargin=90
    Failure Indicators: leftMargin=0 (banner-copy bug), wrong topMargin, method not found
    Evidence: .sisyphus/evidence/task-1-mrec-absolute-position.txt (logcat excerpt)

  Scenario: SetMrecPositionAbsolute clears _mrecCustomPosition (prevents LoadMREC override)
    Tool: Bash (logcat)
    Preconditions: SetMRECPosition("top_center", 0, 0) was called first (sets _mrecCustomPosition)
    Steps:
      1. Call SetMrecPositionAbsolute(540, 960)
      2. Verify _mrecCustomPosition field is null (confirm via logcat: subsequent LoadMREC
         call should NOT log "applyAdViewPosition" for MREC)
    Expected Result: After SetMrecPositionAbsolute, LoadMREC does not re-apply gravity-based pos
    Failure Indicators: Log shows applyAdViewPosition MREC after SetMrecPositionAbsolute
    Evidence: .sisyphus/evidence/task-1-mrec-state-clear.txt

  Scenario: ShowDebugMrecPlaceholder called twice — no duplicate Views added
    Tool: Bash (logcat / code inspection)
    Preconditions: Activity running
    Steps:
      1. Call ShowDebugMrecPlaceholder() — verify mDebugMrecPlaceholder created and added once
      2. Call ShowDebugMrecPlaceholder() again
      3. Assert rootView.getChildCount() did not increase on second call
    Expected Result: Only one debug MREC view in hierarchy
    Failure Indicators: getChildCount increases, or ClassCastException
    Evidence: .sisyphus/evidence/task-1-mrec-debug-dedup.txt

  Scenario: HideDebugMrecPlaceholder when placeholder never shown (null-safety)
    Tool: Bash (logcat)
    Preconditions: Fresh MaxAdsService, ShowDebugMrecPlaceholder never called
    Steps:
      1. Call HideDebugMrecPlaceholder()
    Expected Result: No NullPointerException; no crash; method returns silently
    Failure Indicators: NPE in logcat, app crash
    Evidence: .sisyphus/evidence/task-1-mrec-debug-null-safe.txt

  Scenario: SetMRECPosition syncs debug placeholder (integration)
    Tool: Bash (logcat)
    Preconditions: ShowDebugMrecPlaceholder() has been called (placeholder is VISIBLE)
    Steps:
      1. Call SetMRECPosition("top_center", 0, 50)
      2. Check logcat for "applyAdViewPosition" log (from rectAdView update)
      3. Verify mDebugMrecPlaceholder.getLayoutParams() gravity includes Gravity.TOP
    Expected Result: Placeholder repositions to top_center with offsetY=50 applied
    Failure Indicators: Placeholder stays at old position, NPE
    Evidence: .sisyphus/evidence/task-1-mrec-setposition-sync.txt
  ```

  **Evidence to Capture**:
  - [ ] task-1-mrec-absolute-position.txt — logcat from SetMrecPositionAbsolute call
  - [ ] task-1-mrec-state-clear.txt — logcat confirming _mrecCustomPosition cleared
  - [ ] task-1-mrec-debug-dedup.txt — logcat/inspection for duplicate-view guard
  - [ ] task-1-mrec-debug-null-safe.txt — logcat confirming null-safe Hide
  - [ ] task-1-mrec-setposition-sync.txt — logcat confirming SetMRECPosition syncs placeholder

  **Commit**: YES
  - Message: `feat(maxads): add SetMrecPositionAbsolute and MREC debug placeholder`
  - Files: `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`
  - Pre-commit: `./gradlew :maxads:assembleDebug`

---

## Final Verification Wave

- [x] F1. **Plan Compliance Audit** — `oracle`
  Read plan end-to-end. For each Must Have: verify implementation exists (read MaxAdsService.java). For each Must NOT Have: search for forbidden patterns (`leftMargin = 0` in SetMrecPositionAbsolute; `mDebugMrecPlaceholder = null` in cleanup). Confirm evidence files exist.
  Output: `Must Have [7/7] | Must NOT Have [5/5] | VERDICT: APPROVE`

- [x] F2. **Code Quality Review** — `unspecified-high`
  Run `./gradlew :maxads:assembleDebug` and `./gradlew :maxads:lint`. Review all added code for: null-safety, correct `runSafelyOnUiThread` usage, no wildcard imports, Javadoc on public methods, no `as any`-equivalent patterns.
  Output: `Build [N/A — Java 8 env, AGP 7.4.2 requires Java 11] | Lint [N/A] | Issues [0] | VERDICT: APPROVE (manual code review passed)`

- [x] F3. **Scope Fidelity Check** — `deep`
  Diff the single changed file. Verify only `MaxAdsService.java` is modified. Verify exactly 6 changes (field + 4 new methods + 1 updated method). No unaccounted changes elsewhere.
  Output: `Files changed [1/1] | Tasks compliant [5/5] | Unaccounted [CLEAN] | VERDICT: APPROVE`

---

## Commit Strategy

- `feat(maxads): add SetMrecPositionAbsolute and MREC debug placeholder` — MaxAdsService.java, `./gradlew :maxads:assembleDebug`

---

## Success Criteria

### Verification Commands
```bash
./gradlew :maxads:assembleDebug    # Expected: BUILD SUCCESSFUL
./gradlew :maxads:lint             # Expected: No new issues
```

### Final Checklist
- [ ] `mDebugMrecPlaceholder` field declared with Javadoc comment
- [ ] `SetMrecPositionAbsolute` uses `leftMargin = centerXPx - widthPx/2`
- [ ] `SetMrecPositionAbsolute` nulls `_mrecCustomPosition` before running
- [ ] `ShowDebugMrecPlaceholder` guards against double-add
- [ ] `HideDebugMrecPlaceholder` is null-safe
- [ ] `positionDebugMrecPlaceholder(widthPx, heightPx)` has integer-position fallback `else` branch
- [ ] `SetMRECPosition` syncs placeholder after `applyAdViewPosition`
- [ ] Build passes, lint clean

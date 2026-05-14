# AdmobHelper — Custom (x, y) Position for Banner & MREC

## TL;DR

> **Quick Summary**: Add POSITION_CUSTOM coordinate support and runtime `setBannerPosition()`/`setMrecPosition()` overloads to `AdmobHelper.java`, with backward-compatible API and thread-safe field management.
>
> **Deliverables**:
> - 3 new volatile instance fields: `bannerXOffset`, `bannerYOffset`, `mrecXOffset`
> - New `getLayoutParams(positionCode, xDp, yDp)` overload (old 2-arg delegates to it)
> - New `getBannerLayoutParams(positionCode, xDp, yDp)` (MATCH_PARENT width variant)
> - Fixed `initBanner()` — respects stored offsets on re-init
> - New `public setBannerPosition(int positionCode, int xDp, int yDp)` + private `updateBannerPosition()`
> - New `public setMrecPosition(int positionCode, int xDp, int yDp)` overload; old 2-arg preserved
>
> **Estimated Effort**: Short
> **Parallel Execution**: YES — Wave 4 runs Tasks 4 & 5 in parallel
> **Critical Path**: T1 → T2 → T3 → T4/T5 → F1–F4

---

## Context

### Original Request
> `@admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java tôi muốn có thêm chức năng custom vị trí cho banner, mrec`
> (Translation: "I want to add custom position functionality for banner and MREC")

### Interview Summary

**Key Discussions**:
- Coordinate system: **(0,0) = Top-Left**; xDp from left edge, yDp from top edge
- Banner API: `setBannerPosition(int positionCode, int xDp, int yDp)` — runtime method, mirrors MREC
- MREC strategy: Add overload `setMrecPosition(positionCode, xDp, yDp)`; keep old `setMrecPosition(positionCode, offsetY)` for backward compat (calls new 3-arg version with `xDp=0`)
- Banner width stays MATCH_PARENT always — `xDp` when POSITION_CUSTOM causes right-side clipping; documented, not a bug to fix

**Research Findings**:
- `getLayoutParams()` is `protected` — subclasses may override; delegation must be careful
- `CreateMrecAdView` at line 1533 calls `getLayoutParams(positionCode, 0)` — old 2-arg must remain valid
- `initBanner()` (lines 361-366) uses hardcoded ternary gravity; does NOT call `getLayoutParams()`
- `bannerXOffset`/`bannerYOffset`/`mrecXOffset` fields never existed; commented skeleton at lines 703-705
- `bannerPosition` field (line 140) is non-volatile int; offset fields must be `volatile`
- `getSafeInsets()` at line 776 has pre-existing NPE risk (no null check on `getWindow()`) — out of scope
- Line 632: collapsible string for POSITION_CUSTOM (-1) already falls to "bottom" branch — no change needed

### Metis Review (previous session)
**Identified Gaps** (addressed in this plan):
- Synchronized writes: bannerPosition + bannerXOffset + bannerYOffset must be written together → `setBannerPosition()` uses synchronized block
- Overload resolution: old `setMrecPosition(positionCode, offsetY)` → calls `setMrecPosition(positionCode, 0, offsetY)` for xDp=0 default
- xDp only meaningful for `POSITION_CUSTOM`; documented in method Javadoc
- `initBanner()` must use stored offsets on re-init → uses `getBannerLayoutParams(bannerPosition, bannerXOffset, bannerYOffset)`

---

## Work Objectives

### Core Objective
Extend `AdmobHelper.java` to support absolute (x, y) dp-based custom positioning for both Banner and MREC ad views, while keeping the existing API fully backward-compatible.

### Concrete Deliverables
- `AdmobHelper.java` modified (~100–130 added lines)

### Definition of Done
- [ ] `./gradlew :admobadshelper:assembleDebug` exits 0
- [ ] `./gradlew :admobadshelper:lint` exits 0 (no new lint errors)
- [ ] `setBannerPosition(-1, 100, 200)` positions banner at (100dp, 200dp) from top-left
- [ ] `setMrecPosition(-1, 50, 150)` positions MREC at (50dp, 150dp) from top-left
- [ ] Old `setMrecPosition(0, 0)` still compiles and behaves unchanged

### Must Have
- `setBannerPosition(int positionCode, int xDp, int yDp)` — public runtime method
- `setMrecPosition(int positionCode, int xDp, int yDp)` — public overload
- Backward compat: old `setMrecPosition(positionCode, offsetY)` unchanged
- Thread-safe writes for all new offset fields
- `initBanner()` uses stored `bannerXOffset`/`bannerYOffset` on re-init
- Java only — no Kotlin

### Must NOT Have (Guardrails)
- **No Kotlin** — Java only, per `.github/instructions/java_dev.instructions.md`
- **No null collection returns** — return empty/defaults, never null
- **No fixing unrelated pre-existing bugs** — `getSafeInsets()` NPE is out of scope
- **No orientation-change handling** — out of scope, noted only
- **No minSdk API violations** — current minSdk is 21; no API > 21 without version guard
- **No breaking change to `getLayoutParams(positionCode, topPadding)` signature** — line 1533 must keep compiling

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: NO (0% test coverage, only scaffold files)
- **Automated tests**: NONE (no test framework configured)
- **Agent-Executed QA**: YES — mandatory for all tasks

### QA Policy
Every task includes agent-executed QA scenarios.
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Build verification**: `./gradlew :admobadshelper:assembleDebug`
- **Lint**: `./gradlew :admobadshelper:lint`
- **Compile-only checks**: `./gradlew :admobadshelper:compileDebugJavaWithJavac`

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start immediately — no dependencies):
└── Task 1: Add new instance fields (bannerXOffset, bannerYOffset, mrecXOffset)

Wave 2 (After Wave 1 — MREC layout foundation):
└── Task 2: Add getLayoutParams(positionCode, xDp, yDp) overload; delegate old 2-arg to it

Wave 3 (After Wave 2 — Banner layout foundation):
└── Task 3: Add getBannerLayoutParams(positionCode, xDp, yDp); fix initBanner() to use stored offsets

Wave 4 (After Wave 3 — public API, PARALLEL safe — different line regions):
├── Task 4: MREC — add setMrecPosition(positionCode, xDp, yDp); update updateMrecPosition() to read mrecXOffset
└── Task 5: Banner — add updateBannerPosition() + setBannerPosition(positionCode, xDp, yDp)

Wave FINAL (After ALL — 4 parallel reviews, then user okay):
├── F1: Plan Compliance Audit (oracle)
├── F2: Code Quality Review (unspecified-high)
├── F3: Real Manual QA (unspecified-high)
└── F4: Scope Fidelity Check (deep)
→ Present results → Get explicit user okay

Critical Path: T1 → T2 → T3 → T4 → F1–F4 → user okay
Parallel Speedup: Wave 4 saves ~40% vs fully sequential
Max Concurrent: 2 (Wave 4) + 4 (Wave FINAL)
```

### Dependency Matrix

| Task | Depends On | Blocks |
|------|-----------|--------|
| T1   | —         | T2, T3, T4, T5 |
| T2   | T1        | T3, T4 |
| T3   | T2        | T5 |
| T4   | T2 (getLayoutParams overload exists) | F-wave |
| T5   | T3 (getBannerLayoutParams exists) | F-wave |
| F1–F4 | T4, T5   | user okay |

### Agent Dispatch Summary

- **Wave 1**: 1 task → T1: `quick`
- **Wave 2**: 1 task → T2: `quick`
- **Wave 3**: 1 task → T3: `quick`
- **Wave 4**: 2 parallel → T4: `unspecified-high`, T5: `unspecified-high`
- **Wave FINAL**: 4 parallel → F1: `oracle`, F2: `unspecified-high`, F3: `unspecified-high`, F4: `deep`

---

## TODOs

- [ ] 1. Add new instance fields: `bannerXOffset`, `bannerYOffset`, `mrecXOffset`

  **What to do**:
  - Open `AdmobHelper.java`
  - After line 159 (end of the `volatile` flag block), add three new volatile fields:
    ```java
    private volatile int bannerXOffset = 0;
    private volatile int bannerYOffset = 0;
    private volatile int mrecXOffset = 0;
    ```
  - Also remove the three commented-out field declarations at lines 703-705 (`mPositionCode`, `mHorizontalOffset`, `mVerticalOffset`) since the new fields replace them
  - Do NOT change `int bannerPosition;` at line 140 — leave it as-is (non-volatile, matches existing style)

  **Must NOT do**:
  - Do not rename or modify any existing fields
  - Do not add Kotlin; Java only
  - Do not touch any methods — this task is fields-only

  **Recommended Agent Profile**:
  > Single-file, minimal edit — add 3 fields, delete 3 comment lines.
  - **Category**: `quick`
    - Reason: Mechanical field addition, no logic involved
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `playwright`: UI-only, not applicable here

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 — sole task
  - **Blocks**: Tasks 2, 3, 4, 5 (all need these fields)
  - **Blocked By**: None (start immediately)

  **References**:

  **Pattern References**:
  - `AdmobHelper.java:145-159` — Existing `volatile` flag block; add new fields immediately after line 159, matching the same `private volatile int` style
  - `AdmobHelper.java:703-705` — Commented-out old fields to DELETE (`// private int mPositionCode; // private int mHorizontalOffset; // private int mVerticalOffset;`)

  **Acceptance Criteria**:

  **QA Scenarios**:

  ```
  Scenario: New fields exist and compile
    Tool: Bash
    Preconditions: Task edit applied, fields added after line 159
    Steps:
      1. Run: ./gradlew :admobadshelper:compileDebugJavaWithJavac
      2. Assert: exit code 0, no "cannot find symbol" errors
    Expected Result: Compilation succeeds; no new errors
    Failure Indicators: Any compilation error mentioning the new field names
    Evidence: .sisyphus/evidence/task-1-compile.txt

  Scenario: Old commented fields removed
    Tool: Bash
    Preconditions: Task edit applied
    Steps:
      1. Run: grep -n "mHorizontalOffset\|mVerticalOffset\|mPositionCode" admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java
      2. Assert: zero matches returned
    Expected Result: grep returns empty — commented-out dead code is gone
    Failure Indicators: Any line containing mHorizontalOffset still present
    Evidence: .sisyphus/evidence/task-1-dead-code-check.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-1-compile.txt` — stdout/stderr of compileDebugJavaWithJavac
  - [ ] `.sisyphus/evidence/task-1-dead-code-check.txt` — grep output confirming old comments removed

  **Commit**: NO — grouped with all other tasks in final single commit

- [ ] 2. Add `getLayoutParams(positionCode, xDp, yDp)` overload; delegate old 2-arg method to it

  **What to do**:
  - In `AdmobHelper.java`, locate `protected FrameLayout.LayoutParams getLayoutParams(int positionCode, int topPadding)` at line 725
  - Add a NEW 3-argument overload **above** the existing 2-arg method:
    ```java
    /**
     * Returns FrameLayout params for MREC positioning.
     * For POSITION_CUSTOM (-1): positions at absolute (xDp, yDp) from top-left, respecting safe insets.
     * For other positions: xDp is ignored; yDp acts as extra top padding offset (same as legacy topPadding).
     */
    protected FrameLayout.LayoutParams getLayoutParams(int positionCode, int xDp, int yDp) {
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        adParams.gravity = AdmobHelper.getLayoutGravityForPositionCode(positionCode);
        Insets insets = getSafeInsets();
        int safeInsetLeft = insets.left;
        int safeInsetTop = insets.top;

        adParams.bottomMargin = insets.bottom;
        adParams.rightMargin = insets.right;

        if (positionCode == POSITION_CUSTOM) {
            int leftOffset = (int) AdmobHelper.convertDpToPixel(xDp);
            if (leftOffset < safeInsetLeft) leftOffset = safeInsetLeft;
            int topOffset = (int) AdmobHelper.convertDpToPixel(yDp);
            if (topOffset < safeInsetTop) topOffset = safeInsetTop;
            adParams.leftMargin = leftOffset;
            adParams.topMargin = topOffset;
        } else {
            adParams.leftMargin = safeInsetLeft;
            if (positionCode == POSITION_TOP_CENTER
                    || positionCode == POSITION_TOP_LEFT
                    || positionCode == POSITION_TOP_RIGHT
                    || positionCode == POSITION_CENTER) {
                int topOffsetPixel = (int) AdmobHelper.convertDpToPixel(yDp);
                adParams.topMargin = safeInsetTop + topOffsetPixel;
            }
        }
        return adParams;
    }
    ```
  - Update the old 2-arg method body at line 725 to delegate (replace entire body, keep signature):
    ```java
    protected FrameLayout.LayoutParams getLayoutParams(int positionCode, int topPadding) {
        return getLayoutParams(positionCode, 0, topPadding);
    }
    ```
  - The hardcoded `int leftOffset = 0;` at the old line 737 is now gone (lives in the new overload properly). The old method body (lines 726-759) is replaced entirely with the single delegate call.

  **Must NOT do**:
  - Do not change the method signature of the 2-arg version (line 1533 calls `getLayoutParams(positionCode, 0)` — it must still compile)
  - Do not use the integer literals `-1`, `0`, `2`, `3`, `6` — use the named constants `POSITION_CUSTOM`, `POSITION_TOP_CENTER`, etc.
  - Do not change the WRAP_CONTENT width — this is MREC; banner is handled separately in Task 3

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Mechanical method refactor — new overload + slim delegation in old method
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `playwright`: UI skill, not applicable

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 — sole task
  - **Blocks**: Tasks 3, 4
  - **Blocked By**: Task 1

  **References**:

  **Pattern References**:
  - `AdmobHelper.java:725-760` — Existing `getLayoutParams(int positionCode, int topPadding)` — full body to refactor/replace
  - `AdmobHelper.java:735-736` — Old POSITION_CUSTOM branch with hardcoded `leftOffset = 0` — this is what we're fixing
  - `AdmobHelper.java:762-770` — `convertPixelsToDp()` and `convertDpToPixel()` helpers — use these for unit conversion
  - `AdmobHelper.java:65-72` — Named position constants to use instead of integer literals
  - `AdmobHelper.java:1533` — `getLayoutParams(positionCode, 0)` caller — must keep compiling after change

  **Acceptance Criteria**:

  **QA Scenarios**:

  ```
  Scenario: Old 2-arg caller at line 1533 still compiles
    Tool: Bash
    Preconditions: Task 2 edits applied
    Steps:
      1. Run: ./gradlew :admobadshelper:compileDebugJavaWithJavac
      2. Assert: exit code 0; no errors referencing getLayoutParams or CreateMrecAdView
    Expected Result: Compilation succeeds — old caller at line 1533 resolves to 2-arg overload which delegates
    Failure Indicators: "cannot find symbol" or ambiguous method errors
    Evidence: .sisyphus/evidence/task-2-compile.txt

  Scenario: New 3-arg overload exists in compiled class
    Tool: Bash
    Preconditions: Compilation succeeded
    Steps:
      1. Run: javap -p admobadshelper/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/rofi/admobadshelper/AdmobHelper.class 2>&1 | grep "getLayoutParams"
      2. Assert: output contains both "getLayoutParams(int, int)" and "getLayoutParams(int, int, int)"
    Expected Result: Two overloads visible in compiled bytecode
    Failure Indicators: Only one getLayoutParams signature present
    Evidence: .sisyphus/evidence/task-2-javap-check.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-2-compile.txt`
  - [ ] `.sisyphus/evidence/task-2-javap-check.txt`

  **Commit**: NO — grouped in final single commit

- [ ] 3. Add `getBannerLayoutParams(positionCode, xDp, yDp)` + fix `initBanner()` to use stored offsets

  **What to do**:
  - Add a new protected method `getBannerLayoutParams(int positionCode, int xDp, int yDp)` near the existing `getLayoutParams` methods (insert after the 3-arg `getLayoutParams` overload added in Task 2):
    ```java
    /**
     * Returns FrameLayout params for Banner positioning.
     * Banner always uses MATCH_PARENT width (full-width ad strip).
     * For POSITION_CUSTOM (-1): positions at absolute (xDp, yDp) from top-left.
     * Note: when POSITION_CUSTOM + xDp > 0, the banner is clipped on the right — by design.
     * For other positions: xDp is ignored; yDp acts as extra top/bottom margin offset.
     */
    protected FrameLayout.LayoutParams getBannerLayoutParams(int positionCode, int xDp, int yDp) {
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        Insets insets = getSafeInsets();
        int safeInsetTop = insets.top;
        int safeInsetBottom = insets.bottom;

        adParams.bottomMargin = safeInsetBottom;
        adParams.rightMargin = insets.right;

        if (positionCode == POSITION_CUSTOM) {
            int leftOffset = (int) AdmobHelper.convertDpToPixel(xDp);
            int topOffset = (int) AdmobHelper.convertDpToPixel(yDp);
            if (topOffset < safeInsetTop) topOffset = safeInsetTop;
            adParams.gravity = Gravity.TOP | Gravity.START;
            adParams.leftMargin = leftOffset;
            adParams.topMargin = topOffset;
        } else {
            adParams.leftMargin = insets.left;
            int offsetPixel = (int) AdmobHelper.convertDpToPixel(yDp);
            if (positionCode == POSITION_TOP_CENTER
                    || positionCode == POSITION_TOP_LEFT
                    || positionCode == POSITION_TOP_RIGHT
                    || positionCode == POSITION_CENTER) {
                adParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                adParams.topMargin = safeInsetTop + offsetPixel;
            } else {
                adParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                adParams.bottomMargin = safeInsetBottom + offsetPixel;
            }
        }
        return adParams;
    }
    ```
  - Fix `initBanner()` at lines 361-366: replace the ternary gravity block with a call to `getBannerLayoutParams`:
    - **Old code (lines 361-366)**:
      ```java
      int gravity = bannerPosition == Constants.POSITION_CENTER_TOP ? Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;

      FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);

      layoutParams.setMargins(0, 0, 0, 0);
      bannerView.setLayoutParams(layoutParams);
      ```
    - **New code** (replace the above block entirely):
      ```java
      FrameLayout.LayoutParams layoutParams = getBannerLayoutParams(bannerPosition, bannerXOffset, bannerYOffset);
      bannerView.setLayoutParams(layoutParams);
      ```
  - `Constants.POSITION_CENTER_TOP` == `POSITION_TOP_CENTER` (both = 0). The `getBannerLayoutParams` switch handles all cases including `Constants.POSITION_CENTER_TOP` correctly via the `POSITION_TOP_CENTER` branch.

  **Must NOT do**:
  - Do not use raw integer literals — use named constants
  - Do not change MATCH_PARENT width — banner must stay full-width
  - Do not modify anything outside `initBanner()` and the new `getBannerLayoutParams` method insertion

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: New method + small method body replacement; no logic discovery needed
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 3 — sole task
  - **Blocks**: Task 5
  - **Blocked By**: Task 2 (needs `POSITION_CUSTOM` constant + `getSafeInsets()` pattern reference)

  **References**:

  **Pattern References**:
  - `AdmobHelper.java:361-366` — Banner layout block inside `initBanner()` to REPLACE
  - `AdmobHelper.java:725-760` — 3-arg `getLayoutParams` added in Task 2 — use as pattern for `getBannerLayoutParams` (same structure, different width constant)
  - `AdmobHelper.java:65-72` — Position constants
  - `AdmobHelper.java:762-770` — `convertDpToPixel()` utility
  - `AdmobHelper.java:772-790` — `getSafeInsets()` usage pattern

  **API/Type References**:
  - `AdmobHelper.java:186-200` — `getBannerAdView()` / `setBannerAdView()` — used to retrieve banner view reference
  - `base/src/main/java/com/rofi/base/Constants.java` — `POSITION_CENTER_TOP = 0` equals `POSITION_TOP_CENTER = 0`; no change needed but worth confirming

  **Acceptance Criteria**:

  **QA Scenarios**:

  ```
  Scenario: initBanner() uses getBannerLayoutParams
    Tool: Bash
    Preconditions: Task 3 edits applied
    Steps:
      1. Run: grep -n "getBannerLayoutParams\|ternary.*gravity\|Gravity.CENTER_HORIZONTAL.*TOP.*BOTTOM" admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java | head -20
      2. Assert: "getBannerLayoutParams" appears at/near old line 361; no raw "Gravity.CENTER_HORIZONTAL | Gravity.TOP : Gravity.CENTER_HORIZONTAL" ternary remains
      3. Run: ./gradlew :admobadshelper:compileDebugJavaWithJavac
      4. Assert: exit code 0
    Expected Result: initBanner() calls getBannerLayoutParams; old ternary removed; compiles cleanly
    Failure Indicators: Old ternary still present OR compilation fails
    Evidence: .sisyphus/evidence/task-3-grep.txt + .sisyphus/evidence/task-3-compile.txt

  Scenario: getBannerLayoutParams exists in compiled class
    Tool: Bash
    Preconditions: Compilation succeeded
    Steps:
      1. Run: javap -p admobadshelper/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/rofi/admobadshelper/AdmobHelper.class 2>&1 | grep "getBannerLayoutParams"
      2. Assert: one line containing "getBannerLayoutParams(int, int, int)"
    Expected Result: Method signature visible in compiled bytecode
    Failure Indicators: No getBannerLayoutParams line in javap output
    Evidence: .sisyphus/evidence/task-3-javap.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-3-grep.txt`
  - [ ] `.sisyphus/evidence/task-3-compile.txt`
  - [ ] `.sisyphus/evidence/task-3-javap.txt`

  **Commit**: NO — grouped in final single commit

- [ ] 4. MREC public API — add `setMrecPosition(positionCode, xDp, yDp)` overload; update `updateMrecPosition()` to read `mrecXOffset`

  **What to do**:

  **Step A** — Update `updateMrecPosition(int positionCode, int topPadding)` (line 707):
  - Change the inner call from `getLayoutParams(positionCode, topPadding)` to `getLayoutParams(positionCode, mrecXOffset, topPadding)`
  - This lets the stored `mrecXOffset` field feed into the layout calculation automatically
  - Only change the one line (717); the rest of the method body stays identical

  **Step B** — Add new `setMrecPosition` overload immediately after the existing one (line 825-827):
  ```java
  /**
   * Sets MREC position with custom x/y offsets in dp.
   * When positionCode == POSITION_CUSTOM (-1): positions MREC at absolute (xDp dp, yDp dp) from top-left.
   * For other position codes: xDp is ignored; yDp acts as additional Y padding from the anchored edge.
   * Backward compat: old setMrecPosition(positionCode, offsetY) calls this with xDp=0.
   */
  public void setMrecPosition(int positionCode, int xDp, int yDp) {
      synchronized (this) {
          mrecXOffset = xDp;
      }
      this.updateMrecPosition(positionCode, yDp);
  }
  ```
  - Update the **existing** `setMrecPosition(int positionCode, int offsetY)` at line 825 to delegate to the new overload:
    ```java
    public void setMrecPosition(int positionCode, int offsetY) {
        setMrecPosition(positionCode, 0, offsetY);
    }
    ```
  - This ensures the old 2-arg form compiles and behaves identically (xDp defaults to 0).

  **Must NOT do**:
  - Do not delete the old 2-arg `setMrecPosition` — backward compat is required
  - Do not modify `initMrec()` or `loadMrec()`
  - Do not touch `getLayoutParams` (already updated in Task 2)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Multi-step edit — update existing method body + add overload + update old delegate; requires reading surrounding context carefully
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `playwright`: not applicable

  **Parallelization**:
  - **Can Run In Parallel**: YES — with Task 5
  - **Parallel Group**: Wave 4 (runs alongside Task 5)
  - **Blocks**: F-wave
  - **Blocked By**: Task 2 (3-arg `getLayoutParams` must exist)

  **References**:

  **Pattern References**:
  - `AdmobHelper.java:707-723` — `updateMrecPosition(int positionCode, int topPadding)` — modify line 717 only
  - `AdmobHelper.java:825-827` — Existing `setMrecPosition(int positionCode, int offsetY)` — update body to delegate; add new 3-arg overload immediately after
  - `AdmobHelper.java:146-159` — `volatile` flag fields for pattern — `mrecXOffset` field added in Task 1 follows same pattern

  **Acceptance Criteria**:

  **QA Scenarios**:

  ```
  Scenario: Both setMrecPosition signatures compile
    Tool: Bash
    Preconditions: Task 4 edits applied
    Steps:
      1. Run: ./gradlew :admobadshelper:compileDebugJavaWithJavac
      2. Assert: exit code 0
      3. Run: javap -p admobadshelper/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/rofi/admobadshelper/AdmobHelper.class 2>&1 | grep "setMrecPosition"
      4. Assert: output contains both "setMrecPosition(int, int)" and "setMrecPosition(int, int, int)"
    Expected Result: Both overloads exist in bytecode
    Failure Indicators: Only one setMrecPosition or ambiguous overload error
    Evidence: .sisyphus/evidence/task-4-compile.txt + .sisyphus/evidence/task-4-javap.txt

  Scenario: updateMrecPosition passes mrecXOffset to getLayoutParams
    Tool: Bash
    Preconditions: Task 4 edits applied
    Steps:
      1. Run: grep -n "getLayoutParams" admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java
      2. Assert: The line inside updateMrecPosition contains "getLayoutParams(positionCode, mrecXOffset, topPadding)" — NOT the old 2-arg form "getLayoutParams(positionCode, topPadding)"
    Expected Result: grep shows 3-arg call inside updateMrecPosition
    Failure Indicators: Old 2-arg call still present inside updateMrecPosition body
    Evidence: .sisyphus/evidence/task-4-grep.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-4-compile.txt`
  - [ ] `.sisyphus/evidence/task-4-javap.txt`
  - [ ] `.sisyphus/evidence/task-4-grep.txt`

  **Commit**: NO — grouped in final single commit

- [ ] 5. Banner public API — add `updateBannerPosition()` + `setBannerPosition(positionCode, xDp, yDp)`

  **What to do**:

  **Step A** — Add private `updateBannerPosition(int positionCode, int xDp, int yDp)` method near other banner utility methods (e.g., after `initBanner()` closes at ~line 373, or after `setBannerPosition` if you add that first):
  ```java
  private void updateBannerPosition(int positionCode, int xDp, int yDp) {
      AdView banner = getBannerAdView();
      if (banner == null) return;
      Activity currentActivity = getCurrentActivity();
      if (currentActivity != null) {
          runSafelyOnUiThread(currentActivity, new Runnable() {
              public void run() {
                  AdView bannerView = getBannerAdView();
                  if (bannerView != null) {
                      FrameLayout.LayoutParams lp = getBannerLayoutParams(positionCode, xDp, yDp);
                      bannerView.setLayoutParams(lp);
                      bannerView.requestLayout();
                  }
              }
          });
      }
  }
  ```

  **Step B** — Add public `setBannerPosition(int positionCode, int xDp, int yDp)` immediately after `updateBannerPosition`:
  ```java
  /**
   * Changes the banner ad position at runtime.
   * positionCode: one of POSITION_* constants. Use POSITION_CUSTOM (-1) for absolute (xDp, yDp) placement.
   * xDp: horizontal offset in dp from left edge (only used when positionCode == POSITION_CUSTOM).
   * yDp: vertical offset in dp from top/bottom edge depending on position.
   * Thread-safe: positionCode + offsets written atomically before UI update.
   */
  public void setBannerPosition(int positionCode, int xDp, int yDp) {
      synchronized (this) {
          bannerPosition = positionCode;
          bannerXOffset = xDp;
          bannerYOffset = yDp;
      }
      updateBannerPosition(positionCode, xDp, yDp);
  }
  ```
  - Use a `Runnable` anonymous class for the UI thread callback (not a lambda), to stay consistent with the existing style in `updateMrecPosition` at lines 713-721 which also uses `new Runnable() { public void run() { ... } }`

  **Must NOT do**:
  - Do not modify `initBanner()` — already fixed in Task 3
  - Do not write a lambdas — use `new Runnable() { public void run() {...} }` style consistently
  - Do not touch anything related to MREC — that's Task 4's domain

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Two new methods with threading logic; requires reading existing Runnable/UI-thread patterns to match exactly
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `playwright`: not applicable

  **Parallelization**:
  - **Can Run In Parallel**: YES — with Task 4
  - **Parallel Group**: Wave 4 (runs alongside Task 4; banner code region ~360-640 vs MREC region ~707-827)
  - **Blocks**: F-wave
  - **Blocked By**: Task 3 (`getBannerLayoutParams` must exist before `updateBannerPosition` can call it)

  **References**:

  **Pattern References**:
  - `AdmobHelper.java:707-723` — `updateMrecPosition()` — copy this exact threading pattern (runSafelyOnUiThread + anonymous Runnable + double-check after UI switch) for `updateBannerPosition`
  - `AdmobHelper.java:186-200` — `getBannerAdView()` — use to retrieve view reference in `updateBannerPosition`
  - `AdmobHelper.java:270-285` — `showBanner(Activity activity, ...)` — shows how `bannerPosition` field is stored and `getCurrentActivity()` is used; reference for method placement context
  - `AdmobHelper.java:138-159` — Field declarations area — `bannerXOffset`/`bannerYOffset` fields added in Task 1 are referenced here

  **API/Type References**:
  - `AdmobHelper.java:713` — `runSafelyOnUiThread(Activity, Runnable)` — required to post UI changes safely; always use this, never `runOnUiThread` directly

  **Acceptance Criteria**:

  **QA Scenarios**:

  ```
  Scenario: setBannerPosition compiles and signature is correct
    Tool: Bash
    Preconditions: Task 5 edits applied
    Steps:
      1. Run: ./gradlew :admobadshelper:compileDebugJavaWithJavac
      2. Assert: exit code 0
      3. Run: javap -p admobadshelper/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/rofi/admobadshelper/AdmobHelper.class 2>&1 | grep "setBannerPosition\|updateBannerPosition"
      4. Assert: "setBannerPosition(int, int, int)" visible; "updateBannerPosition" also present (private, may show with -p flag)
    Expected Result: Both methods appear in compiled class
    Failure Indicators: Missing method or compile error
    Evidence: .sisyphus/evidence/task-5-compile.txt + .sisyphus/evidence/task-5-javap.txt

  Scenario: Full module assembles cleanly after all 5 tasks
    Tool: Bash
    Preconditions: All Tasks 1–5 applied (run this scenario after Task 5 as integration smoke test)
    Steps:
      1. Run: ./gradlew :admobadshelper:assembleDebug 2>&1
      2. Assert: exit code 0; "BUILD SUCCESSFUL" in output
      3. Run: ./gradlew :admobadshelper:lint 2>&1 | tail -20
      4. Assert: no NEW lint errors introduced by our changes (pre-existing lint issues are acceptable)
    Expected Result: Debug APK/AAR builds successfully; no new lint issues
    Failure Indicators: BUILD FAILED, or new lint errors mentioning AdmobHelper.java lines we modified
    Evidence: .sisyphus/evidence/task-5-assemble.txt + .sisyphus/evidence/task-5-lint.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-5-compile.txt`
  - [ ] `.sisyphus/evidence/task-5-javap.txt`
  - [ ] `.sisyphus/evidence/task-5-assemble.txt`
  - [ ] `.sisyphus/evidence/task-5-lint.txt`

  **Commit**: YES — after all 5 tasks pass QA
  - Message: `feat(admobadshelper): add custom x/y position support for Banner and MREC`
  - Files: `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java`
  - Pre-commit: `./gradlew :admobadshelper:compileDebugJavaWithJavac`

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE before marking work complete.
> Present consolidated results to user and get explicit "okay" before finishing.
> Rejection or user feedback → fix → re-run → present again → wait for okay.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read this plan end-to-end. For each "Must Have": verify implementation exists (read file, check method signature). For each "Must NOT Have": search `AdmobHelper.java` for forbidden patterns — reject with file:line if found. Verify evidence files exist in `.sisyphus/evidence/`. Compare actual deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `./gradlew :admobadshelper:compileDebugJavaWithJavac` and `./gradlew :admobadshelper:lint`. Review all changed sections of `AdmobHelper.java` for: missing `@Nullable`/`@NonNull` annotations on new public methods, raw `synchronized` on public methods (prefer field-level), improper `volatile` usage, commented-out dead code left behind, missing Javadoc on new public API methods.
  Output: `Compile [PASS/FAIL] | Lint [PASS/FAIL] | Issues found | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high`
  Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence to `.sisyphus/evidence/final-qa/`. Verify: (a) old `setMrecPosition(0, 0)` still compiles, (b) new 3-arg methods exist in compiled class, (c) `initBanner()` path uses `getBannerLayoutParams` not raw ternary.
  Output: `Scenarios [N/N pass] | Backward Compat [PASS/FAIL] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (`git diff HEAD`). Verify 1:1 — everything in spec was built, nothing beyond spec. Check "Must NOT do" compliance per task. Flag any unaccounted changes in `AdmobHelper.java` beyond the 5 tasks. Verify no other files were modified.
  Output: `Tasks [N/N compliant] | Unaccounted changes [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **Single commit after all tasks complete**:
  - Message: `feat(admobadshelper): add custom x/y position support for Banner and MREC`
  - Files: `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java`
  - Pre-commit: `./gradlew :admobadshelper:compileDebugJavaWithJavac`

---

## Success Criteria

### Verification Commands
```bash
# Must all exit 0
./gradlew :admobadshelper:compileDebugJavaWithJavac
./gradlew :admobadshelper:assembleDebug
./gradlew :admobadshelper:lint
```

### Final Checklist
- [ ] All "Must Have" public methods exist and compile
- [ ] All "Must NOT Have" guardrails respected
- [ ] `setMrecPosition(int, int)` backward compat preserved (line 825 untouched)
- [ ] `getLayoutParams(int, int)` backward compat preserved (line 1533 still compiles)
- [ ] All evidence files exist in `.sisyphus/evidence/`

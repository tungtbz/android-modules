# Add onAdFailedToShowFullScreenContent Callback to AdmobHelper

## TL;DR

> **Quick Summary**: Add a new `onAdFailedToShowFullScreenContent(int type, String errorMessage)` callback to `IAdmobAdListener` and wire it into the three `FullScreenContentCallback.onAdFailedToShowFullScreenContent` overrides inside `AdmobHelper` (Interstitial, Rewarded, AppOpenAd).
>
> **Deliverables**:
> - `IAdmobAdListener.java`: 1 new `default` method added
> - `AdmobHelper.java`: 3 callback call-sites inserted
>
> **Estimated Effort**: Quick (< 15 min)
> **Parallel Execution**: NO — single task, sequential
> **Critical Path**: Task 1 only

---

## Context

### Original Request
"thêm callback khi display failCalled" — add callback when ad fails to display (onAdFailedToShowFullScreenContent).

### Interview Summary
**Key Discussions**:
- File already read and analyzed: `AdmobHelper.java` + `IAdmobAdListener.java`
- All three `onAdFailedToShowFullScreenContent` overrides (Interstitial, Rewarded, AOA) currently log + reload with **no callback fired**
- Existing pattern uses `int type`: 0 = AOA, 1 = Interstitial, 2 = Rewarded

**Research Findings**:
- `IAdmobAdListener` is a plain Java interface with 17 abstract methods, **zero default methods**
- The module is a **published AAR** — adding an abstract method breaks all existing consumers at compile time
- Interstitial/Rewarded use safe local-capture null-check pattern; AOA's existing callbacks do NOT (pre-existing bug, out of scope)

### Metis Review
**Identified Gaps** (addressed):
- **Breaking interface change**: Resolved by using `default void` instead of `abstract void` — backward compatible, consumers don't need to update
- **Callback insertion order**: Must be `clear ref` → **callback** → `reload` — matches `onAdDismissedFullScreenContent` pattern
- **AOA null-check inconsistency**: New AOA call-site must use safe local-capture pattern, NOT the unsafe bare `adsEventCallback.` style used by surrounding AOA code
- **errorMessage nullability**: `AdError.getMessage()` is `@NonNull` in AdMob SDK — safe to pass directly

---

## Work Objectives

### Core Objective
Surface display failures to callers via `IAdmobAdListener`, so consumers can react (e.g., show fallback UI, log analytics) when a full-screen ad's `show()` call fails after the ad was already loaded.

### Concrete Deliverables
- `admobadshelper/src/main/java/com/rofi/admobadshelper/IAdmobAdListener.java` — 1 new `default` method
- `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java` — 3 callback insertions

### Definition of Done
- [ ] `./gradlew :admobadshelper:assembleDebug` passes with `BUILD SUCCESSFUL`
- [ ] `IAdmobAdListener` has exactly 1 new method for `onAdFailedToShowFullScreenContent`
- [ ] `AdmobHelper` has exactly 3 call-sites for `onAdFailedToShowFullScreenContent`

### Must Have
- `default void onAdFailedToShowFullScreenContent(int type, String errorMessage) {}` in `IAdmobAdListener` (backward-compatible, no-op default)
- All 3 call-sites use `IAdmobAdListener callback = adsEventCallback; if (callback != null) { ... }` pattern
- Callback fires **after** clearing the ad reference and **before** the reload call

### Must NOT Have (Guardrails)
- **MUST NOT** fix AOA's pre-existing null-unsafe callback calls on lines 1856 and 1886 (out of scope)
- **MUST NOT** reset `aoaBlocker` in AOA failure path (pre-existing behavior preserved)
- **MUST NOT** add `currentInterstitialCode` / `currentRewardCode` to the failure callback signature
- **MUST NOT** change log levels (keep `Log.d` for AOA failure, `Log.w` for Interstitial/Rewarded)
- **MUST NOT** touch Banner, MREC, `onAdFailedToLoad`, consent, or any unrelated methods
- **MUST NOT** use `abstract void` — must be `default void` to avoid breaking the published AAR API

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: NO (0% test coverage per AGENTS.md)
- **Automated tests**: None
- **Agent-Executed QA**: YES — compilation verification + grep pattern checks

### QA Policy
Build verification + grep checks executed by the agent after implementation.

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (single task — sequential):
└── Task 1: Add interface method + 3 call-sites [quick]

Wave FINAL:
└── F1: Compilation + grep verification [quick]
```

---

## TODOs

- [ ] 1. Add `onAdFailedToShowFullScreenContent` to `IAdmobAdListener` and wire 3 call-sites in `AdmobHelper`

  **What to do**:

  **Step A — `IAdmobAdListener.java`**

  After the existing `onAdDismissedFullScreenContent(int type);` method (line 8), add:
  ```java
  /**
   * Called when a full-screen ad fails to display after show() is called.
   * This is distinct from load failure (onAdFailedToLoad).
   * type: 0 = App Open Ad, 1 = Interstitial, 2 = Rewarded
   *
   * @param type         Ad type identifier (0=AOA, 1=Interstitial, 2=Rewarded)
   * @param errorMessage Error description from AdError
   */
  default void onAdFailedToShowFullScreenContent(int type, String errorMessage) {}
  ```

  **Step B — `AdmobHelper.java`, Interstitial** (~line 1216–1223)

  Current block:
  ```java
  @Override
  public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
      Log.w(TAG, "Interstitial failed to show: " + adError.getMessage());
      // Clear reference in synchronized block
      setInterstitialAd(null); // This also sets interstitialAdLoaded = false
      
      // Try to load again
      loadInterstitial();
  }
  ```
  Replace with:
  ```java
  @Override
  public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
      Log.w(TAG, "Interstitial failed to show: " + adError.getMessage());
      // Clear reference in synchronized block
      setInterstitialAd(null); // This also sets interstitialAdLoaded = false
      
      IAdmobAdListener callback = adsEventCallback;
      if (callback != null) {
          callback.onAdFailedToShowFullScreenContent(1, adError.getMessage());
      }
      
      // Try to load again
      loadInterstitial();
  }
  ```

  **Step C — `AdmobHelper.java`, Rewarded** (~line 1530–1537)

  Current block:
  ```java
  @Override
  public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
      Log.w(TAG, "Rewarded failed to show: " + adError.getMessage());
      // Clear reference in synchronized block
      setRewardedAd(null); // This also sets rewardedAdLoaded = false
      
      // Try to load again
      loadRewarded();
  }
  ```
  Replace with:
  ```java
  @Override
  public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
      Log.w(TAG, "Rewarded failed to show: " + adError.getMessage());
      // Clear reference in synchronized block
      setRewardedAd(null); // This also sets rewardedAdLoaded = false
      
      IAdmobAdListener callback = adsEventCallback;
      if (callback != null) {
          callback.onAdFailedToShowFullScreenContent(2, adError.getMessage());
      }
      
      // Try to load again
      loadRewarded();
  }
  ```

  **Step D — `AdmobHelper.java`, AppOpenAd** (~line 1866–1873)

  Current block:
  ```java
  @Override
  public void onAdFailedToShowFullScreenContent(AdError adError) {
      // Called when fullscreen content failed to show.
      // Set the reference to null so isAdAvailable() returns false.
      Log.d(TAG, adError.getMessage());
      _appOpenAd = null;
      _isShowingAd = false;
      loadAd(getCurrentActivity());
  }
  ```
  Replace with:
  ```java
  @Override
  public void onAdFailedToShowFullScreenContent(AdError adError) {
      // Called when fullscreen content failed to show.
      // Set the reference to null so isAdAvailable() returns false.
      Log.d(TAG, adError.getMessage());
      _appOpenAd = null;
      _isShowingAd = false;
      
      IAdmobAdListener callback = adsEventCallback;
      if (callback != null) {
          callback.onAdFailedToShowFullScreenContent(0, adError.getMessage());
      }
      
      loadAd(getCurrentActivity());
  }
  ```

  **Must NOT do**:
  - Do NOT change `Log.d` to `Log.w` in the AOA block
  - Do NOT add `aoaBlocker = false` or modify any other AOA state
  - Do NOT touch lines 1856 or 1886 (pre-existing AOA null-check bug — separate PR)
  - Do NOT alter Banner, MREC, `onAdFailedToLoad`, consent code
  - Do NOT use `abstract void` in the interface (must be `default void`)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 2 files, 4 small edits, zero new logic — pure insertion
  - **Skills**: []
  - **Skills Evaluated but Omitted**:
    - `playwright`: No UI involved
    - `git-master`: No commit requested

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (sole task)
  - **Blocks**: Final verification
  - **Blocked By**: None (can start immediately)

  **References**:

  **Pattern References** (existing code to follow):
  - `AdmobHelper.java:1196–1213` — Interstitial `onAdDismissedFullScreenContent`: shows the `IAdmobAdListener callback = adsEventCallback; if (callback != null)` null-check pattern and the `clear → callback → reload` order
  - `AdmobHelper.java:1515–1527` — Rewarded `onAdDismissedFullScreenContent`: same pattern, use as template
  - `IAdmobAdListener.java:7–8` — Existing `onAdDisplayFullScreenContent` / `onAdDismissedFullScreenContent` declarations: match Javadoc style

  **API/Type References**:
  - `IAdmobAdListener.java` — interface to modify; add new `default void` method after line 8
  - `com.google.android.gms.ads.AdError` — `getMessage()` returns `@NonNull String`

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Interface has the new default method
    Tool: Bash (grep)
    Steps:
      1. grep -n "onAdFailedToShowFullScreenContent" admobadshelper/src/main/java/com/rofi/admobadshelper/IAdmobAdListener.java
    Expected Result: Exactly 1 match; the line contains "default void onAdFailedToShowFullScreenContent"
    Failure Indicators: 0 matches (method not added), or "abstract void" (wrong modifier)
    Evidence: .sisyphus/evidence/task-1-interface-method.txt

  Scenario: All 3 call-sites exist in AdmobHelper
    Tool: Bash (grep)
    Steps:
      1. grep -n "onAdFailedToShowFullScreenContent" admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java
    Expected Result: Exactly 3 matches; each inside a FullScreenContentCallback override (type values: 0, 1, 2)
    Failure Indicators: Fewer than 3 matches, or any match shows bare "adsEventCallback." without null-check wrapper
    Evidence: .sisyphus/evidence/task-1-callsites.txt

  Scenario: Null-check pattern is correct at each call-site
    Tool: Bash (grep)
    Steps:
      1. grep -B2 "onAdFailedToShowFullScreenContent" admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java
    Expected Result: Each of the 3 call-sites is preceded by "IAdmobAdListener callback = adsEventCallback;" — NOT bare "adsEventCallback."
    Failure Indicators: Any call-site missing the local capture line
    Evidence: .sisyphus/evidence/task-1-null-check.txt

  Scenario: Module compiles successfully
    Tool: Bash (./gradlew)
    Steps:
      1. ./gradlew :admobadshelper:assembleDebug
    Expected Result: BUILD SUCCESSFUL, 0 errors
    Failure Indicators: Any compilation error
    Evidence: .sisyphus/evidence/task-1-build.txt
  ```

  **Evidence to Capture**:
  - [ ] task-1-interface-method.txt — grep output for interface
  - [ ] task-1-callsites.txt — grep output for AdmobHelper call-sites
  - [ ] task-1-null-check.txt — grep context output
  - [ ] task-1-build.txt — build output tail

  **Commit**: NO

---

## Final Verification Wave

- [ ] F1. **Compilation + Pattern Audit** — `quick`
  Run `./gradlew :admobadshelper:assembleDebug`. Verify BUILD SUCCESSFUL. Run all 4 grep QA scenarios above. Confirm 1 new `default void` method in interface, 3 null-checked call-sites in AdmobHelper with correct type values (0, 1, 2), reload calls still present after each insertion.
  Output: `Build [PASS/FAIL] | Interface [1 method] | CallSites [3/3] | NullCheck [3/3] | VERDICT: APPROVE/REJECT`

---

## Commit Strategy
- No commit requested.

---

## Success Criteria

### Verification Commands
```bash
# Verify interface
grep -n "onAdFailedToShowFullScreenContent" admobadshelper/src/main/java/com/rofi/admobadshelper/IAdmobAdListener.java
# Expected: 1 line containing "default void"

# Verify call-sites
grep -n "onAdFailedToShowFullScreenContent" admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java
# Expected: 3 lines

# Build
./gradlew :admobadshelper:assembleDebug
# Expected: BUILD SUCCESSFUL
```

### Final Checklist
- [ ] `default void onAdFailedToShowFullScreenContent(int type, String errorMessage) {}` in `IAdmobAdListener`
- [ ] 3 call-sites in `AdmobHelper` with types 0, 1, 2
- [ ] Each call-site uses local-capture null-check pattern
- [ ] Order at each site: clear ref → callback → reload
- [ ] `assembleDebug` passes

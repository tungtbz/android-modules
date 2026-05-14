# Add getBannerHeightInPixels / getMrecHeightInPixels

## TL;DR

> **Quick Summary**: Thêm 4 public method trả về chiều cao pixel của banner và MREC vào `AdmobHelper` và `MaxAdsService`. Pure addition — không sửa bất kỳ code hiện có.
>
> **Deliverables**:
> - `AdmobHelper.getBannerHeightInPixels()` — trả về chiều cao adaptive banner tính bằng pixel
> - `AdmobHelper.getMrecHeightInPixels()` — trả về 250dp → pixel
> - `MaxAdsService.getBannerHeightInPixels()` — trả về chiều cao adaptive banner tính bằng pixel
> - `MaxAdsService.getMrecHeightInPixels()` — trả về 250dp → pixel
>
> **Estimated Effort**: Quick
> **Parallel Execution**: YES — 2 waves
> **Critical Path**: Task 1 → Task 2 → FINAL

---

## Context

### Original Request
Thêm hàm trả về chiều cao tính bằng pixel cho banner và mrec vào `AdmobHelper.java` và `MaxAdsService.java`.

### Interview Summary
**Key Discussions**:
- **Return type**: `int` (pixel), return `0` khi activity không sẵn sàng
- **Activity param**: Không cần — dùng `getCurrentActivity()` bên trong
- **IAdsService**: Không cập nhật interface — chỉ thêm vào class cụ thể

**Research Findings**:
- `AdmobHelper` đã có `getBannerAdSize(Activity)` private và `convertDpToPixel(float)` static để tái sử dụng
- `MaxAdsService` đã tính `heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight()` tại `LoadNormalBanner()` line 1117
- MREC cố định 250dp trong cả hai files

### Metis Review
**Identified Gaps** (addressed):
- **IAdsService not updated**: Confirmed by user — concrete classes only
- **SDK init guard in MaxAdsService**: Thêm kiểm tra `sdk != null` trước `getAdaptiveSize()`
- **try/catch in AdmobHelper**: Bọc `getBannerAdSize()` call để tránh NPE trên API cũ
- **Naming convention**: camelCase (theo `isBannerLoaded`, `isMrecLoaded`)
- **Scope creep locked**: Không thêm width methods, không sửa `getSafeInsets()`, không thêm constants

---

## Work Objectives

### Core Objective
Thêm 4 public getter methods trả về chiều cao pixel của banner và MREC, không thay đổi logic hiện có.

### Concrete Deliverables
- `AdmobHelper.java`: 2 new public methods added after `isMrecLoaded()` (line 987)
- `MaxAdsService.java`: 2 new public methods added after `IsInterReady()` (line 1307)

### Definition of Done
- [ ] `./gradlew :admobadshelper:assembleDebug` → `BUILD SUCCESSFUL`
- [ ] `./gradlew :maxads:assembleDebug` → `BUILD SUCCESSFUL`
- [ ] LSP diagnostics on both files: 0 new errors/warnings

### Must Have
- `public int getBannerHeightInPixels()` và `public int getMrecHeightInPixels()` trong cả 2 classes
- Cả 4 methods return `0` (không throw) khi activity null/finishing/destroyed
- Javadoc trên mỗi method

### Must NOT Have (Guardrails)
- **KHÔNG** thêm methods vào `IAdsService` interface
- **KHÔNG** sửa bất kỳ method, field, constant hiện có
- **KHÔNG** thêm `getBannerWidthInPixels()`, overloads, hay constants mới ngoài 4 methods trên
- **KHÔNG** "fix" bug `getSafeInsets()` NPE hay các lỗi khác phát hiện trong quá trình
- **KHÔNG** refactor `LoadNormalBanner()` để dùng method mới
- **KHÔNG** sửa access modifier của `getBannerAdSize()`

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: NO (0% test coverage)
- **Automated tests**: None — build + lint + LSP là tiêu chuẩn của project này
- **Framework**: N/A

### QA Policy
Build verification + LSP diagnostics + runtime behavior check qua Bash.

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — 2 tasks parallel):
├── Task 1: Add 2 methods to AdmobHelper.java [quick]
└── Task 2: Add 2 methods to MaxAdsService.java [quick]

Wave FINAL (After Task 1 + Task 2 — 2 parallel builds):
├── Task F1: Build :admobadshelper + LSP check [quick]
└── Task F2: Build :maxads + LSP check [quick]

Critical Path: (Task 1 ∥ Task 2) → (F1 ∥ F2)
Parallel Speedup: ~50%
Max Concurrent: 2
```

### Agent Dispatch Summary
- **Wave 1**: 2 tasks → both `quick`
- **FINAL**: 2 tasks → both `quick`

---

## TODOs

- [x] 1. Thêm `getBannerHeightInPixels()` và `getMrecHeightInPixels()` vào `AdmobHelper`

  **What to do**:
  - Mở file `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java`
  - Chèn 2 methods PUBLIC mới **sau** method `isMrecLoaded()` (kết thúc ở khoảng line 987)
  - **Method 1** — `getBannerHeightInPixels()`:
    ```java
    /**
     * Returns the height of the adaptive banner ad in pixels for the current screen orientation.
     * Uses the same size calculation as {@link #getBannerAdSize(Activity)}.
     *
     * @return Banner height in pixels, or 0 if no valid activity is available.
     */
    public int getBannerHeightInPixels() {
        Activity activity = getCurrentActivity();
        if (activity == null) return 0;
        try {
            AdSize adSize = getBannerAdSize(activity);
            return (int) convertDpToPixel(adSize.getHeight());
        } catch (Exception e) {
            Log.e(TAG, "getBannerHeightInPixels: failed to compute height", e);
            return 0;
        }
    }
    ```
  - **Method 2** — `getMrecHeightInPixels()`:
    ```java
    /**
     * Returns the height of the MREC (Medium Rectangle) ad in pixels.
     * MREC is always 250dp per AdMob specification (AdSize.MEDIUM_RECTANGLE).
     *
     * @return MREC height in pixels, or 0 if density information is unavailable.
     */
    public int getMrecHeightInPixels() {
        return (int) convertDpToPixel(250f);
    }
    ```
  - `getMrecHeightInPixels()` không cần activity vì `convertDpToPixel()` dùng `Resources.getSystem()`

  **Must NOT do**:
  - Không sửa `getBannerAdSize()` hay `convertDpToPixel()`
  - Không thay đổi access modifier của bất kỳ method hiện có
  - Không thêm constants hay overloads

  **Recommended Agent Profile**:
  > Single-file Java addition, no logic, pure code insertion.
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (với Task 2)
  - **Blocks**: F1
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java:718-730` — `getBannerAdSize(Activity)` private method (được gọi trong method mới)
  - `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java:838-841` — `convertDpToPixel(float)` static method (được gọi trong cả 2 methods mới)
  - `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java:977-987` — `isBannerLoaded()` và `isMrecLoaded()` — template Javadoc + placement reference

  **Acceptance Criteria**:

  - [ ] `getBannerHeightInPixels()` tồn tại với signature `public int getBannerHeightInPixels()`
  - [ ] `getMrecHeightInPixels()` tồn tại với signature `public int getMrecHeightInPixels()`
  - [ ] Cả 2 methods có Javadoc
  - [ ] Không có method hiện có nào bị sửa

  **QA Scenarios**:

  ```
  Scenario: Build succeeds after addition
    Tool: Bash
    Preconditions: Files modified correctly
    Steps:
      1. Run: ./gradlew :admobadshelper:assembleDebug
      2. Assert exit code = 0
      3. Assert output contains "BUILD SUCCESSFUL"
    Expected Result: BUILD SUCCESSFUL, 0 errors
    Evidence: .sisyphus/evidence/task-1-admob-build.txt

  Scenario: LSP shows no new errors
    Tool: lsp_diagnostics on admobadshelper/.../AdmobHelper.java, severity=error
    Steps:
      1. Run LSP diagnostics on the modified file
      2. Count errors with severity "error"
    Expected Result: 0 new errors introduced by the new methods
    Evidence: .sisyphus/evidence/task-1-admob-lsp.txt
  ```

  **Commit**: YES (groups với Task 2)
  - Message: `feat(admobadshelper): add getBannerHeightInPixels and getMrecHeightInPixels`
  - Files: `admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java`

---

- [x] 2. Thêm `getBannerHeightInPixels()` và `getMrecHeightInPixels()` vào `MaxAdsService`

  **What to do**:
  - Mở file `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`
  - Chèn 2 methods PUBLIC mới **sau** method `IsInterReady()` (kết thúc ở khoảng line 1307)
  - **Method 1** — `getBannerHeightInPixels()`:
    ```java
    /**
     * Returns the adaptive banner height in pixels for the current screen orientation.
     * Uses the same calculation as {@link #LoadNormalBanner(Activity, int)}.
     * Returns 0 if the activity is unavailable or the AppLovin SDK is not yet initialized.
     *
     * @return Banner height in pixels, or 0 if unavailable.
     */
    public int getBannerHeightInPixels() {
        Activity activity = getCurrentActivity();
        if (activity == null) return 0;
        if (sdk == null) return 0;
        try {
            int heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight();
            return AppLovinSdkUtils.dpToPx(activity, heightDp);
        } catch (Exception e) {
            Log.e(TAG, "getBannerHeightInPixels: failed to compute height", e);
            return 0;
        }
    }
    ```
  - **Method 2** — `getMrecHeightInPixels()`:
    ```java
    /**
     * Returns the MREC (Medium Rectangle) height in pixels.
     * MREC is always 250dp per AppLovin MAX specification.
     * Returns 0 if the activity is unavailable.
     *
     * @return MREC height in pixels, or 0 if unavailable.
     */
    public int getMrecHeightInPixels() {
        Activity activity = getCurrentActivity();
        if (activity == null) return 0;
        int heightDp = MaxAdFormat.MREC.getSize().getHeight();
        return AppLovinSdkUtils.dpToPx(activity, heightDp);
    }
    ```
  - Lưu ý: `sdk` field đã là `volatile` — kiểm tra `sdk == null` là thread-safe

  **Must NOT do**:
  - Không thêm method vào `IAdsService` interface
  - Không sửa `LoadNormalBanner()` để gọi method mới
  - Không sửa `LoadMREC()` để gọi method mới

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (với Task 1)
  - **Blocks**: F2
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java:1117-1118` — `heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight()` + `AppLovinSdkUtils.dpToPx()` — pattern cần tái sử dụng
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java:1048-1050` — `AppLovinSdkUtils.dpToPx(activity.getApplicationContext(), 250)` — MREC height pattern
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java:1281-1307` — `IsRewardReady()` và `IsInterReady()` — template placement reference
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java:140` — `private volatile AppLovinSdk sdk` — field cần null-check

  **Acceptance Criteria**:

  - [ ] `getBannerHeightInPixels()` tồn tại với signature `public int getBannerHeightInPixels()`
  - [ ] `getMrecHeightInPixels()` tồn tại với signature `public int getMrecHeightInPixels()`
  - [ ] `getBannerHeightInPixels()` có kiểm tra `sdk == null` guard
  - [ ] Cả 2 methods có Javadoc
  - [ ] `IAdsService` không được sửa đổi

  **QA Scenarios**:

  ```
  Scenario: Build succeeds after addition
    Tool: Bash
    Preconditions: Files modified correctly
    Steps:
      1. Run: ./gradlew :maxads:assembleDebug
      2. Assert exit code = 0
      3. Assert output contains "BUILD SUCCESSFUL"
    Expected Result: BUILD SUCCESSFUL, 0 errors
    Evidence: .sisyphus/evidence/task-2-maxads-build.txt

  Scenario: LSP shows no new errors
    Tool: lsp_diagnostics on maxads/.../MaxAdsService.java, severity=error
    Steps:
      1. Run LSP diagnostics on the modified file
      2. Count errors with severity "error"
    Expected Result: 0 new errors introduced by the new methods
    Evidence: .sisyphus/evidence/task-2-maxads-lsp.txt

  Scenario: IAdsService unchanged
    Tool: Bash (grep)
    Steps:
      1. Run: grep -n "getBannerHeightInPixels\|getMrecHeightInPixels" ads/src/main/java/com/rofi/ads/IAdsService.java
      2. Assert: no matches found
    Expected Result: IAdsService does not contain the new method names
    Evidence: .sisyphus/evidence/task-2-interface-check.txt
  ```

  **Commit**: YES (groups với Task 1)
  - Message: `feat(maxads): add getBannerHeightInPixels and getMrecHeightInPixels`
  - Files: `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`

---

## Final Verification Wave

- [x] F1. **Build + LSP: admobadshelper** — `quick`
  Run `./gradlew :admobadshelper:assembleDebug`. Verify BUILD SUCCESSFUL. Run `lsp_diagnostics` on `AdmobHelper.java`. Verify 0 new errors.
  Output: `Build [PASS/FAIL] | LSP [N errors] | VERDICT: APPROVE/REJECT`

- [x] F2. **Build + LSP: maxads** — `quick`
  Run `./gradlew :maxads:assembleDebug`. Verify BUILD SUCCESSFUL. Run `lsp_diagnostics` on `MaxAdsService.java`. Verify 0 new errors. Grep `IAdsService.java` to confirm interface unchanged.
  Output: `Build [PASS/FAIL] | LSP [N errors] | Interface [UNCHANGED/MODIFIED] | VERDICT: APPROVE/REJECT`

---

## Commit Strategy

- **Wave 1**: `feat(admobadshelper): add getBannerHeightInPixels and getMrecHeightInPixels` + `feat(maxads): add getBannerHeightInPixels and getMrecHeightInPixels`

---

## Success Criteria

### Verification Commands
```bash
./gradlew :admobadshelper:assembleDebug  # Expected: BUILD SUCCESSFUL
./gradlew :maxads:assembleDebug          # Expected: BUILD SUCCESSFUL
grep -n "getBannerHeightInPixels\|getMrecHeightInPixels" admobadshelper/src/main/java/com/rofi/admobadshelper/AdmobHelper.java  # Expected: 2 matches
grep -n "getBannerHeightInPixels\|getMrecHeightInPixels" maxads/src/main/java/com/rofi/maxads/MaxAdsService.java  # Expected: 2 matches
grep -rn "getBannerHeightInPixels\|getMrecHeightInPixels" ads/src/main/java/  # Expected: 0 matches
```

### Final Checklist
- [ ] `AdmobHelper.getBannerHeightInPixels()` added (uses `getBannerAdSize` + `convertDpToPixel`)
- [ ] `AdmobHelper.getMrecHeightInPixels()` added (uses `convertDpToPixel(250f)`)
- [ ] `MaxAdsService.getBannerHeightInPixels()` added (uses `getAdaptiveSize` + `dpToPx`, sdk null-guard)
- [ ] `MaxAdsService.getMrecHeightInPixels()` added (uses `MaxAdFormat.MREC.getSize().getHeight()` + `dpToPx`)
- [ ] `IAdsService` unchanged
- [ ] Both modules build successfully
- [ ] No existing method modified

# Fix: applyAdViewPosition — offset unit px (not dp)

## TL;DR

> **Quick Summary**: `applyAdViewPosition` sai khi gọi `dpToPx()` trên offset đã là px.
> Unity/C# dùng `RectTransformUtility.WorldToScreenPoint()` → truyền raw screen pixels,
> nhưng Java lại convert thêm một lần nữa → offset bị nhân với device density (~2×–3×).
>
> **Deliverables**:
> - `MaxAdsService.java` sửa 2 dòng logic + cập nhật Javadoc/log trong 1 method + 2 public methods
>
> **Estimated Effort**: Quick (< 15 phút)
> **Parallel Execution**: NO — single file, sequential
> **Critical Path**: Task 1 → Build verify

---

## Context

### Original Request
`applyAdViewPosition` đơn vị offset truyền vào là px chứ không phải dp.

### Interview Summary

**Key Discussions**:
- Unity/C# caller dùng `RectTransformUtility.WorldToScreenPoint()` → `Mathf.RoundToInt(screenBL.x)` → raw screen px
- Javadoc hiện tại sai khi viết "in dp" cho cả `applyAdViewPosition`, `SetBannerPosition`, `SetMRECPosition`
- `dpToPx()` ở lines 545–546 là conversion thừa, gây double-scale

**Research Findings**:
- Confirmed: tất cả 4 call site (`SetBannerPosition:1452`, `SetMRECPosition:1544`, `LoadNormalBanner:1217`, `LoadMREC:1059`) đều truyền qua fields `_bannerCustomOffsetX/Y` và `_mrecCustomOffsetX/Y` — cùng một đơn vị px
- 15 lần `dpToPx()` khác trong file là hợp lệ (converting dp constants từ SDK) — KHÔNG được đụng đến

### Metis Review

**Identified Gaps** (addressed):
- Unit ambiguity tại public API boundary → Resolved bằng Unity caller code snippet xác nhận px
- Javadoc description ở line 481 (`"dp offsets"`) cũng cần sửa — đã đưa vào scope
- Tất cả `dpToPx()` calls khác phải giữ nguyên → thêm vào Must NOT Have

---

## Work Objectives

### Core Objective
Xóa double-conversion sai cho offset trong `applyAdViewPosition`. Kết quả: banner/MREC dịch chuyển đúng offset px thay vì `offset × density` px.

### Concrete Deliverables
- `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java` — sửa đúng 2 dòng logic + Javadoc + log

### Definition of Done
- [ ] `./gradlew :maxads:assembleDebug` → BUILD SUCCESSFUL
- [ ] `./gradlew :maxads:testDebugUnitTest` → BUILD SUCCESSFUL (no regressions)
- [ ] Visual smoke test trên thiết bị xxhdpi (density 3): gọi `SetBannerPosition("bottom_center", 60, 60)` → banner dịch lên đúng 60px từ bottom edge, KHÔNG phải 180px

### Must Have
- Xóa `dpToPx()` conversion cho `offsetXPx`/`offsetYPx` (lines 545–546)
- Đổi tên param trong signature: `offsetXDp`/`offsetYDp` → `offsetXPx`/`offsetYPx`
- Cập nhật Javadoc `@description` tại line 481: `"dp offsets"` → `"px offsets"`
- Cập nhật `@param offsetXDp`/`@param offsetYDp` trong `applyAdViewPosition` (lines 486–487)
- Cập nhật `@param offsetX`/`@param offsetY` trong `SetBannerPosition` (lines 1437–1438): `"in dp"` → `"in px"`
- Cập nhật `@param offsetX`/`@param offsetY` trong `SetMRECPosition` (lines 1529–1530): `"in dp"` → `"in px"`
- Cập nhật log statement (lines 591–593): `"dp"` → `"px"` và dùng đúng tên param mới

### Must NOT Have (Guardrails)
- **TUYỆT ĐỐI KHÔNG** đụng đến 15 lần `dpToPx()` khác trong file (lines 437, 503, 506, 507, 1048, 1049, 1118, 1207, 1332, 1350, 1617, 1618, 1683, 1684) — tất cả đang convert dp constants hợp lệ từ AppLovin SDK
- **KHÔNG** đổi tên fields `_bannerCustomOffsetX/Y`, `_mrecCustomOffsetX/Y` — scope riêng biệt
- **KHÔNG** sửa `IAdsService` interface
- **KHÔNG** sửa `AdsManager`
- **KHÔNG** sửa bất kỳ file nào khác ngoài `MaxAdsService.java`
- **KHÔNG** thêm/xóa method, thay đổi behavior của các ad type khác

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification là agent-executed.

### Test Decision
- **Infrastructure exists**: NO (0% test coverage — chỉ có scaffold `ExampleInstrumentedTest`)
- **Automated tests**: None — không thể test visual positioning qua unit test
- **Framework**: N/A

### QA Policy
Build verification + visual smoke test trên thiết bị thực.

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (single task — no parallelism needed for trivial fix):
└── Task 1: Sửa MaxAdsService.java [quick]

Wave FINAL:
└── Task F1: Build + test verification [quick]
```

### Agent Dispatch Summary
- **Wave 1**: 1 task → `quick`
- **FINAL**: 1 task → `quick`

---

## TODOs

- [ ] 1. Sửa `applyAdViewPosition` — đổi params, xóa double-convert, cập nhật Javadoc + log

  **What to do**:

  **A. Đổi method signature** (line 491):
  ```java
  // TỪ:
  private void applyAdViewPosition(MaxAdView adView, String position,
                                   int offsetXDp, int offsetYDp, boolean isBanner) {
  // THÀNH:
  private void applyAdViewPosition(MaxAdView adView, String position,
                                   int offsetXPx, int offsetYPx, boolean isBanner) {
  ```

  **B. Xóa double-convert** (lines 545–546):
  ```java
  // XÓA 2 DÒNG NÀY:
  int offsetXPx = AppLovinSdkUtils.dpToPx(activity, offsetXDp);
  int offsetYPx = AppLovinSdkUtils.dpToPx(activity, offsetYDp);
  // Biến offsetXPx và offsetYPx bây giờ là tên param trực tiếp — không cần khai báo thêm
  ```

  **C. Cập nhật Javadoc của `applyAdViewPosition`** (lines 481–489):
  ```java
  // TỪ:
  /**
   * Applies a gravity-based position + dp offsets to a MaxAdView...
   * @param offsetXDp Horizontal offset in dp (applied to the edge matching alignment)
   * @param offsetYDp Vertical offset in dp (applied to the vertical edge)
   */
  // THÀNH:
  /**
   * Applies a gravity-based position + px offsets to a MaxAdView...
   * @param offsetXPx Horizontal offset in pixels (applied to the edge matching alignment)
   * @param offsetYPx Vertical offset in pixels (applied to the vertical edge)
   */
  ```

  **D. Cập nhật Log.d statement** (lines 591–593):
  ```java
  // TỪ:
  Log.d(TAG, "applyAdViewPosition: " + (isBanner ? "Banner" : "MREC")
          + " pos=" + position + " gravity=" + gravity
          + " offsetX=" + offsetXDp + "dp offsetY=" + offsetYDp + "dp"
          + " margins=[L=" + marginLeft + " T=" + marginTop
          + " R=" + marginRight + " B=" + marginBottom + "]px");
  // THÀNH:
  Log.d(TAG, "applyAdViewPosition: " + (isBanner ? "Banner" : "MREC")
          + " pos=" + position + " gravity=" + gravity
          + " offsetX=" + offsetXPx + "px offsetY=" + offsetYPx + "px"
          + " margins=[L=" + marginLeft + " T=" + marginTop
          + " R=" + marginRight + " B=" + marginBottom + "]px");
  ```

  **E. Cập nhật Javadoc `SetBannerPosition`** (lines 1437–1438):
  ```java
  // TỪ: @param offsetX Horizontal offset in dp (positive = inward from aligned edge)
  //      @param offsetY Vertical offset in dp (positive = inward from aligned edge)
  // THÀNH: @param offsetX Horizontal offset in pixels (positive = inward from aligned edge)
  //        @param offsetY Vertical offset in pixels (positive = inward from aligned edge)
  ```

  **F. Cập nhật Javadoc `SetMRECPosition`** (lines 1529–1530):
  ```java
  // TỪ: @param offsetX Horizontal offset in dp (positive = inward from aligned edge)
  //      @param offsetY Vertical offset in dp (positive = inward from aligned edge)
  // THÀNH: @param offsetX Horizontal offset in pixels (positive = inward from aligned edge)
  //        @param offsetY Vertical offset in pixels (positive = inward from aligned edge)
  ```

  **Must NOT do**:
  - Không đụng đến `dpToPx()` ở bất kỳ dòng nào khác ngoài 545–546
  - Không sửa phần logic gravity, insets, LayoutParams
  - Không đổi tên fields `_bannerCustomOffsetX/Y`, `_mrecCustomOffsetX/Y`

  **Recommended Agent Profile**:
  > Trivial targeted edit — chính xác theo từng dòng.
  - **Category**: `quick`
    - Reason: Thay đổi nhỏ, xác định rõ ràng, không cần phân tích phức tạp
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 — only task
  - **Blocks**: F1
  - **Blocked By**: None (can start immediately)

  **References**:

  **Pattern References**:
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java:490–596` — toàn bộ `applyAdViewPosition` method cần sửa
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java:1427–1455` — `SetBannerPosition` — cập nhật Javadoc
  - `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java:1520–1547` — `SetMRECPosition` — cập nhật Javadoc

  **WHY Each Reference Matters**:
  - Lines 545–546 chứa đúng 2 dòng cần xóa — đây là cốt lõi của bug
  - Lines 486–487 chứa `@param` tags cần rename từ "Dp" → "Px" và "in dp" → "in pixels"
  - Lines 481 chứa description text "dp offsets" cần đổi thành "px offsets"
  - Lines 591–593 là log statement — cập nhật unit label và param names
  - Lines 1437–1438 và 1529–1530 là Javadoc của 2 public methods cần đồng bộ

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Build thành công sau khi sửa
    Tool: Bash
    Preconditions: Source đã được sửa đúng theo spec
    Steps:
      1. Chạy: ./gradlew :maxads:assembleDebug
      2. Chạy: ./gradlew :maxads:testDebugUnitTest
    Expected Result: "BUILD SUCCESSFUL" cho cả 2 lệnh
    Failure Indicators: Compile error, "cannot find symbol offsetXDp/offsetYDp"
    Evidence: .sisyphus/evidence/task-1-build-verify.txt

  Scenario: Xác nhận không còn dòng dpToPx cho offset
    Tool: Bash (grep)
    Preconditions: File đã được sửa
    Steps:
      1. Grep: grep -n "dpToPx.*offsetX\|dpToPx.*offsetY\|offsetXDp\|offsetYDp" MaxAdsService.java
    Expected Result: Không có kết quả nào (empty output)
    Failure Indicators: Bất kỳ match nào xuất hiện = chưa sửa đầy đủ
    Evidence: .sisyphus/evidence/task-1-grep-verify.txt

  Scenario: Xác nhận log statement đã cập nhật đúng
    Tool: Bash (grep)
    Preconditions: File đã được sửa
    Steps:
      1. Grep: grep -n "offsetX.*px\|offsetY.*px" MaxAdsService.java | grep applyAdViewPosition -A5
    Expected Result: Log line chứa "px" thay vì "dp" cho offset values
    Evidence: .sisyphus/evidence/task-1-log-verify.txt
  ```

  **Evidence to Capture**:
  - [ ] task-1-build-verify.txt — output của 2 lệnh gradle
  - [ ] task-1-grep-verify.txt — xác nhận không còn old param names
  - [ ] task-1-log-verify.txt — xác nhận log đã đổi unit

  **Commit**: YES
  - Message: `fix(maxads): remove double dpToPx conversion for ad position offsets`
  - Files: `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`
  - Pre-commit: `./gradlew :maxads:assembleDebug`

---

## Final Verification Wave

- [ ] F1. **Build + Regression Check** — `quick`
  Chạy `./gradlew :maxads:assembleDebug` và `./gradlew :maxads:testDebugUnitTest`. Xác nhận BUILD SUCCESSFUL. Grep kiểm tra không còn `offsetXDp`/`offsetYDp`/`dpToPx.*offset` trong file.
  Output: `Build [PASS/FAIL] | Tests [PASS/FAIL] | Grep clean [YES/NO] | VERDICT: APPROVE/REJECT`

---

## Commit Strategy

- **Task 1**: `fix(maxads): remove double dpToPx conversion for ad position offsets`
  Files: `maxads/src/main/java/com/rofi/maxads/MaxAdsService.java`

---

## Success Criteria

### Verification Commands
```bash
./gradlew :maxads:assembleDebug                    # Expected: BUILD SUCCESSFUL
./gradlew :maxads:testDebugUnitTest                # Expected: BUILD SUCCESSFUL
grep -n "offsetXDp\|offsetYDp\|dpToPx.*offset" \
  maxads/src/main/java/com/rofi/maxads/MaxAdsService.java  # Expected: no output
```

### Final Checklist
- [ ] `applyAdViewPosition` signature: params renamed to `offsetXPx`/`offsetYPx`
- [ ] Lines 545–546 (dpToPx conversion) đã xóa
- [ ] Javadoc description line 481: "dp" → "px"
- [ ] Javadoc params lines 486–487: "Dp" → "Px", "in dp" → "in pixels"
- [ ] Log lines 591–593: "dp" → "px" trong unit label và param names
- [ ] Javadoc `SetBannerPosition` lines 1437–1438: "in dp" → "in pixels"
- [ ] Javadoc `SetMRECPosition` lines 1529–1530: "in dp" → "in pixels"
- [ ] Tất cả 15 lần `dpToPx()` khác **còn nguyên** (không bị chỉnh)
- [ ] Build SUCCESSFUL

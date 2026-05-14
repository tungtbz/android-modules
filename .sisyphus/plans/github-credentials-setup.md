# GitHub Credentials Setup for Maven Publishing

## TL;DR

> **Quick Summary**: Thay thế bare variable access (`GITHUB_TOKEN_PUSH`) bằng `findProperty()` fallback trong 3 module publishing, và cập nhật README với hướng dẫn setup credentials user-level.
>
> **Deliverables**:
> - 3 `build.gradle` files patched (maxads, admobadshelper, analytic)
> - `README.md` cập nhật với hướng dẫn `~/.gradle/gradle.properties`
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO — sequential (3 trivial patches + 1 doc update)
> **Critical Path**: Task 1 → Task 2 → Task 3 → Task 4

---

## Context

### Original Request
Setup `GITHUB_USER_NAME` và `GITHUB_TOKEN_PUSH` phù hợp cho `maxads/build.gradle` và các module publishing khác.

### Interview Summary
- **Two-token model intentional**: `GITHUB_TOKEN` (read, settings.gradle) vs `GITHUB_TOKEN_PUSH` (write, build.gradle)
- **Current problem**: bare variable access gây `MissingPropertyException` khi Gradle sync mà không có credentials
- **Fix**: `findProperty("GITHUB_TOKEN_PUSH") ?: ""` ngăn crash, lỗi vẫn xuất hiện ở publish step (đúng chỗ)
- **Scope**: 3 files duy nhất — maxads, admobadshelper, analytic build.gradle

### Metis Review
**Guardrails applied**:
- Giữ nguyên tên biến (không rename `GITHUB_TOKEN` / `GITHUB_TOKEN_PUSH`)
- Apply `findProperty` cho cả `username` lẫn `password` fields
- Docs phải có exact file path và exact property keys
- KHÔNG thêm CI, token rotation, SDK alignment vào scope này

---

## Work Objectives

### Core Objective
Patch 3 publishing blocks để dùng `findProperty()` fallback, tránh crash khi credentials vắng mặt, và document setup procedure.

### Concrete Deliverables
- `maxads/build.gradle` — credentials dùng `findProperty`
- `admobadshelper/build.gradle` — credentials dùng `findProperty`
- `analytic/build.gradle` — credentials dùng `findProperty`
- `README.md` — thêm section "Publishing Setup"

### Must Have
- `findProperty("GITHUB_USER_NAME") ?: ""` và `findProperty("GITHUB_TOKEN_PUSH") ?: ""` trong cả 3 module
- Gradle sync thành công khi không có credentials
- `./gradlew :maxads:tasks` chạy không crash
- Publish task fail ở auth step (không phải property-resolution step) khi thiếu credentials
- README có exact path `~/.gradle/gradle.properties` và exact key names

### Must NOT Have
- KHÔNG rename property names
- KHÔNG thêm CI workflows
- KHÔNG sửa SDK versions / dependency versions
- KHÔNG commit credentials vào bất kỳ tracked file nào
- KHÔNG sửa `settings.gradle` credentials (đó là read token — scope khác)
- KHÔNG sửa repository URLs hay publishing targets

---

## Verification Strategy

### Test Decision
- **Automated tests**: None (config-only change)
- **Agent-Executed QA**: ALWAYS

### QA Policy
- **Config verification**: grep/read để confirm `findProperty` pattern
- **Build verification**: `./gradlew :maxads:tasks` không crash
- **Failure-path**: Gradle tasks succeed khi không có credentials; publish command fail tại auth step

---

## Execution Strategy

```
Wave 1 (Sequential — 3 patches + 1 doc):
├── Task 1: Patch maxads/build.gradle
├── Task 2: Patch admobadshelper/build.gradle
├── Task 3: Patch analytic/build.gradle
└── Task 4: Update README.md with publishing setup docs

Wave FINAL:
└── Task F1: Verify all patches correct + build config succeeds
```

---

## TODOs

- [x] 1. Patch `maxads/build.gradle` — thay bare variables bằng `findProperty`

  **What to do**:
  - Thay lines 39-40:
    ```groovy
    // BEFORE
    username = GITHUB_USER_NAME
    password = GITHUB_TOKEN_PUSH

    // AFTER
    username = findProperty("GITHUB_USER_NAME") ?: ""
    password = findProperty("GITHUB_TOKEN_PUSH") ?: ""
    ```

  **Must NOT do**:
  - Không sửa bất kỳ dòng nào khác trong file
  - Không đổi tên biến

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential Wave 1
  - **Blocks**: Task 2, Task 3 (pattern consistency check)
  - **Blocked By**: None

  **References**:
  - `maxads/build.gradle:38-41` — credentials block to patch

  **Acceptance Criteria**:
  - [ ] `grep -n "findProperty" maxads/build.gradle` → xuất hiện 2 dòng (username + password)
  - [ ] `grep -n "GITHUB_TOKEN_PUSH\b" maxads/build.gradle` → 0 bare references còn lại

  **QA Scenarios**:
  ```
  Scenario: Verify patch applied correctly
    Tool: Bash (grep)
    Steps:
      1. grep -n "findProperty" maxads/build.gradle
    Expected Result: 2 lines matching — username and password both use findProperty
    Evidence: .sisyphus/evidence/task-1-patch-verify.txt

  Scenario: No bare variable references remain
    Tool: Bash (grep)
    Steps:
      1. grep -c "username = GITHUB_USER_NAME$" maxads/build.gradle
      2. grep -c "password = GITHUB_TOKEN_PUSH$" maxads/build.gradle
    Expected Result: Both return 0
    Evidence: .sisyphus/evidence/task-1-bare-refs.txt
  ```

  **Commit**: YES (groups with Task 2, 3)
  - Message: `fix(maxads): use findProperty for GitHub credentials`
  - Files: `maxads/build.gradle`

---

- [x] 2. Patch `admobadshelper/build.gradle` — thay bare variables bằng `findProperty`

  **What to do**:
  - Thay lines 48-49 (same pattern as Task 1):
    ```groovy
    username = findProperty("GITHUB_USER_NAME") ?: ""
    password = findProperty("GITHUB_TOKEN_PUSH") ?: ""
    ```

  **Must NOT do**:
  - Không sửa bất kỳ dòng nào khác

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 3, after Task 1)
  - **Parallel Group**: Wave 1 (Tasks 2+3 parallel)
  - **Blocks**: Task F1
  - **Blocked By**: None (Task 1 is independent)

  **References**:
  - `admobadshelper/build.gradle:47-50` — credentials block

  **Acceptance Criteria**:
  - [ ] `grep -n "findProperty" admobadshelper/build.gradle` → 2 dòng

  **QA Scenarios**:
  ```
  Scenario: Verify patch applied
    Tool: Bash (grep)
    Steps:
      1. grep -n "findProperty" admobadshelper/build.gradle
    Expected Result: 2 lines matching
    Evidence: .sisyphus/evidence/task-2-patch-verify.txt
  ```

  **Commit**: Groups with Task 1, 3
  - Message: `fix(admobadshelper): use findProperty for GitHub credentials`
  - Files: `admobadshelper/build.gradle`

---

- [x] 3. Patch `analytic/build.gradle` — thay bare variables bằng `findProperty`

  **What to do**:
  - Thay lines 48-49 (same pattern):
    ```groovy
    username = findProperty("GITHUB_USER_NAME") ?: ""
    password = findProperty("GITHUB_TOKEN_PUSH") ?: ""
    ```

  **Must NOT do**:
  - Không sửa bất kỳ dòng nào khác

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 2)
  - **Parallel Group**: Wave 1 (Tasks 2+3 parallel)
  - **Blocks**: Task F1
  - **Blocked By**: None

  **References**:
  - `analytic/build.gradle:47-50` — credentials block

  **Acceptance Criteria**:
  - [ ] `grep -n "findProperty" analytic/build.gradle` → 2 dòng

  **QA Scenarios**:
  ```
  Scenario: Verify patch applied
    Tool: Bash (grep)
    Steps:
      1. grep -n "findProperty" analytic/build.gradle
    Expected Result: 2 lines matching
    Evidence: .sisyphus/evidence/task-3-patch-verify.txt
  ```

  **Commit**: Groups with Task 1, 2
  - Message: `fix(analytic): use findProperty for GitHub credentials`
  - Files: `analytic/build.gradle`

---

- [x] 4. Update `README.md` — thêm section "Publishing Setup"

  **What to do**:
  - Thêm section sau vào `README.md`:

    ```markdown
    ## Publishing Setup

    Ba module (`maxads`, `admobadshelper`, `analytic`) publish AAR lên GitHub Packages.
    Credentials phải được đặt ở **user-level** Gradle properties — không bao giờ commit vào repo.

    ### Setup `~/.gradle/gradle.properties`

    Tạo hoặc chỉnh sửa file `~/.gradle/gradle.properties`:

    ```properties
    # GitHub Packages — android-modules
    GITHUB_USER_NAME=your-github-username

    # Read token (scope: read:packages) — dùng để resolve dependencies
    GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx

    # Write token (scope: write:packages) — dùng để publish
    GITHUB_TOKEN_PUSH=ghp_xxxxxxxxxxxxxxxxxxxx
    ```

    > Có thể dùng 1 token duy nhất với cả hai scope cho cả `GITHUB_TOKEN` và `GITHUB_TOKEN_PUSH`.

    ### Publish commands

    ```bash
    ./gradlew :maxads:publish
    ./gradlew :admobadshelper:publish
    ./gradlew :analytic:publish
    ```

    Nếu thiếu credentials: Gradle sync vẫn thành công, nhưng publish task sẽ fail tại bước authentication.
    ```

  **Must NOT do**:
  - Không commit credentials thật
  - Không thay đổi nội dung khác trong README

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (với Tasks 1-3)
  - **Parallel Group**: Wave 1
  - **Blocks**: Task F1
  - **Blocked By**: None

  **References**:
  - `README.md` — append section

  **Acceptance Criteria**:
  - [ ] `grep -n "GITHUB_USER_NAME" README.md` → có kết quả
  - [ ] `grep -n "~/.gradle/gradle.properties" README.md` → có kết quả
  - [ ] `grep -n "write:packages" README.md` → có kết quả (phân biệt 2 token)

  **QA Scenarios**:
  ```
  Scenario: Docs contain required content
    Tool: Bash (grep)
    Steps:
      1. grep -n "GITHUB_TOKEN_PUSH" README.md
      2. grep -n "GITHUB_TOKEN" README.md
      3. grep -n "~/.gradle/gradle.properties" README.md
    Expected Result: All 3 grep commands return ≥1 result
    Evidence: .sisyphus/evidence/task-4-docs-verify.txt
  ```

  **Commit**: YES (separate commit)
  - Message: `docs: add publishing setup instructions for GitHub Packages`
  - Files: `README.md`

---

## Final Verification Wave

- [x] F1. **Verify + Build Config Check** — `quick`

  Run all verification steps:
  ```bash
  # 1. Confirm findProperty pattern in all 3 modules
  grep -n "findProperty" maxads/build.gradle admobadshelper/build.gradle analytic/build.gradle

  # 2. Confirm no bare variable references remain
  grep -rn "password = GITHUB_TOKEN_PUSH$" maxads/ admobadshelper/ analytic/

  # 3. Confirm README has correct docs
  grep -n "GITHUB_TOKEN_PUSH\|~/.gradle" README.md
  ```

  Expected:
  - Step 1: 6 lines total (2 per module)
  - Step 2: 0 results
  - Step 3: ≥2 results

  Output: `Patches [3/3] | Docs [PASS] | VERDICT: APPROVE/REJECT`

---

## Commit Strategy

- **Commit 1**: `fix: use findProperty for GitHub credentials in publishing modules` — maxads, admobadshelper, analytic build.gradle
- **Commit 2**: `docs: add publishing setup instructions for GitHub Packages` — README.md

## Success Criteria

```bash
# Verify all 3 modules patched
grep -rn "findProperty.*GITHUB" maxads/build.gradle admobadshelper/build.gradle analytic/build.gradle
# Expected: 6 lines (2 per file)

# Verify no bare references remain
grep -rn "password = GITHUB_TOKEN_PUSH$" .
# Expected: no output
```

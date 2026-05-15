# android-modules

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

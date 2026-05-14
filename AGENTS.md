# PROJECT KNOWLEDGE BASE

**Generated:** 2026-05-14
**Commit:** f4c7757
**Branch:** feature/Feature-improve

## OVERVIEW
Android monorepo with 10 modules providing ad SDK integrations (AppLovin MAX, AdMob, IronSource, Facebook), analytics (Firebase, AppsFlyer), remote config, and camera utilities. Pure Java, Gradle 7.5, AGP 7.4.2.

## STRUCTURE
```
android-modules/
├── app/                    # Main application (empty shell, unwired)
├── base/                  # Common utilities, Constants, ThreadUtils
├── ads/                   # Abstraction layer: IAdsService, AdsManager
├── maxads/                # AppLovin MAX implementation
├── admobadshelper/        # AdMob + consent management
├── ironsourceads/        # IronSource implementation
├── analytic/              # Firebase + AppsFlyer analytics
├── nativeads/             # Facebook Audience Network
├── remoteconfig/          # Firebase Remote Config
└── camerapreview/         # CameraX helper (different namespace: com.tungt)
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add new ad network | `:maxads`, `:ironsourceads` | Implement `IAdsService` interface |
| Add analytics event | `:analytic` | FirebaseAnalytic or AppflyerAcnalytic |
| Remote config | `:remoteconfig` | FirebaseRemoteConfigService |
| Consent/GDPR | `:admobadshelper` | GoogleMobileAdsConsentManager |
| Build/publish | Module `build.gradle` | maven-publish to GitHub Packages |

## CODE MAP
| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| IAdsService | Interface | `:ads` | 3 | Ad network contract |
| AdsManager | Class | `:ads` | 2 | Facade for all ad services |
| MaxAdsService | Class | `:maxads` | 1 | AppLovin implementation |
| AdmobHelper | Class | `:admobadshelper` | 2 | AdMob loading/display |
| FirebaseAnalytic | Class | `:analytic` | 1 | Firebase event tracking |
| IAnalytic | Interface | `:analytic` | 2 | Analytics contract |
| FirebaseRemoteConfigService | Class | `:remoteconfig` | 1 | Remote config fetcher |
| CameraHelper | Class | `:camerapreview` | 1 | CameraX wrapper |

## CONVENTIONS (THIS PROJECT)
- **Language**: Java only (not Kotlin) — per `.github/instructions`
- **Java version**: 1.8 (source/target compatibility)
- **Build**: `minifyEnabled false` in all modules — no R8 obfuscation
- **SDK targets**: Mixed — `:app`, `:maxads`, `:remoteconfig` = SDK 33; others = SDK 32
- **minSdk**: 21 most modules, 24 for `:maxads`, `:remoteconfig`, `:camerapreview`
- **Package namespace**: `com.rofi.*` except `camerapreview` = `com.tungt.*`
- **Publishing**: maven-publish to GitHub Packages (modules: maxads, admobadshelper, analytic)

## ANTI-PATTERNS (THIS PROJECT)
- **NEVER use test ad IDs in production** — only in debug builds
- **NEVER show interstitial during checkout/critical flows**
- **ALWAYS call cleanup() in onDestroy()** — prevents memory leaks
- **ALWAYS call onPause()/onResume()** — proper ad lifecycle
- **ALWAYS provide alternatives to rewarded ads** — fallback for users who skip
- **Never return null collections** — return empty list/map instead
- **Do not expose DTOs to UI** — map to domain models first

## UNIQUE STYLES
- **No version catalog** — each module declares dependencies inline; uses `+` dynamic versions (non-reproducible)
- **No CI pipeline** — no `.github/workflows`, manual publishing only
- **Hardcoded GitHub PAT** in `maxads/gradle.properties` — CRITICAL security issue (to be removed)
- **Duplicate settings** — `:admobadshelper` included twice in `settings.gradle`
- **App module unwired** — no `implementation project(':...')` dependencies on library modules

## COMMANDS
```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Run tests
./gradlew testDebugUnitTest

# Lint
./gradlew lint

# Publish (requires GITHUB_TOKEN)
./gradlew :maxads:publish
```

## NOTES
- **SDK version drift**: `admobadshelper`, `analytic`, `ironsourceads`, `nativeads` on SDK 32 vs SDK 33 elsewhere — align before Play Store submission
- **Ghost directories**: `admobnextgen/`, `extralibs/` exist but not included in settings.gradle
- **Empty MainActivity**: `:app` has no initialization of any library modules — needs wiring
- **Test coverage**: 0% — only scaffold `ExampleInstrumentedTest` files exist
- **Credentials**: Add to `~/.gradle/gradle.properties` (user-level), never commit to repo
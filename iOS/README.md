# RCForb — iOS Port

Compose Multiplatform iOS scaffold. Targets iOS 16+ on iPhone and iPad
(arm64 device + arm64 simulator). Source-of-truth is `../android/` and the
spec at `../PORTING.md` §10.

## Status

Toolchain bootstrap. The Compose Multiplatform iOS framework builds, Xcode
embeds it, and the resulting `RCForb.app` launches a placeholder Compose
screen on the simulator and on device. Real functionality (login, lobby,
radio control, audio) hasn't been ported yet — the desktop port pattern of
"copy Kotlin source from `../android/`" needs platform actuals first
because iOS has no JVM:

| Subsystem | Android / Desktop | iOS replacement |
|---|---|---|
| Persistence | `java.util.prefs` / `SharedPreferences` | `NSUserDefaults` (iosMain actual) |
| Audio I/O | `javax.sound.sampled` / `AudioRecord` | `AVFAudio` engine + tap (iosMain actual) |
| libspeex | JNA / NDK JNI | `cinterop` against `libspeex.a` (built for iosArm64 + iosSimulatorArm64) |
| Sockets | `java.net.Socket` / `DatagramSocket` | `ktor-network` (multiplatform, just add to commonMain) |
| HTTP | `HttpURLConnection` | `ktor-client-darwin` |

These get added under `src/iosMain/kotlin/com/rcforb/` as `actual`s when
the corresponding `expect`s land in `src/commonMain/kotlin/com/rcforb/`.

## Layout

```
iOS/
├── build.gradle.kts          # KMP + Compose Multiplatform, iosArm64 + iosSimulatorArm64 targets
├── settings.gradle.kts
├── gradle.properties         # Includes kotlin.apple.xcodeCompatibility.nowarn for Xcode 26
├── gradlew + gradle/         # Wrapper
├── src/
│   ├── commonMain/kotlin/com/rcforb/App.kt           # Compose UI (currently placeholder)
│   └── iosMain/kotlin/com/rcforb/MainViewController.kt  # Bridges Compose to UIViewController
└── iosApp/
    ├── project.yml           # xcodegen spec
    ├── iosApp.xcodeproj      # Generated — regenerate via `cd iosApp && xcodegen`
    └── iosApp/
        ├── iOSApp.swift      # @main entry, hosts ContentView in WindowGroup
        ├── ContentView.swift # UIViewControllerRepresentable wrapping MainViewController()
        └── Info.plist        # NSMicrophoneUsageDescription + bundle identifiers
```

## Build prerequisites

- macOS host with Xcode 16+ (Xcode 26 also works — KMP warning silenced)
- JDK 17+ (Gradle uses it for KMP plugin)
- `xcodegen` (`brew install xcodegen`) to regenerate `iosApp.xcodeproj` from `project.yml`

## Run from source

### Xcode

```bash
open iOS/iosApp/iosApp.xcodeproj
# Select an iPhone simulator, click Run.
```

The pre-build phase invokes
`./gradlew embedAndSignAppleFrameworkForXcode` automatically.

### Command line (simulator smoke test)

```bash
cd iOS/iosApp
xcodebuild -project iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -arch arm64 \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
# Output: build/Debug-iphonesimulator/RCForb.app
```

To install + launch on a booted simulator:

```bash
xcrun simctl boot "iPhone 16"
xcrun simctl install booted build/Debug-iphonesimulator/RCForb.app
xcrun simctl launch booted com.rcforb.ios
```

## Distribution

iOS apps don't go on a GitHub release page like the desktop ports. The
realistic options:

| Path | Audience | Cost |
|---|---|---|
| **TestFlight** | Up to 10,000 external testers via invite link | Apple Developer Program ($99/yr) |
| **Ad-hoc IPA** | Up to 100 specific device UDIDs | Apple Developer Program ($99/yr) |
| **App Store** | Public | Apple Developer Program + review |
| Personal sideload via Xcode | Developer's own devices for 7 days | Free |

When ready, set `DEVELOPMENT_TEAM` in `project.yml`, regenerate the project,
archive in Xcode, distribute via TestFlight Connect.

## Project layout

See "Layout" above. The Kotlin code under `src/commonMain/` will eventually
mirror `../android/app/src/main/java/com/rcforb/android/` minus
android-specific imports — same UI, networking, protocol, models, services.
Audio/codec/persistence will be `expect`/`actual`-paired with the iosMain
implementations.

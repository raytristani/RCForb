# RCForb Client

A multi-platform remote radio control client for [RemoteHams.com](https://www.remotehams.com) stations. RCForb lets amateur radio operators connect to and control remote HF/VHF/UHF radio stations over the internet from anywhere in the world.

## What it does

RCForb Client connects to RCForb Server instances published on RemoteHams.com, giving full remote control of the radio:

- **Frequency tuning** via VFO A/B knobs with configurable step sizes (10 Hz to 10 kHz)
- **Mode selection** (LSB, USB, AM, CW, FM, RTTY, and more)
- **Real-time audio streaming** — receive (Speex / Opus) and transmit via Push-to-Talk
- **S-meter** with live signal strength readings
- **Full radio controls** — buttons, dropdowns, sliders for filters, noise reduction, AGC, squelch, etc.
- **Split mode** for DX pileups (RX on VFO A, TX on VFO B)
- **Chat** with other operators connected to the same station
- **Rotator, amplifier, and antenna switch control** (when available on the remote station)

## Architecture

The Android module is the **canonical source**. All other ports copy the Kotlin source near-verbatim:

- `android/` — Jetpack Compose, native Speex via NDK/JNI, MediaCodec for Opus. Source of truth.
- `macOS/`, `Linux/`, `Windows/` — Compose Multiplatform Desktop. Same Kotlin source as Android (UI, networking, protocol, audio bridge), with platform shims for libspeex (JNA → `.dylib`/`.so`/`.dll`) and `Preferences` (`java.util.prefs`).
- `iOS/` — Compose Multiplatform iOS scaffold. Toolchain bootstrapped; functional port pending iOS actuals (`NSUserDefaults`, `AVFAudio`, ktor-network, libspeex `cinterop`).

The full porting specification is in [`PORTING.md`](PORTING.md).

## Platform support

| Platform | Status | Stack | Distribution |
|----------|--------|-------|--------------|
| Android (tablet) | Released | Kotlin / Jetpack Compose, NDK Speex | Build from source |
| macOS (Apple Silicon / Intel) | Released | Compose Desktop / Kotlin / JDK 21 | Signed + notarized zip on GitHub Releases |
| Linux (x86_64) | Released | Compose Desktop / Kotlin / JDK 21 | `.deb`, `.rpm`, `.tar.gz` on GitHub Releases |
| Windows 10/11 (x86_64) | Released | Compose Desktop / Kotlin / JDK 21 | `.msi`, `.exe` on GitHub Releases |
| iOS / iPadOS | Scaffold only | Compose Multiplatform / Kotlin Native | Build from source via Xcode |

## Installation

Pre-built binaries for v1.0.0 are on the [GitHub Releases page](https://github.com/raytristani/RCForb/releases):

- **macOS** — `RCForb-macos-1.0.0.zip` (signed + notarized; unzip and run)
- **Linux** — `rcforb_1.0.0-1_amd64.deb`, `rcforb-1.0.0-1.x86_64.rpm`, or portable `RCForb-linux-1.0.0.tar.gz`
- **Windows** — `RCForb-windows-1.0.0.msi` or `RCForb-windows-1.0.0.exe`

Android, iOS, and unreleased platforms must be built from source.

## Building from source

All desktop ports follow the same pattern: `cd <platform> && ./gradlew run`. Each port's `README.md` covers prerequisites and packaging.

### Android

```bash
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires Android SDK API 26+, Kotlin 1.9+, NDK 27+, CMake 3.22+. libspeex 1.2.1 is compiled from source via NDK/JNI.

### macOS

```bash
cd macOS
./gradlew run               # dev run
./gradlew macSafeZip        # signed + notarized zip (requires Developer ID)
./gradlew macSafeDmg        # signed + notarized DMG
```

JDK 17+ and Xcode Command Line Tools required. See [`macOS/README.md`](macOS/README.md) for signing/notarization setup.

### Linux

```bash
cd Linux
./gradlew run                          # dev run
./gradlew packageDeb packageRpm        # native packages
./Linux/build-linux.sh                 # cross-build via Docker
```

JDK 17+, gcc, ImageMagick, fakeroot. See [`Linux/README.md`](Linux/README.md).

### Windows

```powershell
cd Windows
.\gradlew run               # dev run (on a Windows host)
.\gradlew packageMsi        # MSI installer
```

JDK 17+, MSYS2 with `mingw-w64-x86_64-gcc` for `speex_jni.dll`. Cross-builds from macOS/Linux are unreliable — use the `release-windows.yml` GitHub Actions workflow. See [`Windows/README.md`](Windows/README.md).

### iOS

```bash
cd iOS/iosApp
xcodegen
open iosApp.xcodeproj
```

JDK 17+, Xcode 16+, `xcodegen` (`brew install xcodegen`). Currently a placeholder Compose screen — see [`iOS/README.md`](iOS/README.md) for the actuals roadmap.

## Project structure

```
RCForb/
├── android/          Canonical source — Jetpack Compose + NDK Speex
├── macOS/            Compose Desktop port (Kotlin source mirrors android/)
├── Linux/            Compose Desktop port
├── Windows/          Compose Desktop port
├── iOS/              Compose Multiplatform iOS scaffold
├── PORTING.md        Canonical porting specification
└── README.md
```

## Test stations

Public stations with TX enabled, useful for testing PTT and audio:

| Station | Radio | Notes |
|---------|-------|-------|
| LARC | IC-7300 | London Amateur Radio Club, public |
| SV1BMQ | IC-7300 | Public, Greece |
| HR5HAC | Kenwood TS-50 | Public, Honduras |
| ZL1HN | IC-7100 | Public, New Zealand |
| ZS6WDL | IC-7300 | Public, South Africa |
| W6JFA | IC-7610 | Public, EFHW antenna |
| KE6GG | IC-7300 | Public |
| K6BJ DeLa | TS-570 | Public, Santa Cruz CA |

> Most stations require tune permission before transmitting. Type "May I tune the remote?" in the chat window and wait for approval. Some require owner approval which may take time.

## Protocol

RCForb speaks a custom protocol over UDP (V10, Opus audio) or TCP (V7, Speex audio). V10/Opus is currently stubbed on desktop ports — V10-only stations won't produce or transmit audio there. Most public stations on RemoteHams are V7 and work fully. See `PORTING.md` §9.4 for the V10 enablement path.

## Author

Ramon E. Tristani (raytristani@gmail.com)

## License

MIT

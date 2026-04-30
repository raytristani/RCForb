# RCForb — macOS Desktop Port

Compose Multiplatform Desktop port of the RCForb Android client. Targets
macOS 11+ (Apple Silicon / Intel universal). Source-of-truth is `../android/`
and the spec at `../PORTING.md`.

## Run from source

```bash
cd macOS
./gradlew run
```

The build:
1. Compiles bundled libspeex 1.2.1 sources (from `android/app/src/main/cpp/`)
   into a universal `libspeex_jni.dylib` via clang.
2. Bundles the dylib + `app_icon.png`, `knob_xlarge.png`, `digital_7_mono.ttf`
   into the JAR resources.
3. Launches a Compose Desktop window at 1395×833.

Java 17+ JDK and Xcode Command Line Tools (`xcode-select --install`) are
required.

## Functional coverage

| Feature | Status |
|---------|--------|
| Login (form-based, RemoteHams.com) | ✅ |
| Saved credentials (XOR + Base64 in `java.util.prefs`) | ✅ |
| Lobby fetch (`xmlfeed.php`) + search + resizable columns | ✅ |
| Favorites sidebar | ✅ |
| V7 TCP connect (Speex audio) | ✅ |
| V10 UDP connect (Opus audio) | ⚠️ stub — see *Opus* below |
| Radio screen — VFO knobs, frequency, S-meter, dropdowns, sliders, buttons, status pills | ✅ |
| PTT (TX button command + Speex TX audio) | ✅ |
| Mic test | ✅ |
| Chat | ✅ |
| Peripherals (rotator, amp, switch) | ✅ |
| Touch ID biometric auth | ⏭ skipped per PORTING.md §9.6 (form login covers all cases) |
| App icon (universal `.icns` from `app_icon.png`) | ✅ |

### Opus codec

V10/Opus is currently stubbed — `OpusEncoder` returns `null` and
`OpusDecoder` returns silence. This means V10-only stations will not produce
or transmit audio. Most public stations on RemoteHams are V7 and work fully.

To enable V10, follow PORTING.md §9.4: bundle `libopus.dylib` (e.g. via
`brew install opus` or build from xiph/opus source) and replace the stubs
in `src/main/kotlin/com/rcforb/audio/OpusEncoder.kt` and `OpusDecoder.kt`
with JNA bindings.

## Distribution

### Build

```bash
./gradlew macSafeDmg     # build/compose/binaries/main/dmg/RCForb-1.0.0.dmg
./gradlew packageZip     # build/compose/binaries/main/zip/RCForb-macos-1.0.0.zip
```

`./gradlew packageDmg` (Compose Desktop's stock task) is broken on macOS
Sequoia/Tahoe + JDK 21 because jpackage's mandatory `codesign -s -` step
rejects the `com.apple.FinderInfo` and `com.apple.fileprovider.fpfs#P`
xattrs that the OS auto-attaches to bundle directories. The custom
`macSafeDmg` task works around this by:

1. Calling jpackage `--type app-image` and ignoring its codesign failure.
2. `mkdir`ing a fresh `.app/` (no auto-attached bundle xattrs) and
   `ditto --noextattr --noqtn`-ing only the `Contents/` subtree into it.
3. Extracting skiko native dylibs from the skiko jar.
4. Codesigning, then `hdiutil create -format UDZO`.

### Choosing a distribution path

The build machine signs ad-hoc by default. **Ad-hoc signatures only run on
the machine that produced them** — on any other Mac, Gatekeeper rejects
the app with "RCForb is damaged" or "developer cannot be verified". Three
options:

#### Option 1 — Apple Developer ID (best, costs $99/yr)

The only way `double-click → it just runs` works on every Mac. Once:

```bash
# Get a Developer ID Application certificate from developer.apple.com
# (costs $99/year). Install it in your login keychain, then:
xcrun notarytool store-credentials "rcforb-notary" \
  --apple-id you@example.com \
  --team-id YOURTEAMID \
  --password app-specific-password   # see appleid.apple.com
```

Then for every release:

```bash
export RCFORB_SIGN_IDENTITY="Developer ID Application: Your Name (TEAMID)"
export RCFORB_NOTARY_PROFILE="rcforb-notary"
./gradlew macSafeDmg
```

The build will codesign with hardened runtime + the entitlements RCForb
needs (JIT, library validation off, mic input), submit to Apple's notary
service, wait for approval, and staple the ticket to the DMG. Distribute
the DMG; recipients double-click it and it just runs.

#### Option 2 — Zip + one-time approval (free)

Ship the zip. Tell users to **right-click `RCForb.app` → Open → Open**
the first time. macOS prompts once, then remembers; subsequent launches
are instant. This is what most open-source mac apps do.

If users prefer Terminal, this also works:

```bash
xattr -dr com.apple.quarantine /Applications/RCForb.app
```

DMGs are not great for ad-hoc-signed apps because mounting them
re-attaches `com.apple.FinderInfo`, breaking signature verification on
the receiving Mac and (sometimes) triggering DMG translocation that
breaks relative path lookups. Zip avoids both. **Recommended for
unsigned/ad-hoc builds.**

#### Option 3 — DMG + tell users to remove quarantine

Build with `./gradlew macSafeDmg`, ship the DMG. After installing:

```bash
sudo xattr -dr com.apple.quarantine /Applications/RCForb.app
```

Works but is the worst UX of the three.

### What's in the DMG/zip

```
RCForb.app/
├── Contents/
│   ├── Info.plist                     # bundle ID, NSMicrophoneUsageDescription, LSMinimumSystemVersion 11
│   ├── MacOS/RCForb                   # native launcher (jpackage)
│   ├── PkgInfo
│   ├── Resources/RCForb.icns          # from icon/AppIcon.icns
│   ├── app/                           # all jars + skiko native dylibs + libspeex_jni.dylib
│   └── runtime/                       # bundled JRE (jlink-trimmed)
```

No external dependencies — fully self-contained. Universal arm64 + x86_64
on the libspeex side; the JRE is whatever jlink produces for the build
machine's architecture. To produce both arches, build twice on each Mac
or set up a cross-build runtime image.

## Project layout

```
macOS/
├── build.gradle.kts        # Compose Desktop + custom buildSpeexJni task
├── settings.gradle.kts
├── icon/AppIcon.icns       # generated from android app_icon.png
└── src/main/
    ├── kotlin/com/rcforb/
    │   ├── Main.kt                       # window + state routing
    │   ├── audio/                        # AudioBridge, codecs, JNA Speex
    │   ├── models/                       # ported verbatim from Android
    │   ├── networking/                   # UDPClient, TCPClientV7, IpExClient
    │   ├── protocol/                     # CommandParser, ProtocolConstants, MD5
    │   ├── services/                     # Auth, Lobby, Credential/Favorites stores, ConnectionManagerViewModel
    │   ├── ui/                           # Compose UI screens + components + theme
    │   └── util/Log.kt                   # tiny Android Log replacement
    └── resources/
        ├── drawable/{app_icon,knob_xlarge}.png    # copied from Android
        └── font/digital_7_mono.ttf                # copied from Android
```

All Kotlin code mirrors the Android source — same package layout, same
classes, same StateFlow API. Nova Olive theme hex values are byte-identical
in `ui/theme/Color.kt`.

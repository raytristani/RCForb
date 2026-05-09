# RCForb — Windows Desktop Port

Compose Multiplatform Desktop port of the RCForb Android client. Targets
Windows 10 64-bit and later (x86_64). Source-of-truth is `../android/` and
the spec at `../PORTING.md`.

The Kotlin source is byte-identical to `../macOS/src/` and `../Linux/src/`
apart from the `Preferences` namespace (`com/rcforb/windows/*`). All UI,
networking, protocol, and audio code is shared.

## Build prerequisites

- JDK 17+ (Adoptium Temurin recommended)
- MSYS2 with `mingw-w64-x86_64-gcc` (for `speex_jni.dll`)
- ImageMagick (only if `icon/AppIcon.ico` needs regenerating from PNG)

The Windows installer formats (.msi via WiX, .exe via jpackage) need a
Windows host. Cross-builds from macOS/Linux aren't reliable — use the
GitHub Actions workflow (`.github/workflows/release-windows.yml`) instead.

## Run from source (on a Windows host)

```powershell
cd Windows
.\gradlew run
```

The build:
1. Compiles bundled libspeex 1.2.1 sources (from `..\android\app\src\main\cpp\`)
   into `speex_jni.dll` via MinGW gcc.
2. Bundles the DLL + drawables + `digital_7_mono.ttf` into the JAR resources.
3. Launches a Compose Desktop window at 1395×833.

## Functional coverage

Identical to macOS / Linux ports — see `..\macOS\README.md` for the table.
V7 (Speex) stations work fully; V10 (Opus) is stubbed pending bundled
libopus. Audio runs through Java Sound, which routes to WASAPI on Windows.

## Distribution

See `RELEASE.md`. Two paths:

1. **GitHub Actions** — push a `v*` tag and the `release-windows.yml`
   workflow builds and uploads `.msi` + `.exe` to the matching release.
2. **Local** — `pwsh .\Windows\build-windows.ps1` on a Windows host.

| Format | Best for | Notes |
|---|---|---|
| `.msi` | Enterprise / managed Windows | Silent install, group-policy friendly |
| `.exe` | Casual users | Wraps the MSI with a per-user shortcut + uninstaller |

Both bundle a jlink-trimmed JRE — no Java install needed.

## Project layout

Same as macOS — see `..\macOS\README.md`. The only platform-specific files
are `build.gradle.kts` (MinGW gcc + jpackage Windows targets) and
`icon/AppIcon.png` (converted to `.ico` at build time).

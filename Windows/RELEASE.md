# Windows release process

Windows packaging (`.msi` via WiX, `.exe` via jpackage's app-installer) needs
a Windows host — there's no reliable cross-build path the way Linux has via
Docker. Two ways to produce the artifacts:

## Option A — GitHub Actions (recommended)

Pushing a `v*` tag triggers `.github/workflows/release-windows.yml`, which:

1. Spins up a `windows-latest` runner.
2. Installs JDK 17 + MSYS2/MinGW64 (`x86_64-w64-mingw32-gcc`).
3. Generates `icon/AppIcon.ico` from `icon/AppIcon.png` via ImageMagick.
4. Runs `./gradlew packageMsi packageExe` (which compiles `speex_jni.dll`
   via gcc and packages both installers via jpackage + WiX).
5. Uploads `RCForb-windows-1.0.0.msi` and `RCForb-windows-1.0.0.exe` as
   assets on the matching GitHub Release.

You can also run it manually from the Actions tab via "Run workflow",
specifying the release tag to attach to.

## Option B — Local Windows host

If you have a Windows machine (or VM):

```powershell
# Prereqs:
#   - JDK 17 (Adoptium Temurin)
#   - MSYS2 with mingw-w64-x86_64-gcc
#   - ImageMagick (only needed if icon/AppIcon.ico is missing)

pwsh .\Windows\build-windows.ps1
```

Output:

```
Windows\build\compose\binaries\main\msi\RCForb-1.0.0.msi
Windows\build\compose\binaries\main\exe\RCForb-1.0.0.exe
Windows\build\RCForb-windows-1.0.0.msi   (renamed copy)
Windows\build\RCForb-windows-1.0.0.exe   (renamed copy)
```

Upload manually:

```bash
gh release upload v1.0.0 \
  Windows/build/RCForb-windows-1.0.0.msi \
  Windows/build/RCForb-windows-1.0.0.exe
```

## Code-signing

Windows packages are unsigned by default. Recipients see a SmartScreen
warning ("Windows protected your PC") on first launch and must click "More
info → Run anyway." For a smoother UX, get an Authenticode code-signing
certificate ($300+/yr from a CA) and sign the `.msi`/`.exe` via `signtool`
before upload. Not currently set up.

## Notes

- The `upgradeUuid` in `Windows/build.gradle.kts` (`8a4cae2b-…`) must
  remain stable across releases or Windows installer treats new versions
  as separate products.
- `speex_jni.dll` is built statically against MinGW's runtime
  (`-static-libgcc`), so no `libgcc_s_seh-1.dll` runtime dep ships.

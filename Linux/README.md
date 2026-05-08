# RCForb — Linux Desktop Port

Compose Multiplatform Desktop port of the RCForb Android client. Targets
modern x86_64 Linux distributions (tested on Ubuntu 22.04+ / Fedora 39+).
Source-of-truth is `../android/` and the spec at `../PORTING.md`.

The Kotlin source is byte-identical to `../macOS/src/` apart from the
`Preferences` namespace (`com/rcforb/linux/*` vs `com/rcforb/macos/*`).
All UI, networking, protocol, and audio code is shared.

## Build prerequisites

```
JDK 17+ (jpackage requires JDK 14+; recommend Adoptium Temurin 21)
gcc + binutils
imagemagick + fakeroot   # only for .deb / .rpm packaging
```

Debian/Ubuntu:
```bash
sudo apt install openjdk-21-jdk gcc imagemagick fakeroot
```

Fedora:
```bash
sudo dnf install java-21-openjdk-devel gcc ImageMagick fakeroot rpm-build
```

## Run from source

```bash
cd Linux
./gradlew run
```

The build:
1. Compiles bundled libspeex 1.2.1 sources (from `../android/app/src/main/cpp/`)
   into `libspeex_jni.so` via gcc.
2. Bundles the .so + `app_icon.png`, `knob_xlarge.png`, `digital_7_mono.ttf`
   into the JAR resources.
3. Launches a Compose Desktop window at 1395×833.

## Functional coverage

Identical to the macOS port — see `../macOS/README.md` for the table. V7
(Speex) stations work fully; V10 (Opus) is stubbed pending bundled libopus.
On Linux, audio runs through Java Sound's default mixer, which routes to
PulseAudio/PipeWire transparently on every modern distro.

## Distribution

```bash
./gradlew packageDeb       # build/compose/binaries/main/deb/rcforb_1.0.0-1_amd64.deb
./gradlew packageRpm       # build/compose/binaries/main/rpm/rcforb-1.0.0-1.x86_64.rpm
./gradlew packageAppImage  # build/compose/binaries/main/app/RCForb/  (run ./RCForb/bin/RCForb)
./gradlew packageReleaseDistributionForCurrentOS  # all of the above
```

Compose Desktop bundles a jlink-trimmed JRE into each package, so users
don't need Java installed. There's no Linux-equivalent of macOS's
notarization — the packages are unsigned and just work.

### Choosing a format

| Format | Best for | Notes |
|---|---|---|
| `.deb` | Debian, Ubuntu, Mint, Pop!_OS | `sudo apt install ./rcforb_1.0.0-1_amd64.deb` |
| `.rpm` | Fedora, RHEL, openSUSE | `sudo rpm -i rcforb-1.0.0-1.x86_64.rpm` |
| AppImage-style dir | Arch, NixOS, distro-agnostic, USB stick | Tarball the `app/RCForb/` dir; run `./RCForb/bin/RCForb` |

### What's in each package

```
RCForb/
├── bin/RCForb                        # native launcher
├── lib/app/                          # all jars + libspeex_jni.so + skiko native libs
├── lib/runtime/                      # bundled JRE (jlink-trimmed)
└── share/{icons,applications}/...    # desktop integration (Deb/Rpm only)
```

No external runtime dependencies beyond glibc and PulseAudio/PipeWire,
both of which are standard on every desktop distro.

## Project layout

Same as macOS — see `../macOS/README.md`. The only platform-specific files
are `build.gradle.kts` (gcc + jpackage Linux targets) and `icon/AppIcon.png`.

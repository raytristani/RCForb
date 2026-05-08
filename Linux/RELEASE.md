# Linux release process

The Linux artifacts (`.deb`, `.rpm`, portable `.tar.gz`) are cross-built in
an `ubuntu:22.04` Docker container, so any developer host with Docker can
produce them — no Linux box required.

## Build

```bash
chmod +x Linux/build-linux.sh   # one-time
./Linux/build-linux.sh
```

The script:
1. Pulls `ubuntu:22.04` (linux/amd64).
2. `apt install`s OpenJDK 17 + 21, gcc, binutils, fakeroot, rpm, imagemagick.
3. Runs `./gradlew clean packageDeb packageRpm packageAppImage` against
   `Linux/`. `buildSpeexJni` compiles `libspeex_jni.so` via gcc (universal
   x86_64 only — arm64 Linux would need a separate container).
4. Tarballs the `app-image` directory into `RCForb-linux-1.0.0.tar.gz`
   (Compose Desktop's `:packageAppImage` produces a directory, not a real
   `.AppImage` file).

Output:

```
Linux/build/compose/binaries/main/deb/rcforb_1.0.0-1_amd64.deb
Linux/build/compose/binaries/main/rpm/rcforb-1.0.0-1.x86_64.rpm
Linux/build/compose/binaries/main/app/RCForb-linux-1.0.0.tar.gz
```

The first run takes ~3 min (image pull + apt + gradle wrapper download).
Subsequent runs use the cached `.gradle-cache-linux/` directory at the
repo root and finish in ~30 s.

## Publishing

Linux artifacts share the same git tag as macOS (`v1.0.0`). Upload them
alongside the macOS zip on the same release page:

```bash
gh release upload v1.0.0 \
  Linux/build/compose/binaries/main/deb/rcforb_1.0.0-1_amd64.deb \
  Linux/build/compose/binaries/main/rpm/rcforb-1.0.0-1.x86_64.rpm \
  Linux/build/compose/binaries/main/app/RCForb-linux-1.0.0.tar.gz
```

Bump the version in `Linux/build.gradle.kts` (`packageVersion = "x.y.z"`)
and the artifact filename in `build-linux.sh` before cutting the next
release.

## Notes

- No code-signing equivalent on Linux. `.deb`/`.rpm` are unsigned; users
  install via `apt`/`rpm` directly.
- The native `libspeex_jni.so` is statically built into the bundle, so no
  `libspeex` package is required on the user's machine.
- Audio routing goes through `javax.sound.sampled` → PulseAudio/PipeWire
  on every modern desktop distro. No extra configuration needed.

#!/usr/bin/env bash
# Cross-build Linux release artifacts from any host with Docker.
#
# Output:
#   Linux/build/compose/binaries/main/deb/rcforb_1.0.0-1_amd64.deb
#   Linux/build/compose/binaries/main/rpm/rcforb-1.0.0-1.x86_64.rpm
#   Linux/build/compose/binaries/main/app/RCForb-linux-1.0.0.tar.gz
#
# Run from the repo root: ./Linux/build-linux.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# OpenJDK 17 satisfies jvmToolchain(17); 21 is the runtime that runs Gradle
# itself. fakeroot/rpm/imagemagick are required by jpackage's deb/rpm packagers.
docker run --rm --platform linux/amd64 \
  -v "$REPO_ROOT":/work -w /work/Linux \
  -e GRADLE_USER_HOME=/work/.gradle-cache-linux \
  ubuntu:22.04 bash -c '
    set -e
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -qq
    apt-get install -y -qq \
      openjdk-17-jdk openjdk-21-jdk-headless \
      gcc binutils imagemagick fakeroot rpm \
      > /tmp/apt.log 2>&1 || (cat /tmp/apt.log; exit 1)

    ./gradlew --no-daemon clean packageDeb packageRpm packageAppImage

    # The :packageAppImage target produces a directory rather than a single
    # .AppImage file (Compose Desktop quirk). Tarball it for distribution.
    cd build/compose/binaries/main/app
    tar -czf RCForb-linux-1.0.0.tar.gz RCForb/

    echo "---ARTIFACTS---"
    find /work/Linux/build/compose/binaries -maxdepth 5 -type f \
      \( -name "*.deb" -o -name "*.rpm" -o -name "*.tar.gz" \) -exec ls -lh {} \;
  '

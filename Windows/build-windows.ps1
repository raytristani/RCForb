# Build Windows release artifacts on a Windows host.
#
# Output:
#   Windows/build/compose/binaries/main/msi/RCForb-1.0.0.msi
#   Windows/build/compose/binaries/main/exe/RCForb-1.0.0.exe
#
# Prerequisites:
#   - JDK 17+ (Adoptium Temurin recommended) on PATH
#   - MSYS2 with MinGW64 toolchain (mingw-w64-x86_64-gcc), launched from
#     the MINGW64 shell, OR `gcc` on PATH from any MinGW distribution
#   - ImageMagick (`magick.exe`) on PATH for .ico generation, OR a
#     pre-existing icon/AppIcon.ico
#   - WiX Toolset 3.x is auto-installed by jpackage on first run
#
# Cross-builders (Linux/macOS) should use the GitHub Actions workflow
# instead — see .github/workflows/release-windows.yml.
#
# Run from the repo root:  pwsh ./Windows/build-windows.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $repoRoot "Windows")

# Generate .ico from .png if missing.
if (-not (Test-Path "icon/AppIcon.ico")) {
    if (-not (Get-Command magick -ErrorAction SilentlyContinue)) {
        throw "icon/AppIcon.ico is missing and ImageMagick (magick.exe) was not found on PATH. Install ImageMagick or pre-generate the .ico."
    }
    magick convert icon/AppIcon.png -define "icon:auto-resize=256,128,64,48,32,16" icon/AppIcon.ico
}

./gradlew --no-daemon clean packageMsi packageExe

# Surface the artifacts at predictable names for upload.
$msi = Get-ChildItem -Recurse build/compose/binaries/main/msi -Filter *.msi | Select-Object -First 1
$exe = Get-ChildItem -Recurse build/compose/binaries/main/exe -Filter *.exe | Select-Object -First 1
if ($msi) { Copy-Item $msi.FullName "build/RCForb-windows-1.0.0.msi" -Force }
if ($exe) { Copy-Item $exe.FullName "build/RCForb-windows-1.0.0.exe" -Force }

Write-Host "---ARTIFACTS---"
Get-ChildItem build/RCForb-windows-1.0.0.* | Format-Table Name, Length

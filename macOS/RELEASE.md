# Release process — GitFlow

This repo uses GitFlow with three long-lived branches and per-release branches.

## Branches

| Branch | Purpose | Auto-deploys? |
|---|---|---|
| `main` | Production. Every commit is a tagged release. | Tagged DMGs uploaded to GitHub Releases. |
| `develop` | Integration. Default branch. All feature work merges here. | Nothing — local builds only. |
| `release/x.y.z` | Release prep. Branched off `develop` when starting a release. Bug fixes only. Merged to `main` (and back to `develop`) on release. | Build + sign + notarize + DMG runs from this branch. |
| `feature/<name>` | Branched off `develop`, merged back via PR. | n/a |
| `hotfix/x.y.z` | Branched off `main` for urgent prod fixes. Merged to `main` and `develop`. | Tagged DMG. |

## Cutting a release

```bash
# 1. Branch from develop
git checkout develop
git pull
git checkout -b release/1.0.0

# 2. Bump versions in build.gradle.kts (`packageVersion = "1.0.0"`,
#    same for `pkgVersion` in macSafeDmg).
git commit -am "Bump version to 1.0.0"

# 3. Build the signed + notarized DMG. Requires:
#    - Apple Developer ID Application certificate installed
#    - notarytool keychain profile saved (one-time setup, see below)
export RCFORB_SIGN_IDENTITY="Developer ID Application: Ramon E Tristani (TEAMID)"
export RCFORB_NOTARY_PROFILE="rcforb-notary"
./gradlew macSafeDmg

# 4. Smoke-test the DMG locally.
open build/compose/binaries/main/dmg/RCForb-1.0.0.dmg

# 5. Push the release branch and open a PR into main.
git push -u origin release/1.0.0
gh pr create --base main --head release/1.0.0 --title "Release 1.0.0" --body "..."

# 6. Merge → tag → publish.
gh pr merge --squash
git checkout main && git pull
git tag -a v1.0.0 -m "v1.0.0"
git push origin v1.0.0
gh release create v1.0.0 build/compose/binaries/main/dmg/RCForb-1.0.0.dmg \
  --title "RCForb 1.0.0" \
  --notes "See CHANGELOG.md for details"

# 7. Merge release branch back into develop.
git checkout develop
git merge --no-ff release/1.0.0
git push
git branch -d release/1.0.0
git push origin --delete release/1.0.0
```

## One-time notary setup

```bash
# Use an app-specific password from appleid.apple.com.
xcrun notarytool store-credentials "rcforb-notary" \
  --apple-id raytristani@gmail.com \
  --team-id <YOUR_TEAM_ID> \
  --password <APP_SPECIFIC_PASSWORD>
```

After this, `RCFORB_NOTARY_PROFILE=rcforb-notary` is enough — the build will
submit, wait, and staple automatically.

## Hotfix flow

```bash
git checkout main
git checkout -b hotfix/1.0.1
# ... fix, bump version ...
./gradlew macSafeDmg          # signed + notarized
# PR → main → tag v1.0.1 → release. Then merge into develop.
```

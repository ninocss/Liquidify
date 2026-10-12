# Building Liquidify 1.0.0 Beta

## Manager

The Android project lives in `radiant-base/Manager`. Use JDK 21, Android SDK 37
and build-tools 37.0.0. Configure `local.properties` locally, then run its wrapper:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The display name is Liquidify Manager; the existing internal package
`dev.nino.squareglass.manager` remains for compatibility. Release version and
versionCode are checked against `release.json`. The bundled manifest is copied
from `patches/manifest.json`, never from the unmodified upstream-only manifest.

For release signing set `RELEASE=true`, `SIGNING_STORE_FILE`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD`.
Release builds fail if signing credentials are absent. Never commit these values.

## Patch regeneration

The checked-in `patches/patches.zip` is the prepared installable payload. It is
validated by `python tools/validate_release.py` before every release. Upstream
Radiant patch specs and files are preserved unchanged; Liquidify entries declare
`source: liquidify`. Compatibility declarations must come from actual tested
metadata, not from blanket relabeling of upstream patches.

To regenerate the payload, use Windows, Python 3.12 with fonttools, JDK 21,
Android SDK 36 and build-tools 37.0.0. The local scripts use Android Studio's
JBR and SDK paths. Download the exact stock APK in `upstream-lock.json` to
`research/tidal-stock-2.200.0.apk` and verify its SHA-256. Decode it with apktool
3.0.3 into `research/tidal`. Obtain the pinned Square font at
`square-reference/app/src/main/res/font/inter.ttf`; the revision and OFL
are recorded in the lock file and licenses. No nested Git repository is required.
Upstream Radiant patches are vendored in `radiant-base/patches`.

```sh
python tools/build_test_apk.py
python tools/package_alpha.py
python tools/verify_strict_patches.py
python -m unittest discover -s tests -v
```

The historical packaging script name is retained, but emits
`dist/Liquidify-1.0.0-beta-patches.zip`. Copy that file to `patches/patches.zip`
and its `manifest.json` to `patches/manifest.json`; run the release validator.
`host-stubs` contains compile-only native ABI declarations and must not ship.
Patch builds reuse the host Compose/Kotlin runtime. TIDAL binaries, keys and
private LAN/account evidence stay in ignored local directories.

## GitHub release

The single root workflow `.github/workflows/release.yml` runs on a matching
version tag or manual dispatch. It builds and tests Manager from source using
JDK 21, checks the prepared patch payload, verifies APK signing, then uploads
APK, patches, metadata and SHA256SUMS to a draft before publishing it.
GitHub automatically offers the matching repository source archives.

Required Actions secrets: `LIQUIDIFY_SIGNING_KEYSTORE` (base64 JKS),
`LIQUIDIFY_SIGNING_PASSWORD`, and `LIQUIDIFY_SIGNING_ALIAS`.
The same signing key must be retained for future Manager updates. Configure these secrets on `ninocss/liquidify` before running the release workflow; private material is excluded from Git. The normal Build workflow needs no release signing secrets and retains a debug Manager APK and the prepared patches as artifacts.

# Android build and release

Supported tools are Android Studio, the Gradle wrapper and GitHub Actions.
The wrapper pins Gradle 9.9.1 with an official SHA-256. CI uses Temurin
21.0.13+9, JVM target 17, compile API 37.2, target 37 and build-tools 37.0.0.
Minimum SDK remains 24. Exact library/tool versions are in the version catalog.
Use Kotlin application sources and Ktor CIO; do not add Java/OkHttp engines.

## Verification

Use CI for builds/tests/lint to preserve the owner's local data budget:

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:lintRelease
./gradlew --no-daemon :app:assembleRelease
```

Release builds require signing secrets below. CI rejects lint warnings and
checks package, version, signing certificate and absence of debuggability.
The wrapper fallback fetches the same official distribution and verifies the
same pinned checksum. No generator environment or API key is needed.

## Signing continuity

Application ID: `com.aistudio.haven.xvykpt` (preserve for installed updates).
SHA-1: `86:28:69:97:15:15:73:65:3B:04:56:ED:AD:7A:2D:56:1D:7A:AE:59`
SHA-256: `B4:D1:A5:EC:00:68:C6:3D:55:49:E6:22:3D:86:D9:0D:C0:37:7D:C5:6B:C7:2D:34:FA:63:F9:38:BD:2D:B6:1E`

Keep both existing signing configurations and never regenerate keys to repair
missing credentials. Register the package and certificate SHA-1 with the Google
Android OAuth client. CI verification does not prove device Google sign-in.

CI secrets: `HAVEN_TEST_KEYSTORE_BASE64`, `HAVEN_RELEASE_KEYSTORE_BASE64` and
`HAVEN_RELEASE_KEYSTORE_PASSWORD`. Release alias: `upload`. CI supplies
`KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD`. Private owner backups
stay ignored under `.signing`; never print or commit their contents.

## Release delivery

PR/main workflows build and upload verification reports and APK candidates.
Forks use an ephemeral debug signer and cannot produce distributable releases.
Publishing a GitHub release triggers the automated signed APK build and upload.
For v0.0.5 the workflow expects code 15 and emits `haven-v0.0.5.apk`,
`SHA256SUMS`, `manifest.txt`, `apk-provenance.txt` and `update.json`.
Metadata is derived from the actual built APK. Do not upload substitute assets.
Check delivered source, package/version, signer and checksum before declaring
delivery complete. Future releases must coordinate app version and workflow pins.

Owner explicitly authorized v0.0.5 publication on 2026-11-10, with device findings
to follow. This changes this release's publication hold; it does not mark device
acceptance passed. Track testing in [the release checklist](RELEASE_V0.0.5.md).

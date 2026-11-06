# Android testing APK build handoff

The build recipe lives here so it does not need to be reconstructed from chat.
AI Studio parity is still unconfirmed; its requested report is listed below.

## Source, identity and pinned inputs

- Repository: Phastboy/haven-app; release work: `fix/v0.0.0-release-readiness`, PR #17.
- Application ID: `com.aistudio.haven.xvykpt`; namespace: `com.example`.
- Candidate versionName/code: `0.0.0` / `9`. Confirm no higher code was distributed.
- Variant: `debug`, testing only. Not Play/store release signing.
- CI JDK: Temurin 17.0.21+9. Java source/target compatibility: 11.
- Gradle wrapper: 9.3.1, distribution checksum pinned in
  `gradle/wrapper/gradle-wrapper.properties`.
- AGP 9.1.1; Kotlin Compose plugin 2.2.10; KSP 2.3.5; Room 2.7.0.
  Dependency versions/BOMs are in `gradle/libs.versions.toml`.
- Compile SDK: android-36.1; target SDK 36; min SDK 24; build-tools 36.0.0.
- CI workflow: `.github/workflows/android-apk.yml`.
- Verification command:
  `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Backend: `https://haven.apps.mistral.run`; deployed API v0.8.65.
- Native Google Web client ID is configured in `HavenAuthClient`; no browser,
  email, PKCE, App Link, or Firebase Auth fallback is supported.
- No Gemini secret is required for Haven's current journeys. Never package
  backend/Cloudinary credentials in the APK.

Use CI for Gradle builds/tests/lint to preserve the owner's local data budget.
Do not change versions or signing keys to work around a build failure.

## Persistent testing signer

CI restores `HAVEN_TEST_KEYSTORE_BASE64` from repository Actions secrets. The
local private backup is `.signing/haven-testing.p12` (ignored by Git, directory
0700/file 0600). Keep an additional secure owner-managed backup before relying
on this identity long term. Never commit/upload it as a public artifact or paste
it in chat. This is a testing key, separate from future production signing.

Public fingerprints for this CI/testing signer:

- SHA-1: `86:28:69:97:15:15:73:65:3B:04:56:ED:AD:7A:2D:56:1D:7A:AE:59`
- SHA-256: `B4:D1:A5:EC:00:68:C6:3D:55:49:E6:22:3D:86:D9:0D:C0:37:7D:C5:6B:C7:2D:34:FA:63:F9:38:BD:2D:B6:1E`

Register an Android OAuth client using the application ID and the SHA-1 above
in the project belonging to the configured Web client ID. The owner has
confirmed simulator sign-in on AI Studio's different signer, not this signer.
An AI Studio-signed installation requires a fresh installation to use this
APK unless its signing identity can be recovered. Uninstalling erases local
pending messages/settings; complete or preserve pending work first.

A missing CI secret fails normal builds rather than silently replacing the
key. Fork PRs use an ephemeral verification-only signer; those artifacts are
not update-compatible with the persistent testing APK. CI checks the expected
certificate SHA-256 for normal builds.

## Artifact and evidence

PR #17 checks link to the build, test and lint reports. A successful run uploads
`haven-android-debug-apk`, containing `app-debug.apk`, `apk-provenance.txt`, and
`apk-sha256.txt`. Provenance records the checked-out commit, workflow run, actual
manifest/package/version, signing certificate, and Gradle/JDK details. PR builds
may use a GitHub merge commit; use the recorded commit, not an assumed branch SHA.

Only use an artifact from the latest reviewed commit with passing checks.
Green CI does not establish OAuth configuration, live production acceptance,
visual quality, physical-device behavior or launch approval. No tag/public
release is created by these changes.

## AI Studio report still required

Record source commit, exact Gradle command/variant, Gradle/JDK/SDK/toolchain
versions, application ID/version, certificate SHA-1/SHA-256, build configuration
input names/differences (no secret values), and final APK SHA-256. Provide the
APK/build log if export is supported. State whether the original signing
keystore is exportable through a secure channel. Do not regenerate it. AI Studio
cannot sign our builds for us; fingerprints alone cannot reproduce a private key.

Pin the confirmed recipe in repository files and update this document when an
intentional change is made. Do not assume byte-for-byte reproducibility merely
because source and variant match; transitive dependency locking is not yet set up.

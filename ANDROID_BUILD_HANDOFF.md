# Haven Android build and release handoff — current review

Current corrective PR: #24, based on main `2621d00`. Release target is
0.0.2 / versionCode 12. The owner authorized resolving reviews, merging and
publishing this patch. Published 0.0.1 assets were attached by CI/CD. Preserve both signing configurations and
existing certificate checks. AI Studio reports are historical; inspect exact CI.

## Source and pinned recipe

- Temurin 21.0.13+9 in CI; Kotlin-only application sources, JVM target 17.
- Gradle 9.9.0 with official distribution SHA-256 in wrapper properties.
  CI prefers the wrapper; if its CDN fails, prepare-gradle.sh fetches the same
  archive through Gradle's official GitHub release asset API and verifies that
  pinned checksum before running it. Wrapper settings and app source stay clean.
- AGP 9.5.1, Kotlin/Compose compiler 2.4.21, KSP 2.3.13, Room 2.8.5.
- Compile API 37.2 / target 37, SDK build-tools 37.0.0, min SDK 24 retained.
- Compose BOM 2026.09.00, Robolectric 4.17, Ktor 3.6.1 **CIO** and Coil 3.6.3
  Ktor image networking. Active dependencies are pinned in the version catalog.
- CI uses current stable checkout/setup-java/setup-gradle/upload actions, builds
  both variants, runs regressions on API 35/37 and rejects lint warnings.
- CI checks the resolved runtime for unwanted Retrofit/OkHttp/Java HTTP engines.
  Its runtime-dependencies.txt is included in verification reports.
- Kotlin-only means our app and test source files, not third-party implementation
  languages. AndroidX Test uses matching alpha03 test releases (Espresso 3.8.0,
  core/runner 1.8.0, ext JUnit 1.4.0): published Espresso 3.7.0 sources reflect
  `InputManager.getInstance()`, which fails on API 37. The newer implementation
  obtains the input service from Context. These are test-only dependencies.

Use CI to preserve the owner's local data budget:

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew --no-daemon :app:assembleRelease :app:lintRelease
```

The second command requires the unchanged release-signing environment below.
Google sign-in remains Credential Manager plus the Haven API; no Firebase,
Google-services JSON, Gemini key or backend/Cloudinary secret is required. The
runtime auth API remains server-confirmed, rotating and account-guarded.

## Owned signing identity and continuity

The owner authorized independent signing after AI Studio's original private key
was not exported. Our initial direct-download release intentionally retains the
owned persistent certificate already used for the testing APK, preserving OAuth
registration and update continuity. Treat this identity as the distribution key
for future APK updates. It differs from AI Studio's certificate.

Package: com.aistudio.haven.xvykpt
SHA-1: 86:28:69:97:15:15:73:65:3B:04:56:ED:AD:7A:2D:56:1D:7A:AE:59
SHA-256: B4:D1:A5:EC:00:68:C6:3D:55:49:E6:22:3D:86:D9:0D:C0:37:7D:C5:6B:C7:2D:34:FA:63:F9:38:BD:2D:B6:1E

Register the Android OAuth client in the same project as the configured Web
client. The owner is configuring this SHA-1; successful sign-in is the final
publication gate. AI Studio builds use a different signer and require a fresh
installation; preserve pending work before uninstalling them.

CI secrets: HAVEN_TEST_KEYSTORE_BASE64 for debug;
HAVEN_RELEASE_KEYSTORE_BASE64 and HAVEN_RELEASE_KEYSTORE_PASSWORD for release.
Release alias is upload. CI supplies KEYSTORE_PATH to a private runner-temporary
PKCS12 and passes STORE_PASSWORD / KEY_PASSWORD from the protected secret.
The release container uses a random password; the certificate/private key remain
identical to our earlier testing identity. Neither container nor password is
tracked in Git. Local backups (ignored .signing directory, restricted permissions):
haven-testing.p12, haven-release.p12 and release-password. Keep secure independent
owner-managed backups. Never print/commit keys or passwords, or regenerate the
identity to work around missing CI secrets. The certificate's Android Debug DN
is historical; release debuggability is controlled and checked in the APK manifest.

## Artifacts, verification and publication

.github/workflows/android-apk.yml builds/tests/lints on PR/main. Same-repository
builds verify the expected release signer, package, code/name and absence of the
application-debuggable flag. Forks perform debug-only verification with an
ephemeral signer and cannot produce the distributable release artifact.

Successful runs upload haven-android-release-apk containing haven-v0.0.2.apk,
SHA256SUMS, manifest.txt and apk-provenance.txt. Provenance records actual source
commit, variant, workflow run and toolchain. PR builds may use a temporary merge
commit; final distribution must reference the verified main/tag commit.

The published-release event builds and attaches the release APK and evidence to
the public GitHub release. Draft creation does not invoke that publication job.
The owner authorized merge and publication after review resolution and passing CI.
Use the published-release workflow to attach assets automatically; do not manually
upload APKs or metadata. Device findings will be tracked in separate issues.

CI is not physical-device or authenticated production acceptance. Remaining
journey, legacy unknown-owner draft recovery, visual and physical-device gates
are tracked in RELEASE_V0.0.0.md. Do not mark the entire launch checklist done.

# Haven Android build and release handoff — 2026-11-07

This is the authoritative build recipe. The AI Studio report is preserved as
historical evidence in AI_STUDIO_BUILD_EVIDENCE.md; its disputed source/client-ID/
keystore/production-secret assertions are explicitly annotated there.

## Source and pinned recipe

Repository: Phastboy/haven-app. UI patch: PR #18, following release integration
PR #17. Candidate is versionName 0.0.1 / versionCode 11, increasing from 0.0.0 / 10.
Application ID: com.aistudio.haven.xvykpt. Namespace: com.example.

- Temurin JDK 17.0.21+9 in CI; Java source/target compatibility 11.
- Gradle wrapper 9.3.1 with distribution checksum in gradle-wrapper.properties.
- AGP 9.1.1; Kotlin Compose plugin 2.2.10; KSP 2.3.5; Room 2.7.0.
- Compile platform android-36.1; build-tools 36.0.0; min SDK 24; target SDK 36.
- Direct dependencies/BOMs are pinned in gradle/libs.versions.toml. Transitive
  dependency locking is not configured; byte-identical rebuilding is not claimed.
- AI Studio reports JDK 21.0.13+9; our independent CI uses the measured/pinned JDK
  above. Its emulator staging wrapper is not needed outside AI Studio.

Use CI for builds/tests/lint to preserve the owner's local data budget:

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew --no-daemon :app:assembleRelease :app:lintRelease
```

The second command requires the release signing environment described below.
Release is non-debuggable; minification is disabled. No product flavors exist.
Backend is https://haven.apps.mistral.run, API v0.8.65. Native auth uses Credential
Manager and HavenAuthClient.GOOGLE_WEB_CLIENT_ID (1064336033731 project prefix).
Browser/email/PKCE/App Link fallback is retired. No Gemini or backend/Cloudinary
secret is required or permitted in the APK. Secrets-plugin example defaults
support building without a private .env; Google-services missing-file handling
is configured in source. Never add dummy production credentials to make CI pass.

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

Successful runs upload haven-android-release-apk containing haven-v0.0.1.apk,
SHA256SUMS, manifest.txt and apk-provenance.txt. Provenance records actual source
commit, variant, workflow run and toolchain. PR builds may use a temporary merge
commit; final distribution must reference the verified main/tag commit.

The published-release event builds and attaches the release APK and evidence to
the public GitHub release. Draft creation does not invoke that publication job.
The owner authorized merge/release preparation and requires final Google sign-in
confirmation before public publication. Hold the release as a draft until then.

CI is not physical-device or authenticated production acceptance. Remaining
journey, legacy unknown-owner draft recovery, visual and physical-device gates
are tracked in RELEASE_V0.0.0.md. Do not mark the entire launch checklist done.

# Android v0.0.0 release preparation

Status: NOT release-ready. Branch: `fix/v0.0.0-release-readiness`.
Baseline: main `3191ee27386baedc6dc6c709e6e09b65a8688e8c`.
Backend: deployed v0.8.65. Android and backend versions are independent.

## APK identity and build parity

Application ID remains `com.aistudio.haven.xvykpt`; versionName is `0.0.0`.
Candidate versionCode is 9 (previous source declared 8). Before distribution,
confirm AI Studio has not distributed a higher code; choose a greater code if so.
Do not reset the version code to zero.

Ask AI Studio for this report, without uploading private keys or passwords:

> Report the source commit used for your latest APK; exact Gradle command and
> variant; `./gradlew --version`; installed compile SDK and build-tools versions;
> application ID, version name and code; signing certificate SHA-256; names of
> build-time configuration inputs and which differ from repository defaults
> (redact secret values); and the APK SHA-256. State whether you can sign the
> final APK with the same key used for the working Google sign-in build.
> Do not modify code or regenerate signing keys while collecting this report.

A baseline APK and its build log allow inspection of actual manifest, signer,
version and hash. Matching source/config/toolchain/variant produces comparable
behavior; byte-for-byte equality is not assumed. Updating an installed APK
requires the same application ID and signing identity plus a suitable version
code. Each signer needs the matching Google Android OAuth registration.
Keep the existing AI Studio signing configuration intact. Arrange any release
key through secure secret storage or have AI Studio sign the final artifact;
never put private keys/passwords into chat, Git, or build logs.

Current CI: Temurin JDK 17; Gradle wrapper 9.3.1; SDK android-36.1;
build-tools 36.0.0; `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
CI generates a debug key if absent; its APK is testing-only and does not prove
AI Studio signer parity, Google sign-in, or production release signing.

## Confirmed blocker addressed on this branch

- Room changed `feed_offers` without increasing version 3. Version 4 now
  migrates both historical version-3 schemas (with/without `location`), including
  the version-2 chain. Destructive fallback is removed to protect messages and
  outbox data. Tests reconstruct old schemas and exercise Room validation and
  pending-outbox preservation. CI execution remains pending.
- Unsupported database versions fail rather than silently erase user data.
  Historical version-1 schema/migration evidence is still needed if such APKs
  were distributed; do not infer support from a clean installation test.

## Required audit before release approval

These remain open; newly added UI and a successful compile are not acceptance.

- [ ] Isolate saved state and distance by account AND reference context. Query
  keys alone are insufficient while globally shared offer rows contain both.
  Capture request identity before suspension; reject stale results after switch.
- [ ] Prove account changes cannot expose or send another person's cached
  messages/outbox; verify pending/acknowledged message reconciliation under races.
- [ ] Bound SSE reconnection/auth retries; close streams on leaving/background/
  sign-out/account switch; prevent overlapping resyncs and polling jobs.
- [ ] Follow/unfollow: pending guard, server-confirmed state, visible failure,
  cancellation and profile/account-change protection.
- [ ] Audit profile/avatar edits, deletion, offer attachments, unread status,
  collection pagination, explicit proximity on/change/off and expired references.
- [ ] Verify create/edit/photos/location and partial-failure recovery against
  current contracts; never replace errors with simulated success.
- [ ] CI build, unit tests and lint must pass on the exact release commit.
- [ ] Inspect final APK version, signer, backend configuration and permissions;
  record source commit, build run, variant and artifact SHA-256.
- [ ] Verify signed Google sign-in/session reopening on the intended APK.
- [ ] Physical Android and authenticated production acceptance remain separate,
  including keyboard/back navigation, older-message scrolling and genuine offers.

No release/tag/publication is authorized by completing this preparation document.

# Android v0.0.0 release preparation

Status: NOT release-ready. Branch: `fix/v0.0.0-release-readiness`.
Baseline: main `3191ee27386baedc6dc6c709e6e09b65a8688e8c`.
Backend: deployed v0.8.65. Android and backend versions are independent.

## Historical build context

This is an archived preparation record, not current operating instructions.
The obsolete generator build/signing handoff has been removed by owner request.
Use [the supported build instructions](ANDROID_BUILD_HANDOFF.md) for current
SDK, toolchain, signing identity and delivery. Historical findings below retain
their original evidence scope; they do not establish current acceptance.

## Confirmed blocker addressed on this branch

- Room changed `feed_offers` without increasing version 3. Version 4 now
  migrates both historical version-3 schemas (with/without `location`), including
  the version-2 chain. Destructive fallback is removed to protect messages and
  outbox data. Tests reconstruct old schemas and exercise Room validation and
  pending-outbox preservation. Use PR #17 checks for execution evidence.
- Unsupported database versions fail rather than silently erase user data.
  Historical version-1 schema/migration evidence is still needed if such APKs
  were distributed; do not infer support from a clean installation test.

## Required audit before release approval

Checked entries indicate implemented repairs. Unchecked entries remain gates;
new UI and successful compilation are not acceptance.

- [x] Implement isolation of saved state and distance by account AND reference context. Per-query snapshots hold contextual
  values; public offer rows exclude them. Capture request identity before
  suspension and reject stale responses.
- [x] Add account guards and regression coverage so account changes cannot expose or send another person's cached
  messages/outbox; verify pending/acknowledged message reconciliation under races.
- [x] Bound SSE reconnection/auth retries; close streams on leaving/background/
  sign-out/account switch; prevent overlapping resyncs and polling jobs.
- [x] Follow/unfollow implementation: pending guard, server-confirmed state, visible failure,
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

The owner authorized merge and release preparation on 2026-11-07. Public
publication is held until the owner confirms Google sign-in after configuration.

## Testing build handoff

See `ANDROID_BUILD_HANDOFF.md` for the pinned build recipe, persistent testing
signer fingerprints, OAuth registration action, secure backup location and APK
provenance. This checklist marks implementation separately from CI/device/live
acceptance; the overall release remains unapproved.

Legacy outbox rows with no proven sender identity remain preserved but quarantined
from automatic sending. Shared conversation ownership cannot safely identify the
sender of an old `me` draft. Review/recovery of such pre-existing pending drafts
is an open upgrade acceptance item; never relabel them as another account's sends.

## Release preparation — 2026-11-07

Historical source update `8fbf547` is integrated; the source history retains the original integration context. CI builds both
variants, runs 94 debug unit/Robolectric tests and both lints, verifies release
package/version/certificate and rejects a debuggable release APK. Candidate
versionName/code: 0.0.0 / 10, increasing from distributed testing code 9.

The initial direct-download release retains our owned persistent certificate
(SHA-1 86:28:69:97:15:15:73:65:3B:04:56:ED:AD:7A:2D:56:1D:7A:AE:59), so Google
OAuth registration and installation continuity remain valid. This deliberately
establishes that certificate as the initial distribution identity; it must be
preserved for future updates. This identity must remain stable. The release
keystore uses alias upload and a random password held in Actions secrets
HAVEN_RELEASE_KEYSTORE_BASE64 / HAVEN_RELEASE_KEYSTORE_PASSWORD. Ignored local
backups are .signing/haven-release.p12 and .signing/release-password; keep secure
owner-managed backups. Neither keystore nor password is committed.

Successful CI uploads haven-android-release-apk with haven-v0.0.0.apk, SHA256SUMS,
manifest.txt and apk-provenance.txt. Publishing v0.0.0 subsequently builds and
attaches verified release assets automatically. Final publication is held until
the owner confirms Google sign-in after OAuth configuration. A draft release
is not public launch acceptance; physical/live journey gates remain separate.

Fork PRs run debug-only verification and do not build, validate or upload the
signed release APK. Same-repository/main runs additionally require release signing
secrets and run :app:assembleRelease :app:lintRelease; see ANDROID_BUILD_HANDOFF.md.

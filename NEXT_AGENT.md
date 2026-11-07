# Current review — AI Studio swipe navigation and lint cleanup

Owner requested review/corrective PR(s) for issues #21 and #19 after AI Studio's
main commit `2621d00`. Work branch: `fix/review-swipe-and-lint`. Do not merge,
tag or publish without the owner's instruction. Keep Android 0.0.1 / code 11.
Owner now requires full lint resolution, including dependency/toolchain/SDK
updates. Use Ktor CIO throughout networking and Kotlin-only application sources;
no Retrofit/OkHttp/Java HTTP engines. Page swipes are interior gestures and must
not consume Android edge-back. Earlier deferred-upgrade guidance is superseded.

Read [ANDROID_SWIPE_LINT_REVIEW.md](ANDROID_SWIPE_LINT_REVIEW.md) for findings,
corrections and warning accounting. Verify exact PR CI before claiming a pass.
Android builds/tests/lints run in CI to preserve the local data budget.
Samsung touch/edge-back/launcher acceptance remains separate.

# Start here — Android v0.0.1 released; warning cleanup next

Android PR #18 was squash-merged at `02262bc77b3b2252d0c8e958ce5c92bf47e09ae5`.
Release `v0.0.1` is published, versionCode `11`. CI/CD has attached the signed
APK, SHA256SUMS, manifest.txt and apk-provenance.txt. Do not manually rebuild or
upload release assets. Backend releases and versions remain independent.

## Current task for AI Studio / the next agent

Read [AI_STUDIO_WARNINGS_HANDOFF.md](AI_STUDIO_WARNINGS_HANDOFF.md). It contains
the complete instructions, warning counts and all 65 baseline locations/messages;
fetching GitHub issue #19 is not required. Check current main/CI before changes.

Baseline [CI 42379480988](https://github.com/Phastboy/haven-app/actions/runs/42379480988)
passed all 115 tests, debug/release builds and both lints. Each lint variant has
65 warnings and zero errors. Do not double-count them as 130 distinct defects.
The owner's next authorized work is a separate warning-cleanup PR, with no
blanket suppression or unverified bulk toolchain upgrades. Run Android checks in
CI to preserve the local data budget. No further merge/publication is authorized
by this handoff alone. Samsung/device and production acceptance remain separate.

The UI patch preserves exactly three offer-card text rows, price priority,
explicit-only distance and matching filter/proximity/create controls. Decorative
outlines/shadows must not return. Preserve authentication, cache/outbox isolation,
signing identity and existing product scope. No new backend or product features.

## Prior release context (historical; superseded where stated above)

# Start here — v0.0.0 release preparation, 2026-11-06

Work branch: `fix/v0.0.0-release-readiness`; PR #17. Baseline main:
`3191ee27386baedc6dc6c709e6e09b65a8688e8c` (AI Studio update already pulled).
Read `RELEASE_V0.0.0.md` and `ANDROID_BUILD_HANDOFF.md` first. Check the latest
PR commit/checks before continuing; earlier green runs are not final evidence.

## Implemented and under verification

Native feed/detail/profile/collections, Google sign-in, create/edit/photo/location,
inquiries/threads and older-history pagination were already integrated. The
latest AI Studio update also implements follow/unfollow, profile/avatar editing,
owned-offer deletion, discovery scope/reference controls, unread and SSE.
PRs #15/#16 are merged; do not recreate them or treat these paths as scaffolds.

This branch repairs both shipped Room v3 schemas without destructive resets,
isolates cached saved/distance context, rejects stale feed responses, refreshes
price filters, and exposes paging errors. Message outbox entries have persisted
ownership; private cache reads and sends are guarded against account changes.
Remote thread merges preserve acknowledged delivery; enqueue/outbox persistence
is atomic. Live thread streams and fallback polling run only while resumed and
stop on leaving/background. Follow writes use confirmed responses. Deletion
waits for server confirmation; avatar reads run off the UI thread with a 5 MB cap.
Regression tests and CI APK provenance accompany these changes. Inspect actual
checks before claiming they passed.

## Signing and remaining acceptance

Candidate versionName/code: 0.0.0 / 10. CI has a persistent testing signer and
verifies its fingerprint; the private backup is ignored by Git. Register this
signer's SHA-1 for native Google OAuth before testing sign-in. AI Studio signer
parity is unresolved until its quota resets and its build/export report arrives.
Never replace or commit signing keys. No publication/tag/merge is requested.

Owner confirmed native Google sign-in on the simulator using AI Studio's signer.
Physical-device testing is scheduled later. Full authenticated live journeys,
visual/prototype acceptance, genuine provider offers, Cloudinary production
preset resizing/metadata removal, and reliability/recovery proof remain open.
The release checklist distinguishes implementation from acceptance.

Backend v0.8.65 is deployed (release run 42182772275, actual Koyeb service update).
Contracts: `MOBILE_NATIVE_API_SWEEP.md`, `MOBILE_OFFER_LIFECYCLE.md`,
`MOBILE_NATIVE_PAGES.md`, `MOBILE_NATIVE_API_BACKLOG.md`. Backend is authoritative.
Native auth uses Credential Manager → `/api/v1/auth/google`, rotating refresh
credentials, and `/api/v1/me`. Browser PKCE/App Link/email fallback is retired.
Do not reinstate it based on an old AI Studio report or historical docs.

Scope remains implemented Haven v0.1–v0.8 parity. Backend roadmap/journeys and
freeze decisions govern. No payments, commitments, ratings, push, fabricated
state, GPS inference or proximity exclusion. General discovery is default;
proximity is explicit apply/change/off, separate from the offer's public location.
Use Haven theme tokens and prototypes; keep runtime samples out of integrated UI.
Run Android build/tests/lint in CI, preserving the owner's local data budget.

## Release preparation — 2026-11-07

AI Studio main update `8fbf547` is integrated; its original report is preserved
in `AI_STUDIO_BUILD_EVIDENCE.md` with disputed claims identified. CI builds both
variants, runs 94 debug unit/Robolectric tests and both lints, verifies release
package/version/certificate and rejects a debuggable release APK. Candidate
versionName/code: 0.0.0 / 10, increasing from distributed testing code 9.

The initial direct-download release retains our owned persistent certificate
(SHA-1 86:28:69:97:15:15:73:65:3B:04:56:ED:AD:7A:2D:56:1D:7A:AE:59), so Google
OAuth registration and installation continuity remain valid. This deliberately
establishes that certificate as the initial distribution identity; it must be
preserved for future updates. It is not the AI Studio certificate. The release
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

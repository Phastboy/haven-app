# PR #38 CI correction — 2026-11-10

Run 42866164700 at 3ad7268 failed debug/release lint on VectorRaster
(288dp intrinsic logo) and UseKtx (Uri.parse). Corrected the vector's intrinsic
size to 192dp; explicit 288dp presentation bounds and viewport/path stay intact.
Use String.toUri for the installation-settings URI. No lint suppression or
baseline added. Reports retained at /tmp/haven38-failed-reports and log at
/tmp/haven38-failed.log. Fresh exact-head CI is required; no polling. Existing
device/theme/update acceptance and 2026-11-11 maintainer review remain open.

# Startup, GitHub updates and feed pagination — 2026-11-10

Owner explicitly approved these changes. Branch `fix/startup-updates-feed`,
based on main `1d37dcc`, candidate 0.0.5/code 15. Owner completed workflow
authorization on 2026-11-10; release-update.patch has now been applied to the
workflow and app version. Fresh exact-head CI remains required.
Branded restoration keeps
retry on failure; vector logo converted from owner-provided SVG. Android 12+
persists the chosen app night mode for subsequent system splash windows.
Legacy system starting windows can appear before app preference code runs;
verify API 24–30 separately rather than claiming forced-theme launch parity.
Feed removes loaded count, prefetches near the bottom and shows two skeletons
for pagination, retaining cards/cursor on failure. GitHub update checks use
public stable-release metadata and never carry Haven bearer credentials.
Download checks size, checksum, package, version and identical signer sets;
Android installer remains authoritative and requires person confirmation.
The first APK containing this updater must still be installed manually.
The workflow publishes update.json derived from the actual APK alongside existing
provenance, checksums and manifest. No existing assets are manually modified.

Static XML parsing, workflow shell syntax and whitespace checks PASS locally.
Android build/test/debug+release lint and rendered tests must run in CI;
no local Gradle downloads per repository data-budget instructions. Do not
claim CI or device acceptance before actual evidence. Acting maintainer/agent
owns 2026-11-11 check review before readiness: verify exact-head CI/review,
inspect retained screenshots, test cold launch across app/OS themes, offline
restoration/retry, near-bottom paging/failure/end, and signed old→new APK install
including permission denial, installer cancellation, background interruption
and checksum/signer/version rejection. Requires device, matching signed APKs
and a newer stable GitHub release; none is established by source alone.
No release/tag/merge is performed by this implementation task.

# Current coordinated release preflight — PR #35

Owner authorized merge/release of Android #35 with backend #73 when their gates
are met. Candidate 0.0.4/code 14; see RELEASE_V0.0.4.md. Existing checks passed
at c1aadd1; fresh version/workflow CI and exact-head review are required.
The backend strict-scrutiny register retains unfinished Q25 acceptance evidence;
no merge or publication is claimed. No CI watching: resume when owner reports
completion. Release authorization persists; no repeated approval is needed.

# Current review — AI Studio swipe navigation and lint cleanup

Owner requested review/corrective PR(s) for issues #21 and #19 after AI Studio's
main commit `2621d00`. Work branch: `fix/review-swipe-and-lint`. The owner has authorized merge and release after review resolution and passing CI.
Release target: Android 0.0.2 / code 12; see RELEASE_V0.0.2.md.
Owner now requires full lint resolution, including dependency/toolchain/SDK
updates. Use Ktor CIO throughout networking and Kotlin-only application sources;
no Retrofit/OkHttp/Java HTTP engines. Page swipes are interior gestures and must
not consume Android edge-back. Earlier deferred-upgrade guidance is superseded.

Read [ANDROID_SWIPE_LINT_REVIEW.md](ANDROID_SWIPE_LINT_REVIEW.md) for findings,
corrections and warning accounting. Verify exact PR CI before claiming a pass.
Android builds/tests/lints run in CI to preserve the local data budget.
Samsung touch/edge-back/launcher acceptance remains separate.

# Historical — Android v0.0.1 release and warning handoff (superseded)

The following v0.0.1/v0.0.0 instructions are historical, including their sign-in
publication holds. Follow the v0.0.2 authorization above and RELEASE_V0.0.2.md.

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

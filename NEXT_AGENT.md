# Public Android download metadata — 2026-11-13

Owner-authorized companion to backend fix/web-branding-seo: /download and
landing promotion consume verified public release assets. Source base b442f38;
branch fix/release-download-metadata. No release/version bump or signing change.

Metadata derives from actual verified aapt/apksigner outputs. Existing updater
fields stay compatible, with package/signer/variant/source fields added. Tag
identity, increasing versionCode and immutable advertised APK bytes are enforced.
APK/checksum/provenance/manifest upload first; update.json is the final readiness
marker. Missing progression evidence blocks publication. No cross-repo token or
web deployment hook; web discovery refreshes at most once per five minutes.

Six stdlib Python tests and twelve workflow Bash syntax checks PASS locally.
Android build/tests/lint and actual workflow publication remain CI/delivery
checks; no local Gradle per data budget. Acting maintainer/executing agent owns
review on owner CI notification, target 2026-11-14; no CI polling. Publication
still requires cadence, signing, review/CI and installation/update acceptance.
Older handoffs below remain historical; this PR does not authorize a release.

# Issue #41 camera capture for offer photos — 2026-11-12

Branch `fix/41-camera-capture` off `main` (a1b534c). No release authorized; no
merge performed.

Owner approval adds camera capture beside the existing gallery/file source for
create and edit. The editor now offers explicit `Take photo` and `Add images`
actions in both paths. `OfferPhotosSection` owns both launchers and
`OfferImagesEditor` is the shared stateless presentation; the screen's image
block is one call. Confirmed captures enter `OfferEditorViewModel.addImages`, so
count, size/type validation, preview, reorder, removal, draft retention and the
existing server-mediated upload are unchanged.

Capture uses `ActivityResultContracts.TakePicture` with a dedicated
`${applicationId}.captures` FileProvider authority scoped to
`cache-path offer-captures/`. No CAMERA or storage permission is declared; the
system camera app owns its permission. Only the capture file name is held in
`rememberSaveable`, so recreation can rebuild the provider URI and consume the
result. Cancelled, empty or failed captures delete their file and add nothing;
overall launch failure reports and leaves the draft intact. Opening the editor
cleans abandoned captures older than 24 hours while preserving files referenced
by the retained draft or a pending capture. Captures follow the existing upload
and server-side image processing; no location metadata is added by the app.

Tests added: `OfferCameraFilesTest` (private storage, authority/path scope,
confirm/cancel/empty, recreation recovery, cleanup, path traversal, no permissions)
and `OfferPhotoSourcesRobolectricTest` (both sources, five-photo and busy limits,
pending state, move/remove callbacks, section wiring, light/dark screenshots).
Android checks run on GitHub CI only; no local Gradle execution per data budget.
Physical-device camera evidence (camera app/device/Android/build) remains NOT RUN
and is recorded separately from CI.

Editor coordination: #43 owns the offer-location block; this change replaces
only the photo block in `NativeOfferEditorScreen.kt` and leaves the location
state and UI untouched.

# Issues #30 / #31 profile retention and skeleton shell — 2026-11-11

Branch `fix/30-31-profile-retention-skeleton` off `main` (d64f2be). No release
authorized; no merge performed.

#30: `ProfileViewModel.load` now retains the same-account profile on re-entry.
`ProfileUiState` gains `refreshing`; `loading` is true only on a true cache miss.
An explicit freshness policy (`FRESHNESS_WINDOW_MS = 30s`, injectable clock) skips
redundant requests, an in-flight guard prevents overlapping fetches, a failed
refresh keeps the profile with a nonblocking error, and a different `personId`
always resets state so another account's cache is never reused. `invalidate()`
marks retained data stale after edits/collection changes. `OffersScreen` now
calls `load(person)` (non-forced) on profile activation/resume, keeps `force` for
retry and post-edit refresh, and invalidates after a confirmed save/unsave and
after an offer is created or edited.

#31: `ProfileScreen` always renders the static shell. Missing remote identity and
counts reserve geometry with reusable `ProfileIdentitySkeleton`/`ProfileCountSkeleton`
(no page spinner; `profile_loading` removed) and offers reuse `OffersGridSkeleton`.
Profile-dependent rows/edit/Manage/Public view are disabled without identity;
Appearance stays available. Skeletons are noninteractive and announced as
loading. Shared `shimmerBackground` renders statically when the platform animator
duration scale is 0 (reduced motion). Light/dark use existing tokens.

Tests added: `ProfileViewModelRetentionTest` (repeated entry, stale refresh,
overlap, offline refresh, cold-miss error, account switch, invalidate, clear) and
`ProfileShellRobolectricTest` (cold start shell/skeletons, disabled controls,
static Appearance, retained-while-refreshing, nonblocking retry, cold-start
failure). Existing profile/theme/appearance tests kept by inspection.

Whitespace PASS; no local Android execution per owner data budget. Fresh
exact-head CI required before review/merge; no CI polling. Shares
`OffersScreen.kt` with #34 (PR #44) at disjoint regions (save state vs. profile
load) — rebase if that merges first. Device verification remains separate.

# Issue #34 shared save button — 2026-11-11

Owner issue #34: one configurable, theme-aware save control reused by feed,
provider and saved cards plus offer detail, removing the unconditional white
card-heart tint. Added `ui/components/SaveOfferButton.kt` with
`SaveOfferButtonDefaults` presets (card 28dp/16dp, detail 44dp/22dp) over a
shared 48dp touch target. The scrim is `HavenColors.Canvas` at 0.85 alpha,
matching the existing detail `FrostedCircleButton`; no platform backdrop blur is
assumed, so it is valid on minSdk 24. Saved fill uses `HavenColors.Primary` and
the unsaved outline uses `HavenColors.TextPrimary`. Pending/disabled keep the
icon, change the label (Save/Unsave/Saving offer) and dim to 0.55 without
accepting clicks.

`FeedOfferCard` (feed/provider/saved) and `OfferDetailScreen` /
`NativeOfferDetailScreen` now render the shared control; `OffersScreen` wires the
detail `saving` flag from `savingSlugs`. Auth, network writes, rollback and owner
suppression stay in the screen/state layers.

Verification is Robolectric (`SaveOfferButtonRobolectricTest`): touch target
>=48dp with distinct 28/44 visible circles, Save/Unsave/Saving semantics,
enabled/disabled/pending click behaviour, and rendered centre-pixel contrast over
mid-gray busy stripes plus a theme-following scrim over black/white covers in
both themes. Screenshots land in `build/reports/screenshots/`. Android checks run
on GitHub CI only; no local Gradle execution per data budget. No release
authorized.

# PR #39 / #40 review corrections — 2026-11-11

Owner requests review resolution and merge, explicitly no release. Confirmed
rendering tests captured immutable state outside composition; changing-state
cases now invoke a state provider inside composition. Confirmed refresh/cache
race: failed refresh status must survive later Room content. Corrected failure
presentation and added deterministic delayed-cache regression alongside existing
warm-cache tests. Failing runs retained in /tmp/haven39-failure.log and
/tmp/haven40-failure.log. Whitespace PASS; no local Android execution per data
budget. Fresh exact-head CI required before resolving findings/merge. Acting
maintainer/agent owns evidence review on owner notification, target 2026-11-12;
no CI polling. Device evidence remains separate. Camera capture for offer images
is owner-approved future work alongside opt-in location issue #28, not part of
these PRs. No release/tag authorized; batch for later eligible release.

# Android handoff — v0.0.5, 2026-11-10

Read AGENTS.md, prd.md, MOBILE_OFFERS_FEED.md and PROJECT_INFO.md.
Use Android Studio/Gradle and GitHub CI. The former generator integration and
handoff files have been removed by owner instruction; Git history retains them.
Keep package, signer, auth and shipped-schema migration compatibility.

PR #38 implements branded startup, Android12+ persistent launch appearance,
GitHub APK update checks and continuous feed paging. Legacy starting windows
follow OS appearance; Compose honors saved Light/Dark/System without overriding
activity resources. Repeated cursors become retryable paging errors, and APK
requests allow 15 minutes while preserving connection/socket inactivity limits.

CI 42873093487 at 45c4ed8 PASS: 190 tests, no failures/errors and zero issues in
each debug/release lint. All five review threads resolved. Source changes after
that head in this cleanup are documentation, unused generator assets/metadata,
signing/migration comments and a migration test method rename, with existing
code behaviour and signing configuration preserved. Owner explicitly says not
to wait for another CI run for this cleanup and authorizes release now, reporting
device issues afterward. This is not device acceptance or a general policy waiver.

[RELEASE_V0.0.5.md](RELEASE_V0.0.5.md) is the owner's persistent device checklist.
Acting maintainer/agent owns release artifact verification; owner executes device
scenarios and reports findings, review target 2026-11-11. Pending tests remain
pending until actual evidence. PR #39/#33 thread skeleton work is separate and
not included here; other agents must keep independent branches and PRs.

Release workflow owns APK/metadata upload. Verify actual publication, matching
source/version/package/signer/checksum before reporting delivery. No CI watcher
is required for this documentation cleanup; release asset delivery is separate.

# Documentation pruning — 2026-11-11

Removed eight obsolete release-preparation/audit/backlog documents. Historical
sources remain in Git at e90cfb3; current build contract and API documents stay.
Release notes are now concise and user-facing; device checklist moved intact to
`docs/testing/v0.0.5.md`. Historical unfinished checkbox candidates below remain
owned by acting maintainer/agent for 2026-11-12 source/evidence reconciliation
before readiness. Device/live/Cloudinary tests require their actual environments;
no checkbox is completed by deletion. No application code or release is changed.

- `RELEASE_V0.0.0.md`: Audit profile/avatar edits, deletion, offer attachments, unread status,
- `RELEASE_V0.0.0.md`: Verify create/edit/photos/location and partial-failure recovery against
- `RELEASE_V0.0.0.md`: CI build, unit tests and lint must pass on the exact release commit.
- `RELEASE_V0.0.0.md`: Inspect final APK version, signer, backend configuration and permissions;
- `RELEASE_V0.0.0.md`: Verify signed Google sign-in/session reopening on the intended APK.
- `RELEASE_V0.0.0.md`: Physical Android and authenticated production acceptance remain separate,
- `RELEASE_V0.0.1.md`: Exact PR commit passes debug/release builds, unit tests and both lints.
- `RELEASE_V0.0.1.md`: Review generated light/dark screenshots at 158dp and 173dp card widths,
- `RELEASE_V0.0.1.md`: Samsung/device: startup, Google session reopening, offline retry, safe edges,
- `RELEASE_V0.0.1.md`: Inspect APK version, signer and provenance before distribution.
- `MOBILE_NATIVE_API_BACKLOG.md`: Completed web Google sign-in round trip on the live host is verified.
- `MOBILE_NATIVE_API_BACKLOG.md`: Physical-device sign-in/session acceptance is recorded; simulator evidence does not close this gate.
- `MOBILE_NATIVE_API_BACKLOG.md`: Merge Android PR #15 before release (do not recreate its fix in the Android app).
- `MOBILE_NATIVE_API_BACKLOG.md`: Android provider Follow and Following-row Unfollow use the real APIs and confirmed state.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android sends optional thread offer tags by `offer_id`; existing attachment scaffolding is not wired to transport.
- `MOBILE_NATIVE_API_BACKLOG.md`: Authenticated production and physical-device conversation acceptance are recorded.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android real profile-edit ViewModel/repository/navigation replaces the sample defaults and simulated save callback.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android confirmation, real deletion, visible retry and cache/list refresh are implemented.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android uses this boolean rather than counting the first inbox page; no numeric total is supplied by this API.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android lifecycle-safe stream, bounded reconnect/backoff and deduplicated resync are implemented.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android feed sends bearer where available, parses nullable saved state and isolates private metadata by account.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android cache round trips retain confirmed saved state; unknown is never treated as confirmed unsaved.
- `MOBILE_NATIVE_API_BACKLOG.md`: Prototype visual acceptance, production edit acceptance and physical-device testing are recorded.
- `MOBILE_NATIVE_API_BACKLOG.md`: Production Cloudinary incoming-preset resize/metadata-removal settings require verification; the retired backlog listed this as an unfinished acceptance task.
- `MOBILE_NATIVE_API_BACKLOG.md`: Authenticated production create/upload/recovery and physical-device acceptance are recorded.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android real picker/upload/removal and progress/error/retry states replace profile-photo simulation.
- `MOBILE_NATIVE_API_BACKLOG.md`: Production upload preset and real delivery are verified separately.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android scope/reference controls, explicit activation/change/deactivation and owner/reference-aware cache keys are implemented.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android keeps offer operating location separate from the explorer's reference; no GPS, profile or offer location auto-activation.
- `MOBILE_NATIVE_API_BACKLOG.md`: the Android maintainer completes the unchecked Android implementation tasks from the inspected handoff.
- `MOBILE_NATIVE_API_BACKLOG.md`: Physical-device and authenticated production acceptance are completed later with the owner.
- `MOBILE_NATIVE_API_BACKLOG.md`: Remaining obvious UI quality issues are corrected against the prototypes.

## PR #45 review corrections — 2026-11-11

Passing source 8cee490 (CI 43004286703) had confirmed freshness races. Added
invalidation generation so pre-write responses cannot mark the profile fresh;
forced requests during a fetch coalesce into one follow-up. Request identity
and account reset protect replacement work. Added regressions for in-flight
invalidation, forced follow-up and account switch; ordinary re-entry still
coalesces. Confirmed provider follow/unfollow, Following unfollow and My offers
delete now invalidate profile only after success and with current-account guards.
No source from PR #44 included; preserve its own-profile showSave=false change
when the branches integrate. Whitespace PASS; local Android checks NOT RUN per
data budget. Fresh CI and device mutation-return/slow-network evidence remain
required. Acting maintainer/executing agent reviews on owner notification,
target 2026-11-12; supported signed build/device are prerequisites. No polling,
merge or release performed.

## PR #44 review corrections — 2026-11-11

CI 43001629738 on 5eb426e failed Compose Modifier-factory lint in the test
helper. Converted it to a receiver-preserving Modifier extension. Confirmed
review fixes: own-profile cards hide Save (with navigation regression), contrast
sampling stays inside the glass circle, unsaved screenshots precede the state
change, and shared accessibility labels use string resources. Screenshot names
are test-owned literals; no untrusted path input exists (static analyzer warning).
No blanket lint suppression or baseline added. Existing public-component KDoc
covers its contract; tests do not need artificial docstrings for a bot percentage.
Whitespace checked; Android build/tests/lint NOT RUN locally per data budget.
Fresh CI and rendered artifacts still required; physical-device contrast,
TalkBack and network failure evidence remain separate and unverified. Acting
maintainer/executing agent reviews on owner notification, target 2026-11-12,
with supported device/build prerequisites. No CI polling, merge or release.

PR #44 follow-up: run 43004058297 failed unit-test compilation due to an
incorrect top-level assertDoesNotExist import introduced in b04cb62. Removed
the import; existing repository tests call the node member without it. Both
lint tasks passed on that run; tests did not execute. Fresh CI remains required.

## Integrated offer location + camera — 2026-11-12

Owner approves #43 with camera #41/PR #46. Location modules separate pure fix
validation/precision, Android foreground acquisition, permission/review state,
lifecycle integration and stateless editor UI. Both Create/Edit reuse them.
No automatic request/reacquisition: tap, permission, bounded current fix, review,
explicit add to draft, then existing submit. Manual selection/removal cancel
pending work. Stop/disposal/account changes invalidate callbacks; process restart
requires another tap. Selected tokens use existing SavedStateHandle draft recovery.
New backend POST /api/v1/me/offer-places/coordinates validates/signs the existing
place model for bearer owner; it does not publish or write an offer. Backend must
be deployed before device end-to-end acceptance; failure retains prior draft.
Camera integration fixes duplicate provider class, rejects oversized captures,
preflights both sources before draft insertion, rejects cross-editor/account
results, and blocks submit while capture/validation/location review is pending.
CI 43132409960 originally failed 3 provider tests and junit-version lint; uses
separate capture provider plus official junit 1.4.0-alpha04. No lint suppression.
Local static XML/whitespace checks only; no Android Gradle per budget. Regression
coverage added for location intent/review/failure/cancel/late results/precision,
provider coexistence and photo limits. Fresh CI and physical-device permission,
rotation/process restart, camera and public saved-location evidence NOT RUN.
Acting maintainer/executing agent owns CI/review on owner notification and device
prerequisite review 2026-11-13; signed build, deployed backend and supported phone
required. No polling, merge or release. See docs/testing/offer-capture.md.

## PR #46 CI/review correction — 2026-11-12

Backend #74 exact 3697f3f: CI 43206947423, supply-chain 43206947435 and
build/scan/smoke 43206947300 SUCCESS; deployment skipped for PR, no deployment
claim. No backend inline findings. Mobile 6242552 run 43206219536 failed five
provider tests plus lint for espresso/core/runner alpha04 and String.toUri.
Exact AndroidX core1.19.1 source confirms static FileProvider.sCache by authority;
Robolectric creates a different cacheDir per method. Reset only test cache state;
keep independent installed provider component identities and path scopes.
Aligned remaining test-only AndroidX alpha04 versions and used the KTX URI API.
Confirmed new reviews: decode an actual bounded <=1024-per-axis preview after
size/type checks, use a valid JPEG fixture and reject truncated prefix; listen
to both eligible foreground providers within one bounded request, removing both
on success/cancel; report same-account photo overflow while rejecting stale
account results silently. Added real-adapter network/cleanup tests. Also fixed the valid review-summary
finding: retain pending validation URIs and actor in saved state, restart after
recreation, preserve those files during cleanup and deduplicate already-retained
images after interrupted completion. Cancellation never clears pending inputs.
Temporary checkout disappeared after all earlier work was pushed; restored
source in persistent ignored .worktrees/haven-app-46, no unpushed work lost.
Whitespace PASS; Android tests/lint NOT RUN locally per budget. Fresh CI and
remaining review resolution required on owner notification (review target
2026-11-13). Device/deployment prerequisites remain in docs/testing/offer-capture.md.
No CI polling, merge, deployment or release.

PR #46 follow-up 2026-11-12: run 43224128373 on 5f46ce6 failed only
MutableCollectionMutableState lint for saved validation ArrayList. Use a List
state value, replaced as a whole via distinct(), preserving saveable URI contents
and recreation recovery without mutable-collection state or suppression.
Backend #74 merged as 6904b5b; merge is not deployed-endpoint acceptance.
Fresh mobile CI remains required on owner notification; no polling or release.

## Owner-directed release preparation — 2026-11-12

Owner explicitly requests backend v0.8.67 and Android v0.0.6 publication, granting
the timing exception for both. Prior release history: backend feature v0.8.66
2026-11-09 17:00:44Z, Android feature v0.0.5 2026-11-10 18:36:08Z. No other
release today at pickup. Android v0.0.6 is versionCode16; workflow identity,
asset names and tag checks aligned. Preserve package and signer. Concise notes
in RELEASE_V0.0.6.md; device checks remain docs/testing/offer-capture.md.
Device capture/TalkBack/privacy acceptance remains NOT RUN and must not be
reported as passed. Fresh release-head CI/review and delivered APK/update metadata
identity are required. No local Gradle. Owner-directed publication is not evidence
of installed-device acceptance or a standing waiver for future releases.

Prepublication rendered evidence review: 43dae3d has 282 passing tests and zero
lint issues per variant. Dark location screenshot exposed a test wrapper missing
HavenColors.Canvas (the actual native screen already paints it). Corrected the
wrapper to match production before accepting refreshed visual evidence. No
application behavior change; release-head checks remain required.

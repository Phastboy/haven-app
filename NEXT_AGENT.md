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
- `MOBILE_NATIVE_API_BACKLOG.md`: Production Cloudinary incoming-preset resize/metadata-removal settings are confirmed.
- `MOBILE_NATIVE_API_BACKLOG.md`: Authenticated production create/upload/recovery and physical-device acceptance are recorded.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android real picker/upload/removal and progress/error/retry states replace profile-photo simulation.
- `MOBILE_NATIVE_API_BACKLOG.md`: Production upload preset and real delivery are verified separately.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android scope/reference controls, explicit activation/change/deactivation and owner/reference-aware cache keys are implemented.
- `MOBILE_NATIVE_API_BACKLOG.md`: Android keeps offer operating location separate from the explorer's reference; no GPS, profile or offer location auto-activation.
- `MOBILE_NATIVE_API_BACKLOG.md`: the Android maintainer completes the unchecked Android implementation tasks from the inspected handoff.
- `MOBILE_NATIVE_API_BACKLOG.md`: Physical-device and authenticated production acceptance are completed later with the owner.
- `MOBILE_NATIVE_API_BACKLOG.md`: Remaining obvious UI quality issues are corrected against the prototypes.

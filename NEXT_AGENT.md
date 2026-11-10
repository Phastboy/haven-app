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

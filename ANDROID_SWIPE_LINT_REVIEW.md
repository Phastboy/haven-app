# Android review: swipe tabs and lint cleanup

## Reviewed source and evidence

Historical source changed main in `2621d00`, following `2adebed` (the file-based handoff).
[Its CI 42384744900](https://github.com/Phastboy/haven-app/actions/runs/42384744900)
passed existing checks but added no gesture regression tests. Both lint reports
contain 40 warnings: 35 compatibility/version advisories and 5 legacy launcher
shape warnings, compared with the original 65 per variant. The resource moves,
unused-colour/label removal and KTX conversions account for 25 resolved warnings.

## Corrections in this review

- Use settled pager navigation for page activation and guest sign-in, avoiding
  actions during cancelled drags or intermediate animated pages. Tab presses
  cancel superseded navigation requests. Tab controls expose selected/Tab semantics.
- Reset tab state for a different account and keep private content gated during
  pager precomposition. Unread badge reads are separate from loading the inbox;
  page composition does not trigger a private inbox read.
- Hoist inbox/profile/category scroll state so tab changes and child-route Back
  retain their position. Root paging stays out of child routes and pauses for
  filters, errors and profile dialogs. Category overscroll is consumed locally.
- Reuse existing round Haven launcher artwork for pre-26 launchers, replacing
  the duplicate full-square legacy icons. Adaptive icon files and logo artwork
  stay unchanged. Review the legacy resource on a device separately.
- Remove a pre-existing fixed-key encryption fallback from production credentials.
  Only tests inject test keys. A transient key-provider failure propagates without
  deleting saved ciphertext, allowing startup retry. Existing Keystore alias,
  cipher format and signing identity are retained; no database migration/reset.

## Owner clarification and full lint modernization

The owner explicitly superseded the earlier plan to retain compatibility notices:
fix all lint issues, including dependencies/toolchain/SDK where necessary. Tab
swipes start within page content, not at an Android screen edge. Android's system
edge-back behavior stays intact; no system gesture exclusions or raw edge handler
are installed. Regression swipes explicitly start/end inside the page.

Networking is **Ktor 3.6.1 with CIO**, including API reads/writes, uploads, SSE and
Coil 3 image fetching. No Retrofit, OkHttp or Java HTTP engine remains. Application
source is Kotlin-only. Android/JVM platform and cryptographic APIs are still used;
no replacement cryptography or signing identities are invented.

Gradle 9.9.0 (verified distribution checksum), AGP 9.5.1, Kotlin/Compose compiler
2.4.21, KSP 2.3.13, Compose BOM 2026.09.00, Room 2.8.5 and Robolectric 4.17 are
pinned. Compile API 37.2 / target 37; CI installs the matching platform and uses Temurin
21.0.13+9. Active AndroidX/coroutines/Credential Manager dependencies are updated
from official Maven metadata. Unused Firebase/Retrofit/Moshi/camera/navigation
scaffold declarations/plugins are removed instead of retained or upgraded as dead
weight. Existing native Google OAuth and signing configuration are preserved.

Coil 3.6.3 uses its Ktor 3 network fetcher and explicit CIO clients. Public CDN
redirects use a separate image client and never inherit an API bearer. JSON/API
redirects remain disabled. CIO cancels stream reads on coroutine cancellation;
refresh credential rotation completes/persists before releasing its coroutine
mutex, preventing old-refresh replay if its caller is cancelled. HTTP failures
retain typed status/body and multipart keeps its stable upload identity.

The earlier source review measured the historical 65 → 40 warning improvement.
This PR targets **zero warnings** in both lint variants: no baseline, suppression
or disabled version checks. Lint warnings are errors, and CI verifies the resolved
runtime uses CIO and contains no Retrofit/OkHttp/Java HTTP engines or Java app
source. Exact final counts/results must be recorded from the final CI revision.

Compatibility sources:
- [AGP 9.5 requirements](https://developer.android.com/build/releases/agp-9-5-0-release-notes)
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk)
- [Ktor engine support](https://ktor.io/docs/client-engines.html)
- [Coil 3 migration](https://coil-kt.github.io/coil/upgrading_to_coil3/)
- [Robolectric supported platforms](https://robolectric.org/compatibility_table/)

## Verification

New regressions cover swipe/tap selection, no wraparound, cancelled drags,
guest gating, rapid requests, private inbox read isolation, child/modal/category
isolation, scroll retention, account reset/restoration, credential failure and
legacy launcher resource resolution, Ktor status/body and multipart fields,
stream cancellation, refresh rotation/cancellation and sign-out races. The root
pager regressions run on both API 35 and 37. Exact build/test/lint results belong in the
closing PR description; source implementation alone is not proof they passed.

CI `42395058536` verified all 14 pager tests after the AndroidX Test input-service
fix. The sole remaining test failure exposed Robolectric's native ImageDecoder
FileSource limitation (`Only supported on Android`). The photo regression uses
Coil's supported BitmapFactory decoder in tests only, retaining real JPEG decode,
pixel checks and the production card. Production decoder selection is unchanged;
physical-device photo acceptance remains separate. Compose test rules use the
current v2 APIs and their standard test dispatcher, rather than suppressing the
deprecated rule warnings.

The upgraded lint additionally identified 20 Modifier parameter ordering issues,
two composition reads of frequently changing scroll values and one empty super
call. These are corrected directly; scroll persistence now uses snapshotFlow.
Five primitive-state hints are corrected as well. No lint baseline or suppression
is used. Final verification still determines the final count.

## Review resolution and authorized release

Owner authorized merge/publication of v0.0.2/code 12 after review resolution.
Sign-out now completes its mutex/revocation operation despite cancellation and
clears local credentials/session even when token decryption is unavailable.
Explicit permanent invalidation and API 33+ corrupted/missing-key codes reset the
device key and credentials; unclassified/provider failures stay retryable.
Regression tests cover the default missing-provider path and fresh sign-in after
key replacement. These match the documented Android Keystore exception contract:
https://developer.android.com/reference/android/security/KeyStoreException

Foreground activation is replayed after a pager animation settles, including
return to the same settled tab. Offers always cancels a pending navigation job.
Loading profile content uses its own scroll state so saved profile offsets survive
restoration. Tests use the production inbox/profile renderers. Unread-only tests
also verify that a supplied local repository is neither observed nor refreshed.
SSE heartbeats reset the failure budget without invalidating content; genuine
consecutive failed connections retain the five-attempt cap.

CI checkout does not persist credentials. PR caches are read-only and published
release builds disable Gradle action caching. Release assets remain automatic.
Exact final CI evidence is recorded on PR #24, not inferred from earlier passes.
Physical Samsung swipes, system edge-back, TalkBack and launcher rendering still
need device acceptance and must not be inferred from CI.

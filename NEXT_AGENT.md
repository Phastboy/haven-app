# Start here — Haven mobile/backend pickup, 2026-11-02

> 2026-11-04: browser PKCE sign-in was removed. Native Google
> (`POST /api/v1/auth/google`) is the only app sign-in path; the `/auth/callback`
> App Link, `assetlinks` configuration and email magic link no longer exist.

## Current release and owner direction

Backend PR #53 merged at `d2c166a56e8e3d87592cbfc0845da12dd9a8db7d`.
[v0.8.5](https://github.com/Phastboy/haven/releases/tag/v0.8.5) is live.
Keep backend/Topcoat releases within **v0.8.*** for the coming months.
The next API exposure increments the patch to 0.8.6; it is not selected yet.
Google AI Studio should implement the corresponding native pages using
[`MOBILE_NATIVE_PAGES.md`](MOBILE_NATIVE_PAGES.md). Backend contracts lead;
no runtime samples, fake timers, simulated success, or local substitute writes.

Five live screen APIs:

- Public offer detail: `GET /api/v1/offers/{slug}`.
- Provider profile: `GET /api/v1/profiles/{handle}?after=...`.
- My published offers: `GET /api/v1/me/offers?after=...`.
- Available Saved offers: `GET /api/v1/me/saved?after=...`.
- Following: `GET /api/v1/me/following?after=...`.

One-tap writes use valid native bearer credentials:
`POST /api/v1/offers/{slug}/saved` and `DELETE` on the same path.
My offers contains published offers only; Saved omits unavailable offers.
Follow/unfollow, editing, inquiry/send and native thread/live APIs are outside
this batch. Existing supported web journeys remain the fallback for those actions.

## Android implementation and next pickup

Android PR #8 merged feed card/category shimmers and profile action placement.
Appearance is in the profile actions group; Sign out is below the offered cards.
Android PR #9 merged at `0e011cc` with native detail and feed/detail save wiring.
Review fixes were committed directly to main at `b0766051d6f9659abe961b913ef71393fcf854c0`
under an explicit one-time owner exception. All four review threads are resolved:
backend price_display is preserved, provider opens a real profile, loading/error
supports visible and system Back, and an unknown outlined Save action explicitly
saves instead of unexpectedly unsaving. Do not generalize the main-commit exception.

[Android CI 41419917646](https://github.com/Phastboy/haven-app/actions/runs/41419917646)
passed build, tests, lint, reports and APK upload, including five review regressions.
Use CI for builds/tests/lint; local Gradle dependency downloads exceed the owner's
data budget. Read the actual versionCode before distributing another APK and bump
it as needed; Android versionName is independent from backend releases.

Next AI Studio tasks:

1. Inspect current native detail/save code before duplicating it. Match
   `prototype/haven_offer_detail` in light/dark mode, including gallery, identity,
   location, fixed action, system insets and structure-shaped shimmer loading.
2. Exercise signed-in production save/unsave from feed and detail, repeat taps,
   failures, returning to feed, app restart, account change and guest sign-in.
   Unknown save status is not confirmed unsaved status; a visible Save action
   must not silently turn into DELETE.
3. Integrate provider profile, My offers, Saved and Following against the live
   contracts and their matching prototypes. Use real empty/error/retry/paging
   states and server responses; keep unsupported writes on real web journeys.
4. Compare actual device feed/profile with their prototypes. Feed visual fidelity
   was rejected by the owner; no final device comparison has passed. Green CI
   and source alignment do not establish visual acceptance.

Feed/profile use shared FeedOfferCard with optional server distance; general feed
does not provide distance. Never invent it. Integrated Messages inbox opens real
web threads in themed Custom Tabs; native thread history/send/live remains later.
Auth is native Google only: Credential Manager → `POST /api/v1/auth/google` →
rotating refresh tokens → `GET /api/v1/me`. The app never opens a browser for
authentication. The email option is visible but disabled, with a one-line note.

## Evidence and remaining limits

Production curl smoke on 2026-11-02: feed, `/api/v1/offers/buy-me-623b2271` and
`/api/v1/profiles/aanusteven8-60ed44` returned 200. The three private lists and both
save writes returned 401 anonymously. These checks did not mutate production.
No authenticated production save/unsave or device journey is claimed.

Forty recorded local curl checks passed using a separate native-auth-configured
server: token exchange, private reads, idempotent save, detail readback, Saved
membership, idempotent unsave and final empty collection. The exchange step used
the retired PKCE route; the private-read and save evidence still applies. The test actor's
Saved collection was restored to empty. See the backend report
`verification/native-api-v085/RESULT.md` for nonempty fixture, pagination,
second-actor isolation and image-delivery limits. Credentials are not committed.

## Identity and repositories

- Backend: Phastboy/haven, workspace `expts/rust`.
- Android: Phastboy/haven-app, local `/home/tgenericx/dev/github.com/phastboy/h-mobile`.
- Host: `https://haven.apps.mistral.run` (Koyeb `haven/web`, custom domain).
  The legacy `https://haven-odysseon-d48990aa.koyeb.app` returns
  `404: No active service`. `HavenAuthClient.HOST` uses the live domain.
- Package: `com.aistudio.haven.xvykpt`. The Android OAuth client for native Google
  must be registered with this package and the installed APK's signing SHA-1.
- Pinned AI Studio signer:
  `00:27:07:D7:C9:09:36:6B:CE:B9:72:30:95:C9:64:5E:54:4D:E8:FF:3B:74:C8:61:BF:3F:1E:BB:66:36:41:49`.
  CI debug keys can differ, and each signing certificate needs its own Android
  OAuth client.
- Tag `v*` triggers backend publication/deployment. Android release artifacts
  currently use testing debug signing, not production APK signing.

Theme changes use HavenColors/ThemeManager. Match prototypes without arbitrary
underlining; validate rendered light/dark screens. AI Studio can edit/test Android,
not this backend; never give it backend credentials.

## Mobile pickup records

Start with this file and MOBILE_NATIVE_PAGES.md, then prd.md, PROJECT_INFO.md,
MOBILE_OFFERS_FEED.md, MOBILE_PROFILE.md and MOBILE_MESSAGES.md. Older contract
files describe earlier slices; this pickup record governs current integration
status. Preserve unrelated files such as env_update if present. No additional
direct-to-main permission is inferred from the previous review-fix exception.

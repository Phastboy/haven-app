# Start here — Haven mobile/backend handoff, 2026-11-02

## Latest steering: Messages after Profile

Profile is complete: backend PR #50 shipped v0.8.3 and Android PR #4 is merged.
Backend PR #51 shipped v0.8.4; Android PR #6 is merged at `783ce27`. Its
post-merge build/test/lint passed in run
[41369778485](https://github.com/Phastboy/haven-app/actions/runs/41369778485).
Read `MOBILE_MESSAGES.md` for the inbox contract and current boundary.

This slice is the Messages inbox only. It renders real API rows and opens each
real Haven web thread in a themed Custom Tab. Native thread/send/live updates
remain later work. No runtime fixture or old demo-thread fallback is permitted.

The owner pinned the AI Studio signer to
`00:27:07:D7:C9:09:36:6B:CE:B9:72:30:95:C9:64:5E:54:4D:E8:FF:3B:74:C8:61:BF:3F:1E:BB:66:36:41:49`.
Live `assetlinks.json` and Google Digital Asset Links verification currently
recognize that fingerprint. CI debug APKs use a separate ephemeral key.

Feed and profile now share `FeedOfferCard`, supporting optional real distance.
Current general API results contain no distance; no distance is invented. CI
renders card/profile screenshots in both themes and tests foreground pixels.
Inspect those artifacts and request device comparison before accepting exact
prototype fidelity. Native edit/collections/detail still open real web journeys.


## Owner decisions

- Backend contracts lead Android development. Expose an existing Haven journey,
  then integrate its screen using real responses.
- Once a screen is selected for integration, no runtime dummy rows, fake loading
  timers, fabricated errors/success, or local-only replacements for server actions.
  Real empty/error/pending states are required. Fixtures are for tests/previews.
- Prioritize fast movement and correctness of core journeys. Do not resume
  deferred performance investigations or chase maximum performance as a gate.
  Security, data loss, and broken core journeys still need immediate triage.
- Use CI for builds/tests/lint; local Android dependency downloads exceed the
  owner's data budget. Java/Gradle/SDK tools are installed, but project build
  dependencies have not been fully downloaded locally.
- Each backend API exposure increments the patch release and workspace/lockfile
  versions. Current live version is **0.8.4**; the next exposure is **0.8.5**.
  Cargo uses three-part versions. Android versionName is independent; it can
  start at `0.0.0`. Keep versionCode increasing for distributed builds.

## Repositories and completed work

- Backend: `Phastboy/haven`, Rust workspace `expts/rust`.
- Android: `Phastboy/haven-app`, local checkout
  `/home/tgenericx/dev/github.com/phastboy/h-mobile`.
- Backend branch `feat/mobile-offers-feed-api`, PR #48 merged at
  `93cc5e72a9d6e0064f2e50dec6144ff64f068681`.
- Android branch `feat/backend-offers-feed`, PR #2 merged at
  `107ac62494c37f515e5a91d8f9c5518aff3e8096`.
- Backend releases [v0.8.3](https://github.com/Phastboy/haven/releases/tag/v0.8.3)
  and [v0.8.4](https://github.com/Phastboy/haven/releases/tag/v0.8.4) are deployed.
  The latter adds the private inbox endpoint; production unauthenticated access
  returns 401 with `private, no-store`.
- Backend CI: 254 tests, formatting, Clippy, docs, security, container checks pass.
  Android final feed CI [41296308456](https://github.com/Phastboy/haven-app/actions/runs/41296308456)
  passed build, unit/Robolectric tests, and Android Lint; testing APK is an artifact.

## Actual integration status

Auth uses browser PKCE authorization/code exchange, rotating refresh tokens, and
`GET /api/v1/me`. The owner confirmed completing Google sign-in and returning to
the app. Existing browser cookies can make sign-in immediate. Do not claim a
fresh browser-session test was traced: it remains pending. Email delivery is
disabled, so email sign-in is unavailable.

`GET /api/v1/offers` and private profile overview are live. The native profile
uses the real profile endpoint and persists System/Light/Dark appearance.
Android PR #6 integrates the private conversation inbox against the deployed
v0.8.4 API. Native thread/send/live updates, detail, and saved-state writes are
not integrated. Do not introduce local fake state.

The owner says the feed does **not** look exactly like its prototype. No device
screenshot comparison has passed. Source alignment and green CI do not establish
visual fidelity. Resolve this before claiming the feed is visually complete.

## Current corrections and next work

1. Android: auth and web handoffs use Chrome Custom Tabs. The Messages inbox
   row handoff also uses a themed Custom Tab; check CI/device behavior.
2. Remove inappropriate underlining of clickable controls. The integrated feed
   filter sheet had an explicit underline on Clear; clickability alone must not
   imply underlining. Compare the actual device screen with
   `prototype/haven_offers_feed`, including light/dark modes, spacing, type,
   controls, cards, bottom navigation and system insets. Do not redesign by guess.
3. Messages inbox is integrated. The next native screen/API slice has not been
   selected. Native thread history/send/live updates still need their own
   backend contract; until then, inbox rows open the real web thread.

AI Studio can edit/test the Android app in its simulated device. It cannot edit
this backend. Give it precise Android/device tasks; handle backend work here.

## Deployment and auth identity

- Host: `https://haven-odysseon-d48990aa.koyeb.app`; Koyeb service `haven/web`.
- Android package: `com.aistudio.haven.xvykpt`.
- `MOBILE_APP_LINK`: same host with `/auth/callback`.
- AI Studio APK SHA-256 signer (pinned to workspace debug.keystore via debugConfig):
  `00:27:07:D7:C9:09:36:6B:CE:B9:72:30:95:C9:64:5E:54:4D:E8:FF:3B:74:C8:61:BF:3F:1E:BB:66:36:41:49`.
- Configured via `ANDROID_APP_PACKAGE` and `ANDROID_APP_SHA256_CERT`.
  `/.well-known/assetlinks.json` returns 200; Google returned `linked: true`.
  CI debug APKs can have a different signer; do not assume their App Links verify.
- Tag `v*` triggers backend container publication/deployment. Android release
  publication currently attaches a debug testing APK, not a production-signed APK.

## Android version and verification

The current branch uses `versionCode` 6; it is independent of backend version
0.8.4 and Android `versionName` 1.0. Keep incrementing versionCode for future
distributed builds. CI is the build/test/lint path; do not download Gradle
dependencies locally. The AI Studio signer is pinned in the project's debug
keystore; CI can generate its own temporary debug key when that file is absent.

## Theme and design rules

`ui/theme/HavenColors.kt` defines the central light/dark palettes. HavenTheme
provides them through CompositionLocal; integrated feed and sign-in use those
tokens, and ThemeManager supports System/Light/Dark. Literal colors belong in
the palette, not individual product screens. The Google brand mark retains its
brand colors; system transparent bars are platform chrome. Older prototype files
still have some literal colors: audit only as each screen is integrated, and do
not claim the entire repository has passed a theme audit.

Do not underline a control merely because it is clickable. Match the selected
prototype, validate appropriate affordances, and inspect actual rendered screens
in both themes. Feed visual fidelity is explicitly NOT accepted by the owner.
The new correction does not by itself prove the whole feed matches the reference.

Start with this file, `prd.md`, `MOBILE_OFFERS_FEED.md`, and `PROJECT_INFO.md`.
Historical `AI_STUDIO_AUTH_HANDOFF.md` describes a now-resolved association
failure; its backend-edit instructions do not apply to AI Studio. Check current
Git state and CI before continuing, preserve unrelated work, and request no
backend credentials in Android prompts or commits.

# Native API exposure backlog

Reconciled 2026-11-06 against backend `4a48247` (v0.8.64) and Android
`5d1b6813ffd7925d2dfa4e766717d291f007c070`. The additional API sweep is implemented
on `feat/native-parity-api-sweep`; it is **not deployed**. Checkmarks distinguish
implemented/tested code from the separately unchecked deployment/device gates.

This is parity for shipped v0.1–v0.8 journeys, authorized by the owner; no 0.9
scope is introduced. Contracts: [existing native pages](MOBILE_NATIVE_PAGES.md),
[offer lifecycle](MOBILE_OFFER_LIFECYCLE.md), [API sweep](MOBILE_NATIVE_API_SWEEP.md).
Frontend tasks come from [the inspected Android handoff](MOBILE_AI_STUDIO_HANDOFF.md).

## Status at a glance

| Area | Backend | Android implementation |
| --- | --- | --- |
| Native Google sign-in | Released | Implemented; owner confirmed simulator sign-in |
| Offer detail, profile, My offers, Saved, Following | Released | Integrated; collection ViewModels refactored on main |
| Native thread, enquiry and older-history paging | Released | Integrated; duplicate-key rendering fix in PR #15 |
| Offer create/edit, photos and operating location | Released in v0.8.64 | Merged in PR #14 |
| Follow/unfollow | Released | Still browser handoffs; follow state is not parsed |
| Profile text edit and avatar | Implemented/tested in API sweep | Not integrated; existing profile editor is a simulation |
| Offer deletion | Implemented/tested in API sweep | No native delete API action |
| Global unread | Implemented/tested in API sweep | Badge still counts loaded inbox rows |
| Thread JSON SSE | Implemented/tested in API sweep | No subscription/reconnect implementation |
| Feed saved state | Implemented/tested in API sweep | Public feed does not send bearer or parse saved state |
| Proximity and Inquired discovery | Implemented/tested in API sweep | No native scope/reference query or controls |

## P0 — Authentication and host configuration

- [x] Google ID-token exchange, rotating refresh/revoke, `/api/v1/me` and native-only sign-in are implemented and deployed.
- [x] Browser PKCE authorize/exchange/callback and assetlinks paths were removed in v0.8.62.
- [x] Android OAuth client/signing configuration was owner-confirmed; owner subsequently confirmed sign-in on the Android simulator.
- [x] Web Google redirect now points to `https://haven.apps.mistral.run/auth/google/redirect` (read-only check 2026-11-06).
- [ ] Completed web Google sign-in round trip on the live host is verified.
- [ ] Physical-device sign-in/session acceptance is recorded; simulator evidence does not close this gate.

## P1 — Conversation pack and follow integration

- [x] Bearer follow/unfollow writes are idempotent; unknown handles 404, self-follow 400, cookie-only writes 401.
- [x] Provider profile returns bearer-confirmed `is_following`; guest state is omitted.
- [x] Participant-scoped thread reads/send, offer-tag validation, mark-read behavior and opaque older-history cursors are implemented and released.
- [x] Offer enquiry send and untagged profile enquiry send use real conversation/message storage.
- [x] Native sends broadcast updates to existing web listeners; stable client IDs deduplicate thread-send retries.
- [x] Android routes inbox/enquiry success to `NativeConversationThreadScreen`; production transport contains no simulator fallback.
- [x] Android older-history paging and send/retry are implemented; Android PR #13 and subsequent CI provide implementation evidence.
- [x] Duplicate `history_status` keys were found by source inspection and fixed separately in Android PR #15; its rendering regression/build/tests/lint pass.
- [ ] Merge Android PR #15 before release (do not recreate its fix in AI Studio).
- [ ] Android provider Follow and Following-row Unfollow use the real APIs and confirmed state.
- [ ] Android sends optional thread offer tags by `offer_id`; existing attachment scaffolding is not wired to transport.
- [ ] Authenticated production and physical-device conversation acceptance are recorded.

Shared write-limiting decision from 2026-11-03 remains: bearer checks, domain/body
bounds and existing admission limits apply. No ad hoc per-person limiter was
added; shared native-write rate limiting needs one consistent policy.

## P2 — Profile text editing

- [x] `PATCH /api/v1/me/profile` updates supplied display name, handle and bio via `UserProfileUpdate`; `POST` is an equivalent alias for the current Android client.
- [x] Absent bio retains it; null/empty clears it. Domain bounds are enforced.
- [x] Taken handles produce stable HTTP 409 JSON (`error: handle_taken`).
- [x] Bearer required; cookie-only requests 401; tests cover edits, retained/cleared bio, validation and conflicts.
- [ ] Android real profile-edit ViewModel/repository/navigation replaces the sample defaults and simulated save callback.

## P3 — Offer deletion

- [x] `DELETE /api/v1/offers/{slug}` is owner-scoped and also supports private offers.
- [x] Missing/other-owned offers return identical 404; bearer required; cookie-only 401.
- [x] Owner success/isolation/unknown/guest tests pass.
- [ ] Android confirmation, real deletion, visible retry and cache/list refresh are implemented.

## P4 — Global unread

- [x] `GET /api/v1/conversations/unread` returns the bearer-scoped `unread_exists` boolean across all conversations.
- [x] Guest/cookie-only isolation and incoming-message state are tested.
- [ ] Android uses this boolean rather than counting the first inbox page; no numeric total is supplied by this API.

## P5 — Live thread updates

- [x] `GET /api/v1/conversations/{id}/events` exposes bearer-authenticated JSON invalidations from the existing broadcaster.
- [x] Participant isolation, revocation before notifications and connection termination at token expiry are tested.
- [x] Contract specifies initial `resync`, `changed`, lag resync, keepalive, bounded connections and reconnect behavior.
- [x] Process-local delivery limitation is documented; full thread resync and a slow foreground fallback are required.
- [ ] Android lifecycle-safe stream, bounded reconnect/backoff and deduplicated resync are implemented.

## P6 — Saved state on discovery cards

- [x] `/api/v1/offers` accepts optional bearer and returns `is_saved` for signed-in pages; guests omit the field.
- [x] `saved_among` resolves the bounded page in one operation; no query per card.
- [x] Mixed saved/unsaved and guest isolation are tested; existing public pagination tests pass.
- [ ] Android feed sends bearer where available, parses nullable saved state and isolates private metadata by account.
- [ ] Android cache round trips retain confirmed saved state; unknown is never treated as confirmed unsaved.

## P7 — Offer editing

- [x] Actual API: `GET`/`POST /api/v1/me/offer-management/{id}` (replaces the earlier proposed PATCH-by-slug contract).
- [x] Category, price/currency, public offer location and photo add/remove/order use existing domain/repository rules.
- [x] Owner-only access, signed place selection/removal, last-photo protection and image retry/capacity behavior are tested.
- [x] Android native editor and owner management collection are merged in PR #14.
- [x] Existing private offers can save without publication; published offers retain a photo.
- [ ] Prototype visual acceptance, production edit acceptance and physical-device testing are recorded.

## P8 — Offer creation

- [x] Owner authorized native creation; this is no longer an undecided/deferred item.
- [x] `POST /api/v1/me/offer-management` preserves creation idempotency; first photo publishes the offer.
- [x] Multipart photos reuse existing signed Cloudinary storage; no server image decoder was added.
- [x] Categories/currencies/limits and private recent places come from `/api/v1/me/offer-form`; place search uses signed person-bound selections.
- [x] Owner management pagination includes offers awaiting photos, so interrupted publication remains recoverable.
- [x] Android native create flow, stable upload identities, retained entries and partial-save messaging are merged.
- [ ] Production Cloudinary incoming-preset resize/metadata-removal settings are confirmed.
- [ ] Authenticated production create/upload/recovery and physical-device acceptance are recorded.

Backend PR #62 and Android PR #14 are merged. v0.8.64 deployment succeeded in run
42084317790; the private form API returned 401 anonymously. This does not establish
an authenticated production lifecycle pass. Later overlapping-image retry fixes
are included in the released backend.

## P9 — Avatar upload/removal

- [x] Backend avatar parity is included in the owner-authorized API sweep.
- [x] `POST /api/v1/me/avatar` (multipart `avatar`) and `DELETE /api/v1/me/avatar` reuse signed Cloudinary storage and repository-triggered cleanup.
- [x] Bearer identity supplies the owner; supported signatures, empty/size bounds and upload failure handling are implemented.
- [x] Tests cover successful adapter upload, unsupported/empty input, unauthenticated upload and removal.
- [ ] Android real picker/upload/removal and progress/error/retry states replace profile-photo simulation.
- [ ] Production upload preset and real delivery are verified separately.

## P10 — Proximity and Inquired discovery (previously missing from this backlog)

- [x] `/api/v1/offers?scope=public|saved|inquired` applies the existing facets and scoped pagination; `/api/v1/me/inquired` is an explicit alias.
- [x] `GET /api/v1/places/search?q=...` returns real explorer-reference choices without activating proximity.
- [x] Explicit `near=<choice>` uses existing nearest-first retrieval; absent `near` or `near=off` is general discovery, ignoring browser cookies.
- [x] All eligible distant offers remain; unlocated offers sort last. Provider identity remains in every card.
- [x] Actual distance is supplied only for located offers under an active reference; no distance is fabricated for general discovery.
- [x] Tests cover nearest/distant/unlocated results, signed-in collection isolation, reference-bound paging and browser-preference isolation.
- [ ] Android scope/reference controls, explicit activation/change/deactivation and owner/reference-aware cache keys are implemented.
- [ ] Android keeps offer operating location separate from the explorer's reference; no GPS, profile or offer location auto-activation.

## Remaining release gates (not implementation completion)

- [ ] Merge/release/deploy the new API sweep; do not treat these branch contracts as live yet.
- [ ] Backend sweep CI and container/security checks pass on the final branch head.
- [ ] AI Studio completes the unchecked Android implementation tasks from the inspected handoff.
- [ ] Physical-device and authenticated production acceptance are completed later with the owner.
- [ ] Remaining obvious UI quality issues are corrected against the prototypes.

0.9.x, commitments, payments, push notifications and new product behavior remain
outside this parity sweep. Acknowledged performance debt is not reopened here.

## Current evidence

- Baseline Android main `5d1b681`: CI 42174560135 passed (ViewModel migration).
- Android duplicate-key fix PR #15: CI 42179102446 passed, including rendering regression.
- API sweep: workspace Clippy with warnings denied passed; 67 native API tests passed locally, including eight new parity cases. Full branch CI is a separate gate above.
- Web Google redirect target was checked read-only on 2026-11-06 and is the live domain.

## Historical verification log

| Date | Change | Evidence |
|---|---|---|
| 2026-11-03 | P1b thread read/send | `cargo test -p haven-web` 203 passed; `cargo test -p haven-db conversations` 6 passed; clippy `-D warnings` clean; contract in `MOBILE_NATIVE_PAGES.md` |
| 2026-11-03 | P1a follow/unfollow + profile state | `api::native_follow` 6 tests passed (idempotent POST/DELETE, cookie/anonymous 401, self-follow 400, unknown handle 404, invalid bearer 401, guest omits `is_following`); contract in `MOBILE_NATIVE_PAGES.md` |
| 2026-11-04 | P1b review fixes | Read state captured and updated atomically under the conversation lock; response assembled before mark-read; tagged offers resolved in one batch query; missing tags return `is_available: false`. `cargo test -p haven-web` 211 passed; `cargo test -p haven-db` 19 passed; clippy `-D warnings` clean |
| 2026-11-04 | P0 deploy + smoke | PR #55 merged (`3db144c`); v0.8.61 CD run 41790803720 deployed to Koyeb; production `POST /api/v1/auth/google` with a malformed token returned `401` + `private, no-store`; Android CI passed on `8c7ea0a` |
| 2026-11-04 | P1a/P1b released | v0.8.61 tag includes follow/unfollow, profile follow state, and native thread read/send |
| 2026-11-04 | P1c inquiry send | `api::inquiries` 7 tests passed: create/tag, repeat reuse, self-inquiry 400, unpublished/unknown 404, guest+cookie+invalid bearer 401, blank/oversized 400, `private, no-store`; broadcast parity with the web inquiry path |
| 2026-11-04 | Browser PKCE removed | `cargo test -p haven-web` 217 passed (incl. `browser_pkce_routes_are_gone`); `cargo test -p haven-db` 19 passed; clippy `-D warnings` clean; `mobile_auth_code` dropped by migration 024 |
| 2026-11-04 | PKCE removal deployed | PR #57 squash `3e31da0`; v0.8.62 CD run 41800757633 deployed. Production `haven.apps.mistral.run`: authorize/exchange/callback/assetlinks `404`; malformed `/auth/google` `401` + `private, no-store`; invalid `/me` bearer `401`. Android PR #11 squash `d745405`; APK run 41800518683 passed. Web Google redirect still targets the retired `*.koyeb.app` host — follow-up recorded above. |

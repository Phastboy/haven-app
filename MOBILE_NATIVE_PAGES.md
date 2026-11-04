# Native page API handoff — v0.8.5 live

Backend PR #53 merged at `d2c166a`; release v0.8.5 was published on 2026-11-02
and is live. Production smoke checks returned 200 for public feed, detail and
provider profile, and 401 for anonymous private lists and save/unsave writes.
Authenticated save persistence was tested locally, not against production.
Android PR #9 merged at `0e011cc`; review fixes are on main at `b076605` and
passed build/tests/lint in run 41419917646. Inspect that implementation before
duplicating detail/save work in AI Studio. Device fidelity remains unaccepted.

Base URL: `https://haven-odysseon-d48990aa.koyeb.app`.
Use native Google sign-in (`POST /api/v1/auth/google`) and rotating bearer tokens
for private requests. The browser PKCE routes and `/auth/callback` no longer
exist; the app never opens a browser for authentication.
Do not request backend credentials or change backend code from AI Studio.

## Five prototype screens

| Prototype | Endpoint | Authentication | Response |
| --- | --- | --- | --- |
| `haven_offer_detail` | `GET /api/v1/offers/{slug}` | Public; optional valid bearer adds saved state | Detail object below |
| `haven_provider_profile` | `GET /api/v1/profiles/{handle}?after=...` | Public | `{person, offers: {items, next_cursor}}` |
| `haven_my_offers` | `GET /api/v1/me/offers?after=...` | Required | `{items, next_cursor}`; published offers only |
| `haven_saved_offers` | `GET /api/v1/me/saved?after=...` | Required | `{items, next_cursor}`; still-public offers in save activity order |
| `haven_following` | `GET /api/v1/me/following?after=...` | Required | `{items: [person], next_cursor}` |

`person` contains `id`, `handle`, `display_name`, nullable `bio`, display-ready
`joined`, and nullable `avatar_url`. It never includes private account email.

Offer cards use the existing feed fields: `id`, `slug`, `title`, `category`
(backend slug), `price`, `currency`, `price_display`, `maker_name`, `maker_handle`,
and nullable `cover_url`. Display `price_display` verbatim.

Detail contains `id`, `slug`, `title`, `description`, `category` (display label),
`price`, `currency`, `price_display`, `maker_name`, `maker_handle`, `images`
(ordered URL array), nullable `location` (the public operating-address label),
and `is_saved`. Do not invent neighbourhood/city/country breakdowns or distance.
Private/unpublished/unknown offers return 404. Invalid bearer tokens return 401.

## One-tap save and unsave

- `POST /api/v1/offers/{slug}/saved` saves idempotently.
- `DELETE /api/v1/offers/{slug}/saved` removes idempotently.
- Both require a valid bearer token; cookie-only writes return 401.
- Both return `{ "is_saved": true/false }` after storage confirms the operation.
- Feed and detail must share confirmed state; prevent duplicate pending taps.
- Failures retain the previous state and offer retry. Guests use real sign-in.
- Initial saved state can be read from authenticated detail; general feed cards
  do not yet carry personal saved status. Do not assume an unknown status is a
  confirmed unsaved status. If displaying an outlined Save action while status is
  unknown, issue an explicit idempotent save; never fetch true then silently unsave.

## Pagination and state rules

Pass each opaque `next_cursor` as `after` to the same endpoint. Null means end.
Pages use server ordering and a fixed bounded page size. Unknown query parameters,
malformed/oversized cursors return 400 rather than silently restarting. Saved
cursors are bound to the authenticated person. Authentication failures return 401;
storage failures return server errors. Responses are no-store; private reads and
writes use private/no-store.

Show real loading, empty, error, retry and pending states. Follow each prototype
in light and dark mode. Skeleton shimmers should match gallery, text, identity,
card, and list-row geometry; only show them during actual pending work. Preserve
Back behavior, scroll position, system insets and accessible control labels.

## Current boundaries

My offers exposes published offers using the existing profile listing, not drafts
or a new offer-management API. Saved excludes deleted/unpublished offers; it does
not return unavailable tombstone rows. Following is a read API; native follow,
unfollow, create/edit/delete, inquiry/send, and profile edit writes are outside this
batch. Open supported existing web journeys for those actions. Never simulate a
successful action or make dormant prototype samples reachable at runtime.

Source compile/CI success does not establish deployment or device visual fidelity.

Local HTTP verification now covers all five reads and the authenticated
save/readback/list/unsave round trip: [curl results](https://github.com/Phastboy/haven/blob/main/expts/rust/verification/native-api-v085/RESULT.md).
Forty recorded checks passed on the native-auth-configured local server. Read the
report's remaining fixture, pagination, isolation and media limits before acceptance.

## Next mobile pickup

Google AI Studio owns the corresponding native pages. Provider profile, My offers,
Saved and Following are ready to integrate using this contract. Existing Android
detail/save wiring needs real-device light/dark prototype comparison and signed-in
production save/readback/restart checks. Preserve backend price_display, real
provider navigation and Back during loading/error; these regressions are covered
by NativeOfferDetailReviewTest. Feed skeletons and profile action placement shipped
in PR #8. Appearance belongs in the profile action group; Sign out follows offers.
Messages is an integrated inbox with web thread handoff; native history/send/live
updates still require a separate backend contract. Build/test/lint in CI to preserve
the owner's data budget. Keep backend changes within v0.8.*.

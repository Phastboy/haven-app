# Native page API handoff — v0.8.5 live

> Current inventory (2026-11-06): this document preserves the original page batch
> and its contracts. Native create/edit and messaging are now merged; see
> `MOBILE_OFFER_LIFECYCLE.md` and [historical record](https://github.com/Phastboy/haven-app/blob/e90cfb3d0c79be41856c707a5d69e0a1a418ea32/MOBILE_NATIVE_API_BACKLOG.md). Remaining API
> work and the source-inspected Android tasks are in `MOBILE_NATIVE_API_SWEEP.md`
> and `MOBILE_NATIVE_API_SWEEP.md`; sweep routes are released/deployed in v0.8.65.


Backend PR #53 merged at `d2c166a`; release v0.8.5 was published on 2026-11-02
and is live. Production smoke checks returned 200 for public feed, detail and
provider profile, and 401 for anonymous private lists and save/unsave writes.
Authenticated save persistence was tested locally, not against production.
Android PR #9 merged at `0e011cc`; review fixes are on main at `b076605` and
passed build/tests/lint in run 41419917646. Inspect that implementation before
duplicating detail/save work in the Android app. Device fidelity remains unaccepted.

Base URL: `https://haven.apps.mistral.run`.
Use native Google sign-in (`POST /api/v1/auth/google`) with rotating bearer tokens
for private requests. Do not request backend credentials or change backend code
from the Android maintainer.

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

## Follow and unfollow (released)

Native clients can follow a person without opening the web profile. This is
released since v0.8.61. Android follow controls still need native wiring.

- `POST /api/v1/profiles/{handle}/follow` follows idempotently.
- `DELETE /api/v1/profiles/{handle}/follow` unfollows idempotently.
- Both require a valid bearer token; cookie-only writes return 401.
- Both return `{ "is_following": true/false }` after storage confirms the change.
- The caller cannot follow themself: that returns 400.
- Unknown handles return 404.
- `GET /api/v1/profiles/{handle}` includes `person.is_following` for an
  authenticated viewer. The field is **omitted** for guests, so absence is not
  the same as false; only act on it when a viewer is signed in.
- Repeat taps are safe; the server is idempotent. Keep the previous state on
  failure and offer retry. Responses are `private, no-store`.

## Conversation thread (released; native Android integrated)

Native clients can read and send in an existing conversation without opening the
web thread. The backend contract is released and Android read/send/older-history
are integrated. Apply the focused duplicate-key repair in Android PR #15 before
release; live updates are covered by the separate, undeployed API sweep.

`GET /api/v1/conversations/{id}?after=...` requires the authenticated person and
returns:

```json
{
  "conversation_id": "uuid",
  "other_person": { "id": "...", "handle": "...", "display_name": "...", "avatar_url": null },
  "messages": [
    {
      "id": "uuid",
      "sender_id": "uuid",
      "body": "text",
      "offer": { "title": "...", "slug": "...", "cover_url": null, "is_available": true },
      "created_at": "2026-11-03T12:00:00Z"
    }
  ],
  "next_cursor": "opaque",
  "is_unread": true
}
```

- Messages are oldest-to-newest within a page; pass `next_cursor` as `after` to
  fetch older messages. Null means the start of the conversation.
- `offer` is null when the message tagged no offer. `is_available` is false if the
  tagged offer no longer exists.
- Unknown conversations and conversations the caller is not part of both return
  **404** (IDOR rule; never 403).
- Reading the thread marks it read. `is_unread` reflects the state **before** this
  read, and is scoped to this conversation, not the global inbox badge.
- Response headers are `private, no-store` with `Vary: Authorization, Cookie`.

`POST /api/v1/conversations/{id}/messages` requires a bearer token (cookie-only
writes return 401) and accepts:

```json
{ "body": "Hello", "offer_id": "optional uuid" }
```

- `body` uses the existing `MessageBody` validation: non-blank, at most 2000
  characters; failures return 400.
- `offer_id` is optional. If present it must belong to one of the two participants;
  anything else returns 400. Empty/whitespace is treated as omitted.
- The sender always comes from the token; the client cannot supply a sender id.
- Success returns `{ "message_id": "uuid", "conversation_id": "uuid" }` with
  `private, no-store`.
- Unknown conversations and non-participants return **404**. Malformed JSON and
  malformed cursors return 400.

## Offer inquiry send (released; native Android integrated)

Native clients can start a conversation about an offer without opening the web
offer page. The backend is released since v0.8.62 and Android opens the native
thread after a storage-confirmed enquiry.

`POST /api/v1/offers/{slug}/inquiries` requires a bearer token (cookie-only
writes return 401) and accepts:

```json
{ "body": "Is this still available?" }
```

- `body` uses the existing `MessageBody` validation: non-blank, at most 2000
  characters; failures return 400.
- The sender always comes from the token; the client cannot supply a sender id.
- Only published offers can be inquired about. Unknown and unpublished offers
  both return **404**, so existence is not disclosed.
- Inquiring about your own offer returns 400.
- Repeating the request reuses the canonical conversation for the pair and
  appends another message; it never creates a duplicate conversation.
- The stored message is tagged with the offer, so the native thread, web thread
  and offer inquirers list all report the same context.
- Success returns `{ "conversation_id": "uuid", "message_id": "uuid" }` with
  `private, no-store` and `Vary: Authorization, Cookie`. Open
  `GET /api/v1/conversations/{conversation_id}` next.

## Current delivery boundary

`/api/v1/me/offers` remains published-only. Android My offers management uses
`/api/v1/me/offer-management`, including owned private offers awaiting photos.
Saved still excludes unavailable offers. Follow, native thread read/send,
enquiries and create/edit/photo/location APIs are released.

Profile/avatar/deletion/global-unread/SSE and scoped/proximity discovery are now
released/deployed in v0.8.65 (CD run 42182772275). Their contracts and the exact
remaining Android work are in `MOBILE_NATIVE_API_SWEEP.md` and
`MOBILE_NATIVE_API_SWEEP.md`. Do not reactivate sample screens or simulate success.

Source/CI/deployment success does not establish authenticated end-to-end or
physical-device acceptance. Historical local read/save evidence remains in
[the v0.8.5 report](https://github.com/Phastboy/haven/blob/main/expts/rust/verification/native-api-v085/RESULT.md).

## Next mobile pickup

Read `MOBILE_NATIVE_API_SWEEP.md`. It was written after inspecting Android main
`5d1b681`, including current collection ViewModels. Preserve existing native
create/edit and messaging; implement the documented missing actions/cache/live
updates instead of reactivating sample screens. Merge Android PR #15's thread-key
fix first. Build/test/lint in CI to preserve the owner's data budget. Backend
changes remain within v0.8.*; new API release precedes Android production acceptance.

# Native parity API sweep — wire contract

Updated 2026-11-06. **Implemented/tested on `feat/native-parity-api-sweep`, not yet
merged or deployed.** Live baseline is v0.8.64. AI Studio must not invent fallback
success if an undeployed route returns 404. Backend release must precede Android
production acceptance. Existing lifecycle APIs are in `MOBILE_OFFER_LIFECYCLE.md`.

## Authentication and response handling

Private routes require `Authorization: Bearer <access_token>`. Cookie-only
requests are rejected with 401. Invalid supplied bearer credentials never fall
back to cookies/guest state. Private/personal/proximity data uses
`Cache-Control: private, no-store`; never share it between people.

Reuse serialized refresh rotation and retry once after a genuine 401. Preserve
fields on failure. Do not infer success from a UI timer, locally changed fields,
an empty response or a swallowed exception. Most validation errors are HTTP 400;
upload body overflow can be 413; temporary storage failure is 503. Parse status
and structured error bodies where specified below, rather than exception strings.

## Profile and avatar

`PATCH /api/v1/me/profile` or equivalent `POST /api/v1/me/profile`:

```json
{"display_name":"A Real Person","handle":"real-person","bio":"What I offer"}
```

All fields are optional; omitted fields remain unchanged. `bio: null` or an empty
bio clears it. Display name: 2–50 characters; handle: 3–30 and the domain's existing
slug rules; bio: at most 500. Unknown keys are rejected. Identity is always the
bearer's; do not submit a person/account ID.

Success 200: `{id,display_name,handle,bio,avatar_url}`. A handle collision is 409:

```json
{"error":"handle_taken","field":"handle","message":"This handle is already taken."}
```

The POST alias is provided for the existing Android client, which currently only
allows POST/DELETE mutations. Reload `/api/v1/me/profile` for counts/joined date
and synchronize `/api/v1/me`/session identity after a text edit.

`POST /api/v1/me/avatar`: multipart/form-data, exactly one field named `avatar`,
JPEG/PNG/WebP signatures, nonempty, maximum 5 MB. Existing 6 MB whole-request cap
applies. Returns the same identity object. `DELETE /api/v1/me/avatar` removes the
photo and returns `avatar_url: null`. Removal is idempotent. Cloudinary upload
credentials and transformations remain server-side; production preset validation
is still a separate check. Avatar retries may replace the photo with another
immutable asset; they are not multi-photo attachments. Database triggers queue
old-asset cleanup.

## Offer deletion

`DELETE /api/v1/offers/{slug}` accepts an owned public or private offer. Returns
200 `{ "deleted": true, "id": "uuid", "slug": "..." }`. Unknown and other-owned
offers return identical 404. Deletion uses the existing repository and cleanup
triggers. Client confirms before sending, refreshes owner/public/saved collections
and invalidates cached detail after confirmed success. A timeout is uncertain:
reconcile with a successful owner collection read rather than pretending a 404
alone proves your deletion succeeded.

## Unified discovery

`GET /api/v1/offers` preserves `q`, `category`, `currency`, `min_price`, `max_price`
and `after`, and adds:

| Parameter | Meaning |
| --- | --- |
| `scope` absent or `public` | General eligible public offers |
| `scope=saved` | Bearer's available Saved offers |
| `scope=inquired` | Bearer's available Inquired offers |
| `near` absent or `off` | General ordering, irrespective of browser cookies |
| `near=<choice>` | Explicit reference and nearest-first ordering |

Personal scopes require bearer credentials. Search/category/currency-price rules
are unchanged; prices are integer minor units. Page size remains 20; `after` is
the opaque `next_cursor` returned for that exact traversal. There is no unbounded
result mode or client-supplied person ID. Reset the cursor when actor, scope,
facets or reference changes. Personal/proximity cursors enforce those bindings.

`GET /api/v1/me/inquired` is an alias with Inquired forced; it accepts the same
filters/reference and optional `scope=inquired`, rejecting a different scope.
Existing `/api/v1/me/saved` remains compatible; use the unified feed for scoped
facets/proximity. Owner management `/api/v1/me/offer-management` remains separate
and includes private offers; never replace it with public discovery.

Response remains `{items,next_cursor,categories,currencies}`. Every card retains
`id,slug,title,category,price,currency,price_display,maker_name,maker_handle,cover_url`
and adds `location` (public offer label or null). Bearer pages include `is_saved`
as true/false from a single bounded `saved_among` query; guests omit it. Missing
state is unknown, not confirmed unsaved. The backend does not query per card.

Only an explicit active reference produces `distance_meters`, and only for an
offer with its own location. It is an approximate spherical distance. Unlocated
offers omit distance and sort after located offers. Distant offers are never
excluded by an implicit radius. Server order is authoritative; do not locally
re-sort cached pages, filter out distant offers or hide provider identity.

## Explorer reference search and opt-in

`GET /api/v1/places/search?q=Ibadan` uses the configured geocoder and request-rate
limits. Guest searches are supported; an invalid supplied bearer is rejected.
Response: `{ "items": [{ "label": "...", "near": "opaque-choice" }] }`.

Searching/selecting a candidate does not activate proximity. Only explicit Apply
adds its `near` choice to discovery requests; explicit Off removes it or sends
`near=off`. The server does not infer a native reference from IP, GPS, browser
cookies, an offer's location or the provider profile. Native preference storage
must reflect explicit user actions and the approved device-local retention policy.
Never modify it merely by loading a detail page or following a link.

This reference is different from `/api/v1/me/offer-places/search`: offer forms use
signed, person-bound operating-location selections and warn that that address
will be public. Reference choices are not offer-place tokens and cannot replace
them in create/edit payloads.

## Global unread and live thread invalidations

`GET /api/v1/conversations/unread` returns `{ "unread": true|false }` across the
bearer's whole inbox. It is a boolean, not an unread count. Reading a thread uses
the existing mark-read behavior; refresh this flag afterwards and on foreground.

`GET /api/v1/conversations/{id}/events` requires a bearer and participant access.
Unknown/other-owned threads return 404, guest/cookie-only 401. Response is
`text/event-stream` with private no-store semantics:

```text
event: resync
data: {}

event: changed
id: message-uuid
data: {"conversation_id":"conversation-uuid","message_id":"message-uuid"}
```

Initial connection and a lagged subscriber emit `resync`; `changed` is an
invalidation, not a complete message and not HTML. GET the newest thread page on
connect/resync/change and merge by real server identity. An event ID is not a
history pagination cursor. No replay guarantee is made for `Last-Event-ID`.

Keepalive comments are sent every 20 seconds. Each connection ends after at most
five minutes or the access/device token deadline, whichever is earlier.
Revocation is checked before notifications. Reconnect with bounded backoff and
valid refreshed credentials, then GET/resync; stop on sign-out/background/leave.
The existing broadcaster is process-local, so use a slow foreground-only thread
refresh fallback (for example 30 seconds) to recover from cross-instance/lost
notifications. No background timer or new push-notification service is implied.

# Google AI Studio — complete the existing native Haven journeys

Prepared 2026-11-06 **after inspecting the Android checkout**, not from backlog
checkboxes alone. Backend owns the contracts. Implement the remaining Android
work; do not modify backend behavior or invent success/fallback data.

## Read first and release dependency

- Android inspected main: `5d1b6813ffd7925d2dfa4e766717d291f007c070`
  (`refactor: migrate offers screens to ViewModels`). Main CI 42174560135 passed.
  Fetch/inspect current main before editing; preserve newer work.
- Backend inspected baseline: `4a48247`, deployed as v0.8.64. Native create/edit
  PR #62 and Android PR #14 are merged, not still drafts.
- Remaining API sweep: backend PR #63 is merged and **v0.8.65 is deployed**.
  Release CD 42182772275 passed build/scan/smoke and Deploy to Koyeb; logs confirm
  the update was executed. Live `/me/inquired` returned 401 anonymously and
  `/offers?near=off` returned 200. Read `MOBILE_NATIVE_API_SWEEP.md` for exact
  contracts. APIs are available for integration; authenticated/device acceptance
  remains separate. Never turn a request failure into simulated success.
- Native create/edit contracts: `MOBILE_OFFER_LIFECYCLE.md`; existing page and
  follow/thread contracts: `MOBILE_NATIVE_PAGES.md`.
- Native Google sign-in succeeded on the owner's Android simulator. Physical
  device acceptance is deliberately scheduled later; don't claim it has passed.
- Run Android build/tests/lint in GitHub CI, not local Gradle downloads, per
  `AGENTS.md` and the owner's data budget. Never request backend credentials.

## Focused prerequisite already prepared: thread rendering

Inspection found two LazyColumn items named `history_status` in
`NativeConversationThreadScreen.kt`, before and after messages. This is fixed in
[Android PR #15](https://github.com/Phastboy/haven-app/pull/15), commit `8e93f13`.
It retains one older-history control and a distinct bottom spacer. CI 42179102446
passed, including a populated-thread scrolling regression. Merge/rebase that fix
first; do not duplicate it or treat its pre-merge baseline as accepted runtime.

## What exists — preserve and extend it

| Existing implementation | Actual source and behavior |
| --- | --- |
| Native Google authentication | `auth/HavenAuthClient.kt`, `ui/model/UserSession.kt` (`SessionManager`); rotating bearer/refresh credentials and real `/me` |
| Discovery and detail | `data/HavenOffersRepository.kt`, `ui/model/OffersViewModel.kt`, `OfferDetailScreen.kt` (`NativeOfferDetailScreen`); title/category/currency-price filtering, paging, real detail/save/enquiry |
| Provider profile and collections | `HavenNativePagesRepository.kt`; dedicated `MyOffersViewModel`, `SavedOffersViewModel`, `FollowingViewModel` and repositories were added on current main |
| Native create/edit | `HavenOfferEditorRepository.kt`, `OfferEditorViewModel.kt`, `NativeOfferEditorScreen.kt`; photos, location, private-save recovery, idempotent retries and native navigation |
| Owner collection | `MyOffersScreen.kt` uses management IDs for Edit and distinguishes private offers; preserve private-offer recovery |
| Messaging | `data/messaging/HavenLocalFirstMessagingRepository.kt`, `HavenProductionMessagingTransport.kt`, `ConversationThreadViewModel.kt`, native thread and enquiry composer; real API reads/sends and older-page loading |
| Navigation | `ui/screens/OffersScreen.kt` is the current root; create/edit and conversation destinations already exist |

Do not rebuild those flows, restore `MockMessagingTransport` to production, or
replace current ViewModels with old prototype state. Runtime screens use server
responses; fake fixtures belong only in tests.

## What the inspection found missing

All paths below are relative to `app/src/main/java/com/example/`.

| Gap | Evidence in current source | Required integration |
| --- | --- | --- |
| Follow/unfollow | `OffersScreen.kt` provider follow callback and Following action still call `openWeb("/profiles/...")`; `Person`/`parsePerson` omit `is_following` | Parse confirmed follow state and use the existing POST/DELETE follow APIs |
| Profile text editing | `ProfileScreen.kt` opens `/profile/edit`; `EditProfileScreen.kt` defaults to Adaeze/sample bio/avatar and calls `onSaveSuccess()` after local validation without a request | Real profile-edit destination, state, repository and writes; no simulated success |
| Avatar editing | Existing profile editor changes local photo state/sample image; no native avatar repository route | Real system picker, multipart avatar upload/removal, progress and error/retry |
| Offer deletion | No delete transport/action in `MyOffersRepository`, owner ViewModel or editor; `OfferManagementScreens.kt` has sample inquiry data and a callback-only delete scaffold | Add deletion to actual owner UI, using real title/slug and explicit confirmation |
| Global unread | `OffersScreen.kt` counts `messagesState.items.count { it.isUnread }` | Fetch the global unread boolean; do not present a loaded-page count as the total |
| Live thread updates | `MessagingTransport` has get/send only; ViewModel refreshes on open/resume and has no stream | Connect JSON SSE invalidations, reconnect/resync and foreground fallback |
| Feed saved state | `HavenOffersRepository.fetch` makes anonymous URL requests; `FeedOffer` and parser have no `is_saved` field | Optional authenticated feed reads and nullable, confirmed saved state |
| Saved-state cache round trip | `FeedOfferEntity.isSaved` exists but conversion into `FeedOffer` drops it, and fresh cache writes do not supply it | Preserve confirmed state in account-scoped metadata, including offline reconstruction |
| Native proximity/Inquired | `OffersQuery` has no scope/reference; `computeQueryKey` uses only q/category/currency/price; no native reference destination | Explicit collection/reference state, real place search, scoped paging and safe cache keys |
| Extra thread offer tags | ViewModel has pending attachment fields, but production mutation sends only body/client ID; no caller wires its attach selector | Wire at most one actual `offer_id` when offering that action; do not pretend a slug was attached |
| Visible paging failures | Provider paging swallows exceptions in `OffersScreen.kt`; collection ViewModels clear loading flags without a visible paging error | Keep real rows and show a retryable error; no silent failure |

## Implementation pass A — connected profile and ownership actions

### Follow and Following

Use `GET /api/v1/profiles/{handle}` → `person.is_following` for a bearer. Guest
responses omit it. `POST`/`DELETE /api/v1/profiles/{handle}/follow` return
`{"is_following":true|false}` and are idempotent. Guest actions request sign-in;
own profile hides self-follow. Disable repeat taps while pending, keep prior
confirmed state on error and refresh Following/profile counts after success.
Following membership itself confirms a row is followed; its Unfollow action can
issue explicit DELETE without guessing a toggle. Replace both browser handoffs.

### Profile text and avatar

Use `prototype/haven_edit_profile` and `prototype/haven_my_profile` for structure,
Haven theme tokens for light/dark, and real identity loaded from `/me/profile`.
Remove sample defaults, fixed avatar URLs and the local-only Save success path.
The existing `EditProfileScreen` is a visual scaffold, not a live editor.

POST `/api/v1/me/profile` works with the current mutation helper; PATCH is also
supported. Preserve edited values on error; render 409 `handle_taken` at the
handle field. Extend typed HTTP error handling so callers can inspect status and
body instead of parsing an exception's text. Update/reload session identity and
profile after confirmed success, including the new handle used for navigation.

Avatar upload is multipart `avatar`, not `image`; current
`authenticatedImageUpload` hardcodes `image` for offer photos. Generalize it
without breaking stable offer upload identities. Use the existing system picker
pattern and retained read permissions, real URI preview, JPEG/PNG/WebP up to 5 MB,
and DELETE `/api/v1/me/avatar` for removal. Show success only after the server
responds, then reload the actual profile/avatar URL. Keep Cloudinary keys and
preset configuration out of the Android project.

### Offer deletion

Add Delete to the real My offers/editor management flow. The manage-offer
prototype may guide placement, but its sample inquiries/status controls are not
runtime data. Confirm using the actual offer title; call DELETE
`/api/v1/offers/{slug}`. On success update `MyOffersViewModel`, discovery/detail,
Saved and relevant caches, and leave deleted detail. Failure keeps the action
retryable. A lost response requires reconciliation with a successful owner list
read; do not infer success from an arbitrary 404 or silently navigate away.

## Implementation pass B — complete discovery, Saved and Inquired

Extend the current repository/ViewModel path rather than creating a second fake
feed. Use `/api/v1/offers` with `scope=public|saved|inquired`; `/me/inquired` is an
explicit alias. Preserve existing title/category/price filters, server order,
provider identity and cursor paging. Personal collections require bearer auth.

Use `prototype/haven_offers_feed`, `haven_saved_offers` and
`haven_reference_location`. Add a real reference selection destination or sheet:
search `/api/v1/places/search?q=...`, show returned labels, retain a selected
candidate, and explicitly Apply/Change/Off. Search alone does not activate
proximity. First-use discovery is general. Preserve only explicitly chosen
preferences under the approved device-local expiry policy; never silently enable
from GPS, IP, an offer's public address or a provider profile. Off omits `near` or
sends `near=off`. Offer-form place tokens and explorer reference choices are
different contracts and must stay separate.

With active `near`, render server `distance_meters` where supplied; missing means
unknown/unlocated, not zero. Do not radius-filter distant offers or re-sort pages.
Reset pagination on changed actor/scope/facets/reference; append one page at a
time. Keep the reference when returning from detail; do not mutate preference
because a detail link carries another reference.

### Cache changes are required, not optional polish

`computeQueryKey` currently omits actor, scope and reference. Include all of them
plus normalized facets. Private Saved/Inquired memberships and saved flags must
never cross accounts or appear in guest caches. Clear/invalidate account data on
sign-out/change before rendering another account.

`FeedOfferEntity` is globally keyed by offer ID: its single `isSaved` field cannot
represent two actors, and its single `distanceMeters` cannot represent two
references. Store these as actor/query-specific metadata or equivalent safe
relations; merely changing the list key does not fix shared-row contamination.
Preserve authoritative server ordering through the cache. Migrate existing cache
rows safely; they do not establish saved state or proximity context.

Feed `is_saved` is true/false only when bearer-confirmed; omitted is unknown.
Reconcile feed/detail/Saved after save/unsave. An outlined visible Save action
must explicitly POST when state is unknown, never infer DELETE. Keep nullable
state through serialization and Room reconstruction. Preserve the existing
available-Saved behavior: unavailable offers are omitted.

## Implementation pass C — unread, live threads and existing attachment limits

Use `/api/v1/conversations/unread` for a global unread indicator. The API returns
a boolean, not a count; render a dot/appropriate nonnumeric badge. Refresh after
thread reads, enquiry/send and foreground return, using the current actor only.
Do not derive the whole-inbox result from a paginated local subset.

Connect `/api/v1/conversations/{id}/events` only while that thread is visible and
the app is foregrounded. SSE requires an Authorization header, not a token in a
URL. Do not hold `HavenAuthClient`'s synchronized refresh/mutation lock while
blocking on the event stream. Acquire credentials safely, then read independently;
keep refresh rotation serialized. Close old streams on navigation/account change.

Treat `resync`/`changed` as invalidations and GET the newest thread page; do not
append them as messages or render web HTML. Reconnect on EOF/expiry/network loss
with bounded backoff, refresh once on a real 401 and always resync. Honor the
process-local broadcaster limitation with a slow foreground-only fallback, e.g.
30 seconds. No background polling service or new push behavior is requested.

Keep older history paginated on upward scroll, its cursor distinct from event
IDs, and the visible message anchored while prepending. Preserve pending/failed
messages and stable delivery IDs. Apply server-ID reconciliation and ACK updates
transactionally so an event racing a send cannot duplicate a row or overwrite a
confirmed SENT row with an earlier SENDING snapshot. Ensure messaging cache and
`isFromMe` values cannot carry another account's pending state across a switch.

If wiring the existing offer attachment affordance, send the actual optional
`offer_id` UUID with the required nonblank message body. Backend allows one offer
tag owned by either participant. Do not reproduce the thread prototype's
multi-offer attachment sample: it is not a backend capability. Keep the existing
initial offer-enquiry tagging behavior. Do not expose a selector that silently
drops its attachment in transport.

## Completion and review

Use small reviewable commits against current main, preserving the recent
collection-ViewModel architecture. Add meaningful tests for confirmed writes,
409 handling, deletion isolation/failure, owner/reference cache separation,
nullable saved state, reconnect/dedup, older-page anchoring and real loading/error
navigation. CI runs `assembleDebug`, `testDebugUnitTest`, `lintDebug`; retain its
reports. Also check light/dark prototypes for obvious layout inconsistencies.

Return a short implemented/pending matrix with source paths and CI evidence.
Do not claim physical-device or authenticated production acceptance, cloud preset
verification or genuine offer publication on the basis of source/CI. Those launch
checks remain owner-led later. No commitments/payments/ratings/people search or
other 0.9/new product behavior belongs in this handoff.

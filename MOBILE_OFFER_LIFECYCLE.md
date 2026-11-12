# Native offer lifecycle — implementation contract

Status updated 2026-11-06: backend PR #62 and Android PR #14 are merged.
Backend v0.8.64 deployed successfully (CD run 42084317790). Authenticated
production lifecycle and physical-device acceptance remain separate from CI.

All routes require valid native bearer credentials, reject cookie-only requests,
and return `Cache-Control: private, no-store`. IDs refer to owned offers; unknown,
malformed and other-owned IDs return 404. Every offer write uses repository
ownership checks. Public discovery and published-only collection contracts stay
unchanged.

| Method and path | Purpose |
| --- | --- |
| GET `/api/v1/me/offer-form` | Categories, currencies, field limits, private recent places and suggested place token |
| GET `/api/v1/me/offer-management?after=...` | Owner's offers, including those without photos, 20 per page |
| POST `/api/v1/me/offer-management` | Idempotent creation; no photo means private |
| GET `/api/v1/me/offer-management/{id}` | Editable fields, image identities/URLs and signed selected location |
| POST `/api/v1/me/offer-management/{id}` | Full form update |
| POST `/api/v1/me/offer-places/search` | `{ "query": "..." }` → `{ "items": [{ "token": "...", "label": "..." }] }` |
| POST `/api/v1/me/offer-management/{id}/images` | Multipart field `image`; UUID `Idempotency-Key` header |
| POST `/api/v1/me/offer-management/{id}/images/remove` | `{ "image_id": "uuid" }`; retain at least one photo |
| POST `/api/v1/me/offer-management/{id}/images/reorder` | `{ "image_ids": ["uuid", "..."] }`; complete order, first is cover |

Create/update JSON: `title`, `description`, `category` (slug), `price` (integer
minor units represented as a string), `currency`, optional `place_token`, and
`idempotency_key` (required UUID on create). Title: 3–50 characters; description:
10–2000; nonnegative price; supported currency/category only. Create retries must
reuse their original key. A successful create may be returned after a lost
response, so clients must finish any later edits with the update route.

Omitting `place_token` on update retains the location. Empty or `none` explicitly
removes it. A nonempty token must be server-issued, unexpired and bound to the
current person. Offer location is public and does not activate or change the
explorer's proximity reference. Search remains server-side and rate limited.

Editor responses include `id`, `slug`, editable fields, `is_public`, `location`
(null or `{token,label}`) and `images` (`{id,url}`). The management collection uses
the existing feed-card shape with an additional `is_public` boolean. Private
cards open the editor rather than unavailable public detail.

Photos: JPEG/PNG/WebP only, maximum 5 MB per image, five per offer, existing
6 MB request-body limit. Files pass signature/size checks and use the existing
server-signed Cloudinary adapter. Upload retries reuse their UUID. Image
normalization is delegated to the configured Cloudinary incoming preset; no
server-side image decoding dependency was added. The production preset's resize
and metadata-removal settings still require verification: usable preset
configuration was not available locally.

Android sources are the gallery/file picker and the system camera app through
`ActivityResultContracts.TakePicture`. Both produce the same ordered local
`EditorImage` entries and share preview, count, reorder, removal and upload.
Camera output is written by the system camera into an app-private cache file
exposed only through the dedicated `${applicationId}.captures` FileProvider
authority; the app declares no CAMERA or storage permission. Only the capture
file name survives recreation, and cancelled, empty or failed captures are
deleted rather than added. The app never appends location coordinates; camera
images use the same server-side upload and privacy processing as selected
images.

Android follows `prototype/haven_create_offer` and `prototype/haven_edit_offer`
for structure, adapted to backend bounds and location selection. Photo edits are
staged locally until submission. Submission is a sequence of confirmed writes,
not one atomic transaction: creation can leave a private offer; adding its first
photo makes it public. Failed saves preserve entries and report that some changes
may already have saved. Retrying finishes the sequence. Earlier confirmed edits
are not rolled back when abandoning retained entries. Android keeps URI read
permissions, form data and creation/upload identities across recreation.

Before Android production acceptance: verify the Cloudinary preset, test native
create/edit against the deployed backend, and complete physical-device acceptance.
For later parity APIs, follow the separate backend-first release gate in
`MOBILE_NATIVE_API_SWEEP.md`.

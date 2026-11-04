# Mobile public Offers feed — 0.8.2

Backend branch: `feat/mobile-offers-feed-api`.
Android branch: `feat/backend-offers-feed` in `phastboy/haven-app`
(local checkout: `h-mobile`).

This slice exposes existing public discovery. The next backend patch after the
owner-reported/deployed 0.8.1 is 0.8.2. The previous workspace manifest and lock
entries still said 0.7.0; they are reconciled to 0.8.2 here. This document is an
implementation record, not evidence that 0.8.2 has been published or deployed.

## Contract

`GET /api/v1/offers` is public and needs no bearer token. It returns up to 20
public offers in Haven's existing newest-first order. Offers without a published
image are excluded by the existing repository query.

Optional query parameters:

| Parameter | Meaning |
| --- | --- |
| `q` | Existing validated title search; not maker or category text search. |
| `category` | Exact category slug from the returned vocabulary. |
| `currency` | Supported currency, required with a price bound. |
| `min_price`, `max_price` | Nonnegative inclusive integer bounds in Haven's stored minor units. At least one bound is required with currency. `max_price=0` selects free offers in that currency. |
| `after` | Opaque `next_cursor` from the preceding page, with the same criteria. |

The response contains `items`, nullable `next_cursor`, `categories` (all eleven
backend slugs/labels), and `currencies`. Each item contains `id`, `slug`, `title`,
`category`, integer `price`, `currency`, `price_display`, `maker_name`,
`maker_handle`, and nullable `cover_url`. Price display matches Haven web;
Android must not infer currency conversion or reinterpret the stored amount.

Malformed criteria/cursors and unknown parameters return 400. Invalid input
never silently becomes an unfiltered feed. Responses use `Cache-Control:
no-store`. The API does not expose personal collections, saved status, account
email, or session data. It ignores browser proximity preferences because this
slice is explicit general discovery; proximity parameters are not accepted yet.

## Android behavior

The feed uses real HTTP requests, backend categories, title search, price bounds,
and cursor paging. Loading reflects a pending request. An empty response is
empty; errors allow retry and never fall back to sample content. Changing
criteria cancels old request delivery and resets paging. A paging error retains
the already loaded real offers.

The layout follows `prototype/haven_offers_feed`: pinned rounded search and
filter controls, category rail, two-column square image cards, title/maker/price
lines, and bottom navigation. Missing/failed images show an unavailable-image
state instead of bundled product photographs. No fictitious proximity label,
distance, unread badge, favorite status, or successful write is displayed.

Offer detail/save, location discovery, messages, and profile navigation use real
Haven web pages until their native APIs/screens are integrated. The heart opens
the real offer page to save there; it does not toggle a local favorite. Account
sign-out still revokes the native device session. Existing prototype screen
files can remain for subsequent work, but the integrated feed does not navigate
to their simulated content.

Android retains `versionName=1.0` and advances `versionCode` from 1 to 2 for the
next distributed APK. Version codes must increase for subsequent distributed
builds; API patch numbers and Android version codes are separate sequences.
The signing certificate still determines whether an APK can update an existing
installation and which Android OAuth client native Google sign-in can use.

## Verification and handoff

Run the backend contract tests and lints:

```sh
cargo test -p haven-web app::api::offers
cargo clippy -p haven-web --all-targets -- -D warnings
```

The HTTP tests cover visibility, real query intersections, free offers, empty
categories, a second page, category vocabulary, malformed filters, and rejecting
personal scope. Android tests cover backend ordering/paging, changed criteria,
failed requests/retry, invalid price input, and parsing without dummy images.

After backend deployment, AI Studio should run the updated app against the live
endpoint, compare the native feed with its prototype, and test guest and signed-in
browsing, search, categories, price bounds, empty results, retry, and paging. The
backend is maintained in Haven; AI Studio owns Android/device verification.
Record its actual screenshot and device outcomes before claiming visual or live
end-to-end verification. No load benchmark is required for this focused slice;
existing release performance debt remains open.

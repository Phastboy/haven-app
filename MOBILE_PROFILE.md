# Native My profile and contrast correction — 0.8.3

Backend branch: `feat/mobile-profile-api`. Android branch:
`feat/real-profile-and-theme-fix` in `Phastboy/haven-app`.

## Contract

`GET /api/v1/me/profile` accepts the same bearer/cookie credentials as `/api/v1/me`.
Bearer credentials take precedence; invalid bearer credentials cannot fall back
to a valid browser cookie. Guest/invalid credentials return 401. Responses are
`private, no-store` and contain no account email or token data. A supplied actor
query parameter cannot select another person's data.

Fields: `id`, `handle`, `display_name`, nullable `bio`, `joined` (existing Haven
date label), nullable `avatar_url`, `counts`, and `offers` (up to four public
offer cards using the existing feed card contract). Counts are actual DB values:
`offers` includes owned unpublished offers, `saved` counts saved offers still
publicly eligible, and `following` counts followed people. No preview or count
is inferred from the global feed. No migration is required.

## Android

Profile opens natively from the bottom navigation and follows
`prototype/haven_my_profile`: profile header/public view, identity/avatar/bio,
edit action, grouped collection rows with real counts, and an offer preview.
Appearance retains real persisted System/Light/Dark selection. Sign-out uses the
existing device-session revocation. Edit/public/collection/offer actions open
their actual Haven web pages until those native integrations exist. No sample
identity, sample biography/photo, spec-state chips, fake updated snackbar,
invented counts or simulated child screens remain in this profile runtime.

Profile credentials share one auth client with sign-in/restoration. Refresh
rotation, authenticated reads and sign-out are serialized to avoid concurrent
reuse of a rotating refresh token. Profile responses must match the signed-in
person; account changes/sign-out clear the previous profile. Returning from web
management reloads real profile data without inventing a successful edit.

Android versionCode advances to 4; versionName remains 1.0, independent from the
backend patch version and still eligible to change to 0.0.0 for production.

## Screenshot finding and shared offer card

The owner supplied a dark-mode screenshot showing unreadable card title/price
and filter controls. Theme tokens existed, but HavenTheme did not provide a
default foreground through Material3 LocalContentColor. The correction supplies
the palette text color centrally, and shared card title/price colors are explicit
theme tokens. Both palettes must be verified; token use alone is not a pass.

`ui/components/FeedOfferCard.kt` is reusable by feed and profile. It supports an
optional `distanceMeters` value and suppresses absent, negative or nonfinite
values. Current general discovery/profile responses do not supply distances;
the app must not calculate or invent one. Native proximity API integration is
still pending. Distances require a selected reference and real backend results.

## Verification

Backend tests cover guest/invalid bearer access, current-person identity,
private headers, SQL count scoping and unpublished saved offers. Android tests
cover profile errors/retry, cross-account rejection, persisted theme selection,
optional distance, and rendered card title pixels in both themes. CI uploads
screenshots with test/lint reports for inspection. Test fixtures are isolated
from runtime sources.

Build/lint success is not a full prototype-fidelity acceptance. Compare the
actual device profile/feed with the prototypes in both themes, including long
names/bios, absent avatar/bio, counts, preview cards, navigation and insets.
AI Studio owns Android/device testing; backend changes remain in Haven.

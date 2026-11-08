# Haven Android v0.0.3

Owner-authorized patch for #25. versionName 0.0.3 / versionCode 13, unchanged
package and signing identity. Provider catalogue reuses FeedOfferCard and shared
OfferCardSkeleton through OffersGridSkeleton, with centred identity, Follow-first
actions and equal two-column rows including odd tails. Owner profiles hide save;
viewer saves use existing authenticated writes and rollback feedback. No distance
is shown without explicit reference data. Follow/paging errors remain visible;
paging is guarded and deduplicated, and stale actor/provider results are ignored.

Focused Robolectric tests cover layout/action order, shared save callback, owner
visibility, loaded-error retry, shared skeleton and light/dark captures. CI must
pass strict debug/release builds/lint, tests and signer/provenance verification.
Rendered capture inspection and physical-device/TalkBack checks are distinct.
Acting maintainer/agent reviews remaining device prerequisites on 2026-11-09;
no physical-device acceptance is inferred. Automatic release workflow attaches
APK, SHA256SUMS, manifest.txt and apk-provenance.txt. Verify delivered source,
version, signer and asset identity; do not manually upload substitute APKs.

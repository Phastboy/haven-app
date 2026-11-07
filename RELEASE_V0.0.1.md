# Android 0.0.1 — UI and startup corrections

Type: patch / fix. Version name `0.0.1`, versionCode `11`, following source
`0.0.0 / 10`. Backend remains independent at its existing release.

## Changes

- Flat safe-area containers and welcome/sign-in actions remove unwanted outlines.
  Decorative avatar/attachment/message outlines, inbox/profile dividers, floating
  control shadows and toast shadows are also removed. Existing input boundaries,
  selection-state boundaries and focus/interaction feedback remain.
- Startup waits for refresh plus verified identity before showing authenticated
  Offers. No saved credentials or confirmed refresh rejection opens Welcome.
  Network/server failure with retained credentials shows retry; it never clears
  credentials merely because the device is offline. Back from the authenticated
  feed root exits the activity rather than opening Welcome.
- Offer cards use the owner-reviewed November 7 composition: square cover with
  12dp corners, save heart, no border/shadow, title/price/provider hierarchy.
  Exactly three single-line rows are enforced. Optional server distance shares
  the price row only with active proximity, using remaining width. Price wins;
  distance ellipsizes first. No text shrinking or additional rows. Missing/loading
  images retain square geometry; skeletons match the three-row layout.
- Filter, proximity and create use one custom component with 40dp visible circles,
  18dp icons and independent 48dp touch targets, preserving their actual actions.

## Acceptance

- [ ] Exact PR commit passes debug/release builds, unit tests and both lints.
- [ ] Review generated light/dark screenshots at 158dp and 173dp card widths,
      including 2x text and large prices; these are source-rendering checks.
- [ ] Samsung/device: startup, Google session reopening, offline retry, safe edges,
      keyboard/navigation modes, card imagery and all three header controls.
- [ ] Inspect APK version, signer and provenance before distribution.

Merging produces CI candidate artifacts. It does not publish a release or prove
production/device acceptance. Existing signing identity is preserved. Tag/release
publication is a separate owner action. Historical 0.0.0 acceptance debt remains
in RELEASE_V0.0.0.md; this patch does not mark those gates passed.

Next, separately: inventory and address the 65 owner-reported warnings using
current CI reports; see NEXT_AGENT.md. No blanket warning suppressions are added.

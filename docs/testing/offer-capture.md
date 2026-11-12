# Offer location and camera device checks

Follow repository AGENTS.md and the backend strict-scrutiny/check-register policy.
NOT RUN: record app SHA/version/signer, backend revision, phone/Android, camera
app and permission state with each result. Acting maintainer/executing agent owns
prerequisite review 2026-11-13; these are acceptance requirements, not a test pass.

- Create and edit: capture after tap only, denied/coarse/precise/revoked access,
  services off, timeout/stale fix, offline signing and retry. Previous place stays
  intact on failure. Review shows coordinates and honest accuracy.
- Accept/reject/recapture; replace with manual place or remove during pending
  acquisition/signing. Late results must not overwrite the newer choice.
- Leave/discard, rotate, stop/resume, kill/reopen process and switch account/offer.
  No automatic reacquisition or publication; saved draft selection restores.
- Submit the reviewed place, then confirm saved/public location through matching
  backend data and UI. Removal works. Capture alone creates no offer.
- Camera success/cancel/empty/unavailable/oversize; gallery still works. Check
  five-photo limit, mixed sources, preview/order/remove, upload failure and retry.
- Rotate/process interruption while camera is open; return to same draft and
  reject old account/editor results. Submit stays blocked while photo is pending.
- Camera provider and APK-update installer both work in the same installed app.
- Both themes, large fonts and TalkBack: controls/feedback readable and reachable.

No new release authorized; feature preparation/cadence and all release gates apply.

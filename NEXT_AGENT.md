# Start here — Android native parity handoff, 2026-11-06

Read `MOBILE_AI_STUDIO_HANDOFF.md`, `MOBILE_NATIVE_API_SWEEP.md` and
`MOBILE_NATIVE_API_BACKLOG.md` first. This pickup supersedes older browser-thread
and native-create/edit-draft statements in the historical project records.

## Backend release is available

Backend PR #63 is merged and **v0.8.65 is deployed**. Release action 42182772275
completed successfully, including Build/Scan/Smoke and Deploy to Koyeb. Logs
confirm an actual service update, not an internal skip. Live read-only checks:
`/api/v1/me/inquired` returned 401 anonymously; `/api/v1/offers?near=off` returned
200. The remaining profile/avatar/delete/unread/SSE and scoped/proximity discovery
contracts are available for Android integration. Authenticated end-to-end and
physical-device acceptance remain separate from deployment confirmation.

## Current Android implementation and exact remaining work

The handoff was based on main `5d1b6813ffd7925d2dfa4e766717d291f007c070`, including
its dedicated My offers/Saved/Following ViewModels; CI 42174560135 passed. Inspect
newer main before editing and preserve subsequent work. Native Google sign-in,
feed/detail/collections, create/edit/photo/location, enquiry/thread sends and
older-history paging are integrated. Backend #62 and Android #14 are merged.

Merge focused thread-key fix PR #15 first: CI 42179102446 passed, including a
populated-thread scrolling regression. Do not duplicate that repair.

Still implement native follow/unfollow; real profile/avatar editing; confirmed
offer deletion; account/reference-safe discovery caches; Public/Saved/Inquired
and explicit proximity controls; authoritative global unread; live thread
resync/reconnect; and visible paging failures. The handoff names actual files,
existing scaffolds and protocol limits. Existing EditProfileScreen samples and
simulated success callbacks are not a production editor.

This docs bundle is in draft PR #16 so Google AI Studio can read it from this
repository. AI Studio implements Android only and must never request backend
credentials or change backend contracts.

## Scope and acceptance

This is existing v0.1–v0.8 parity, not a new product stage. Follow HavenColors,
Inter typography and the prototype directory. Do not fabricate runtime offers,
messages, distances or success states; do not invent multi-offer tags, ratings,
commitments, payments or push behavior. General discovery is the default;
proximity is explicitly applied/changed/cleared and remains independent of an
offer's public operating location.

Use CI for build/tests/lint, not local Gradle downloads, per the owner's data
budget. Physical-device testing is owner-led later. The owner already confirmed
native Google sign-in on the Android simulator. Production preset configuration,
authenticated full journeys, visual acceptance and genuine offer publication
remain separate launch checks. Read actual versionCode before distributing an
APK; CI debug signing is not production signing.

Live host: `https://haven.apps.mistral.run`. Native authentication is Google
ID-token exchange plus rotating bearer/refresh credentials. Browser PKCE,
`/auth/callback` and assetlinks are retired; don't restore browser sign-in as a
fallback. Package: `com.aistudio.haven.xvykpt`; each signer requires a matching
Android OAuth client. Read `prd.md`, `MOBILE_OFFERS_FEED.md` and `PROJECT_INFO.md`
for retained product context, with this dated pickup taking precedence for status.

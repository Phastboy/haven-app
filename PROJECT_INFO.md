> Current release status: see `RELEASE_V0.0.5.md` and `NEXT_AGENT.md`.
> Native threads and offer lifecycle are integrated; production/device acceptance
> remains separate from implementation and automated verification.

# Haven mobile project info

## Identity

| Field | Value |
|---|---|
| App name | Haven |
| Repository | Phastboy/haven-app (local checkout: h-mobile) |
| Platform | Android |
| Implementation | Kotlin, Jetpack Compose, Material 3 |
| Status | Native v0.1–v0.8 parity integrated; release acceptance tracked in NEXT_AGENT.md |
| Parent project | https://github.com/phastboy/haven |
| Android application ID | `com.aistudio.haven.xvykpt` (installed package identity) |
| Android namespace | `com.example` (current namespace) |
| Version name / code | `0.0.5` / `15` (authorized patch release target) |
| Minimum Android version | Android 7.0 / API 24 |

## Repository description

Native Android prototype for Haven: discover offers, see the people behind them,
and start non-binding conversations. Built with Kotlin and Jetpack Compose.

## App copy

**Tagline:** Find offers. Know the person behind them.

**Short description:** Discover offers, know the people behind them, and start a conversation.

**Product description:** Haven is a place to discover what people offer and get to
know the people behind those offers. Browse offers, explore a person's catalogue,
and express interest through private conversation. An inquiry starts a discussion;
it does not create a commitment, reservation, or agreement.

**Current capability disclosure:** Native Google sign-in, offer discovery/detail,
create/edit/photo/location, profile/avatar editing, saves/follows, inquiries and
native messaging are integrated with the live Haven backend. Release CI verifies
builds and automated regressions; authenticated production and physical-device
acceptance remain separate. See NEXT_AGENT.md and RELEASE_V0.0.5.md.

The product description is reusable copy, not a claim that every journey is live
in this prototype. Use the current capability disclosure above when describing this build.

## Integration boundaries

- The main Haven repository owns the product roadmap and backend behavior.
- Live backend: `https://haven.apps.mistral.run`; auth, `/api/v1/me`, `/api/v1/offers`, `/api/v1/me/profile`, and `/api/v1/conversations` are integrated. The legacy `haven-odysseon-d48990aa.koyeb.app` host no longer serves the app.
- Every screen selected for integration must use verified backend behavior. Samples, fake timers, and fabricated successful writes are forbidden in that runtime path.
- People search, agreement/closing, and payments are not current app
  capabilities. Do not advertise them as available.
- No Gemini feature or embedded-frame permission is required by the current UI.

Package identity, signing keys, store listing links, support contact, privacy
policy, and release versioning must be settled for publication. This document
does not invent those details or change existing application identity.

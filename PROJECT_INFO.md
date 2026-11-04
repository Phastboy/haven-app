# Haven mobile project info

## Identity

| Field | Value |
|---|---|
| App name | Haven |
| Repository | Phastboy/haven-app (local checkout: h-mobile) |
| Platform | Android |
| Implementation | Kotlin, Jetpack Compose, Material 3 |
| Status | Real sign-in/feed/profile/Messages inbox; native thread integration not yet selected; inspect NEXT_AGENT.md and MOBILE_MESSAGES.md |
| Parent project | https://github.com/phastboy/haven |
| Android application ID | `com.aistudio.haven.xvykpt` (current scaffold identity) |
| Android namespace | `com.example` (current scaffold namespace) |
| Version name / code | `1.0` / `6` (independent of backend 0.8.4; visible version may start at 0.0.0) |
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

**Current capability disclosure:** Sign-in, Offers feed, profile, and Messages inbox use the live Haven backend. Selecting a conversation opens its real web thread; native thread sending, collection management, and offer detail are not integrated. Remaining prototype files must not be used as runtime fallbacks. Read `NEXT_AGENT.md` first for current scope and outstanding device checks.

The product description is reusable copy, not a claim that every journey is live
in this prototype. Pair it with the prototype disclosure wherever the app's
current capabilities are described.

## Integration boundaries

- The main Haven repository owns the product roadmap and backend behavior.
- Live backend: `https://haven.apps.mistral.run`; auth, `/api/v1/me`, `/api/v1/offers`, `/api/v1/me/profile`, and `/api/v1/conversations` are integrated. The legacy `haven-odysseon-d48990aa.koyeb.app` host no longer serves the app.
- Every screen selected for integration must use verified backend behavior. Samples, fake timers, and fabricated successful writes are forbidden in that runtime path.
- People search, proximity, agreement/closing, and payments are not current app
  capabilities. Do not advertise them as available.
- No Gemini feature or embedded-frame permission is required by the current UI.

Package identity, signing keys, store listing links, support contact, privacy
policy, and release versioning must be settled for publication. This document
does not invent those details or change existing application identity.

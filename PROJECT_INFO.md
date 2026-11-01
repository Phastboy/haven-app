# Haven mobile project info

## Identity

| Field | Value |
|---|---|
| App name | Haven |
| Repository | h-mobile |
| Platform | Android |
| Implementation | Kotlin, Jetpack Compose, Material 3 |
| Status | UI prototype; backend integration pending |
| Parent project | https://github.com/phastboy/haven |
| Android application ID | `com.aistudio.haven.xvykpt` (current scaffold identity) |
| Android namespace | `com.example` (current scaffold namespace) |
| Version name / code | `1.0` / `1` (scaffold values, independent of Haven roadmap stages) |
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

**Prototype disclosure:** This Android app currently demonstrates these journeys
with sample data and local state. Live authentication, backend messaging, and
persistent personal collections are not connected yet.

The product description is reusable copy, not a claim that every journey is live
in this prototype. Pair it with the prototype disclosure wherever the app's
current capabilities are described.

## Integration boundaries

- The main Haven repository owns the product roadmap and backend behavior.
- The mobile app currently has no configured Haven API client or endpoint.
- Sign-in, offers, and conversations must use the backend's verified contracts
  when integration begins; local simulation is not authentication.
- People search, proximity, agreement/closing, and payments are not current app
  capabilities. Do not advertise them as available.
- No Gemini feature or embedded-frame permission is required by the current UI.

Package identity, signing keys, store listing links, support contact, privacy
policy, and release versioning must be settled for publication. This document
does not invent those details or change existing application identity.

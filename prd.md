# Haven: Project Brief & Product Requirements Document (PRD)

---

## 1. Executive Summary & Vision

**Haven** is an identity-centered, text-forward mobile platform for reciprocal local exchange of goods, skills, and community offerings. Rooted in structural minimalism, editorial discipline, and authentic craft, Haven rejects algorithmic noise, visual stimulation, and intrusive monetization in favor of dependable, quiet local utility.

The production client is developed as a native **Android application built in Kotlin**, targeting modern mobile form factors (standardized around a 390 × 844 baseline viewport and adaptive edge-to-edge layouts), strictly honoring monochrome aesthetics, 48dp minimum touch targets, whitespace over decorative borders/shadows, and explicit system inset handling.

---

## 2. Brand Identity & Design System Principles

Haven operates under the **Haven Monastic Market** visual architecture:
- **Palette**: Strict monochrome & grayscale (`#FFFFFF`, `#FAFAFA`, `#F3F4F6`, `#E5E7EB`, `#6B7280`, `#111827`, `#000000`). No chromatic brand accents; genuine community photography serves as the sole organic source of color and warmth.
- **Elevation**: Completely shadowless. Visual hierarchy is achieved via surface tonal shifts (`#FFFFFF` to `#FAFAFA`) without decorative borders. Input/selection boundaries remain purposeful. Default Material elevation shadows are explicitly suppressed (`tonalElevation = 0.dp`, `shadowElevation = 0.dp`).
- **Depth & Frosted Overlays**: Frosted translucent scrims (`Color.White.copy(alpha = 0.8f)` with platform blur / `RenderEffect`) for contextual floating controls (Back, Save/Heart, Gallery chevrons) and modal bottom sheets.
- **Typography**: Inter (Google Fonts / downloadable font), high editorial contrast, tight letter-spacing (`-0.025em` to `-0.01em`), single-line truncations on feed card titles, and crisp proportional metadata scaling.
- **Ergonomics & Edge-to-Edge**: Full Android edge-to-edge support with `WindowInsets.safeDrawing`, `WindowInsets.statusBars`, `WindowInsets.navigationBars`, and `WindowInsets.ime` padding to guarantee uncompromised access across device varieties and virtual keyboard engagement.

---

## 3. Native Android Technology Stack & Architecture

Haven is engineered natively for Android. The Haven backend and its documented product journeys lead development: each running screen uses real server data and server-confirmed actions. Local persistence may cache that data, but cannot define different product behavior.

### 3.1 Core Technologies
- **Language**: Kotlin 2.x, Gradle version catalog, KSP (not kapt)
- **UI**: Jetpack Compose with Navigation Compose
- **Architecture**: UI layer and data layer. A domain layer is added only where
  logic is reused or complex.
  - UI: MVVM with unidirectional data flow. One immutable `UiState` per screen,
    exposed as `StateFlow`. One-off events are modelled as state, not `SharedFlow`.
  - Data: repositories are the only code that touches both network and database.
  - Room may be the source of truth for cached product data after the corresponding API contract exists. Authentication credentials use secure storage; pending actions and errors remain explicit UI state.
- **Async**: Kotlin Coroutines + `StateFlow`
- **Dependency Injection**: Hilt
- **Networking**: Ktor Client (CIO engine) + kotlinx.serialization, talking to the
  Haven backend over HTTPS (JSON). Use a server-push transport only when the Haven API defines one. Wrap calls in per-feature API classes; no Ktor types leave the data layer.
- **Authentication**: Sign-in is native Google only. The app obtains a Google ID token with Credential Manager and exchanges it at `POST /api/v1/auth/google`. It stores the rotating refresh token using an Android Keystore-backed key and confirms the person with `GET /api/v1/me` before showing an authenticated session. Access tokens stay in memory. Never store credentials in plain DataStore. The app opens no browser and has no PKCE, App Link or email fallback; email sign-in is shown as unavailable. Do not add Firebase Auth or a separate second sign-in path.
- **Images**: Coil loading Cloudinary URLs, with a capped disk cache. Offer title is
  used as `contentDescription`. First offer image is the cover.
- **Local persistence**
  - **Room (SQLite)**: source of truth for offers, profiles, conversations and messages.
    Exported schemas, tested migrations, no destructive fallback in release builds.
    Add paging or an outbox only when supported by an agreed Haven API contract.
  - **DataStore (Preferences)**: non-sensitive settings only (filters, coarse proximity).
- **Background work**: Add WorkManager, push notifications, or a live connection when an existing Haven journey and API contract require them.
- **Quality**: detekt + ktlint, unit tests for repositories and DAOs

### 3.2 Backend contract and integration rule

- **Source of truth**: The Haven roadmap, journeys, implemented API routes, and their tests define product behavior. Confirm an endpoint and its response before implementing a screen. If a required endpoint is absent, define the smallest contract for the existing Haven journey and implement it in Haven before wiring the screen. Do not invent a client-only substitute.
- **Current mobile auth contract**: `POST /api/v1/auth/google` (Google ID token), `POST /api/v1/auth/refresh`, `POST /api/v1/auth/revoke`, and `GET /api/v1/me`. Public feed integration adds `GET /api/v1/offers`; see `MOBILE_OFFERS_FEED.md` for the exact contract and verification status. Browser PKCE (`/api/v1/auth/authorize`, `/api/v1/auth/exchange`, `/auth/callback`) was removed from the backend and the app. Preserve token rotation and server-confirmed identity.
- **Product data**: Offers, people, saved offers, follows, inquiries, conversations, messages, and proximity results come from Haven. The server enforces ownership, privacy, ordering, and write outcomes. The client handles loading, empty, error, retry, and signed-out states without fabricating successful operations.
- **Proximity**: Haven ranks located offers against the selected reference location and retains distant and unlocated offers under its documented rules. The app does not locally reorder or exclude them.
- **Future transport and sync**: Room, paging, background work, push, WebSockets, tombstones, change feeds, generated schemas, and idempotent writes require an agreed Haven API contract and a demonstrated journey need. Their presence in an architecture proposal does not make them shipped backend capabilities.
- **Prototype boundary**: Sample content and fake repositories may exist only in previews and isolated tests. No running screen may claim a real person, offer, message, save, follow, or write was created by a simulation.

### 3.2.1 Proposed implementation choices, subject to the backend contract

- Room can cache offers, profiles, conversations, and messages once their APIs exist. Export schemas and test migrations; avoid destructive release migrations.
- DataStore stores non-sensitive settings only. Secure auth storage remains separate.
- Adopt Paging 3, WorkManager, FCM, or a streaming transport only when a corresponding Haven endpoint and mobile journey justify them.
- Map actual server error codes and retry semantics into screen state. Do not presume undocumented response formats or an app-version gate.

### 3.2 Design System Translation to Jetpack Compose
- **Design Tokens**: A bespoke `HavenTheme` wrapping Compose primitives without forced Material 3 chromatic accents.
- **Components**:
  - `HavenButton`: Primary solid `#111827`, secondary hairline border, minimum 48dp height.
  - `HavenTextField`: Crisp 1dp hairline perimeter border, permanently anchored label in uppercase caps, zero outer glow on focus.
  - `HavenGallery`: Multi-image horizontal pager (`HorizontalPager`) with snap scrolling, indicator pills, and edge bounding.
  - `HavenBottomSheet`: Modal bottom sheet with translucent backdrop scrim and drag-to-dismiss behavior.

### 3.3 Navigation & Platform Integration
- **Navigation**: Jetpack Compose Navigation with type-safe route definitions (`@Serializable` route classes/objects).
- **Back Navigation**: `PredictiveBackHandler` supporting Android 14+ predictive back gestures.
- **Keyboard Handling**: Built-in `Modifier.imePadding()` for seamless chat composer and listing form input.

---

## 4. User Personas & Core Journeys

### Personas
1. **The Explorer (Guest / Signed-in Member)**: Browses artisanal and regional offerings, filters by category or proximity, saves items to a personal ledger, and initiates direct messaging threads with creators.
2. **The Provider / Maker**: Creates, manages, and edits listings (up to 5 curated images, categories, descriptions, and pricing in multiple currencies), coordinates logistics via 1-on-1 threads, and maintains a public identity card.

### Core User Flows
1. **Discovery & Exploration**:
   - Welcome Screen → Browse Offers → Sticky Discovery Feed (Search + Category Chip Rail + Price/Proximity Filter Sheets) → Single-Line Offer Cards.
2. **Offer Consumption & Engagement**:
   - Offer Detail View (Snap-scroll carousel gallery, frosted safe-area controls, creator initials card, location breakdown, and anchored *Message provider* action).
   - Guest Gateways: Modal prompt seamlessly prompting Sign-in when attempting to save or message as a guest, preserving return context.
3. **Communication**:
   - Messages List (Conversations list with unread markers, avatars, and timestamps).
   - Conversation Thread (Pinned partner header, 1-on-1 chronological chat stream, contextual offer attachments, and fixed composer).
4. **Listing Lifecycle (Provider)**:
   - My Offers → Create Offer / Edit Offer (Reorderable image manager, live character counters, currency selector, unsaved changes modal).
   - Manage Offer (Carousel review, View Public shortcut, Destructive confirmation delete modal).
5. **Account & Identity**:
   - My Profile (Offer count, saved tallies, following counters, bio).
   - Edit Profile (Name, handle, craft bio, location metadata).
   - Following & Saved Offers Lists.

---

## 5. Feature Specifications & Screen Inventory

| Screen Module | Android Architecture / Compose Structure | Core Features & Technical Invariants |
| :--- | :--- | :--- |
| **Welcome** | `WelcomeScreen` (Clean full-screen edge-to-edge) | Pinned brand wordmark with Beta pill; 2×2 mosaic placeholder collage; primary *Browse offers* & secondary *Sign in* actions. |
| **Sign In** | `SignInScreen` (Scaffold with TopAppBar) | Fixed back navigation; centered auth container; native Google SSO button; disabled *Continue with email* action with a one-line explanation. No text fields, browser handoff or magic link. |
| **Offers Feed** | `OffersFeedScreen` (BottomNavigation Scaffold) | Pinned discovery search bar; horizontal category chip rail (`Craft & Making`, `Food & Hearth`, `Stays & Spaces`, etc.); 1-line truncated card titles; frosted filter modal bottom sheet; 3-destination NavigationBar (*Offers*, *Messages*, *Log in / Profile*). |
| **Offer Detail** | `OfferDetailScreen` (Edge-to-edge LazyColumn) | 4:3 `HorizontalPager` multi-image gallery track; frosted circular floating back & heart actions; provider snippet card; location table; anchored *Message provider* CTA. |
| **Manage Offer** | `ManageOfferScreen` (Scaffold) | Owner-exclusive management dashboard; carousel review; action grid (*Edit offer*, *View public*, *Delete* with destructive confirmation dialog). |
| **Create & Edit Offer** | `CreateOfferScreen` / `EditOfferScreen` | Multi-image selector (up to 5 items, reordering/removal); live character counters (Title 80, Desc 500); currency picker; drag-to-dismiss discard sheet. |
| **Messages & Thread** | `MessagesListScreen` / `ConversationThreadScreen` | Message list with active status; 1-on-1 conversation view with attached offer card context and multiline message composer with `imePadding()`. |
| **Profile & Connections** | `ProfileScreen` / `EditProfileScreen` | Public maker profile, private account dashboard, saved collections, following list, and profile editor. |

---

## 6. Offline-First Synchronization & Data Persistence

- **Server-backed state**: Screens render Haven API responses, with Room caching where the agreed endpoint and invalidation rules support it. A first load, offline state, and stale cache must be distinguishable.
- **Writes**: Saves, offers, and messages show pending state until Haven confirms them. Failed writes remain visibly failed and retryable; local state cannot claim server persistence.
- **Proximity Ordering**: Request server-ranked results using the selected reference location. Store the returned order by query; never calculate an alternate feed order on device.

---

## 7. Backend-led delivery sequence

1. **Verify native authentication**: Run native Google sign-in (Credential Manager → `POST /api/v1/auth/google`) on an Android simulated device, confirm `/api/v1/me`, and test cancellation, no-credential, rejected-token and refresh failure. Repeat on the intended installed build. No browser, App Link or PKCE step exists.
2. **Integrate one real journey at a time**: For each offerer or responder journey, inventory the existing Haven endpoint and tests; fill only required API gaps; connect the Android screen to real responses; verify loading, empty, failure, retry, and ownership states. Remove that screen's runtime sample data and simulated actions as it becomes integrated.
3. **Add local persistence only where it supports the real journey**: Cache server data and add offline behavior after consistency, invalidation, and write rules are agreed with Haven. Never let cached data or optimistic UI claim an unconfirmed server result as final.
4. **Release verification**: Run Android tests and a full native Google sign-in and core-journey pass on the installed build. Record any backend or deployment dependency that prevents completion.

The implementation sequence follows Haven's released scope and development-freeze decisions. New product capabilities require an explicit product decision; mobile implementation does not expand the backend contract by implication.

### Owner-approved offer capture — 2026-11-11

Create/Edit may explicitly capture one foreground current location, review its
coordinates and reported accuracy, and add it to the draft. Approximate readings
remain labelled approximate. Capture does not submit an offer or change discovery.
Manual place selection/removal remain available; leaving cancels acquisition and
there is no automatic resume/reacquisition. Selected draft locations persist via
the existing signed token and become public only through confirmed submission.
Camera photos share the current image preview/validation/upload journey.

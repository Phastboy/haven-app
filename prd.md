# Haven: Project Brief & Product Requirements Document (PRD)

---

## 1. Executive Summary & Vision

**Haven** is an identity-centered, text-forward mobile platform for reciprocal local exchange of goods, skills, and community offerings. Rooted in structural minimalism, editorial discipline, and authentic craft, Haven rejects algorithmic noise, visual stimulation, and intrusive monetization in favor of dependable, quiet local utility.

The production client is developed as a native **Android application built in Kotlin**, targeting modern mobile form factors (standardized around a 390 × 844 baseline viewport and adaptive edge-to-edge layouts), strictly honoring monochrome aesthetics, 48dp minimum touch targets, hairline borders over elevation drops, and explicit system inset handling.

---

## 2. Brand Identity & Design System Principles

Haven operates under the **Haven Monastic Market** visual architecture:
- **Palette**: Strict monochrome & grayscale (`#FFFFFF`, `#FAFAFA`, `#F3F4F6`, `#E5E7EB`, `#6B7280`, `#111827`, `#000000`). No chromatic brand accents; genuine community photography serves as the sole organic source of color and warmth.
- **Elevation**: Completely shadowless. Visual hierarchy is achieved via surface tonal shifts (`#FFFFFF` to `#FAFAFA`) and 1dp hairline borders (`#E5E5E5`). Default Material elevation shadows are explicitly suppressed (`tonalElevation = 0.dp`, `shadowElevation = 0.dp`).
- **Depth & Frosted Overlays**: Frosted translucent scrims (`Color.White.copy(alpha = 0.8f)` with platform blur / `RenderEffect`) for contextual floating controls (Back, Save/Heart, Gallery chevrons) and modal bottom sheets.
- **Typography**: Inter (Google Fonts / downloadable font), high editorial contrast, tight letter-spacing (`-0.025em` to `-0.01em`), single-line truncations on feed card titles, and crisp proportional metadata scaling.
- **Ergonomics & Edge-to-Edge**: Full Android edge-to-edge support with `WindowInsets.safeDrawing`, `WindowInsets.statusBars`, `WindowInsets.navigationBars`, and `WindowInsets.ime` padding to guarantee uncompromised access across device varieties and virtual keyboard engagement.

---

## 3. Native Android Technology Stack & Architecture

Haven is engineered natively for Android, leveraging modern Android development (MAD) practices and an offline-first architecture:

### 3.1 Core Technologies
- **Language**: Kotlin 2.x
- **UI Toolkit**: **Jetpack Compose** (100% declarative UI, zero legacy XML layouts)
- **Architecture Pattern**: Unidirectional Data Flow (UDF) with Clean Architecture / MVI (Model-View-Intent) or MVVM
- **Asynchronous Execution & Reactivity**: Kotlin Coroutines + `StateFlow` / `SharedFlow`
- **Dependency Injection**: Hilt / Dagger
- **Networking**: Ktor Client or Retrofit with Kotlinx Serialization
- **Image Loading**: Coil (Compose-first asynchronous image loader with memory caching and blur transformations)
- **Local Persistence**: 
  - **Room Database**: Offline-first caching for listings, provider profiles, and message threads.
  - **Jetpack DataStore (Preferences & Proto)**: Storing user credentials, session state, filter configurations, and proximity coordinates (`haven:proximity:v1`).

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
| **Sign In** | `SignInScreen` (Scaffold with TopAppBar) | Fixed back navigation; centered auth container; email magic link + Google SSO buttons; hairline bordered text fields. |
| **Offers Feed** | `OffersFeedScreen` (BottomNavigation Scaffold) | Pinned discovery search bar; horizontal category chip rail (`Craft & Making`, `Food & Hearth`, `Stays & Spaces`, etc.); 1-line truncated card titles; frosted filter modal bottom sheet; 3-destination NavigationBar (*Offers*, *Messages*, *Log in / Profile*). |
| **Offer Detail** | `OfferDetailScreen` (Edge-to-edge LazyColumn) | 4:3 `HorizontalPager` multi-image gallery track; frosted circular floating back & heart actions; provider snippet card; location table; anchored *Message provider* CTA. |
| **Manage Offer** | `ManageOfferScreen` (Scaffold) | Owner-exclusive management dashboard; carousel review; action grid (*Edit offer*, *View public*, *Delete* with destructive confirmation dialog). |
| **Create & Edit Offer** | `CreateOfferScreen` / `EditOfferScreen` | Multi-image selector (up to 5 items, reordering/removal); live character counters (Title 80, Desc 500); currency picker; drag-to-dismiss discard sheet. |
| **Messages & Thread** | `MessagesListScreen` / `ConversationThreadScreen` | Message list with active status; 1-on-1 conversation view with attached offer card context and multiline message composer with `imePadding()`. |
| **Profile & Connections** | `ProfileScreen` / `EditProfileScreen` | Public maker profile, private account dashboard, saved collections, following list, and profile editor. |

---

## 6. Offline-First Synchronization & Data Persistence

- **Local-First Pattern**: All reads query the local Room database (`Flow<List<OfferEntity>>`). Background sync fetches updates via Ktor/Retrofit and populates Room.
- **Optimistic UI Updates**: Toggling saves or editing listings updates local Room/DataStore state immediately with background sync dispatch.
- **Proximity Calculations**: Proximity filtering performed locally or via geo-bounded queries using reference location coordinates stored in Jetpack DataStore.

---

## 7. Roadmap & Delivery Phases

1. **Phase 1 (UI Prototype & Compose Design System)**: Complete UI component library (`HavenTheme`), Compose screen composables, navigation graph setup, and interactive preview flows.
2. **Phase 2 (Local Architecture & State Layer)**: Room schema, DataStore integration, ViewModels with `StateFlow`, image picker integration (Android Photo Picker), and mock repository implementations.
3. **Phase 3 (Network Integration & Backend Connectivity)**: Ktor/Retrofit API clients, WebSocket real-time messaging client, authentication (Magic Link + Google Credential Manager), and push notifications via FCM.
4. **Phase 4 (Hardening & Play Store Release)**: Edge-to-edge audit across Android API 26–35+, baseline profiles, automated Compose tests, accessibility review (TalkBack 48dp touch targets), and Play Store deployment.

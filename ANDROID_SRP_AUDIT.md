# Android SRP and file-size audit

**Date:** 2026-11-08
**Repository:** `Phastboy/haven-app` (`h-mobile`)
**Revision inspected:** `88e015d` — `fix: add tab swipes and Kotlin CIO cleanup for v0.0.2 (#24)`
**App version:** `0.0.2` (versionCode 12, `minSdk 24`, `targetSdk 37`)
**Scope:** every tracked Kotlin file at or above 300 lines — 18 main-source files and 3 test files (21 total).
**Status:** analysis only. No source files were modified. Historical analysis; source findings require revalidation against current main.

---

## 1. Method

1. Enumerated tracked Kotlin files and their line counts with `git ls-files` + `wc -l`.
2. Selected files with `wc -l >= 300`.
3. For each file, extracted top-level declarations and section markers.
4. Counted the concerns actually present: navigation, state creation, effects, repository/network access, media launchers, dialogs, sample data, formatting.
5. Checked liveness by searching call sites for each public composable/class across `app/src` (Kotlin, XML, Markdown).
6. For duplicate names, compared parameter lists to determine which overload the call sites resolve to.

### Caveats

- "Dead" means **unreferenced from the current source tree**. It does not account for reflection, Compose previews, or intentionally retained design references.
- The project was **not compiled** during this audit. Overload resolution conclusions are based on call-site arguments (all live call sites pass `viewModel = …`, which the legacy overloads do not accept).
- Line counts are `wc -l`, so a missing trailing newline undercounts by one.
- Generated/build output is excluded.

---

## 2. Executive summary

| Metric | Value |
|---|---:|
| Kotlin files ≥300 lines | 21 (18 main, 3 test) |
| Main-source lines inside those 18 files | 11,777 |
| **Entirely unreferenced files (≥300)** | **5 files / 4,358 lines** |
| Test-only duplicate screen | 1 file / 490 lines |
| Dead + test-only share of those 18 files | 41.2% |
| Dead + test-only share of all 21,003 main Kotlin lines | 23.1% |

**The dominant problem is not "too much logic per file" — it is abandoned duplicate screens.** Five large files (4,358 lines) implement features that the app no longer navigates to, and a sixth duplicates the production conversation thread while being exercised only by a test. Legacy overloads share function names with live screens (`SavedOffersScreen`, `FollowingScreen`, `MyOffersScreen`), which makes the codebase look larger and hides which implementation ships.

Among **live** code, six files have genuine single-responsibility failures: one navigation god-composable, two data-layer god-objects, and three screens that own work belonging to a ViewModel or a lower layer.

---

## 3. Ranked live violations

### 3.1 `ui/screens/OffersScreen.kt` — 1,280 lines

**Verdict: worst live offender. Four modules in one composable.**

| Responsibility | Evidence |
|---|---|
| Navigation model | `sealed interface OffersDestination` with 9 destinations, lines 128–138 |
| Nav persistence | custom `Saver` and `rememberSaveable` stack, lines 174–202 |
| ViewModel factory | 9 `viewModel(...)` calls: inquiry (162), profile (269), messages (273), edit profile (544), editor (580), my offers (603), saved (625), following (650), thread (670) — plus the feed ViewModel passed in |
| State collection | 7 `collectAsState()` calls (feed, session, inquiry, profile, messages, …) |
| Data loading | 8 `LaunchedEffect` blocks: feed (208), session (277), offer detail (306), provider (347), my offers (359), saved (370), following (383), inquiry completion (444) |
| Theme / appearance | theme scheme switch at 256 |
| Root-tab coordination | tab handling at 291 |
| Feed UI | feed rendering and chips at 837–1155 |
| Child dispatch | destination `when` from 451 through ~750 |
| Platform fallback | `fun openWeb(path)` at 254, used at 660 and 1060 |
| Extra UI | `MessagesInboxScreen` at 1039; helpers at 1156–1280 |

**Split proposal:** `HavenNavHost` (routes only) + one route composable per destination + a `FeedRoute` that owns feed state. Move ViewModel construction into each route.

---

### 3.2 `data/messaging/HavenLocalFirstMessagingRepository.kt` — 494 lines

**Verdict: persistence adapter, sync engine, and outbox worker in one class.**

| Responsibility | Evidence |
|---|---|
| Room access | 4 DAOs, lines 73–76 |
| Concurrency | 2 mutexes, lines 78–79 |
| Session coupling | reads `SessionManager.session.value.personId` directly at 80, 112, 168 |
| Inbox refresh/paging | 111–158 |
| Thread sync + merge | 159–292 |
| Inquiry creation | 293–314 |
| Optimistic send / retry / outbox | 315–470 |
| DTO → entity mapping | 478+ |

**Split proposal:** `LocalMessageStore` (Room), `MessagingSync` (remote fetch + merge), `OutboxWorker` (send/retry). Inject the actor id instead of reading global session state.

---

### 3.3 `auth/HavenAuthClient.kt` — 362 lines

**Verdict: auth manager + HTTP client + diagnostics + Google adapter.**

| Responsibility | Evidence |
|---|---|
| Diagnostics ring buffer | 26–97 |
| Singleton construction | 98 |
| Google native sign-in | 111–192 |
| Session restore / refresh rotation | 196–239 |
| Sign-out | 241–264 |
| Token access + authenticated GET/mutation | 269–310 |
| Multipart and image uploads | 325–358 |
| Raw HTTP execution | 360 |

**Split proposal:** `SessionManager` / `TokenStore`, `HavenHttpClient`, `GoogleSignInAdapter`, `AuthDiagnostics`.

---

### 3.4 `ui/screens/ReferenceLocationScreen.kt` — 486 lines

**Verdict: the UI is the data layer.**

```kotlin
// line 81, inside the composable
val placesRepo = remember(context) { HavenPlacesRepository(HavenAuthClient.shared(context)) }
```

It then owns debounce, cancellation, and error mapping in UI state (85–110):

```kotlin
var searchJob by remember { mutableStateOf<Job?>(null) }
fun performSearch(query: String) {
  searchJob?.cancel()
  ...
  searchJob = scope.launch { placesRepo.searchPlaces(query) ... }
}
```

**Split proposal:** move search, debounce, and error handling into `ReferenceLocationViewModel`; inject the repository.

---

### 3.5 `ui/screens/EditProfileScreen.kt` — 616 lines

**Verdict: avatar/media pipeline and profile form editing share one function.**

- Photo picker launcher at 88.
- Avatar upload/remove state at 191–254.
- Three editable fields with per-field error tags (`display_name_error`, `handle_error`, `bio_error`).
- Save invocation at 551 and success snackbar at 580+.

Business logic already lives in `EditProfileViewModel`; the file still merges two sub-features.

**Split proposal:** `AvatarEditor` and `ProfileForm` sub-composables.

---

### 3.6 `ui/screens/OfferDetailScreen.kt` — 811 lines

**Verdict: moderate. Two public entry points plus a component library.**

- `OfferDetailScreen` (presentational) — line 93.
- `NativeOfferDetailScreen` (wrapper) — line 232; calls the presentational screen at 255.
- 11 private composables (skeleton, gallery, summary, provider card, description, location, heading, action area, frosted button, sign-in dialog, price helper) — 269–811.

**Split proposal:** keep the gallery and action area in separate files; collapse to one public entry point.

---

### 3.7 `ui/screens/ProviderProfileScreen.kt` — 610 lines

**Verdict: borderline.** Screen + `CategoryFilterChip` (429) + `ProviderOfferCard` (461) + skeleton (543). Cohesive, but duplicates the dead `PublicProfileScreen.kt`.

---

## 4. Dead multi-screen files

Liveness checked across `app/src` in `.kt`, `.xml`, and `.md`. All five below have **zero references**.

| File | Lines | Screens inside | Duplicates |
|---|---:|---|---|
| `ui/screens/OfferManagementScreens.kt` | 1,402 | `NewOfferScreen` (101), `OwnerOfferDetailScreen` (679) | `NativeOfferEditorScreen.kt:36`, owner view in `MyOffersScreen` |
| `ui/screens/ProfileChildScreens.kt` | 1,147 | Saved (90), Following (405), My Offers (683) | `SavedOffersScreen.kt:86`, `FollowingScreen.kt:87`, `MyOffersScreen.kt:93` |
| `ui/screens/PublicOfferScreen.kt` | 687 | `PublicOfferScreen` (103) | `OfferDetailScreen.kt:93` |
| `ui/screens/PublicProfileScreen.kt` | 610 | `PublicProfileScreen` (87) | `ProviderProfileScreen.kt:73` |
| `ui/screens/AuthMessageScreens.kt` | 512 | Check email (56), Link expired (168), Error state (280) | none |
| **Total** | **4,358** | | |

### 4.1 `OfferManagementScreens.kt`

- Section 1 (`NewOfferScreen`, 101–660): hardcoded sample defaults at 109–115 (`"Hand-woven Aso Oke fabric…"`, price `"45000"`), local `validate()` at 123, currency dropdown, photo picker, snackbar.
- Section 2 (`OwnerOfferDetailScreen`, 679–1401): sample photo URLs 695–701, sample inquiries 703–709, photo reorder/delete/add 712–745, delete dialog, visibility toggle, upload form.

Production uses `NativeOfferEditorScreen.kt` (170 lines) instead.

### 4.2 `ProfileChildScreens.kt`

- Section 1, Saved: 76–386, 8 hardcoded items at 99–106.
- Section 2, Following: 390–668, sample people.
- Section 3, My Offers: 669–1147, sample offers.

Its public functions are **overloads** of the live screens:

```kotlin
// ProfileChildScreens.kt:90
fun SavedOffersScreen(onBack: () -> Unit, onBrowseOffers: () -> Unit, modifier: Modifier = Modifier)

// SavedOffersScreen.kt:86 (live)
fun SavedOffersScreen(offers: …, loading: …, onBack: …, onRetry: …, onOpenOffer: …, …)
```

Every call site passes arguments that only the live overload accepts (`viewModel =` or `offers =`), so the legacy versions are unreachable.

### 4.3 `PublicOfferScreen.kt`

One composable doing gallery pager, owner/saved toggles, inquiry sheet, toast timer, and message draft (116–135, 629).

### 4.4 `PublicProfileScreen.kt`

Sample offers at 109–149 plus a full profile layout; `ProviderProfileScreen` is the live equivalent.

### 4.5 `AuthMessageScreens.kt`

Three unrelated auth states in one file; none referenced.

### 4.6 `ConversationThreadView.kt` (test-only)

490 lines, duplicate of `NativeConversationThreadScreen.kt` (555). Only `app/src/test/java/com/example/WindowInsetsRobolectricTest.kt:149` exercises it. No production references.

### 4.7 Below the threshold

The same pattern continues under 300 lines:

- `ui/components/AttachOfferBottomSheet.kt` (195) — uses `SampleOffers`, 0 references.
- `ui/model/MessagesViewModel.kt` (202) — defaults to `SampleConversations`, referenced only by `ExampleRobolectricTest`.

---

## 5. Large but cohesive — no split recommended

| File | Lines | Why it is acceptable |
|---|---:|---|
| `MyOffersScreen.kt` | 547 | Container → stateless UI → card → skeleton; one screen |
| `FollowingScreen.kt` | 454 | Same container/presentational pattern |
| `SavedOffersScreen.kt` | 354 | Same pattern |
| `NativeConversationThreadScreen.kt` | 555 | Screen + message bubble only |
| `FilterBottomSheet.kt` | 360 | One sheet, no data access |

These are long because Compose layouts are verbose, not because they serve different actors or layers.

---

## 6. Tests ≥300 lines

| File | Lines | Tests | Concern |
|---|---:|---:|---|
| `HavenMessagingRepositoryTest.kt` | 487 | 17 | Cache, refresh, offline, optimistic send, retry, account isolation, dedupe, paging, gap recovery, migration in one class |
| `NativePagesTest.kt` | 344 | 10 | Five screen rendering suites plus a `parsePerson` JSON unit test |
| `HavenLocalFirstOffersRepositoryTest.kt` | 335 | 11 | Cache, pagination, detail, ViewModel, account isolation, query parsing, profile edit |

Grouping tests by subject is defensible. `NativePagesTest` mixing screen rendering with a parser unit test is the clearest split candidate.

---

## 7. Duplication matrix

| Feature | Live implementation | Duplicate / legacy |
|---|---|---|
| Saved offers | `SavedOffersScreen.kt:86` | `ProfileChildScreens.kt:90` |
| Following | `FollowingScreen.kt:87` | `ProfileChildScreens.kt:405` |
| My offers | `MyOffersScreen.kt:93` | `ProfileChildScreens.kt:683` |
| Conversation thread | `NativeConversationThreadScreen.kt:79` | `ConversationThreadView.kt:75` |
| Provider profile | `ProviderProfileScreen.kt:73` | `PublicProfileScreen.kt:87` |
| Offer detail | `OfferDetailScreen.kt:93` | `PublicOfferScreen.kt:103` |
| Create/edit offer | `NativeOfferEditorScreen.kt:36` | `OfferManagementScreens.kt:101` |
| Owner offer management | `MyOffersScreen` + `OfferDetailScreen` | `OfferManagementScreens.kt:679` |

---

## 8. Recommended remediation order

1. **Delete or relocate the dead screens.** Remove `OfferManagementScreens.kt`, `ProfileChildScreens.kt`, `PublicOfferScreen.kt`, `PublicProfileScreen.kt`, `AuthMessageScreens.kt`, and `ConversationThreadView.kt` — or move them to a `prototype/` source set excluded from the app. This removes 4,848 lines and the overload ambiguity.
2. **Split `OffersScreen.kt`** into `HavenNavHost`, per-destination route composables, and `FeedRoute`.
3. **Split `HavenLocalFirstMessagingRepository`** into `LocalMessageStore`, `MessagingSync`, and `OutboxWorker`; inject the actor id.
4. **Split `HavenAuthClient`** into session/token, HTTP, Google adapter, and diagnostics.
5. **Move `ReferenceLocationScreen` search** into a ViewModel with an injected repository.
6. **Break `EditProfileScreen`** into `AvatarEditor` and `ProfileForm`.
7. **Add guards:** an architecture test that fails on new top-level screens with zero references, and a soft line budget (warn at 400) to prevent regrowth.

---

## 9. Appendix — every Kotlin file ≥300 lines

| Lines | File | Status | Verdict |
|---:|---|---|---|
| 1402 | `ui/screens/OfferManagementScreens.kt` | Dead | 2 screens + sample data + photo/inquiry logic |
| 1280 | `ui/screens/OffersScreen.kt` | Live | **Router + 9 VMs + 8 effects + feed + theme** |
| 1147 | `ui/screens/ProfileChildScreens.kt` | Dead | 3 screens + sample data |
| 811 | `ui/screens/OfferDetailScreen.kt` | Live | 2 entry points + 11 components |
| 687 | `ui/screens/PublicOfferScreen.kt` | Dead | Monolithic offer page |
| 616 | `ui/screens/EditProfileScreen.kt` | Live | Avatar pipeline + form + feedback |
| 610 | `ui/screens/PublicProfileScreen.kt` | Dead | Profile page + sample data |
| 610 | `ui/screens/ProviderProfileScreen.kt` | Live | Screen + 3 components (duplicates dead profile) |
| 555 | `ui/screens/NativeConversationThreadScreen.kt` | Live | Cohesive screen + bubble |
| 547 | `ui/screens/MyOffersScreen.kt` | Live | Cohesive container/UI/card |
| 512 | `ui/screens/AuthMessageScreens.kt` | Dead | 3 unrelated auth states |
| 494 | `data/messaging/HavenLocalFirstMessagingRepository.kt` | Live | **Store + sync + outbox + mapping** |
| 490 | `ui/screens/ConversationThreadView.kt` | Test-only | Duplicate thread screen |
| 487 | `test/.../HavenMessagingRepositoryTest.kt` | Live | 17 tests, many concerns |
| 486 | `ui/screens/ReferenceLocationScreen.kt` | Live | **UI + repository + network search** |
| 454 | `ui/screens/FollowingScreen.kt` | Live | Cohesive container/UI/row |
| 362 | `auth/HavenAuthClient.kt` | Live | **Auth + HTTP + uploads + diagnostics** |
| 360 | `ui/components/FilterBottomSheet.kt` | Live | Cohesive sheet |
| 354 | `ui/screens/SavedOffersScreen.kt` | Live | Cohesive container/UI/skeleton |
| 344 | `test/.../NativePagesTest.kt` | Live | Screen tests + parser unit test |
| 335 | `test/.../HavenLocalFirstOffersRepositoryTest.kt` | Live | 11 tests across cache/paging/isolation |

---

## 10. Cross-references

- Build/OS configuration: `app/build.gradle.kts` (`minSdk 24`, `targetSdk 37`, version `0.0.2`).
- Liveness evidence for the two product bugs found during the same review:
  - Following chip scope bug — `ui/screens/OffersScreen.kt:842`, `ui/model/OffersViewModel.kt:238`, `data/HavenOffersRepository.kt:94`.
  - Following unfollow fallback — `ui/screens/FollowingScreen.kt:253`, `ui/screens/OffersScreen.kt:630`.
- These are tracked separately as `Phastboy/haven-app` issues #22 and #23.

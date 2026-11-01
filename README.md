# Haven for Android

Find offers. Know who is behind them. Start a conversation.

Haven connects people through what they offer. A person's identity and catalogue
stay visible, so discovering an offer can lead to knowing the person behind it
and returning to what they offer later.

This repository contains the native Android prototype for Haven, built with
Kotlin and Jetpack Compose. The main Haven project lives in
[phastboy/haven](https://github.com/phastboy/haven).

## Current state

The app still uses sample offers and conversations. Sign-in now uses Haven's
browser authorization flow and `/api/v1/me`; other features remain local prototypes.

The prototype includes:

- Welcome and passwordless sign-in screens, with browser handoff and email validation.
- Offer browsing, title search, currency and price filters, and saved-offer controls.
- Full-screen offer detail with a swipeable gallery, provider card, and location breakdown.
- Offer attachment sheets inside conversations.
- A conversation inbox and message threads using local state.
- Profile screens and theme controls.

Both sign-in buttons open Haven's web sign-in. The browser handles email and
Google authentication; the app only marks a person signed in after a one-time
PKCE code exchange and `/api/v1/me` response. The email field validates the
address before handoff; the browser may ask for it again. Refresh credentials
are encrypted with Android Keystore and excluded from backup. Messages and
saved state do not represent server-persisted activity.

An inquiry is non-binding communication about an offer. Sending a message does
not imply an agreement or reserve an offer. Payments and closing an exchange
are outside this prototype.

## Run locally

1. Open this repository in Android Studio with an Android SDK and a compatible
   Gradle JDK configured.
2. Sync the Gradle project. The build currently specifies compile SDK 36.1,
   target SDK 36, and minimum SDK 24 (Android 7.0).
3. Select the `app` debug configuration and an emulator or connected device.
4. Build and run the app. Standard Android debug signing is used.

The `Android APK` GitHub Action builds and tests on branch pushes and attaches a
testing APK to each published release. It is debug signed and is not a production
release artifact. To verify the HTTPS callback on an installed app, deploy the
matching package ID and APK signing certificate SHA-256 fingerprint in Haven's
Android App Links configuration. The AI Studio install may use a different signer
than the GitHub Actions APK. Until that fingerprint is known, Android cannot
verify the callback link and end-to-end sign-in remains untested.

No Gemini API key or Haven backend credentials are needed to build the app. The `.env.example` and Firebase/AI dependencies are inherited scaffolding;
there are no Gemini calls in the current app source.

The checkout currently has Gradle wrapper properties but no `gradlew` launcher or
wrapper JAR. Use Android Studio's Gradle setup, or generate a complete wrapper
with Gradle 9.3.1 before using `./gradlew` commands.

## Project layout

| Path | Purpose |
|---|---|
| `app/src/main/java/com/example/MainActivity.kt` | App entry point |
| `app/src/main/java/com/example/ui/screens/` | Compose screens |
| `app/src/main/java/com/example/ui/components/` | Shared controls, cards, and sheets |
| `app/src/main/java/com/example/ui/model/` | View models, sample data, and local session state |
| `app/src/main/java/com/example/ui/theme/` | Colors, typography, themes, and theme state |
| `app/src/main/res/` | Android resources |
| `app/src/test/` | Local tests, including Robolectric UI checks |
| `metadata.json` | App name, description, and AI Studio capability metadata |
| `PROJECT_INFO.md` | Project identity, app copy, and integration boundaries |

## Development

Run the local unit/Robolectric tests through Android Studio's Gradle task
`app:testDebugUnitTest`. A generated Gradle wrapper also supports
`./gradlew :app:testDebugUnitTest` and `./gradlew :app:assembleDebug`.

Keep product language consistent with Haven: person, offer, inquiry, saved offer,
and follow. Distinguish working backend behavior from prototype interactions.
People search and proximity discovery are future product questions, not shipped
mobile capabilities.

Before release, remaining backend integration, persistence, error handling, and
production app identity/signing need to be established. Android version `1.0` is the current
scaffold value; it does not mean Haven's v1.0 product milestone is complete.

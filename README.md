# Haven for Android

Find offers. Know who is behind them. Start a conversation.

Haven connects people through what they offer. A person's identity and catalogue
stay visible, so discovering an offer can lead to knowing the person behind it
and returning to what they offer later.

This repository contains the native Android prototype for Haven, built with
Kotlin and Jetpack Compose. The main Haven project lives in
[phastboy/haven](https://github.com/phastboy/haven).

## Current state

The app is a UI prototype with sample offers, conversations, and local session
state. It is not connected to the Haven backend yet.

The prototype includes:

- Welcome and passwordless sign-in screens, with validation and simulated states.
- Offer browsing, title search, currency and price filters, and saved-offer controls.
- Full-screen offer detail with a swipeable gallery, provider card, and location breakdown.
- Offer attachment sheets inside conversations.
- A conversation inbox and message threads using local state.
- Profile screens and theme controls.

Email sign-in currently simulates success; it does not send or verify a magic
link. Google sign-in is not integrated. Messages and saved state do not represent
server-persisted activity.

An inquiry is non-binding communication about an offer. Sending a message does
not imply an agreement or reserve an offer. Payments and closing an exchange
are outside this prototype.

## Run locally

1. Open this repository in Android Studio with an Android SDK and a compatible
   Gradle JDK configured.
2. Sync the Gradle project. The build currently specifies compile SDK 36.1,
   target SDK 36, and minimum SDK 24 (Android 7.0).
3. Select the `app` debug configuration and an emulator or connected device.
4. Check signing before running: the current AI Studio debug configuration expects
   a `debug.keystore` at the repository root. For a standalone local checkout
   without that file, use Android Studio's standard debug signing configuration
   by removing the explicit `debugConfig` assignment from the debug build type
   in your local setup. Never substitute a production signing key.
5. Build and run the app.

No Gemini API key or Haven backend credentials are needed for the current sample
flows. The `.env.example` and Firebase/AI dependencies are inherited scaffolding;
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

Before release, backend authentication, persistence, error handling, and production
app identity/signing need to be established. Android version `1.0` is the current
scaffold value; it does not mean Haven's v1.0 product milestone is complete.

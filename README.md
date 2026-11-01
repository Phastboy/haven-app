# Haven for Android

Find offers. Know who is behind them. Start a conversation.

Haven connects people through what they offer. A person's identity and catalogue
stay visible, so discovering an offer can lead to knowing the person behind it
and returning to what they offer later.

This repository contains the native Android prototype for Haven, built with
Kotlin and Jetpack Compose. The main Haven project lives in
[phastboy/haven](https://github.com/phastboy/haven).

## Current state

Sign-in uses Haven's browser authorization flow and `/api/v1/me`. The Offers
feed now uses `GET /api/v1/offers` for real public data, search, category and
price filters, and cursor pagination. Its implementation requires backend
0.8.2 or later. Missing connectivity or an unavailable endpoint shows an error,
never sample results. See [the integration contract](MOBILE_OFFERS_FEED.md).

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
saved state in the remaining prototype files do not represent server-persisted activity.
The integrated feed opens real Haven web pages for offer detail/save, location,
messages, and profile journeys; it does not navigate into those sample screens.

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

The repository includes the Gradle 9.3.1 wrapper (`gradlew` and `gradlew.bat`).
It downloads the pinned Gradle distribution on first use and verifies its checksum.
Install Java 17 and the Android SDK (platform 36.1 and build tools 36.0.0),
then set `JAVA_HOME` and `ANDROID_HOME` or configure the SDK through Android Studio.
A separate system Gradle installation is unnecessary.

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

Build the debug APK, run unit/Robolectric tests, and check Kotlin/Java, resources,
and manifest issues with Android Lint:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The Android APK workflow runs these checks on main and feature-branch pushes
and pull requests. It uploads lint and test reports, including when a check fails.
The local lint report is `app/build/reports/lint-results-debug.html`.
Android Lint checks Android correctness and API usage; it does not enforce a
complete Kotlin formatting style.

Keep product language consistent with Haven: person, offer, inquiry, saved offer,
and follow. Distinguish working backend behavior from prototype interactions.
Proximity selection currently opens Haven web; native proximity API integration
is still pending. The public mobile feed itself uses general discovery.

Before release, remaining backend integration, persistence, error handling, and
production app identity/signing need to be established. Android version `1.0` is the current
scaffold value; it does not mean Haven's v1.0 product milestone is complete.

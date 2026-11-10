# Haven for Android

Find offers. Know who is behind them. Start a conversation.

Native Android app built with Kotlin, Jetpack Compose and Material 3.
Backend: [Phastboy/haven](https://github.com/Phastboy/haven).

## Current app

Native Google sign-in uses Credential Manager and the Haven API. Offers,
profiles, saved offers, following, inquiries and messaging use backend data;
Room retains supported feed/message data. Discovery supports All offers and
Following, with explicit proximity in public discovery. There are no simulated
successful writes or sample runtime offers.

Version 0.0.5 adds branded startup, themed launch resources, GitHub APK update
prompts and automatic feed pagination. See [the release test checklist](RELEASE_V0.0.5.md).
The first updater-enabled APK requires manual installation. Later stable GitHub
releases publish update metadata automatically; installation always requires
Android confirmation. This app is distributed through GitHub releases.

## Build and develop

Open this repository in Android Studio with a compatible Gradle JDK and SDK.
CI uses JDK 21, JVM target 17, Gradle 9.9.1, compile SDK 37.2, target SDK 37,
build-tools 37.0.0 and minimum SDK 24. Exact dependencies live in
`gradle/libs.versions.toml`; the wrapper verifies its distribution checksum.

Run builds/tests/lint in GitHub Actions to preserve the owner's local data budget.
See [build and signing instructions](ANDROID_BUILD_HANDOFF.md).
No generator platform, Gemini key, Firebase setup or backend secret is required.
Keep the existing `com.aistudio.haven.xvykpt` application ID: it is the installed
package identity and must remain stable for updates and Google OAuth.

## Layout

- `app/src/main/java/com/example/`: app entry, data, screens, components, state and updates.
- `app/src/main/res/`: native resources and light/dark launch branding.
- `app/src/test/`: unit and Robolectric regression checks.
- `prototype/`: design references, retained for visual comparison.
- `.github/workflows/android-apk.yml`: build, tests, lint and signed release delivery.

Use person, offer and inquiry consistently. An inquiry is non-binding;
payments and commitments remain outside the supported product scope.

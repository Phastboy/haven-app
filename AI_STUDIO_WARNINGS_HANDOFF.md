After Android v0.0.1 (versionCode 11), the owner authorizes a separate warning-cleanup task. AI Studio may implement this Android-only work; use current main and the repository instructions. Do not expand product scope.

Baseline: PR #18 head `7002d5b`, [CI 42379480988](https://github.com/Phastboy/haven-app/actions/runs/42379480988). Both debug and release lint reports contain **65 warnings, zero errors**. These are per-variant results, not 130 distinct defects. All 115 tests and debug/release builds passed. Compiler deprecation warnings are separate; inventory them rather than conflating counts.

## Instructions for AI Studio / next agent

1. Read AGENTS.md and NEXT_AGENT.md, then verify current main/CI and obtain the latest `haven-android-verification-reports` artifact. Reports are `reports/lint-results-debug.xml` and `reports/lint-results-release.xml`. The inventory below is retained because Actions artifacts expire.
2. Reproduce the warning inventory by issue ID and location. Start with safe Android source/resource corrections, preserving runtime resource references, light/dark appearance, startup, authentication, messages/outbox/cache isolation and exactly three offer-card text rows. No decorative borders/shadows may return. All feed action circles remain the same size/strategy.
3. Treat dependency/plugin/Gradle/target-SDK notices as advisories, not permission to upgrade everything. Verify official compatibility and the pinned CI recipe; isolate consequential toolchain/dependency/target changes into separate reviewed PRs. Verify the apparently outdated target notice against the actual SDK. Do not claim an available-version notice is a vulnerability.
4. No blanket suppressions, lint baselines, disabling checks, fabricated passes, destructive cache/schema resets, signing-key regeneration or application-ID/backend changes. Do not alter the launcher brand to silence icon warnings; retain its identity and review any asset/layout correction.
5. Run build/tests/debug+release lint **in CI**, preserving the owner's local data budget. Compare before/after counts by issue ID in both variants. Confirm APK version/signer/provenance and that all existing regressions pass. Report any justified remaining warnings individually.
6. Update NEXT_AGENT.md and release/checklist status to reflect merged PR #18 and the actual published release. Keep Samsung/production acceptance separate. Open a focused fix PR; do not merge, tag, publish or bump the release merely to perform this cleanup.

## Baseline by lint issue ID

| Issue | Count |
|---|---:|
| `GradleDependency` | 23 |
| `UseKtx` | 10 |
| `NewerVersionAvailable` | 9 |
| `UnusedResources` | 7 |
| `IconLocation` | 7 |
| `IconLauncherShape` | 5 |
| `AndroidGradlePluginVersion` | 2 |
| `OldTargetApi` | 1 |
| `RedundantLabel` | 1 |

## Durable baseline locations

<details><summary>All 65 report entries</summary>

- `OldTargetApi` — `app/build.gradle.kts:19`: Not targeting the latest versions of Android; compatibility modes apply. Consider testing and updating this version. Consult the 'android.os.Build.VERSION_CODES' javadoc for details.
- `RedundantLabel` — `app/src/main/AndroidManifest.xml:21`: Redundant label can be removed
- `AndroidGradlePluginVersion` — `gradle/wrapper/gradle-wrapper.properties:4`: A newer version of Gradle than 9.3.1 is available: 9.9.0
- `AndroidGradlePluginVersion` — `gradle/libs.versions.toml:3`: A newer version of com.android.application than 9.1.1 is available: 9.5.1
- `GradleDependency` — `gradle/libs.versions.toml:2`: A newer version of androidx.browser:browser than 1.9.0 is available: 1.10.0
- `GradleDependency` — `gradle/libs.versions.toml:4`: A newer version of androidx.core:core-ktx than 1.18.0 is available: 1.19.1
- `GradleDependency` — `gradle/libs.versions.toml:8`: A newer version of androidx.lifecycle:lifecycle-runtime-ktx than 2.8.7 is available: 2.11.0
- `GradleDependency` — `gradle/libs.versions.toml:9`: A newer version of androidx.lifecycle:lifecycle-viewmodel-compose than 2.8.7 is available: 2.11.0
- `GradleDependency` — `gradle/libs.versions.toml:10`: A newer version of androidx.lifecycle:lifecycle-runtime-compose than 2.8.7 is available: 2.11.0
- `GradleDependency` — `gradle/libs.versions.toml:11`: A newer version of androidx.activity:activity-compose than 1.10.1 is available: 1.13.0
- `GradleDependency` — `gradle/libs.versions.toml:13`: A newer version of androidx.compose:compose-bom than 2024.09.00 is available: 2026.09.00
- `GradleDependency` — `gradle/libs.versions.toml:15`: A newer version of androidx.navigation:navigation-compose than 2.8.9 is available: 2.10.2
- `GradleDependency` — `gradle/libs.versions.toml:16`: A newer version of androidx.room:room-runtime than 2.7.0 is available: 2.8.5
- `GradleDependency` — `gradle/libs.versions.toml:17`: A newer version of androidx.room:room-ktx than 2.7.0 is available: 2.8.5
- `GradleDependency` — `gradle/libs.versions.toml:18`: A newer version of androidx.room:room-compiler than 2.7.0 is available: 2.8.5
- `GradleDependency` — `gradle/libs.versions.toml:20`: A newer version of androidx.test:core than 1.6.1 is available: 1.7.0
- `GradleDependency` — `gradle/libs.versions.toml:21`: A newer version of androidx.test:runner than 1.6.2 is available: 1.7.0
- `GradleDependency` — `gradle/libs.versions.toml:28`: A newer version of com.google.android.gms:play-services-location than 21.3.0 is available: 21.4.0
- `GradleDependency` — `gradle/libs.versions.toml:29`: A newer version of androidx.camera:camera-camera2 than 1.5.0 is available: 1.6.2
- `GradleDependency` — `gradle/libs.versions.toml:30`: A newer version of androidx.camera:camera-lifecycle than 1.5.0 is available: 1.6.2
- `GradleDependency` — `gradle/libs.versions.toml:31`: A newer version of androidx.camera:camera-view than 1.5.0 is available: 1.6.2
- `GradleDependency` — `gradle/libs.versions.toml:32`: A newer version of androidx.camera:camera-core than 1.5.0 is available: 1.6.2
- `GradleDependency` — `gradle/libs.versions.toml:37`: A newer version of androidx.datastore:datastore-preferences than 1.1.7 is available: 1.3.0
- `GradleDependency` — `gradle/libs.versions.toml:39`: A newer version of com.google.firebase:firebase-bom than 34.17.0 is available: 35.1.0
- `GradleDependency` — `gradle/libs.versions.toml:42`: A newer version of androidx.credentials:credentials than 1.5.0 is available: 1.6.0
- `GradleDependency` — `gradle/libs.versions.toml:42`: A newer version of androidx.credentials:credentials-play-services-auth than 1.5.0 is available: 1.6.0
- `GradleDependency` — `gradle/libs.versions.toml:43`: A newer version of com.google.android.libraries.identity.googleid:googleid than 1.1.1 is available: 1.2.1
- `NewerVersionAvailable` — `gradle/libs.versions.toml:12`: A newer version of org.jetbrains.kotlin.plugin.compose than 2.2.10 is available: 2.4.21
- `NewerVersionAvailable` — `gradle/libs.versions.toml:19`: A newer version of org.jetbrains.kotlinx:kotlinx-coroutines-test than 1.10.2 is available: 1.11.0
- `NewerVersionAvailable` — `gradle/libs.versions.toml:23`: A newer version of com.squareup.retrofit2:retrofit than 2.12.0 is available: 3.0.0
- `NewerVersionAvailable` — `gradle/libs.versions.toml:24`: A newer version of com.squareup.retrofit2:converter-moshi than 2.12.0 is available: 3.0.0
- `NewerVersionAvailable` — `gradle/libs.versions.toml:25`: A newer version of org.jetbrains.kotlinx:kotlinx-coroutines-android than 1.10.2 is available: 1.11.0
- `NewerVersionAvailable` — `gradle/libs.versions.toml:26`: A newer version of org.jetbrains.kotlinx:kotlinx-coroutines-core than 1.10.2 is available: 1.11.0
- `NewerVersionAvailable` — `gradle/libs.versions.toml:33`: A newer version of com.squareup.okhttp3:logging-interceptor than 4.10.0 is available: 5.5.1
- `NewerVersionAvailable` — `gradle/libs.versions.toml:34`: A newer version of com.squareup.okhttp3:okhttp than 4.10.0 is available: 5.5.1
- `NewerVersionAvailable` — `gradle/libs.versions.toml:38`: A newer version of org.robolectric:robolectric than 4.16.1 is available: 4.17
- `UnusedResources` — `app/src/main/res/values/colors.xml:3`: The resource 'R.color.purple_200' appears to be unused
- `UnusedResources` — `app/src/main/res/values/colors.xml:4`: The resource 'R.color.purple_500' appears to be unused
- `UnusedResources` — `app/src/main/res/values/colors.xml:5`: The resource 'R.color.purple_700' appears to be unused
- `UnusedResources` — `app/src/main/res/values/colors.xml:6`: The resource 'R.color.teal_200' appears to be unused
- `UnusedResources` — `app/src/main/res/values/colors.xml:7`: The resource 'R.color.teal_700' appears to be unused
- `UnusedResources` — `app/src/main/res/values/colors.xml:8`: The resource 'R.color.black' appears to be unused
- `UnusedResources` — `app/src/main/res/values/colors.xml:9`: The resource 'R.color.white' appears to be unused
- `IconLauncherShape` — `app/src/main/res/mipmap-hdpi/ic_launcher.png`: Launcher icons should not fill every pixel of their square region; see the design guide for details
- `IconLauncherShape` — `app/src/main/res/mipmap-mdpi/ic_launcher.png`: Launcher icons should not fill every pixel of their square region; see the design guide for details
- `IconLauncherShape` — `app/src/main/res/mipmap-xhdpi/ic_launcher.png`: Launcher icons should not fill every pixel of their square region; see the design guide for details
- `IconLauncherShape` — `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`: Launcher icons should not fill every pixel of their square region; see the design guide for details
- `IconLauncherShape` — `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`: Launcher icons should not fill every pixel of their square region; see the design guide for details
- `IconLocation` — `app/src/main/res/drawable/fabric_craft.jpg`: Found bitmap drawable 'res/drawable/fabric_craft.jpg' in densityless folder
- `IconLocation` — `app/src/main/res/drawable/guitar_lessons.jpg`: Found bitmap drawable 'res/drawable/guitar_lessons.jpg' in densityless folder
- `IconLocation` — `app/src/main/res/drawable/haven_icon.jpg`: Found bitmap drawable 'res/drawable/haven_icon.jpg' in densityless folder
- `IconLocation` — `app/src/main/res/drawable/lamp_craft.jpg`: Found bitmap drawable 'res/drawable/lamp_craft.jpg' in densityless folder
- `IconLocation` — `app/src/main/res/drawable/pottery_craft.jpg`: Found bitmap drawable 'res/drawable/pottery_craft.jpg' in densityless folder
- `IconLocation` — `app/src/main/res/drawable/textbooks.jpg`: Found bitmap drawable 'res/drawable/textbooks.jpg' in densityless folder
- `IconLocation` — `app/src/main/res/drawable/veg_box.jpg`: Found bitmap drawable 'res/drawable/veg_box.jpg' in densityless folder
- `UseKtx` — `app/src/main/java/com/example/data/HavenOfferEditorRepository.kt:45`: Use the KTX extension function 'String.toUri' instead?
- `UseKtx` — `app/src/main/java/com/example/data/HavenOffersRepository.kt:87`: Use the KTX extension function 'String.toUri' instead?
- `UseKtx` — `app/src/main/java/com/example/ui/screens/OffersScreen.kt:246`: Use the KTX extension function 'String.toUri' instead?
- `UseKtx` — `app/src/main/java/com/example/auth/SecureAuthStore.kt:46`: Use the KTX extension function 'SharedPreferences.edit' instead?
- `UseKtx` — `app/src/main/java/com/example/auth/SecureAuthStore.kt:58`: Use the KTX extension function 'SharedPreferences.edit' instead?
- `UseKtx` — `app/src/main/java/com/example/auth/SecureAuthStore.kt:66`: Use the KTX extension function 'SharedPreferences.edit' instead?
- `UseKtx` — `app/src/main/java/com/example/auth/SecureAuthStore.kt:74`: Use the KTX extension function 'SharedPreferences.edit' instead?
- `UseKtx` — `app/src/main/java/com/example/auth/SecureAuthStore.kt:79`: Use the KTX extension function 'SharedPreferences.edit' instead?
- `UseKtx` — `app/src/main/java/com/example/auth/SecureAuthStore.kt:85`: Use the KTX extension function 'SharedPreferences.edit' instead?
- `UseKtx` — `app/src/main/java/com/example/ui/theme/ThemeManager.kt:46`: Use the KTX extension function 'SharedPreferences.edit' instead?

</details>

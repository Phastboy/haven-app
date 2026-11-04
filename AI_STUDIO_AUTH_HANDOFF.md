# AI Studio task: repair Haven Android sign-in

> **Superseded 2026-11-04.** The browser PKCE flow, `/auth/callback` App Link and
> `assetlinks.json` association described here were removed. Sign-in is native
> Google via Credential Manager → `POST /api/v1/auth/google`. This file is kept
> as a historical record of the earlier investigation.

## Goal

Make the existing Haven backend sign-in flow work end to end in AI Studio's simulated Android device, then provide the exact checks needed for the installed build. Keep Haven as the authentication authority. Do not replace its flow with Firebase Auth, Credential Manager, a simulated user, or local success state.

## Repositories and current contract

- Android app: `phastboy/h-mobile`, branch containing `app/src/main/java/com/example/auth/HavenAuthClient.kt`, `MainActivity.kt`, `AndroidManifest.xml`, and `prd.md`.
- Haven backend: `phastboy/haven`, mobile API plan at `expts/rust/MOBILE_API_PLAN.md`; handlers under `expts/rust/crates/haven-web/src/app/api/` and Google OAuth under `expts/rust/crates/haven-web/src/app/auth/google/`.
- App begins `GET /api/v1/auth/authorize?code_challenge=...&state=...` in the system browser. Haven web handles Google or email sign-in, then redirects to configured HTTPS `/auth/callback?code=...&state=...`. Android verifies state and exchanges the code with PKCE at `POST /api/v1/auth/exchange`, then calls `GET /api/v1/me`. Refresh/revoke use `/api/v1/auth/refresh` and `/api/v1/auth/revoke`.
- The app currently targets `haven-odysseon-d48990aa.koyeb.app` (retired 2026-11-04; the live domain is `haven.apps.mistral.run`); confirm that the deployed backend and `MOBILE_APP_LINK` use the same host and callback path.

## Observed failure

Immediately after tapping **Sign in with Google**, the UI reported “Google sign-in was cancelled.” Haven's web callback currently maps any Google `error` query parameter to `google_cancelled`, so this message alone does not identify the cause. Determine whether the message is shown by Haven web, a prototype/simulated screen, or a native callback. Capture the actual URL/error code and redirect sequence without recording tokens, authorization codes, cookies, or client secrets in the report.

## Investigation and implementation

1. Run the current app in AI Studio's simulated Android device. Tap Google sign-in and record whether the system browser opens, the first URL, the Google result, the Haven callback result, and whether Android receives the HTTPS App Link. Keep sensitive query values redacted.
2. Inspect Google Cloud OAuth client configuration: authorized redirect URI must exactly match Haven's `GOOGLE_REDIRECT_URI`; check consent-screen publishing/test-user access and the deployed `GOOGLE_CLIENT_ID` configuration. Report which setting is wrong before changing it. Do not expose secrets in source or the report.
3. Fix misleading error mapping in Haven if Google returned a non-cancellation error. Preserve the actual error category in safe logs/UI. Keep state and PKCE checks intact.
4. Verify `/.well-known/assetlinks.json` is served by Haven with Android's actual package ID and the SHA-256 fingerprint of the certificate that signed the APK installed in AI Studio. Confirm the Android manifest host/path match `MOBILE_APP_LINK`. The AI Studio signer may differ from CI's debug signer.
5. Complete a successful flow: Google → Haven browser callback → Android App Link → state check → code exchange → `/api/v1/me` → authenticated screen. Test denial, stale/mismatched state, failed exchange, and browser return without app launch. No simulated authentication success is acceptable.
6. Keep changes focused. Update both repositories only where the verified cause requires it. Provide changed files, test results, deployed configuration still needed, and a reproducible installed-build test procedure.

## Inputs needed from the owner or deployment access

- Access to the current Android and Haven repositories/branches and the AI Studio simulated device.
- The exact failed browser URL or a redacted redirect trace and the Google error shown, if available.
- Read access to Google Cloud OAuth client and consent-screen settings, plus the deployed Haven environment variable *names and values where non-secret*. Share secrets through a secure configuration channel, never in the prompt or commits.
- The installed APK's package ID and signing certificate SHA-256 fingerprint. If AI Studio cannot reveal the signer, obtain it from the built APK before changing `assetlinks.json`.

## Acceptance

The app authenticates only after Haven returns a valid code and `/api/v1/me` confirms the person. A failed Google request reports its actual class of error. A fresh install of the tested APK returns from the browser to Android through a verified HTTPS App Link. The final report distinguishes simulator results from the installed-build verification.

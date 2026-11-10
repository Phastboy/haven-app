# Haven Android v0.0.5 — owner device test checklist

Candidate: versionName **0.0.5**, versionCode **15**, PR #38.
Install: [GitHub release](https://github.com/Phastboy/haven-app/releases/tag/v0.0.5).
Package and persistent signing certificate are unchanged. The first APK with
this updater must be installed manually, over the existing signed app.

Owner explicitly authorized publication on 2026-11-10 and will report device
findings after release. Device acceptance below remains **NOT RUN**, not passed.
The cleanup commit does not require waiting for another CI run by owner direction.
Functional head 45c4ed8 passed CI 42873093487: 190 tests and zero debug/release
lint issues. Automated delivery must still build and verify the released source.

For each session, record phone model, Android version, installed Haven version,
OS light/dark setting, app appearance preference and network conditions. Tick
only scenarios actually tested. Owner executes; maintainer triages findings.
First results review target: 2026-11-11; no automated appointment is implied.

## 1. Install and existing data

- [ ] Download `haven-v0.0.5.apk` after automated release assets arrive.
- [ ] Install over the existing Haven APK without uninstalling. Version shows 0.0.5.
- [ ] Confirm Google sign-in/session, messages, queued messages and saved state
      remain usable. Report any unexpected sign-out or missing data.

## 2. Startup and themes

- [ ] Close Haven fully and launch it. The logo remains during restoration;
      no spinner/“Opening Haven…” screen appears, and the app eventually opens.
- [ ] Switch away with Home and return through Recents; existing content resumes.
- [ ] Repeat full launches with app Light, Dark and System, each with OS light
      and dark. Check logo/background contrast, flashes and final screen theme.
- [ ] Switch from forced Light/Dark back to System without closing the app;
      the displayed theme follows the OS. Repeat on older Android if available.
- [ ] With a saved session and network disabled, launch; restoration failure has
      a working retry. Restore connectivity and retry; no false sign-out.
- [ ] With TalkBack, the logo announces Haven and that the session is restoring.

Android 12+ persists the app launch preference. On API 24–30 the system's
starting window can follow OS appearance before app code runs; the app content
still follows the saved preference. Record this distinction when reporting.

## 3. Offers feed

- [ ] Loaded-offer count and the manual “More offers” control are absent.
- [ ] First load shows card skeletons. Scroll towards the bottom: another page
      starts automatically; while waiting, two skeleton cards appear below
      retained cards, without a second pagination spinner.
- [ ] Continue across several pages; cards retain order, scroll position and
      unique identities. At the end, requests and skeletons stop.
- [ ] Open an offer and return; retain the expected feed/scroll context.
- [ ] Repeat with search/category/price and Following; proximity stays explicit
      and applies only to its supported public scope.
- [ ] Disable connectivity while paging. Existing cards remain; retry feedback
      appears. Restore connectivity and retry; no duplicate or missing page.
- [ ] Change filters or account during a slow page; old results never appear in
      the new context. Check light/dark and large text.

A server returning an unchanged non-null cursor must stop with a retryable
paging error; automated regression covers this malformed response.

## 4. GitHub updates

A successful newer-update test needs a later stable release with a higher
versionCode, compatible signer and generated `update.json`. Merely reopening
0.0.5 while 0.0.5 is latest should not offer an update. This future-release
prerequisite remains open; do not publish a fake release just for this checklist.

- [ ] Current/latest version opens normally without an update prompt.
- [ ] Offline or failed GitHub check does not block Haven; next launch can retry.
- [ ] When a genuine newer stable release exists, opening Haven shows version,
      release notes, Later and Download update. Later keeps the app usable.
- [ ] Download on a slower connection; a progressing transfer can exceed one
      minute. Total limit is 15 minutes, with connection/inactivity limits kept.
- [ ] Deny installation permission; return to Haven and grant it through the
      offered settings flow. Return and tap Install; Android asks confirmation.
- [ ] Cancel the installer; the existing app remains usable. Retry installation.
- [ ] Confirm an accepted update installs over Haven without uninstalling or
      losing messages/account data, and reports the newer version.
- [ ] Close/interrupt Haven during download, reopen and retry. Partial downloads
      are not installed; interrupted downloads currently restart from the beginning.

Checksum, size, package, version and installed certificate-set checks occur
before installer handoff; Android performs final signature/install verification.
Corruption, wrong signer/package and downgrade rejection still need controlled
APK/device evidence; metadata rejection is also covered by automated tests.

## Report an issue

Include the checklist section, expected/observed behaviour, exact app/device/OS,
app/OS appearance and network state, reproduction steps and a screenshot or
short recording where useful. Do not include tokens or private message content.
Track each confirmed defect separately and link it to PR #38 / v0.0.5.

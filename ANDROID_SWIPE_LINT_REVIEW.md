# Android review: swipe tabs and lint cleanup

## Reviewed source and evidence

AI Studio changed main in `2621d00`, following `2adebed` (the file-based handoff).
[Its CI 42384744900](https://github.com/Phastboy/haven-app/actions/runs/42384744900)
passed existing checks but added no gesture regression tests. Both lint reports
contain 40 warnings: 35 compatibility/version advisories and 5 legacy launcher
shape warnings, compared with the original 65 per variant. The resource moves,
unused-colour/label removal and KTX conversions account for 25 resolved warnings.

## Corrections in this review

- Use settled pager navigation for page activation and guest sign-in, avoiding
  actions during cancelled drags or intermediate animated pages. Tab presses
  cancel superseded navigation requests. Tab controls expose selected/Tab semantics.
- Reset tab state for a different account and keep private content gated during
  pager precomposition. Unread badge reads are separate from loading the inbox;
  page composition does not trigger a private inbox read.
- Hoist inbox/profile/category scroll state so tab changes and child-route Back
  retain their position. Root paging stays out of child routes and pauses for
  filters, errors and profile dialogs. Category overscroll is consumed locally.
- Reuse existing round Haven launcher artwork for pre-26 launchers, replacing
  the duplicate full-square legacy icons. Adaptive icon files and logo artwork
  stay unchanged. Review the legacy resource on a device separately.
- Remove a pre-existing fixed-key encryption fallback from production credentials.
  Only tests inject test keys. A transient key-provider failure propagates without
  deleting saved ciphertext, allowing startup retry. Existing Keystore alias,
  cipher format and signing identity are retained; no database migration/reset.

## Warning accounting and accepted boundaries

No baselines, disabled checks or blanket suppressions are added. The intended
remaining set is 35 informational compatibility/version advisories, subject to
confirmation in both exact-revision CI reports:

| Lint ID | Count in AI Studio CI | Review disposition |
|---|---:|---|
| GradleDependency | 23 | Pinned Android/library updates require official compatibility checks and dedicated regression verification. |
| NewerVersionAvailable | 9 | Version suggestions are not vulnerability findings; isolate compatible updates rather than bulk upgrade. |
| AndroidGradlePluginVersion | 2 | Gradle/AGP are part of the pinned build/signing recipe; migration is separate from warning/source cleanup. |
| OldTargetApi | 1 | Current target 36/compile 36.1 are pinned. A target-SDK migration needs behavior/device acceptance, not a silent version bump. |

All 35 original locations/messages are retained in AI_STUDIO_WARNINGS_HANDOFF.md.
This review must report the actual remaining counts, not claim warning-free CI.
Dependency/toolchain/target migrations should stay visible as follow-up work.
No release or version bump is performed by this corrective PR.

## Verification

New regressions cover swipe/tap selection, no wraparound, cancelled drags,
guest gating, rapid requests, private inbox read isolation, child/modal/category
isolation, scroll retention, account reset/restoration, credential failure and
legacy launcher resource resolution. Exact build/test/lint results belong in the
closing PR description; source implementation alone is not proof they passed.
Physical Samsung swipes, system edge-back, TalkBack and launcher rendering still
need device acceptance and must not be inferred from CI.

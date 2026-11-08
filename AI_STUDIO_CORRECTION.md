# Correction of AI Studio main 011db149

The owner requested reversal of quality-gate and unrequested UI/navigation
regressions while retaining useful cleanup. AI Studio commits directly to main;
this corrective branch starts at 011db1493e05dd54d51e2d19ecdc83d97d9440f9.

Restores the previously reviewed lint warnings-as-errors gate and default
abort-on-error behavior, AGP 9.5.1 pin, shared 8dp flat buttons with loading text,
and account-keyed pager state with settled-page activation. These implementations
are restored from 88e015d, rather than introducing another design or navigation
rewrite. The previous custom spinner is restored with the button implementation
to keep this correction limited to the established behavior. Its default color
uses HavenColors.TextPrimary; current callers retain their explicit colors.

Retains removal of obsolete sample components/tests and corrected callback,
semantics-tree and named-argument test calls. Adds regressions requiring sign-out
and subsequent sign-in through a guest session to start at Offers without a
spurious sign-in prompt. Existing rapid navigation, cancelled swipe, restoration,
modal and category-strip tests remain intact.

## Verification

Local source diff and git diff --check passed. No local Gradle downloads/builds
were run, per the repository data budget. CI must pass debug/release builds,
unit tests and both lint variants with strict enforcement. CI/device/rendered
acceptance is not implied by restoration from a historical revision.
Acting maintainer/agent: review CI failures immediately; outstanding runtime,
light/dark button and physical-device checks have a 2026-11-09 review target.
Retain exact SHA, command, environment and results before closing those checks.

Issues #22, #23 and #25 are separate work and are not closed by this correction.

CI run 42482255141 passed debug/release builds, tests, lint, dependency and
signer/provenance checks at 1853bce. A subsequent theme-default correction
requires fresh CI. Review suggestion to add a Google button outline is rejected:
the current owner explicitly requires reversal of unsolicited UI changes and
NEXT_AGENT.md prohibits returning decorative outlines; no new boundary is added.
Qodo review is unavailable because its subscription is inactive, not passed.

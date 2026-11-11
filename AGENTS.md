# Android working context

Read `NEXT_AGENT.md` first, then `prd.md`, `MOBILE_OFFERS_FEED.md`, and `PROJECT_INFO.md`.
The Haven backend defines the contract. No runtime samples or simulated actions
are allowed on integrated screens. Use theme tokens and match the prototypes;
clickability alone is not a reason to underline text. Run build/tests/lint in CI
to preserve the owner's local data budget. Report device verification separately.

## Release rules — 2026-11-10

At most one release per Africa/Lagos calendar day. No new features without
explicit approval. Feature releases require at least three days preparation and
at least 72 hours between feature-bearing releases; mixed releases count.
Check actual release history before publication; cadence is not authorization.
Release Markdown must be concise and user-facing. Store internal CI/review
evidence and device checklists separately. Do not release documentation changes.

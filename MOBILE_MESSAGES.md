# Native Messages inbox — Android handoff

Backend branch: `feat/mobile-messages-api`, API PR and release v0.8.4 pending.
Android branch: `feat/native-messages-inbox`.

## Contract

`GET /api/v1/conversations` uses the shared authenticated Haven client and
returns 20 current-person conversation previews ordered by activity. Pass the
opaque `next_cursor` as `after` for the next page. Rows contain conversation
and other-person IDs, display name, handle, nullable avatar URL, actual last
message text/time, and server-calculated unread state. The response is private
and not cacheable. Backend owns identity scoping, unread semantics and ordering;
Android must not infer or mutate them locally.

## Runtime behavior

The bottom Messages tab loads the real inbox when signed in. Guest selection
opens the real sign-in flow. Loading, empty, error/retry and pagination states
are based on API results. Account changes/sign-out clear prior rows. A row opens
Haven's existing authenticated `/conversations/{id}` web thread in a themed
Chrome Custom Tab. It does not navigate to `MessagesViewModel`,
`SampleConversations`, or the old simulated thread.

This slice does not provide native thread history, sending, offer attachments,
read mutation or live updates. Do not describe those as native capabilities.
The feed/profile theme tokens remain the source of light/dark colors. The
prototype reference is `prototype/haven_messages/screen.png`.

## Verification

Unit tests cover response parsing, first/next page behavior, and clearing on
account exit. Robolectric checks the real-row presentation in both themes and
captures CI screenshots. Verify CI and compare on the AI Studio device before
accepting visual fidelity. CI debug signing is separate from the pinned Studio
signer used for App Links.

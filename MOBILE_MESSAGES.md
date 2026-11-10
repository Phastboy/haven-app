# Native Messages inbox — Android handoff

Backend PR #51 is merged; release v0.8.4 is deployed. Production returns 401
with `private, no-store` when `/api/v1/conversations` is requested anonymously.
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
prototype reference is `prototype/haven_messages/screen.png`; inbox rows include
its right chevron, participant initials/avatar, unread marker, preview and time.

## Verification

CI run [41366162809](https://github.com/Phastboy/haven-app/actions/runs/41366162809)
passed build, unit/Robolectric tests, Android Lint, and light/dark screenshots;
the merged-main run [41369778485](https://github.com/Phastboy/haven-app/actions/runs/41369778485)
also passed. Compare the full screen on a supported Android device before claiming
device-level visual acceptance. CI debug signing is separate from the pinned
installed APK signer registered for the Android OAuth client.

# Haven Android v0.0.2

Patch release target: versionName `0.0.2`, versionCode `12`, package
`com.aistudio.haven.xvykpt`, unchanged signing identity. The owner authorized
resolving PR #24 reviews, merging and publishing this release. Devices can update
from v0.0.1/code 11. Backend versioning and deployment are independent.

PR #24 closes issues #19 and #21: interior swipe navigation between root tabs,
complete lint cleanup, current dependencies and Kotlin-only app/test sources with
Ktor CIO networking. Credential rotation, sign-out, permanently invalid keys,
foreground refresh, scroll restoration and SSE heartbeats have focused regressions.

Build/test/lint and signer/version checks run in CI. The published release event
builds and attaches `haven-v0.0.2.apk`, `SHA256SUMS`, `manifest.txt` and
`apk-provenance.txt` automatically. Verify the exact merged/tagged commit and
workflow outcome before claiming publication complete. No manual APK upload.

Physical Samsung edge-back/touch, TalkBack, launcher, production image decoding
and authenticated live acceptance remain separate. The owner will open issues
for device findings; this release does not certify those checks or the full launch
journey. Historical release handoffs do not supersede this release authorization.

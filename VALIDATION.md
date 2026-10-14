# Liquidify 1.0.0 Beta validation

The back-navigation follow-up builds 677 extension classes and 97 patches, with
34 Python/Java checks passing. Exact-host and native-hook linkage, strict patch
application, ZIP checksums and release validation pass. The physical-device
transition recording and installation are pending while the Pixel is locked.

The first 2026-10-14 player/navigation correction contains 675 extension classes and 97
ordered patches. Exact-host linkage and the additional native-hook linkage audit
report no missing types, methods, fields or duplicate classes. All patches pass
the Manager strict diff engine against the pinned TIDAL stock APK. The 32
Python/Java checks pass. Packaging requires successful checks of the current
extension bytes and every patch checksum.

The corrected ZIP was imported and installed through Liquidify Manager on the
physical Pixel 8. Expanding the mini-player by tap and upward swipe, opening the
queue, navigating to Search and Collection, and opening design settings passed.
Home/navigation and the player were inspected in light and dark themes, then the
original System theme was restored. No new crash-buffer entries appeared during
these checks. Playback stayed paused. The original player failure was an invalid
DEX return type in the centering hook, now corrected to
`Arrangement$HorizontalOrVertical`. Icon feedback no longer paints glass circles;
the Home settings circle was removed, retaining its touch target.

Five Manager JVM tests cover patch ownership, explicit compatibility, hidden
dependencies and patch files, unchanged Radiant metadata, and beta version
ordering and beta import filenames. `tools/validate_release.py` verifies every payload checksum, the exact
version/manifest and unchanged upstream Radiant patch files (Git checkout line
endings are normalized for the source comparison; ZIP checksums remain exact).

The post-release UI correction checks shared static/runtime muted text tokens,
the persistent playback material over extreme artwork. Navigation uses the
library catalog's light glass material and original selection lens, with a
single animated black icon row. The blue tinted duplicate row is omitted; only
the glass panel is captured for the combined selection backdrop. The library's
original translucent paints are used without the previous custom contrast film.
Album and playlist native controls retain their actions, use equal-width glass
surfaces. Secondary action rows are unfilled, compact and reflow when native availability changes after loading. Detail toolbars use 48dp touch targets around 40dp glass circles. My Tracks
has no AppBar elevation frame. Page-local glass no longer samples a backdrop
containing itself. Home, My Tracks, album and playlist were inspected in both
system themes on emulator-5554 without starting playback. This corrected local
patch ZIP is newer than the published v1.0.0-beta release assets.

The release workflow builds Manager from the same Git commit, runs its JVM tests,
verifies APK signing and publishes the prepared, validated patch archive alongside
metadata and checksums. Its GitHub Actions result is the evidence for the released
Manager APK. Local debug builds use a separate test signing identity.

`reports/manager-roundtrip.json` records the latest complete local Manager ZIP
import, patch, signing, installation and APK readback. That report applies only
to its recorded ZIP SHA-256. It does not independently certify hardware audio.

Muted Majik acceptance is preserved in `reports/output-qa-alpha22.json`, including
two autonomous track transitions with the app absent and emulator screen asleep,
native transport/volume, queue edits, shuffle/repeat, reconnect and paused handback.
The original 148 queue entries and their exact URI/metadata, source, volume, mute
and queue modes were restored with transport stopped. Those checks precede the
Beta branding and stricter Manager filtering.

No audible quality assessment or new unmuted playback is part of this release.
Physical Android Doze, TalkBack, alternate receiver hardware and extensive
network/queue edge cases remain outside the tested scope. The process-local
End of track timer showed approximately two seconds polling latency into the
next track. See LINN_OUTPUT.md for setup and capacity-staging limits.

## UI refresh, 2026-10-14

This update adds static refraction with reduced motion, the monochrome catalog
navigation lens, native/Compose sheet presentation, playlist sort/search styling,
TIDAL/Liquidify settings tabs and restored original Developer/WIP patch choices.
Current build identities and check results are recorded in
`reports/ui-refresh-validation.json`, `reports/upstream-compatibility.json` and
`reports/upstream-source-verification.json`.

The separate test app installs and reaches the initial consent screen on the
Pixel_10 emulator without an AndroidRuntime crash. It has no signed-in TIDAL
session. This does not certify authenticated playlist, settings, player-menu or
physical-phone UI behavior. The existing signed-in emulator installation was
preserved after the normal debug-signature update failed. Automatic approval
review rejected extracting/reusing its embedded signing key; no such operation
was performed. A full Manager patch/install roundtrip remains an acceptance item.

### UI regression fix, 2026-10-14

Pixel 8 crash logs confirmed the premature ComposeView measurement in
PlaylistSortMenu and unsupported RectangleShape in ComposeSheetGlass. The local
instrumentation in tools/AndroidUiRegression.java reproduced both failures in
the separate test APK before their fixes. With the corrections it passes three
real sort-dialog open/layout/dismiss cycles and attaches/draws the fullscreen
rectangle surface. It also covers the safe fallback when a source and target
share a window. All 34 existing tests, 101 strict native diffs and 17 optional
patch configurations pass. See reports/ui-regression-fix.json for phone results.

The corrected ZIP was subsequently imported and installed through the existing
Manager on Pixel 8. APK readback verified the exact current helpers, native hooks,
resources and signature. Fullscreen opened repeatedly; Alphabetical sorting
persisted and Custom order was restored. The player sheet opened with one grip.
Uniform accessory and menu films were visually inspected. There were no new
TIDAL crash entries after installation at 17:41:20; playback stayed paused.

# Liquidify 1.0.0 Beta validation

The current patch build contains 603 extension classes. Exact-host linkage reports
no missing types, methods, fields or duplicate classes. All 96 ordered patches
pass the Manager strict diff engine against the pinned TIDAL stock APK. The
separate local test APK passes signing and ZIP alignment. All 28 Python/Java
checks pass.

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

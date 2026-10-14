# Liquidify 1.1.0 Beta

- Adds the updated Liquid Glass interface, native sheet lifecycle handling, playlist search and sort controls, and refined gesture and accessory feedback.
- Improves patch compatibility checks and Manager patch selection, including clearer handling of optional upstream patches.
- Fixes navigation, backdrop capture, icon motion, and player presentation regressions.
- Refines Linn OpenHome discovery and output switching while keeping playback direct from the renderer.

Install `Liquidify-Manager-1.1.0-beta.apk`. The Manager fetches `patches.zip`
and `data.json` from this repository. `SHA256SUMS` covers all downloadable assets.
Requires Android 13+ for the patches and the pinned TIDAL 2.200.0 (10004).
The Manager itself supports Android 7+.

Manager is built from this release source and signed with the project's permanent
release key. Older debug-signed test builds may require a clean Manager install.

See LINN_OUTPUT.md for the supported setup and hardware validation scope.

Credits: Radiant Lyrics / meowarex and contributors, Square / Lelonio,
AndroidLiquidGlass / Kyant0, and Inter. GPL and third-party notices are included.

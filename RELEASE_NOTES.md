# Liquidify 1.0.0 Beta

- Liquidify patches and Liquidify Manager share one release and update repository.
- ninocss is identified as Editor, with explicit Radiant Lyrics fork credits and upstream links.
- Liquidify patches are the default. Radiant Lyrics patches remain unchanged and appear only with explicit compatibility and available dependencies; they are opt-in.
- Hidden/unselected patch files and extension classes are excluded from the patch plan.
- Liquid Glass navigation, native menus, theme selection and the Linn OpenHome output are included.
- Beta updates are discovered from release tags, including prerelease versions.

Install `Liquidify-Manager-1.0.0-beta.apk`. The Manager fetches `patches.zip`
and `data.json` from this repository. `SHA256SUMS` covers all downloadable assets.
Requires Android 13+ for the patches and the pinned TIDAL 2.200.0 (10004).
The Manager itself supports Android 7+.

Manager is built from the release source and signed with the project's permanent
release key. Older debug-signed test builds may require a clean Manager install.

The Linn implementation was tested on a real Majik with hardware mute, including
two autonomous track transitions after controller termination. Acoustic quality,
physical-phone Doze, TalkBack and alternate receivers remain outside this scoped
acceptance. The End of track sleep timer showed approximately two seconds polling
latency into the next track. See LINN_OUTPUT.md for setup and boundaries.

Credits: Radiant Lyrics / meowarex and contributors, Square / Lelonio,
AndroidLiquidGlass / Kyant0, and Inter. GPL and third-party notices are included.

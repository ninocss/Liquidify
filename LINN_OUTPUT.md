# Linn OpenHome output — Liquidify 1.0.0 Beta

The native TIDAL broadcast registry now includes a Linn provider. The Output
button opens the existing 320 dp anchored AndroidLiquidGlass popup. Device names
and UUIDs come from SSDP/device descriptions. The active row appears after the
provider confirms the transferred queue and transport. Refresh and closing the
popup stop discovery without disconnecting playback.

## Setup and supported content

Use the pinned TIDAL 2.200.0 (10004) APK and Liquidify Manager 1.0.0 Beta. Import
the release's `patches.zip`. Phone and renderer must share a LAN.
Sign in to TIDAL for the renderer once in the Linn app. The Android account is
not copied to it. A missing valid Linn TIDAL login produces an explicit error.

Open an online music queue, open Output and choose the discovered Majik. The
provider transfers the queue, preserving duplicate-title occurrences, selected
item, position and pause/play state. Existing volume and mute are preserved.
The application does not force mute during normal use.

If multicast is blocked, “Linn not found? Add by address” accepts a private IPv4
address or an actual discovered device-description URL. Some firmware does not
answer unicast SSDP; then the description URL is required. Emulator NAT required
this fallback. No firmware-specific description path is guessed.

V1 supports one renderer and online TIDAL music. Offline downloads, uploads,
video and multiroom groups are excluded. TracksMax is checked before mutation.
A reversible handoff temporarily stages new entries beside the old device queue,
so both must fit. A full renderer may require clearing its old queue through Linn
before connecting; queues are never silently truncated.

## Integration

LinnProvider implements the pinned native broadcast provider interfaces.
LinnPlayer implements the central k0 player interface. UI buttons, media session
commands and sleep timer continue through native dispatch. The bridge follows
native activation/deactivation and progress lifecycle. Original Cast, Sonos and
TIDAL Connect providers retain their registry entries.

The device owns playback and its queue. No relay server or Android audio stream
is used. This firmware accepts `tidal://track?version=2&trackId=…&token=…`, using
the valid token identifier reported by its own OpenHome OAuth status. Token
secrets, passwords and the Android account are not transferred. URI/token data
remain in memory. Reconnect persistence stores native track metadata and remote
occurrence IDs, without device stream URIs or OAuth identifiers.

Native queue edits and shuffle/repeat changes reconcile through the same adapter.
Repeat Single uses a one-entry repeating device queue while retaining the native
queue for subsequent navigation. Manual Next follows TIDAL's native behavior and
leaves Single repeat. Reattachment preserves an already shuffled occurrence order
without shuffling it again. Device IDs map individual occurrences. GENA
subscriptions validate IDs and sequence numbers. Bounded SOAP polling remains
available when callbacks cannot cross emulator NAT or a firewall. Discovery has
its own executor, so scanning cannot block playback commands.

Remote volume uses VolumeProviderCompat and OpenHome NoUnmute operations.
App restart reads the device session without replacing its queue. External TIDAL
queue changes can be adopted. Non-TIDAL content is left untouched with an error.
Network loss leaves local playback paused. “This phone” pauses Linn and returns
the current native queue and position to the local adapter, initially paused.

Device HTTP is confined to private LAN endpoints. Redirects, URL credentials,
DTD/entities, oversized XML and HTTP headers are rejected. Actual SCPD supplies
action argument names. UUID identity supports discovery after an address change.

## Hardware evidence and limits

The real Majik identifies as Majik DSM/4 (2020), firmware 4.118.607, with a
1000-entry OpenHome queue. Renderer TIDAL OAuth authentication was already valid.
No credential-changing operation was used. TIDAL Connect capability is not
inferred from SSDP; this delivery uses the verified OpenHome path.

The user explicitly authorized muted playback. Transport QA therefore confirmed
hardware Mute before playing. A preliminary controller inserted two TIDAL
references with metadata and independently observed automatic advancement after
the controller exited. The integrated TIDAL app then transferred a real 25-title
queue and exercised native transport and remote volume. Two automatic track
transitions were observed with the app process absent and emulator screen asleep.
Queue removal, reorder, Shuffle, Repeat All and Single, paused handback and
reattachment were exercised. The End of track sleep timer paused the renderer
about two seconds into the next title in the polling setup; it is not a precise
device-side boundary timer. Original renderer queue/source/volume/mute are
privately backed up and restored after testing.
Raw URLs, account identifiers, queue backups and screenshots remain in ignored
research files and are excluded from release archives.

`reports/output-qa-alpha22.json` records the muted hardware acceptance of Alpha22
and its remaining gaps. The Beta preserves that integration and adds release
branding and strict compatible-patch selection. `reports/manager-roundtrip.json`
binds the latest verified Manager-installed APK to its own exact ZIP.
Historical Alpha21 evidence applies to that earlier menu-only version.
Mute permits verifying the streaming pipeline, transport and autonomous playback;
audible sound and acoustic quality were deliberately not assessed. Physical
Android Doze, TalkBack, alternate receiver hardware and every native-provider or
sleep-timer regression require their own acceptance.

Primary reference: [OpenHome platform](https://openhome.org/pages/use/).

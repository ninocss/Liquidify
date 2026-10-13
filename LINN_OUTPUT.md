# Linn OpenHome output — Liquidify 1.0.0 Beta

## 2026-10-13 responsiveness revision — physical acceptance pending

The last renderer appears immediately as an unverified remembered row. Its address
is checked alongside an incremental 2.5-second SSDP search; each valid renderer is
published separately. Refresh closes the old UDP socket. Description workers are
bounded, and Linn rows/confirmed selection are delivered directly to the popup,
so a slow native output provider cannot hold up their appearance. UUID, advertised
boot/config IDs, leases and network changes govern discovery refresh. A remembered
row is not proof that its renderer is online.

Device descriptions and actual SCPD argument definitions use bounded five-minute
process caches. Discovery loads only the device description; capabilities are
warmed separately. Each handoff reads renderer TIDAL OAuth once. SourceXml is
cached and invalidated by Product events. App reattachment checks player readiness
every 250 ms for up to ten seconds rather than imposing the old startup delay.

Confirmed session snapshots are stored separately for each UUID, with native
occurrence keys and remote IDs but without device URIs or OAuth identifiers.
Choosing This phone disables automatic reattachment while keeping the snapshot.
A transfer retains the longest ordered set of matching occurrences only after
confirming actual IDs, URIs and metadata. Identical queues require zero Insert or
DeleteId calls; append/removal/reorder changes stage only the necessary entries.
Staging capacity still includes the old queue and newly inserted entries. Writes
are serialized, lost Insert replies are reconciled, and rollback stops on foreign
edits or uncertain writes. Repeat Single keeps its special one-track queue.

Validated GENA payloads update native state directly. With recent callbacks,
periodic position reads replace full observations between 30-second reconciliations;
without callbacks, full polling remains at two seconds. New controls invalidate
older observations. Pending absolute volume controls are coalesced. Track selection
and seeking confirm asynchronously on the control worker, yielding between reads.
New track targets supersede old ones; Pause/Play/Seek intent during a pending track
change applies only after its ID is confirmed. Seeking the existing stream does not
restart it or change its transport state. Selection deadlines are bounded and the
popup's progress messages no longer reset its timeout. A pending Linn connection
can be cancelled with guarded recovery.

The popup supports direct Linn-to-Linn switching with one active output. It prepares
the destination before confirming the old renderer's pause and moving current
position/selection. Native output selection changes after destination confirmation.
This phone handback remains paused and uses current device position/ID. Cast, Sonos
and other native providers continue through their existing disconnect callbacks.

`tests/LinnPerformanceChecks.java` exercises local HTTP, UDP and GENA fixtures for
incremental discovery/cancel, lazy metadata caches, identical/full queues, duplicate
occurrences, append/reorder, cancelled/partial commits, foreign edits, uncertain
deletes, asynchronous controls and event SID/sequence validation. The Linn checks
also run in the Build workflow. `reports/linn-performance.json` binds this revision
to the rebuilt patch ZIP and records validation and remaining hardware acceptance.
The historical evidence below does not establish latency, audible playback or
physical Android lifecycle acceptance for this revision.

## 2026-10-12 reliability revision — phone acceptance pending

This revision separates an empty-queue Ready selection from a playback handoff.
Selecting Ready does not change the device source, queue or transport. An existing
TIDAL Playlist source can be adopted; other sources remain untouched until an
explicit music start. Source selection uses the advertised Playlist type/index,
including renamed sources. Capabilities and renderer OAuth are checked first.

SOAP faults retain HTTP/action/code context with allowlisted descriptions. LAN
sockets bind to Wi-Fi/Ethernet rather than changing the process network. Read-only
requests get at most two transient retries; writes do not. Lost Insert responses
are reconciled using the exact queue order, URI and metadata. Ambiguous writes
stop recovery rather than risking duplicate tracks or destroying another queue.
Rollback never uses DeleteAll and preserves the original failure if recovery fails.
The UI distinguishes checking, ready, transferring, confirming and reconnecting.
The 2026-10-13 follow-up waits for the selected track ID after asynchronous SeekId
before applying position and pause. Position zero does not issue an unnecessary
seek against the previous stream. One deadline bounds the complete selection.
Active transport buttons update the local player state immediately and run on a
dedicated serialized control worker. Status polling and OpenHome events run on a
separate worker, so a slow status request cannot queue ahead of Play, Pause or Seek.
The fallback status interval is two seconds. Output rows draw simple phone and
hi-fi renderer icons that remain legible at the existing touch size.
Buffering gets a bounded confirmation interval; a failed observation no longer
closes the session. Three failed cycles trigger UUID rediscovery with backoff.

`tests/LinnFaultChecks.java` uses an actual local HTTP server to test SOAP faults,
redaction, read retries, non-retried writes, delayed playback, response deadlines,
lost Insert replies, duplicate occurrences and simultaneous foreign insertion.
These protocol checks do not prove native Android lifecycle or audible playback.
Earlier Alpha22 hardware evidence below is historical and is not acceptance of
this revision. See `reports/linn-connect-fix.json` for current artifact hashes and
remaining phone/renderer checks. No successful phone handoff is claimed until
the original failure is reproduced and the revised build passes on that phone.

On the Pixel 8, r3 reproduced a paused-handoff confirmation timeout with all SOAP
actions returning HTTP 200. A playing handoff then reached Active and audible
Majik playback was confirmed by the user. The r4 correction still needs phone
acceptance; that r3 result does not certify r4 or the full acceptance matrix.

For read-only phone diagnostics run `python tools/linn_collect_diagnostics.py`.
The default package is the installed TIDAL; the separate test APK uses
`--package dev.nino.tidalglass.test`. Output stays under ignored `research/`.
Open Output, choose the Linn once, then collect again. The collector neither
clears logs nor changes accounts, playback or installed packages.

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

Choose Output in the main Settings page to select a device before choosing music,
or open an online music queue and use the player's Output button. Choose the discovered Majik. The
provider transfers the queue, preserving duplicate-title occurrences, selected
item, position and pause/play state. Existing volume and mute are preserved.
The application does not force mute during normal use.

If multicast is blocked, “Linn not found? Add by address” accepts a private IPv4
address or an actual discovered device-description URL. Some firmware does not
answer unicast SSDP; then the description URL is required. Emulator NAT required
this fallback. No firmware-specific description path is guessed.

V1 supports one active renderer, renderer switching and online TIDAL music. Offline downloads, uploads,
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

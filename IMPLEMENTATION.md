# SquareGlass alpha20 implementation

## Alpha20 navigation icons and accessory readiness

The navigation tab content is composed and drawn once, above both the capsule
surface and moving selection lens. The hidden blue-tinted row and combined
icon backdrop are removed. A separate non-drawing hit area retains the native
indicator drag behavior; selection semantics, labels and original tab callbacks
remain in the visible row. All tab ink uses the existing theme palette.

Both navigation and mini-player use `AccessoryGlass`. Before the first native
recording, the capsule draws a readable theme surface. The first recording and
frozen/live handovers explicitly invalidate both accessories; ordinary captures
never write snapshot state. Test-only diagnostics expose first capture/draw
uptimes and handover counts without media or account data.

`NativeViewBackdrop` gives native transactions one traversal to install their
animations, then resumes capture on the first safe pre-draw. It retries only
while actual finite view animations, layout transitions or overlapping fragment
roots are present. The general Activity page root can have multiple visible
siblings without blocking capture. Infinite content animations do not block it.
The independent last sample bridges tab replacement. Reattach, lifecycle resume
and full-player disposal request a fresh sample. Activity accessories keep the
native page backdrop while the separate full-player window owns its own
background, including the player entry/exit frames. No fixed 700ms timer remains.

## Alpha19 persistent accessories and motion

Collection's native parent constructs its original category list and remembers
its original Downloads callback before calling one `SquareLibrary.Grid`. The
Downloads tile follows Artists in that grid. Its native route, offline behavior
and conditional Purchases visibility remain intact; no overlay or translated
second row is used.

`AccessoryGlass` supplies the exact same continuous capsule, 8dp blur, 24dp lens
and 40% day/night film to the library navigation row and mini-player. The library's
draggable selection indicator and interaction resource remain mounted. External
selected-tab changes animate the indicator without dispatching another route.

`NativeViewBackdrop.transition` resolves the last stable recording into an
independent accessory-region image before tab selection. ImageReader and the
offscreen HardwareRenderer are closed after the image is copied. The existing
native-animation detection is retained in Alpha20 with frame-based retries:
transitional Fragment views are not redrawn for a new sample. A detach retains the last sample until the
source is released. No Activity-wide capture includes the glass itself.

`MiniProgress` reads native playback position/duration every 250ms while its view
is visible and the lifecycle is resumed. Unknown durations hide the line; queue
handoff positions are clamped. There is no new seek gesture, fake timer or player
callback. `PlaybackProgressChecks` covers unknown/negative/overshoot/large times.

`SurfaceMotion` gives menus a 220ms anchored scale/reveal and sheets a 260ms reveal,
without competing with Material's sheet translation. Custom timer content has a
small entry translation, inline Settings expansion is eased, and native/settings
slider thumbs share a restrained spring. Authoritative slider values remain
immediate. Additional drag/highlight/selection animation snaps or becomes static
when Reduce motion or Android animation disabling is active. Native page and
player transition ownership remains unchanged to preserve the Alpha18 lifecycle
and cancellation corrections.

## Alpha18 layout and decorative motion

`PlayerArtwork.frame` records the native moving portrait container. Cover and
footer offsets use `localPositionOf` inside that container, so native drag
translation is never canceled by a second window-relative correction. The native
pager, animated cover and controls remain mounted. `PageInsets` removes the
native top margin in layout instead of translating an undersized page, preserving
the bottom edge. My Tracks' AppBar uses continuous SCROLL without snap/min-height
handoff; its retained toolbar remains outside the collapsing artwork.

Retained page bindings keep their decorations, colors and inset shells on detach,
reattach observers once, and initialize paint before drawing. Detail pages have
one palette owner. A fragment-scoped weak cache preserves loaded collage
artwork when its native view is recreated; decoration height is defined before
first layout. `NativeViewBackdrop` retains its last glass sample while native
Fragment roots overlap or legacy view animations are running. Alpha18 also used a 700ms lifecycle
fence for disappearing roots; Alpha20 replaces that delay with traversal and
animation checks. The capture guard avoids replaying
`View.draw` during the real transition, where a second draw can advance and
prematurely finish disappearing-child animation state. Header opacity derives from native list scroll plus local
AppBar offset, never a page transition's window translation. The Playlists
overview skips the patch hero entirely; actual playlist details retain artwork.
Downloads and category entries share `SquareLibrary.Tile` geometry.

The legacy `TidalSearchView` keeps its input, cursor, clear action and IME logic;
supported containers receive a single glass surface with the opaque native fill
cleared. The native Compose input is covered by patches 150/151, preserving
layout, callbacks and default masks while replacing container, shape and cursor.

`IconMotion` transforms only Compose icon drawing. `NativeIconMotion` scopes
legacy favorite drawing to the native ImageView child, leaving the label and
hit region stable. Touch observation consumes no events; native state remains
authoritative. Like particles require a state change after that button's tap;
failed actions and initial composition cannot produce success particles.
One shared scheduler selects one eligible icon after 5 seconds quiet and at
12-second intervals, rotating wobble, turn and double-pulse styles. Detached,
unfocused, obscured-window and IME views are ineligible. The native activity touch
hook, player pointer observation and menu registry suspend standby. Existing
reduced motion and `ValueAnimator.areAnimatorsEnabled()` gate every extra draw.
Patches 152-155 cover player favorites, Home/Collection Settings and native
legacy favorite actions. No callback or action model is replaced by motion.

## Alpha17 native pages and recommendation transactions

`PageCoordinator` observes native Fragment view creation and resumption. Each view
gets one binding; retained views reattach listeners and keep their existing native
RecyclerView, adapter, callbacks and scroll state. Custom entry alpha/translation
animations were removed so native navigation owns its transition. Native backdrop
sources deduplicate attach subscriptions and retain a reattach listener after detach.

Albums use a full-width square cover, no CardView radius or compat padding, a
cover-derived fade and surface, and native actions below the metadata. Collection
overview lists use a large collage from up to four loaded contained covers, falling
back to native category drawables. My Tracks keeps its native collapsing AppBar;
its toolbar is moved into a fixed inset shell so collapsing the artwork cannot
clip navigation controls. Downloads keeps its native tabs and offline behavior.

`HeaderProgress` clamps the first 64dp of scroll to a smoothstep. `HeaderFade`
paints a vertically fading, cover-toned scrim and becomes fully transparent at the
top. Safe toolbar placement accounts for both native padding and current window
origin, preventing status insets from being applied twice.

Search retains the native arrangement and routing. `PageGlass` installs local
AndroidLiquidGlass paint behind supported native input/row containers; it avoids
capturing its own containing page. Other controls receive adaptive ripple surfaces.
Native supported context actions supply swipe behavior across actionable media rows.

`RecommendationUpdates` starts only after the native Add busy guard. It retains
the pre-operation item snapshot, disables the tapped Add control, and lets TIDAL
perform its original network operation. The result and matching playlist event
update the native view model but share one deferred native DiffUtil dispatch.
The matching native Add event uses TIDAL's existing incremental item factory and
append callback even for one item on a fully loaded list. This avoids the native
single-item full reload. Immediately before dispatch the interacted recommendation
and its current window position are captured (falling back to the visible row);
DiffUtil maps that row to the new list, or its nearest surviving neighbour. Nested
pre-scroll restores the collapsing AppBar and list together. Only
this transaction suppresses the native scroll-to-top and automatic recommendation
refill. Manual reload and unrelated playlist events retain native behavior. Failure,
detachment and a ten-second timeout restore the pending control and flush changes.

A delayed native rebind of the same playlist revision is recognized for 15 seconds
on the same view. Its metadata is retained without clearing items or reloading
suggestions. The guard matches UUID and actual fully loaded item count rather than the stale
native Add event metadata;
manual refresh clears this guard. A different revision follows the native reload.

The three recommendation hooks are exact-version patches 147-149. Their ABI bridge
uses the pinned native presenter, model, callback and view directly; no reflection
or replacement playlist service is introduced.

Pinned host: stock TIDAL 2.200.0 (10004), Android 13+. Native routes, models,
callbacks, media pipelines and account ownership remain in TIDAL.

## Runtime design

`SquareSettings` owns versioned app-local SharedPreferences. The Manager selection
is only the initial theme. `SquareRuntime` keeps AppCompat's night mode and the
Activity Configuration consistent; both day/night resource tables remain present.
`DesignState` invalidates Compose consumers without replacing native navigation.
The native main settings list exposes expandable `DesignEntry` controls for theme,
glass presets, component toggles, reduced motion, blur/tint/refraction and swipe
choices. The pinned Offline-mode model callback identifies this page; shared
subpage renderers do not receive the entry. Choices and sliders expand inline.
The System selection follows the device without changing its global preference.

`NativeViewBackdrop` records only the native page sibling, excluding floating
chrome. The player captures only its own background. Glass-off uses opaque
surfaces; page capture stops when navigation and mini-player glass are disabled.
No second Compose/Kotlin runtime or native media library is shipped.

## Navigation, detail pages and player

`LiquidBottomTabs` renders one icon/label row. Its independent shell stays painted
under the selector throughout horizontal dragging. Platform touch slop, RTL,
cancellation, native callbacks and selected accessibility semantics are retained.
New motion follows the reduced-motion preference and Android animator state.

`SquareDetails` adapts native playlist, mix, album and track views. Playlist
metadata/actions form a content-sized vertical flow instead of depending on the
original fixed header height. Covers fade into a strongly tinted, cached artwork
sample. Coil's pinned CrossfadeDrawable ABI is compile-only; hardware bitmaps are
copied to a readable sample without changing the host image or its bounds.
The native AnimatedAlbumCoverView remains mounted. Attach/detach observers and
weak bookkeeping avoid retaining abandoned pages. Toolbar/menu and caption
colors are scoped to these detail pages.

Player transport controls retain native callbacks and clear icon shapes. Secondary
Market buttons use the tertiary style on one common glass bar. Cover-derived
backgrounds receive a strong adaptive film; enabled secondary paint has a minimum
ink alpha while disabled states stay disabled. Native Lyrics and queue navigation
remain available. Existing Lyrics viewport clipping/fades are retained.

## Native actions and gestures

`SquareSongMenu` projects native ListAdapter models, grouping the remaining
options above available quick actions. Native item creation/binding and click
callbacks execute every action; no copied network action, label matching or
assumed item index is used. The exact host action types are pinned. Unavailable
or disabled actions remain unavailable. Labeled native button containers use
AndroidLiquidGlass backgrounds, adaptive grid spans, press feedback and a capped
scrollable footer at large font sizes. Adapter observers and compositions are
removed with the presentation.

`LiquidMenuSurface` uses the vendored AndroidLiquidGlass backdrop/effect chain.
One RenderNode capture per presentation excludes the menu window itself. Screen
coordinates bridge the independent Activity, popup and sheet origins. Native
parent translation refreshes glass placement only when its position changes.
Popup window lifecycle/saved-state owners are supplied before Compose attaches;
button measurement occurs after attachment. Glass-off uses an opaque themed film.

`SquareGestures` arbitrates horizontal swipes against vertical scrolling and
cancels pending long-press work after claiming a drag. A committed gesture opens
the row's native context action pipeline with a narrowly scoped request. Only a
menu synchronously initiated on that Activity can consume it; delayed adapter
population remains supported. Cancelled gestures restore the original row. A
parent-overlay drawable paints only the edge exposed by the moving row, with an
action icon and label. The original foreground and native handlers remain intact.
Threshold haptics use Android preferences; return motion obeys the app's
reduced-motion option and platform animator state.

Stationary completed taps anywhere in the native context-menu pipeline retain
anchored native callbacks even with long lists or large text: toolbar, song-list,
card and player actions share this policy. Their constrained native RecyclerView
scrolls inside the popup. A menu near the bottom opens above its anchor and stays
inside the visible window. Long presses, unanchored openings and windows too small
for a native action below the header retain sheets. Native filtering, dismissal and Activity
lifecycle remain authoritative. This adapter covers the pinned native context-menu
pipeline; independent purchase, upload and settings dialogs are not replaced.

## Listening, mini-player gestures and Linn WLAN (alpha9)

`NativeMiniGestures` claims horizontal/upward drags after platform touch slop,
opens upward intent at 28dp during the drag (or a short release flick), and rejects
multiple pointers, downward drags and cancellations. The native callback owns the
full-screen transition; the pill is never scaled outside its clipped slot. Horizontal
swipes call the pinned native player; previous explicitly skips instead of rewinding.
Tap/button callbacks and reduced-motion behavior remain native/available.

The Home row observes the design preference and switches between the untouched
native filter pipeline and three accessible, horizontally scrollable action buttons.
`NativePlayback` observes the real native state and advancing media position. A song
enters the local profile after 30 listened seconds; seeks and paused time don't count.
Up to 160 model copies and hourly counts are stored asynchronously in private app
preferences. Native source references are removed from the copies. Logout clears
the profile. `TimeTaste` ranks circular hour affinity, frequency, recency and
rediscovery age; queues limit repeated albums and use the original TIDAL queue
preparation/play pipeline. Existing server history is not imported.

The custom Linn/OpenHome implementation described in earlier alphas was removed
in alpha14. Output selection uses the native TIDAL receiver pipeline.

## Artwork heroes and press feedback (alpha10)

Playlist and mix headers use square, fully fitted artwork. A foreground
gradient keeps the middle of the artwork clear and meets the cover-derived page surface at the
bottom. A 24px sample is cached per drawable and theme; the list, header and page
share the same tone. Rebinding and Coil crossfade completion refresh the sample.

The native player still owns its album cover, animated cover, pager and controls.
Artwork padding, border/shadow, portrait placement and background paint are adapted. PlayerArtwork
uses the existing CoilImageLoader provider and native album request builder in a
composition-bound coroutine. Sampling runs off the main thread; missing art falls
back to native metadata. Album changes cancel stale sampling, and the selected tone
animates with the same reduced-motion preference. The fade uses the exact same
surface as the background, avoiding a seam. CoverColors checks secondary-text
contrast before choosing a surface. No image or music download service is added.

The native legacy tab route no longer adds its selected rectangular background or
press indication. The outer accessory's ripple is suppressed without replacing its
callback; the glass navbar is clipped to its capsule and retains its selector,
drag gestures and accessible tab semantics.

## Top-edge artwork (alpha11)

The portrait player pager is measured as a full-width square and placed at the
composition root top. Its native weighted slot remains allocated for the existing
controls, and the native header row draws above the artwork. The landscape path
remains native. Playlist header constraints remove the legacy vertical bias and
any rebound top margin. Page positioning uses the actual window origin rather
than subtracting system insets twice. Toolbar actions remain at their safe native
position with individual rounded contrast surfaces. A short top scrim keeps the
status bar legible; the existing bottom fade and palette extraction remain.
Alpha17 replaces the old solid toolbar restoration with a cover-toned vertical
gradient. Its opacity follows a clamped smoothstep over the first 64dp of scroll
and returns to zero when the native list cannot scroll upward.

## Validation and remaining scope

See `dist/VALIDATION.md` and current JSON reports for artifact-bound evidence.
Older `reports/pixel-qa.json` covers earlier physical Pixel builds only. It is
historical evidence and does not certify alpha11. Private captures stay in ignored
research storage and are excluded from distributables.

This remains an integration alpha, not a complete Square transplant. Native
content/routing and some page structures remain TIDAL. Full TalkBack coverage,
all-screen theme parity, downloads/offline behavior, audible playback and physical
frame-time/battery measurements still require broader acceptance. Reduced motion
controls the added effects; TIDAL's own artwork/media animations retain their native
lifecycle. TIDAL's original x86_64 ELF libraries still need Android's 16-KB
compatibility mode on the current emulator.

## Immersive portrait player (alpha12)

Portrait artwork is uniformly enlarged to 74% of the window height (capped at
2x window width), centered and cropped at the sides. Native artwork/pager
components remain mounted. A graded bottom fade joins a muted cover-derived
surface with white foreground contrast of at least 5.5:1. This player uses dark
artwork surfaces independently of the app theme; playlist artwork is unchanged.
Native metadata and transport move slightly upward; header and secondary actions
occupy two lower rows. Native favorite, more, queue, lyrics, share and output
callbacks remain present. No volume slider is added. Player system-bar icons use
the light appearance and other windows retain their normal theme behavior.

The final portrait build is checked in the PC emulator. Landscape, larger fonts,
all animated artwork and physical-device behavior remain outside this acceptance.

## Restored top actions (alpha13)

The native artist, lyrics and output header retains its original top position.
Only the secondary queue/share/more row remains below transport. Immersive
artwork and its fade are unchanged.

## Theme, library navigation and menu window source (alpha14)

Player surface and top scrim follow the active theme. Media tint and system bars
follow the same foreground. Both player tone families have contrast coverage.
LiquidBottomTabs is restored from the pinned AndroidLiquidGlass catalog; only
package/host ABI, runtime theme selection and cancellation adapters differ.
The original invisible row is a lens source, not a second accessible tab list.

The player More callback captures a menu-scoped RenderNode before native window
transitions. The bottom sheet paints that player snapshot behind its content and
samples it for glass, rather than sampling MainActivity's Home page. Captures
are released on dismissal, replaced on a new request, or timed out if no menu
consumes them. Native action callbacks and account/library ownership are retained.

Custom LinnPlayback and OpenHomeClient, their settings entry and transport hook
are removed. Native output discovery and receiver handling are unchanged.
An unsupported Majik DSM cannot be manufactured as a working TIDAL Connect
receiver by inserting a row. No Linn hardware acceptance is claimed.


## Alpha15 timer, English and additional materials

`SleepDeadline` uses elapsed realtime, generation tokens and one-shot consumption.
`SleepTimer` schedules a listener-based AlarmManager exact alarm plus a main-thread
backup and retains the bound native player only while active. Expiry invokes its
pause method, never a playback toggle. Cancellation, replacement and logout remove
callbacks and invalidate old generations. No queue contents are changed. The timer
is process-local and does not survive process death or force-stop. Physical Doze,
background playback and receiver pause remain unverified.

Timer access is appended to the native player menu projection and native queue
Toolbar menu. A dialog offers presets, 1-180 custom minutes, a live countdown and
cancellation. Its snapshot comes from the current menu/queue window, preserving
the player backdrop when the native full-screen player closes for a menu.

`QueueGlass` inserts a paint layer between the queue artwork and its controls,
without replacing native tabs, RecyclerView or bound actions. The layer captures
the separate player window once and disposes compositions when the queue detaches.
The previous standalone design dialog is removed. `GlassPanels` styles native
Settings and Wave text inputs against their flat page base, avoiding self-capture
inside the native page RenderNode. Glass-off remains an opaque surface.

`SquareRuntime` selects English app locales on Android 13+ and supplies an English
Activity Configuration; custom strings are English. `NativeTabs` shortens the
native English My Collection label to Collection. Music titles and account content
are not translated. The Manager retains its own existing localization.

## Alpha16 single sheets and responsive gestures

The player timer reuses its existing `SquareMenus` shell and captured player
backdrop. Queue access replaces the contents in its current window directly;
`Window.setContentView` is avoided on a showing window because the native Scene
transition would reattach removed Compose views in an ownerless overlay. Native
DialogFragment dismissal remains authoritative. Custom duration expands in the
same capped scrolling sheet, with no extra Dialog/AlertDialog.

`TrackEndGate` adds one-shot current-track completion: native queue item change,
position reaching duration, or repeat resetting after the final 500ms. Polling is
250ms normally and 40ms near the end; player rebinding schedules a fresh check.
Manual skip ends the selected current track as well. Normal paused time does not
expire End of track. This mode remains process-local like the timed mode.

The mini-player native skip control is conditional on gestures being disabled.
A short upward gesture triggers immediately, horizontal gestures use distance or
velocity on release, and native tap/play callbacks remain in place.

Track action previews move the actual row one-to-one up to the commit distance,
with resistance beyond it. A parent overlay paints only the exposed edge, then
returns over 200ms or immediately under reduced motion. Vertical scrolling,
multitouch, cancellation, detachment and recycled rows clean up the overlay.
Committed gestures retain the exact native context-action request pipeline.

Home no longer links to a separate design window. Its third optional shortcut is
Shuffle history, using the same real locally observed track models and native
queue preparation as time-based autoplay.

## Alpha22 native Linn OpenHome output

This integration supersedes the historical removal of the earlier custom Linn
implementation. `LinnProvider` now participates in the native broadcast registry;
`LinnPlayer` implements the central `k0` lifecycle. `NativeLinnBridge` updates the
native queue, state, progress and remote-volume path. UI controls, media commands
and the process-local sleep timer use that same adapter.

`OpenHome` discovers UUID-based device identities and reads actual SCPD argument
names. Reversible queue staging checks capacity, device TIDAL authorization and
confirmed transport before publishing the active output. Device occurrence IDs
preserve duplicate items. GENA plus bounded polling reconcile device changes.
Shuffle restoration keeps the persisted occurrence order instead of reshuffling
it. Single repeat uses one device entry; manual navigation follows native TIDAL
queue behavior. Return to This phone starts paused at the device position.

Real Majik testing used hardware mute throughout playback, including two automatic
track changes with the controller process absent and emulator screen asleep.
The final Manager-installed APK is bound to the ZIP in `manager-roundtrip.json`.
See `LINN_OUTPUT.md`, `dist/VALIDATION.md` and `reports/output-qa-alpha22.json` for
the exact checks and remaining physical-device acceptance.

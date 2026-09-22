# xx-auto — Design

Operator lock **2026-09-21**. Answers from the kickoff Q&A are law here;
`todo.md` is the work, this is the why.

> **AMENDED 2026-09-21 (same day, later) — AU1 is reopened.** The xx-maps
> kickoff established that the operator wants the car screen, and that Android
> Auto in fact works on GrapheneOS via the sandboxed Google Play compatibility
> layer. AU1's "Closed" no longer holds. See **AU1**, **AU7**, and the new
> **AU12–AU14**. The alignment is written up in
> [contracts/XX-MAPS.md](contracts/XX-MAPS.md), which is now a two-way
> contract rather than a one-way handoff.
>
> Nothing else in this document changed. xx-auto is still a phone-in-a-mount
> screen with no `INTERNET`, and that is still its default and primary form.

## What it is

A driving screen for a phone in a car mount. One activity that shows what's
playing, lets a thumb hit play / skip / thumbs, opens maps, and dials a
favourite. Then it gets out of the way.

The default build is not Android Auto: nothing is projected, no head unit, no
Play Services, no Google. **Amended 2026-09-21** — an opt-in `gms` flavor may
add a templated media surface for a real head unit (AU12). That flavor is the
only place Google appears, it is not what most installs get, and the phone in
the mount remains the product.

> Big targets. Black ground. Nothing else.

## Identity and build

- Product: **xx-auto**. Brand: PiercingXX. Sits beside its siblings, so it gets
  a per-app mark, not the family logomark (BRAND-GUIDE §5).
- applicationId / namespace: `com.piercingxx.xxauto`.
- Lands at `Phone-Projects/android/xx-auto`; forge `PiercingXX/xx-auto` on
  GitHub, mirrored to the estate Gitea like every sibling.
- Gradle Kotlin DSL, version catalog copied from `xx-camera`. JDK 17,
  compileSdk 35, targetSdk 35, minSdk 26. Compose only.
- Media3 `session` at the **same version SKPP-Radio-App pins** — custom
  layout / media-button APIs moved between 1.4 and 1.6; a mismatch means the
  thumbs vanish.
- Permissions: **no `INTERNET`.** `BLUETOOTH_CONNECT` (auto-launch device
  pick, runtime), `READ_CONTACTS` + `CALL_PHONE` (Calls surface only, runtime,
  asked when the surface is switched on), `SYSTEM_ALERT_WINDOW` (auto-launch
  only, see below), `POST_NOTIFICATIONS` (auto-launch fallback), `WAKE_LOCK`
  is not needed — keep-screen-on is a window flag.
- Suite doors, same as every sibling: `ThemeSyncReceiver`,
  `LogDumpProvider` (`${applicationId}.logs`), vendored
  `SuiteBackupProvider` (`${applicationId}.suite.backup`, prefs only).

## Locked decisions

| ID | Decision |
|---|---|
| AU1 | **Phone in a mount is the default.** Still the primary form, still what the `noGms` build is. **Amended 2026-09-21:** projection is no longer closed. Android Auto works on GrapheneOS — the OS shipped a sandboxed Google Play compatibility extension for it in Dec 2023, with its own permission toggles for wired AA, wireless AA, audio routing and calls. xx-auto **may** ship a templated media surface in a `gms` product flavor (see AU12). The `noGms` build stays Google-free and is what most installs get. |
| AU2 | **Manual launch is the default.** Settings toggle `auto_launch` (off). When on: pick one bonded Bluetooth device; `ACL_CONNECTED` from it opens xx-auto, `ACL_DISCONNECTED` closes it **only if it was auto-opened**. A manual launch never self-closes. |
| AU3 | Every surface is a settings toggle: `surface_now_playing` (on), `surface_quick_pick` (on), `surface_nav` (on), `surface_calls` (off until contacts permission is granted, then on). A surface that is off is not drawn and holds no permission. |
| AU4 | **Generic media session.** xx-auto is a Media3 `MediaController` over the suite players. It sends `Player` commands and renders each session's **custom layout** as buttons. That is how SKPP Radio's three thumbs (`THUMB_UP` / `THUMB_REST` / `THUMB_DOWN`, `ListenCommands.kt`) show up: the radio already publishes them, xx-auto draws whatever a session offers. No radio-specific code. |
| AU5 | SKPP Radio's three thumb buttons are **mandatory on the now-playing card** whenever the radio session is the active one. A now-playing card that can play/pause but has no thumbs is a bug, not a v1 cut. |
| AU6 | Suite players are found by **component, not by scanning**: `com.skpp.radio/.playback.PlaybackService` and `com.piercingxx.audiobook/<its MediaSessionService>`. No `MediaSessionManager`, no notification-listener access. Third-party players are out of scope (see Later). |
| AU7 | **Navigation tile launches xx-maps** (`com.piercingxx.maps`). The handoff is `contracts/XX-MAPS.md`. Until it is installed the tile reads `xx-maps — not installed` and opens the xx-apps listing. Nothing else is ever launched from this tile. **Amended 2026-09-21:** xx-maps now exists as a brief (`xx-maps/contracts/CLEANROOM-BRIEF.md`) and the contract is two-way. The phone-side tile behaviour is unchanged — the resolution order, the extras and the back behaviour all stand exactly as written. |
| AU8 | **Calls = favourites + hand off.** Starred contacts as tiles, `ACTION_CALL` on tap. The in-call screen is whatever the default dialer is (xx-dialer on a suite phone). xx-auto is never an `InCallService`. |
| AU9 | Screen options, all toggles: `keep_screen_on` (on), `follow_rotation` (on; off = landscape lock), `always_ink` (on; off = follow the launcher's `THEME_CHANGED` like every sibling). |
| AU10 | No Skippy voice, no DND, no messages, no notifications mirror. "Stay out of the way" means those do not get built without a new lock. |
| AU11 | Local-only. No network permission, no telemetry, no server. Everything it knows lives in one DataStore file and gets backed up through the suite door. |
| AU12 | **On the car screen, xx-auto and xx-maps are siblings, not host and guest.** Android Auto cannot render one app's UI inside another's — every app registers its own `CarAppService` with its own category, and a navigation app must itself declare `androidx.car.app.category.NAVIGATION`. So **xx-maps owns the car screen for navigation; xx-auto owns it for media.** They appear as two apps in the AA launcher. xx-auto's projection surface, if built, is a templated **media** surface (now-playing + the session's custom buttons + the quick-pick list) — it never draws a map and never declares the navigation category. |
| AU13 | **External display (`DisplayManager` / `Presentation`) is first-come.** Whichever of the two the user opened owns the second screen; the other stays on the phone. No arbitration protocol, no service, no negotiation — that is a whole subsystem for a problem one person driving one car does not have. xx-auto's Maps tile, pressed while xx-auto owns the external display, releases it and launches xx-maps with the drive action; xx-maps then claims it. That is the entire handoff. |
| AU14 | **xx-auto does nothing about guidance audio. Do not add volume code.** xx-maps requests transient-may-duck audio focus when it speaks; the *players* (SKPP Radio, xx-audiobook) respond to that focus change, exactly as they do for any other app. xx-auto is a `MediaController`, not a player — it has no audio stream to duck and no business touching one. A future agent who "fixes" a volume dip during a turn instruction has broken this lock. AU10's "no Skippy voice **in xx-auto**" is unchanged and unaffected: xx-maps speaking is not xx-auto speaking. |

## Screens

There is one screen and one settings screen.

### Drive (`DriveScreen`)

Ink ground. Layout is a single column in portrait and two columns in
landscape (card left, tiles right). Minimum touch target **72dp**; text sizes
start at 24sp body / 40sp title. Space Mono for the title line, JetBrains Mono
for everything else, weight light.

1. **Now-playing card** (top / left): title, subtitle (artist / book chapter),
   elapsed / duration for seekable media. One transport row: `⏮ ▶︎/⏸ ⏭`. One
   custom row: the session's custom layout buttons, in the order the session
   gives them. For the radio that row is `👍 — 👎` (icons come from the
   `CommandButton` the session ships; fall back to its display name). Tapping a
   custom button calls `sendCustomCommand`. Long-press on the card opens the
   owning app.
2. **Tiles** (bottom / right), each a Signal-outlined block, active state
   inverted per BRAND-GUIDE §3.1:
   - `Radio` — tap: play the radio's last station (`controller.play()` when the
     session has an item, otherwise the first playable child of the library
     root). Long-press: quick-pick sheet listing the radio's top-level browse
     children (stations, speed dials) via `MediaBrowser`.
   - `Audiobook` — tap: resume (`play()` on the audiobook session; deep-link
     the app if it has no item). Long-press: open the app.
   - `Maps` — tap: xx-maps drive intent per `contracts/XX-MAPS.md`.
   - `Calls` — tap: favourites sheet (starred contacts, name + one number,
     big rows). Tap a row: `ACTION_CALL`.
3. A `⚙` glyph bottom-corner opens Settings. There is no other chrome: no app
   bar, no status text, no clock (the phone already has one).

**Which session is "active":** the one currently `isPlaying`; if none, the one
that most recently had a media item; if none, the card shows `Nothing playing`
and the Radio / Audiobook tiles are the whole story.

### Settings (`SettingsScreen`)

Plain list, one toggle per row, the suite's calm product register.

| Key | Default | Row |
|---|---|---|
| `auto_launch` | off | Open when the car connects — pick a Bluetooth device |
| `auto_launch_device` | — | Bonded-device picker, shown only when `auto_launch` is on |
| `surface_now_playing` | on | Now playing |
| `surface_quick_pick` | on | Radio and audiobook tiles |
| `surface_nav` | on | Maps tile |
| `surface_calls` | off | Calls tile — asks for Contacts + Phone when switched on |
| `keep_screen_on` | on | Keep the screen on |
| `follow_rotation` | on | Rotate with the phone (off = landscape) |
| `always_ink` | on | Always black (off = follow the suite theme) |

Plus one static block: version, and the local-only statement in the product
register: *"Free and ad-free. Collects no personal data. Nothing here leaves
your device."*

## Auto-launch mechanics (AU2)

Android 10+ blocks activity starts from the background. Two paths, chosen at
the moment the toggle is switched on:

1. **Overlay grant** — request `SYSTEM_ALERT_WINDOW` ("Display over other
   apps"). Holding it exempts the app from the background-start restriction on
   API 29–35. xx-auto never draws an overlay; the permission is only the
   exemption. The settings row says so in one line.
2. **Notification fallback** — if the grant is refused, the receiver posts a
   high-priority notification `Car connected — open xx-auto` with a
   full-screen-intent-free content tap. Auto-launch is then one tap, not zero.

The receiver is manifest-declared for `BluetoothDevice.ACTION_ACL_CONNECTED` /
`ACTION_ACL_DISCONNECTED`, filters on the saved device address, and is
`enabled=false` in the manifest until the toggle turns it on
(`PackageManager.setComponentEnabledSetting`). Off means nothing runs.

## Theme

- Tokens come from `piercingxx-branding/tokens/android-colors.xml`, vendored
  as `res/values/colors_brand.xml`. No retyped hexes.
- `always_ink` on: window ground is `ink`, and `ThemeSyncReceiver` still
  stores the launcher's theme but the drive screen ignores it.
- `always_ink` off: ground follows the stored suite theme, light presets
  included (Paper / Mist invert the ramp like xx-camera does).
- Accent is Signal white. No product accent. Warn / Error only on the
  `not installed` tile state and a failed call.

## Mark

Per-app mark in the house glyph language (108 viewport, mark inside 34..74,
Signal stroke 4, round caps, no fill). Draw a **steering wheel**: circle
`r=20` centred at 54,54; a horizontal spoke `34..74` through centre; a
vertical spoke `54,54 → 54,74`; hub dot `r=3` filled. Check against
xx-clock (circle r16 + two hands) and xx-note at 48dp before committing — if
they read alike, thicken the rim to 5 and drop the hub dot.

## Sibling work this design depends on

| Repo | Change | Why |
|---|---|---|
| `xx-audiobook/produced/xx-audiobook-app` | Add a `MediaSessionService` wrapping the existing ExoPlayer (`PhoneListen` / `TransportControls`), exported with the `androidx.media3.session.MediaSessionService` filter, `foregroundServiceType="mediaPlayback"`. Session metadata: book title, chapter, artwork URI, duration. | AU4 / AU6. Without it the Audiobook tile can only deep-link. |
| `xx-apps` | `CatalogSeed` row `com.piercingxx.xxauto` / `xx-auto` / `piercingxx/xx-auto`; `CatalogListings` entry with tagline + `icon_xx_auto`; default **on** (local, no server). | Store install + updates. |
| `xx-launcher` | `ThemeBroadcaster.FAMILY_PACKAGES` += `com.piercingxx.xxauto`. | Theme fan-out when `always_ink` is off. |
| `xx-apps` (suite theme client) | Same package added wherever `SuiteThemeClient` fans out. | Same. |

No change to SKPP Radio. Its session already carries the Auto library tree
and the three thumbs as custom commands.

## Later (not v1, needs a lock)

- Third-party players through `MediaSessionManager` (needs notification
  listener access — a permission the suite has avoided so far).
- Landscape-only "dash" layout with a speed readout (needs location).
- A launcher swipe bound to xx-auto (declined in kickoff; revisit).

**No longer on this list:** head-unit projection, moved to AU1/AU12 by the
2026-09-21 amendment. It is now allowed, scoped to a media surface in a `gms`
flavor, and sequenced in `todo.md` Phase 9 — behind the whole phone-in-mount
product, because that is what actually gets driven with.

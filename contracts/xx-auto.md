# xx-auto — build contract

Scopes the goal: build xx-auto, a driving screen for a phone in a car mount,
per `design.md` and `todo.md`. Full in-repo build, Phases 0–6 then Phase 9
last. Phase 7 (audiobook MediaSessionService — already done in the produced
sibling) and Phase 8 (xx-apps/xx-launcher registration — separate repos, held
pending operator) are **out of scope** here.

## State of the tree (measured this session)

This repo is **spec-only today**. Measured 2026-09-25: no `gradlew`, no
`app/` module, no source, no wrapper. The only files are the spec and the
house scaffolding:

- `design.md` — the spec (read in full this session).
- `todo.md` — the work list (read in full this session).
- `README.md` — product framing.
- `contracts/XX-MAPS.md` — the two-way xx-auto ⇄ xx-maps handoff (read in full).
- `scripts/check-docs.sh` — docs lane (links, trailers, secrets, gate honesty).
- `scripts/check-permissions.sh` — AU11/AU12 audit; exits 0 with "no build
  yet" until a merged manifest exists (lines 24–33).
- `scripts/check-nogms.sh` — AU1/AU12 noGms purity; exits 0 with "no build
  yet" until `gradlew` exists (lines 18–22).
- `.github/workflows/ci.yml` — phase-aware; build lane self-arms on
  `hashFiles('gradlew')` (lines 44–53).
- `.gitignore` — already covers `app/build/`, `*.keystore`-style build dirs.

**Every deliverable the goal names is a gap.** None of the build artifacts
exist: no Gradle scaffold, no `app/build.gradle.kts`, no manifest, no
`DriveScreen`, no suite doors, no pure-logic objects, no tests, no flavors.
The gate `./gradlew :app:testDebugUnitTest --offline` cannot run — there is no
wrapper. `git log` shows only the two spec commits (`37c7ebd`, `c35218f`).
This contract scopes the full build from an empty tree; nothing is already
done and nothing is skipped.

Deferred-verification items (the box cannot prove these) are collected in the
**Deferred verification** section before the Final gate.

---

## T1 — Phase 0: scaffold

Build the Gradle scaffold and the empty app shell so the build exists and the
audits arm. From `todo.md` Phase 0: `.gitignore` additions, Gradle from the
xx-camera template (`settings.gradle.kts`, root `build.gradle.kts`,
`gradle/libs.versions.toml`, wrapper; AGP 8.5.2, Kotlin 2.0.20, Compose BOM
2024.09.03), `media3 = "1.8.1"` + `media3-session` only (no exoplayer, no ui)
+ `datastore-preferences`, `app/build.gradle.kts` with
`com.piercingxx.xxauto`, minSdk 26, compile/target 35, versionCode 1 /
versionName 0.1.0, `unitTests.isReturnDefaultValues = true`, release signing
block from xx-clock (keystore.properties or `XXAUTO_*` env; unsigned
otherwise), manifest with **no INTERNET** and the design's permission set
(`BLUETOOTH_CONNECT`, `READ_CONTACTS`, `CALL_PHONE`, `SYSTEM_ALERT_WINDOW`,
`POST_NOTIFICATIONS`, `WAKE_LOCK` not needed), `MainActivity` exported +
LAUNCHER + `singleTask`, Ink theme no action bar. Brand tokens vendored from
`piercingxx-branding/tokens/android-colors.xml` → `res/values/colors_brand.xml`
with Compose `Ink.kt` (`ink`, `inkRaised`, `line`, `muted`, `text`, `signal`,
`warn`, `error`). Fonts Space Mono + JetBrains Mono under `res/font/` with
`Type.kt` (display = Space Mono light, body = JetBrains Mono light). Steering
wheel adaptive icon `mipmap-anydpi-v26` on Ink. Suite doors
`theme/ThemeSyncReceiver.kt` + `ThemeStore`, `log/LogDumpProvider.kt` at
`${applicationId}.logs`, vendored suite backup providers + `AutoBackupProvider`
exporting `prefs/` only. `strings.xml` app label `xx-auto`. Empty `DriveScreen`
rendering `Nothing playing` on Ink.

The empty `DriveScreen` is not dead code: it must render the design's
`Nothing playing` empty state (design "Which session is active": when no suite
session has a media item, the card shows `Nothing playing`). That empty state
is a pure seam, `drive/NothingPlaying.kt` (plain Kotlin, no Android imports),
which the DriveScreen consumes to decide and label the empty card. The verify
runs the `NothingPlayingTest` node, which calls `NothingPlaying.shouldShow` and
`NothingPlaying.LABEL` by name — the test cannot compile if the seam is absent,
so it cannot pass on dead code. The unit-test command is the Final gate and is
**not** claimed here.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.drive.NothingPlayingTest --offline
- files: .gitignore, settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, gradle/wrapper/gradle-wrapper.jar, gradlew, gradlew.bat, app/build.gradle.kts, app/src/main/AndroidManifest.xml, app/src/main/res/values/colors_brand.xml, app/src/main/res/values/strings.xml, app/src/main/res/font/spacemono.ttf, app/src/main/res/font/jetbrainsmono.ttf, app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml, app/src/main/res/drawable/ic_launcher_foreground.xml, app/src/main/res/drawable/ic_launcher_background.xml, app/src/main/java/com/piercingxx/xxauto/MainActivity.kt, app/src/main/java/com/piercingxx/xxauto/ui/theme/Ink.kt, app/src/main/java/com/piercingxx/xxauto/ui/theme/Type.kt, app/src/main/java/com/piercingxx/xxauto/ui/theme/ThemeSyncReceiver.kt, app/src/main/java/com/piercingxx/xxauto/ui/theme/ThemeStore.kt, app/src/main/java/com/piercingxx/xxauto/log/LogDumpProvider.kt, app/src/main/java/com/piercingxx/xxauto/backup/AutoBackupProvider.kt, app/src/main/java/com/piercingxx/suite/backup/TarStream.kt, app/src/main/java/com/piercingxx/suite/backup/Snapshot.kt, app/src/main/java/com/piercingxx/suite/backup/SuiteBackupProvider.kt, app/src/main/java/com/piercingxx/xxauto/drive/DriveScreen.kt, app/src/main/java/com/piercingxx/xxauto/drive/NothingPlaying.kt, app/src/test/java/com/piercingxx/xxauto/drive/NothingPlayingTest.kt

## T2 — Phase 1: settings + pure `SettingsMapper`

`settings/AutoPrefs.kt` — one DataStore file, keys and defaults exactly as
design "Settings" (`auto_launch`, `auto_launch_device`, `surface_now_playing`,
`surface_quick_pick`, `surface_nav`, `surface_calls`, `keep_screen_on`,
`follow_rotation`, `always_ink`). Pure `Settings` data class + `SettingsMapper`
(prefs ⇄ data class) with JUnit tests. `SettingsScreen`: toggle rows, version
block, local-only statement, reached from the `⚙` glyph, back returns to Drive.
`surface_calls` on → request `READ_CONTACTS` + `CALL_PHONE`; denied → toggle
snaps back off with one-line reason; off → nothing requested, nothing held.

The verify runs the `SettingsMapperTest` node, which calls `SettingsMapper` by
name — the test cannot compile if the symbol is absent.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.settings.SettingsMapperTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/settings/AutoPrefs.kt, app/src/main/java/com/piercingxx/xxauto/settings/Settings.kt, app/src/main/java/com/piercingxx/xxauto/settings/SettingsMapper.kt, app/src/main/java/com/piercingxx/xxauto/settings/SettingsScreen.kt, app/src/test/java/com/piercingxx/xxauto/settings/SettingsMapperTest.kt

## T3 — Phase 1: auto-launch trigger + pure `CarConnectDecision`

`auto_launch` on → `BLUETOOTH_CONNECT` runtime request → bonded-device picker
(name + address) → store `auto_launch_device`, then the overlay flow
(`ACTION_MANAGE_OVERLAY_PERMISSION`, one-line explain; refusal is fine — the
notification path). `trigger/CarConnectReceiver.kt`: manifest receiver for
`ACL_CONNECTED` / `ACL_DISCONNECTED`, `enabled="false"`; `AutoPrefs` write of
`auto_launch` flips it with `setComponentEnabledSetting`. Pure
`CarConnectDecision.decide(action, deviceAddress, savedAddress, overlayGranted,
wasAutoOpened) → Open | OpenViaNotification | Close | Ignore` with tests. Open
path `startActivity` with `NEW_TASK` + `auto_opened=true`; notification path
channel `car` high importance auto-cancel; close path finishes only when
auto-opened (`com.piercingxx.xxauto.action.CAR_GONE`). `POST_NOTIFICATIONS`
requested only on the notification path, first time.

The verify runs the `CarConnectDecisionTest` node, which calls
`CarConnectDecision.decide` by name — the test cannot compile if the symbol is
absent.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.trigger.CarConnectDecisionTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/trigger/CarConnectReceiver.kt, app/src/main/java/com/piercingxx/xxauto/trigger/CarConnectDecision.kt, app/src/main/AndroidManifest.xml, app/src/main/java/com/piercingxx/xxauto/settings/SettingsScreen.kt, app/src/main/java/com/piercingxx/xxauto/settings/AutoPrefs.kt, app/src/test/java/com/piercingxx/xxauto/trigger/CarConnectDecisionTest.kt

## T4 — Phase 2: media core — `SuitePlayers`, `SessionHub`, pure `SessionPick`

`media/SuitePlayers.kt`: the two `ComponentName`s from design AU6
(`com.skpp.radio/.playback.PlaybackService`,
`com.piercingxx.audiobook/<its MediaSessionService>`). Pure
`SessionPick.active(states) → which` implementing the active-session rule
(playing > most-recent-item > none) with tests. `media/SessionHub.kt`: builds a
`SessionToken` per component (skip if the package is absent —
`PackageManager.getServiceInfo` catch), one `MediaController` each via
`MediaController.Builder(...).buildAsync()`, releases on activity stop,
reconnects on `ACTION_PACKAGE_ADDED/REMOVED` for the two packages.

The verify runs the `SessionPickTest` node, which calls
`SessionPick.active` by name — the test cannot compile if the symbol is absent.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.media.SessionPickTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/media/SuitePlayers.kt, app/src/main/java/com/piercingxx/xxauto/media/SessionHub.kt, app/src/main/java/com/piercingxx/xxauto/media/SessionPick.kt, app/src/test/java/com/piercingxx/xxauto/media/SessionPickTest.kt

## T5 — Phase 2: media core — pure `NowPlayingState` mapper + transport + ticker

`media/NowPlayingState.kt`: title, subtitle, artworkUri, positionMs,
durationMs, isPlaying, isSeekable, hasPrev/hasNext, `customButtons:
List<CustomButton(commandAction, displayName, iconRes/iconUri, isEnabled)`. Pure
mapper from `MediaMetadata` + `Player` command availability +
`mediaButtonPreferences`/`customLayout` (prefer `mediaButtonPreferences`, fall
back to `customLayout` if empty) with tests. Transport
`play/pause/seekToNext/seekToPrevious/seekTo`; custom
`sendCustomCommand(SessionCommand(action, EMPTY), EMPTY)` surfacing a failed
`SessionResult` as a one-line Warn. Position ticker 1s while `isPlaying` and
the activity is resumed; nothing runs in the background.

The verify runs the `NowPlayingStateTest` node, which constructs media3
`MediaMetadata` / `CommandButton` inputs and calls the mapper by name — the
test cannot compile if the mapper is absent. media3 types are plain androidx
classes (not `android.*` framework), so JUnit covers it without Robolectric.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.media.NowPlayingStateTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/media/NowPlayingState.kt, app/src/main/java/com/piercingxx/xxauto/media/Transport.kt, app/src/main/java/com/piercingxx/xxauto/media/PositionTicker.kt, app/src/test/java/com/piercingxx/xxauto/media/NowPlayingStateTest.kt

## T6 — Phase 2: custom-button row — pure layout model test

Per `todo.md` Phase 2 last row: feed a `NowPlayingState` with the three radio
actions (`THUMB_UP` / `THUMB_REST` / `THUMB_DOWN`) and assert the card renders
exactly three custom buttons in session order. Chosen as a **pure layout-model
test**: a pure `CustomButtons.rows(state: NowPlayingState): List<CustomButtonUi>`
seam (no Android imports) that the DriveScreen card consumes, so the row order
is checkable without Robolectric or a Compose UI test.

The verify runs the `CustomButtonLayoutTest` node, which builds a
`NowPlayingState` with three custom buttons and asserts `CustomButtons.rows`
returns exactly three in order — the test cannot compile if the seam is absent.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.media.CustomButtonLayoutTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/media/CustomButtons.kt, app/src/main/java/com/piercingxx/xxauto/drive/DriveScreen.kt, app/src/test/java/com/piercingxx/xxauto/media/CustomButtonLayoutTest.kt

## T7 — Phase 3: drive screen + pure `DriveTiles.visible`

`DriveScreen` layout: portrait single column, landscape card-left / tiles-right;
targets ≥ 72dp; Space Mono title, JetBrains Mono rest, light weight. Now-playing
card (title, subtitle, time row only when seekable, transport row, custom-button
row via the `CustomButtons` seam). `Nothing playing` state when no session has
an item. Tiles Radio / Audiobook / Maps / Calls, each drawn only if its surface
toggle is on — gated through the pure `DriveTiles.visible(surfaces) → List<Tile>`
seam (surface_now_playing / surface_quick_pick / surface_nav / surface_calls)
with tests. Radio tap `play()` or browse root → first playable child →
`setMediaItem` + `play()`; radio long-press `QuickPickSheet` from
`MediaBrowser.getChildren(root)`. Audiobook tap `play()` if item else launch
app; long-press launch app. `⚙` glyph → Settings. Rotation
(`follow_rotation` off → `SENSOR_LANDSCAPE`, on → `UNSPECIFIED`), keep-screen-on
(`FLAG_KEEP_SCREEN_ON`), always_ink — all applied in `onResume` from prefs.

The verify runs the `DriveTilesTest` node, which calls `DriveTiles.visible` by
name — the test cannot compile if the seam is absent. The onResume window-flag
behaviors (rotation, keep-screen-on, always_ink) are Android-side and cannot be
unit-tested without Robolectric; they are deferred to manual QA (see Deferred
verification).

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.drive.DriveTilesTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/drive/DriveScreen.kt, app/src/main/java/com/piercingxx/xxauto/drive/DriveTiles.kt, app/src/main/java/com/piercingxx/xxauto/drive/QuickPickSheet.kt, app/src/main/java/com/piercingxx/xxauto/MainActivity.kt, app/src/test/java/com/piercingxx/xxauto/drive/DriveTilesTest.kt

## T8 — Phase 4: maps tile + pure `MapsLaunch.resolve`

`nav/MapsLaunch.kt`: pure `resolve(hasDriveAction, isInstalled) → Drive |
Launcher | NotInstalled` with tests; Android side per `contracts/XX-MAPS.md`
Part 1 resolution order (drive action → launcher → not-installed; never a
`geo:` chooser or third-party maps app). Not-installed state: tile text
`xx-maps — not installed` in `muted`, tap opens the xx-apps listing
(`com.piercingxx.apps` detail intent, else its launcher intent). Tile label
switches `Maps` → `xx-maps` only in the not-installed state.

The verify runs the `MapsLaunchTest` node, which calls `MapsLaunch.resolve` by
name — the test cannot compile if the symbol is absent.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.nav.MapsLaunchTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/nav/MapsLaunch.kt, app/src/main/java/com/piercingxx/xxauto/drive/DriveScreen.kt, app/src/test/java/com/piercingxx/xxauto/nav/MapsLaunchTest.kt

## T9 — Phase 5: calls tile + pure `FavouritePick.primaryNumber`

`calls/Favourites.kt`: query `ContactsContract` starred contacts, one primary
number each (prefer mobile). Pure `FavouritePick.primaryNumber(numbers)` with
tests. `FavouritesSheet`: name + number, rows ≥ 72dp, at most 8 (more scrolls);
tap → `ACTION_CALL` with `tel:`; denied `CALL_PHONE` → fall back to
`ACTION_DIAL` and say so once. Empty state: `No starred contacts` in `muted` —
no instructions, no link.

The verify runs the `FavouritePickTest` node, which calls
`FavouritePick.primaryNumber` by name — the test cannot compile if the symbol
is absent.

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.calls.FavouritePickTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/calls/Favourites.kt, app/src/main/java/com/piercingxx/xxauto/calls/FavouritePick.kt, app/src/main/java/com/piercingxx/xxauto/calls/FavouritesSheet.kt, app/src/main/java/com/piercingxx/xxauto/drive/DriveScreen.kt, app/src/test/java/com/piercingxx/xxauto/calls/FavouritePickTest.kt

## T10 — Phase 6: polish — `AppLog` ring buffer, backup, release

`AboutVersion`-style block in Settings (copy the sibling helper). Log: `AppLog`
ring buffer feeding `LogDumpProvider`, same shape as xx-camera. Backup:
`AutoBackupProvider.contents()` = `prefs/`; `afterRestore()` re-applies the
receiver enabled state from `auto_launch`. Release: `keystore.properties` or
`XXAUTO_*` env, `assembleRelease`, `apksigner verify --print-certs` matches
`PinnedSigners.SUITE`.

The verify runs the `AppLogTest` node, which exercises the `AppLog` ring buffer
by name (append, cap, drain) — the test cannot compile if the symbol is absent.
The release path (keystore, apksigner against `PinnedSigners.SUITE`) cannot run
offline without a signed APK and is deferred to release time (see Deferred
verification).

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.log.AppLogTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/log/AppLog.kt, app/src/main/java/com/piercingxx/xxauto/log/LogDumpProvider.kt, app/src/main/java/com/piercingxx/xxauto/backup/AutoBackupProvider.kt, app/src/main/java/com/piercingxx/xxauto/settings/SettingsScreen.kt, app/src/test/java/com/piercingxx/xxauto/log/AppLogTest.kt

## T11 — Phase 9: `noGms`/`gms` flavor split

`app/build.gradle.kts`: `flavorDimensions += "dist"`, products `noGms`
(default) and `gms`; `androidx.car.app` declared **only** in the `gms` source
set. A `noGms` build that pulls in Google is a failed build, not a warning.
`docs/MANUAL.md`: the GrapheneOS Android Auto setup path (install AA from the
GrapheneOS App Store; configure under Settings → Apps → Sandboxed Google Play →
Android Auto with separate wired/wireless/audio/calls toggles; enable AA
Developer Mode via Settings → Connected devices → Android Auto → tap Version,
allow unknown sources).

The verify is the repo's own noGms-purity audit, `scripts/check-nogms.sh`,
which asserts `androidx.car.app` and `com.google.*` never resolve into the
noGms classpath — it fails the build if the flavor split is wrong. The manual's
content is a doc deliverable; its links are covered by `scripts/check-docs.sh`
on every push and the prose is deferred to review (see Deferred verification).

- verify: ./scripts/check-nogms.sh
- files: app/build.gradle.kts, app/src/gms/java/com/piercingxx/xxauto/car/AutoCarAppService.kt, docs/MANUAL.md

## T12 — Phase 9: `AutoCarAppService` media-category-only

`gms/.../car/AutoCarAppService.kt : CarAppService` — **media category only.**
Declares `androidx.car.app.category.MEDIA` (or POI/templated as the library
allows). **Never** `category.NAVIGATION`, never `NAVIGATION_TEMPLATES`, never a
map (AU12 — navigation on the car screen is xx-maps'). Screens reuse the pure
`SessionPick.active` / `NowPlayingState` / `CustomButtons` seams from Phases 2
and 6 verbatim (those tests already cover the seams). A Maps entry fires the
AU7 drive intent so the tile behaves the same on the car screen as on the phone.

The verify greps the `AutoCarAppService` source for the MEDIA category
annotation — the named symbol must exist and declare the media category. The
absence of `NAVIGATION` in the gms source is a negative claim a single grep
cannot prove; it is enforced by `scripts/check-permissions.sh`'s AU12 audit in
CI and deferred to review (see Deferred verification).

- verify: grep -Fq androidx.car.app.category.MEDIA app/src/gms/java/com/piercingxx/xxauto/car/AutoCarAppService.kt
- files: app/src/gms/java/com/piercingxx/xxauto/car/AutoCarAppService.kt, app/src/gms/java/com/piercingxx/xxauto/car/CarScreens.kt, app/src/main/java/com/piercingxx/xxauto/media/SessionPick.kt, app/src/main/java/com/piercingxx/xxauto/media/NowPlayingState.kt, app/src/main/java/com/piercingxx/xxauto/media/CustomButtons.kt, app/src/main/AndroidManifest.xml

## T13 — Phase 9: external display + pure `DisplayClaim.decide`

External display (AU13): `DisplayManager` + `Presentation` rendering the drive
screen on a DP-alt/HDMI head unit. **Not `gms`-gated** — plain Android, works
on a bare GrapheneOS install. Pure `DisplayClaim.decide(...)` for the first-come
rule with tests. The Maps tile releases the display (`Presentation.dismiss()`)
before launching xx-maps.

The verify runs the `DisplayClaimTest` node, which calls
`DisplayClaim.decide` by name — the test cannot compile if the symbol is
absent. The `Presentation` wiring itself is Android-side and deferred to manual
QA (see Deferred verification).

- verify: ./gradlew :app:testDebugUnitTest --tests com.piercingxx.xxauto.display.DisplayClaimTest --offline
- files: app/src/main/java/com/piercingxx/xxauto/display/DisplayClaim.kt, app/src/main/java/com/piercingxx/xxauto/display/ExternalDisplay.kt, app/src/main/java/com/piercingxx/xxauto/drive/DriveScreen.kt, app/src/test/java/com/piercingxx/xxauto/display/DisplayClaimTest.kt

---

## Deferred verification

These deliverables are named by the goal but the box cannot prove them with a
bare command in this contract; they are deferred to the named mechanism and
must not be reported as verified by the automated gate:

- **Phase 3 onResume behaviors** (rotation lock, keep-screen-on, always_ink
  applied from prefs) — Android-side window/activity wiring, no Robolectric.
  Deferred to manual QA on the phone in the mount (`todo.md` Phase 6 QA row).
- **Phase 3/5/9 Android-side UI** (custom-button icons via `createPackageContext`
  / `iconUri`, long-press owning-app launch, `FavouritesSheet` rows, car-screen
  `ListTemplate`/`PaneTemplate` rendering) — deferred to manual QA.
- **Phase 6 release** (keystore, `assembleRelease`, `apksigner verify --print-certs`
  matching `PinnedSigners.SUITE`, `release_suite.sh`) — cannot run offline
  without a signed APK and keystore; deferred to release time.
- **Phase 6 backup `afterRestore()`** receiver-state re-application — deferred
  to manual QA.
- **Phase 9 `docs/MANUAL.md` prose** — doc deliverable; links checked by
  `scripts/check-docs.sh`, content deferred to review.
- **Phase 9 "never NAVIGATION" in the gms source** — negative claim a single
  grep cannot prove; enforced by `scripts/check-permissions.sh`'s AU12 audit
  in CI and deferred to review.
- **Phase 9 external-display `Presentation` wiring** — deferred to manual QA.

## Final gate

The whole workstream's unit tests — every pure seam across Phases 1–6 and 9 —
must pass. This is the operator exam's GATE command and the one command that
checks the tasks hold together. (The goal's full GATE additionally runs
`assembleDebug`; this named command is the test half that proves the pure
logic.)

- verify: ./gradlew testDebugUnitTest --offline
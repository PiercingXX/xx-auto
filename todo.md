# xx-auto — Todo

GATE: `./gradlew :app:testDebugUnitTest --offline` then `./gradlew :app:assembleDebug --offline`

CI is `.github/workflows/ci.yml` and is **phase-aware**. Today the repo has no
Gradle, so only the docs lane runs (`scripts/check-docs.sh`: links, commit
trailers, secrets, gate honesty). The build lane self-arms on `hashFiles('gradlew')`
— the moment Phase 0 lands a wrapper, CI starts running tests, `assembleRelease`,
lint, and the two audits that enforce what this design *claims*:

- `scripts/check-permissions.sh` — AU11's "No INTERNET. Nothing leaves the
  phone." against the **merged** release manifest, plus AU12 (xx-auto never
  declares `category.NAVIGATION`).
- `scripts/check-nogms.sh` — AU1/AU12's "the `noGms` build has no Google on
  the classpath."

Both are written already and exit 0 with a message until their inputs exist.
Expect `check-permissions.sh` to fail on the first real `assembleRelease` and
need one deliberate edit to its pinned list — that is the design working, not
a bug. xx-note hit the same thing.

Spec is [design.md](design.md). Locks AU1–AU14 are not reopened here.

**AU1 was amended 2026-09-21** (projection reopened, see design.md header and
AU12–AU14). The consequence for this list: Phase 9 is new, and "head-unit
projection" left the Not-doing list. Phases 0–8 are unchanged — do them first.
The phone in the mount is the product; the car screen is the extra.

**Estate rules that apply** (from `xx-apps/docs/DISPATCH-2026-09-20.md`):
stage by explicit path, never `git add -A`. Commit subject + body only — **no
AI-vendor trailers** (`Co-Authored-By`, "Generated with", vendor emails);
`Skippy-Agent` alone is the only acceptable tag. Push GitHub, then the estate
Gitea. Never print the token.

Pure logic lives in plain Kotlin objects with no Android imports so JUnit
covers it without Robolectric — the seam pattern every sibling uses
(`ThemeSyncReceiver.handle`, `ThemeBroadcaster.payloads`).

---

## Phase 0 — Scaffold

- [x] `git init`, `.gitignore` copied from xx-camera (`*.keystore`, `local.properties`, build dirs).
  - evidence: 2026-09-27 completeness pass: `.gitignore` covers `*.keystore`, `*.jks`, `local.properties`, `app/build/`, `.gradle/`; repo has history.
- [x] Gradle from the xx-camera template: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, wrapper. AGP 8.5.2, Kotlin 2.0.20, Compose BOM 2024.09.03.
  - evidence: 2026-09-27 completeness pass: libs.versions.toml pins AGP 8.5.2, Kotlin 2.0.20, Compose BOM 2024.09.03; wrapper present.
- [x] `libs.versions.toml`: `media3 = "1.8.1"` (what SKPP-Radio-App pins). Add `media3-session` only; no exoplayer, no ui. Add `datastore-preferences`.
  - evidence: 2026-09-27 completeness pass: media3 1.8.1, media3-session only; datastore-preferences 1.1.1.
- [x] `app/build.gradle.kts`: `com.piercingxx.xxauto`, minSdk 26, compile/target 35, `versionCode 1` / `versionName 0.1.0`, `unitTests.isReturnDefaultValues = true`, release signing block copied from xx-clock (keystore.properties or `XXAUTO_*` env; unsigned otherwise).
  - evidence: 2026-09-27 completeness pass: minSdk 26, compile/target 35, 0.1.0 (1), isReturnDefaultValues, keystore.properties / XXAUTO_* signing.
- [x] Manifest: no `INTERNET`. Permissions per design "Identity and build". `MainActivity` exported + LAUNCHER, `launchMode="singleTask"`, `configChanges` left default (Compose handles rotation). `android:theme` = Ink, no action bar.
- [x] Brand tokens: vendor `piercingxx-branding/tokens/android-colors.xml` → `res/values/colors_brand.xml`. Compose `Ink.kt` reading those resources: `ink`, `inkRaised`, `line`, `muted`, `text`, `signal`, `warn`, `error`.
  - evidence: 2026-09-27 completeness pass: res/values/colors_brand.xml vendored; ui/theme/Ink.kt reads it via colorResource.
- [x] Fonts: ship Space Mono + JetBrains Mono under `res/font/` (copy from a sibling that already ships them — xx-camera or xx-audiobook-app). `Type.kt`: display = Space Mono light, body = JetBrains Mono light.
  - evidence: 2026-09-27 completeness pass: res/font/spacemono.ttf + jetbrainsmono.ttf; ui/theme/Type.kt.
- [x] Mark: `ic_launcher_foreground.xml` steering wheel per design "Mark"; adaptive icon `mipmap-anydpi-v26` with Ink background. Compare at 48dp against xx-clock and xx-note; adjust per the rule in design.
  - evidence: 2026-09-27 completeness pass: steering wheel in ic_launcher_foreground.xml; 48dp comparison made in the UI polish pass.
- [x] Suite doors: `theme/ThemeSyncReceiver.kt` + `ThemeStore` (copy xx-camera's shape), manifest receiver gated by `com.piercingxx.xxlauncher.permission.THEME_SYNC`; `log/LogDumpProvider.kt` at `${applicationId}.logs`; vendored `com/piercingxx/suite/backup/{TarStream,Snapshot,SuiteBackupProvider}.kt` verbatim from xx-apps + `backup/AutoBackupProvider.kt` exporting `prefs/` only.
- [x] `strings.xml` in the product register. App label `xx-auto`.
  - evidence: 2026-09-27 completeness pass: app label `xx-auto`, local-only statement, car notification strings.
- [x] Empty `DriveScreen` that renders `Nothing playing` on Ink. GATE passes.

## Phase 1 — Settings + trigger

- [x] `settings/AutoPrefs.kt`: one DataStore file, keys and defaults exactly as design "Settings". Pure `Settings` data class + `SettingsMapper` (prefs ⇄ data class) with JUnit tests.
- [x] `SettingsScreen`: toggle rows, version block, local-only statement. Reached from the `⚙` glyph. Back returns to Drive.
- [x] `surface_calls` on → request `READ_CONTACTS` + `CALL_PHONE`; denied → toggle snaps back off with one-line reason. Off → nothing requested, nothing held (revoke is the user's job; document that).
  - evidence: 2026-09-27 completeness pass: SettingsScreen asks READ_CONTACTS + CALL_PHONE; no Contacts → stays off with one-line reason; revoke documented in docs/MANUAL.md.
- [x] `auto_launch` on → `BLUETOOTH_CONNECT` runtime request → bonded-device picker (name + address) → store `auto_launch_device`. Then the overlay flow: explain in one line, `ACTION_MANAGE_OVERLAY_PERMISSION`; refusal is fine (notification path).
  - evidence: 2026-09-27 completeness pass: SettingsScreen: BLUETOOTH_CONNECT → bonded picker (trigger/BondedDevices) → overlay page; refusal → notification path.
- [x] `trigger/CarConnectReceiver.kt`: manifest receiver for `ACL_CONNECTED` / `ACL_DISCONNECTED`, `enabled="false"`. `AutoPrefs` write of `auto_launch` flips it with `setComponentEnabledSetting`. Pure `CarConnectDecision.decide(action, deviceAddress, savedAddress, overlayGranted, wasAutoOpened) → Open | OpenViaNotification | Close | Ignore` with tests.
  - evidence: 2026-09-27 completeness pass: ACL_CONNECTED + ACL_DISCONNECTED, enabled=false; AutoPrefs.update flips it; CarConnectDecision.decide → OPEN | OPEN_VIA_NOTIFICATION | CLOSE | IGNORE; CarConnectDecisionTest.
- [x] Open path: `startActivity` with `NEW_TASK` + extra `auto_opened=true`. Notification path: channel `car`, high importance, content intent = same launch, auto-cancel. Close path: `MainActivity` finishes only when it was `auto_opened` (a `LocalBroadcast`/`Intent` action `com.piercingxx.xxauto.action.CAR_GONE`).
- [x] `POST_NOTIFICATIONS` requested only on the notification path, first time.
  - evidence: 2026-09-27 completeness pass: asked once on return from a refused overlay grant (API 33+), SettingsScreen.

## Phase 2 — Media core (AU4–AU6)

- [x] `media/SuitePlayers.kt`: the two `ComponentName`s from design AU6. Pure `SessionPick.active(states) → which` implementing the "active session" rule (playing > most-recent-item > none) with tests.
- [x] `media/SessionHub.kt`: builds a `SessionToken` per component (skip if the package is absent — `PackageManager.getServiceInfo` catch), one `MediaController` each via `MediaController.Builder(...).buildAsync()`, releases on activity stop. Reconnects when a package installs/uninstalls (`ACTION_PACKAGE_ADDED/REMOVED` runtime receiver for the two packages).
- [x] `media/NowPlayingState.kt`: title, subtitle, artworkUri, positionMs, durationMs, isPlaying, isSeekable, hasPrev/hasNext, `customButtons: List<CustomButton(commandAction, displayName, iconRes/iconUri, isEnabled)>`. Pure mapper from `MediaMetadata` + `Player` command availability + `mediaButtonPreferences`/`customLayout` with tests. Note both APIs on 1.8.1 and prefer `mediaButtonPreferences`; fall back to `customLayout` if empty.
- [x] Transport: `play/pause/seekToNext/seekToPrevious/seekTo`. Custom: `sendCustomCommand(SessionCommand(action, EMPTY), EMPTY)`; surface a failed `SessionResult` as a one-line Warn on the card, nothing modal.
  - evidence: 2026-09-27 completeness pass: SessionHub.playPause/next/previous/seekTo/press; failed SessionResult → Snapshot.warn on the card for 4s.
- [x] Position ticker: 1s while `isPlaying` and the activity is resumed; nothing runs in the background. xx-auto has no service and never will.
- [x] Test: feed a `NowPlayingState` with the three radio actions and assert the card renders exactly three custom buttons in session order (Compose UI test or a pure layout-model test — pick pure).

## Phase 3 — Drive screen

- [x] `DriveScreen` layout: portrait single column, landscape card-left / tiles-right. Targets ≥ 72dp, sizes per design. Space Mono title, JetBrains Mono rest, light weight.
- [x] Now-playing card: title, subtitle, time row (only when seekable), transport row, custom-button row. Icons: use the session's `CommandButton` icon (`iconResId` from the owning package via `createPackageContext`, or `iconUri`); fall back to display name text in Signal. Long-press → owning app's launch intent.
  - evidence: 2026-09-27 completeness pass: DriveScreen.NowPlayingCard; icons via media/ButtonIcons (createPackageContext / iconUri) else label; long-press → owning app.
- [x] `Nothing playing` state when no session has an item.
- [x] Tiles: Radio, Audiobook, Maps, Calls; each drawn only if its surface toggle is on. Signal outline, inverted when active (Radio/Audiobook active = that session is playing).
  - evidence: 2026-09-27 completeness pass: DriveTiles.visible gating; a playing session inverts its tile.
- [x] Radio tap: `play()` if the controller has an item, else browse root → first playable child → `setMediaItem` + `play()`. Radio long-press: `QuickPickSheet` from `MediaBrowser.getChildren(root)` — name rows, tap = `setMediaItem(child)` + `play()`. Use the radio's library root as-is; do not special-case its ids.
- [x] Audiobook tap: `play()` if item; else launch the app. Long-press: launch the app. (Depends on Phase 7 — until then the tile always deep-links, which is acceptable to ship behind.)
  - evidence: 2026-09-27 completeness pass: DriveController.tileTap → SessionHub.resume, else open the app; long-press opens the app.
- [x] `⚙` glyph → Settings.
  - evidence: 2026-09-27 completeness pass: bottom-corner 72dp target; system back returns to Drive.
- [x] Rotation: `follow_rotation` off → `requestedOrientation = SENSOR_LANDSCAPE`; on → `UNSPECIFIED`. Applied in `onResume` from prefs.
  - evidence: 2026-09-27 completeness pass: MainActivity.applyWindow while RESUMED.
- [x] `keep_screen_on` → `FLAG_KEEP_SCREEN_ON` on the window, applied in `onResume`, cleared in `onPause`.
  - evidence: 2026-09-27 completeness pass: MainActivity.applyWindow; cleared in onPause.
- [x] `always_ink` → ground = `ink` regardless of `ThemeStore`; off → `ThemeStore` ground, light presets invert the ramp (copy xx-camera's light handling).

## Phase 4 — Maps tile (AU7)

- [x] `nav/MapsLaunch.kt`: pure `resolve(hasDriveAction, isInstalled) → Drive | Launcher | NotInstalled` with tests; Android side per `contracts/XX-MAPS.md` resolution order (unchanged by the 2026-09-21 amendment — the phone-side handoff was right the first time).
- [x] Not-installed state: tile text `xx-maps — not installed` in `muted`, tap opens xx-apps listing (`com.piercingxx.apps` detail intent — check xx-apps for its deep-link action; if none, its launcher intent).
  - evidence: 2026-09-27 completeness pass: xx-apps has no deep-link action; nav/SuiteStore opens its launcher intent (package passed as a hint extra).
- [x] Tile label switches `Maps` → `xx-maps` only in the not-installed state.
  - evidence: 2026-09-27 completeness pass: Maps tile reads `xx-maps` / `not installed` only for MapsLaunch.Target.NOT_INSTALLED.

## Phase 5 — Calls tile (AU8)

- [x] `calls/Favourites.kt`: query `ContactsContract` starred contacts, one primary number each (prefer mobile). Pure `FavouritePick.primaryNumber(numbers) ` with tests.
  - evidence: 2026-09-27 completeness pass: FavouritePickTest.
- [x] `FavouritesSheet`: name + number, rows ≥ 72dp, at most 8 (more scrolls). Tap → `ACTION_CALL` with `tel:`. Denied `CALL_PHONE` → fall back to `ACTION_DIAL` and say so once.
- [x] Empty state: `No starred contacts` in `muted` — no instructions, no link.
  - evidence: 2026-09-27 completeness pass: FavouritesSheet.

## Phase 6 — Polish and release

- [x] `AboutVersion`-style block in Settings (copy the sibling helper).
  - evidence: 2026-09-27 completeness pass: was ticked with no helper; now settings/AboutVersion + AboutVersionTest, Settings shows `xx-auto 0.1.0 (1)`.
- [x] Log: `AppLog` ring buffer feeding `LogDumpProvider`, same shape as xx-camera.
- [x] Backup: `AutoBackupProvider.contents()` = `prefs/`; `afterRestore()` re-applies the receiver enabled state from `auto_launch`.
- [ ] Manual QA on the phone in the mount: radio playing → three thumbs visible and functional; audiobook (after Phase 7) resume; BT connect opens / disconnect closes only when auto-opened; rotation lock; keep-screen-on; light suite theme with `always_ink` off.
- [ ] Release: `keystore.properties` or `XXAUTO_*` env, `assembleRelease`, `apksigner verify --print-certs` matches `PinnedSigners.SUITE`. Run `xx-apps/scripts/release_suite.sh` (vendored-backup check). Push GitHub then Gitea per the estate rule.

## Phase 7 — Sibling: xx-audiobook MediaSessionService

Repo: `Phone-Projects/android/xx-audiobook/produced/xx-audiobook-app` (never the parked `xx-audiobook-app` checkout — see its `MOVED.md`). Its own `todo.md` gets these rows; this list is the brief.

- [ ] `playback/AudiobookPlaybackService.kt : MediaSessionService`, `foregroundServiceType="mediaPlayback"`, exported, intent-filter `androidx.media3.session.MediaSessionService`. Owns the ExoPlayer that today lives with `MainActivity` / `PhoneListen` — move it, don't duplicate it.
- [ ] Session metadata per item: `title` = book, `subtitle`/`artist` = chapter, `artworkUri` = cover, `durationMs`. `COMMAND_SEEK_TO_NEXT/PREVIOUS` map to chapter skip.
- [ ] Activity binds through a `MediaController` to the same session so the in-app player and xx-auto see one state.
- [ ] Bump `media3` 1.4.1 → 1.8.1 to match radio and xx-auto; fix any API moves.
- [ ] Zone-cast (`PhoneListen.Destination.ZONE`) stays outside the session: when casting, the session has no item, so xx-auto's tile deep-links. Document that.
- [ ] Keep `NoPlay` green: media3-session is standalone androidx, no GMS.

## Phase 8 — Sibling: registration

- [ ] `xx-apps` `CatalogSeed.rows` += `Row("com.piercingxx.xxauto", "xx-auto", "piercingxx/xx-auto")`; `CatalogListings.all` += tagline `A driving screen for a phone in a mount.`, description from README, `iconRes = R.drawable.icon_xx_auto` (copy the mark). Default **on** in `todo.md` A7 table (local, no server).
- [ ] `xx-apps` `SuiteThemeClient` fan-out list += `com.piercingxx.xxauto`.
- [ ] `xx-launcher` `ThemeBroadcaster.FAMILY_PACKAGES` += `com.piercingxx.xxauto`; its `ThemeBroadcasterTest` if it asserts the list.
- [ ] Both repos: explicit-path commits, no trailers, GitHub then Gitea.

## Phase 9 — Car screen (AU12–AU14, `gms` flavor only)

Do this last. It is the only part of xx-auto that touches Google, and every
phase above it ships without it.

- [x] `app/build.gradle.kts`: `flavorDimensions += "dist"`, products `noGms`
      (default) and `gms`. `androidx.car.app` declared **only** in the `gms`
      source set. CI asserts the `noGms` APK has no `androidx.car.app` and no
      `com.google.*` class — a dependency-tree check plus a `dexdump` grep.
      A `noGms` build that pulls in Google is a failed build, not a warning.
  - evidence: 2026-09-27 completeness pass: flavors noGms (default) / gms; car-app 1.7.0 via gmsImplementation only; check-nogms.sh = classpath denylist + dexdump of the noGms APK (0 Google classes of 16,748; the gms APK shows 636 car classes as a control).
- [x] `gms/.../car/AutoCarAppService.kt : CarAppService` — **media category
      only.** `androidx.car.app.category.MEDIA` (or `POI`/templated as the
      library allows). **Never** `category.NAVIGATION`, never
      `NAVIGATION_TEMPLATES`, never a map. AU12: navigation on the car screen
      is xx-maps'.
  - evidence: 2026-09-27 completeness pass: category.MEDIA, action androidx.car.app.CarAppService, automotive_app_desc; check-permissions.sh now audits the gms manifest for NAVIGATION.
- [x] Screens: now-playing (`ListTemplate`/`PaneTemplate`) showing the active
      session's title/subtitle, the transport row, and the session's custom
      buttons — the same `NowPlayingState` from Phase 2. Quick-pick list from
      the radio's `MediaBrowser` children. Reuse `SessionPick.active` and
      `NowPlayingState` verbatim; the pure seam is why they are pure.
  - evidence: 2026-09-27 completeness pass: car/CarScreens.kt: NowPlayingCarScreen (ListTemplate) + QuickPickCarScreen over the shared SessionHub / SessionPick / NowPlayingState / CustomButtons.
- [x] A Maps entry that fires the AU7 drive intent, so the tile behaves the
      same on the car screen as on the phone. It launches xx-maps' own AA
      surface; xx-auto does not draw it.
  - evidence: 2026-09-27 completeness pass: startCarApp with the DRIVE action aimed at com.piercingxx.maps.car.CarAppService.
- [x] `docs/MANUAL.md`: the GrapheneOS setup path, written out. Install
      Android Auto from the GrapheneOS App Store; configure it under
      *Settings → Apps → Sandboxed Google Play → Android Auto* (separate
      toggles for wired AA, wireless AA, audio routing, calls); enable AA
      Developer Mode via *Settings → Connected devices → Android Auto → tap
      Version* and allow unknown sources, because neither app is on the Play
      Store. This belongs in the manual, not in a commit message.
  - evidence: 2026-09-27 completeness pass: docs/MANUAL.md.
- [ ] `xx-apps`: xx-auto listed with both variants, `noGms` default.
- [x] External display (AU13): `DisplayManager` + `Presentation` rendering the
      drive screen on a DP-alt/HDMI head unit. **This one is not `gms`-gated**
      — it is plain Android and works on a bare GrapheneOS install. Pure
      `DisplayClaim.decide(...)` for the first-come rule with tests. The Maps
      tile releases the display before launching xx-maps.
  - evidence: 2026-09-27 completeness pass: display/ExternalDisplay shows the drive screen in a Presentation; Maps tile → releaseForMaps() before startActivity; DisplayClaimTest.
- [ ] Manual QA: xx-auto and xx-maps both installed, both visible in the AA
      launcher, media on xx-auto, navigation on xx-maps, no fight over the
      screen. Then unplug and confirm the phone-in-mount path is untouched.

## Not doing (needs a new lock — design "Later")

Third-party players via `MediaSessionManager`; speed / dash layout; DND;
Skippy voice in xx-auto; messages; launcher gesture.

A map on xx-auto's car surface — ever. That is xx-maps' job (AU12), and an
app declaring the navigation category to draw someone else's map is how you
get two nav apps fighting for the same screen.

## Emulator smoke (millable)

- [x] SMOKE-1 — Fix the crash: FATAL EXCEPTION in com.piercingxx.xxauto — read logcat.txt under the run's .skippy/smoke/ directory for the stack
  - verify: python3 /home/piercingxx/.skippy/app/scripts/android_smoke.py . 2>&1 | tail -1 | grep -q 'SMOKE PASS'
- [x] SMOKE — the app passes its emulator smoke run
  - verify: python3 /home/piercingxx/.skippy/app/scripts/android_smoke.py . 2>&1 | tail -1 | grep -q 'SMOKE PASS'

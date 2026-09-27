# xx-auto — Manual

A driving screen for a phone in a car mount. It shows what's playing, gives a
thumb big targets for play / skip / thumbs, opens maps, and dials a starred
contact. Then it stays out of the way.

Spec: [design.md](../design.md). The xx-maps handoff:
[contracts/XX-MAPS.md](../contracts/XX-MAPS.md).

## Two builds

| Build | Who it's for | What it has |
|---|---|---|
| `noGms` (default) | Most installs. | The phone-in-a-mount screen and the external display. No Google on the classpath, no `INTERNET`. |
| `gms` (opt-in) | Phones running Android Auto. | Everything in `noGms`, plus a templated **media** surface on the car's screen. |

xx-apps lists both variants and installs `noGms` unless you pick otherwise.
Both variants share the `com.piercingxx.xxauto` package id, so switching
variants means uninstalling one first.

Build them with `./gradlew :app:assembleNoGmsRelease` or
`:app:assembleGmsRelease` (JDK 17).

## The drive screen

- **Now playing** (top in portrait, left in landscape) shows the active suite
  player: title, subtitle, a time row for seekable media, `⏮ ▶︎/⏸ ⏭`, and
  the session's own buttons in the session's order. For SKPP Radio that row
  holds the three thumbs. Long-press the card to open the player app.
  - *Active* means the player that is playing. If none is, it's the one that
    most recently had something loaded. If neither player has anything
    loaded, the card reads `Nothing playing`.
- **Radio** tile: tap plays the radio's last station. If the radio has
  nothing loaded, the tile plays the first playable item in its library.
  Long-press opens the quick-pick sheet (the radio's library: tap a folder to
  open it, tap a station to play it, press back to step out).
- **Audiobook** tile: tap resumes the book. If the audiobook has nothing
  loaded (for example while it is casting to a zone), the tile opens the
  app instead. Long-press also opens the app.
- **Maps** tile: opens xx-maps in drive mode. If xx-maps isn't installed, the
  tile reads `xx-maps — not installed` and a tap opens xx-apps.
- **Calls** tile: your starred contacts, one number each (mobile preferred).
  Tapping a contact places the call. The in-call screen is your normal
  dialer. xx-auto never handles calls itself.
- `⚙` in the bottom corner opens Settings. Back returns to the drive screen.

A tile that's switched off in Settings isn't drawn.

Only two players are supported: SKPP Radio (`com.skpp.radio`) and xx-audiobook
(`com.piercingxx.audiobook`). Third-party players are out of scope (design
"Later").

## Settings

| Row | Default | Notes |
|---|---|---|
| Open when the car connects | off | See *Auto-launch* below. |
| Now playing | on | |
| Radio and audiobook tiles | on | |
| Maps tile | on | |
| Calls tile | off | Asks for **Contacts** and **Phone** when you switch it on. Without Contacts it stays off. Without Phone, a tap opens the dialer with the number filled in, and you press call yourself. |
| Keep the screen on | on | Only while the drive screen is in front. |
| Rotate with the phone | on | Off locks the screen to landscape. |
| Always black | on | Off follows the suite theme from xx-launcher, light themes included. |

Switching a surface off does **not** revoke a permission it was granted.
Android only lets you do that yourself: *Settings → Apps → xx-auto →
Permissions*. Once a surface is off, xx-auto doesn't use the permission.

## Auto-launch

Off by default. When it's on, xx-auto opens when one chosen Bluetooth device
(your car) connects. When that device disconnects, xx-auto closes, but only
if the connection is what opened it. If you opened xx-auto yourself, it stays
open.

Setup (Settings → *Open when the car connects*):

1. Allow **Nearby devices** (Bluetooth) when asked.
2. Pick your car from the list of paired devices. If it isn't there, pair the
   car in Android's Bluetooth settings first.
3. Android opens **Display over other apps** for xx-auto. Allowing it is what
   lets the drive screen open on its own. xx-auto never draws over other apps;
   the permission only lifts Android's block on opening a screen from the
   background.
4. If you'd rather not allow that, go back. xx-auto then posts a
   *Car connected — open xx-auto* notification when the car connects, and one
   tap opens it. It asks for notification permission the first time.

With the toggle off, xx-auto doesn't listen for Bluetooth at all.

## External display (USB-C DP-alt / HDMI)

This is plain Android and works on every install, including `noGms`. While
the drive screen is open, xx-auto shows it on an attached external display.

The first app to claim the display keeps it: whichever of xx-auto and xx-maps
you opened first owns it, and the other stays on the phone. Pressing the
**Maps** tile while xx-auto owns the display releases it first, so xx-maps
can take it over. xx-auto won't claim the display again until you open it
fresh (launcher icon or auto-launch).

## Android Auto on GrapheneOS (`gms` build only)

Android Auto works on GrapheneOS through the sandboxed Google Play
compatibility layer. On the car's screen, xx-auto and xx-maps appear as two
separate apps:

- xx-auto shows media: now playing, play/pause and skip, the session's
  buttons (the radio's thumbs), and the radio's stations.
- xx-maps shows navigation.
- xx-auto's **Maps** action switches the car screen to xx-maps' own surface.
  xx-auto never draws a map.

Setup:

1. Install **sandboxed Google Play** from the GrapheneOS App Store, if it
   isn't installed already.
2. Install **Android Auto** from the GrapheneOS App Store.
3. Configure it under *Settings → Apps → Sandboxed Google Play → Android
   Auto*. There are separate toggles for **wired Android Auto**, **wireless
   Android Auto**, **audio routing** and **calls**. Turn on the ones your car
   uses. Media needs audio routing.
4. Enable Android Auto's developer mode: *Settings → Connected devices →
   Android Auto* (or open the Android Auto app), scroll to **Version**, and
   tap it about ten times until it confirms.
5. From the Android Auto developer settings (three-dot menu → *Developer
   settings*), turn on **Unknown sources**. Neither xx-auto nor xx-maps comes
   from the Play Store, and Android Auto hides sideloaded apps without this.
6. Install the `gms` build of xx-auto (and of xx-maps, if you want navigation
   on the car screen), then connect to the car.
7. In the car's app launcher, open **xx-auto**. If it's missing, check step 5,
   then use *Customize launcher* in Android Auto's settings.

A note on the media surface: xx-auto registers its car service in the Car App
Library's **media** category. Whether a given Android Auto host version shows
templated media apps can change on Google's side. If xx-auto doesn't appear
after the steps above, the radio and the audiobook still show up in Android
Auto as ordinary media apps (both publish their own media library). That's
the same music with Google's player chrome instead of xx-auto's.

Guidance audio: when xx-maps speaks, it ducks the players. xx-auto has no
audio of its own and doesn't touch volume (AU14).

## Privacy

Free and ad-free. Collects no personal data. Nothing here leaves your device.

xx-auto has no `INTERNET` permission, and CI audits the merged manifest to
keep it that way (`scripts/check-permissions.sh`). All of its settings live
in one local file, and the suite backup (xx-apps) saves them along with the
launcher theme it last received.

## Troubleshooting

- **Card says `Nothing playing` while music plays.** Only SKPP Radio and
  xx-audiobook are supported. If the audiobook is playing, update xx-audiobook
  to the build with the media session (its `playback/PlaybackService`).
- **Thumbs missing on the radio card.** SKPP Radio and xx-auto must both be on
  media3 1.8.x. The thumbs come from the radio's session, so an older radio
  build won't send them.
- **Auto-launch never fires.** Check that the picked car is the device that
  connects (Settings shows its name), that Nearby devices is still allowed,
  and that the notification path is permitted if the overlay grant was
  refused.
- **Logs.** xx-apps collects xx-auto's log ring buffer through the suite log
  door (`com.piercingxx.xxauto.logs`) when you send a report from there.

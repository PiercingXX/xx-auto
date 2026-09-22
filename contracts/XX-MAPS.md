# xx-auto ⇄ xx-maps — two-way contract

**v1** written 2026-09-21 by xx-auto, before xx-maps existed — a one-way
handoff describing what xx-auto sends and what xx-maps must receive.

**v2** amended 2026-09-21 (same day, later) by xx-maps, after its kickoff
Q&A reopened AU1. This is now a **two-way contract**: both repos read it,
either may propose a change, and a change lands **here first** before either
side follows it.

> **The v1 phone-side handoff below is unchanged.** It was right the first
> time. Everything new is additive and lives under *Car screen* onward. An
> xx-auto agent implementing only v1 is still correct and still ships.

Companion document: `xx-maps/contracts/CLEANROOM-BRIEF.md`, which is the
xx-maps build brief and cites this contract as binding.

## Package

`com.piercingxx.maps` (reserved in `xx-apps/todo.md` A7; brief written, repo
not yet forged).

---

# Part 1 — The phone handoff (v1, unchanged)

## What xx-auto sends

The Maps tile fires one explicit intent:

```kotlin
Intent("com.piercingxx.maps.action.DRIVE")
    .setPackage("com.piercingxx.maps")
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    .putExtra("com.piercingxx.maps.extra.FROM", "com.piercingxx.xxauto")
```

Optional extras xx-auto may add later, all ignorable:

| Extra | Type | Meaning |
|---|---|---|
| `com.piercingxx.maps.extra.DEST` | String, `geo:lat,lng?q=…` | A destination to route to. Absent means "just open in drive mode". |
| `com.piercingxx.maps.extra.INK` | Boolean | `true` when xx-auto has `always_ink` on. A hint that the user wants pure black; xx-maps may use it for its night style. |

Resolution order in xx-auto:

1. `ACTION_DRIVE` above, if `resolveActivity` finds it.
2. The package's `LAUNCHER` intent (`getLaunchIntentForPackage`), if the
   package is installed but predates the action.
3. Not installed: the tile reads `xx-maps — not installed` and tapping opens
   the xx-apps listing for `com.piercingxx.maps`.

xx-auto never falls through to a `geo:` chooser or a third-party maps app.

## What xx-maps must do

- Declare an exported activity with an intent-filter for
  `com.piercingxx.maps.action.DRIVE` + `category.DEFAULT`.
- Opening on that action means **drive mode**: map fills the screen, large
  targets, no onboarding, no dialogs. If a route is in progress, resume it.
- Back from that activity returns to xx-auto (it was started as a new task;
  do not `finishAffinity` the caller, do not relaunch the launcher).
- Same signing key, same theme door (`xx.launcher.THEME_CHANGED`), same suite
  providers as every sibling. Nothing xx-auto-specific beyond the action.

## What xx-auto is *not* asking for

- No callbacks. xx-maps does not report anything back.
- No shared process, no bound service, no content provider between the two.
- No dependency the other way: xx-maps must run fine with xx-auto absent.

---

# Part 2 — The car screen (v2, new)

## The correction that forced this amendment

The operator's model was "xx-auto is Android Auto, hosting xx-maps to the
car's screen." **Android Auto does not work that way, and no amount of design
makes it.** One app cannot render another app's UI on the car screen. Every
app registers its own `CarAppService` with its own category, the host draws
from templates that app supplies, and a navigation app must itself declare
`androidx.car.app.category.NAVIGATION`.

So the two are **siblings on the car screen, not host and guest.** They appear
as two separate entries in the Android Auto launcher.

| Surface | Owner | Category | Notes |
|---|---|---|---|
| Car screen — **navigation** | **xx-maps** | `category.NAVIGATION` + `NAVIGATION_TEMPLATES` | The map, the route, the guidance. xx-auto never draws this. |
| Car screen — **media** | **xx-auto** (`gms` flavor, optional) | media / templated. **Never** `NAVIGATION` | Now-playing, transport, the session's custom buttons, quick-pick. Never a map. |
| **External display** (DP-alt / HDMI) | **first-come** | n/a — plain `Presentation` | No Google involved. See *External display* below. |
| **Phone in a mount** | both, independently | n/a | The default, the primary form, and what Part 1 describes. |

Locks: xx-auto **AU12** (siblings, not host/guest), **AU13** (external display
first-come), **AU14** (xx-auto does nothing about guidance audio).

## Build flavors — both repos, same shape

Both apps split `noGms` (default) / `gms`. `androidx.car.app` appears **only**
in the `gms` source set of each. Both repos assert in CI that the `noGms` APK
carries no `androidx.car.app` and no `com.google.*` class. xx-apps lists both
variants for both apps and defaults to `noGms`.

A `noGms` phone — which is the normal case — gets the full product from both
apps with zero Google on the device.

## Crossing over on the car screen

xx-auto's car-screen media surface carries a **Maps entry** that fires the
same Part 1 drive intent. Pressing it hands off to xx-maps' own Android Auto
surface. xx-auto does not draw, wrap, embed or proxy anything xx-maps renders.

Still no callbacks. Still no bound service. The car screen changes *where*
each app draws, not *how* they talk.

## External display (AU13)

Plain `DisplayManager` + `Presentation` on a USB-C DP-alt or HDMI head unit.
No Google, works on a bare GrapheneOS install, and it is the surface that
keeps working when Google changes its mind about sideloaded car apps.

**First-come ownership.** Whichever app the user opened owns the second
screen; the other stays on the phone. There is no arbitration protocol, no
coordinator service, no negotiation — that is a whole subsystem for a problem
one person driving one car does not have.

The one handoff: xx-auto's Maps tile, pressed while xx-auto owns the external
display, **releases the display first**, then fires the drive intent. xx-maps
claims it on the way up. That is the entire mechanism, and it is one
`Presentation.dismiss()` before one `startActivity`.

## Audio (AU14)

xx-maps requests transient-may-duck audio focus when it speaks. The
**players** — SKPP Radio, xx-audiobook — respond to that focus change, exactly
as they would for any other app.

**xx-auto does nothing.** It is a `MediaController`, not a player: it has no
audio stream to duck and no business touching one. Any volume-management code
added to xx-auto during a turn instruction is a bug against this contract, not
a fix.

xx-auto's AU10 ("no Skippy voice") is unchanged and unaffected. xx-maps
speaking is not xx-auto speaking.

## Network and the "nothing leaves the phone" claim

xx-auto has **no `INTERNET` permission** and that is unchanged. xx-maps has
`INTERNET`, a Skippy-Tel login, traffic providers and log shipping.

The two facts coexist because the apps are separate and the handoff is a
one-way intent. **Nothing xx-maps fetches ever travels back through xx-auto,
because there is no path for it to travel** — no callbacks, no provider, no
bound service. xx-auto's README claim is about xx-auto and remains literally
true.

An xx-auto agent must not add `INTERNET` to satisfy anything in this contract.
If a future change appears to require it, the change is wrong.

## Changing this contract

Either side may propose. The change lands **here first**, then both repos
follow. A change that touches Part 1 needs an operator lock, because xx-auto's
AU7 and xx-maps' drive-mode activity both depend on it verbatim.

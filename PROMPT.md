# Intermission — build prompt

*The original brief this was built from, kept as written.*

Build me a Wear OS app for my Pixel Watch that buzzes my wrist at the safe
bathroom-break windows during *Spider-Man: Brand New Day*. I'm seeing it
**tonight (Aug 2, 2026)**, so this has to be installed and working on the watch
within a few hours. Favor a small, working, sideloadable app over a polished one.

## The data

Runtime is **145 minutes** (2h25m). Break windows, in minutes **from the moment
the movie itself starts** (not from showtime):

| # | Window      | Note                                                  |
|---|-------------|-------------------------------------------------------|
| 1 | 20:00–23:00 | early quiet stretch                                   |
| 2 | 48:00–51:00 | dialogue transition between two big sequences          |
| 3 | 94:00–97:00 | last safe exit before the finale                       |

After minute 97 there is **no safe break** — the third act runs straight to the
end. Treat that as a hard rule: never schedule an alert past 97:00.

Put these in a single config object at the top of one file so I can edit the
numbers in ten seconds if the article turns out to be off, or if I want to reuse
this for a different movie.

## The critical design constraint (do not get this wrong)

The times above are relative to **first frame of the feature**, but I'll be in my
seat ~20 minutes earlier watching trailers, and previews never run exactly on
schedule. So:

- The app must have **one big "Movie starts NOW" button**. I tap it when the
  studio logo hits. All alerts are scheduled relative to that tap.
- Do **not** ask me for the showtime and do arithmetic on it. Do not use a
  fixed wall-clock schedule. The anchor is the tap.
- Once tapped, show a running clock of elapsed movie time and a countdown to the
  next window, so I can sanity-check drift at a glance.
- Let me nudge the anchor ±30s after the fact in case I tapped late.

## The other critical constraint: it must never light up

I'm in a dark theater. A glowing wrist is worse than a full bladder.

- **Haptics only.** No sound. No screen wake, no ambient/always-on brightening,
  no heads-up notification, no vibrant colors — if any UI does render, it should
  be near-black.
- If you need a foreground service notification to stay alive, make it minimum
  importance and silent.
- Verify the alerts still fire with the watch in **Theater Mode** and in **Do Not
  Disturb** — that's how I'll have it set. If Theater Mode suppresses vibration,
  say so up front and tell me what to set instead; don't let me find out at
  minute 20.

## Alert design

Three distinct haptic patterns so I can tell them apart without looking:

- **T-60s before a window opens:** one long buzz — "start wrapping up"
- **Window opens:** two short buzzes — "go now"
- **T-45s before the window closes:** three rapid buzzes — "get back in your seat"

Make the buzzes strong enough to feel through a jacket sleeve. Use alarm-class
vibration attributes so the OS doesn't quietly drop them.

## Reliability

This is the whole point of the app, so:

- Alerts must fire with the screen off and my wrist down for 90+ minutes. Handle
  Doze/app standby — use exact alarms that survive idle, or a foreground service
  with a wake lock, whichever you can actually verify works on Wear OS 5/6.
- Declare whatever exact-alarm permission the current API level requires, and
  tell me if I need to grant anything by hand in Settings.
- Nothing should depend on my phone being reachable — assume the phone is in my
  pocket on airplane mode or dead.
- Include a **test mode** that compresses the whole 145-minute schedule into ~2
  minutes so I can confirm all nine buzzes actually fire before I leave the house.
  I want to have tested this on the real watch, not in an emulator.

## Platform / delivery

Target: Pixel Watch, Wear OS. Native app, Kotlin + Compose for Wear OS, built in
Android Studio and sideloaded over ADB (Wi-Fi debugging).

Before you write any code, do a **feasibility check and tell me the answer**:
confirm whether I have Android Studio, the Wear OS SDK, and ADB pairing working
on this machine right now. If any of that is missing and would take more than
~30 minutes to set up, **stop and tell me** rather than burning the evening —
then propose the fastest fallback that still buzzes my wrist, e.g. a phone-side
page or script that fires silent notifications the watch relays as vibrations.
I'd rather have a working fallback at 6pm than a half-flashed native app at 7:30.

## Deliverable

- Working APK installed on the watch, plus the exact commands you used so I can
  reinstall it.
- A README with: how to arm it, what each buzz pattern means, and how to edit the
  timings for another movie.
- Tell me plainly what you tested on real hardware versus what you only reasoned
  about.

## Out of scope

No accounts, no cloud, no analytics, no companion phone UI, no Play Store
packaging, no theming. One screen, one button.

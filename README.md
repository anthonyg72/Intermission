# Intermission

Buzzes your wrist at the safe bathroom windows during a movie. Haptics only —
it never lights up, never makes a sound, and shows no notification.

Built for a Pixel Watch 4 (Wear OS, API 37).

## Using it tonight

1. Open **Intermission** on the watch. It shows `READY`.
2. **Tap `MOVIE STARTS NOW` when the first frame of the feature hits** — after the
   trailers, when the studio logo appears. Not at showtime.
3. That's it. Put your wrist down.

Everything is scheduled off that tap, so previews running long doesn't matter.

## The three screens

Once armed, the app is three full screens stacked vertically. Swipe up/down or
turn the crown; the dots on the right edge show where you are. One swipe moves
exactly one screen, so nothing can carry you through to `STOP` by accident.

| # | Screen | What's on it |
|---|---|---|
| 1 | Countdown | Elapsed time, countdown to the next window. No buttons. |
| 2 | `ADJUST START` | `-30s` / `+30s` and a live elapsed readout. |
| 3 | `END SESSION` | `STOP`, behind a confirmation. |

Before you arm it, screens 2 and 3 don't exist — there's nothing to adjust or
stop, and swiping does nothing.

**Nudging.** `-30s` and `+30s` move *the start instant*, not the clock. Tapped
late, so the movie really started before you hit the button? Press **`-30s`** —
the elapsed readout jumps forward 30s and all nine alerts re-shift with it.

**Stopping takes two deliberate taps, two screens in.** `STOP` swaps itself for
`YES, STOP` / `keep going`; only `YES, STOP` cancels the alerts. The confirmation
gives up after 8 seconds, and swiping away or leaving the app cancels it too — so
a stray tap in the dark can't end the session.

## What the buzzes mean

| Pattern | When | Meaning |
|---|---|---|
| One long buzz | 60s before a window opens | Start wrapping up |
| Two short buzzes | Window opens | Go now |
| Three rapid buzzes | 45s before the window closes | Head back to your seat |

## The schedule

*Spider-Man: Brand New Day*, 145 min. Windows in minutes from first frame:

| # | Window | Note |
|---|---|---|
| 1 | 20:00–23:00 | early quiet stretch |
| 2 | 48:00–51:00 | dialogue transition |
| 3 | 94:00–97:00 | last safe exit before the finale |

After 97:00 there are no alerts at all — the third act runs straight through.
The app displays `SIT TIGHT` once you're past it.

## Test run

The `TEST RUN` button on the READY screen compresses all 145 minutes by 60× and
fires all nine alerts in about 100 seconds. Use it to confirm haptics before you
leave the house.

## Reusing it for another movie

Edit [Config.java](app/src/main/java/com/intermission/Config.java) — `MOVIE_TITLE`,
`RUNTIME_MIN`, and the `WINDOWS` array. Nothing else needs to change.

## Building from a fresh clone

`local.properties` is gitignored because it hardcodes an SDK path. Recreate it:

```sh
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

`gradle.properties` also pins `org.gradle.java.home` to the JDK bundled with
Android Studio on macOS. On another machine, point it at any JDK 17+.

## Rebuilding and reinstalling

```sh
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
export ANDROID_SERIAL=192.168.0.29:40021    # zsh won't word-split "-s <serial>"

# if the watch dropped off Wi-Fi debugging, re-pair (ports change every time):
adb pair 192.168.0.29:<pair-port> <6-digit-code>
adb connect 192.168.0.29:<debug-port>

gradle :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.intermission/.MainActivity
```

## Design notes

**No foreground service, no notification.** Nine `AlarmManager.setAlarmClock()`
alarms instead. `setAlarmClock` is the only scheduling API fully exempt from Doze
and app-standby throttling — `setExactAndAllowWhileIdle` is rate-limited to
roughly one alarm per 9 minutes, which would silently drop the clustered alerts
within a window. Dropping the service also means no notification that could wake
the screen.

**`USAGE_ALARM` vibration attributes.** A notification-usage vibration gets
swallowed by Do Not Disturb. Alarm usage is what carries through.

**Zero dependencies.** Plain Java and framework Views — no Kotlin, no Compose, no
androidx. Nothing to resolve, fast builds. The pager is
[PagerScrollView.java](app/src/main/java/com/intermission/PagerScrollView.java),
~130 lines of snapping `ScrollView`, rather than a ViewPager2 dependency.

**Paging is vertical.** On Wear OS a horizontal right-swipe is the system
swipe-to-dismiss gesture, so a horizontal pager would fight it. Vertical is also
what the rotating crown drives.

**One gesture, one page.** A fling is clamped to ±1 page from wherever the finger
went down. Without that clamp a hard flick travels the drag *and* the fling —
measured on the watch, that jumped screen 1 straight to `STOP`.

**Night-vision palette.** Black ground, red text, near-black buttons.

## Verified on hardware

Real Pixel Watch 4, not an emulator:

- All nine alerts fired in a compressed test run, each within ~100 ms of target,
  with the screen off and the watch on the wrist.
- `USE_EXACT_ALARM` and `VIBRATE` are both granted at install — nothing to enable
  by hand in Settings.
- Arming schedules the correct first alarm (`+18m58s`, the 19:00 pre-warning).
- `STOP` cancels all pending alarms.

Re-verified after the three-screen change:

- One swipe moves exactly one screen, both directions, and the dots track it.
- `STOP` → `YES, STOP` cancels all nine alarms (`dumpsys alarm` shows nine
  `alarm_cancelled` records) and drops back to `READY`. `keep going` leaves all
  nine scheduled.
- Screens 2 and 3 are unreachable while disarmed.
- `-30s` moved elapsed 4:27 → 4:59 and `+30s` moved it back to 4:31.

## Do NOT use Theater Mode

**Theater Mode suppresses the alerts.** Tested on the watch — nothing buzzes.
`USAGE_ALARM` attributes carry through Do Not Disturb but not Theater Mode,
which cuts haptics outright.

You don't need it anyway: this app never wakes the screen on its own. The only
thing Theater Mode buys you in a cinema is stopping tilt-to-wake from lighting
the display when you raise your arm — and you can get that without losing the
alerts by turning off **Settings → Display → Tilt to wake** instead.

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

If you tapped late, hit **`-30s`** (means "the movie actually started 30s earlier
than I tapped"). **`+30s`** is the opposite. The whole schedule re-shifts.

**`STOP`** disarms and cancels every pending alert.

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

The `test run` button on the READY screen compresses all 145 minutes by 60× and
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
androidx. Nothing to resolve, fast builds.

**Night-vision palette.** Black ground, red text, near-black buttons.

## Verified on hardware

Real Pixel Watch 4, not an emulator:

- All nine alerts fired in a compressed test run, each within ~100 ms of target,
  with the screen off and the watch on the wrist.
- `USE_EXACT_ALARM` and `VIBRATE` are both granted at install — nothing to enable
  by hand in Settings.
- Arming schedules the correct first alarm (`+18m58s`, the 19:00 pre-warning).
- `STOP` cancels all pending alarms.

## Do NOT use Theater Mode

**Theater Mode suppresses the alerts.** Tested on the watch — nothing buzzes.
`USAGE_ALARM` attributes carry through Do Not Disturb but not Theater Mode,
which cuts haptics outright.

You don't need it anyway: this app never wakes the screen on its own. The only
thing Theater Mode buys you in a cinema is stopping tilt-to-wake from lighting
the display when you raise your arm — and you can get that without losing the
alerts by turning off **Settings → Display → Tilt to wake** instead.

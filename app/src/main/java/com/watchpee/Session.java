package com.watchpee;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.List;

/**
 * Owns the anchor (the instant the movie actually started) and the nine alarms
 * hung off it. Everything is relative to the anchor, so a late tap or a nudge
 * just re-scheduled the whole set.
 */
public final class Session {

    private static final String PREFS = "watchpee";
    private static final String KEY_ANCHOR = "anchor";
    private static final String KEY_SPEEDUP = "speedup";
    private static final int REQUEST_BASE = 8100;

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isArmed(Context c) {
        return prefs(c).getLong(KEY_ANCHOR, 0L) > 0L;
    }

    /** Wall-clock ms at which the movie started. */
    public static long anchor(Context c) {
        return prefs(c).getLong(KEY_ANCHOR, 0L);
    }

    public static int speedup(Context c) {
        return prefs(c).getInt(KEY_SPEEDUP, 1);
    }

    /** Elapsed movie-seconds, already un-compressed in test mode. */
    public static long elapsedSec(Context c) {
        if (!isArmed(c)) return 0;
        long realMs = System.currentTimeMillis() - anchor(c);
        return (realMs / 1000L) * speedup(c);
    }

    public static void arm(Context c, boolean testMode) {
        prefs(c).edit()
                .putLong(KEY_ANCHOR, System.currentTimeMillis())
                .putInt(KEY_SPEEDUP, testMode ? Config.TEST_SPEEDUP : 1)
                .apply();
        schedule(c);
    }

    /** Shift the anchor by deltaSec of movie time (negative = movie started earlier than I tapped). */
    public static void nudge(Context c, int deltaSec) {
        if (!isArmed(c)) return;
        long shiftMs = (long) deltaSec * 1000L / speedup(c);
        prefs(c).edit().putLong(KEY_ANCHOR, anchor(c) - shiftMs).apply();
        schedule(c);
    }

    public static void disarm(Context c) {
        cancelAll(c);
        prefs(c).edit().remove(KEY_ANCHOR).remove(KEY_SPEEDUP).apply();
    }

    /** Next event still in the future, or null once they have all fired. */
    public static Config.Event next(Context c) {
        long elapsed = elapsedSec(c);
        for (Config.Event e : Config.events()) {
            if (e.offsetSec > elapsed) return e;
        }
        return null;
    }

    private static void schedule(Context c) {
        cancelAll(c);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;

        long anchor = anchor(c);
        int speedup = speedup(c);
        long now = System.currentTimeMillis();

        List<Config.Event> events = Config.events();
        for (int i = 0; i < events.size(); i++) {
            Config.Event e = events.get(i);
            long fireAt = anchor + ((long) e.offsetSec * 1000L / speedup);
            if (fireAt <= now) continue; // already passed -- don't fire late

            // setAlarmClock is the only scheduling API fully exempt from Doze and
            // app-standby throttling. setExactAndAllowWhileIdle is rate-limited to
            // roughly one alarm per 9 minutes, which would drop our clustered alerts.
            am.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(fireAt, null),
                    pending(c, i, e));
        }
    }

    private static void cancelAll(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        List<Config.Event> events = Config.events();
        for (int i = 0; i < events.size(); i++) {
            am.cancel(pending(c, i, events.get(i)));
        }
    }

    private static PendingIntent pending(Context c, int index, Config.Event e) {
        Intent i = new Intent(c, AlertReceiver.class)
                .setAction("com.watchpee.ALERT." + index)
                .putExtra(AlertReceiver.EXTRA_KIND, e.kind.name());
        return PendingIntent.getBroadcast(
                c, REQUEST_BASE + index, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Session() {}
}

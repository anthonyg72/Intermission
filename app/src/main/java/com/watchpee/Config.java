package com.watchpee;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything movie-specific lives here. To reuse this app for a different film,
 * change MOVIE_TITLE, RUNTIME_MIN and WINDOWS -- nothing else.
 *
 * All offsets are seconds from the first frame of the feature, NOT from showtime.
 */
public final class Config {

    public static final String MOVIE_TITLE = "Spider-Man: Brand New Day";
    public static final int RUNTIME_MIN = 145;

    /** Safe bathroom windows as {openMinute, closeMinute}. Nothing may be scheduled after the last close. */
    private static final int[][] WINDOWS = {
            {20, 23},
            {48, 51},
            {94, 97},
    };

    /** How far before a window opens to give the "start wrapping up" buzz. */
    private static final int PRE_WARN_SEC = 60;
    /** How far before a window closes to give the "get back" buzz. */
    private static final int CLOSING_WARN_SEC = 45;

    /** Test mode compresses the whole schedule by this factor (145 min -> ~1.6 min). */
    public static final int TEST_SPEEDUP = 60;

    public enum Kind { PRE, OPEN, CLOSING }

    public static final class Event {
        public final int offsetSec;
        public final Kind kind;
        public final int windowIndex;

        Event(int offsetSec, Kind kind, int windowIndex) {
            this.offsetSec = offsetSec;
            this.kind = kind;
            this.windowIndex = windowIndex;
        }

        public String label() {
            // Phrased to read as the target of a countdown: "4:12 / break 2 heads-up".
            switch (kind) {
                case PRE:     return "break " + (windowIndex + 1) + " heads-up";
                case OPEN:    return "break " + (windowIndex + 1) + " opens";
                default:      return "last call -- head back";
            }
        }
    }

    /** The nine alerts, in chronological order. */
    public static List<Event> events() {
        List<Event> out = new ArrayList<>();
        for (int i = 0; i < WINDOWS.length; i++) {
            int openSec = WINDOWS[i][0] * 60;
            int closeSec = WINDOWS[i][1] * 60;
            out.add(new Event(openSec - PRE_WARN_SEC, Kind.PRE, i));
            out.add(new Event(openSec, Kind.OPEN, i));
            out.add(new Event(closeSec - CLOSING_WARN_SEC, Kind.CLOSING, i));
        }
        return out;
    }

    /** Last moment it is ever safe to be out of your seat. Hard stop -- the third act has no gaps. */
    public static int lastSafeSec() {
        return WINDOWS[WINDOWS.length - 1][1] * 60;
    }

    private Config() {}
}

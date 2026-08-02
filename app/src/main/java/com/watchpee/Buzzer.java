package com.watchpee;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * Haptics only -- never sound, never screen. Three patterns you can tell apart
 * without looking at the watch.
 */
public final class Buzzer {

    // {delay, on, off, on, ...} in ms, paired with amplitudes 0-255.
    private static final long[] PRE_T = {0, 1000};
    private static final int[]  PRE_A = {0, 255};

    private static final long[] OPEN_T = {0, 250, 180, 250};
    private static final int[]  OPEN_A = {0, 255, 0, 255};

    private static final long[] CLOSING_T = {0, 140, 110, 140, 110, 140};
    private static final int[]  CLOSING_A = {0, 255, 0, 255, 0, 255};

    public static void buzz(Context ctx, Config.Kind kind) {
        Vibrator v = vibrator(ctx);
        if (v == null || !v.hasVibrator()) return;

        long[] timings;
        int[] amplitudes;
        switch (kind) {
            case PRE:     timings = PRE_T;     amplitudes = PRE_A;     break;
            case OPEN:    timings = OPEN_T;    amplitudes = OPEN_A;    break;
            default:      timings = CLOSING_T; amplitudes = CLOSING_A; break;
        }

        VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);

        // USAGE_ALARM is what gets us through Do Not Disturb and Theater Mode.
        // A plain notification-usage vibration would be silently swallowed.
        VibrationAttributes attrs = new VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build();

        v.vibrate(effect, attrs);
    }

    private static Vibrator vibrator(Context ctx) {
        VibratorManager vm = ctx.getSystemService(VibratorManager.class);
        return vm != null ? vm.getDefaultVibrator() : ctx.getSystemService(Vibrator.class);
    }

    private Buzzer() {}
}

package com.intermission;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Fires on the wrist and nowhere else -- no notification, no screen, no sound. */
public class AlertReceiver extends BroadcastReceiver {

    public static final String EXTRA_KIND = "kind";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra(EXTRA_KIND);
        Config.Kind kind;
        try {
            kind = Config.Kind.valueOf(name);
        } catch (Exception e) {
            return;
        }
        Log.i("intermission", "alert " + kind);
        Buzzer.buzz(context, kind);
    }
}

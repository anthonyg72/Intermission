package com.intermission;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvTitle, tvMain, tvSub;
    private Button btnArm, btnMinus, btnPlus, btnSecondary;
    private LinearLayout rowNudge;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            render();
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvTitle);
        tvMain = findViewById(R.id.tvMain);
        tvSub = findViewById(R.id.tvSub);
        btnArm = findViewById(R.id.btnArm);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        btnSecondary = findViewById(R.id.btnSecondary);
        rowNudge = findViewById(R.id.rowNudge);

        btnArm.setOnClickListener(v -> {
            if (Session.isArmed(this)) {
                Session.disarm(this);
            } else {
                Session.arm(this, false);
            }
            render();
        });

        btnSecondary.setOnClickListener(v -> {
            if (!Session.isArmed(this)) {
                Session.arm(this, true);
                render();
            }
        });

        // Nudge the anchor: "-30s" means the movie really started 30s before I tapped.
        btnMinus.setOnClickListener(v -> { Session.nudge(this, -30); render(); });
        btnPlus.setOnClickListener(v -> { Session.nudge(this, 30); render(); });
    }

    @Override protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void render() {
        boolean armed = Session.isArmed(this);
        btnArm.setText(armed ? "STOP" : "MOVIE STARTS NOW");
        rowNudge.setVisibility(armed ? View.VISIBLE : View.GONE);

        if (!armed) {
            tvTitle.setText("Spider-Man");
            tvMain.setText("READY");
            tvSub.setText("Tap when the movie starts");
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText("test run");
            return;
        }

        btnSecondary.setVisibility(View.GONE);
        long elapsed = Session.elapsedSec(this);
        boolean test = Session.speedup(this) > 1;
        tvTitle.setText(test ? "TEST RUN" : "elapsed " + mmss(elapsed));

        Config.Event next = Session.next(this);
        if (next == null) {
            tvMain.setText(elapsed >= Config.lastSafeSec() ? "SIT TIGHT" : "--:--");
            tvSub.setText("No safe breaks left");
            return;
        }

        long until = next.offsetSec - elapsed;
        if (test) until = until / Session.speedup(this);
        tvMain.setText(mmss(until));
        tvSub.setText(next.label());
    }

    private static String mmss(long sec) {
        if (sec < 0) sec = 0;
        return String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
    }
}

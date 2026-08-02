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

    /** How long the "YES, STOP" confirmation stays live before it reverts. */
    private static final long CONFIRM_TIMEOUT_MS = 8000L;

    private static final int PAGE_COUNTDOWN = 0;
    private static final int PAGE_NUDGE = 1;
    private static final int PAGE_STOP = 2;

    private PagerScrollView pager;
    private View page2, page3, dots, dot1, dot2, dot3;
    private TextView tvTitle, tvMain, tvSub, tvElapsed, tvHintDown, tvStopTitle;
    private Button btnArm, btnMinus, btnPlus, btnSecondary, btnStop, btnStopConfirm, btnStopCancel;
    private LinearLayout groupStopIdle, groupStopConfirm;

    /** True once STOP has been tapped and we're waiting on the second tap. */
    private boolean confirmingStop;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            render();
            handler.postDelayed(this, 500);
        }
    };
    private final Runnable confirmTimeout = new Runnable() {
        @Override public void run() { setConfirming(false); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        page2 = findViewById(R.id.page2);
        page3 = findViewById(R.id.page3);
        dots = findViewById(R.id.dots);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        tvTitle = findViewById(R.id.tvTitle);
        tvMain = findViewById(R.id.tvMain);
        tvSub = findViewById(R.id.tvSub);
        tvElapsed = findViewById(R.id.tvElapsed);
        tvHintDown = findViewById(R.id.tvHintDown);
        tvStopTitle = findViewById(R.id.tvStopTitle);

        btnArm = findViewById(R.id.btnArm);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        btnSecondary = findViewById(R.id.btnSecondary);
        btnStop = findViewById(R.id.btnStop);
        btnStopConfirm = findViewById(R.id.btnStopConfirm);
        btnStopCancel = findViewById(R.id.btnStopCancel);
        groupStopIdle = findViewById(R.id.groupStopIdle);
        groupStopConfirm = findViewById(R.id.groupStopConfirm);

        btnArm.setOnClickListener(v -> {
            if (!Session.isArmed(this)) {
                Session.arm(this, false);
                render();
            }
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

        // Stopping takes two taps on a page that is two deliberate swipes away.
        btnStop.setOnClickListener(v -> setConfirming(true));
        btnStopCancel.setOnClickListener(v -> setConfirming(false));
        btnStopConfirm.setOnClickListener(v -> {
            setConfirming(false);
            Session.disarm(this);
            pager.goToPage(PAGE_COUNTDOWN, false);
            render();
        });

        // Backing off the stop page abandons a pending confirmation.
        pager.setOnPageChangeListener(page -> {
            if (page != PAGE_STOP && confirmingStop) setConfirming(false);
            updateDots(page);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        // Never come back to the app already sitting on STOP.
        setConfirming(false);
        pager.goToPage(PAGE_COUNTDOWN, false);
        handler.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(confirmTimeout);
    }

    private void setConfirming(boolean confirming) {
        confirmingStop = confirming;
        handler.removeCallbacks(confirmTimeout);
        if (confirming) handler.postDelayed(confirmTimeout, CONFIRM_TIMEOUT_MS);

        tvStopTitle.setText(confirming ? "STOP?" : "END SESSION");
        groupStopIdle.setVisibility(confirming ? View.GONE : View.VISIBLE);
        groupStopConfirm.setVisibility(confirming ? View.VISIBLE : View.GONE);
    }

    private void render() {
        boolean armed = Session.isArmed(this);
        setPagesVisible(armed);

        btnArm.setVisibility(armed ? View.GONE : View.VISIBLE);
        btnSecondary.setVisibility(armed ? View.GONE : View.VISIBLE);
        tvHintDown.setVisibility(armed ? View.VISIBLE : View.GONE);

        if (!armed) {
            tvTitle.setText("Spider-Man");
            tvMain.setText("READY");
            tvSub.setText("Tap when the movie starts");
            return;
        }

        long elapsed = Session.elapsedSec(this);
        boolean test = Session.speedup(this) > 1;
        tvTitle.setText(test ? "TEST RUN" : "elapsed " + mmss(elapsed));
        tvElapsed.setText(mmss(elapsed));

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

    /** Pages 2 and 3 exist only while armed -- when disarmed there is nothing to nudge or stop. */
    private void setPagesVisible(boolean visible) {
        int want = visible ? View.VISIBLE : View.GONE;
        if (page2.getVisibility() == want) return;

        page2.setVisibility(want);
        page3.setVisibility(want);
        dots.setVisibility(want);
        pager.refreshPages();
        updateDots(pager.currentPage());
    }

    private void updateDots(int page) {
        dot1.setAlpha(page == PAGE_COUNTDOWN ? 1f : 0.28f);
        dot2.setAlpha(page == PAGE_NUDGE ? 1f : 0.28f);
        dot3.setAlpha(page == PAGE_STOP ? 1f : 0.28f);
    }

    private static String mmss(long sec) {
        if (sec < 0) sec = 0;
        return String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
    }
}

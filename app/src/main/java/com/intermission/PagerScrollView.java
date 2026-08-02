package com.intermission;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * A ScrollView that snaps to whole screens. Its single child is a vertical
 * LinearLayout whose visible children each get sized to exactly one viewport --
 * so scroll position 1 == page 1, and nothing can ever come to rest half-way
 * between two pages.
 *
 * Paging is vertical on purpose: on Wear OS a horizontal right-swipe is the
 * system swipe-to-dismiss gesture, and vertical scrolling is what the rotating
 * crown drives.
 *
 * GONE pages contribute no height, so hiding a page removes it from the pager
 * with no extra bookkeeping.
 */
public class PagerScrollView extends ScrollView {

    public interface OnPageChangeListener {
        void onPageChanged(int page);
    }

    private int pageHeight;
    private int lastReportedPage = -1;
    private boolean flinging;
    /** Page the current gesture started from -- one gesture may never move more than one page. */
    private int settledPage;
    private OnPageChangeListener listener;

    private final Runnable snapRunnable = new Runnable() {
        @Override public void run() { goToPage(currentPage(), true); }
    };

    public PagerScrollView(Context c) { this(c, null); }
    public PagerScrollView(Context c, AttributeSet a) { this(c, a, 0); }
    public PagerScrollView(Context c, AttributeSet a, int def) {
        super(c, a, def);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public void setOnPageChangeListener(OnPageChangeListener l) { this.listener = l; }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        pageHeight = h;
        applyPageHeights();
    }

    /** Call after changing any page's visibility so heights and scroll range stay correct. */
    public void refreshPages() {
        applyPageHeights();
    }

    private void applyPageHeights() {
        if (pageHeight <= 0) return;
        final ViewGroup content = content();
        if (content == null) return;
        // Deferred: setLayoutParams during a layout pass would be dropped.
        post(new Runnable() {
            @Override public void run() {
                for (int i = 0; i < content.getChildCount(); i++) {
                    View page = content.getChildAt(i);
                    if (page.getVisibility() == GONE) continue;
                    ViewGroup.LayoutParams lp = page.getLayoutParams();
                    if (lp.height != pageHeight) {
                        lp.height = pageHeight;
                        page.setLayoutParams(lp);
                    }
                }
            }
        });
    }

    private ViewGroup content() {
        return getChildCount() > 0 ? (ViewGroup) getChildAt(0) : null;
    }

    public int pageCount() {
        ViewGroup content = content();
        if (content == null) return 0;
        int n = 0;
        for (int i = 0; i < content.getChildCount(); i++) {
            if (content.getChildAt(i).getVisibility() != GONE) n++;
        }
        return n;
    }

    public int currentPage() {
        if (pageHeight <= 0) return 0;
        return clamp((getScrollY() + pageHeight / 2) / pageHeight);
    }

    public void goToPage(int page, boolean animate) {
        if (pageHeight <= 0) return;
        int y = clamp(page) * pageHeight;
        if (animate) smoothScrollTo(0, y); else scrollTo(0, y);
    }

    private int clamp(int page) {
        int max = Math.max(0, pageCount() - 1);
        return Math.max(0, Math.min(page, max));
    }

    private void snapSoon(long delayMs) {
        removeCallbacks(snapRunnable);
        postDelayed(snapRunnable, delayMs);
    }

    @Override
    public void fling(int velocityY) {
        // Swallow the native fling entirely. One gesture moves exactly one page,
        // measured from where the finger went down -- so a hard flick can't carry
        // through the nudge screen onto STOP.
        flinging = true;
        goToPage(settledPage + (velocityY > 0 ? 1 : -1), true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            removeCallbacks(snapRunnable);
            settledPage = currentPage();
            flinging = false;
        }

        boolean handled = super.onTouchEvent(ev);

        if (!flinging && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)) {
            // A slow drag settles on the nearest page, still capped at one page of travel.
            int target = Math.max(settledPage - 1, Math.min(currentPage(), settledPage + 1));
            goToPage(target, true);
        }
        return handled;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        boolean handled = super.onGenericMotionEvent(ev);
        // Crown rotation arrives as a stream of scroll events; snap once it stops.
        snapSoon(140);
        return handled;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldL, int oldT) {
        super.onScrollChanged(l, t, oldL, oldT);
        int page = currentPage();
        if (page != lastReportedPage) {
            lastReportedPage = page;
            if (listener != null) listener.onPageChanged(page);
        }
    }
}

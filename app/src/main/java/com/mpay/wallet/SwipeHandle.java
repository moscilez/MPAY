package com.mpay.wallet;

import android.content.Context;
import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

/** Exposes the same click action to touch gestures, keyboard and accessibility services. */
// This app uses framework Activity + Theme.Material, not AppCompat's view inflation/theme.
@SuppressLint("AppCompatCustomView")
final class SwipeHandle extends TextView {
    private float startX, startY;
    private final int slop;
    SwipeHandle(Context context) { super(context); slop = ViewConfiguration.get(context).getScaledTouchSlop(); }
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX(); startY = event.getY(); setPressed(true); return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                setPressed(false);
                float dx = Math.abs(event.getX() - startX), dy = startY - event.getY();
                if ((dx <= slop && Math.abs(dy) <= slop) || (dy > slop && dy > dx)) performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false); return true;
            default: return super.onTouchEvent(event);
        }
    }
    @Override public boolean performClick() { return super.performClick(); }
}

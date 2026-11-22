package com.david.ex2lite;

import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

/** Respuesta táctil: la pieza se hunde al tocarla, rebota al soltar y vibra si el equipo puede. */
public final class TouchFx implements View.OnTouchListener {
    private static final TouchFx INSTANCE = new TouchFx();
    private TouchFx() {}

    /** Aplica el efecto a todo lo que se pueda tocar dentro de v. */
    public static void apply(View v) {
        if (v.hasOnClickListeners() || v instanceof android.widget.CompoundButton) {
            v.setOnTouchListener(INSTANCE);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) apply(g.getChildAt(i));
        }
    }

    @Override public boolean onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                v.animate().cancel();
                v.animate().scaleX(0.955f).scaleY(0.955f).setDuration(70).start();
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.animate().cancel();
                v.animate().scaleX(1f).scaleY(1f).setDuration(220)
                        .setInterpolator(new OvershootInterpolator(2.2f)).start();
                break;
            default:
                break;
        }
        return false;
    }
}

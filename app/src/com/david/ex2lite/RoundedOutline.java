package com.david.ex2lite;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

/** Recorta una vista con esquinas redondeadas (para que la imagen de fondo no sobresalga). */
public final class RoundedOutline extends ViewOutlineProvider {
    private final float radius;

    public RoundedOutline(float radius) {
        this.radius = radius;
    }

    @Override public void getOutline(View v, Outline o) {
        o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
    }
}

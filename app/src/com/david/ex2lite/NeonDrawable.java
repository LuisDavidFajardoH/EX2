package com.david.ex2lite;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Forma estilo Tron: esquinas cortadas (arriba-izquierda y abajo-derecha), relleno con degradado
 * y borde de neón con resplandor (varios trazos superpuestos, sin desenfoque).
 */
public final class NeonDrawable extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int top, bottom, stroke;
    private final float sw, cut;
    private final boolean glow;

    public NeonDrawable(int fillTop, int fillBottom, int strokeColor, float strokePx, float cutPx, boolean glow) {
        this.top = fillTop;
        this.bottom = fillBottom;
        this.stroke = strokeColor;
        this.sw = strokePx;
        this.cut = cutPx;
        this.glow = glow;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeJoin(Paint.Join.MITER);
        line.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override protected void onBoundsChange(Rect b) {
        float inset = (glow && sw > 0) ? sw * 2.2f : sw / 2f;
        float l = b.left + inset, t = b.top + inset, r = b.right - inset, bt = b.bottom - inset;
        float c = Math.min(cut, Math.min(r - l, bt - t) * 0.4f);
        path.reset();
        path.moveTo(l + c, t);
        path.lineTo(r, t);
        path.lineTo(r, bt - c);
        path.lineTo(r - c, bt);
        path.lineTo(l, bt);
        path.lineTo(l, t + c);
        path.close();
        fill.setShader(top == bottom ? null
                : new LinearGradient(b.left, b.top, b.right, b.bottom, top, bottom, Shader.TileMode.CLAMP));
        fill.setColor(top);
    }

    @Override public void draw(Canvas cv) {
        if ((top >>> 24) != 0 || (bottom >>> 24) != 0) cv.drawPath(path, fill);
        if (sw <= 0 || (stroke >>> 24) == 0) return;
        int rgb = stroke & 0x00FFFFFF, a = stroke >>> 24;
        if (glow) {
            line.setColor(rgb | (Math.round(a * 0.10f) << 24));
            line.setStrokeWidth(sw * 4.4f);
            cv.drawPath(path, line);
            line.setColor(rgb | (Math.round(a * 0.22f) << 24));
            line.setStrokeWidth(sw * 2.6f);
            cv.drawPath(path, line);
        }
        line.setColor(stroke);
        line.setStrokeWidth(sw);
        cv.drawPath(path, line);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter cf) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

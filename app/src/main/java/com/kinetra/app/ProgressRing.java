package com.kinetra.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class ProgressRing extends View {
    final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), arc = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    float shown, d;
    ValueAnimator va;

    public ProgressRing(Context c, AttributeSet at) {
        super(c, at);
        d = getResources().getDisplayMetrics().density;
        track.setStyle(Paint.Style.STROKE); track.setStrokeWidth(8 * d); track.setColor(c.getColor(R.color.line));
        arc.setStyle(Paint.Style.STROKE); arc.setStrokeWidth(8 * d); arc.setStrokeCap(Paint.Cap.ROUND);
        txt.setColor(c.getColor(R.color.ink)); txt.setTextAlign(Paint.Align.CENTER); txt.setTextSize(17 * d);
        txt.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        arc.setShader(new SweepGradient(w / 2f, h / 2f,
                new int[]{getContext().getColor(R.color.primary), getContext().getColor(R.color.violet), getContext().getColor(R.color.primary)}, null));
    }

    public void set(float p) {
        final float to = Math.max(0f, Math.min(1f, p));
        if (va != null) va.cancel();
        va = ValueAnimator.ofFloat(shown, to);
        va.setDuration(180);
        va.addUpdateListener(x -> { shown = (Float) x.getAnimatedValue(); invalidate(); });
        va.start();
    }

    @Override protected void onDraw(Canvas c) {
        float pad = 8 * d;
        r.set(pad, pad, getWidth() - pad, getHeight() - pad);
        c.drawArc(r, 0, 360, false, track);
        c.save();
        c.rotate(-90, getWidth() / 2f, getHeight() / 2f);
        c.drawArc(r, 0, 360 * shown, false, arc);
        c.restore();
        c.drawText(Math.round(shown * 100) + "%", getWidth() / 2f, getHeight() / 2f + txt.getTextSize() / 3f, txt);
    }
}

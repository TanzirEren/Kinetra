package com.kinetra.app;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

/** Small UI toolkit: cards, chips, sliders, toggles, fields and motion. */
public class Ui {
    public interface IntCb { void on(int v); }
    public interface BoolCb { void on(boolean v); }
    public interface StrCb { void on(String v); }
    public interface Fmt { String f(int v); }

    public static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    public static int num(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }

    public static int idx(int[] a, int v) { for (int i = 0; i < a.length; i++) if (a[i] == v) return i; return -1; }

    public static int idx(int[][] a, int w, int h) { for (int i = 0; i < a.length; i++) if (a[i][0] == w && a[i][1] == h) return i; return -1; }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(c.getColor(color));
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static LinearLayout card(Context c, String title, String sub) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundResource(R.drawable.bg_card);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        if (title != null) l.addView(text(c, title, 16, R.color.ink, true));
        if (sub != null) {
            TextView s = text(c, sub, 12.5f, R.color.sub, false);
            s.setPadding(0, dp(c, 2), 0, dp(c, 4));
            l.addView(s);
        }
        return l;
    }

    public static TextView label(Context c, String s) {
        TextView t = text(c, s, 12.5f, R.color.sub, true);
        t.setPadding(0, dp(c, 14), 0, dp(c, 8));
        return t;
    }

    public static TextView chip(Context c, String s, boolean on) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, 0, dp(c, 8), 0);
        t.setLayoutParams(lp);
        style(t, on);
        t.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        press(t);
        return t;
    }

    public static void style(TextView t, boolean on) {
        int l = t.getPaddingLeft(), tp = t.getPaddingTop(), r = t.getPaddingRight(), b = t.getPaddingBottom();
        t.setBackgroundResource(on ? R.drawable.bg_chip_sel : R.drawable.bg_chip);
        t.setPadding(l, tp, r, b);
        t.setTextColor(t.getContext().getColor(on ? R.color.white : R.color.ink));
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    public static View chips(Context c, String[] labels, int sel, IntCb cb) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(row);
        final TextView[] ts = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int k = i;
            ts[i] = chip(c, labels[i], i == sel);
            ts[i].setOnClickListener(x -> {
                for (int j = 0; j < ts.length; j++) style(ts[j], j == k);
                cb.on(k);
            });
            row.addView(ts[i]);
        }
        return hs;
    }

    public static View slider(Context c, String title, int min, int max, int val, Fmt fmt, IntCb cb) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(c, 8), 0, 0);
        LinearLayout row = new LinearLayout(c);
        row.addView(text(c, title, 14.5f, R.color.ink, false), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView vt = text(c, fmt.f(val), 13.5f, R.color.primary, true);
        row.addView(vt);
        col.addView(row);
        SeekBar sb = new SeekBar(c);
        sb.setMax(max - min);
        sb.setProgress(val - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) { vt.setText(fmt.f(p + min)); if (user) cb.on(p + min); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        col.addView(sb);
        return col;
    }

    public static View toggle(Context c, String title, String sub, boolean on, BoolCb cb) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 8), 0, dp(c, 4));
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(text(c, title, 14.5f, R.color.ink, false));
        if (sub != null) l.addView(text(c, sub, 12, R.color.sub, false));
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch s = new Switch(c);
        s.setChecked(on);
        s.setOnCheckedChangeListener((b, v) -> cb.on(v));
        row.addView(s);
        return row;
    }

    public static void watch(EditText e, StrCb cb) {
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void afterTextChanged(Editable s) { cb.on(s.toString()); }
        });
    }

    public static EditText field(Context c, String hint, String val, boolean multi, StrCb cb) {
        EditText e = new EditText(c);
        e.setHint(hint); e.setText(val); e.setTextSize(14);
        e.setTextColor(c.getColor(R.color.ink));
        e.setHintTextColor(c.getColor(R.color.sub));
        e.setBackgroundResource(R.drawable.bg_input);
        e.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        if (multi) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP);
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        } else {
            e.setSingleLine(true);
        }
        watch(e, cb);
        return e;
    }

    public static Button btn(Context c, String s, boolean primary) {
        Button b = new Button(c);
        b.setText(s); b.setAllCaps(false); b.setTextSize(14.5f);
        b.setStateListAnimator(null);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setBackgroundResource(primary ? R.drawable.bg_btn_primary : R.drawable.bg_btn_soft);
        b.setTextColor(c.getColor(primary ? R.color.white : R.color.primary));
        b.setMinHeight(0); b.setMinimumHeight(0);
        b.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        press(b);
        return b;
    }

    public static View iconBtn(Context c, int res, View.OnClickListener l) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setColorFilter(c.getColor(R.color.ink));
        v.setPadding(dp(c, 9), dp(c, 9), dp(c, 9), dp(c, 9));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));
        v.setOnClickListener(l);
        press(v);
        return v;
    }

    /** Press feedback: quick squish. */
    public static void press(View v) {
        v.setOnTouchListener((x, e) -> {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) x.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) x.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            return false;
        });
    }

    /** Entrance motion: children rise and fade in one after another. */
    public static void stagger(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            c.setAlpha(0f);
            c.setTranslationY(dp(g.getContext(), 26));
            c.animate().alpha(1f).translationY(0f).setStartDelay(55L * i).setDuration(380)
                    .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        }
    }

    public static View videoRow(final MainActivity a, final File f, boolean actions, final Runnable onChange) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackgroundResource(R.drawable.bg_card);
        int p = dp(a, 12);
        r.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(a, 10));
        r.setLayoutParams(lp);
        ImageView ic = new ImageView(a);
        ic.setImageResource(R.drawable.ic_movie);
        ic.setColorFilter(a.getColor(R.color.primary));
        ic.setBackgroundResource(R.drawable.bg_icon_round);
        ic.setPadding(dp(a, 11), dp(a, 11), dp(a, 11), dp(a, 11));
        r.addView(ic, new LinearLayout.LayoutParams(dp(a, 46), dp(a, 46)));
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(a, 12), 0, dp(a, 8), 0);
        TextView n = text(a, f.getName(), 14.5f, R.color.ink, true);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        t.addView(n);
        t.addView(text(a, f.getParentFile().getName() + " · " + Store.size(f.length()) + " · " + Store.date(f.lastModified()), 12, R.color.sub, false));
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(iconBtn(a, R.drawable.ic_play, v -> a.play(f)));
        if (actions) {
            r.addView(iconBtn(a, R.drawable.ic_share, v -> a.share(f)));
            r.addView(iconBtn(a, R.drawable.ic_delete, v -> a.confirmDelete(f, onChange)));
        }
        r.setOnClickListener(v -> a.play(f));
        press(r);
        return r;
    }
}

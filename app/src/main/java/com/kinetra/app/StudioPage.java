package com.kinetra.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public class StudioPage implements MainActivity.Page {
    static final int IDLE = 0, LOADING = 1, READY = 2, REC = 3, DONE = 4;
    static final String DESK = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    static final String DUR_JS = "(function(){try{if(window.KINETRA_DURATION)return +window.KINETRA_DURATION;var m=0;"
            + "document.getAnimations().forEach(function(x){var e=x.effect&&x.effect.getComputedTiming().endTime;if(isFinite(e)&&e>m)m=e});"
            + "return m>0?m+600:0}catch(e){return 0}})()";

    final MainActivity a;
    final View v;
    final Handler main = new Handler(Looper.getMainLooper());
    final FrameLayout holder;
    final ProgressRing ring;
    final TextView tvStatus, tvTime, tvFrames, tvLog, tvResult;
    final Button btnStart, btnStop;
    final View resultCard;
    WebView web;
    Recorder rec;
    int state = IDLE;
    boolean pending;
    File lastFile;
    final StringBuilder logb = new StringBuilder();

    StudioPage(MainActivity act) {
        a = act;
        v = LayoutInflater.from(a).inflate(R.layout.page_studio, null);
        holder = v.findViewById(R.id.previewHolder);
        ring = v.findViewById(R.id.ring);
        tvStatus = v.findViewById(R.id.tvStatus);
        tvTime = v.findViewById(R.id.tvTime);
        tvFrames = v.findViewById(R.id.tvFrames);
        tvLog = v.findViewById(R.id.tvLog);
        tvResult = v.findViewById(R.id.tvResult);
        btnStart = v.findViewById(R.id.btnStart);
        btnStop = v.findViewById(R.id.btnStop);
        resultCard = v.findViewById(R.id.resultCard);
        for (int id : new int[]{R.id.btnStart, R.id.btnStop, R.id.btnReload, R.id.btnShot, R.id.btnPlay, R.id.btnShare}) Ui.press(v.findViewById(id));
        btnStart.setOnClickListener(x -> start());
        btnStop.setOnClickListener(x -> { if (rec != null) rec.stop(); });
        v.findViewById(R.id.btnReload).setOnClickListener(x -> { if (state != REC) load(); });
        v.findViewById(R.id.btnShot).setOnClickListener(x -> snap());
        v.findViewById(R.id.btnPlay).setOnClickListener(x -> { if (lastFile != null) a.play(lastFile); });
        v.findViewById(R.id.btnShare).setOnClickListener(x -> { if (lastFile != null) a.share(lastFile); });
        log("Kinetra ready.");
        setState(IDLE);
    }

    void log(String s) {
        logb.append("› ").append(s).append('\n');
        String[] lines = logb.toString().split("\n");
        if (lines.length > 9) {
            logb.setLength(0);
            for (int i = lines.length - 9; i < lines.length; i++) logb.append(lines[i]).append('\n');
        }
        tvLog.setText(logb.toString().trim());
    }

    void status(String s) { tvStatus.setText(s); }

    void setState(int s) {
        state = s;
        boolean busy = s == LOADING || s == REC;
        btnStart.setEnabled(!busy);
        btnStart.setAlpha(busy ? .5f : 1f);
        btnStop.setVisibility(s == REC ? View.VISIBLE : View.GONE);
    }

    void ensureWeb() {
        if (web != null) return;
        web = new WebView(a);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { onLoaded(); }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage m) { log("page: " + m.message()); return true; }
        });
        holder.addView(web);
    }

    void layoutWeb() {
        int availW = a.getResources().getDisplayMetrics().widthPixels - Ui.dp(a, 60);
        float sc = Math.min(availW / (float) Cfg.w, Ui.dp(a, 420) / (float) Cfg.h);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams((int) (Cfg.w * sc), (int) (Cfg.h * sc));
        hp.gravity = android.view.Gravity.CENTER;
        holder.setLayoutParams(hp);
        FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(Cfg.w, Cfg.h);
        web.setLayoutParams(wp);
        web.setPivotX(0f);
        web.setPivotY(0f);
        web.setScaleX(sc);
        web.setScaleY(sc);
        web.setInitialScale(Cfg.scale);
        web.setBackgroundColor(Cfg.BG[Cfg.bgIdx]);
        web.getSettings().setUserAgentString(Cfg.desktop ? DESK : null);
    }

    void loadSource() {
        if (Cfg.src == 2) {
            String u = Cfg.url.trim();
            if (!u.startsWith("http") && !u.startsWith("file")) u = "https://" + u;
            web.loadUrl(u);
        } else {
            web.loadDataWithBaseURL("https://kinetra.local/", Cfg.html, "text/html", "UTF-8", null);
        }
    }

    /** Called from Create: build the live preview. */
    public void load() {
        ensureWeb();
        layoutWeb();
        pending = false;
        resultCard.setVisibility(View.GONE);
        ring.set(0f);
        setState(LOADING);
        status("Loading page…");
        loadSource();
    }

    void inject(boolean forRec) {
        StringBuilder css = new StringBuilder();
        if (Cfg.hideScroll) css.append("::-webkit-scrollbar{display:none!important}html{scrollbar-width:none}");
        css.append(Cfg.css);
        StringBuilder js = new StringBuilder();
        js.append("(function(){var s=document.createElement('style');s.textContent=").append(JSONObject.quote(css.toString()))
                .append(";(document.head||document.documentElement).appendChild(s);})();");
        float sp = Cfg.SPEEDS[Cfg.speedIdx];
        if (sp != 1f) js.append("setInterval(function(){document.getAnimations().forEach(function(x){if(x.playbackRate!=")
                .append(sp).append(")x.playbackRate=").append(sp).append("})},150);");
        if (!Cfg.js.trim().isEmpty()) js.append("try{").append(Cfg.js).append("\n}catch(e){console.log('custom js: '+e)}");
        if (forRec && !Cfg.clickSel.trim().isEmpty())
            js.append("setTimeout(function(){var e=document.querySelector(").append(JSONObject.quote(Cfg.clickSel.trim()))
                    .append(");if(e)e.click()},60);");
        web.evaluateJavascript(js.toString(), null);
    }

    void onLoaded() {
        if (web == null || state == REC) return;
        inject(pending);
        web.evaluateJavascript(DUR_JS, val -> {
            long d = 0;
            try { d = (long) Double.parseDouble(val.replace("\"", "")); } catch (Exception ignored) { }
            Cfg.detectedMs = d > 0 ? d : 10000;
            if (pending) {
                pending = false;
                main.postDelayed(this::beginRec, Cfg.trim * 100L + 150);
            } else {
                setState(READY);
                status("Live preview · auto length " + String.format(Locale.US, "%.1f", Cfg.durMs() / 1000.0) + " s" + (d > 0 ? "" : " (default)"));
                tvTime.setText("00:00 / " + fmt(Cfg.durMs()));
            }
        });
    }

    void start() {
        if (state == REC || state == LOADING) return;
        if (web == null) { a.toast("Open a project from Create first"); a.go(1); return; }
        if (!Store.hasAccess(a)) {
            a.toast("Allow “All files access” so Kinetra can save to /Kinetra, then tap Start again");
            Store.requestAccess(a);
            return;
        }
        pending = true;
        resultCard.setVisibility(View.GONE);
        ring.set(0f);
        setState(LOADING);
        status("Restarting page for a clean take…");
        layoutWeb();
        loadSource();
    }

    void beginRec() {
        File dir = Store.projectDir(Cfg.project);
        File out = Store.unique(dir, Cfg.fileName, "mp4");
        long dur = Cfg.durMs();
        log("Recording " + fmt(dur) + " → " + out.getName());
        if (Cfg.awake) a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setState(REC);
        status("Recording… keep Kinetra open");
        rec = new Recorder(a, web, new Recorder.L() {
            @Override public void progress(float p, long ms, int frames) {
                ring.set(p);
                tvTime.setText(fmt(ms) + " / " + fmt(dur));
                tvFrames.setText(frames + " frames · " + (ms > 0 ? frames * 1000L / ms : 0) + " fps captured");
            }
            @Override public void done(File f, long ms) {
                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                lastFile = f;
                ring.set(1f);
                setState(DONE);
                status("Done");
                tvResult.setText(f.getAbsolutePath() + "\n" + Store.size(f.length()) + " · " + fmt(ms));
                resultCard.setVisibility(View.VISIBLE);
                log("Saved " + f.getName());
                if (Cfg.haptic) buzz();
                if (Cfg.autoOpen) a.play(f);
            }
            @Override public void error(String m) {
                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                setState(READY);
                status("Something went wrong");
                log(m);
                a.toast(m);
            }
            @Override public void log(String s) { StudioPage.this.log(s); }
        });
        rec.start(out, dur);
    }

    void snap() {
        if (web == null) { a.toast("Open a project first"); return; }
        if (!Store.hasAccess(a)) { Store.requestAccess(a); return; }
        try {
            Bitmap b = Bitmap.createBitmap(Cfg.w, Cfg.h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.drawColor(Cfg.BG[Cfg.bgIdx]);
            web.draw(c);
            File f = Store.unique(Store.projectDir(Cfg.project), Cfg.fileName + "_snap", "png");
            try (FileOutputStream fo = new FileOutputStream(f)) { b.compress(Bitmap.CompressFormat.PNG, 100, fo); }
            Store.scan(a, f);
            log("PNG saved: " + f.getName());
            a.toast("PNG saved to " + f.getParentFile().getName());
        } catch (Exception e) { a.toast("Couldn't save PNG"); }
    }

    void buzz() {
        try {
            Vibrator vb = (Vibrator) a.getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vb != null) vb.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) { }
    }

    static String fmt(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    @Override public void onShow() {
        Ui.stagger((ViewGroup) v.findViewById(R.id.studioCol));
    }
}

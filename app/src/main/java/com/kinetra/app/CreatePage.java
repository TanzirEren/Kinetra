package com.kinetra.app;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class CreatePage implements MainActivity.Page {
    final MainActivity a;
    final View v;
    final LinearLayout host;
    final TextView tvEst, tvPath, tvPicked;
    final TextView[] chip = new TextView[3];
    final View[] pnl = new View[3];
    final EditText etProject, etFile, etHtml, etUrl;
    EditText etW, etH;
    TextView tvAudio;

    CreatePage(MainActivity act) {
        a = act;
        v = LayoutInflater.from(a).inflate(R.layout.page_create, null);
        host = v.findViewById(R.id.settingsHost);
        tvEst = v.findViewById(R.id.tvEst);
        tvPath = v.findViewById(R.id.tvPath);
        tvPicked = v.findViewById(R.id.tvPicked);
        etProject = v.findViewById(R.id.etProject);
        etFile = v.findViewById(R.id.etFile);
        etHtml = v.findViewById(R.id.etHtml);
        etUrl = v.findViewById(R.id.etUrl);
        chip[0] = v.findViewById(R.id.chipFile); chip[1] = v.findViewById(R.id.chipPaste); chip[2] = v.findViewById(R.id.chipUrl);
        pnl[0] = v.findViewById(R.id.pnlFile); pnl[1] = v.findViewById(R.id.pnlPaste); pnl[2] = v.findViewById(R.id.pnlUrl);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Ui.press(chip[i]);
            chip[i].setOnClickListener(x -> setSrc(k));
        }
        etProject.setText(Cfg.project);
        etFile.setText(Cfg.fileName);
        etUrl.setText(Cfg.url);
        Ui.watch(etProject, s -> { Cfg.project = s; updatePath(); });
        Ui.watch(etFile, s -> { Cfg.fileName = s; updatePath(); });
        Ui.watch(etHtml, s -> { Cfg.html = s; if (Cfg.src == 1) Cfg.pickedName = ""; });
        Ui.watch(etUrl, s -> Cfg.url = s);
        v.findViewById(R.id.btnPick).setOnClickListener(x -> a.pickHtml());
        v.findViewById(R.id.btnAuto).setOnClickListener(x -> autoOptimize());
        v.findViewById(R.id.btnReset).setOnClickListener(x -> { Cfg.reset(); rebuild(); a.toast("Settings reset"); });
        v.findViewById(R.id.btnStudio).setOnClickListener(x -> openStudio());
        Ui.press(v.findViewById(R.id.btnPick));
        Ui.press(v.findViewById(R.id.btnAuto));
        Ui.press(v.findViewById(R.id.btnReset));
        Ui.press(v.findViewById(R.id.btnStudio));
        setSrc(Cfg.src);
        rebuild();
    }

    void setSrc(int i) {
        Cfg.src = i;
        for (int k = 0; k < 3; k++) {
            Ui.style(chip[k], k == i);
            pnl[k].setVisibility(k == i ? View.VISIBLE : View.GONE);
        }
    }

    /** Called after a file or soundtrack is picked. */
    public void refresh() {
        tvPicked.setText(Cfg.pickedName.isEmpty() ? "No file chosen" : Cfg.pickedName + "  ·  " + (Cfg.html.length() / 1024) + " KB");
        etFile.setText(Cfg.fileName);
        setSrc(Cfg.src);
        if (tvAudio != null) tvAudio.setText(Cfg.audioName.isEmpty() ? "No soundtrack" : Cfg.audioName);
        upd();
    }

    void updatePath() {
        tvPath.setText(Store.root().getPath() + "/" + Store.clean(Cfg.project) + "/" + Store.clean(Cfg.fileName)
                + (Cfg.stamp ? "_<time>" : "") + ".mp4");
    }

    void upd() {
        double mb = Cfg.bitrate() / 8.0 / 1e6 * 10;
        tvEst.setText(String.format(Locale.US, "%dx%d · %d fps · %.1f Mbps\n≈ %.1f MB per 10 s",
                Cfg.w, Cfg.h, Cfg.fps, Cfg.bitrate() / 1e6, mb));
        updatePath();
        Cfg.save();
    }

    void autoOptimize() {
        Cfg.autoDur = true; Cfg.autoBitrate = true; Cfg.fps = 30; Cfg.quality = 2; Cfg.hevc = false;
        Cfg.speedIdx = 1; Cfg.scale = 100; Cfg.hideScroll = true;
        boolean portrait = Cfg.h > Cfg.w;
        int tw = portrait ? 1080 : 1920, th = portrait ? 1920 : 1080;
        if (!Store.supports(tw, th)) { tw = portrait ? 720 : 1280; th = portrait ? 1280 : 720; }
        Cfg.w = tw; Cfg.h = th;
        rebuild();
        a.toast("Auto: " + tw + "x" + th + " · 30 fps · High quality");
    }

    void openStudio() {
        boolean empty = Cfg.src == 2 ? Cfg.url.trim().isEmpty() : Cfg.html.trim().isEmpty();
        if (empty) { a.toast(Cfg.src == 2 ? "Enter a URL first" : "Choose a file or paste some HTML first"); return; }
        Cfg.save();
        a.studio.load();
        a.go(2);
    }

    void rebuild() {
        host.removeAllViews();
        LinearLayout c;

        c = Ui.card(a, "Output", "Size, speed and codec of your video");
        host.addView(c);
        c.addView(Ui.label(a, "Format preset"));
        c.addView(Ui.chips(a, Cfg.PN, -1, i -> { Cfg.apply(i); a.toast("Preset: " + Cfg.PN[i]); rebuild(); }));
        c.addView(Ui.label(a, "Resolution"));
        c.addView(Ui.chips(a, Cfg.RN, Ui.idx(Cfg.RV, Cfg.w, Cfg.h), i -> {
            Cfg.w = Cfg.RV[i][0]; Cfg.h = Cfg.RV[i][1];
            etW.setText(String.valueOf(Cfg.w)); etH.setText(String.valueOf(Cfg.h)); upd();
        }));
        c.addView(Ui.label(a, "Custom size (px)"));
        LinearLayout sz = new LinearLayout(a);
        etW = Ui.field(a, "Width", String.valueOf(Cfg.w), false, s -> { int x = Ui.num(s); if (x >= 64) { Cfg.w = Math.min(x, 3840); upd(); } });
        etH = Ui.field(a, "Height", String.valueOf(Cfg.h), false, s -> { int x = Ui.num(s); if (x >= 64) { Cfg.h = Math.min(x, 3840); upd(); } });
        etW.setInputType(InputType.TYPE_CLASS_NUMBER);
        etH.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f), l2 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.setMarginEnd(Ui.dp(a, 8));
        sz.addView(etW, l1);
        sz.addView(etH, l2);
        c.addView(sz);
        c.addView(Ui.label(a, "Frame rate"));
        c.addView(Ui.chips(a, new String[]{"15 fps", "24 fps", "30 fps", "60 fps"}, Ui.idx(Cfg.FPS, Cfg.fps), i -> { Cfg.fps = Cfg.FPS[i]; upd(); }));
        c.addView(Ui.label(a, "Video codec"));
        c.addView(Ui.chips(a, new String[]{"H.264 · compatible", "H.265 · smaller"}, Cfg.hevc ? 1 : 0, i -> {
            if (i == 1 && !Store.hasEncoder("video/hevc")) { a.toast("No H.265 encoder on this phone"); Cfg.hevc = false; rebuild(); return; }
            Cfg.hevc = i == 1; upd();
        }));

        c = Ui.card(a, "Quality", "Bitrate decides sharpness vs file size");
        host.addView(c);
        c.addView(Ui.chips(a, new String[]{"Draft", "Balanced", "High", "Ultra"}, Cfg.quality, i -> { Cfg.quality = i; upd(); }));
        c.addView(Ui.toggle(a, "Auto bitrate", "Smart bitrate from size, fps and quality", Cfg.autoBitrate, b -> { Cfg.autoBitrate = b; upd(); }));
        c.addView(Ui.slider(a, "Manual bitrate", 1, 60, Cfg.mbps, x -> x + " Mbps", x -> { Cfg.mbps = x; Cfg.autoBitrate = false; upd(); }));

        c = Ui.card(a, "Timing", "How long to record and how it moves");
        host.addView(c);
        c.addView(Ui.toggle(a, "Auto duration", "Detects CSS animations, or window.KINETRA_DURATION", Cfg.autoDur, b -> { Cfg.autoDur = b; upd(); }));
        c.addView(Ui.slider(a, "Manual duration", 1, 300, Cfg.durSec, x -> x + " s", x -> { Cfg.durSec = x; Cfg.autoDur = false; upd(); }));
        c.addView(Ui.slider(a, "Trim start", 0, 50, Cfg.trim, x -> (x / 10.0) + " s", x -> { Cfg.trim = x; upd(); }));
        c.addView(Ui.label(a, "Animation speed (CSS / Web Animations)"));
        c.addView(Ui.chips(a, new String[]{"0.5×", "1×", "1.5×", "2×", "3×"}, Cfg.speedIdx, i -> { Cfg.speedIdx = i; upd(); }));
        c.addView(Ui.slider(a, "Fade in", 0, 30, Cfg.fadeIn, x -> (x / 10.0) + " s", x -> { Cfg.fadeIn = x; upd(); }));
        c.addView(Ui.slider(a, "Fade out", 0, 30, Cfg.fadeOut, x -> (x / 10.0) + " s", x -> { Cfg.fadeOut = x; upd(); }));

        c = Ui.card(a, "Look", "Background, scale and watermark");
        host.addView(c);
        c.addView(Ui.label(a, "Background colour"));
        c.addView(Ui.chips(a, Cfg.BGN, Cfg.bgIdx, i -> { Cfg.bgIdx = i; upd(); }));
        c.addView(Ui.label(a, "Page scale (higher = bigger text)"));
        c.addView(Ui.chips(a, new String[]{"100%", "125%", "150%", "200%"}, Ui.idx(Cfg.SCALES, Cfg.scale), i -> { Cfg.scale = Cfg.SCALES[i]; upd(); }));
        c.addView(Ui.label(a, "Watermark text"));
        c.addView(Ui.field(a, "e.g. @tanzirdev", Cfg.watermark, false, s -> { Cfg.watermark = s; upd(); }));
        c.addView(Ui.label(a, "Watermark position"));
        c.addView(Ui.chips(a, new String[]{"Top left", "Top right", "Bottom left", "Bottom right", "Center"}, Cfg.wmPos, i -> { Cfg.wmPos = i; upd(); }));
        c.addView(Ui.slider(a, "Watermark opacity", 10, 100, Cfg.wmAlpha, x -> x + "%", x -> { Cfg.wmAlpha = x; upd(); }));

        c = Ui.card(a, "Capture", "How the page is loaded and recorded");
        host.addView(c);
        c.addView(Ui.toggle(a, "Hide scrollbars", "Cleaner frames", Cfg.hideScroll, b -> { Cfg.hideScroll = b; upd(); }));
        c.addView(Ui.toggle(a, "Scroll capture", "Records the page scrolling top to bottom", Cfg.scrollCap, b -> { Cfg.scrollCap = b; upd(); }));
        c.addView(Ui.toggle(a, "Desktop mode", "Loads the desktop version of websites", Cfg.desktop, b -> { Cfg.desktop = b; upd(); }));
        c.addView(Ui.label(a, "Auto-click selector (for pages that wait for a tap)"));
        c.addView(Ui.field(a, "#start", Cfg.clickSel, false, s -> { Cfg.clickSel = s; upd(); }));

        c = Ui.card(a, "Soundtrack", "Page audio isn't captured. Add AAC/M4A music instead.");
        host.addView(c);
        tvAudio = Ui.text(a, Cfg.audioName.isEmpty() ? "No soundtrack" : Cfg.audioName, 13.5f, R.color.ink, false);
        tvAudio.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 10));
        c.addView(tvAudio);
        LinearLayout ar = new LinearLayout(a);
        android.widget.Button pick = Ui.btn(a, "Choose audio", false), rm = Ui.btn(a, "Remove", false);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
        rl.setMarginStart(Ui.dp(a, 10));
        pick.setOnClickListener(x -> a.pickAudio());
        rm.setOnClickListener(x -> { Cfg.audioPath = ""; Cfg.audioName = ""; tvAudio.setText("No soundtrack"); });
        ar.addView(pick);
        ar.addView(rm, rl);
        c.addView(ar);

        c = Ui.card(a, "Extras", null);
        host.addView(c);
        c.addView(Ui.toggle(a, "Save poster image", "A PNG thumbnail next to your video", Cfg.poster, b -> { Cfg.poster = b; upd(); }));
        c.addView(Ui.toggle(a, "Add timestamp to file name", "Never overwrite an older render", Cfg.stamp, b -> { Cfg.stamp = b; upd(); }));

        c = Ui.card(a, "Advanced", "Runs inside the page before recording");
        host.addView(c);
        c.addView(Ui.label(a, "Custom CSS"));
        c.addView(Ui.field(a, "body { zoom: 1.1 }", Cfg.css, true, s -> { Cfg.css = s; Cfg.save(); }));
        c.addView(Ui.label(a, "Custom JavaScript"));
        c.addView(Ui.field(a, "document.title = 'Hi'", Cfg.js, true, s -> { Cfg.js = s; Cfg.save(); }));

        upd();
    }

    @Override public void onShow() {
        refresh();
        Ui.stagger((ViewGroup) v.findViewById(R.id.createCol));
    }
}

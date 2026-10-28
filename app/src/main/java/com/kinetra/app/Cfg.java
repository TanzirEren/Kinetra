package com.kinetra.app;

import android.content.Context;
import android.content.SharedPreferences;

/** All user settings in one place (static for speed and simplicity). */
public class Cfg {
    public static String project = "My First Project", fileName = "reel", url = "", html = "", pickedName = "",
            css = "", js = "", clickSel = "", watermark = "", audioPath = "", audioName = "";
    public static int src = 0, w = 1280, h = 720, fps = 30, quality = 1, mbps = 8, durSec = 10, trim = 0,
            speedIdx = 1, fadeIn = 0, fadeOut = 0, bgIdx = 0, scale = 100, wmPos = 3, wmAlpha = 70;
    public static long detectedMs = 10000;
    public static boolean autoBitrate = true, autoDur = true, hevc = false, hideScroll = true, scrollCap = false,
            desktop = false, poster = true, stamp = true, autoOpen = false, haptic = true, awake = true;

    public static final float[] SPEEDS = {0.5f, 1f, 1.5f, 2f, 3f};
    public static final int[] BG = {0xFFFFFFFF, 0xFFFBF3E3, 0xFF0B0C10, 0xFFEEF0FF, 0xFFE6F8F1};
    public static final String[] BGN = {"White", "Cream", "Black", "Indigo mist", "Mint"};
    public static final String[] PN = {"Instagram Reel", "TikTok / Shorts", "YouTube 1080p", "YouTube 720p",
            "Square Post", "WhatsApp Status", "Portrait 4:5", "Smooth 60 fps", "Draft Fast"};
    public static final int[][] PV = {{1080, 1920, 30, 2}, {1080, 1920, 30, 2}, {1920, 1080, 30, 2}, {1280, 720, 30, 1},
            {1080, 1080, 30, 2}, {720, 1280, 30, 1}, {1080, 1350, 30, 2}, {1280, 720, 60, 2}, {640, 360, 15, 0}};
    public static final String[] RN = {"360p", "720p", "1080p", "9:16 HD", "9:16 FHD", "1:1 FHD", "4:5 FHD"};
    public static final int[][] RV = {{640, 360}, {1280, 720}, {1920, 1080}, {720, 1280}, {1080, 1920}, {1080, 1080}, {1080, 1350}};
    public static final int[] FPS = {15, 24, 30, 60};
    public static final int[] SCALES = {100, 125, 150, 200};

    static SharedPreferences sp;

    public static void apply(int i) {
        w = PV[i][0]; h = PV[i][1]; fps = PV[i][2]; quality = PV[i][3]; autoBitrate = true;
    }

    public static int bitrate() {
        if (!autoBitrate) return mbps * 1000000;
        double[] bpp = {0.07, 0.10, 0.15, 0.22};
        double b = (double) w * h * fps * bpp[quality];
        if (hevc) b *= 0.7;
        return (int) Math.max(1500000, Math.min(60000000, b));
    }

    public static long durMs() {
        long d = autoDur ? (long) (detectedMs / SPEEDS[speedIdx]) : durSec * 1000L;
        return Math.max(1000, d);
    }

    public static void reset() {
        w = 1280; h = 720; fps = 30; quality = 1; mbps = 8; durSec = 10; trim = 0; speedIdx = 1; fadeIn = 0; fadeOut = 0;
        bgIdx = 0; scale = 100; wmPos = 3; wmAlpha = 70; watermark = ""; css = ""; js = ""; clickSel = "";
        autoBitrate = true; autoDur = true; hevc = false; hideScroll = true; scrollCap = false; desktop = false;
        poster = true; stamp = true; audioPath = ""; audioName = "";
    }

    public static void load(Context c) {
        sp = c.getSharedPreferences("kinetra", 0);
        project = sp.getString("project", project); fileName = sp.getString("file", fileName); url = sp.getString("url", url);
        css = sp.getString("css", css); js = sp.getString("js", js); clickSel = sp.getString("click", clickSel);
        watermark = sp.getString("wm", watermark);
        w = sp.getInt("w", w); h = sp.getInt("h", h); fps = sp.getInt("fps", fps); quality = sp.getInt("q", quality);
        mbps = sp.getInt("mbps", mbps); durSec = sp.getInt("dur", durSec); trim = sp.getInt("trim", trim);
        speedIdx = sp.getInt("speed", speedIdx); fadeIn = sp.getInt("fin", fadeIn); fadeOut = sp.getInt("fout", fadeOut);
        bgIdx = sp.getInt("bg", bgIdx); scale = sp.getInt("scale", scale); wmPos = sp.getInt("wmpos", wmPos);
        wmAlpha = sp.getInt("wma", wmAlpha);
        autoBitrate = sp.getBoolean("abr", autoBitrate); autoDur = sp.getBoolean("adur", autoDur);
        hevc = sp.getBoolean("hevc", hevc); hideScroll = sp.getBoolean("hs", hideScroll);
        scrollCap = sp.getBoolean("sc", scrollCap); desktop = sp.getBoolean("desk", desktop);
        poster = sp.getBoolean("poster", poster); stamp = sp.getBoolean("stamp", stamp);
        autoOpen = sp.getBoolean("open", autoOpen); haptic = sp.getBoolean("hap", haptic); awake = sp.getBoolean("awake", awake);
    }

    public static void save() {
        if (sp == null) return;
        sp.edit().putString("project", project).putString("file", fileName).putString("url", url)
                .putString("css", css).putString("js", js).putString("click", clickSel).putString("wm", watermark)
                .putInt("w", w).putInt("h", h).putInt("fps", fps).putInt("q", quality).putInt("mbps", mbps)
                .putInt("dur", durSec).putInt("trim", trim).putInt("speed", speedIdx).putInt("fin", fadeIn)
                .putInt("fout", fadeOut).putInt("bg", bgIdx).putInt("scale", scale).putInt("wmpos", wmPos)
                .putInt("wma", wmAlpha).putBoolean("abr", autoBitrate).putBoolean("adur", autoDur)
                .putBoolean("hevc", hevc).putBoolean("hs", hideScroll).putBoolean("sc", scrollCap)
                .putBoolean("desk", desktop).putBoolean("poster", poster).putBoolean("stamp", stamp)
                .putBoolean("open", autoOpen).putBoolean("hap", haptic).putBoolean("awake", awake).apply();
    }
}

package com.kinetra.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Paths: /storage/emulated/0/Kinetra/<Project>/<File>.<ext> */
public class Store {
    public static File root() { return new File(Environment.getExternalStorageDirectory(), "Kinetra"); }

    public static String clean(String s) {
        s = s == null ? "" : s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return s.isEmpty() ? "Project" : s;
    }

    public static File projectDir(String p) { return new File(root(), clean(p)); }

    public static boolean hasAccess(Context c) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestAccess(Activity a) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                a.startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + a.getPackageName())), MainActivity.RQ_ACCESS);
            } catch (Exception e) {
                a.startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), MainActivity.RQ_ACCESS);
            }
        } else {
            a.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, MainActivity.RQ_ACCESS);
        }
    }

    public static File unique(File dir, String base, String ext) {
        dir.mkdirs();
        base = clean(base);
        if (Cfg.stamp && ext.equals("mp4")) base += "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File f = new File(dir, base + "." + ext);
        int n = 1;
        while (f.exists()) f = new File(dir, base + "_" + (n++) + "." + ext);
        return f;
    }

    public static void scan(Context c, File f) {
        try { MediaScannerConnection.scanFile(c, new String[]{f.getAbsolutePath()}, null, null); } catch (Exception ignored) { }
    }

    public static List<File> videos() {
        List<File> out = new ArrayList<>();
        File[] ps = root().listFiles();
        if (ps != null) for (File p : ps) {
            File[] fs = p.isDirectory() ? p.listFiles() : null;
            if (fs != null) for (File f : fs) if (f.getName().toLowerCase(Locale.US).endsWith(".mp4")) out.add(f);
        }
        Collections.sort(out, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return out;
    }

    public static String size(long b) {
        if (b < 1024 * 1024) return String.format(Locale.US, "%.0f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format(Locale.US, "%.1f MB", b / 1048576.0);
        return String.format(Locale.US, "%.2f GB", b / 1073741824.0);
    }

    public static String date(long ms) { return new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(new Date(ms)); }

    public static void copy(File a, File b) throws Exception {
        try (InputStream in = new FileInputStream(a); OutputStream out = new FileOutputStream(b)) {
            byte[] buf = new byte[1 << 16]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    public static boolean hasEncoder(String mime) {
        try {
            for (MediaCodecInfo i : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (!i.isEncoder()) continue;
                for (String t : i.getSupportedTypes()) if (t.equalsIgnoreCase(mime)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    public static boolean supports(int w, int h) {
        try {
            for (MediaCodecInfo i : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (!i.isEncoder()) continue;
                for (String t : i.getSupportedTypes()) if (t.equalsIgnoreCase("video/avc")
                        && i.getCapabilitiesForType(t).getVideoCapabilities().isSizeSupported(w, h)) return true;
            }
        } catch (Exception e) { return true; }
        return false;
    }

    public static String encoders() {
        StringBuilder sb = new StringBuilder();
        try {
            for (MediaCodecInfo i : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (!i.isEncoder()) continue;
                for (String t : i.getSupportedTypes()) {
                    if (t.equals("video/avc") || t.equals("video/hevc")) {
                        MediaCodecInfo.VideoCapabilities v = i.getCapabilitiesForType(t).getVideoCapabilities();
                        sb.append(i.getName()).append("\n   ").append(t.equals("video/avc") ? "H.264" : "H.265")
                                .append(" up to ").append(v.getSupportedWidths().getUpper()).append("x")
                                .append(v.getSupportedHeights().getUpper()).append("\n");
                    }
                }
            }
        } catch (Exception ignored) { }
        return sb.length() == 0 ? "No video encoders found" : sb.toString().trim();
    }
}

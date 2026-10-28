package com.kinetra.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.webkit.WebView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * Captures a WebView frame by frame, pushes frames into a hardware video encoder
 * (MediaCodec input Surface) and writes an MP4 with MediaMuxer.
 */
public class Recorder {
    public interface L {
        void progress(float p, long ms, int frames);
        void done(File f, long ms);
        void error(String m);
        void log(String s);
    }

    final Context ctx; final WebView web; final L lis;
    final Handler main = new Handler(Looper.getMainLooper());
    MediaCodec codec; Surface surface; MediaMuxer muxer;
    Bitmap bmp, posterBmp; Canvas bc;
    int W, H, track = -1, frames;
    long startNs, durMs, firstPts = -1;
    volatile boolean stop, muxOn, failed;
    boolean finished;
    File tmp, out;
    final Paint fadeP = new Paint(), wmP = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Recorder(Context c, WebView w, L l) { ctx = c; web = w; lis = l; }

    public void stop() { stop = true; }

    public void start(File outFile, long dur) {
        out = outFile; durMs = dur;
        W = Cfg.w & ~1; H = Cfg.h & ~1;
        try {
            String mime = Cfg.hevc ? "video/hevc" : "video/avc";
            MediaCodec probe = MediaCodec.createEncoderByType(mime);
            MediaCodecInfo.VideoCapabilities vc = probe.getCodecInfo().getCapabilitiesForType(mime).getVideoCapabilities();
            probe.release();
            if (!vc.isSizeSupported(W, H)) {
                W = (W + 15) & ~15; H = (H + 15) & ~15;
                if (!vc.isSizeSupported(W, H)) {
                    lis.error("This phone's " + (Cfg.hevc ? "H.265" : "H.264") + " encoder can't do " + Cfg.w + "x" + Cfg.h + ". Try Auto-optimize.");
                    return;
                }
                lis.log("Size adjusted to " + W + "x" + H + " for the encoder");
            }
            MediaFormat f = MediaFormat.createVideoFormat(mime, W, H);
            f.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            f.setInteger(MediaFormat.KEY_BIT_RATE, Cfg.bitrate());
            f.setInteger(MediaFormat.KEY_FRAME_RATE, Cfg.fps);
            f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            codec = MediaCodec.createEncoderByType(mime);
            codec.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = codec.createInputSurface();
            codec.start();
            tmp = new File(ctx.getCacheDir(), "k_" + System.currentTimeMillis() + ".mp4");
            muxer = new MediaMuxer(tmp.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            bc = new Canvas(bmp);
            lis.log("Encoder ready: " + W + "x" + H + " @ " + Cfg.fps + "fps, " + String.format(java.util.Locale.US, "%.1f", Cfg.bitrate() / 1e6) + " Mbps");
        } catch (Exception e) {
            lis.error("Encoder error: " + e.getMessage());
            return;
        }
        new Thread(this::drain, "kinetra-drain").start();
        startNs = System.nanoTime();
        main.post(tick);
    }

    final Runnable tick = new Runnable() {
        @Override public void run() {
            if (finished) return;
            long el = (System.nanoTime() - startNs) / 1000000L;
            if (stop || failed || el >= durMs) { finish(); return; }
            try {
                drawFrame(el);
                frames++;
                lis.progress(Math.min(1f, el / (float) durMs), el, frames);
            } catch (Exception e) {
                failed = true;
                finish();
                return;
            }
            long next = startNs + frames * 1000000000L / Math.max(1, Cfg.fps);
            main.postDelayed(this, Math.max(1, (next - System.nanoTime()) / 1000000L));
        }
    };

    void drawFrame(long el) {
        if (Cfg.scrollCap) {
            web.evaluateJavascript("(function(){var d=document.documentElement,b=document.body,m=Math.max(0,Math.max(d.scrollHeight,b?b.scrollHeight:0)-window.innerHeight);window.scrollTo(0,m*"
                    + (el / (float) durMs) + ");})()", null);
        }
        bc.drawColor(Cfg.BG[Cfg.bgIdx]);
        int save = bc.save();
        if (web.getWidth() > 0 && web.getHeight() > 0 && web.getWidth() != W)
            bc.scale(W / (float) web.getWidth(), H / (float) web.getHeight());
        web.draw(bc);
        bc.restoreToCount(save);

        if (!Cfg.watermark.isEmpty()) {
            float sz = H > W ? W * 0.045f : H * 0.05f, pad = W * 0.04f;
            wmP.setColor(Color.WHITE);
            wmP.setAlpha(Cfg.wmAlpha * 255 / 100);
            wmP.setTextSize(sz);
            wmP.setShadowLayer(4, 0, 2, 0x99000000);
            wmP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            float tw = wmP.measureText(Cfg.watermark), x = pad, y = pad + sz;
            if (Cfg.wmPos == 1 || Cfg.wmPos == 3) x = W - pad - tw;
            if (Cfg.wmPos == 2 || Cfg.wmPos == 3) y = H - pad;
            if (Cfg.wmPos == 4) { x = (W - tw) / 2f; y = H / 2f + sz / 3f; }
            bc.drawText(Cfg.watermark, x, y, wmP);
        }
        float a = 0f;
        long fi = Cfg.fadeIn * 100L, fo = Cfg.fadeOut * 100L;
        if (fi > 0 && el < fi) a = 1f - el / (float) fi;
        if (fo > 0 && el > durMs - fo) a = Math.max(a, (el - (durMs - fo)) / (float) fo);
        if (a > 0f) {
            fadeP.setColor(Color.BLACK);
            fadeP.setAlpha((int) (Math.min(1f, a) * 255));
            bc.drawRect(0, 0, W, H, fadeP);
        }
        if (Cfg.poster && posterBmp == null && el >= durMs * 0.3) posterBmp = bmp.copy(Bitmap.Config.ARGB_8888, false);

        Canvas c;
        try { c = surface.lockHardwareCanvas(); } catch (Exception e) { c = surface.lockCanvas(null); }
        c.drawBitmap(bmp, 0, 0, null);
        surface.unlockCanvasAndPost(c);
    }

    void finish() {
        if (finished) return;
        finished = true;
        main.removeCallbacks(tick);
        try { codec.signalEndOfInputStream(); } catch (Exception ignored) { }
    }

    void drain() {
        MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
        String err = null;
        try {
            boolean eos = false;
            while (!eos) {
                int i = codec.dequeueOutputBuffer(bi, 10000);
                if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = muxer.addTrack(codec.getOutputFormat());
                    muxer.start();
                    muxOn = true;
                } else if (i >= 0) {
                    ByteBuffer b = codec.getOutputBuffer(i);
                    if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bi.size = 0;
                    if (bi.size > 0 && muxOn && b != null) {
                        if (firstPts < 0) firstPts = bi.presentationTimeUs;
                        bi.presentationTimeUs = Math.max(0, bi.presentationTimeUs - firstPts);
                        b.position(bi.offset);
                        b.limit(bi.offset + bi.size);
                        muxer.writeSampleData(track, b, bi);
                    }
                    codec.releaseOutputBuffer(i, false);
                    if ((bi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) eos = true;
                }
            }
        } catch (Exception e) { err = String.valueOf(e); failed = true; }
        try { codec.stop(); } catch (Exception ignored) { }
        try { codec.release(); } catch (Exception ignored) { }
        try { surface.release(); } catch (Exception ignored) { }
        try { if (muxOn) muxer.stop(); } catch (Exception e) { if (err == null) err = "mux: " + e; }
        try { muxer.release(); } catch (Exception ignored) { }

        final long took = (System.nanoTime() - startNs) / 1000000L;
        final String ferr = err;
        if (err != null && !muxOn) { main.post(() -> lis.error("Encoding failed: " + ferr)); return; }
        try {
            File audio = Cfg.audioPath.isEmpty() ? null : new File(Cfg.audioPath);
            boolean muxed = false;
            if (audio != null && audio.exists()) {
                try { Mux.addAudio(tmp, audio, out, took * 1000L); muxed = true; }
                catch (Exception e) { final String m = e.getMessage(); main.post(() -> lis.log("Soundtrack skipped: " + m)); }
            }
            if (!muxed) Store.copy(tmp, out);
            tmp.delete();
            if (posterBmp != null) {
                File p = new File(out.getParentFile(), out.getName().replace(".mp4", "_poster.png"));
                try (FileOutputStream fo = new FileOutputStream(p)) { posterBmp.compress(Bitmap.CompressFormat.PNG, 100, fo); }
                Store.scan(ctx, p);
            }
            Store.scan(ctx, out);
            main.post(() -> lis.done(out, took));
        } catch (Exception e) {
            final String m = String.valueOf(e);
            main.post(() -> lis.error("Save failed: " + m));
        }
    }
}

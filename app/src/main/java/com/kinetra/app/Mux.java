package com.kinetra.app;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Adds an AAC soundtrack to the rendered video without re-encoding. */
public class Mux {
    static int pick(MediaExtractor e, String prefix) {
        for (int i = 0; i < e.getTrackCount(); i++) {
            String m = e.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (m != null && m.startsWith(prefix)) return i;
        }
        return -1;
    }

    static void copy(MediaExtractor ex, int track, MediaMuxer mx, int idx, long limitUs) {
        ex.selectTrack(track);
        ByteBuffer buf = ByteBuffer.allocate(1 << 20);
        MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
        while (true) {
            int n = ex.readSampleData(buf, 0);
            if (n < 0) break;
            long ts = ex.getSampleTime();
            if (ts > limitUs) break;
            bi.set(0, n, ts, (ex.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0 ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
            mx.writeSampleData(idx, buf, bi);
            ex.advance();
        }
    }

    public static void addAudio(File video, File audio, File out, long videoUs) throws IOException {
        MediaExtractor ve = new MediaExtractor(), ae = new MediaExtractor();
        try {
            ve.setDataSource(video.getAbsolutePath());
            ae.setDataSource(audio.getAbsolutePath());
            int vt = pick(ve, "video/"), at = pick(ae, "audio/");
            if (vt < 0 || at < 0) throw new IOException("No audio track found");
            MediaFormat af = ae.getTrackFormat(at);
            if (!"audio/mp4a-latm".equals(af.getString(MediaFormat.KEY_MIME)))
                throw new IOException("Soundtrack must be AAC (.m4a / .aac / .mp4)");
            MediaMuxer mx = new MediaMuxer(out.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int vi = mx.addTrack(ve.getTrackFormat(vt)), ai = mx.addTrack(af);
            mx.start();
            copy(ve, vt, mx, vi, Long.MAX_VALUE);
            copy(ae, at, mx, ai, videoUs);
            mx.stop();
            mx.release();
        } finally {
            ve.release();
            ae.release();
        }
    }
}

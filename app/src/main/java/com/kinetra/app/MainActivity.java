package com.kinetra.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    public interface Page { void onShow(); }

    public static final int RQ_HTML = 21, RQ_AUDIO = 22, RQ_ACCESS = 11;
    final View[] views = new View[5];
    final Page[] pages = new Page[5];
    final ImageView[] ni = new ImageView[5];
    final TextView[] nt = new TextView[5];
    int cur = -1;
    HomePage home; CreatePage create; StudioPage studio; LibraryPage library; MorePage more;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Cfg.load(this);
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build()); // allow file:// sharing
        setContentView(R.layout.activity_main);
        FrameLayout host = findViewById(R.id.pages);
        home = new HomePage(this); create = new CreatePage(this); studio = new StudioPage(this);
        library = new LibraryPage(this); more = new MorePage(this);
        views[0] = home.v; views[1] = create.v; views[2] = studio.v; views[3] = library.v; views[4] = more.v;
        pages[0] = home; pages[1] = create; pages[2] = studio; pages[3] = library; pages[4] = more;
        int[] nav = {R.id.nav0, R.id.nav1, R.id.nav2, R.id.nav3, R.id.nav4};
        int[] icon = {R.id.navi0, R.id.navi1, R.id.navi2, R.id.navi3, R.id.navi4};
        int[] txt = {R.id.navt0, R.id.navt1, R.id.navt2, R.id.navt3, R.id.navt4};
        for (int i = 0; i < 5; i++) {
            final int k = i;
            host.addView(views[i]);
            views[i].setVisibility(View.GONE);
            ni[i] = findViewById(icon[i]);
            nt[i] = findViewById(txt[i]);
            findViewById(nav[i]).setOnClickListener(v -> go(k));
        }
        go(0);
        handleIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); handleIntent(i); }

    void handleIntent(Intent i) {
        if (i == null || !Intent.ACTION_VIEW.equals(i.getAction()) || i.getData() == null) return;
        loadHtmlUri(i.getData());
    }

    public void go(int i) {
        if (i == cur) return;
        if (cur >= 0) {
            final View out = views[cur];
            final int idx = cur;
            out.animate().alpha(0f).setDuration(120).withEndAction(() -> { if (cur != idx) out.setVisibility(View.GONE); }).start();
        }
        View in = views[i];
        in.animate().cancel();
        in.setVisibility(View.VISIBLE);
        in.setAlpha(0f);
        in.setTranslationY(Ui.dp(this, 14));
        in.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        cur = i;
        for (int k = 0; k < 5; k++) {
            boolean on = k == i;
            ni[k].setColorFilter(getColor(on ? R.color.primary : R.color.sub));
            nt[k].setTextColor(getColor(on ? R.color.primary : R.color.sub));
            ni[k].animate().scaleX(on ? 1.18f : 1f).scaleY(on ? 1.18f : 1f).setDuration(200).start();
        }
        pages[i].onShow();
    }

    public void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    public void pickHtml() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, RQ_HTML);
    }

    public void pickAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/mp4"});
        startActivityForResult(i, RQ_AUDIO);
    }

    String nameOf(Uri u) {
        String n = null;
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) { int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (ix >= 0) n = c.getString(ix); }
        } catch (Exception ignored) { }
        if (n == null) n = u.getLastPathSegment();
        return n == null ? "file.html" : n;
    }

    void loadHtmlUri(Uri u) {
        try (InputStream in = getContentResolver().openInputStream(u)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[1 << 16]; int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            Cfg.html = bo.toString("UTF-8");
            Cfg.src = 0;
            Cfg.pickedName = nameOf(u);
            String base = Cfg.pickedName.replaceAll("\\.[^.]*$", "");
            if (!base.isEmpty()) Cfg.fileName = base;
            create.refresh();
            go(1);
            toast("Loaded " + Cfg.pickedName);
        } catch (Exception e) { toast("Couldn't read that file"); }
    }

    @Override protected void onActivityResult(int rq, int res, Intent data) {
        super.onActivityResult(rq, res, data);
        if (rq == RQ_ACCESS) { library.onShow(); more.onShow(); return; }
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        if (rq == RQ_HTML) loadHtmlUri(u);
        if (rq == RQ_AUDIO) {
            try (InputStream in = getContentResolver().openInputStream(u)) {
                File f = new File(getCacheDir(), "soundtrack.bin");
                try (FileOutputStream fo = new FileOutputStream(f)) {
                    byte[] buf = new byte[1 << 16]; int n;
                    while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
                }
                Cfg.audioPath = f.getAbsolutePath();
                Cfg.audioName = nameOf(u);
                create.refresh();
                toast("Soundtrack added");
            } catch (Exception e) { toast("Couldn't read that audio"); }
        }
    }

    @Override public void onRequestPermissionsResult(int rq, String[] p, int[] r) {
        super.onRequestPermissionsResult(rq, p, r);
        library.onShow();
        more.onShow();
    }

    public void play(File f) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(f), "video/mp4");
            startActivity(i);
        } catch (Exception e) { toast("No video player found"); }
    }

    public void share(File f) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("video/mp4");
        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
        startActivity(Intent.createChooser(i, "Share video"));
    }

    public void confirmDelete(final File f, final Runnable after) {
        new AlertDialog.Builder(this).setTitle("Delete video?").setMessage(f.getName())
                .setPositiveButton("Delete", (d, w) -> { f.delete(); Store.scan(this, f); if (after != null) after.run(); })
                .setNegativeButton("Cancel", null).show();
    }

    @Override public void onBackPressed() {
        if (cur != 0) go(0); else super.onBackPressed();
    }
}

package com.kinetra.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibraryPage implements MainActivity.Page {
    final MainActivity a;
    final View v;
    final LinearLayout list, sortHost;
    final TextView sub, empty;
    final Button grant;
    int sort = 0;

    LibraryPage(MainActivity act) {
        a = act;
        v = LayoutInflater.from(a).inflate(R.layout.page_library, null);
        list = v.findViewById(R.id.libList);
        sortHost = v.findViewById(R.id.sortHost);
        sub = v.findViewById(R.id.tvLibSub);
        empty = v.findViewById(R.id.libEmpty);
        grant = v.findViewById(R.id.btnLibGrant);
        grant.setOnClickListener(x -> Store.requestAccess(a));
        Ui.press(grant);
        sortHost.addView(Ui.chips(a, new String[]{"Newest", "Oldest", "Largest"}, 0, i -> { sort = i; fill(); }));
    }

    void fill() {
        list.removeAllViews();
        boolean ok = Store.hasAccess(a);
        grant.setVisibility(ok ? View.GONE : View.VISIBLE);
        List<File> all = ok ? Store.videos() : new ArrayList<File>();
        if (sort == 1) Collections.reverse(all);
        if (sort == 2) Collections.sort(all, (x, y) -> Long.compare(y.length(), x.length()));
        long total = 0;
        for (File f : all) total += f.length();
        sub.setText(all.size() + " videos · " + Store.size(total) + " · " + Store.root().getPath());
        for (File f : all) list.addView(Ui.videoRow(a, f, true, this::fill));
        empty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(ok ? "No videos yet.\nRender something in Studio and it will appear here."
                : "Kinetra needs “All files access” to save into Kinetra/<Project>/ on your phone.");
    }

    @Override public void onShow() {
        fill();
        Ui.stagger((ViewGroup) v.findViewById(R.id.libCol));
    }
}

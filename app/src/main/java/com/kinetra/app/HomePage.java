package com.kinetra.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashSet;
import java.util.List;

public class HomePage implements MainActivity.Page {
    final MainActivity a;
    final View v;
    final LinearLayout presetRow, recent;
    final TextView empty, stVideos, stSize, stProjects;

    HomePage(MainActivity act) {
        a = act;
        v = LayoutInflater.from(a).inflate(R.layout.page_home, null);
        presetRow = v.findViewById(R.id.presetRow);
        recent = v.findViewById(R.id.recentList);
        empty = v.findViewById(R.id.recentEmpty);
        stVideos = v.findViewById(R.id.stVideos);
        stSize = v.findViewById(R.id.stSize);
        stProjects = v.findViewById(R.id.stProjects);
        v.findViewById(R.id.btnNew).setOnClickListener(x -> a.go(1));
        v.findViewById(R.id.btnOpen).setOnClickListener(x -> a.pickHtml());
        Ui.press(v.findViewById(R.id.btnNew));
        Ui.press(v.findViewById(R.id.btnOpen));
        for (int i = 0; i < Cfg.PN.length; i++) {
            final int k = i;
            LinearLayout c = new LinearLayout(a);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setBackgroundResource(R.drawable.bg_card);
            int p = Ui.dp(a, 14);
            c.setPadding(p, p, p, p);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 150), -2);
            lp.setMargins(0, 0, Ui.dp(a, 10), 0);
            c.setLayoutParams(lp);
            int w = Cfg.PV[i][0], h = Cfg.PV[i][1], g = gcd(w, h);
            TextView pill = Ui.chip(a, (w / g) + ":" + (h / g), false);
            pill.setTextColor(a.getColor(R.color.primary));
            c.addView(pill);
            TextView n = Ui.text(a, Cfg.PN[i], 14.5f, R.color.ink, true);
            n.setPadding(0, Ui.dp(a, 10), 0, 0);
            c.addView(n);
            c.addView(Ui.text(a, w + "×" + h + " · " + Cfg.PV[i][2] + " fps", 12, R.color.sub, false));
            c.setOnClickListener(x -> { Cfg.apply(k); Cfg.save(); a.create.rebuild(); a.toast("Preset: " + Cfg.PN[k]); a.go(1); });
            Ui.press(c);
            presetRow.addView(c);
        }
    }

    static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }

    @Override public void onShow() {
        recent.removeAllViews();
        List<File> all = Store.hasAccess(a) ? Store.videos() : new java.util.ArrayList<File>();
        long total = 0;
        HashSet<String> proj = new HashSet<>();
        for (File f : all) { total += f.length(); proj.add(f.getParent()); }
        stVideos.setText(String.valueOf(all.size()));
        stSize.setText(Store.size(total));
        stProjects.setText(String.valueOf(proj.size()));
        for (int i = 0; i < Math.min(3, all.size()); i++) recent.addView(Ui.videoRow(a, all.get(i), false, null));
        empty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
        Ui.stagger((ViewGroup) v.findViewById(R.id.homeCol));
    }
}

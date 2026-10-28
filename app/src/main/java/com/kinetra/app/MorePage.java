package com.kinetra.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MorePage implements MainActivity.Page {
    final MainActivity a;
    final View v;
    final LinearLayout host;

    MorePage(MainActivity act) {
        a = act;
        v = LayoutInflater.from(a).inflate(R.layout.page_more, null);
        host = v.findViewById(R.id.moreHost);
        build();
    }

    void build() {
        host.removeAllViews();
        LinearLayout c = Ui.card(a, "Preferences", "Small things that make renders smoother");
        host.addView(c);
        c.addView(Ui.toggle(a, "Keep screen awake", "Stops the display sleeping while recording", Cfg.awake, b -> { Cfg.awake = b; Cfg.save(); }));
        c.addView(Ui.toggle(a, "Vibrate when finished", "A short buzz when your MP4 is saved", Cfg.haptic, b -> { Cfg.haptic = b; Cfg.save(); }));
        c.addView(Ui.toggle(a, "Auto-play after render", "Open the video player as soon as it is saved", Cfg.autoOpen, b -> { Cfg.autoOpen = b; Cfg.save(); }));

        c = Ui.card(a, "Storage", "Videos are saved here");
        host.addView(c);
        TextView path = Ui.text(a, Store.root().getPath() + "/<Project>/<File>.mp4", 12.5f, R.color.ink, false);
        path.setTypeface(android.graphics.Typeface.MONOSPACE);
        c.addView(path);
        boolean ok = Store.hasAccess(a);
        TextView st = Ui.text(a, ok ? "Access granted" : "Access needed", 13.5f, ok ? R.color.mint : R.color.danger, true);
        st.setPadding(0, Ui.dp(a, 8), 0, 0);
        c.addView(st);
        if (!ok) {
            Button g = Ui.btn(a, "Allow storage access", true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = Ui.dp(a, 10);
            g.setLayoutParams(lp);
            g.setOnClickListener(x -> Store.requestAccess(a));
            c.addView(g);
        }

        c = Ui.card(a, "Device encoders", "What this phone can encode in hardware");
        host.addView(c);
        TextView enc = Ui.text(a, Store.encoders(), 11.5f, R.color.sub, false);
        enc.setTypeface(android.graphics.Typeface.MONOSPACE);
        c.addView(enc);

        c = Ui.card(a, "Tips", null);
        host.addView(c);
        c.addView(Ui.text(a, "• Pages can set window.KINETRA_DURATION = 12000 (ms) to tell Kinetra how long to record.\n"
                + "• Use Auto-click selector (like #start) for pages that wait for a tap.\n"
                + "• Smaller sizes capture faster; 720p gives the smoothest result on most phones.\n"
                + "• Page sound is not captured. Add an AAC/M4A soundtrack in Create instead.\n"
                + "• Inline your CSS, images and fonts as data: URIs so nothing is missing offline.", 12.5f, R.color.sub, false));

        c = Ui.card(a, null, null);
        host.addView(c);
        ImageView wm = new ImageView(a);
        wm.setImageResource(R.drawable.wordmark_ink);
        wm.setAdjustViewBounds(true);
        c.addView(wm, new LinearLayout.LayoutParams(Ui.dp(a, 150), Ui.dp(a, 37)));
        c.addView(Ui.text(a, "HTML to MP4, on-device. Version 1.0.0", 12.5f, R.color.sub, false));
        Button reset = Ui.btn(a, "Reset all settings", false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = Ui.dp(a, 12);
        reset.setLayoutParams(lp);
        reset.setOnClickListener(x -> { Cfg.reset(); Cfg.save(); a.create.rebuild(); a.toast("Settings reset"); });
        c.addView(reset);
    }

    @Override public void onShow() {
        build();
        Ui.stagger((ViewGroup) v.findViewById(R.id.moreCol));
    }
}

package com.david.ex2lite;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/**
 * Pantalla única, estilo McLaren: negro carbono, naranja papaya, líneas limpias.
 * Izquierda: modos de manejo (Ciudad / Autopista). Derecha: sonido de bloqueo.
 */
public class MainActivity extends Activity {
    static final int BLACK = 0xFF07080A, PANEL = 0xCC101216, PANEL_LINE = 0xFF23272E,
            PAPAYA = 0xFFFF8000, WHITE = 0xFFF5F5F5, DIM = 0xFF8C929B, BTN = 0xFF1A1D23,
            DANGER = 0xFFFF5A4E;

    static final int CITY_DRIVE = 1, CITY_REGEN = 2;   // Eco + Media
    static final int HWY_DRIVE = 3, HWY_REGEN = 2;     // Sport + Media

    private final CarLink car = new CarLink();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences p;

    private LinearLayout cityCard, hwyCard;
    private TextView modeNow, lockNow, versionTxt;
    private LinearLayout lockList;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Prefs.get(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.bg_carbon);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(36), dp(22), dp(36), dp(22));

        body.addView(header());

        LinearLayout cols = new LinearLayout(this);
        cols.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams colsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        colsLp.topMargin = dp(18);
        cols.addView(modesColumn(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f));
        View gap = new View(this);
        cols.addView(gap, new LinearLayout.LayoutParams(dp(20), 1));
        cols.addView(soundColumn(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        body.addView(cols, colsLp);

        root.addView(body, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        KeeperService.start(this);
        new Thread(() -> {
            boolean ok = car.connect(this);
            if (ok) car.listenChanges(new CarLink.Changes(v -> ui.post(this::refreshMode)));
            ui.post(() -> {
                refreshMode();
                if (!ok) toast("No se pudo conectar con el carro");
            });
        }).start();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshMode();
        refreshLock();
    }

    @Override protected void onPause() {
        LockSound.stopPreview();
        super.onPause();
    }

    // ------------------------------------------------------------ cabecera

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);

        View bar = new View(this);
        bar.setBackgroundColor(PAPAYA);
        h.addView(bar, new LinearLayout.LayoutParams(dp(6), dp(44)));

        TextView t = new TextView(this);
        t.setText("EX2  LITE");
        t.setTextColor(WHITE);
        t.setTextSize(30);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        t.setLetterSpacing(0.12f);
        t.setPadding(dp(14), 0, 0, 0);
        h.addView(t);

        View spacer = new View(this);
        h.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        modeNow = new TextView(this);
        modeNow.setTextColor(PAPAYA);
        modeNow.setTextSize(15);
        modeNow.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        modeNow.setLetterSpacing(0.1f);
        h.addView(modeNow);
        return h;
    }

    // ------------------------------------------------------------ modos

    private View modesColumn() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        cityCard = modeCard("CIUDAD", "Eco · Regeneración media", R.drawable.ic_city,
                () -> applyMode(CITY_DRIVE, CITY_REGEN));
        hwyCard = modeCard("AUTOPISTA", "Sport · Regeneración media", R.drawable.ic_highway,
                () -> applyMode(HWY_DRIVE, HWY_REGEN));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(230), 1);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(230), 1);
        lp2.leftMargin = dp(16);
        row.addView(cityCard, lp1);
        row.addView(hwyCard, lp2);
        c.addView(row);

        Switch sw = new Switch(this);
        sw.setText("Recordar al encender el carro");
        sw.setTextColor(WHITE);
        sw.setTextSize(17);
        sw.setChecked(p.getBoolean(Prefs.REMEMBER, true));
        sw.setPadding(dp(4), dp(22), 0, dp(6));
        sw.setOnCheckedChangeListener((v, on) -> {
            p.edit().putBoolean(Prefs.REMEMBER, on).apply();
            if (on) KeeperService.start(this);
        });
        c.addView(sw);

        TextView hint = hint("El modo se guarda solo cada vez que lo cambias, en la pantalla o en el volante. "
                + "Al encender el carro (en P) se vuelve a poner.");
        c.addView(hint);

        Button upd = outline("BUSCAR ACTUALIZACIONES");
        upd.setOnClickListener(v -> {
            toast("Revisando...");
            new Thread(() -> {
                Updater.Result r = Updater.check(this, true);
                ui.post(() -> toast(r.message));
            }).start();
        });
        LinearLayout.LayoutParams ul = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        ul.topMargin = dp(18);
        c.addView(upd, ul);

        versionTxt = hint("Versión " + Updater.installedCode(this));
        versionTxt.setPadding(dp(4), dp(6), 0, 0);
        c.addView(versionTxt);
        return c;
    }

    private LinearLayout modeCard(String title, String sub, int icon, Runnable onPick) {
        LinearLayout card = panel();
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setOnClickListener(v -> onPick.run());

        ImageView img = new ImageView(this);
        img.setImageResource(icon);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(img, new LinearLayout.LayoutParams(dp(96), dp(96)));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(WHITE);
        t.setTextSize(24);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        t.setLetterSpacing(0.08f);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(12), 0, 0);
        card.addView(t);

        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextColor(DIM);
        s.setTextSize(14);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, dp(4), 0, 0);
        card.addView(s);

        TextView tag = new TextView(this);
        tag.setText("ACTIVO");
        tag.setTextColor(BLACK);
        tag.setTextSize(11);
        tag.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tag.setLetterSpacing(0.15f);
        tag.setPadding(dp(10), dp(3), dp(10), dp(3));
        tag.setBackground(rounded(PAPAYA, 0, 0));
        tag.setVisibility(View.INVISIBLE);
        tag.setTag("tag");
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = dp(14);
        card.addView(tag, tl);
        return card;
    }

    private void applyMode(int drive, int regen) {
        p.edit().putInt(Prefs.DRIVE, drive).putInt(Prefs.REGEN, regen).apply();
        refreshMode();
        new Thread(() -> {
            boolean ok1 = car.setDrive(drive);
            boolean ok2 = car.setRegen(regen);
            if (!ok1 || !ok2) ui.post(() -> toast("El carro no aceptó el cambio"));
        }).start();
    }

    private void refreshMode() {
        int d = p.getInt(Prefs.DRIVE, 0), r = p.getInt(Prefs.REGEN, 0);
        boolean city = d == CITY_DRIVE && r == CITY_REGEN;
        boolean hwy = d == HWY_DRIVE && r == HWY_REGEN;
        markCard(cityCard, city);
        markCard(hwyCard, hwy);
        modeNow.setText(city ? "MODO  CIUDAD" : hwy ? "MODO  AUTOPISTA" : "MODO  PERSONALIZADO");
    }

    private void markCard(LinearLayout card, boolean on) {
        card.setBackground(rounded(on ? 0xFF1B1408 : PANEL, on ? PAPAYA : PANEL_LINE, on ? 3 : 1));
        View tag = card.findViewWithTag("tag");
        if (tag != null) tag.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
    }

    // ------------------------------------------------------------ sonido

    private View soundColumn() {
        LinearLayout c = panel();
        c.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView t = new TextView(this);
        t.setText("SONIDO DE BLOQUEO");
        t.setTextColor(WHITE);
        t.setTextSize(20);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        t.setLetterSpacing(0.1f);
        c.addView(t);

        lockNow = new TextView(this);
        lockNow.setTextColor(PAPAYA);
        lockNow.setTextSize(15);
        lockNow.setPadding(0, dp(6), 0, dp(8));
        c.addView(lockNow);

        LinearLayout actions = new LinearLayout(this);
        Button rescan = outline("BUSCAR");
        rescan.setOnClickListener(v -> refreshLock());
        Button verify = outline("VERIFICAR");
        verify.setOnClickListener(v -> verifyLock());
        Button orig = outline("ORIGINAL");
        orig.setOnClickListener(v -> {
            toast(LockSound.restore(this) ? "Listo: sonido original" : "No se pudo restaurar");
            refreshLock();
        });
        actions.addView(rescan, weight());
        actions.addView(verify, weightGap());
        actions.addView(orig, weightGap());
        c.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(this);
        lockList = new LinearLayout(this);
        lockList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(lockList);
        c.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        c.addView(hint("Pon tus .wav (máx. 1 MB) en la USB. Cópialos al carro con COPIAR y luego ya no necesitas la USB. "
                + "Se quedan en la carpeta " + LockSound.FOLDER + "."));
        return c;
    }

    private void refreshLock() {
        String name = p.getString(Prefs.LOCK_NAME, null);
        lockNow.setText(name == null ? "Ahora: el del carro" : "Ahora: " + name);

        lockList.removeAllViews();
        List<File> files = LockSound.findCandidates();
        if (files.isEmpty()) {
            lockList.addView(hint("No encontré archivos .wav. Conecta la USB y toca BUSCAR."));
            return;
        }
        for (File f : files) lockList.addView(lockRow(f));
    }

    private void verifyLock() {
        File f = LockSound.ours(this);
        String cur = LockSound.current(this);
        String msg;
        if (f == null) msg = "No tienes un sonido elegido.";
        else if (cur != null && cur.equals(f.getAbsolutePath())) msg = "Bien: el sonido está puesto.";
        else {
            LockSound.enforce(this);
            String now = LockSound.current(this);
            msg = (now != null && now.equals(f.getAbsolutePath()))
                    ? "Estaba cambiado; lo volví a poner."
                    : "No pude ponerlo. Revisa el archivo.";
        }
        toast(msg);
        refreshLock();
    }

    private View lockRow(File f) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView n = new TextView(this);
        n.setText(f.getName());
        n.setTextColor(WHITE);
        n.setTextSize(16);
        n.setSingleLine(true);
        TextView s = new TextView(this);
        String bad = LockSound.check(f);
        boolean usb = !f.getAbsolutePath().startsWith("/sdcard/");
        s.setText(bad != null ? bad : (f.length() / 1024) + " KB · " + (usb ? "USB" : "en el carro"));
        s.setTextColor(bad != null ? DANGER : DIM);
        s.setTextSize(12);
        s.setSingleLine(true);
        info.addView(n);
        info.addView(s);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (usb && bad == null) {
            Button copy = small("COPIAR");
            copy.setOnClickListener(v -> {
                String e = LockSound.copyToCar(f);
                toast(e == null ? "Copiado al carro" : e);
                refreshLock();
            });
            row.addView(copy, smallLp(0));
        }
        Button play = small("PROBAR");
        play.setOnClickListener(v -> { String e = LockSound.preview(f); if (e != null) toast(e); });
        row.addView(play, smallLp(8));

        Button use = small("USAR");
        use.setEnabled(bad == null);
        use.setAlpha(bad == null ? 1f : 0.35f);
        use.setTextColor(PAPAYA);
        use.setOnClickListener(v -> {
            String e = LockSound.apply(this, f);
            toast(e == null ? "Listo. Ciérralo con la llave para oírlo." : e);
            refreshLock();
        });
        row.addView(use, smallLp(8));
        return row;
    }

    // ------------------------------------------------------------ helpers de UI

    private LinearLayout panel() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(PANEL, PANEL_LINE, 1));
        return c;
    }

    private TextView hint(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(DIM);
        t.setTextSize(13);
        t.setPadding(0, dp(12), 0, 0);
        return t;
    }

    private Button outline(String s) {
        Button b = baseButton(s, 13);
        b.setBackground(rounded(BTN, PANEL_LINE, 1));
        return b;
    }

    private Button small(String s) {
        Button b = baseButton(s, 12);
        b.setBackground(rounded(BTN, PANEL_LINE, 1));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        return b;
    }

    private LinearLayout.LayoutParams smallLp(int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        lp.leftMargin = dp(leftDp);
        return lp;
    }

    private Button baseButton(String s, int size) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(size);
        b.setTextColor(WHITE);
        b.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        b.setLetterSpacing(0.08f);
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, dp(48), 1); }

    private LinearLayout.LayoutParams weightGap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
        lp.leftMargin = dp(8);
        return lp;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(6));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}

package com.david.ex2lite;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Menú de inicio estilo McLaren: fondo ilustrado, tarjetas de cristal, naranja papaya.
 * Inicio → Manejo / Sonido / Red / Actualizar.
 */
public class MainActivity extends Activity {
    // Colores del tema activo (se rellenan en applyTheme)
    private int BLACK, ACCENT, ACCENT_HI, ACCENT_LO, WHITE, DIM, SOFT, DANGER,
            GLASS_TOP, GLASS_BOT, GLASS_LINE, FIELD, ON_ACCENT, A10, A22, A33, A44, AAA, HERO_TOP, HERO_BOT;
    private boolean tron;
    private float textScale = 1f;

    private void applyTheme() {
        tron = p.getInt(Prefs.THEME, 1) == 1;
        if (tron) {
            BLACK = 0xFF01040A; ACCENT = 0xFF19E6FF; ACCENT_HI = 0xFF7DF3FF; ACCENT_LO = 0xFF0AA6C2;
            WHITE = 0xFFEAFBFF; DIM = 0xFF7FA6B5; SOFT = 0xFFBFE9F2; DANGER = 0xFFFF7A18;
            GLASS_TOP = 0xE6061C2A; GLASS_BOT = 0xE602090F; GLASS_LINE = 0x8019E6FF; FIELD = 0x2619E6FF;
            ON_ACCENT = 0xFF001018; A10 = 0x1019E6FF; A22 = 0x2219E6FF; A33 = 0x3319E6FF; A44 = 0x4419E6FF; AAA = 0xAA19E6FF;
            HERO_TOP = 0xFF04303F; HERO_BOT = 0xFF020A12;
            textScale = 0.82f;
        } else {
            BLACK = 0xFF07080A; ACCENT = 0xFFFF8000; ACCENT_HI = 0xFFFFA03A; ACCENT_LO = 0xFFE86A00;
            WHITE = 0xFFF5F5F5; DIM = 0xFF9AA1AB; SOFT = 0xFFD7DBE0; DANGER = 0xFFFF5A4E;
            GLASS_TOP = 0xE6222833; GLASS_BOT = 0xE60E1116; GLASS_LINE = 0x33FFFFFF; FIELD = 0x40FFFFFF;
            ON_ACCENT = ON_ACCENT; A10 = 0x10FF8000; A22 = 0x22FF8000; A33 = 0x33FF8000; A44 = 0x44FF8000; AAA = 0xAAFF8000;
            HERO_TOP = 0xFF4A2400; HERO_BOT = 0xFF14161B;
            textScale = 1f;
        }
    }

    private int cardR() { return tron ? 6 : 26; }

    static final int REQ_LOCATION = 7;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences p;
    private Typeface fXB, fSB, fLT;

    private FrameLayout root;
    private View home, soundPage, netPage, updPage, current;

    // inicio
    private TextView clock, dateTxt, wifiChip, ipChip, updBanner;
    private View tileNet, tileUpd;
    private TextView homeSound;

    // sonido
    private TextView lockNow;
    private LinearLayout lockList, gainRow;

    // red
    private TextView netSsid, netIp, netSignal;
    private Switch wifiSwitch;
    private boolean syncingSwitch;
    private LinearLayout netList;
    private final ScanReceiver scanRx = new ScanReceiver(this);
    private boolean scanRegistered;

    // actualizar
    private TextView updStatus, launchInfo;
    private String launchNote = "";

    private final Runnable periodic = this::tickPeriodic;

    private void tickPeriodic() {
        updateClock();
        refreshNet();
        ui.postDelayed(periodic, 5000);
    }

    void onScanResults() {
        ui.post(() -> { if (current == netPage) refreshNetList(); });
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Prefs.get(this);
        applyTheme();
        if (tron) {
            fXB = font(R.font.orb_xbold, Typeface.create("sans-serif-condensed", Typeface.BOLD));
            fSB = font(R.font.orb_medium, Typeface.create("sans-serif-medium", Typeface.NORMAL));
            fLT = font(R.font.orb_regular, Typeface.create("sans-serif-light", Typeface.NORMAL));
        } else {
            fXB = font(R.font.mont_xbold_italic, Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
            fSB = font(R.font.mont_semibold, Typeface.create("sans-serif-medium", Typeface.BOLD));
            fLT = font(R.font.mont_light, Typeface.create("sans-serif-light", Typeface.NORMAL));
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(BLACK);
        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImageResource(tron ? R.drawable.bg_tron : R.drawable.bg_home);
        root.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (tron) {   // oscurece un poco la rejilla para que se lea el contenido
            View dim = new View(this);
            dim.setBackgroundColor(0x55000000);
            root.addView(dim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        home = homePage();
        soundPage = page("SONIDO DE BLOQUEO", soundContent());
        netPage = page("RED WI-FI", netContent());
        updPage = page("ACTUALIZAR", updContent());
        for (View v : new View[]{home, soundPage, netPage, updPage}) {
            root.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            v.setVisibility(View.GONE);
        }
        setContentView(root);
        TouchFx.apply(root);
        noteLaunch();
        show(home);
        checkUpdateQuietly();
    }

    @Override protected void onNewIntent(android.content.Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        noteLaunch();
    }

    /** Al abrir, revisa en silencio si hay versión nueva y lo avisa en el inicio. */
    private void checkUpdateQuietly() {
        new Thread(() -> {
            String v = Updater.peek(this);
            if (v != null) ui.post(() -> {
                updBanner.setText("NUEVA VERSIÓN " + v + "  ·  TOCA PARA ACTUALIZAR");
                updBanner.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private void runUpdate() {
        updStatus.setText("Revisando...");
        new Thread(() -> {
            Updater.Result r = Updater.check(this, true);
            ui.post(() -> { updStatus.setText(r.message); toast(r.message); });
        }).start();
    }

    /** Anota quién abrió la app, para poder rastrear aperturas que nadie pidió. */
    private void noteLaunch() {
        String ref = "?";
        try {
            android.net.Uri u = getReferrer();
            if (u != null) ref = u.toString().replace("android-app://", "");
        } catch (Throwable ignored) {}
        String caller = getCallingPackage();
        launchNote = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())
                + "  origen: " + ref + (caller != null ? "  llamó: " + caller : "");
        p.edit().putString("last_launch", launchNote).apply();
        if (launchInfo != null) launchInfo.setText("Última apertura  " + launchNote);
    }

    @Override protected void onResume() {
        super.onResume();
        registerScan();
        refreshLock();
        refreshNet();
        ui.removeCallbacks(periodic);
        ui.post(periodic);
    }

    @Override protected void onPause() {
        LockSound.stopPreview();
        ui.removeCallbacks(periodic);
        unregisterScan();
        super.onPause();
    }

    @Override public void onBackPressed() {
        if (current != null && current != home) show(home);
        else finishAndRemoveTask();
    }

    private void show(View v) {
        for (View x : new View[]{home, soundPage, netPage, updPage}) {
            x.setVisibility(x == v ? View.VISIBLE : View.GONE);
        }
        current = v;
        if (v == soundPage) refreshLock();
        if (v == netPage) showNet();
        if (v == home) refreshAll();
    }

    private void refreshAll() {
        refreshLock();
        refreshNet();
        updateClock();
    }

    // ------------------------------------------------------------ inicio

    private View homePage() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(32), dp(24), dp(32), dp(24));

        // ---- columna izquierda: marca, reloj, red
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView fox = new ImageView(this);
        fox.setImageResource(tron ? R.drawable.fox_tron : R.drawable.fox_hero);
        brand.addView(fox, new LinearLayout.LayoutParams(dp(92), dp(92)));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(6), 0, 0, 0);
        TextView t1 = tv("EX2 LITE", 26, WHITE, fXB);
        t1.setLetterSpacing(0.08f);
        TextView t2 = tv("GEELY EX2", 11, ACCENT, fSB);
        t2.setLetterSpacing(0.32f);
        names.addView(t1);
        names.addView(t2);
        brand.addView(names);
        left.addView(brand);

        left.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));

        clock = tv("--:--", 66, WHITE, fLT);
        clock.setIncludeFontPadding(false);
        left.addView(clock);
        dateTxt = tv("", 13, ACCENT, fSB);
        dateTxt.setLetterSpacing(0.18f);
        dateTxt.setPadding(dp(4), dp(4), 0, 0);
        left.addView(dateTxt);

        left.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 0.6f));

        updBanner = tv("", 13, ON_ACCENT, fSB);
        updBanner.setPadding(dp(16), dp(9), dp(16), dp(9));
        updBanner.setBackground(papayaPill());
        updBanner.setVisibility(View.GONE);
        updBanner.setOnClickListener(v -> { show(updPage); runUpdate(); });
        LinearLayout.LayoutParams ubl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ubl.bottomMargin = dp(12);
        left.addView(updBanner, ubl);

        TextView themeBtn = tv(tron ? "TEMA: TRON  \u21C4" : "TEMA: PAPAYA  \u21C4", 12, ACCENT, fSB);
        themeBtn.setLetterSpacing(0.14f);
        themeBtn.setPadding(dp(16), dp(9), dp(16), dp(9));
        themeBtn.setBackground(ripple(shape(FIELD, FIELD, GLASS_LINE, 1, 24)));
        themeBtn.setOnClickListener(v -> {
            p.edit().putInt(Prefs.THEME, tron ? 0 : 1).apply();
            recreate();
        });
        LinearLayout.LayoutParams tbl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tbl.bottomMargin = dp(12);
        left.addView(themeBtn, tbl);

        wifiChip = new TextView(this);
        ipChip = new TextView(this);
        left.addView(chip(R.drawable.ic_wifi, wifiChip), chipLp(0));
        left.addView(chip(R.drawable.ic_ip, ipChip), chipLp(10));

        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.8f));

        // ---- columna derecha: tarjeta grande de modo + tres tarjetas
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.8f);
        rlp.leftMargin = dp(20);

        View big = soundHero();
        right.addView(big, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.12f));

        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        tileNet = wideTile("RED", R.drawable.ic_wifi, () -> show(netPage));
        tileUpd = wideTile("ACTUALIZAR", R.drawable.ic_refresh, () -> show(updPage));
        stack.addView(tileNet, stackLp(0));
        stack.addView(tileUpd, stackLp(14));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        slp.leftMargin = dp(16);
        right.addView(stack, slp);

        row.addView(right, rlp);
        return row;
    }

    /** Tarjeta grande del inicio: el sonido de bloqueo actual. */
    private View soundHero() {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(ripple(shape(HERO_TOP, HERO_BOT, GLASS_LINE, 1, 26)));
        f.setOutlineProvider(new RoundedOutline(dp(cardR())));
        f.setClipToOutline(true);
        f.setOnClickListener(v -> show(soundPage));

        ImageView wave = new ImageView(this);
        wave.setImageResource(tron ? R.drawable.wave_tron : R.drawable.wave);
        wave.setScaleType(ImageView.ScaleType.FIT_XY);
        wave.setAlpha(0.75f);
        FrameLayout.LayoutParams wl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(110), Gravity.CENTER_VERTICAL);
        wl.setMargins(dp(14), 0, dp(14), dp(30));
        f.addView(wave, wl);

        ImageView badge = badge(R.drawable.ic_music, 52);
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.END);
        bl.setMargins(0, dp(18), dp(18), 0);
        f.addView(badge, bl);

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(24), dp(20), dp(24), dp(22));
        TextView lbl = tv("SONIDO DE BLOQUEO", 11, ACCENT, fSB);
        lbl.setLetterSpacing(0.3f);
        txt.addView(lbl);
        homeSound = tv("", 26, WHITE, fXB);
        homeSound.setSingleLine(true);
        homeSound.setEllipsize(TextUtils.TruncateAt.END);
        txt.addView(homeSound);
        TextView pill = tv("TOCA PARA ELEGIR  \u203A", 12, ON_ACCENT, fSB);
        pill.setLetterSpacing(0.12f);
        pill.setPadding(dp(16), dp(8), dp(16), dp(8));
        pill.setBackground(papayaPill());
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pl.topMargin = dp(12);
        txt.addView(pill, pl);
        f.addView(txt, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return f;
    }

    private LinearLayout.LayoutParams stackLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private LinearLayout.LayoutParams chipLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    /** Tarjeta ancha de cristal: insignia + título + dato en vivo + flecha. */
    private LinearLayout wideTile(String title, int icon, Runnable onTap) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.HORIZONTAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(18), dp(10), dp(14), dp(10));
        t.setBackground(ripple(glass(22)));

        t.addView(badge(icon, 56), new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), 0, dp(8), 0);
        TextView tt = tv(title, 19, WHITE, fXB);
        tt.setLetterSpacing(0.06f);
        col.addView(tt);
        TextView sub = tv("", 13, DIM, null);
        sub.setTag("sub");
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(sub);
        t.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        ImageView chev = new ImageView(this);
        chev.setImageResource(R.drawable.ic_chev);
        t.addView(chev, new LinearLayout.LayoutParams(dp(26), dp(26)));

        t.setOnClickListener(v -> onTap.run());
        return t;
    }

    private void setSub(View tile, String s) {
        if (tile == null) return;
        TextView sub = tile.findViewWithTag("sub");
        if (sub != null) sub.setText(s);
    }

    private void updateClock() {
        Locale es = new Locale("es", "CO");
        Date now = new Date();
        clock.setText(new SimpleDateFormat("HH:mm", es).format(now));
        dateTxt.setText(new SimpleDateFormat("EEEE d 'de' MMMM", es).format(now).toUpperCase(es));
    }

    // ------------------------------------------------------------ red

    private void refreshNet() {
        String ssid = NetInfo.ssid(this);
        String ip = NetInfo.ip(this);
        Integer rssi = NetInfo.rssi(this);
        String wTxt = ssid != null ? ssid : (NetInfo.enabled(this) ? "Sin conexión" : "Wi-Fi apagado");
        String iTxt = ip != null ? ip : "Sin IP";

        if (wifiSwitch != null) {
            boolean on = NetInfo.enabled(this);
            if (wifiSwitch.isChecked() != on) {
                syncingSwitch = true;
                wifiSwitch.setChecked(on);
                syncingSwitch = false;
            }
        }
        wifiChip.setText(wTxt);
        ipChip.setText(iTxt);
        netSsid.setText(wTxt);
        netIp.setText(iTxt);
        netSignal.setText(rssi == null ? "No conectado" : "Señal " + quality(rssi) + "  (" + rssi + " dBm)");
        setSub(tileNet, ssid != null ? wTxt + "  ·  " + iTxt : "Sin conexión");
    }

    private static String quality(int rssi) {
        if (rssi >= -55) return "excelente";
        if (rssi >= -67) return "buena";
        if (rssi >= -75) return "regular";
        return "débil";
    }

    private void showNet() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
        refreshNet();
        refreshNetList();
        NetInfo.scan(this);
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == REQ_LOCATION) {
            NetInfo.scan(this);
            refreshNetList();
        }
    }

    private void registerScan() {
        if (scanRegistered) return;
        registerReceiver(scanRx, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        scanRegistered = true;
    }

    private void unregisterScan() {
        if (!scanRegistered) return;
        try { unregisterReceiver(scanRx); } catch (IllegalArgumentException ignored) {}
        scanRegistered = false;
    }

    private View netContent() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout info = glassPanel();
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(28), dp(22), dp(28), dp(22));

        info.addView(ringBadge(R.drawable.ic_wifi), new LinearLayout.LayoutParams(dp(150), dp(150)));

        TextView lbl = tv("CONECTADA A", 11, DIM, fSB);
        lbl.setLetterSpacing(0.3f);
        lbl.setPadding(0, dp(14), 0, 0);
        info.addView(lbl);

        netSsid = tv("", 26, WHITE, fXB);
        netSsid.setSingleLine(true);
        netSsid.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(netSsid);

        LinearLayout ipRow = new LinearLayout(this);
        ipRow.setGravity(Gravity.CENTER_VERTICAL);
        ipRow.setPadding(0, dp(10), 0, 0);
        TextView ipl = tv("IP", 12, ON_ACCENT, fSB);
        ipl.setPadding(dp(8), dp(2), dp(8), dp(2));
        ipl.setBackground(papayaPill());
        ipRow.addView(ipl);
        netIp = tv("", 22, ACCENT, fSB);
        netIp.setPadding(dp(10), 0, 0, 0);
        ipRow.addView(netIp);
        info.addView(ipRow);

        netSignal = tv("", 14, DIM, null);
        netSignal.setPadding(0, dp(8), 0, 0);
        info.addView(netSignal);

        wifiSwitch = new Switch(this);
        wifiSwitch.setText("Wi-Fi");
        wifiSwitch.setTextColor(WHITE);
        wifiSwitch.setTextSize(17 * textScale);
        wifiSwitch.setTypeface(fSB);
        wifiSwitch.setPadding(0, dp(14), 0, 0);
        wifiSwitch.setOnCheckedChangeListener((v, on) -> {
            if (syncingSwitch) return;
            if (!NetInfo.setEnabled(this, on)) toast("No pude cambiar el Wi-Fi");
            ui.postDelayed(this::refreshNet, 1500);
            ui.postDelayed(() -> { NetInfo.scan(this); refreshNetList(); }, 3500);
        });
        styleSwitch(wifiSwitch);
        info.addView(wifiSwitch);

        info.addView(hint("Toca una red de la derecha para conectarte. Si pide clave, aparece un cuadro para escribirla."));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout list = glassPanel();
        list.setPadding(dp(22), dp(18), dp(22), dp(18));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView h = tv("REDES CERCANAS", 12, DIM, fSB);
        h.setLetterSpacing(0.3f);
        head.addView(h, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button scan = ghost("BUSCAR");
        scan.setOnClickListener(v -> { NetInfo.scan(this); toast("Buscando redes..."); });
        head.addView(scan, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));
        list.addView(head);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        netList = new LinearLayout(this);
        netList.setOrientation(LinearLayout.VERTICAL);
        netList.setPadding(0, dp(12), 0, 0);
        sv.addView(netList);
        list.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.4f);
        rl.leftMargin = dp(18);
        row.addView(list, rl);
        return row;
    }

    private void refreshNetList() {
        if (netList == null) return;
        netList.removeAllViews();
        String cur = NetInfo.ssid(this);
        List<ScanResult> nets = NetInfo.nearby(this);
        if (!NetInfo.enabled(this)) {
            netList.addView(hint("El Wi-Fi está apagado. Enciéndelo con el interruptor."));
            return;
        }
        if (nets.isEmpty()) {
            netList.addView(hint("No veo redes todavía. Toca BUSCAR o revisa que el Wi-Fi esté encendido."));
            return;
        }
        for (ScanResult r : nets) netList.addView(netRow(r, r.SSID.equals(cur)));
        TouchFx.apply(netList);
    }

    private View netRow(ScanResult r, boolean connected) {
        boolean secured = NetInfo.isSecured(r);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackground(ripple(rounded(connected ? A33 : FIELD, connected ? ACCENT : 0, connected ? 2 : 0, 16)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView n = tv(r.SSID, 17, WHITE, fSB);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        String sub = (connected ? "Conectada  ·  " : "") + (secured ? "Con clave" : "Abierta") + "  ·  " + r.level + " dBm";
        TextView s = tv(sub, 12, connected ? ACCENT : DIM, null);
        info.addView(n);
        info.addView(s);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (secured) {
            ImageView lock = new ImageView(this);
            lock.setImageResource(R.drawable.ic_lock);
            lock.setColorFilter(DIM);
            row.addView(lock, new LinearLayout.LayoutParams(dp(22), dp(22)));
        }
        row.setOnClickListener(v -> pickNetwork(r, connected));
        return row;
    }

    private void pickNetwork(ScanResult r, boolean connected) {
        if (connected) { toast("Ya estás conectada a " + r.SSID); return; }
        boolean secured = NetInfo.isSecured(r);
        boolean saved = NetInfo.savedId(this, r.SSID) != -1;
        if (secured && !saved) {
            EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            et.setHint("Clave de la red");
            et.setSingleLine(true);
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                    .setTitle(r.SSID)
                    .setView(et)
                    .setPositiveButton("Conectar", (d, w) -> {
                        toast(NetInfo.connect(this, r.SSID, true, et.getText().toString()));
                        ui.postDelayed(this::refreshNet, 3000);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        toast(NetInfo.connect(this, r.SSID, secured, null));
        ui.postDelayed(this::refreshNet, 3000);
    }

    // ------------------------------------------------------------ sonido

    private View soundContent() {
        LinearLayout c = glassPanel();
        c.setPadding(dp(24), dp(18), dp(24), dp(16));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout lblCol = new LinearLayout(this);
        lblCol.setOrientation(LinearLayout.VERTICAL);
        TextView l1 = tv("AHORA SUENA", 11, DIM, fSB);
        l1.setLetterSpacing(0.3f);
        lblCol.addView(l1);
        lockNow = tv("", 20, ACCENT, fXB);
        lockNow.setSingleLine(true);
        lockNow.setEllipsize(TextUtils.TruncateAt.END);
        lblCol.addView(lockNow);
        top.addView(lblCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ImageView wave = new ImageView(this);
        wave.setImageResource(tron ? R.drawable.wave_tron : R.drawable.wave);
        wave.setScaleType(ImageView.ScaleType.FIT_XY);
        top.addView(wave, new LinearLayout.LayoutParams(0, dp(46), 1.3f));
        c.addView(top);

        LinearLayout actions = new LinearLayout(this);
        Button rescan = ghost("BUSCAR");
        rescan.setOnClickListener(v -> refreshLock());
        Button verify = ghost("VERIFICAR");
        verify.setOnClickListener(v -> verifyLock());
        Button orig = ghost("ORIGINAL");
        orig.setOnClickListener(v -> {
            toast(LockSound.restore(this) ? "Listo: sonido original" : "No se pudo restaurar");
            refreshLock();
        });
        actions.addView(rescan, weight(0));
        actions.addView(verify, weight(10));
        actions.addView(orig, weight(10));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(14);
        c.addView(actions, alp);

        TextView gl = tv("VOLUMEN DEL SONIDO", 11, DIM, fSB);
        gl.setLetterSpacing(0.3f);
        gl.setPadding(0, dp(14), 0, dp(6));
        c.addView(gl);
        LinearLayout gains = new LinearLayout(this);
        String[] gn = {"NORMAL", "FUERTE", "MÁXIMO"};
        for (int i = 0; i < 3; i++) {
            final int lvl = i;
            Button gb = ghost(gn[i]);
            gb.setTag("gain" + i);
            gb.setOnClickListener(v -> {
                toast(LockSound.setGain(this, lvl));
                refreshGain();
            });
            gains.addView(gb, weight(i == 0 ? 0 : 10));
        }
        c.addView(gains, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        gainRow = gains;
        refreshGain();

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        lockList = new LinearLayout(this);
        lockList.setOrientation(LinearLayout.VERTICAL);
        lockList.setPadding(0, dp(10), 0, 0);
        sv.addView(lockList);
        c.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        c.addView(hint("Pon tus .wav (máx. 1 MB) en la USB. Cópialos al carro con COPIAR y luego ya no necesitas la USB. "
                + "Se quedan en la carpeta " + LockSound.FOLDER + "."));
        return c;
    }

    private void refreshLock() {
        String name = p.getString(Prefs.LOCK_NAME, null);
        if (lockNow != null) lockNow.setText(name == null ? "El del carro" : name.replaceAll("(?i)\\.wav$", ""));
        if (homeSound != null) homeSound.setText(name == null ? "El del carro" : name.replaceAll("(?i)\\.wav$", ""));
        if (lockList == null) return;

        lockList.removeAllViews();
        List<File> files = LockSound.findCandidates();
        if (files.isEmpty()) {
            lockList.addView(hint("No encontré archivos .wav. Conecta la USB y toca BUSCAR."));
            return;
        }
        for (File f : files) lockList.addView(lockRow(f));
        TouchFx.apply(lockList);
    }

    /** Marca en naranja el nivel de volumen elegido. */
    private void refreshGain() {
        if (gainRow == null) return;
        int sel = p.getInt(Prefs.LOCK_GAIN, 0);
        for (int i = 0; i < 3; i++) {
            Button b = (Button) gainRow.findViewWithTag("gain" + i);
            if (b == null) continue;
            boolean on = i == sel;
            b.setBackground(on ? ripple(papayaPill()) : ripple(rounded(FIELD, GLASS_LINE, 1, 40)));
            b.setTextColor(on ? ON_ACCENT : WHITE);
        }
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
        row.setPadding(dp(16), dp(10), dp(12), dp(10));
        row.setBackground(rounded(FIELD, 0, 0, 16));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(8);
        row.setLayoutParams(rlp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView n = tv(f.getName(), 16, WHITE, fSB);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        String bad = LockSound.check(f);
        boolean usb = !f.getAbsolutePath().startsWith("/sdcard/");
        TextView s = tv(bad != null ? bad : (f.length() / 1024) + " KB · " + (usb ? "USB" : "en el carro"),
                12, bad != null ? DANGER : DIM, null);
        s.setSingleLine(true);
        info.addView(n);
        info.addView(s);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (usb && bad == null) {
            Button copy = ghost("COPIAR");
            copy.setOnClickListener(v -> {
                String e = LockSound.copyToCar(f);
                toast(e == null ? "Copiado al carro" : e);
                refreshLock();
            });
            row.addView(copy, smallLp(0));
        }
        Button play = ghost("PROBAR");
        play.setOnClickListener(v -> LockSound.preview(this, f, msg -> ui.post(() -> { if (msg != null) toast(msg); })));
        row.addView(play, smallLp(8));

        Button use = primary("USAR");
        use.setEnabled(bad == null);
        use.setAlpha(bad == null ? 1f : 0.35f);
        use.setOnClickListener(v -> {
            String e = LockSound.apply(this, f);
            toast(e == null ? "Listo. Ciérralo con la llave para oírlo." : e);
            refreshLock();
        });
        row.addView(use, smallLp(8));
        return row;
    }

    // ------------------------------------------------------------ actualizar

    private View updContent() {
        LinearLayout c = glassPanel();
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(30), dp(20), dp(30), dp(20));

        c.addView(ringBadge(R.drawable.ic_refresh), new LinearLayout.LayoutParams(dp(150), dp(150)));

        TextView v = tv("VERSIÓN " + versionName(), 22, WHITE, fXB);
        v.setLetterSpacing(0.06f);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(12), 0, 0);
        c.addView(v);

        updStatus = tv("Revisa si hay una versión nueva en el repositorio.", 14, DIM, null);
        updStatus.setGravity(Gravity.CENTER);
        updStatus.setPadding(0, dp(8), 0, dp(16));
        c.addView(updStatus);

        Button upd = primary("BUSCAR ACTUALIZACIONES");
        upd.setTextSize(15 * textScale);
        upd.setOnClickListener(x -> runUpdate());
        c.addView(upd, new LinearLayout.LayoutParams(dp(380), dp(56)));

        launchInfo = tv("", 11, DIM, null);
        launchInfo.setGravity(Gravity.CENTER);
        launchInfo.setPadding(0, dp(16), 0, 0);
        launchInfo.setText("Última apertura  " + p.getString("last_launch", "-"));
        c.addView(launchInfo);
        return c;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return String.valueOf(Updater.installedCode(this));
        }
    }

    // ------------------------------------------------------------ páginas

    private View page(String title, View content) {
        LinearLayout pg = new LinearLayout(this);
        pg.setOrientation(LinearLayout.VERTICAL);
        pg.setPadding(dp(32), dp(22), dp(32), dp(22));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setPadding(dp(14), dp(14), dp(14), dp(14));
        back.setBackground(ripple(glassOval()));
        back.setOnClickListener(v -> show(home));
        top.addView(back, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout tcol = new LinearLayout(this);
        tcol.setOrientation(LinearLayout.VERTICAL);
        tcol.setPadding(dp(18), 0, 0, 0);
        TextView t = tv(title, 24, WHITE, fXB);
        t.setLetterSpacing(0.08f);
        tcol.addView(t);
        View bar = new View(this);
        bar.setBackground(papayaPill());
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(dp(44), dp(4));
        bl.topMargin = dp(4);
        tcol.addView(bar, bl);
        top.addView(tcol);
        pg.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        cl.topMargin = dp(18);
        pg.addView(content, cl);
        return pg;
    }

    // ------------------------------------------------------------ piezas de diseño

    private Typeface font(int id, Typeface fallback) {
        try {
            Typeface t = getResources().getFont(id);
            return t != null ? t : fallback;
        } catch (Throwable e) {
            return fallback;
        }
    }

    private TextView tv(String s, int sp, int color, Typeface f) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * textScale);
        if (f != null) t.setTypeface(f);
        return t;
    }

    /** Insignia circular naranja con el icono en blanco. */
    private ImageView badge(int icon, int sizeDp) {
        ImageView b = new ImageView(this);
        b.setImageResource(icon);
        b.setColorFilter(tron ? ON_ACCENT : 0xFFFFFFFF);
        int pad = dp(sizeDp) / 4;
        b.setPadding(pad, pad, pad, pad);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{ACCENT_HI, ACCENT_LO});
        g.setShape(GradientDrawable.OVAL);
        b.setBackground(g);
        return b;
    }

    /** Insignia grande con anillos de resplandor alrededor. */
    private View ringBadge(int icon) {
        FrameLayout f = new FrameLayout(this);
        f.addView(ringView(A22), centered(150));
        f.addView(ringView(A44), centered(122));
        f.addView(badge(icon, 92), centered(92));
        return f;
    }

    private View ringView(int color) {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(0x00000000);
        g.setStroke(dp(2), color);
        v.setBackground(g);
        return v;
    }

    private FrameLayout.LayoutParams centered(int sizeDp) {
        return new FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp), Gravity.CENTER);
    }

    /** Degradado oscuro para que el texto se lea sobre la ilustración. */
    private View shade() {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x10000000, 0x66000000, 0xEE050608});
        v.setBackground(g);
        return v;
    }

    private LinearLayout chip(int icon, TextView text) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(12), dp(8), dp(18), dp(8));
        c.setBackground(glass(24));
        ImageView i = new ImageView(this);
        i.setImageResource(icon);
        i.setColorFilter(ACCENT);
        c.addView(i, new LinearLayout.LayoutParams(dp(20), dp(20)));
        text.setTextColor(WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14 * textScale);
        text.setTypeface(fSB);
        text.setSingleLine(true);
        text.setPadding(dp(10), 0, 0, 0);
        c.addView(text);
        return c;
    }

    private LinearLayout glassPanel() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(glass(26));
        return c;
    }

    private TextView hint(String s) {
        TextView t = tv(s, 13, DIM, null);
        t.setPadding(0, dp(12), 0, 0);
        return t;
    }

    private Button primary(String s) {
        Button b = baseButton(s, 12, ON_ACCENT);
        b.setBackground(ripple(papayaPill()));
        return b;
    }

    private Button ghost(String s) {
        Button b = baseButton(s, 12, WHITE);
        b.setBackground(ripple(rounded(FIELD, GLASS_LINE, 1, 40)));
        return b;
    }

    private Button baseButton(String s, int size, int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * textScale);
        b.setTextColor(color);
        b.setTypeface(fSB);
        b.setLetterSpacing(0.1f);
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(18), 0, dp(18), 0);
        return b;
    }

    private LinearLayout.LayoutParams smallLp(int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        lp.leftMargin = dp(leftDp);
        return lp;
    }

    private LinearLayout.LayoutParams weight(int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1);
        lp.leftMargin = dp(leftDp);
        return lp;
    }

    /** Interruptor visible sobre fondo oscuro: pista clara y pulgar naranja al encender. */
    private void styleSwitch(Switch s) {
        int[] on = {android.R.attr.state_checked};
        GradientDrawable thumbOn = oval(ACCENT_HI, 28), thumbOff = oval(0xFFE6E8EB, 28);
        StateListDrawable thumb = new StateListDrawable();
        thumb.addState(on, thumbOn);
        thumb.addState(new int[]{}, thumbOff);
        GradientDrawable trackOn = pill(AAA, 54, 30), trackOff = pill(0x66FFFFFF, 54, 30);
        trackOff.setStroke(dp(1), 0x66FFFFFF);
        StateListDrawable track = new StateListDrawable();
        track.addState(on, trackOn);
        track.addState(new int[]{}, trackOff);
        s.setThumbDrawable(thumb);
        s.setTrackDrawable(track);
        s.setSwitchMinWidth(dp(54));
    }

    private GradientDrawable oval(int color, int sizeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        g.setSize(dp(sizeDp), dp(sizeDp));
        return g;
    }

    private GradientDrawable pill(int color, int wDp, int hDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(hDp));
        g.setSize(dp(wDp), dp(hDp));
        return g;
    }

    /** Forma con relleno degradado y borde: redondeada en papaya, de esquinas cortadas con neón en Tron. */
    private Drawable shape(int top, int bottom, int stroke, int strokeDp, int radiusDp) {
        if (tron) {
            float cut = dp(Math.max(5, Math.min(14, radiusDp / 2)));
            return new NeonDrawable(top, bottom, stroke, strokeDp > 0 ? dp(strokeDp) * 1.4f : 0, cut, strokeDp > 0);
        }
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private Drawable papayaPill() {
        if (tron) return new NeonDrawable(ACCENT_HI, ACCENT_LO, 0, 0, dp(5), false);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{ACCENT_HI, ACCENT_LO});
        g.setCornerRadius(dp(60));
        return g;
    }

    private Drawable glass(int radiusDp) {
        return shape(GLASS_TOP, GLASS_BOT, GLASS_LINE, 1, radiusDp);
    }

    private GradientDrawable glassOval() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GLASS_TOP, GLASS_BOT});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(dp(tron ? 2 : 1), tron ? ACCENT : GLASS_LINE);
        return g;
    }

    private Drawable rounded(int fill, int stroke, int strokeDp, int radiusDp) {
        return shape(fill, fill, stroke, strokeDp, radiusDp);
    }

    private RippleDrawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(A44), content, null);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}

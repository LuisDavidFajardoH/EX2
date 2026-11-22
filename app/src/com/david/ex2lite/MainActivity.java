package com.david.ex2lite;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
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
 * Menú de inicio estilo McLaren: negro carbono, naranja papaya, mosaicos grandes.
 * Inicio → Manejo / Sonido / Red / Actualizar.
 */
public class MainActivity extends Activity {
    static final int BLACK = 0xFF07080A, PANEL = 0xCC101216, PANEL_LINE = 0xFF23272E,
            PAPAYA = 0xFFFF8000, PAPAYA_HI = 0xFFFF9A2E, PAPAYA_LO = 0xFF7A3A00,
            WHITE = 0xFFF5F5F5, DIM = 0xFF8C929B, BTN = 0xFF1A1D23, CHIP = 0xFF161A20,
            DANGER = 0xFFFF5A4E, TILE_TOP = 0xFF1C2028, TILE_BOT = 0xFF0C0E12;

    static final int CITY_DRIVE = 1, CITY_REGEN = 2;   // Eco + Media
    static final int HWY_DRIVE = 3, HWY_REGEN = 2;     // Sport + Media
    static final int REQ_LOCATION = 7;

    private final CarLink car = new CarLink();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences p;

    private FrameLayout root;
    private View home, modesPage, soundPage, netPage, updPage, current;

    // inicio
    private TextView clock, dateTxt, wifiChip, ipChip;
    private View tileMode, tileSound, tileNet, tileUpd;

    // manejo
    private LinearLayout cityCard, hwyCard;
    private TextView modeNow;

    // sonido
    private TextView lockNow;
    private LinearLayout lockList;

    // red
    private TextView netSsid, netIp, netSignal;
    private LinearLayout netList;
    private final ScanReceiver scanRx = new ScanReceiver(this);

    void onScanResults() {
        ui.post(() -> { if (current == netPage) refreshNetList(); });
    }
    private boolean scanRegistered;

    // actualizar
    private TextView updStatus;

    private final Runnable periodic = this::tickPeriodic;

    private void tickPeriodic() {
        updateClock();
        refreshNet();
        ui.postDelayed(periodic, 5000);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Prefs.get(this);

        root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.bg_carbon);

        home = homePage();
        modesPage = page("MANEJO", modesContent());
        soundPage = page("SONIDO DE BLOQUEO", soundContent());
        netPage = page("RED WI-FI", netContent());
        updPage = page("ACTUALIZAR", updContent());
        for (View v : new View[]{home, modesPage, soundPage, netPage, updPage}) {
            root.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            v.setVisibility(View.GONE);
        }
        setContentView(root);
        show(home);

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
        registerScan();
        refreshMode();
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
        else super.onBackPressed();
    }

    private void show(View v) {
        for (View x : new View[]{home, modesPage, soundPage, netPage, updPage}) {
            x.setVisibility(x == v ? View.VISIBLE : View.GONE);
        }
        current = v;
        if (v == soundPage) refreshLock();
        if (v == netPage) showNet();
        if (v == home) refreshAll();
    }

    private void refreshAll() {
        refreshMode();
        refreshLock();
        refreshNet();
        updateClock();
    }

    // ------------------------------------------------------------ inicio

    private View homePage() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(30), dp(22), dp(30), dp(22));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_VERTICAL);

        ImageView fox = new ImageView(this);
        fox.setImageResource(R.mipmap.ic_launcher);
        left.addView(fox, new LinearLayout.LayoutParams(dp(104), dp(104)));

        TextView title = new TextView(this);
        title.setText("EX2  LITE");
        title.setTextColor(WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        title.setLetterSpacing(0.12f);
        title.setPadding(0, dp(8), 0, 0);
        left.addView(title);

        TextView hi = new TextView(this);
        hi.setText("Hola, David");
        hi.setTextColor(DIM);
        hi.setTextSize(16);
        left.addView(hi);

        clock = new TextView(this);
        clock.setTextColor(WHITE);
        clock.setTextSize(58);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setPadding(0, dp(22), 0, 0);
        left.addView(clock);

        dateTxt = new TextView(this);
        dateTxt.setTextColor(PAPAYA);
        dateTxt.setTextSize(14);
        dateTxt.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        dateTxt.setLetterSpacing(0.06f);
        left.addView(dateTxt);

        wifiChip = chip();
        ipChip = chip();
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl.topMargin = dp(18);
        left.addView(wifiChip, cl);
        LinearLayout.LayoutParams cl2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl2.topMargin = dp(8);
        left.addView(ipChip, cl2);

        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.85f));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        tileMode = tile("MANEJO", R.drawable.ic_speed, true, () -> show(modesPage));
        tileSound = tile("SONIDO", R.drawable.ic_music, false, () -> show(soundPage));
        tileNet = tile("RED", R.drawable.ic_wifi, false, () -> show(netPage));
        tileUpd = tile("ACTUALIZAR", R.drawable.ic_refresh, false, () -> show(updPage));

        LinearLayout r1 = new LinearLayout(this);
        r1.addView(tileMode, tileLp(0));
        r1.addView(tileSound, tileLp(12));
        LinearLayout r2 = new LinearLayout(this);
        r2.addView(tileNet, tileLp(0));
        r2.addView(tileUpd, tileLp(12));
        grid.addView(r1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout.LayoutParams r2lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        r2lp.topMargin = dp(12);
        grid.addView(r2, r2lp);

        row.addView(grid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.6f));
        return row;
    }

    private LinearLayout.LayoutParams tileLp(int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = dp(leftDp);
        return lp;
    }

    private LinearLayout tile(String title, int icon, boolean accent, Runnable onTap) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(12), dp(14), dp(12));
        t.setBackground(ripple(accent ? gradient(PAPAYA_HI, PAPAYA_LO, 0xFF000000) : gradient(TILE_TOP, TILE_BOT, PANEL_LINE)));

        ImageView img = new ImageView(this);
        img.setImageResource(icon);
        t.addView(img, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView tt = new TextView(this);
        tt.setText(title);
        tt.setTextColor(WHITE);
        tt.setTextSize(22);
        tt.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        tt.setLetterSpacing(0.08f);
        tt.setGravity(Gravity.CENTER);
        tt.setPadding(0, dp(8), 0, 0);
        t.addView(tt);

        TextView sub = new TextView(this);
        sub.setTag("sub");
        sub.setTextColor(accent ? 0xFFFFE6CC : DIM);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setPadding(dp(8), dp(2), dp(8), 0);
        t.addView(sub, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        t.setOnClickListener(v -> onTap.run());
        return t;
    }

    private void setSub(View tile, String s) {
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
        String wTxt = ssid != null ? ssid : "Sin Wi-Fi";
        String iTxt = ip != null ? ip : "Sin IP";

        wifiChip.setText("Wi-Fi   " + wTxt);
        ipChip.setText("IP   " + iTxt);
        netSsid.setText(wTxt);
        netIp.setText("IP   " + iTxt);
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

        LinearLayout info = panel();
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(26), dp(22), dp(26), dp(22));

        ImageView wifi = new ImageView(this);
        wifi.setImageResource(R.drawable.ic_wifi);
        info.addView(wifi, new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView lbl = label("CONECTADA A");
        lbl.setPadding(0, dp(12), 0, 0);
        info.addView(lbl);

        netSsid = new TextView(this);
        netSsid.setTextColor(WHITE);
        netSsid.setTextSize(26);
        netSsid.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        netSsid.setSingleLine(true);
        netSsid.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(netSsid);

        netIp = new TextView(this);
        netIp.setTextColor(PAPAYA);
        netIp.setTextSize(22);
        netIp.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        netIp.setPadding(0, dp(10), 0, 0);
        info.addView(netIp);

        netSignal = new TextView(this);
        netSignal.setTextColor(DIM);
        netSignal.setTextSize(14);
        netSignal.setPadding(0, dp(6), 0, 0);
        info.addView(netSignal);

        info.addView(hint("Toca una red de la derecha para conectarte. Si pide clave, aparece un cuadro para escribirla."));

        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout list = panel();
        list.setPadding(dp(20), dp(18), dp(20), dp(18));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView h = label("REDES CERCANAS");
        head.addView(h, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button scan = small("BUSCAR");
        scan.setOnClickListener(v -> { NetInfo.scan(this); toast("Buscando redes..."); });
        head.addView(scan, smallLp(0));
        list.addView(head);

        ScrollView sv = new ScrollView(this);
        netList = new LinearLayout(this);
        netList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(netList);
        list.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.4f);
        rl.leftMargin = dp(16);
        row.addView(list, rl);
        return row;
    }

    private void refreshNetList() {
        if (netList == null) return;
        netList.removeAllViews();
        String cur = NetInfo.ssid(this);
        List<ScanResult> nets = NetInfo.nearby(this);
        if (nets.isEmpty()) {
            netList.addView(hint("No veo redes todavía. Toca BUSCAR o revisa que el Wi-Fi esté encendido."));
            return;
        }
        for (ScanResult r : nets) netList.addView(netRow(r, r.SSID.equals(cur)));
    }

    private View netRow(ScanResult r, boolean connected) {
        boolean secured = NetInfo.isSecured(r);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(ripple(rounded(connected ? 0xFF1B1408 : BTN, connected ? PAPAYA : PANEL_LINE, 1)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView n = new TextView(this);
        n.setText(r.SSID);
        n.setTextColor(WHITE);
        n.setTextSize(17);
        n.setSingleLine(true);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView s = new TextView(this);
        String sub = (connected ? "Conectada  ·  " : "") + (secured ? "Con clave" : "Abierta") + "  ·  " + r.level + " dBm";
        s.setText(sub);
        s.setTextColor(connected ? PAPAYA : DIM);
        s.setTextSize(12);
        info.addView(n);
        info.addView(s);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (secured) {
            ImageView lock = new ImageView(this);
            lock.setImageResource(R.drawable.ic_lock);
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

    // ------------------------------------------------------------ manejo

    private View modesContent() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);

        modeNow = new TextView(this);
        modeNow.setTextColor(PAPAYA);
        modeNow.setTextSize(16);
        modeNow.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        modeNow.setLetterSpacing(0.1f);
        modeNow.setPadding(0, 0, 0, dp(12));
        c.addView(modeNow);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        cityCard = modeCard("CIUDAD", "Eco · Regeneración media", R.drawable.ic_city,
                () -> applyMode(CITY_DRIVE, CITY_REGEN));
        hwyCard = modeCard("AUTOPISTA", "Sport · Regeneración media", R.drawable.ic_highway,
                () -> applyMode(HWY_DRIVE, HWY_REGEN));
        row.addView(cityCard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp2.leftMargin = dp(16);
        row.addView(hwyCard, lp2);
        c.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        Switch sw = new Switch(this);
        sw.setText("Recordar al encender el carro");
        sw.setTextColor(WHITE);
        sw.setTextSize(17);
        sw.setChecked(p.getBoolean(Prefs.REMEMBER, true));
        sw.setPadding(dp(4), dp(18), 0, dp(4));
        sw.setOnCheckedChangeListener((v, on) -> {
            p.edit().putBoolean(Prefs.REMEMBER, on).apply();
            if (on) KeeperService.start(this);
        });
        c.addView(sw);

        c.addView(hint("El modo se guarda cada vez que lo cambias, en la pantalla o en el volante. "
                + "Al encender el carro (en P) se vuelve a poner."));
        return c;
    }

    private LinearLayout modeCard(String title, String sub, int icon, Runnable onPick) {
        LinearLayout card = panel();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setOnClickListener(v -> onPick.run());

        ImageView img = new ImageView(this);
        img.setImageResource(icon);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(img, new LinearLayout.LayoutParams(dp(110), dp(110)));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(WHITE);
        t.setTextSize(26);
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
        if (cityCard != null) {
            markCard(cityCard, city);
            markCard(hwyCard, hwy);
        }
        String label = city ? "MODO  CIUDAD" : hwy ? "MODO  AUTOPISTA" : "MODO  PERSONALIZADO";
        if (modeNow != null) modeNow.setText(label);
        if (tileMode != null) setSub(tileMode, city ? "Ciudad · Eco" : hwy ? "Autopista · Sport" : "Personalizado");
    }

    private void markCard(LinearLayout card, boolean on) {
        card.setBackground(rounded(on ? 0xFF1B1408 : PANEL, on ? PAPAYA : PANEL_LINE, on ? 3 : 1));
        View tag = card.findViewWithTag("tag");
        if (tag != null) tag.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
    }

    // ------------------------------------------------------------ sonido

    private View soundContent() {
        LinearLayout c = panel();
        c.setPadding(dp(22), dp(18), dp(22), dp(18));

        lockNow = new TextView(this);
        lockNow.setTextColor(PAPAYA);
        lockNow.setTextSize(17);
        lockNow.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        lockNow.setPadding(0, 0, 0, dp(10));
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
        if (lockNow != null) lockNow.setText(name == null ? "Ahora: el del carro" : "Ahora: " + name);
        if (tileSound != null) setSub(tileSound, name == null ? "Del carro" : name.replaceAll("(?i)\\.wav$", ""));
        if (lockList == null) return;

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

    // ------------------------------------------------------------ actualizar

    private View updContent() {
        LinearLayout c = panel();
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(30), dp(24), dp(30), dp(24));

        ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.ic_refresh);
        c.addView(img, new LinearLayout.LayoutParams(dp(80), dp(80)));

        TextView v = new TextView(this);
        v.setText("Versión instalada  " + Updater.installedCode(this));
        v.setTextColor(WHITE);
        v.setTextSize(22);
        v.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(14), 0, 0);
        c.addView(v);

        updStatus = new TextView(this);
        updStatus.setText("Revisa si hay una versión nueva en el repositorio.");
        updStatus.setTextColor(DIM);
        updStatus.setTextSize(14);
        updStatus.setGravity(Gravity.CENTER);
        updStatus.setPadding(0, dp(10), 0, dp(18));
        c.addView(updStatus);

        Button upd = outline("BUSCAR ACTUALIZACIONES");
        upd.setOnClickListener(x -> {
            updStatus.setText("Revisando...");
            new Thread(() -> {
                Updater.Result r = Updater.check(this, true);
                ui.post(() -> { updStatus.setText(r.message); toast(r.message); });
            }).start();
        });
        c.addView(upd, new LinearLayout.LayoutParams(dp(360), dp(58)));
        return c;
    }

    // ------------------------------------------------------------ páginas y helpers

    private View page(String title, View content) {
        LinearLayout pg = new LinearLayout(this);
        pg.setOrientation(LinearLayout.VERTICAL);
        pg.setPadding(dp(28), dp(20), dp(28), dp(20));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setPadding(dp(12), dp(12), dp(12), dp(12));
        back.setBackground(ripple(rounded(PANEL, PANEL_LINE, 1)));
        back.setOnClickListener(v -> show(home));
        top.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));

        View bar = new View(this);
        bar.setBackgroundColor(PAPAYA);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(dp(6), dp(34));
        bl.leftMargin = dp(18);
        top.addView(bar, bl);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(WHITE);
        t.setTextSize(26);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        t.setLetterSpacing(0.1f);
        t.setPadding(dp(14), 0, 0, 0);
        top.addView(t);
        pg.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        cl.topMargin = dp(18);
        pg.addView(content, cl);
        return pg;
    }

    private LinearLayout panel() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(PANEL, PANEL_LINE, 1));
        return c;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(DIM);
        t.setTextSize(12);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        t.setLetterSpacing(0.14f);
        return t;
    }

    private TextView chip() {
        TextView t = new TextView(this);
        t.setTextColor(WHITE);
        t.setTextSize(13);
        t.setSingleLine(true);
        t.setPadding(dp(14), dp(7), dp(14), dp(7));
        t.setBackground(rounded(CHIP, PANEL_LINE, 1));
        return t;
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
        Button b = baseButton(s, 15);
        b.setBackground(ripple(rounded(BTN, PAPAYA, 1)));
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

    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, dp(52), 1); }

    private LinearLayout.LayoutParams weightGap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1);
        lp.leftMargin = dp(8);
        return lp;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(14));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private GradientDrawable gradient(int top, int bottom, int stroke) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(dp(20));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private RippleDrawable ripple(GradientDrawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x44FF8000), content, null);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}

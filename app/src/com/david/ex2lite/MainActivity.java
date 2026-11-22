package com.david.ex2lite;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
    static final int BLACK = 0xFF07080A, PAPAYA = 0xFFFF8000, PAPAYA_HI = 0xFFFFA03A, PAPAYA_LO = 0xFFE86A00,
            WHITE = 0xFFF5F5F5, DIM = 0xFF9AA1AB, SOFT = 0xFFD7DBE0, DANGER = 0xFFFF5A4E,
            GLASS_TOP = 0xE6222833, GLASS_BOT = 0xE60E1116, GLASS_LINE = 0x33FFFFFF, FIELD = 0x40FFFFFF;

    static final int CITY_DRIVE = 1, CITY_REGEN = 2;   // Eco + Media
    static final int HWY_DRIVE = 3, HWY_REGEN = 2;     // Sport + Media
    static final int REQ_LOCATION = 7;

    private final CarLink car = new CarLink();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences p;
    private Typeface fXB, fSB, fLT;

    private FrameLayout root;
    private View home, modesPage, soundPage, netPage, updPage, current;

    // inicio
    private TextView clock, dateTxt, wifiChip, ipChip, modeName, modeSub;
    private ImageView modeArt;
    private View tileSound, tileNet, tileUpd;

    // manejo
    private FrameLayout cityCard, hwyCard;

    // sonido
    private TextView lockNow;
    private LinearLayout lockList;

    // red
    private TextView netSsid, netIp, netSignal;
    private LinearLayout netList;
    private final ScanReceiver scanRx = new ScanReceiver(this);
    private boolean scanRegistered;

    // actualizar
    private TextView updStatus;

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
        fXB = font(R.font.mont_xbold_italic, Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        fSB = font(R.font.mont_semibold, Typeface.create("sans-serif-medium", Typeface.BOLD));
        fLT = font(R.font.mont_light, Typeface.create("sans-serif-light", Typeface.NORMAL));

        root = new FrameLayout(this);
        root.setBackgroundColor(BLACK);
        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImageResource(R.drawable.bg_home);
        root.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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
        row.setPadding(dp(32), dp(24), dp(32), dp(24));

        // ---- columna izquierda: marca, reloj, red
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView fox = new ImageView(this);
        fox.setImageResource(R.drawable.fox_hero);
        brand.addView(fox, new LinearLayout.LayoutParams(dp(92), dp(92)));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(6), 0, 0, 0);
        TextView t1 = tv("EX2 LITE", 26, WHITE, fXB);
        t1.setLetterSpacing(0.08f);
        TextView t2 = tv("GEELY EX2", 11, PAPAYA, fSB);
        t2.setLetterSpacing(0.32f);
        names.addView(t1);
        names.addView(t2);
        brand.addView(names);
        left.addView(brand);

        left.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));

        clock = tv("--:--", 66, WHITE, fLT);
        clock.setIncludeFontPadding(false);
        left.addView(clock);
        dateTxt = tv("", 13, PAPAYA, fSB);
        dateTxt.setLetterSpacing(0.18f);
        dateTxt.setPadding(dp(4), dp(4), 0, 0);
        left.addView(dateTxt);

        left.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 0.6f));

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

        View big = modeHero();
        right.addView(big, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.12f));

        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        tileSound = wideTile("SONIDO", R.drawable.ic_music, () -> show(soundPage));
        tileNet = wideTile("RED", R.drawable.ic_wifi, () -> show(netPage));
        tileUpd = wideTile("ACTUALIZAR", R.drawable.ic_refresh, () -> show(updPage));
        stack.addView(tileSound, stackLp(0));
        stack.addView(tileNet, stackLp(14));
        stack.addView(tileUpd, stackLp(14));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        slp.leftMargin = dp(16);
        right.addView(stack, slp);

        row.addView(right, rlp);
        return row;
    }

    /** Tarjeta grande con la ilustración del modo activo. */
    private View modeHero() {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(ripple(rounded(0xFF111318, GLASS_LINE, 1, 26)));
        f.setOutlineProvider(new RoundedOutline(dp(26)));
        f.setClipToOutline(true);
        f.setOnClickListener(v -> show(modesPage));

        modeArt = new ImageView(this);
        modeArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        f.addView(modeArt, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        f.addView(shade(), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView badge = badge(R.drawable.ic_speed, 52);
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.END);
        bl.setMargins(0, dp(18), dp(18), 0);
        f.addView(badge, bl);

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(24), dp(20), dp(24), dp(22));
        TextView lbl = tv("MODO ACTUAL", 11, PAPAYA, fSB);
        lbl.setLetterSpacing(0.3f);
        txt.addView(lbl);
        modeName = tv("", 34, WHITE, fXB);
        modeName.setMaxLines(1);
        modeName.setEllipsize(TextUtils.TruncateAt.END);
        txt.addView(modeName);
        modeSub = tv("", 14, SOFT, null);
        modeSub.setPadding(0, dp(2), 0, dp(12));
        txt.addView(modeSub);
        TextView pill = tv("TOCA PARA CAMBIAR  ›", 12, 0xFF1A0F05, fSB);
        pill.setLetterSpacing(0.12f);
        pill.setPadding(dp(16), dp(8), dp(16), dp(8));
        pill.setBackground(papayaPill());
        txt.addView(pill, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        String wTxt = ssid != null ? ssid : "Sin Wi-Fi";
        String iTxt = ip != null ? ip : "Sin IP";

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
        TextView ipl = tv("IP", 12, 0xFF1A0F05, fSB);
        ipl.setPadding(dp(8), dp(2), dp(8), dp(2));
        ipl.setBackground(papayaPill());
        ipRow.addView(ipl);
        netIp = tv("", 22, PAPAYA, fSB);
        netIp.setPadding(dp(10), 0, 0, 0);
        ipRow.addView(netIp);
        info.addView(ipRow);

        netSignal = tv("", 14, DIM, null);
        netSignal.setPadding(0, dp(8), 0, 0);
        info.addView(netSignal);

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
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackground(ripple(rounded(connected ? 0x33FF8000 : FIELD, connected ? PAPAYA : 0, connected ? 2 : 0, 16)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView n = tv(r.SSID, 17, WHITE, fSB);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        String sub = (connected ? "Conectada  ·  " : "") + (secured ? "Con clave" : "Abierta") + "  ·  " + r.level + " dBm";
        TextView s = tv(sub, 12, connected ? PAPAYA : DIM, null);
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

    // ------------------------------------------------------------ manejo

    private View modesContent() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        cityCard = modeCard("CIUDAD", "Eco · Regeneración media", R.drawable.card_city,
                () -> applyMode(CITY_DRIVE, CITY_REGEN));
        hwyCard = modeCard("AUTOPISTA", "Sport · Regeneración media", R.drawable.card_highway,
                () -> applyMode(HWY_DRIVE, HWY_REGEN));
        row.addView(cityCard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp2.leftMargin = dp(18);
        row.addView(hwyCard, lp2);
        c.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(8), dp(20), dp(8));
        bar.setBackground(glass(18));
        Switch sw = new Switch(this);
        sw.setText("Recordar mi modo al encender el carro");
        sw.setTextColor(WHITE);
        sw.setTextSize(16);
        sw.setTypeface(fSB);
        sw.setChecked(p.getBoolean(Prefs.REMEMBER, true));
        sw.setOnCheckedChangeListener((v, on) -> {
            p.edit().putBoolean(Prefs.REMEMBER, on).apply();
            if (on) KeeperService.start(this);
        });
        bar.addView(sw, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(16);
        c.addView(bar, blp);

        c.addView(hint("El modo se guarda cada vez que lo cambias, en la pantalla o en el volante. "
                + "Al encender el carro (en P) se vuelve a poner."));
        return c;
    }

    private FrameLayout modeCard(String title, String sub, int art, Runnable onPick) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(ripple(rounded(0xFF111318, GLASS_LINE, 1, 26)));
        f.setOutlineProvider(new RoundedOutline(dp(26)));
        f.setClipToOutline(true);
        f.setOnClickListener(v -> onPick.run());

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageResource(art);
        img.setTag("art");
        f.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        f.addView(shade(), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(26), dp(20), dp(26), dp(24));
        TextView t = tv(title, 32, WHITE, fXB);
        t.setLetterSpacing(0.04f);
        txt.addView(t);
        txt.addView(tv(sub, 15, SOFT, null));
        f.addView(txt, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        TextView tag = tv("ACTIVO", 12, 0xFF1A0F05, fSB);
        tag.setLetterSpacing(0.2f);
        tag.setPadding(dp(14), dp(6), dp(14), dp(6));
        tag.setBackground(papayaPill());
        tag.setTag("tag");
        tag.setVisibility(View.INVISIBLE);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        tl.setMargins(dp(20), dp(20), 0, 0);
        f.addView(tag, tl);

        View border = new View(this);
        border.setBackground(rounded(0, PAPAYA, 3, 26));
        border.setTag("border");
        border.setVisibility(View.INVISIBLE);
        f.addView(border, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return f;
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
        if (modeName != null) {
            modeName.setText(city ? "CIUDAD" : hwy ? "AUTOPISTA" : "PERSONALIZADO");
            modeSub.setText(city ? "Eco · Regeneración media" : hwy ? "Sport · Regeneración media" : "Configuración propia");
            modeArt.setImageResource(city ? R.drawable.card_city : R.drawable.card_highway);
        }
    }

    private void markCard(FrameLayout card, boolean on) {
        View tag = card.findViewWithTag("tag");
        View border = card.findViewWithTag("border");
        View art = card.findViewWithTag("art");
        if (tag != null) tag.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        if (border != null) border.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        if (art != null) art.setAlpha(on ? 1f : 0.62f);
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
        lockNow = tv("", 20, PAPAYA, fXB);
        lockNow.setSingleLine(true);
        lockNow.setEllipsize(TextUtils.TruncateAt.END);
        lblCol.addView(lockNow);
        top.addView(lblCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ImageView wave = new ImageView(this);
        wave.setImageResource(R.drawable.wave);
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
        setSub(tileSound, name == null ? "Sonido del carro" : name.replaceAll("(?i)\\.wav$", ""));
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
        play.setOnClickListener(v -> { String e = LockSound.preview(f); if (e != null) toast(e); });
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
        upd.setTextSize(15);
        upd.setOnClickListener(x -> {
            updStatus.setText("Revisando...");
            new Thread(() -> {
                Updater.Result r = Updater.check(this, true);
                ui.post(() -> { updStatus.setText(r.message); toast(r.message); });
            }).start();
        });
        c.addView(upd, new LinearLayout.LayoutParams(dp(380), dp(56)));
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
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (f != null) t.setTypeface(f);
        return t;
    }

    /** Insignia circular naranja con el icono en blanco. */
    private ImageView badge(int icon, int sizeDp) {
        ImageView b = new ImageView(this);
        b.setImageResource(icon);
        b.setColorFilter(0xFFFFFFFF);
        int pad = dp(sizeDp) / 4;
        b.setPadding(pad, pad, pad, pad);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{PAPAYA_HI, PAPAYA_LO});
        g.setShape(GradientDrawable.OVAL);
        b.setBackground(g);
        return b;
    }

    /** Insignia grande con anillos de resplandor alrededor. */
    private View ringBadge(int icon) {
        FrameLayout f = new FrameLayout(this);
        f.addView(ringView(0x22FF8000), centered(150));
        f.addView(ringView(0x44FF8000), centered(122));
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
        c.addView(i, new LinearLayout.LayoutParams(dp(20), dp(20)));
        text.setTextColor(WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
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
        Button b = baseButton(s, 12, 0xFF1A0F05);
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
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
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

    private GradientDrawable papayaPill() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{PAPAYA_HI, PAPAYA_LO});
        g.setCornerRadius(dp(60));
        return g;
    }

    private GradientDrawable glass(int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GLASS_TOP, GLASS_BOT});
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), GLASS_LINE);
        return g;
    }

    private GradientDrawable glassOval() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GLASS_TOP, GLASS_BOT});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(dp(1), GLASS_LINE);
        return g;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private RippleDrawable ripple(GradientDrawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x44FF8000), content, null);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}

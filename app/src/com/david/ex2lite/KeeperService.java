package com.david.ex2lite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.car.hardware.CarPropertyValue;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * Servicio que recuerda el modo de manejo y el sonido de bloqueo.
 *
 * Por defecto NO se queda corriendo: arranca al encender/despertar el carro, repone
 * lo guardado durante ~45 s y se apaga solo. Solo si el usuario activa "Vigilar
 * cambios en segundo plano" se queda vivo para guardar cambios hechos fuera de la app.
 *
 * - Guardar: cada vez que el carro cambia modo o regeneración (desde la pantalla,
 *   el volante o la app), se guarda al instante. Así no depende de "apagar bien".
 * - Restaurar: el carro vuelve a valores de fábrica al prender y al despertar.
 *   Durante ~45 s tras eso repone lo guardado, solo con el carro en P.
 *   Mientras dura esa ventana NO guarda, para no sobrescribir lo bueno con los
 *   valores de fábrica.
 * - Sonido: en cada revisión verifica que el sonido elegido siga puesto.
 */
public class KeeperService extends Service {
    static final String T = "EX2Lite";
    static final long WINDOW_MS = 45_000;
    static final long TICK_MS = 3_000;
    static final long SLEEP_GAP_MS = 5_000;

    static final int GEAR = 289408001;   // 1=N 2=R 4=P 8=D
    static final int GEAR_PARK = 4;

    private HandlerThread thread;
    private Handler h;
    private final CarLink car = new CarLink();
    private volatile long windowEnd = 0;
    private long lastSleep = -1;
    private volatile boolean watching = false;

    public static void start(Context c) {
        try { c.startForegroundService(new Intent(c, KeeperService.class)); }
        catch (Throwable t) { Log.w(T, "start", t); }
    }

    @Override public void onCreate() {
        super.onCreate();
        startForeground(1, notif());
        thread = new HandlerThread("ex2lite");
        thread.start();
        h = new Handler(thread.getLooper());
        h.post(this::init);
    }

    @Override public int onStartCommand(Intent i, int flags, int id) {
        watching = Prefs.get(this).getBoolean(Prefs.WATCH, false);
        if (h != null) h.post(this::openWindow);
        return watching ? START_STICKY : START_NOT_STICKY;
    }

    private void init() {
        for (int i = 0; i < 10 && !car.connect(this); i++) SystemClock.sleep(1500);
        if (!car.ready()) Log.w(T, "car service not ready");
        openWindow();

        if (watching) {
            car.listenChanges(new CarLink.Changes(this::onCarChanged));
            boolean power = car.listenPower(state -> {
                Log.i(T, "power state " + state);
                if (state == CarLink.SUSPEND_EXIT) h.post(this::openWindow);
            });
            Log.i(T, "power listener: " + power);
        }
        lastSleep = sleepMs();
        h.post(this::tick);
    }

    private void openWindow() {
        windowEnd = SystemClock.elapsedRealtime() + WINDOW_MS;
        Log.i(T, "restore window open");
    }

    /** Tiempo acumulado en suspensión desde el boot. Solo crece mientras duerme. */
    private static long sleepMs() { return SystemClock.elapsedRealtime() - SystemClock.uptimeMillis(); }

    /** Guarda el cambio del carro, salvo durante la ventana de restauración. */
    private void onCarChanged(CarPropertyValue value) {
        if (SystemClock.elapsedRealtime() < windowEnd) return;
        try {
            int id = value.getPropertyId();
            int raw = (Integer) value.getValue();
            SharedPreferences.Editor e = Prefs.get(this).edit();
            if (id == CarLink.DRIVE) {
                int lvl = raw - CarLink.DRIVE;
                if (lvl >= 1 && lvl <= 3) e.putInt(Prefs.DRIVE, lvl);
            } else if (id == CarLink.REGEN) {
                int lvl = raw - CarLink.REGEN;
                if (lvl >= 1 && lvl <= 3) e.putInt(Prefs.REGEN, lvl);
            }
            e.apply();
        } catch (Throwable t) {
            Log.w(T, "onCarChanged", t);
        }
    }

    private void tick() {
        // Respaldo por si el carro no avisa el despertar: si dormimos entre revisiones, abrimos ventana.
        long s = sleepMs();
        if (lastSleep >= 0 && s - lastSleep >= SLEEP_GAP_MS) openWindow();
        lastSleep = s;

        if (SystemClock.elapsedRealtime() < windowEnd) restore();
        LockSound.enforce(this);
        if (!watching && SystemClock.elapsedRealtime() >= windowEnd + TICK_MS) {
            Log.i(T, "listo; me apago");
            stopSelf();
            return;
        }
        h.postDelayed(this::tick, TICK_MS);
    }

    private void restore() {
        SharedPreferences p = Prefs.get(this);
        if (!p.getBoolean(Prefs.REMEMBER, true)) return;
        int wantD = p.getInt(Prefs.DRIVE, 0), wantR = p.getInt(Prefs.REGEN, 0);
        if (wantD == 0 && wantR == 0) return;
        if (!car.ready() && !car.connect(this)) return;
        if (!parkedOrUnknown()) return;

        if (wantD != 0 && car.drive() != wantD) Log.i(T, "drive -> " + wantD + " ok=" + car.setDrive(wantD));
        if (wantR != 0 && car.regen() != wantR) Log.i(T, "regen -> " + wantR + " ok=" + car.setRegen(wantR));
    }

    private boolean parkedOrUnknown() {
        Integer g = car.gear();
        return g == null || g == GEAR_PARK;
    }

    private Notification notif() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            NotificationChannel ch = new NotificationChannel("ex2lite", "EX2 Lite", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        return new Notification.Builder(this, "ex2lite")
                .setContentTitle("EX2 Lite")
                .setContentText(watching ? "Vigilando tu modo y tu sonido" : "Aplicando tu modo y tu sonido")
                .setSmallIcon(android.R.drawable.ic_menu_preferences)
                .build();
    }

    @Override public void onDestroy() {
        if (thread != null) thread.quitSafely();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}

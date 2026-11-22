package com.david.ex2lite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * Servicio de corta vida: al encender o despertar el carro verifica durante ~45 s que el
 * sonido de bloqueo elegido siga puesto (si el carro o una actualización lo cambió, lo repone)
 * y se apaga solo. Con la app cerrada no queda nada corriendo.
 */
public class KeeperService extends Service {
    static final String T = "EX2Lite";
    static final long WINDOW_MS = 45_000;
    static final long TICK_MS = 3_000;

    private HandlerThread thread;
    private Handler h;
    private volatile long windowEnd = 0;

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
        openWindow();
        h.post(this::tick);
    }

    @Override public int onStartCommand(Intent i, int flags, int id) {
        if (h != null) h.post(this::openWindow);
        return START_NOT_STICKY;
    }

    private void openWindow() {
        windowEnd = SystemClock.elapsedRealtime() + WINDOW_MS;
    }

    private void tick() {
        LockSound.enforce(this);
        if (SystemClock.elapsedRealtime() >= windowEnd) {
            Log.i(T, "listo; me apago");
            stopSelf();
            return;
        }
        h.postDelayed(this::tick, TICK_MS);
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
                .setContentText("Comprobando tu sonido de bloqueo")
                .setSmallIcon(android.R.drawable.ic_menu_preferences)
                .build();
    }

    @Override public void onDestroy() {
        if (thread != null) thread.quitSafely();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}

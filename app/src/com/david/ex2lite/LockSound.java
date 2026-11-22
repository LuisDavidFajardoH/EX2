package com.david.ex2lite;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Sonido de bloqueo.
 *
 * El CarService del EX2 reproduce, al cerrar el carro con la llave, el archivo
 * que indica Settings.System "lock_car_audio_path" (y si está vacío, el de
 * fábrica en /vendor/etc/carlock/). Como esta app corre con uid system, igual
 * que CarService, basta con copiar el .wav a nuestra carpeta privada y apuntar
 * el ajuste a esa copia. No hace falta root ni tocar /system.
 * (Mecanismo documentado por SuspiciousRepair/drive_assist.)
 */
public final class LockSound {
    static final String T = "EX2Lite";
    public static final String SETTING = "lock_car_audio_path";
    public static final String OEM_DEFAULT = "/vendor/etc/carlock/yuedongyinghe.wav";
    public static final long MAX_BYTES = 1024 * 1024;
    /** Carpeta que la app revisa en la memoria interna y en la raíz de cada USB. */
    public static final String FOLDER = "SonidosBloqueo";
    private static final String PREFIX = "lock_";

    private LockSound() {}

    // ---------------------------------------------------------------- buscar

    /** Todos los .wav en /sdcard/SonidosBloqueo y en cada USB (raíz y SonidosBloqueo/). */
    public static List<File> findCandidates() {
        List<File> dirs = new ArrayList<>();
        dirs.add(new File("/sdcard/" + FOLDER));
        File[] vols = new File("/storage").listFiles();
        if (vols != null) {
            for (File v : vols) {
                String n = v.getName();
                if (n.equals("emulated") || n.equals("self")) continue;
                dirs.add(v);
                dirs.add(new File(v, FOLDER));
                dirs.add(new File(v, "锁车音效")); // la carpeta del método chino, por si ya la tienes
            }
        }
        List<File> out = new ArrayList<>();
        for (File d : dirs) {
            File[] fs = d.listFiles();
            if (fs == null) continue;
            Arrays.sort(fs);
            for (File f : fs) {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".wav")) out.add(f);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- validar

    /** null si sirve; si no, el motivo en español. */
    public static String check(File f) {
        long size = f.length();
        if (size <= 0) return "El archivo está vacío";
        if (size > MAX_BYTES) return "Pesa más de 1 MB (" + (size / 1024) + " KB). Recórtalo.";
        byte[] h = new byte[12];
        try (InputStream in = new FileInputStream(f)) {
            if (in.read(h) < 12) return "No es un WAV válido";
        } catch (Exception e) {
            return "No se pudo leer el archivo";
        }
        boolean riff = h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F';
        boolean wave = h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E';
        return (riff && wave) ? null : "No es un WAV real (¿es un MP3 renombrado?)";
    }

    // ---------------------------------------------------------------- aplicar

    public static String current(Context c) {
        try { return Settings.System.getString(c.getContentResolver(), SETTING); }
        catch (Throwable t) { return null; }
    }

    static boolean isOurs(Context c, String path) {
        return path != null && path.startsWith(c.getFilesDir().getAbsolutePath() + "/" + PREFIX);
    }

    /** Copia el .wav a la carpeta de la app y lo deja como sonido de bloqueo. null = OK. */
    public static String apply(Context c, File src) {
        String why = check(src);
        if (why != null) return why;

        File dir = c.getFilesDir();
        File dst = new File(dir, PREFIX + System.currentTimeMillis() + ".wav");
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            Log.w(T, "copy", e);
            dst.delete();
            return "No se pudo copiar el archivo";
        }
        dst.setReadable(true, false);

        SharedPreferences p = Prefs.get(c);
        String cur = current(c);
        // Guardamos el sonido del carro solo la primera vez (nunca uno nuestro).
        if (!isOurs(c, cur) && !p.contains(Prefs.LOCK_ORIG)) {
            p.edit().putString(Prefs.LOCK_ORIG, cur == null ? "" : cur).apply();
        }
        try {
            if (!Settings.System.putString(c.getContentResolver(), SETTING, dst.getAbsolutePath())) {
                dst.delete();
                return "El sistema no aceptó el cambio";
            }
        } catch (Throwable t) {
            Log.w(T, "putString", t);
            dst.delete();
            return "Sin permiso para cambiar el ajuste (¿la app quedó como app de sistema?)";
        }
        p.edit().putString(Prefs.LOCK_NAME, src.getName()).apply();
        cleanup(c, dst);
        return null;
    }

    /** Vuelve al sonido que tenía el carro antes de usar la app. */
    public static boolean restore(Context c) {
        SharedPreferences p = Prefs.get(c);
        String orig = p.getString(Prefs.LOCK_ORIG, "");
        if (orig == null || orig.isEmpty() || isOurs(c, orig)) orig = OEM_DEFAULT;
        boolean ok;
        try { ok = Settings.System.putString(c.getContentResolver(), SETTING, orig); }
        catch (Throwable t) { ok = false; }
        if (ok) {
            p.edit().remove(Prefs.LOCK_NAME).remove(Prefs.LOCK_ORIG).apply();
            cleanup(c, null);
        }
        return ok;
    }

    /** Borra nuestras copias viejas; deja solo {@code keep}. */
    private static void cleanup(Context c, File keep) {
        File[] fs = c.getFilesDir().listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(PREFIX) && (keep == null || !f.equals(keep))) f.delete();
        }
    }

    /** Ruta de nuestra copia activa, o null si no hay sonido elegido. */
    public static File ours(Context c) {
        File[] fs = c.getFilesDir().listFiles();
        if (fs == null) return null;
        for (File f : fs) {
            if (f.getName().startsWith(PREFIX) && f.isFile()) return f;
        }
        return null;
    }

    /**
     * Garantiza que el sonido elegido siga puesto. Si el carro (o una actualización)
     * lo cambió, lo vuelve a poner. Se llama en cada revisión del servicio.
     * Devuelve true si tuvo que volver a aplicarlo.
     */
    public static boolean enforce(Context c) {
        if (!Prefs.get(c).contains(Prefs.LOCK_NAME)) return false;
        File f = ours(c);
        if (f == null) return false;
        String want = f.getAbsolutePath();
        if (want.equals(current(c))) return false;
        try {
            if (Settings.System.putString(c.getContentResolver(), SETTING, want)) {
                Log.i(T, "lock sound re-applied");
                return true;
            }
        } catch (Throwable t) { Log.w(T, "enforce", t); }
        return false;
    }

    /**
     * Copia un .wav de la USB a la carpeta del carro (/sdcard/SonidosBloqueo),
     * para elegirlo después sin la USB. null = OK; si no, el motivo.
     */
    public static String copyToCar(File src) {
        String why = check(src);
        if (why != null) return why;
        File dir = new File("/sdcard/" + FOLDER);
        if (!dir.isDirectory() && !dir.mkdirs()) return "No se pudo crear la carpeta " + FOLDER;
        File dst = new File(dir, src.getName());
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            Log.w(T, "copyToCar", e);
            dst.delete();
            return "No se pudo copiar a la central";
        }
        dst.setReadable(true, false);
        return null;
    }

    // ---------------------------------------------------------------- probar

    private static MediaPlayer player;

    /** Resultado asíncrono de la prueba: null si sonó bien, o el motivo del fallo. */
    public interface Done { void on(String problem); }

    public static void preview(android.content.Context c, File f, Done done) {
        stopPreview();
        final android.media.AudioManager am =
                (android.media.AudioManager) c.getSystemService(android.content.Context.AUDIO_SERVICE);
        try {
            int vol = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            if (vol == 0) { done.on("El volumen de medios está en 0. Súbelo y prueba otra vez."); return; }

            focus = new android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs()).build();
            am.requestAudioFocus(focus);

            player = new MediaPlayer();
            player.setAudioAttributes(attrs());
            player.setVolume(1f, 1f);
            player.setDataSource(f.getAbsolutePath());
            player.setOnCompletionListener(mp -> { stopPreview(); abandon(am); done.on(null); });
            player.setOnErrorListener((mp, what, extra) -> {
                stopPreview(); abandon(am);
                done.on("No se pudo reproducir (error " + what + "/" + extra + "). Revisa que sea un .wav normal.");
                return true;
            });
            player.setOnPreparedListener(mp -> {
                mp.start();
                done.on("Sonando... (volumen " + vol + ")");
            });
            player.prepareAsync();
        } catch (Exception e) {
            stopPreview(); abandon(am);
            done.on("No se pudo reproducir: " + e.getMessage());
        }
    }

    private static android.media.AudioAttributes attrs() {
        return new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build();
    }

    private static android.media.AudioFocusRequest focus;

    private static void abandon(android.media.AudioManager am) {
        if (focus != null) { try { am.abandonAudioFocusRequest(focus); } catch (Throwable ignored) {} focus = null; }
    }

    public static void stopPreview() {
        if (player != null) {
            try { player.release(); } catch (Throwable ignored) {}
            player = null;
        }
    }
}

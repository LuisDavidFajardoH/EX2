package com.david.ex2lite;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Actualización en línea.
 *
 * Lee un version.json de un repo (GitHub). Si trae un versionCode mayor que el
 * instalado, descarga el APK, verifica el SHA-256 y lo instala usando el adbd
 * local como root (pm install -r). Así se salta el instalador normal, que
 * rechazaría una app de sistema.
 *
 * Formato esperado de version.json:
 * {
 *   "versionCode": 2,
 *   "versionName": "1.1",
 *   "apkUrl": "https://.../ex2lite.apk",
 *   "sha256": "abcd...",          // en minúsculas
 *   "notes": "qué cambió"         // opcional
 * }
 */
public final class Updater {
    static final String T = "EX2Lite";

    /** CAMBIA esto por la URL cruda de tu version.json en GitHub. */
    public static final String MANIFEST_URL =
            "https://raw.githubusercontent.com/LuisDavidFajardoH/ex2/main/update/version.json";

    public static final class Result {
        public final boolean ok;
        public final String message;
        public final boolean updated;
        Result(boolean ok, boolean updated, String message) {
            this.ok = ok; this.updated = updated; this.message = message;
        }
    }

    private Updater() {}

    public static int installedCode(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return (int) pi.getLongVersionCode();
        } catch (Throwable t) { return -1; }
    }

    /** Hace todo el flujo. Pensado para llamar desde un hilo, no en la UI. */
    public static Result check(Context c, boolean install) {
        try {
            String json = get(MANIFEST_URL, 15000);
            JSONObject m = new JSONObject(json);
            int remote = m.getInt("versionCode");
            int local = installedCode(c);
            if (remote <= local) {
                return new Result(true, false, "Ya tienes la última versión (" + local + ").");
            }
            String name = m.optString("versionName", String.valueOf(remote));
            if (!install) {
                return new Result(true, false, "Hay una actualización: " + name + ".");
            }
            String apkUrl = m.getString("apkUrl");
            String sha = m.getString("sha256").trim().toLowerCase(Locale.ROOT);

            File apk = new File(c.getCacheDir(), "update.apk");
            download(apkUrl, apk, 60000);

            String got = sha256(apk);
            if (!got.equals(sha)) {
                apk.delete();
                return new Result(false, false, "El archivo descargado no coincide (hash). No se instaló.");
            }
            if (!RootShell.available()) {
                apk.delete();
                return new Result(false, false, "No se pudo abrir el acceso root local para instalar.");
            }
            // Legible por el instalador y pm install como root.
            apk.setReadable(true, false);
            // El script corre en el adbd (no muere cuando el sistema reemplaza esta app):
            // instala, borra el APK y vuelve a abrir la app ya actualizada.
            String log = "/data/local/tmp/ex2up.txt";
            RootShell.run(10000, "rm -f " + log);
            RootShell.run(10000, "nohup sh -c 'pm install -r -d " + apk.getAbsolutePath() + " > " + log
                    + " 2>&1; rm -f " + apk.getAbsolutePath()
                    + "; sleep 1; am start -n com.david.ex2lite/.MainActivity' >/dev/null 2>&1 &");
            // Si la instalación sale bien, el sistema cierra esta app y se reabre sola.
            // Solo seguimos aquí si algo falló.
            String out = "";
            for (int i = 0; i < 60; i++) {
                Thread.sleep(1500);
                String r = RootShell.run(10000, "cat " + log);
                if (r != null && !r.trim().isEmpty()) { out = r; break; }
            }
            if (out.toLowerCase(Locale.ROOT).contains("success")) {
                return new Result(true, true, "Actualizado a " + name + ". Reiniciando...");
            }
            apk.delete();
            return new Result(false, false, "La instalación falló: " + out);
        } catch (Throwable t) {
            Log.w(T, "update", t);
            return new Result(false, false, "No se pudo revisar: " + t.getMessage());
        }
    }

    // ---------------------------------------------------------------- red

    private static HttpURLConnection open(String url, int timeout) throws Exception {
        HttpURLConnection cx = (HttpURLConnection) new URL(url).openConnection();
        cx.setConnectTimeout(timeout);
        cx.setReadTimeout(timeout);
        cx.setInstanceFollowRedirects(true);
        cx.setRequestProperty("User-Agent", "EX2Lite");
        cx.setUseCaches(false);
        cx.setRequestProperty("Cache-Control", "no-cache");
        return cx;
    }

    /** Agrega la hora actual a la URL para que GitHub no sirva una copia en caché. */
    private static String nocache(String url) {
        return url + (url.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
    }

    private static String get(String url, int timeout) throws Exception {
        HttpURLConnection cx = open(nocache(url), timeout);
        try (InputStream in = cx.getInputStream()) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        } finally { cx.disconnect(); }
    }

    private static void download(String url, File dst, int timeout) throws Exception {
        HttpURLConnection cx = open(nocache(url), timeout);
        try (InputStream in = cx.getInputStream(); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally { cx.disconnect(); }
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new java.io.FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

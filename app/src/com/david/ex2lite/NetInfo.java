package com.david.ex2lite;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Red Wi-Fi: lo que está conectado, la IP, redes cercanas y conexión a una de ellas. */
final class NetInfo {
    private NetInfo() {}

    static WifiManager wifi(Context c) {
        return (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    static boolean enabled(Context c) {
        WifiManager wm = wifi(c);
        return wm != null && wm.isWifiEnabled();
    }

    static boolean setEnabled(Context c, boolean on) {
        WifiManager wm = wifi(c);
        return wm != null && wm.setWifiEnabled(on);
    }

    /** Nombre de la red conectada, o null si no hay. */
    static String ssid(Context c) {
        WifiManager wm = wifi(c);
        if (wm == null || !wm.isWifiEnabled()) return null;
        WifiInfo wi = wm.getConnectionInfo();
        if (wi == null || wi.getNetworkId() == -1) return null;
        String s = wi.getSSID();
        if (s == null || s.equals("<unknown ssid>")) return "Wi-Fi conectada";
        return s.replace("\"", "");
    }

    /** Intensidad en dBm de la red conectada, o null. */
    static Integer rssi(Context c) {
        WifiManager wm = wifi(c);
        if (wm == null || !wm.isWifiEnabled()) return null;
        WifiInfo wi = wm.getConnectionInfo();
        return wi == null || wi.getNetworkId() == -1 ? null : wi.getRssi();
    }

    /** IPv4 del Wi-Fi (wlan*) o de cualquier interfaz activa; null si no hay. */
    static String ip(Context c) {
        String fallback = null;
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof Inet4Address)) continue;
                    String h = a.getHostAddress();
                    if (ni.getName().startsWith("wlan")) return h;
                    if (fallback == null) fallback = h;
                }
            }
        } catch (Exception ignored) {}
        if (fallback != null) return fallback;
        WifiManager wm = wifi(c);
        if (wm == null || !wm.isWifiEnabled()) return null;
        WifiInfo wi = wm.getConnectionInfo();
        int v = wi == null ? 0 : wi.getIpAddress();
        if (v == 0) return null;
        return (v & 0xff) + "." + ((v >> 8) & 0xff) + "." + ((v >> 16) & 0xff) + "." + ((v >> 24) & 0xff);
    }

    /** Pide un escaneo; los resultados llegan con getScanResults(). */
    static void scan(Context c) {
        WifiManager wm = wifi(c);
        if (wm != null && wm.isWifiEnabled()) wm.startScan();
    }

    /** Redes cercanas, sin duplicados y de más señal a menos. */
    static List<ScanResult> nearby(Context c) {
        WifiManager wm = wifi(c);
        List<ScanResult> out = new ArrayList<>();
        if (wm == null) return out;
        List<ScanResult> all = wm.getScanResults();
        if (all == null) return out;
        for (ScanResult r : all) {
            if (r.SSID == null || r.SSID.isEmpty()) continue;
            boolean dup = false;
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i).SSID.equals(r.SSID)) {
                    dup = true;
                    if (r.level > out.get(i).level) out.set(i, r);
                    break;
                }
            }
            if (!dup) out.add(r);
        }
        Collections.sort(out, (a, b) -> Integer.compare(b.level, a.level));
        return out;
    }

    static boolean isSecured(ScanResult r) {
        String cap = r.capabilities;
        return cap != null && (cap.contains("WPA") || cap.contains("WEP") || cap.contains("SAE"));
    }

    /** Id de una red ya guardada con ese nombre, o -1. */
    static int savedId(Context c, String ssid) {
        WifiManager wm = wifi(c);
        if (wm == null) return -1;
        List<WifiConfiguration> cfg = wm.getConfiguredNetworks();
        if (cfg == null) return -1;
        for (WifiConfiguration w : cfg) {
            if (w.SSID != null && w.SSID.equals("\"" + ssid + "\"")) return w.networkId;
        }
        return -1;
    }

    /** Conecta a una red. password puede ser null si es abierta o ya está guardada. Devuelve mensaje. */
    static String connect(Context c, String ssid, boolean secured, String password) {
        WifiManager wm = wifi(c);
        if (wm == null) return "Wi-Fi no disponible";
        if (!wm.isWifiEnabled()) wm.setWifiEnabled(true);

        int id = savedId(c, ssid);
        if (id == -1 || (password != null && !password.isEmpty())) {
            WifiConfiguration w = new WifiConfiguration();
            w.SSID = "\"" + ssid + "\"";
            if (id != -1) w.networkId = id;
            if (secured) {
                if (password == null || password.length() < 8) return "La clave debe tener al menos 8 caracteres";
                w.preSharedKey = "\"" + password + "\"";
            } else {
                w.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
            id = wm.addNetwork(w);
            if (id == -1) return "No pude guardar la red";
        }
        wm.disconnect();
        boolean ok = wm.enableNetwork(id, true);
        wm.reconnect();
        wm.saveConfiguration();
        return ok ? "Conectando a " + ssid + "..." : "No pude conectarme a " + ssid;
    }
}

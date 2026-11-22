package com.david.ex2lite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Sube el volumen de un .wav PCM de 16 bits editando las muestras (el tamaño del archivo no cambia).
 *
 *  Nivel 1 FUERTE : lleva el pico al máximo sin deformar (normalizar).
 *  Nivel 2 MÁXIMO : normaliza y además comprime con una curva suave, para que suene más "lleno".
 */
public final class WavGain {
    private WavGain() {}

    public static final int NORMAL = 0, FUERTE = 1, MAXIMO = 2;
    private static final double MAX_BOOST = 8.0;   // tope: +18 dB

    /** Resultado: ganancia aplicada en dB (>= 0) o un número negativo si no se pudo procesar. */
    public static double process(File in, File out, int level) {
        try {
            byte[] b = readAll(in);
            if (level <= NORMAL) { write(out, b); return 0; }

            int fmtPos = -1, dataPos = -1, dataLen = 0;
            int pos = 12;
            while (pos + 8 <= b.length) {
                String id = new String(b, pos, 4, "US-ASCII");
                int len = le32(b, pos + 4);
                if (id.equals("fmt ")) fmtPos = pos + 8;
                if (id.equals("data")) {
                    dataPos = pos + 8;
                    dataLen = (len < 0 || dataPos + len > b.length) ? b.length - dataPos : len;
                    break;
                }
                pos += 8 + len + (len & 1);
            }
            if (fmtPos < 0 || dataPos < 0) return -1;
            int format = le16(b, fmtPos), bits = le16(b, fmtPos + 14);
            if (!(format == 1 || format == 0xFFFE) || bits != 16) return -2;   // solo PCM 16 bits

            int n = dataLen / 2;
            int peak = 1;
            for (int i = 0; i < n; i++) {
                int s = Math.abs((short) ((b[dataPos + 2 * i] & 0xFF) | (b[dataPos + 2 * i + 1] << 8)));
                if (s > peak) peak = s;
            }
            double boost = Math.min(MAX_BOOST, 32767.0 * 0.97 / peak);
            double a = 2.4, norm = Math.tanh(a);
            for (int i = 0; i < n; i++) {
                int o = dataPos + 2 * i;
                double x = ((short) ((b[o] & 0xFF) | (b[o + 1] << 8))) * boost / 32768.0;
                if (level >= MAXIMO) x = Math.tanh(a * x) / norm * 0.97;
                int s = (int) Math.round(Math.max(-1.0, Math.min(1.0, x)) * 32767);
                b[o] = (byte) (s & 0xFF);
                b[o + 1] = (byte) ((s >> 8) & 0xFF);
            }
            write(out, b);
            double db = 20 * Math.log10(boost);
            return level >= MAXIMO ? db + 7.7 : db;
        } catch (Exception e) {
            return -3;
        }
    }

    private static byte[] readAll(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int r;
            while ((r = in.read(buf)) > 0) bos.write(buf, 0, r);
            return bos.toByteArray();
        }
    }

    private static void write(File f, byte[] b) throws IOException {
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(b); }
    }

    private static int le16(byte[] b, int p) { return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8); }
    private static int le32(byte[] b, int p) { return le16(b, p) | (le16(b, p + 2) << 16); }
}

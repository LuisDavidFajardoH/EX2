package com.david.ex2lite;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferencias en almacenamiento "device protected": se leen desde el arranque. */
public final class Prefs {
    public static final String DRIVE = "drive";          // último modo de manejo 1..3 (Eco/Comfort/Sport)
    public static final String REGEN = "regen";          // última regeneración 1..3 (Baja/Media/Alta)
    public static final String REMEMBER = "remember";    // reaplicar al encender
    public static final String LOCK_ORIG = "lock_orig";  // sonido que tenía el carro
    public static final String WATCH = "watch";          // vigilar cambios en segundo plano (apagado por defecto)
    public static final String REOPEN = "reopen";        // reabrir la app tras una actualización
    public static final String LOCK_GAIN = "lock_gain";  // 0 normal, 1 fuerte, 2 máximo
    public static final String THEME = "theme";          // 0 papaya (McLaren), 1 tron
    public static final String LOCK_NAME = "lock_name";  // nombre del sonido elegido

    /** Modos guardados por el usuario. */
    public static final String CITY_DRIVE = "city_drive", CITY_REGEN = "city_regen";
    public static final String HWY_DRIVE = "hwy_drive", HWY_REGEN = "hwy_regen";

    private Prefs() {}

    public static SharedPreferences get(Context c) {
        try {
            return c.createDeviceProtectedStorageContext().getSharedPreferences("ex2lite", Context.MODE_PRIVATE);
        } catch (Throwable e) {
            return c.getSharedPreferences("ex2lite", Context.MODE_PRIVATE);
        }
    }
}

package com.david.ex2lite;

import android.car.Car;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

/**
 * Acceso mínimo al carro: modo de manejo, regeneración y estados de energía.
 * IDs del VHAL del IHU629G (Geely EX2), tomados de assef/meu-ex2 (MIT).
 */
public class CarLink {
    static final String T = "EX2Lite";

    /** Eco=+1, Comfort=+2, Sport=+3 (áreas 0/1). */
    public static final int DRIVE = 570491136;
    /** Baja=+1, Media=+2, Alta=+3 (áreas 0/1). */
    public static final int REGEN = 537003264;

    /** Estados de CarPowerStateListener en Android 9. */
    public static final int SUSPEND_EXIT = 3;

    public interface PowerListener { void onState(int state); }

    /** Callback de cambios del carro (clase con nombre, no anónima, para el compilador de Android). */
    public static final class Changes implements CarPropertyManager.CarPropertyEventCallback {
        public interface Sink { void changed(CarPropertyValue value); }
        private final Sink sink;
        public Changes(Sink s) { this.sink = s; }
        @Override public void onChangeEvent(CarPropertyValue value) { sink.changed(value); }
        @Override public void onErrorEvent(int propId, int areaId) { }
    }

    private Car car;
    private CarPropertyManager cpm;

    public boolean connect(Context c) {
        if (cpm != null) return true;
        try {
            car = Car.createCar(c);
            if (!car.isConnected()) {
                try { car.connect(); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < 50 && !car.isConnected(); i++) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
            cpm = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
            return cpm != null;
        } catch (Throwable t) {
            Log.w(T, "connect", t);
            cpm = null;
            return false;
        }
    }

    public boolean ready() { return cpm != null; }

    private Integer readInt(int id) {
        for (int a : new int[]{0, 1}) {
            try { return cpm.getIntProperty(id, a); } catch (Throwable ignored) {}
        }
        return null;
    }

    private boolean writeInt(int id, int val) {
        boolean ok = false;
        for (int a : new int[]{0, 1}) {
            try { cpm.setIntProperty(id, a, val); ok = true; } catch (Throwable ignored) {}
        }
        return ok;
    }

    /** 1..3, o -1 si no se pudo leer. */
    public int drive() { Integer v = cpm == null ? null : readInt(DRIVE); return v == null ? -1 : v - DRIVE; }
    public int regen() { Integer v = cpm == null ? null : readInt(REGEN); return v == null ? -1 : v - REGEN; }

    /** Marcha: 1=N 2=R 4=P 8=D, o null si no se pudo leer. */
    public Integer gear() {
        if (cpm == null) return null;
        for (int a : new int[]{0, 1, 16777216}) {
            try { return cpm.getIntProperty(KeeperService.GEAR, a); } catch (Throwable ignored) {}
        }
        return null;
    }

    /** Avisa cuando el carro cambia modo o regeneración (desde la pantalla o desde el volante). */
    public boolean listenChanges(CarPropertyManager.CarPropertyEventCallback cb) {
        if (cpm == null) return false;
        boolean a = cpm.registerCallback(cb, DRIVE, CarPropertyManager.SENSOR_RATE_ONCHANGE);
        boolean b = cpm.registerCallback(cb, REGEN, CarPropertyManager.SENSOR_RATE_ONCHANGE);
        return a || b;
    }

    public boolean setDrive(int level) { return cpm != null && level >= 1 && level <= 3 && writeInt(DRIVE, DRIVE + level); }
    public boolean setRegen(int level) { return cpm != null && level >= 1 && level <= 3 && writeInt(REGEN, REGEN + level); }

    /**
     * Escucha los estados de energía del carro (suspender / despertar).
     * CarPowerManager no está en los stubs, así que va por reflexión.
     */
    public boolean listenPower(PowerListener l) {
        try {
            Object pm = car == null ? null : car.getCarManager("power");
            if (pm == null) return false;
            Class<?> lc = Class.forName("android.car.hardware.power.CarPowerManager$CarPowerStateListener",
                    false, pm.getClass().getClassLoader());
            Object proxy = Proxy.newProxyInstance(lc.getClassLoader(), new Class<?>[]{lc}, (self, m, args) -> {
                switch (m.getName()) {
                    case "onStateChanged": l.onState((Integer) args[0]); return null;
                    case "hashCode": return System.identityHashCode(self);
                    case "equals": return self == args[0];
                    case "toString": return "EX2LitePower";
                    default: return null;
                }
            });
            Executor direct = Runnable::run;
            Method set = pm.getClass().getMethod("setListener", lc, Executor.class);
            set.invoke(pm, proxy, direct);
            return true;
        } catch (Throwable t) {
            Log.w(T, "listenPower", t);
            return false;
        }
    }
}

package com.david.ex2lite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/** Aplica el modo y el sonido al encender; tras una actualización pedida desde la app, la reabre. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        Log.i("EX2Lite", "evento: " + i.getAction());
        KeeperService.start(c);
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) {
            SharedPreferences p = Prefs.get(c);
            boolean reopen = p.getBoolean(Prefs.REOPEN, false);
            p.edit().putBoolean(Prefs.REOPEN, false).apply();
            if (reopen) {
                try {
                    c.startActivity(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Throwable t) {
                    Log.w("EX2Lite", "reabrir tras actualizar", t);
                }
            }
        }
    }
}

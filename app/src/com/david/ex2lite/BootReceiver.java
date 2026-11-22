package com.david.ex2lite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Arranca el servicio al encender la pantalla del carro o al actualizar la app. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        Log.i("EX2Lite", "boot: " + i.getAction());
        KeeperService.start(c);
    }
}

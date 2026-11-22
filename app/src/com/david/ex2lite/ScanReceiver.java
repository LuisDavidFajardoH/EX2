package com.david.ex2lite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Avisa a la pantalla cuando el Wi-Fi terminó de buscar redes. */
public class ScanReceiver extends BroadcastReceiver {
    private final MainActivity host;

    public ScanReceiver(MainActivity host) {
        this.host = host;
    }

    @Override public void onReceive(Context c, Intent i) {
        host.onScanResults();
    }
}

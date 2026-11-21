package com.julian.foldhud;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StepsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        final PendingResult pr = goAsync();
        Steps.snapshot(c, new Runnable() {
            @Override public void run() { pr.finish(); }
        });
    }
}

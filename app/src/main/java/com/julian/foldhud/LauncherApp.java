package com.julian.foldhud;


import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;

public class LauncherApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // One cheap, batched check of the step counter per hour (no background service).
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(this, 1, new Intent(this, StepsReceiver.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        if (am != null) am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 3600000L, AlarmManager.INTERVAL_HOUR, pi);
    }
}

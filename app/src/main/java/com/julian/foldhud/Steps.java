package com.julian.foldhud;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.util.Calendar;

/** Reads the phone's step counter once in a while (never keeps the sensor running) and adds up steps per day. */
final class Steps {
    static String key(Calendar c) {
        return String.valueOf(c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH));
    }

    static void snapshot(Context ctx, final Runnable done) {
        final Context c = ctx.getApplicationContext();
        if (Build.VERSION.SDK_INT >= 29 && c.checkSelfPermission("android.permission.ACTIVITY_RECOGNITION") != PackageManager.PERMISSION_GRANTED) {
            if (done != null) done.run();
            return;
        }
        final SensorManager sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        final Sensor s = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (s == null) {
            if (done != null) done.run();
            return;
        }
        final boolean[] fin = new boolean[1];
        final SensorEventListener l = new SensorEventListener() {
            @Override public void onSensorChanged(SensorEvent e) {
                if (fin[0]) return;
                fin[0] = true;
                sm.unregisterListener(this);
                record(c, e.values[0]);
                if (done != null) done.run();
            }
            @Override public void onAccuracyChanged(Sensor x, int a) { }
        };
        sm.registerListener(l, s, SensorManager.SENSOR_DELAY_NORMAL);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                if (!fin[0]) {
                    fin[0] = true;
                    sm.unregisterListener(l);
                    if (done != null) done.run();
                }
            }
        }, 4000);
    }

    private static synchronized void record(Context c, float counter) {
        SharedPreferences sp = Prefs.sp(c);
        float last = sp.getFloat("s_last", -1f);
        float delta = last < 0 ? 0 : (counter >= last ? counter - last : counter);   // counter restarts at 0 after a reboot
        String k = "s" + key(Calendar.getInstance());
        sp.edit().putInt(k, sp.getInt(k, 0) + Math.round(delta)).putFloat("s_last", counter).apply();
    }
}

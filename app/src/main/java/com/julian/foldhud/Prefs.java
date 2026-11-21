package com.julian.foldhud;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;

final class Prefs {
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("hud", Context.MODE_PRIVATE); }

    /** 0 = Saver (still picture), 1 = Balanced (default), 2 = Full */
    static int userMode(Context c) { return sp(c).getInt("mode", 1); }

    static int battery(Context c) {
        Intent i = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return 100;
        int l = i.getIntExtra("level", 100), s = i.getIntExtra("scale", 100);
        return s > 0 ? l * 100 / s : 100;
    }

    /** The mode really used: Saver is forced when Android Battery Saver is on or the battery is under 15%. */
    static int mode(Context c) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isPowerSaveMode()) return 0;
        if (battery(c) < 15) return 0;
        return userMode(c);
    }

    /** How old the news may get before it is fetched again (minutes: 5, 10 or 30). Feeds that did not change cost almost no data. */
    static long newsTtl(Context c) { return sp(c).getInt("newsmin", 5) * 60000L; }

    static boolean flag(Context c, String k, boolean def) { return sp(c).getBoolean(k, def); }
    static void setFlag(Context c, String k, boolean v) { sp(c).edit().putBoolean(k, v).apply(); }
}

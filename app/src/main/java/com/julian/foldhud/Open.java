package com.julian.foldhud;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.widget.Toast;

/** What each widget opens. You can pick your own app for any widget (menu: Which app each widget opens). */
final class Open {
    static void go(Activity a, int what, int arg) {
        if (what != HomeView.A_FIN && what != HomeView.A_GOOGLE) {
            String mine = Prefs.sp(a).getString("tgt_" + what, null);
            if (mine != null && pkg(a, mine)) return;
        }
        switch (what) {
            case HomeView.A_CLOCK:
                if (!pkg(a, "com.sec.android.app.clockpackage", "com.google.android.deskclock", "com.android.deskclock"))
                    start(a, new Intent(AlarmClock.ACTION_SHOW_ALARMS));
                break;
            case HomeView.A_CAL:
                if (!pkg(a, "com.samsung.android.calendar", "com.google.android.calendar", "com.android.calendar"))
                    start(a, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR));
                break;
            case HomeView.A_WEATHER:
                if (!pkg(a, "com.sec.android.daemonapp", "com.google.android.apps.weather")) web(a, "https://www.google.com/search?q=weather");
                break;
            case HomeView.A_HEALTH:
                if (!pkg(a, "com.sec.android.app.shealth", "com.google.android.apps.fitness")) toast(a, "Samsung Health or Google Fit is not installed");
                break;
            case HomeView.A_CARE:
                if (!pkg(a, "com.samsung.android.lool")) start(a, new Intent("android.settings.BATTERY_SAVER_SETTINGS"));
                break;
            case HomeView.A_DATA:
                start(a, new Intent("android.settings.DATA_USAGE_SETTINGS"));
                break;
            case HomeView.A_SETTINGS:
                start(a, new Intent(Settings.ACTION_SETTINGS));
                break;
            case HomeView.A_FIN:
                web(a, "https://finance.yahoo.com/quote/" + Uri.encode(Config.STOCKS[Math.max(0, Math.min(arg, Config.STOCKS.length - 1))]));
                break;
            case HomeView.A_GOOGLE:
                if (!pkg(a, "com.google.android.googlequicksearchbox")) web(a, "https://www.google.com");
                break;
            default:
                break;
        }
    }

    static String firstInstalled(Context c, String... pkgs) {
        for (String p : pkgs) if (c.getPackageManager().getLaunchIntentForPackage(p) != null) return p;
        return null;
    }

    static boolean pkg(Activity a, String... pkgs) {
        for (String p : pkgs) {
            Intent i = a.getPackageManager().getLaunchIntentForPackage(p);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { a.startActivity(i); return true; } catch (Exception e) { /* try next */ }
            }
        }
        return false;
    }

    static void start(Activity a, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { a.startActivity(i); } catch (Exception e) { toast(a, "No app found for this"); }
    }

    static void web(Activity a, String url) { start(a, new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }

    static void toast(Activity a, String s) { Toast.makeText(a, s, Toast.LENGTH_SHORT).show(); }
}

package com.julian.foldhud;


import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads news, stocks, weather and location, but only when the saved copy is old, and only on a background thread. */
final class Refresher {
    private static final ExecutorService EX = Executors.newSingleThreadExecutor();
    private static volatile boolean busy;

    static void run(final Context app, final Data d, final boolean force, final Runnable done) {
        if (busy) return;
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null || cm.getActiveNetwork() == null) return;
        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        final int mult = (pm != null && pm.isPowerSaveMode()) ? 4 : 1;      // refresh a quarter as often in Battery Saver
        final long now = System.currentTimeMillis();
        final boolean needLoc = force || Double.isNaN(d.lat) || now - d.locUpd > Config.LOC_TTL * mult;
        final boolean needNews = force || now - d.newsUpd > Prefs.newsTtl(app) * mult;
        final boolean needStocks = force || now - d.stocksUpd > Config.STOCKS_TTL * mult;
        final boolean needWx = force || d.weatherUpd == 0 || now - d.weatherUpd > Config.WEATHER_TTL * mult || needLoc;
        if (!(needLoc || needNews || needStocks || needWx)) return;
        busy = true;
        final double oldLat = d.lat, oldLon = d.lon;
        final Handler main = new Handler(Looper.getMainLooper());
        EX.execute(new Runnable() {
            @Override public void run() {
                try {
                    String[] loc = needLoc ? locate(app) : null;
                    double la = oldLat, lo = oldLon;
                    if (loc != null) { la = Double.parseDouble(loc[0]); lo = Double.parseDouble(loc[1]); }
                    String wx = null;
                    if (needWx && !Double.isNaN(la)) {
                        try {
                            wx = Net.get("https://api.open-meteo.com/v1/forecast?latitude=" + la + "&longitude=" + lo
                                    + "&timezone=auto&forecast_days=4&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
                                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset", 10000);
                        } catch (Exception e) { wx = null; }
                    }
                    String q = needStocks ? quotes() : null;
                    List<NewsItem> w = new ArrayList<>(), m = new ArrayList<>();
                    if (needNews) {
                        w = Net.loadFeeds(Config.WORLD, 6, 30, Prefs.sp(app));
                        m = Net.loadFeeds(Config.MYANMAR, 3, 30, Prefs.sp(app));
                    }
                    final String[] fl = loc;
                    final String fwx = wx, fq = q;
                    final List<NewsItem> fw = w, fm = m;
                    main.post(new Runnable() {
                        @Override public void run() {
                            SharedPreferences sp = Prefs.sp(app);
                            if (fl != null) d.applyLocation(sp, Double.parseDouble(fl[0]), Double.parseDouble(fl[1]), fl[2], fl[3]);
                            if (fwx != null) d.applyWeather(sp, fwx);
                            if (fq != null) d.applyQuotes(sp, fq);
                            if (!fw.isEmpty() || !fm.isEmpty()) d.applyNews(sp, fw, fm);
                            busy = false;
                            if (done != null) done.run();
                        }
                    });
                } catch (Throwable t) {
                    busy = false;
                }
            }
        });
    }

    /** {lat, lon, city, country}: the phone's last known position if allowed, otherwise a city-level guess from the internet connection. */
    private static String[] locate(Context c) {
        try {
            if (c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
                Location best = null;
                if (lm != null) {
                    for (String p : lm.getProviders(true)) {
                        Location l = lm.getLastKnownLocation(p);
                        if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                    }
                }
                if (best != null) return new String[]{String.valueOf(best.getLatitude()), String.valueOf(best.getLongitude()), "YOUR LOCATION", ""};
            }
        } catch (Exception e) { /* fall through */ }
        try {
            JSONObject j = new JSONObject(Net.get("https://ipwho.is/", 8000));
            if (j.optBoolean("success", true)) {
                return new String[]{String.valueOf(j.getDouble("latitude")), String.valueOf(j.getDouble("longitude")),
                        j.optString("city", "YOUR LOCATION").toUpperCase(), j.optString("country", "")};
            }
        } catch (Exception e) { /* none */ }
        return null;
    }

    private static String quotes() {
        JSONArray out = new JSONArray();
        for (int i = 0; i < Config.STOCKS.length; i++) {
            try {
                String s = Net.get("https://query1.finance.yahoo.com/v8/finance/chart/" + URLEncoder.encode(Config.STOCKS[i], "UTF-8")
                        + "?interval=15m&range=1d", 9000);
                JSONObject r = new JSONObject(s).getJSONObject("chart").getJSONArray("result").getJSONObject(0);
                JSONObject meta = r.getJSONObject("meta");
                double price = meta.getDouble("regularMarketPrice");
                double prev = meta.optDouble("chartPreviousClose", price);
                JSONArray cl = r.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
                ArrayList<Double> pts = new ArrayList<>();
                for (int k = 0; k < cl.length(); k++) if (!cl.isNull(k)) pts.add(cl.getDouble(k));
                int step = Math.max(1, pts.size() / 24);
                JSONArray sp = new JSONArray();
                for (int k = 0; k < pts.size(); k += step) sp.put(pts.get(k));
                JSONObject o = new JSONObject();
                o.put("n", Config.STOCK_NAMES[i]);
                o.put("p", price);
                o.put("c", prev == 0 ? 0 : (price - prev) / prev * 100);
                o.put("sp", sp);
                out.put(o);
            } catch (Exception e) { /* skip this symbol */ }
        }
        return out.length() > 0 ? out.toString() : null;
    }
}

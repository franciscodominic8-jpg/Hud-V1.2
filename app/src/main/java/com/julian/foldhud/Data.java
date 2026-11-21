package com.julian.foldhud;


import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/** Everything the home screen shows that comes from the internet or the step counter. Only the main thread touches it. */
final class Data {
    static final class Quote { String name; double price, chg; float[] spark; }
    static final class Hot { String name; float lat, lon; NewsItem item; int n; }

    final ArrayList<NewsItem> world = new ArrayList<>(), myanmar = new ArrayList<>();
    final ArrayList<Quote> quotes = new ArrayList<>();
    final ArrayList<Hot> hot = new ArrayList<>();
    double lat = Double.NaN, lon = Double.NaN;
    String city = "YOUR LOCATION", country = "";
    boolean hasW;
    double temp, wind;
    int hum;
    String wdesc = "--", sunrise = "--:--", sunset = "--:--";
    final String[] fday = new String[3], fdesc = new String[3];
    final int[] fhi = new int[3], flo = new int[3];
    long newsUpd, stocksUpd, weatherUpd, locUpd;
    int[] steps = new int[7];
    int todayIdx;

    static String wcode(int c) {
        switch (c) {
            case 0: return "CLEAR";
            case 1: return "MOSTLY CLEAR";
            case 2: return "PARTLY CLOUDY";
            case 3: return "OVERCAST";
            case 45: case 48: return "FOG";
            case 51: case 53: case 55: return "DRIZZLE";
            case 61: case 63: return "RAIN";
            case 65: return "HEAVY RAIN";
            case 71: case 73: case 75: return "SNOW";
            case 80: case 81: return "SHOWERS";
            case 82: return "HEAVY SHOWERS";
            case 95: case 96: case 99: return "STORM";
            default: return "---";
        }
    }

    // ---------- loading from / saving to the phone's small settings file ----------
    void load(SharedPreferences sp) {
        readList(sp.getString("nw", ""), world);
        readList(sp.getString("nm", ""), myanmar);
        newsUpd = sp.getLong("nu", 0);
        stocksUpd = sp.getLong("su", 0);
        weatherUpd = sp.getLong("wu", 0);
        locUpd = sp.getLong("lu", 0);
        try {
            if (sp.contains("lat")) { lat = Double.parseDouble(sp.getString("lat", "NaN")); lon = Double.parseDouble(sp.getString("lon", "NaN")); }
        } catch (Exception e) { lat = Double.NaN; lon = Double.NaN; }
        city = sp.getString("city", city);
        country = sp.getString("country", country);
        parseQuotes(sp.getString("q", ""));
        parseWeather(sp.getString("wx", ""));
        loadSteps(sp);
    }

    private static void readList(String json, List<NewsItem> out) {
        out.clear();
        if (json == null || json.isEmpty()) return;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) out.add(NewsItem.fromJson(a.getJSONObject(i)));
        } catch (Exception e) { /* ignore */ }
    }

    private static String listJson(List<NewsItem> l) {
        JSONArray a = new JSONArray();
        for (NewsItem n : l) a.put(n.toJson());
        return a.toString();
    }

    void applyNews(SharedPreferences sp, List<NewsItem> w, List<NewsItem> m) {
        if (!w.isEmpty()) { world.clear(); world.addAll(w); }
        if (!m.isEmpty()) { myanmar.clear(); myanmar.addAll(m); }
        newsUpd = System.currentTimeMillis();
        sp.edit().putString("nw", listJson(world)).putString("nm", listJson(myanmar)).putLong("nu", newsUpd).apply();
        calcHot();
    }

    void applyQuotes(SharedPreferences sp, String json) {
        if (json == null || json.length() < 3) return;
        parseQuotes(json);
        stocksUpd = System.currentTimeMillis();
        sp.edit().putString("q", json).putLong("su", stocksUpd).apply();
    }

    void applyWeather(SharedPreferences sp, String json) {
        if (json == null || json.isEmpty()) return;
        parseWeather(json);
        weatherUpd = System.currentTimeMillis();
        sp.edit().putString("wx", json).putLong("wu", weatherUpd).apply();
    }

    void applyLocation(SharedPreferences sp, double la, double lo, String c, String k) {
        lat = la; lon = lo; city = c; country = k; locUpd = System.currentTimeMillis();
        sp.edit().putString("lat", String.valueOf(la)).putString("lon", String.valueOf(lo)).putString("city", c).putString("country", k).putLong("lu", locUpd).apply();
    }

    private void parseQuotes(String json) {
        quotes.clear();
        if (json == null || json.isEmpty()) return;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Quote q = new Quote();
                q.name = o.getString("n");
                q.price = o.getDouble("p");
                q.chg = o.getDouble("c");
                JSONArray sp = o.getJSONArray("sp");
                q.spark = new float[sp.length()];
                for (int j = 0; j < sp.length(); j++) q.spark[j] = (float) sp.getDouble(j);
                quotes.add(q);
            }
        } catch (Exception e) { quotes.clear(); }
    }

    private void parseWeather(String s) {
        if (s == null || s.isEmpty()) return;
        try {
            JSONObject j = new JSONObject(s), c = j.getJSONObject("current");
            temp = c.getDouble("temperature_2m");
            hum = c.getInt("relative_humidity_2m");
            wind = c.getDouble("wind_speed_10m");
            wdesc = wcode(c.getInt("weather_code"));
            JSONObject dl = j.getJSONObject("daily");
            JSONArray t = dl.getJSONArray("time"), hi = dl.getJSONArray("temperature_2m_max"), lo = dl.getJSONArray("temperature_2m_min"),
                    wc = dl.getJSONArray("weather_code"), sr = dl.getJSONArray("sunrise"), ss = dl.getJSONArray("sunset");
            for (int i = 0; i < 3; i++) {
                fday[i] = t.getString(i + 1).substring(5);
                fhi[i] = (int) Math.round(hi.getDouble(i + 1));
                flo[i] = (int) Math.round(lo.getDouble(i + 1));
                fdesc[i] = wcode(wc.getInt(i + 1));
            }
            String a = sr.getString(0), b = ss.getString(0);
            sunrise = a.substring(a.indexOf('T') + 1);
            sunset = b.substring(b.indexOf('T') + 1);
            hasW = true;
        } catch (Exception e) { /* keep old values */ }
    }

    // ---------- steps (Monday to Sunday of this week) ----------
    void loadSteps(SharedPreferences sp) {
        Calendar c = Calendar.getInstance();
        int dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;   // Monday = 0
        Calendar d = (Calendar) c.clone();
        d.add(Calendar.DAY_OF_YEAR, -dow);
        int[] out = new int[7];
        for (int i = 0; i < 7; i++) {
            out[i] = i <= dow ? sp.getInt("s" + Steps.key(d), 0) : 0;
            d.add(Calendar.DAY_OF_YEAR, 1);
        }
        steps = out;
        todayIdx = dow;
    }

    // ---------- which countries are in the news ----------
    void calcHot() {
        WorldData.init();
        HashMap<String, Hot> h = new HashMap<>();
        scan(world, h);
        scan(myanmar, h);
        if (!myanmar.isEmpty() && !h.containsKey("Myanmar")) {
            for (int i = 0; i < WorldData.NAMES.length; i++) {
                if (WorldData.NAMES[i].equals("Myanmar")) {
                    Hot o = new Hot();
                    o.name = "Myanmar"; o.lat = WorldData.CLAT[i]; o.lon = WorldData.CLON[i]; o.item = myanmar.get(0); o.n = myanmar.size();
                    h.put("Myanmar", o);
                }
            }
        }
        ArrayList<Hot> l = new ArrayList<>(h.values());
        Collections.sort(l, (a, b) -> Long.compare(b.item.ts, a.item.ts));
        hot.clear();
        hot.addAll(l);
    }

    private void scan(List<NewsItem> list, HashMap<String, Hot> h) {
        for (NewsItem it : list) {
            if (it.title == null) continue;
            for (int i = 0; i < WorldData.RE.length; i++) {
                if (WorldData.RE[i].matcher(it.title).find()) {
                    String n = WorldData.NAMES[i];
                    Hot o = h.get(n);
                    if (o == null) {
                        o = new Hot();
                        o.name = n; o.lat = WorldData.CLAT[i]; o.lon = WorldData.CLON[i]; o.item = it; o.n = 1;
                        h.put(n, o);
                    } else {
                        o.n++;
                        if (it.ts > o.item.ts) o.item = it;
                    }
                }
            }
        }
    }
}

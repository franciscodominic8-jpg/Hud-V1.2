package com.julian.foldhud;


import android.content.SharedPreferences;
import android.text.Html;
import android.util.Xml;
import org.json.JSONArray;
import org.xmlpull.v1.XmlPullParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

final class Net {
    static String get(String url, int ms) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(ms);
        c.setReadTimeout(ms);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (FoldHUD)");
        c.setInstanceFollowRedirects(true);
        InputStream in = null;
        try {
            in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n, total = 0;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
                total += n;
                if (total > 1500000) break;
            }
            return out.toString("UTF-8");
        } finally {
            if (in != null) try { in.close(); } catch (IOException e) { /* ignore */ }
            c.disconnect();
        }
    }

    static long parseDate(String t) {
        String[] f = {"EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss zzz",
                "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd HH:mm:ss"};
        for (String p : f) {
            try {
                SimpleDateFormat s = new SimpleDateFormat(p, Locale.US);
                if (p.endsWith("'Z'")) s.setTimeZone(TimeZone.getTimeZone("UTC"));
                return s.parse(t.trim()).getTime();
            } catch (Exception e) { /* try next */ }
        }
        return System.currentTimeMillis();
    }

    private static String plain(String html, int max) {
        if (html == null) return "";
        String s = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().replace('\n', ' ').replace('\uFFFC', ' ').trim();
        s = s.replaceAll("\\s+", " ");
        return s.length() > max ? s.substring(0, max - 1) + "\u2026" : s;
    }

    /** Reads RSS or Atom. src = outlet name, or null to use the feed's own source name (Google News). */
    static List<NewsItem> parseFeed(String xml, String src) {
        ArrayList<NewsItem> out = new ArrayList<>();
        try {
            XmlPullParser p = Xml.newPullParser();
            p.setInput(new StringReader(xml));
            int ev = p.getEventType();
            NewsItem cur = null;
            String tag = null;
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    String n = p.getName();
                    if (n.equals("item") || n.equals("entry")) {
                        cur = new NewsItem();
                        tag = null;
                    } else if (cur != null) {
                        tag = n;
                        if (n.equals("link") && cur.link == null) {
                            String h = p.getAttributeValue(null, "href");
                            if (h != null) cur.link = h;
                        }
                    }
                } else if (ev == XmlPullParser.TEXT && cur != null && tag != null) {
                    String t = p.getText();
                    if (t != null && !t.trim().isEmpty()) {
                        switch (tag) {
                            case "title": cur.title = (cur.title == null ? "" : cur.title) + t; break;
                            case "link": if (cur.link == null) cur.link = t.trim(); break;
                            case "description": case "summary": case "content": if (cur.desc == null) cur.desc = t; break;
                            case "pubDate": case "published": case "updated": case "date": if (cur.ts == 0) cur.ts = parseDate(t); break;
                            case "source": if (cur.source == null) cur.source = t.trim(); break;
                            default: break;
                        }
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    String n = p.getName();
                    if (n.equals("item") || n.equals("entry")) {
                        if (cur != null && cur.title != null) {
                            cur.title = plain(cur.title, 200);
                            String s = src != null ? src : (cur.source != null ? cur.source : "NEWS");
                            if (src == null && cur.title.endsWith(" - " + s)) cur.title = cur.title.substring(0, cur.title.length() - s.length() - 3);
                            cur.source = s.toUpperCase(Locale.US);
                            if (cur.source.length() > 14) cur.source = cur.source.substring(0, 14);
                            cur.desc = plain(cur.desc, 420);
                            if (cur.desc.toLowerCase(Locale.US).startsWith(cur.title.toLowerCase(Locale.US).substring(0, Math.min(30, cur.title.length())))
                                    && cur.desc.length() < cur.title.length() + 40) cur.desc = "";
                            if (cur.ts == 0) cur.ts = System.currentTimeMillis();
                            if (!cur.title.isEmpty()) out.add(cur);
                        }
                        cur = null;
                    }
                    tag = null;
                }
                ev = p.next();
            }
        } catch (Exception e) { /* return what we have */ }
        return out;
    }

    private static List<NewsItem> dedupe(List<NewsItem> all, int keep) {
        Collections.sort(all, (a, b) -> Long.compare(b.ts, a.ts));
        ArrayList<NewsItem> out = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (NewsItem n : all) {
            if (seen.add(n.title.toLowerCase(Locale.US))) {
                out.add(n);
                if (out.size() >= keep) break;
            }
        }
        return out;
    }

    /** Downloads a page, but asks the server first whether it changed; returns null when it did not (HTTP 304). */
    private static String getCond(String url, int ms, SharedPreferences sp, String key) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(ms);
        c.setReadTimeout(ms);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (FoldHUD)");
        c.setInstanceFollowRedirects(true);
        if (sp != null) {
            String e = sp.getString(key + "e", null), m = sp.getString(key + "m", null);
            if (e != null) c.setRequestProperty("If-None-Match", e);
            if (m != null) c.setRequestProperty("If-Modified-Since", m);
        }
        InputStream in = null;
        try {
            int code = c.getResponseCode();
            if (code == 304) return null;
            in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n, total = 0;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
                total += n;
                if (total > 1500000) break;
            }
            if (sp != null) {
                sp.edit().putString(key + "e", c.getHeaderField("ETag")).putString(key + "m", c.getHeaderField("Last-Modified")).apply();
            }
            return out.toString("UTF-8");
        } finally {
            if (in != null) try { in.close(); } catch (IOException e) { /* ignore */ }
            c.disconnect();
        }
    }

    private static List<NewsItem> cached(SharedPreferences sp, String key) {
        ArrayList<NewsItem> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString(key + "c", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(NewsItem.fromJson(a.getJSONObject(i)));
        } catch (Exception e) { /* empty */ }
        return out;
    }

    static List<NewsItem> loadFeeds(String[][] feeds, int perSource, int keep, SharedPreferences sp) {
        ArrayList<NewsItem> all = new ArrayList<>();
        for (String[] f : feeds) {
            List<NewsItem> got = null;
            for (String u : f[1].split("\\|")) {
                String key = "f" + Integer.toHexString(u.hashCode());
                try {
                    String body = getCond(u, 10000, sp, key);
                    if (body == null) {
                        got = sp != null ? cached(sp, key) : null;               // unchanged since last time
                    } else {
                        got = parseFeed(body, f[0]);
                        if (sp != null && got != null && !got.isEmpty()) {
                            JSONArray ja = new JSONArray();
                            for (int i = 0; i < got.size() && i < 8; i++) ja.put(got.get(i).toJson());
                            sp.edit().putString(key + "c", ja.toString()).apply();
                        }
                    }
                } catch (Exception e) {
                    got = sp != null ? cached(sp, key) : null;                   // offline: use the saved copy
                }
                if (got != null && !got.isEmpty()) break;
            }
            if (got != null) {
                Collections.sort(got, (a, b) -> Long.compare(b.ts, a.ts));
                for (int i = 0; i < got.size() && i < perSource; i++) all.add(got.get(i));
            }
        }
        return dedupe(all, keep);
    }

    /** Latest stories about one country (Google News search feed). Myanmar also gets the Myanmar outlets. */
    static List<NewsItem> countryNews(String query, boolean myanmar) {
        ArrayList<NewsItem> items = new ArrayList<>();
        String[] when = {"2d", "7d"};
        for (String w : when) {
            try {
                String url = "https://news.google.com/rss/search?q=" + URLEncoder.encode(query + " when:" + w, "UTF-8")
                        + "&hl=en-US&gl=US&ceid=US:en";
                List<NewsItem> got = parseFeed(get(url, 10000), null);
                items.clear();
                for (int i = 0; i < got.size() && i < 12; i++) items.add(got.get(i));
            } catch (Exception e) { items.clear(); }
            if (items.size() >= 4) break;
        }
        if (myanmar) items.addAll(loadFeeds(Config.MYANMAR, 3, 12, null));
        return dedupe(items, 8);
    }
}

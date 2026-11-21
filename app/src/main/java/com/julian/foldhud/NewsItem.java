package com.julian.foldhud;


import org.json.JSONObject;

final class NewsItem {
    String title, source, desc, link;
    long ts;
    String[] lines;   // wrapped-title cache used by the drawing code
    float linesW;

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("t", title);
            o.put("s", source == null ? "" : source);
            o.put("d", desc == null ? "" : desc);
            o.put("l", link == null ? "" : link);
            o.put("ts", ts);
        } catch (Exception e) { /* ignore */ }
        return o;
    }

    static NewsItem fromJson(JSONObject o) {
        NewsItem n = new NewsItem();
        n.title = o.optString("t");
        n.source = o.optString("s");
        n.desc = o.optString("d");
        n.link = o.optString("l");
        n.ts = o.optLong("ts");
        return n;
    }
}

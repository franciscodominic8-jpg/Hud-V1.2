package com.julian.foldhud;


import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.TrafficStats;
import android.os.Environment;
import android.os.StatFs;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** The whole home screen, drawn by hand on one canvas (one view = very little memory). Layout A of the design. */
final class HomeView extends View {
    static final int A_CLOCK = 1, A_CAL = 2, A_WEATHER = 3, A_HEALTH = 4, A_CARE = 5, A_DATA = 6, A_SETTINGS = 7, A_FIN = 8,
            A_GOOGLE = 9, A_NEWS = 10, A_TAB = 11, A_DOCK = 13, A_DRAWER = 14, A_MENU = 15, A_HOT = 16, A_TILE = 17;
    static final float IW = 884f, IH = 736f, CW = 360f, CH = 924f;       // design sizes: unfolded screen / cover screen
    static final int ORANGE = 0xFFFFA31A, RED = 0xFFE5384F, BLUE = 0xFF7CC2FF, GREEN = 0xFF5FD38D, PINK = 0xFFFF6B7D, WHITE = 0xFFFFFFFF;
    static final int LEFT = 0, CENTER = 1, RIGHT = 2;
    static final float ROW = 62f;

    final HomeActivity host;
    final Data d;
    final GlobeRenderer globe = new GlobeRenderer();
    final Paint pf = new Paint(Paint.ANTI_ALIAS_FLAG), ps = new Paint(Paint.ANTI_ALIAS_FLAG), pt = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint pbmp = new Paint(Paint.FILTER_BITMAP_FLAG);   // for pictures: always fully opaque (pf's colour alpha made the dock look faded)
    final Path path = new Path();
    final RectF rf = new RectF();
    final float[] tmp = new float[3];
    final DashPathEffect dash = new DashPathEffect(new float[]{4f, 4f}, 0f);
    final ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();

    int layout = 0;   // 0 unfolded landscape, 1 unfolded portrait, 2 cover portrait, 3 cover landscape
    float s = 1f, ox, oy, DW = IW, DH = IH, gcx, gcy, gR = 150f;
    final float[] hx = new float[128], hy = new float[128], hw = new float[128], hh = new float[128];
    final int[] ha = new int[128], hg = new int[128];
    int hn;

    // cached clock / system values
    String timeStr = "--:--", dateStr = "", monthStr = "";
    long lastMinute = -1;
    int dim = 30, firstDow = 0, today = 1, moonPct = 0, bat = 100, memPct = 0, stoPct = 0;
    double rxKbps, txKbps;
    long lastSys, lastRx, lastTx;
    final float[] netHist = new float[14];

    // interaction + animation
    boolean running, moved, optGrid = true, optHot = true, optRot = false;
    int mode = 1, tmode, tab = 0;
    float vLon, vLat, downX, downY, lastX, lastY, newsScroll, newsTarget, nX, nY, nW, nH, visRows = 3f;
    long lastTouch, lastStepAt, lastTick;

    HomeView(HomeActivity a, Data data) {
        super(a);
        host = a;
        d = data;
        loadOpts();
    }

    void loadOpts() {
        Context c = getContext();
        optGrid = Prefs.flag(c, "grid", true);
        optHot = Prefs.flag(c, "hot", true);
        optRot = Prefs.flag(c, "rot", true);
    }

    static int wa(int pct) { return (pct * 255 / 100) << 24 | 0xFFFFFF; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float den = getResources().getDisplayMetrics().density;
        boolean wide = w > h, big = Math.min(w, h) / den >= 500f;
        layout = big ? (wide ? 0 : 1) : (wide ? 3 : 2);
        DW = layout == 0 ? 884f : layout == 1 ? 816f : layout == 2 ? 360f : 924f;
        DH = layout == 0 ? 736f : layout == 1 ? 980f : layout == 2 ? 924f : 360f;
        s = Math.min(w / DW, h / DH);
        ox = (w - DW * s) / 2f;
        oy = (h - DH * s) / 2f;
    }

    // ---------------- small drawing helpers ----------------
    void text(Canvas c, String t, float x, float y, float size, int col, int align, boolean bold) {
        pt.setTextSize(size);
        pt.setColor(col);
        pt.setTextAlign(align == LEFT ? Paint.Align.LEFT : align == CENTER ? Paint.Align.CENTER : Paint.Align.RIGHT);
        pt.setFakeBoldText(bold);
        c.drawText(t, x, y, pt);
    }

    void hit(float x, float y, float w, float h, int a, int g) {
        if (hn >= hx.length) return;
        hx[hn] = x; hy[hn] = y; hw[hn] = w; hh[hn] = h; ha[hn] = a; hg[hn] = g;
        hn++;
    }

    void box(Canvas c, float x, float y, float w, float h, float r, int fill, int stroke) {
        rf.set(x, y, x + w, y + h);
        pf.setStyle(Paint.Style.FILL);
        pf.setColor(fill);
        c.drawRoundRect(rf, r, r, pf);
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(1f);
        ps.setColor(stroke);
        c.drawRoundRect(rf, r, r, ps);
    }

    void corners(Canvas c, float x, float y, float w, float h, int col) {
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(2f);
        ps.setColor(col);
        float k = 12f;
        c.drawLine(x, y, x + k, y, ps); c.drawLine(x, y, x, y + k, ps);
        c.drawLine(x + w, y, x + w - k, y, ps); c.drawLine(x + w, y, x + w, y + k, ps);
        c.drawLine(x, y + h, x + k, y + h, ps); c.drawLine(x, y + h, x, y + h - k, ps);
        c.drawLine(x + w, y + h, x + w - k, y + h, ps); c.drawLine(x + w, y + h, x + w, y + h - k, ps);
    }

    void panel(Canvas c, float x, float y, float w, float h, String title, String tap, boolean orange, int a, int g) {
        pf.setStyle(Paint.Style.FILL);
        pf.setColor(0xFF080B10);
        c.drawRect(x, y, x + w, y + h, pf);
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(1f);
        ps.setColor(0x26FFFFFF);
        c.drawRect(x, y, x + w, y + h, ps);
        corners(c, x, y, w, h, orange ? ORANGE : WHITE);
        if (title != null) text(c, title, x + 10, y + 15, 10, 0xB8FFFFFF, LEFT, false);
        if (tap != null) text(c, "\u2197 " + tap, x + w - 10, y + 15, 9, ORANGE, RIGHT, false);
        if (a > 0) hit(x, y, w, h, a, g);
    }

    void bar(Canvas c, float x, float y, float w, int pct) {
        pf.setStyle(Paint.Style.FILL);
        pf.setColor(0x26FFFFFF);
        rf.set(x, y, x + w, y + 5);
        c.drawRoundRect(rf, 2.5f, 2.5f, pf);
        pf.setColor(WHITE);
        rf.set(x, y, x + w * Math.max(0, Math.min(100, pct)) / 100f, y + 5);
        c.drawRoundRect(rf, 2.5f, 2.5f, pf);
    }

    void ring(Canvas c, float cx, float cy, float r, int pct, int col) {
        rf.set(cx - r, cy - r, cx + r, cy + r);
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(5f);
        ps.setColor(0x26FFFFFF);
        c.drawOval(rf, ps);
        ps.setColor(col);
        c.drawArc(rf, -90f, 360f * Math.max(0, Math.min(100, pct)) / 100f, false, ps);
    }

    void spark(Canvas c, float x, float y, float w, float h, float[] v, int col) {
        int n = v.length;
        if (n < 2) return;
        float lo = v[0], hi = v[0];
        for (int i = 1; i < n; i++) { lo = Math.min(lo, v[i]); hi = Math.max(hi, v[i]); }
        float span = hi - lo < 1e-6f ? 1f : hi - lo;
        path.reset();
        for (int i = 0; i < n; i++) {
            float px = x + w * i / (n - 1), py = y + h - h * (v[i] - lo) / span;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(1.5f);
        ps.setColor(col);
        c.drawPath(path, ps);
    }

    void chip(Canvas c, float x, float y, float w, String label) {
        box(c, x, y, w, 22, 6, 0xFF0B0F14, 0x30FFFFFF);
        text(c, label, x + w / 2, y + 15, 10, 0xC8FFFFFF, CENTER, false);
    }

    float pill(Canvas c, float x, float y, String label, int dot, int a, int g) {
        pt.setTextSize(10);
        float w = pt.measureText(label) + 26f;
        box(c, x, y, w, 22, 10, 0xEE080B10, 0x4DFFFFFF);
        pf.setStyle(Paint.Style.FILL);
        pf.setColor(dot);
        c.drawCircle(x + 11, y + 11, 3f, pf);
        text(c, label, x + 19, y + 15, 10, WHITE, LEFT, false);
        if (a > 0) hit(x, y, w, 22, a, g);
        return w;
    }

    void pulse(Canvas c, float x, float y, int col, long now) {
        pf.setStyle(Paint.Style.FILL);
        pf.setColor(col);
        c.drawCircle(x, y, 3.5f, pf);
        float k = running ? ((now / 90) % 12) / 12f : 0.5f;
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(1.5f);
        ps.setColor((col & 0x00FFFFFF) | ((int) (255 * (1f - k)) << 24));
        c.drawCircle(x, y, 4f + 12f * k, ps);
    }

    static String ago(long ts) {
        long m = Math.max(0, (System.currentTimeMillis() - ts) / 60000);
        return m < 60 ? m + "m" : m < 1440 ? (m / 60) + "h" : (m / 1440) + "d";
    }

    static String kbps(double k) {
        return k >= 1000 ? String.format(Locale.US, "%.1f Mb/s", k / 1000) : String.format(Locale.US, "%d kb/s", (int) k);
    }

    List<NewsItem> list(int t) { return t == 0 ? d.world : d.myanmar; }

    // ---------------- time and phone numbers (updated rarely) ----------------
    void updateTime(long now) {
        long m = now / 60000;
        if (m == lastMinute) return;
        lastMinute = m;
        Calendar c = Calendar.getInstance();
        timeStr = String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        dateStr = new SimpleDateFormat("EEE, d MMM", Locale.US).format(c.getTime());
        monthStr = new SimpleDateFormat("MMMM yyyy", Locale.US).format(c.getTime()).toUpperCase(Locale.US);
        today = c.get(Calendar.DAY_OF_MONTH);
        dim = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar f = (Calendar) c.clone();
        f.set(Calendar.DAY_OF_MONTH, 1);
        firstDow = f.get(Calendar.DAY_OF_WEEK) - 1;
        double ph = ((now / 86400000.0 - 10962.76) / 29.530588) % 1.0;
        if (ph < 0) ph += 1.0;
        moonPct = (int) Math.round((1 - Math.cos(ph * 2 * Math.PI)) / 2 * 100);
        d.loadSteps(Prefs.sp(getContext()));
    }

    void updateSys(long now) {
        if (lastSys != 0 && now - lastSys < 3000) return;
        long dt = now - lastSys;
        lastSys = now;
        Context ctx = getContext();
        bat = Prefs.battery(ctx);
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.getMemoryInfo(memInfo);
            memPct = memInfo.totalMem > 0 ? (int) (100 * (memInfo.totalMem - memInfo.availMem) / memInfo.totalMem) : 0;
        }
        try {
            StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
            long tot = sf.getTotalBytes(), fr = sf.getAvailableBytes();
            stoPct = tot > 0 ? (int) (100 * (tot - fr) / tot) : 0;
        } catch (Exception e) { /* keep old value */ }
        long rx = TrafficStats.getTotalRxBytes(), tx = TrafficStats.getTotalTxBytes();
        if (rx >= 0 && lastRx > 0 && dt > 0 && dt < 60000 && rx >= lastRx) {
            rxKbps = (rx - lastRx) * 8.0 / 1000.0 / (dt / 1000.0);
            txKbps = Math.max(0, (tx - lastTx) * 8.0 / 1000.0 / (dt / 1000.0));
            System.arraycopy(netHist, 1, netHist, 0, netHist.length - 1);
            netHist[netHist.length - 1] = (float) rxKbps;
        }
        lastRx = rx;
        lastTx = tx;
    }

    // ---------------- drawing ----------------
    @Override protected void onDraw(Canvas c) {
        long now = System.currentTimeMillis();
        hn = 0;
        updateTime(now);
        updateSys(now);
        c.drawColor(0xFF030303);
        c.save();
        c.translate(ox, oy);
        c.scale(s, s);
        if (layout == 0) drawInner(c, now);
        else if (layout == 1) drawInnerPortrait(c, now);
        else if (layout == 2) drawCover(c, now);
        else drawCoverLandscape(c, now);
        c.restore();
    }

    void drawInner(Canvas c, long now) {
        text(c, "Julian's Fold", 16, 33, 19, WHITE, LEFT, true);
        hit(8, 8, 190, 34, A_MENU, 0);
        if (!running || (now / 700) % 2 == 0) { pf.setStyle(Paint.Style.FILL); pf.setColor(ORANGE); c.drawCircle(158, 27, 3f, pf); }
        text(c, "LIVE", 166, 31, 9, 0xC8FFFFFF, LEFT, false);
        String[] ch = {"BATTERY " + bat + "%", "MEMORY " + memPct + "%", "STORAGE " + stoPct + "%", "\u2193 " + kbps(rxKbps)};
        for (int i = 0; i < 4; i++) chip(c, 200 + i * 112, 10, 106, ch[i]);
        hit(200, 10, 448, 22, A_CARE, 0);
        text(c, timeStr, 868, 34, 34, WHITE, RIGHT, true);
        text(c, dateStr + "   \u2197 Clock", 868, 52, 11, 0xC8FFFFFF, RIGHT, false);
        hit(700, 4, 168, 60, A_CLOCK, 0);

        phonePanel(c, 16, 70, 224, 150);
        networkPanel(c, 16, 232, 224, 96);
        weatherPanel(c, 16, 340, 224, 128);
        calendarPanel(c, 16, 480, 224, 172);
        moonPanel(c, 16, 662, 224, 58);

        googleBar(c, 258, 70, 250);
        box(c, 516, 70, 112, 36, 18, 0xFF0B0F14, 0x30FFFFFF);
        text(c, "Layers \u25BE", 572, 93, 13, WHITE, CENTER, false);
        hit(516, 70, 112, 36, A_MENU, 0);
        globeWidget(c, 442, 292, 150f, 3, now);
        text(c, "drag to rotate \u00B7 tap a country for its news", 442, 454, 10, 0xB8FFFFFF, CENTER, false);
        breakingPanel(c, 258, 480, 368, 64);
        stepsPanel(c, 258, 552, 368, 92, false);
        dock(c, 442, 656, 5);

        newsPanel(c, 644, 70, 224, 300);
        stocksPanel(c, 644, 382, 224);
        tiles(c, 644, 534, 224, 70);
        alertsPanel(c, 644, 612, 224);
    }

    void drawCover(Canvas c, long now) {
        text(c, timeStr, CW / 2, 76, 60, WHITE, CENTER, true);
        text(c, dateStr + (d.hasW ? "  \u00B7  " + Math.round(d.temp) + "\u00B0C " + d.wdesc : ""), CW / 2, 100, 12, 0xC8FFFFFF, CENTER, false);
        hit(40, 16, 280, 92, A_CLOCK, 0);
        text(c, "\u22EE", 340, 28, 20, WHITE, CENTER, true);
        hit(322, 6, 36, 36, A_MENU, 0);
        String[] ch = {"BATT " + bat + "%", "MEM " + memPct + "%", "STOR " + stoPct + "%"};
        for (int i = 0; i < 3; i++) chip(c, 14 + i * 114, 120, 106, ch[i]);
        hit(14, 120, 332, 22, A_CARE, 0);
        globeWidget(c, 178, 288, 105f, 2, now);
        newsPanel(c, 12, 424, 336, 196);
        stepsPanel(c, 12, 630, 336, 74, true);
        stocksMini(c, 12, 712, 336, 52);
        dock(c, 180, 786, 4);
        googleBar(c, 12, 872, 336);
    }

    /** Unfolded, held upright: big clock and globe on top, then three columns (phone/network/weather, news, calendar/steps/stocks). */
    void drawInnerPortrait(Canvas c, long now) {
        text(c, "Julian's Fold", 16, 33, 19, WHITE, LEFT, true);
        hit(8, 8, 190, 34, A_MENU, 0);
        if (!running || (now / 700) % 2 == 0) { pf.setStyle(Paint.Style.FILL); pf.setColor(ORANGE); c.drawCircle(158, 27, 3f, pf); }
        text(c, "LIVE", 166, 31, 9, 0xC8FFFFFF, LEFT, false);
        String[] ch = {"BATTERY " + bat + "%", "MEMORY " + memPct + "%", "STORAGE " + stoPct + "%", "\u2193 " + kbps(rxKbps)};
        for (int i = 0; i < 4; i++) chip(c, 200 + i * 112, 10, 106, ch[i]);
        hit(200, 10, 448, 22, A_CARE, 0);

        // hero band: clock + weather + search on the left, globe on the right
        text(c, timeStr, 16, 138, 84, WHITE, LEFT, true);
        text(c, dateStr + "   \u2197 Clock", 20, 168, 13, 0xC8FFFFFF, LEFT, false);
        hit(10, 66, 380, 112, A_CLOCK, 0);
        if (d.hasW) {
            text(c, Math.round(d.temp) + "\u00B0C  " + d.wdesc, 20, 204, 22, WHITE, LEFT, true);
            text(c, "wind " + Math.round(d.wind) + " km/h \u00B7 " + d.hum + "%", 20, 224, 11, 0xC0FFFFFF, LEFT, false);
            hit(10, 182, 300, 48, A_WEATHER, 0);
        }
        googleBar(c, 16, 250, 250);
        box(c, 274, 250, 108, 36, 18, 0xFF0B0F14, 0x30FFFFFF);
        text(c, "Layers \u25BE", 328, 273, 13, WHITE, CENTER, false);
        hit(274, 250, 108, 36, A_MENU, 0);
        globeWidget(c, 612, 208, 138f, 3, now);
        text(c, "drag to rotate \u00B7 tap a country for its news", 612, 372, 10, 0xB8FFFFFF, CENTER, false);
        breakingPanel(c, 16, 384, 784, 64);

        // left column
        phonePanel(c, 16, 460, 252, 150);
        networkPanel(c, 16, 622, 252, 100);
        weatherPanel(c, 16, 734, 252, 140);
        // middle: news, tall
        newsPanel(c, 282, 460, 252, 414);
        // right column
        calendarPanel(c, 548, 460, 252, 170);
        stepsPanel(c, 548, 642, 252, 92, true);
        stocksPanel(c, 548, 746, 252);

        // bottom row
        tiles(c, 16, 896, 210, 70);
        dock(c, 408, 906, 5);
        alertsPanel(c, 590, 902, 210);
    }

    /** Folded (cover screen), held sideways: a wide, short screen. */
    void drawCoverLandscape(Canvas c, long now) {
        text(c, timeStr, 14, 62, 50, WHITE, LEFT, true);
        hit(8, 8, 200, 70, A_CLOCK, 0);
        text(c, dateStr + (d.hasW ? "  \u00B7  " + Math.round(d.temp) + "\u00B0C" : "") + "  \u00B7  " + bat + "%", 14, 84, 11, 0xC8FFFFFF, LEFT, false);
        text(c, "\u22EE", 262, 30, 20, WHITE, CENTER, true);
        hit(244, 6, 36, 36, A_MENU, 0);
        stepsPanel(c, 12, 96, 262, 84, true);
        stocksMini(c, 12, 188, 262, 46);
        googleBar(c, 12, 244, 262);
        globeWidget(c, 460, 130, 100f, 2, now);
        dock(c, 460, 286, 5);
        newsPanel(c, 646, 8, 266, 254);
        tiles(c, 646, 270, 266, 72);
    }

    void phonePanel(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, "THIS PHONE", "Battery care", false, A_CARE, 0);
        ring(c, x + 42, y + 68, 28, bat, ORANGE);
        text(c, bat + "%", x + 42, y + 73, 13, WHITE, CENTER, true);
        text(c, "Memory " + memPct + "%", x + 82, y + 50, 11, WHITE, LEFT, false);
        bar(c, x + 82, y + 56, w - 108, memPct);
        text(c, "Storage " + stoPct + "%", x + 82, y + 82, 11, WHITE, LEFT, false);
        bar(c, x + 82, y + 88, w - 108, stoPct);
        text(c, "Battery " + bat + "%", x + 10, y + h - 10, 10, 0xC8FFFFFF, LEFT, false);
    }

    void networkPanel(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, "NETWORK", "Data usage", false, A_DATA, 0);
        text(c, "\u2191 " + kbps(txKbps) + "   \u2193 " + kbps(rxKbps), x + 10, y + 38, 11, WHITE, LEFT, false);
        spark(c, x + 10, y + 48, w - 20, h - 58, netHist, WHITE);
    }

    void weatherPanel(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, "WEATHER", "Weather", false, A_WEATHER, 0);
        if (!d.hasW) { text(c, "Loading\u2026", x + 10, y + 52, 14, 0xC8FFFFFF, LEFT, false); return; }
        text(c, Math.round(d.temp) + "\u00B0C", x + 10, y + 52, 30, WHITE, LEFT, true);
        text(c, d.wdesc + " \u00B7 wind " + Math.round(d.wind) + " km/h \u00B7 " + d.hum + "%", x + 10, y + 72, 10, 0xC8FFFFFF, LEFT, false);
        for (int i = 0; i < 3; i++) {
            text(c, d.fday[i] == null ? "--" : d.fday[i], x + 10 + i * ((w - 20f) / 3f), y + 96, 10, 0xD0FFFFFF, LEFT, false);
            text(c, d.fhi[i] + "/" + d.flo[i] + "\u00B0", x + 10 + i * ((w - 20f) / 3f), y + 110, 10, WHITE, LEFT, false);
        }
    }

    void calendarPanel(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, monthStr, "Calendar", false, A_CAL, 0);
        String dn = "SMTWTFS";
        float cw = (w - 40f) / 6f, rh = Math.max(22f, Math.min(36f, (h - 70f) / 6f));
        for (int i = 0; i < 7; i++) text(c, dn.substring(i, i + 1), x + 20 + i * cw, y + 34, 10, 0xB8FFFFFF, CENTER, false);
        for (int day = 1; day <= dim; day++) {
            int idx = firstDow + day - 1, col = idx % 7, row = idx / 7;
            float cx = x + 20 + col * cw, cy = y + 56 + row * rh;
            if (day == today) {
                pf.setStyle(Paint.Style.FILL);
                pf.setColor(ORANGE);
                c.drawCircle(cx, cy, 10f, pf);
                text(c, String.valueOf(day), cx, cy + 4, 11, 0xFF000000, CENTER, true);
            } else {
                text(c, String.valueOf(day), cx, cy + 4, 11, 0xCCFFFFFF, CENTER, false);
            }
        }
    }

    void moonPanel(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, "MOON \u00B7 SUN", "Weather", false, A_WEATHER, 0);
        text(c, "Moon " + moonPct + "% \u00B7 rise " + d.sunrise + " \u00B7 set " + d.sunset, x + 10, y + 38, 10, 0xD0FFFFFF, LEFT, false);
    }

    void googleBar(Canvas c, float x, float y, float w) {
        box(c, x, y, w, 36, 18, 0xFF202124, 0x30FFFFFF);
        text(c, "G", x + 18, y + 25, 17, 0xFF4285F4, LEFT, true);
        text(c, "Search Google", x + 44, y + 23, 13, 0xC0FFFFFF, LEFT, false);
        hit(x, y, w, 36, A_GOOGLE, 0);
    }

    void globeWidget(Canvas c, float cx, float cy, float R, int maxCallouts, long now) {
        gcx = cx; gcy = cy; gR = R;
        globe.setView();
        globe.draw(c, cx, cy, R, optGrid);
        if (optHot) {
            int n = 0;
            for (int i = 0; i < d.hot.size() && n < maxCallouts; i++) {
                Data.Hot h = d.hot.get(i);
                globe.proj(h.lon, h.lat, tmp);
                if (tmp[2] < 0.25f) continue;
                float px = cx + R * tmp[0], py = cy - R * tmp[1];
                pulse(c, px, py, RED, now);
                float off = n % 2 == 0 ? 10f : -150f, dy = n % 2 == 0 ? -30f : 8f;
                pill(c, px + off, py + dy, h.name.toUpperCase(Locale.US) + "  " + ago(h.item.ts), RED, A_HOT, i);
                n++;
            }
        }
        if (!Double.isNaN(d.lat)) {
            globe.proj((float) d.lon, (float) d.lat, tmp);
            if (tmp[2] > 0.1f) {
                float px = cx + R * tmp[0], py = cy - R * tmp[1];
                pulse(c, px, py, BLUE, now);
                pill(c, px + 8, py + 10, "YOU ARE HERE", BLUE, 0, 0);
            }
        }
    }

    void breakingPanel(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, "BREAKING \u00B7 BY COUNTRY", "country news", false, 0, 0);
        if (d.hot.isEmpty()) { text(c, "No country headlines yet", x + 10, y + 36, 10, 0xB8FFFFFF, LEFT, false); return; }
        for (int i = 0; i < 3 && i < d.hot.size(); i++) {
            Data.Hot hh2 = d.hot.get(i);
            String t = hh2.item.title;
            int lim = Math.max(10, (int) ((w - 20) / 5.6f) - hh2.name.length() - 10);
            if (t.length() > lim) t = t.substring(0, lim - 1) + "\u2026";
            text(c, "\u25CF " + hh2.name.toUpperCase(Locale.US) + "  " + t + "  " + ago(hh2.item.ts), x + 10, y + 32 + i * 13, 10, 0xDDFFFFFF, LEFT, false);
            hit(x + 4, y + 22 + i * 13, w - 8, 13, A_HOT, i);
        }
    }

    String updStr() {
        if (d.newsUpd == 0) return "--:--";
        Calendar k = Calendar.getInstance();
        k.setTimeInMillis(d.newsUpd);
        return String.format(Locale.US, "%02d:%02d", k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE));
    }

    static String num(int n) { return String.format(Locale.US, "%,d", n); }

    void stepsPanel(Canvas c, float x, float y, float w, float h, boolean mini) {
        panel(c, x, y, w, h, "STEPS \u00B7 THIS WEEK", "Samsung Health", false, A_HEALTH, 0);
        int tdy = d.steps[d.todayIdx], total = 0;
        for (int v : d.steps) total += v;
        int avg = total / (d.todayIdx + 1);
        int pct = Math.min(100, tdy * 100 / Config.STEP_GOAL);
        boolean vertical = mini && w < 240f, compact = mini && w < 300f && !vertical;
        float gap, bw, maxh, bx, base;
        if (vertical) {
            gap = (w - 28f) / 7f; bw = 14f; base = y + h - 16f; maxh = Math.min(34f, h - 84f); bx = x + 14f + (gap - bw) / 2f;
        } else if (compact) {
            gap = 20f; bw = 11f; base = y + h - 18f; maxh = Math.min(30f, h - 50f); bx = x + w - 14f - 7 * gap + 3f;
        } else {
            gap = mini ? 22f : 24f; bw = mini ? 12f : 14f; maxh = mini ? 26f : 38f; bx = x + w - 14f - 7 * gap + 4f; base = y + h - (mini ? 20f : 22f);
        }
        if (vertical) {
            text(c, num(tdy) + " steps today", x + 12, y + 36, 13, WHITE, LEFT, true);
            text(c, "week " + num(total) + " \u00B7 avg " + num(avg), x + 12, y + 52, 10, 0xC8FFFFFF, LEFT, false);
        } else if (compact) {
            text(c, num(tdy) + " steps", x + 12, y + 38, 13, WHITE, LEFT, true);
            text(c, "week " + num(total), x + 12, y + 54, 10, 0xC8FFFFFF, LEFT, false);
            text(c, "avg " + num(avg), x + 12, y + 68, 10, 0xC8FFFFFF, LEFT, false);
        } else if (!mini) {
            ring(c, x + 34, y + 52, 22, pct, ORANGE);
            text(c, pct + "%", x + 34, y + 57, 12, WHITE, CENTER, true);
            text(c, num(tdy) + " steps", x + 70, y + 40, 15, WHITE, LEFT, true);
            text(c, "week " + num(total), x + 70, y + 58, 10, 0xC8FFFFFF, LEFT, false);
            text(c, "avg " + num(avg) + " a day", x + 70, y + 72, 10, 0xC8FFFFFF, LEFT, false);
        } else {
            text(c, num(tdy) + " steps today", x + 12, y + 36, 13, WHITE, LEFT, true);
            text(c, "week " + num(total) + " \u00B7 avg " + num(avg), x + 12, y + 54, 10, 0xC8FFFFFF, LEFT, false);
        }
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(1f);
        ps.setColor(0x40FFFFFF);
        ps.setPathEffect(dash);
        c.drawLine(bx - 4, base - maxh, bx + 7 * gap - 2, base - maxh, ps);
        ps.setPathEffect(null);
        String dn = "MTWTFSS";
        for (int i = 0; i < 7; i++) {
            float bh = Math.max(5f, Math.min(Config.STEP_GOAL, d.steps[i]) / (float) Config.STEP_GOAL * maxh);
            boolean isToday = i == d.todayIdx;
            pf.setStyle(Paint.Style.FILL);
            if (d.steps[i] > 0 || isToday) {
                pf.setColor(isToday ? ORANGE : 0xB8FFFFFF);
                c.drawRect(bx + i * gap, base - bh, bx + i * gap + bw, base, pf);
            } else {
                ps.setPathEffect(dash);
                ps.setColor(0x55FFFFFF);
                c.drawRect(bx + i * gap, base - 5, bx + i * gap + bw, base, ps);
                ps.setPathEffect(null);
            }
            text(c, dn.substring(i, i + 1), bx + i * gap + bw / 2, base + 12, 9, isToday ? ORANGE : 0xB0FFFFFF, CENTER, false);
        }
    }

    void stocksPanel(Canvas c, float x, float y, float w) {
        panel(c, x, y, w, 138, "STOCKS", "Finance", false, 0, 0);
        for (int i = 0; i < 4; i++) {
            float yy = y + 36 + i * 28;
            if (i < d.quotes.size()) {
                Data.Quote q = d.quotes.get(i);
                boolean up = q.chg >= 0;
                text(c, q.name, x + 10, yy, 11, WHITE, LEFT, false);
                spark(c, x + w / 2 - 34, yy - 12, 64, 16, q.spark, up ? GREEN : PINK);
                text(c, String.format(Locale.US, "%+.2f%%", q.chg), x + w - 10, yy, 11, up ? GREEN : PINK, RIGHT, false);
            } else {
                text(c, i < Config.STOCK_NAMES.length ? Config.STOCK_NAMES[i] : "", x + 10, yy, 11, 0x80FFFFFF, LEFT, false);
                text(c, "--", x + w - 10, yy, 11, 0x80FFFFFF, RIGHT, false);
            }
            hit(x, yy - 16, w, 26, A_FIN, i);
        }
    }

    void stocksMini(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, "STOCKS", "Finance", false, A_FIN, 0);
        for (int i = 0; i < 3; i++) {
            if (i < d.quotes.size()) {
                Data.Quote q = d.quotes.get(i);
                String lab = q.name + " " + String.format(Locale.US, "%+.1f%%", q.chg);
                text(c, lab, x + 12 + i * (w - 12) / 3f, y + 38, 11, q.chg >= 0 ? GREEN : PINK, LEFT, false);
            }
        }
    }

    /** One quick-open tile: the app's icon in the HUD style. Tap opens it, hold changes it. */
    void tile(Canvas c, float x, float y, int i) {
        box(c, x, y, 44, 44, 11, 0xFF11151B, 0x38FFFFFF);
        String pkg = host.tiles[i];
        Bitmap b = pkg == null ? null : host.apps.icon(pkg);
        if (b != null) {
            rf.set(x + 4, y + 4, x + 40, y + 40);
            c.drawBitmap(b, null, rf, pbmp);
        } else {
            text(c, "+", x + 22, y + 29, 18, ORANGE, CENTER, true);
        }
        hit(x, y, 44, 44, A_TILE, i);
    }

    void tiles(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, "QUICK OPEN", "tap open \u00B7 hold change", false, 0, 0);
        float gap = (w - 20f) / 4f;
        for (int i = 0; i < 4; i++) tile(c, x + 10 + i * gap + (gap - 44f) / 2f, y + 22, i);
    }

    void tilesCompact(Canvas c, float x, float y) {
        panel(c, x, y, 112, 118, "QUICK OPEN", null, false, 0, 0);
        for (int i = 0; i < 4; i++) tile(c, x + 8 + (i % 2) * 52, y + 24 + (i / 2) * 46, i);
    }

    void alertsPanel(Canvas c, float x, float y, float w) {
        panel(c, x, y, w, 64, "ALERTS", "Device care", false, A_CARE, 0);
        boolean a1 = bat < 20 || memPct > 90, a2 = stoPct > 90;
        String[] lab = {"BATTERY / MEM", "STORAGE"};
        boolean[] on = {a1, a2};
        for (int i = 0; i < 2; i++) {
            int col = on[i] ? RED : WHITE;
            ps.setStyle(Paint.Style.STROKE);
            ps.setStrokeWidth(2f);
            ps.setColor(col);
            c.drawCircle(x + 21 + i * 104, y + 41, 8f, ps);
            pf.setStyle(Paint.Style.FILL);
            pf.setColor(col);
            c.drawCircle(x + 21 + i * 104, y + 41, 3f, pf);
            text(c, lab[i], x + 36 + i * 104, y + 44, 9, 0xC8FFFFFF, LEFT, false);
        }
    }

    void wrap(NewsItem it, float w) {
        if (it.lines != null && it.linesW == w) return;
        pt.setTextSize(11);
        String t = it.title;
        String[] out = new String[]{"", ""};
        int start = 0;
        for (int ln = 0; ln < 2 && start < t.length(); ln++) {
            int n = pt.breakText(t, start, t.length(), true, w, null);
            if (n <= 0) break;
            int end = start + n;
            if (end < t.length() && ln == 0) {
                int sp = t.lastIndexOf(' ', end - 1);
                if (sp > start + 8) end = sp;
            }
            String piece = t.substring(start, end).trim();
            if (ln == 1 && end < t.length()) piece = piece.substring(0, Math.max(0, piece.length() - 1)) + "\u2026";
            out[ln] = piece;
            start = end;
        }
        it.lines = out;
        it.linesW = w;
    }

    void newsPanel(Canvas c, float x, float y, float w, float h) {
        panel(c, x, y, w, h, null, null, true, 0, 0);
        String[] names = {"WORLD", "MYANMAR"};
        float tx = x + 10;
        for (int i = 0; i < 2; i++) {
            boolean on = i == tab;
            pt.setTextSize(10);
            float tw = pt.measureText(names[i]);
            text(c, names[i], tx, y + 15, 10, on ? ORANGE : 0xB8FFFFFF, LEFT, on);
            if (on) { ps.setStyle(Paint.Style.STROKE); ps.setStrokeWidth(1f); ps.setColor(ORANGE); c.drawLine(tx, y + 19, tx + tw, y + 19, ps); }
            hit(tx - 3, y + 2, tw + 6, 22, A_TAB, i);
            tx += tw + 12;
        }
        text(c, "\u2197 article \u00B7 " + updStr(), x + w - 10, y + 15, 9, ORANGE, RIGHT, false);
        List<NewsItem> l = list(tab);
        int n = Math.min(l.size(), 30);
        float top = y + 34, vis = h - 46;
        visRows = vis / ROW;
        nX = x; nY = y; nW = w; nH = h;
        if (n == 0) { text(c, "Loading news\u2026", x + 12, top + 20, 11, 0xB8FFFFFF, LEFT, false); return; }
        c.save();
        c.clipRect(x + 8, top, x + w - 12, top + vis);
        int first = (int) Math.floor(newsScroll);
        for (int i = Math.max(0, first - 1); i < n && i < first + (int) visRows + 2; i++) {
            float ry = top + (i - newsScroll) * ROW;
            NewsItem it = l.get(i);
            wrap(it, w - 36);
            pf.setStyle(Paint.Style.FILL);
            pf.setColor(ORANGE);
            c.drawRect(x + 12, ry + 6, x + 17, ry + 11, pf);
            text(c, it.lines[0], x + 24, ry + 14, 11, 0xF2FFFFFF, LEFT, false);
            text(c, it.lines[1], x + 24, ry + 28, 11, 0xF2FFFFFF, LEFT, false);
            text(c, it.source + "  \u00B7  " + ago(it.ts), x + 24, ry + 44, 9, 0xAAFFFFFF, LEFT, false);
            ps.setStyle(Paint.Style.STROKE);
            ps.setStrokeWidth(1f);
            ps.setColor(0x1AFFFFFF);
            c.drawLine(x + 12, ry + ROW - 3, x + w - 16, ry + ROW - 3, ps);
            float y0 = Math.max(ry, top), y1 = Math.min(ry + ROW, top + vis);
            if (y1 - y0 > 14) hit(x + 8, y0, w - 20, y1 - y0, A_NEWS, i);
        }
        c.restore();
        float max = Math.max(0f, n - visRows);
        float th = Math.max(24f, vis * visRows / Math.max(n, visRows));
        pf.setStyle(Paint.Style.FILL);
        pf.setColor(0x26FFFFFF);
        c.drawRect(x + w - 6, top, x + w - 3, top + vis, pf);
        pf.setColor(ORANGE);
        float ty = top + (vis - th) * (max > 0 ? Math.min(1f, newsScroll / max) : 0f);
        c.drawRect(x + w - 6, ty, x + w - 3, ty + th, pf);
    }

    void dock(Canvas c, float cx, float y, int count) {
        float total = (count + 1) * 58f;
        box(c, cx - total / 2 - 6, y - 10, total + 12, 66, 26, 0xFF101318, 0x55FFFFFF);
        float x0 = cx - total / 2 + 7;
        for (int i = 0; i < count; i++) {
            String pkg = host.dock[i];
            Bitmap b = pkg == null ? null : host.apps.icon(pkg);
            float ix = x0 + i * 58;
            if (b != null) {
                rf.set(ix, y, ix + 44, y + 44);
                c.drawBitmap(b, null, rf, pbmp);
            } else {
                box(c, ix, y, 44, 44, 13, 0xFF171B22, 0x2EFFFFFF);
            }
            hit(ix - 6, y - 6, 56, 56, A_DOCK, i);
        }
        float bx = x0 + count * 58;
        box(c, bx, y, 44, 44, 13, 0xFF171B22, 0x2EFFFFFF);
        pf.setStyle(Paint.Style.FILL);
        pf.setColor(ORANGE);
        for (int r = 0; r < 3; r++) for (int q = 0; q < 3; q++) c.drawCircle(bx + 12 + q * 10, y + 12 + r * 10, 2.2f, pf);
        hit(bx - 6, y - 6, 56, 56, A_DRAWER, 0);
    }

    // ---------------- touch ----------------
    boolean inGlobe(float ux, float uy) { return Math.hypot(ux - gcx, uy - gcy) <= gR; }
    boolean inNews(float ux, float uy) { return ux >= nX && ux <= nX + nW && uy >= nY + 34 && uy <= nY + nH; }

    float lpX, lpY;
    boolean longFired;
    int lpAct, lpArg;
    final Runnable longPress = new Runnable() {
        @Override public void run() {
            if (moved || tmode == 1 || tmode == 2) return;
            longFired = true;
            if (lpAct == A_DOCK) host.editDock(lpArg);
            else if (lpAct == A_TILE) host.editTile(lpArg);
        }
    };

    @Override public boolean onTouchEvent(MotionEvent e) {
        float ux = (e.getX() - ox) / s, uy = (e.getY() - oy) / s;
        long now = System.currentTimeMillis();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = ux;
                downY = lastY = uy;
                moved = false;
                longFired = false;
                lastTouch = now;
                vLon = vLat = 0f;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                lpAct = 0;
                for (int i = hn - 1; i >= 0; i--) {
                    if (ux >= hx[i] && ux <= hx[i] + hw[i] && uy >= hy[i] && uy <= hy[i] + hh[i]) { lpAct = ha[i]; lpArg = hg[i]; break; }
                }
                tmode = inGlobe(ux, uy) ? 1 : (inNews(ux, uy) ? 2 : 0);
                if (tmode == 1 && lpAct == A_HOT) tmode = 0;           // a country callout is a button, not a drag
                if (lpAct == A_DOCK || lpAct == A_TILE) postDelayed(longPress, 500);
                ensureLoop();
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = ux - lastX, dy = uy - lastY;
                if (Math.hypot(ux - downX, uy - downY) > 5f) { moved = true; removeCallbacks(longPress); }
                lastTouch = now;
                if (tmode == 1 && moved) {
                    float dpp = (float) (180.0 / (Math.PI * gR));
                    globe.gLon = wrap(globe.gLon - dx * dpp);
                    globe.gLat = Math.max(-80f, Math.min(80f, globe.gLat + dy * dpp));
                    vLon = -dx * dpp;
                    vLat = dy * dpp;
                    invalidate();
                } else if (tmode == 2 && moved) {
                    float max = Math.max(0f, Math.min(list(tab).size(), 30) - visRows);
                    newsScroll = Math.max(0f, Math.min(max, newsScroll - dy / ROW));
                    newsTarget = newsScroll;
                    invalidate();
                }
                lastX = ux;
                lastY = uy;
                return true;
            }
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                lastTouch = now;
                if (longFired) {
                    longFired = false;
                } else if (!moved) {
                    tap(ux, uy);
                } else if (tmode == 0 && downY - uy > 90f) {
                    host.openDrawer();                              // swipe up opens the app drawer
                } else if (tmode == 2) {
                    newsTarget = newsScroll = Math.round(newsScroll);
                }
                tmode = 0;
                ensureLoop();
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                tmode = 0;
                return true;
            default:
                return true;
        }
    }

    static float wrap(float lon) { return ((lon + 540f) % 360f) - 180f; }

    boolean hitAt(float ux, float uy) {
        for (int i = hn - 1; i >= 0; i--) if (ux >= hx[i] && ux <= hx[i] + hw[i] && uy >= hy[i] && uy <= hy[i] + hh[i]) return true;
        return false;
    }

    void tap(float ux, float uy) {
        for (int i = hn - 1; i >= 0; i--) {
            if (ux >= hx[i] && ux <= hx[i] + hw[i] && uy >= hy[i] && uy <= hy[i] + hh[i]) { doAction(ha[i], hg[i]); return; }
        }
        if (inGlobe(ux, uy) && globe.unproject((ux - gcx) / gR, (gcy - uy) / gR, tmp)) {
            float la = tmp[0], lo = tmp[1];
            if (WorldData.onLand(la, lo)) {
                int id = WorldData.nearest(la, lo);
                if (id >= 0) host.showCountry(WorldData.NAMES[id], null);
            }
        }
    }

    void doAction(int a, int g) {
        switch (a) {
            case A_NEWS: {
                List<NewsItem> l = list(tab);
                if (g < l.size()) host.showNews(l.get(g));
                break;
            }
            case A_TAB:
                tab = g;
                newsScroll = newsTarget = 0f;
                lastStepAt = System.currentTimeMillis();
                invalidate();
                break;
            case A_DOCK: host.launchDock(g); break;
            case A_TILE: host.launchTile(g); break;
            case A_DRAWER: host.openDrawer(); break;
            case A_MENU: host.showMenu(); break;
            case A_HOT:
                if (g < d.hot.size()) { Data.Hot h = d.hot.get(g); host.showCountry(h.name, h.item); }
                break;
            default: Open.go(host, a, g);
        }
    }

    // ---------------- animation loop (runs only while the home screen is on screen) ----------------
    final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.currentTimeMillis();
            step(now);
            invalidate();
            boolean spin = optRot && mode != 0 && now - lastTouch < 60000;
            boolean busy = tmode == 1 || spin || Math.abs(vLon) + Math.abs(vLat) > 0.05f || Math.abs(newsTarget - newsScroll) > 0.01f;
            if (mode == 0 && !busy) { running = false; return; }
            postDelayed(this, (tmode == 1 || Math.abs(vLon) + Math.abs(vLat) > 0.05f || mode == 2) ? 33 : (spin ? 66 : 160));
        }
    };

    void step(long now) {
        float dt = Math.min(0.5f, (now - lastTick) / 1000f);
        lastTick = now;
        if (Math.abs(vLon) + Math.abs(vLat) > 0.05f && tmode != 1) {
            globe.gLon = wrap(globe.gLon + vLon);
            globe.gLat = Math.max(-80f, Math.min(80f, globe.gLat + vLat));
            vLon *= 0.93f;
            vLat *= 0.93f;
        } else if (mode != 0 && optRot && tmode != 1 && now - lastTouch > 2000 && now - lastTouch < 60000) {
            globe.gLon = wrap(globe.gLon - 6f * dt);
        }
        int n = Math.min(list(tab).size(), 30);
        float max = Math.max(0f, n - visRows);
        if (mode != 0 && now - lastTouch > 60000 && now - lastStepAt > 12000) {
            lastStepAt = now;
            if (newsTarget >= max - 0.01f) { tab = (tab + 1) % 2; newsScroll = newsTarget = 0f; }
            else newsTarget = Math.min(max, newsTarget + 3f);
        }
        newsScroll += (newsTarget - newsScroll) * 0.25f;
        if (Math.abs(newsTarget - newsScroll) < 0.01f) newsScroll = newsTarget;
    }

    void ensureLoop() {
        if (running) return;
        running = true;
        lastTick = System.currentTimeMillis();
        post(tick);
    }

    void startLoop() {
        mode = Prefs.mode(getContext());
        loadOpts();
        if (running) return;
        if (mode == 0) { invalidate(); return; }
        ensureLoop();
    }

    void stopLoop() {
        running = false;
        removeCallbacks(tick);
    }

    void onResumeVisible() {
        lastTouch = System.currentTimeMillis();
        lastSys = 0;
        lastMinute = -1;
        startLoop();
        invalidate();
    }

    void resetView() {
        globe.gLat = 22f;
        invalidate();
    }
}

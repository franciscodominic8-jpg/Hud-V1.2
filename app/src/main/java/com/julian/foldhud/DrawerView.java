package com.julian.foldhud;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * The app drawer, in the HUD style: header with search and clock, filter chips, category rail, a Recent row, the
 * A-Z grid, and (on wide screens) top used / suggested / mini HUD. The grid is a recycling GridView, so only visible
 * icons exist in memory.
 */
final class DrawerView extends LinearLayout {
    static final int ALL = 0, RECENT = 1, FREQ = 2, SAMSUNG = 3, WORK = 4, FAV = 5, COMM = 6, MEDIA = 7, TOOLS = 8, GAMES = 9, HIDDEN = 10;
    static final String[] CHIPS = {"All", "Recent", "Frequent", "Samsung", "Work"};
    static final int[] CHIP_MODE = {ALL, RECENT, FREQ, SAMSUNG, WORK};
    static final String[] RAIL_NAMES = {"All apps", "Favorites", "Communication", "Media", "Tools", "Samsung", "Games", "Hidden"};
    static final int[] RAIL_MODE = {ALL, FAV, COMM, MEDIA, TOOLS, SAMSUNG, GAMES, HIDDEN};
    static final int ORANGE = 0xFFFFA31A;

    final HomeActivity host;
    final AppIndex idx;
    final float dp;
    final GridView grid;
    final EditText search;
    final LinearLayout searchBox, head, searchRow, body, railList, center, right;
    final TextView title, count, clockT, dateT, hint;
    final LinearLayout clockBox;
    final HorizontalScrollView chipScroll;
    final TextView[] chips = new TextView[5];
    final TextView[] railName = new TextView[8], railNum = new TextView[8];
    final LinearLayout[] railRow = new LinearLayout[8];
    final Panel railPanel, recentPanel, allPanel, topPanel, sugPanel, miniPanel;
    final RecentRow recentRow;
    final TopUsed topUsed;
    final Suggested suggested;
    final MiniHud miniHud;
    final DockBar dockBar;
    final Rail rail;
    final BaseAdapter adapter;
    final ArrayList<AppIndex.App> shown = new ArrayList<>();
    final ArrayList<String> recentKeys = new ArrayList<>(), topKeys = new ArrayList<>(), sugKeys = new ArrayList<>();
    final Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG);
    final RectF rf = new RectF();
    final Handler clockH = new Handler();
    int fmode = ALL, lay = -1, layKey = -1;
    TextView foot;

    DrawerView(HomeActivity a, AppIndex i) {
        super(a);
        host = a;
        idx = i;
        dp = a.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setBackgroundColor(0xFA000000);
        setPadding(px(12), px(34), px(12), px(8));

        // ---- header: title | search | clock ----
        head = new LinearLayout(a);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tb = new LinearLayout(a);
        tb.setOrientation(VERTICAL);
        title = tv(a, "All apps", 22, 0xFFFFFFFF, true);
        count = tv(a, "", 10, 0x99FFFFFF, false);
        count.setTypeface(Typeface.MONOSPACE);
        tb.addView(title);
        tb.addView(count);
        head.addView(tb, new LinearLayout.LayoutParams(-2, -2));

        search = new EditText(a);
        search.setHint("Search apps");
        search.setHintTextColor(0x99FFFFFF);
        search.setTextColor(0xFFFFFFFF);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setBackground(null);
        search.setPadding(px(16), 0, px(4), 0);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { refresh(); }
            @Override public void afterTextChanged(Editable e) { }
        });
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent ev) { googleSearch(); return true; }
        });
        TextView g = tv(a, "G", 17, ORANGE, true);
        g.setPadding(px(14), px(8), px(16), px(8));
        g.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { googleSearch(); } });
        searchBox = new LinearLayout(a);
        searchBox.setOrientation(HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable sg = new GradientDrawable();
        sg.setColor(0xFF15181D);
        sg.setCornerRadius(px(22));
        sg.setStroke(1, 0x33FFFFFF);
        searchBox.setBackground(sg);
        searchBox.addView(search, new LinearLayout.LayoutParams(0, -1, 1f));
        searchBox.addView(g, new LinearLayout.LayoutParams(-2, -2));

        clockBox = new LinearLayout(a);
        clockBox.setOrientation(VERTICAL);
        clockBox.setGravity(Gravity.END);
        clockT = tv(a, "", 26, 0xFFFFFFFF, true);
        clockT.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        dateT = tv(a, "", 10, 0xB0FFFFFF, false);
        dateT.setGravity(Gravity.END);
        clockBox.addView(clockT);
        clockBox.addView(dateT);
        head.addView(clockBox, new LinearLayout.LayoutParams(-2, -2));
        addView(head, new LinearLayout.LayoutParams(-1, -2));

        searchRow = new LinearLayout(a);
        searchRow.setOrientation(HORIZONTAL);
        addView(searchRow, new LinearLayout.LayoutParams(-1, -2));

        // swipe down on the header closes the drawer
        final GestureDetector gd = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (vy > 900 && Math.abs(vy) > Math.abs(vx)) { host.closeDrawer(); return true; }
                return false;
            }
        });
        head.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) { gd.onTouchEvent(e); return true; }
        });

        // ---- chips ----
        LinearLayout chipRow = new LinearLayout(a);
        chipRow.setOrientation(HORIZONTAL);
        for (int k = 0; k < 5; k++) {
            final int mm = CHIP_MODE[k];
            TextView t = tv(a, CHIPS[k], 11, 0xB8FFFFFF, false);
            t.setTypeface(Typeface.MONOSPACE);
            t.setPadding(px(11), px(5), px(11), px(5));
            t.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { setMode(mm); } });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = px(8);
            chips[k] = t;
            chipRow.addView(t, lp);
        }
        chipScroll = new HorizontalScrollView(a);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.addView(chipRow);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = px(10);
        cp.bottomMargin = px(8);
        addView(chipScroll, cp);

        // ---- body ----
        body = new LinearLayout(a);
        body.setOrientation(HORIZONTAL);
        addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        railPanel = new Panel(a, "CATEGORIES");
        railList = new LinearLayout(a);
        railList.setOrientation(VERTICAL);
        for (int k = 0; k < 8; k++) {
            final int mm = RAIL_MODE[k];
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(px(10), px(11), px(10), px(11));
            railName[k] = tv(a, RAIL_NAMES[k], 12, 0xCCFFFFFF, false);
            railNum[k] = tv(a, "", 11, 0x99FFFFFF, false);
            railNum[k].setTypeface(Typeface.MONOSPACE);
            row.addView(railName[k], new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(railNum[k]);
            row.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { setMode(mm); } });
            railRow[k] = row;
            railList.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        railPanel.addView(railList, new FrameLayout.LayoutParams(-1, -2));
        hint = tv(a, "Hold an app for options", 9, 0x77FFFFFF, false);
        hint.setTypeface(Typeface.MONOSPACE);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2);
        hp.gravity = Gravity.BOTTOM;
        railPanel.addView(hint, hp);
        LinearLayout.LayoutParams rpp = new LinearLayout.LayoutParams(px(150), -1);
        rpp.rightMargin = px(8);
        body.addView(railPanel, rpp);

        center = new LinearLayout(a);
        center.setOrientation(VERTICAL);
        body.addView(center, new LinearLayout.LayoutParams(0, -1, 1f));

        recentPanel = new Panel(a, "RECENT");
        recentRow = new RecentRow(a);
        recentPanel.addView(recentRow, new FrameLayout.LayoutParams(-1, px(78)));
        center.addView(recentPanel, new LinearLayout.LayoutParams(-1, -2));

        allPanel = new Panel(a, "ALL APPS · A–Z");
        grid = new GridView(a);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(px(8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(new ColorDrawable(0));
        grid.setVerticalScrollBarEnabled(false);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-1, -1);
        gp.rightMargin = px(20);
        allPanel.addView(grid, gp);
        rail = new Rail(a);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(px(20), -1);
        rp.gravity = Gravity.END;
        allPanel.addView(rail, rp);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, 0, 1f);
        ap.topMargin = px(8);
        center.addView(allPanel, ap);

        right = new LinearLayout(a);
        right.setOrientation(VERTICAL);
        topPanel = new Panel(a, "TOP USED");
        topUsed = new TopUsed(a);
        topPanel.addView(topUsed, new FrameLayout.LayoutParams(-1, -1));
        sugPanel = new Panel(a, "SUGGESTED");
        suggested = new Suggested(a);
        sugPanel.addView(suggested, new FrameLayout.LayoutParams(-1, -1));
        miniPanel = new Panel(a, "MINI HUD");
        miniHud = new MiniHud(a);
        miniPanel.addView(miniHud, new FrameLayout.LayoutParams(-1, -1));
        right.addView(topPanel, new LinearLayout.LayoutParams(-1, 0, 1.15f));
        LinearLayout.LayoutParams sp2 = new LinearLayout.LayoutParams(-1, 0, 1f);
        sp2.topMargin = px(8);
        right.addView(sugPanel, sp2);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, 0, 1.25f);
        mp.topMargin = px(8);
        right.addView(miniPanel, mp);
        LinearLayout.LayoutParams rtp = new LinearLayout.LayoutParams(px(190), -1);
        rtp.leftMargin = px(8);
        body.addView(right, rtp);

        // ---- dock + hint ----
        dockBar = new DockBar(a);
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(-1, px(84));
        dp2.topMargin = px(8);
        addView(dockBar, dp2);
        foot = tv(a, "swipe down on the title or press Back to close · hold an app for options", 9, 0x77FFFFFF, false);
        foot.setGravity(Gravity.CENTER);
        addView(foot, new LinearLayout.LayoutParams(-1, -2));

        adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int p) { return shown.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View cv, ViewGroup parent) {
                if (cv == null) {
                    LinearLayout r = new LinearLayout(host);
                    r.setOrientation(VERTICAL);
                    r.setGravity(Gravity.CENTER_HORIZONTAL);
                    r.setPadding(0, px(3), 0, px(3));
                    ImageView iv = new ImageView(host);
                    r.addView(iv, new LinearLayout.LayoutParams(px(46), px(46)));
                    TextView t = tv(host, "", 10.5f, 0xFFFFFFFF, false);
                    t.setGravity(Gravity.CENTER);
                    t.setSingleLine(true);
                    t.setEllipsize(TextUtils.TruncateAt.END);
                    t.setPadding(px(2), px(3), px(2), 0);
                    r.addView(t, new LinearLayout.LayoutParams(-1, -2));
                    r.setTag(new View[]{iv, t});
                    cv = r;
                }
                View[] h = (View[]) cv.getTag();
                AppIndex.App a2 = shown.get(p);
                ((ImageView) h[0]).setImageBitmap(idx.icon(a2));
                ((TextView) h[1]).setText(a2.label);
                return cv;
            }
        };
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override public void onItemClick(android.widget.AdapterView<?> pa, View v, int pos, long id) {
                idx.launch(shown.get(pos));
                host.closeDrawer();
            }
        });
        grid.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(android.widget.AdapterView<?> pa, View v, int pos, long id) {
                itemMenu(shown.get(pos));
                return true;
            }
        });
        applyLayout(0, false);
        styleAll();
        refresh();
    }

    // ---------------- helpers ----------------
    int px(float v) { return (int) (v * dp + 0.5f); }

    TextView tv(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    void drawIcon(Canvas c, Bitmap b, float x, float y, float s) {
        if (b == null) return;
        rf.set(x, y, x + s, y + s);
        c.drawBitmap(b, null, rf, bp);
    }

    void googleSearch() {
        String q = search.getText().toString().trim();
        Open.web(host, q.isEmpty() ? "https://www.google.com" : "https://www.google.com/search?q=" + Uri.encode(q));
        host.closeDrawer();
    }

    void setMode(int m) {
        fmode = m;
        refresh();
        styleAll();
        grid.setSelection(0);
    }

    // ---------------- layout by screen width ----------------
    @Override protected void onSizeChanged(final int w, int h, int ow, int oh) {
        final int nl = w / dp >= 780f ? 2 : w / dp >= 520f ? 1 : 0;
        final boolean sh = h / dp < 480f;          // cover screen held sideways: very short
        post(new Runnable() { @Override public void run() { applyLayout(nl, sh); } });
    }

    void applyLayout(int nl, boolean sh) {
        int w = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        float wdp = w / dp;
        int key = nl * 2 + (sh ? 1 : 0);
        if (key != layKey) {
            layKey = key;
            lay = nl;
            boolean wide = nl >= 1;
            setPadding(px(12), sh ? px(14) : px(34), px(12), px(8));
            railPanel.setVisibility(wide && !sh ? VISIBLE : GONE);
            right.setVisibility(nl == 2 && !sh ? VISIBLE : GONE);
            recentPanel.setVisibility(sh ? GONE : VISIBLE);
            dockBar.setVisibility(sh ? GONE : VISIBLE);
            foot.setVisibility(sh ? GONE : VISIBLE);
            clockBox.setVisibility(wide ? VISIBLE : GONE);
            ViewGroup par = (ViewGroup) searchBox.getParent();
            if (par != null) par.removeView(searchBox);
            if (wide) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(44), 1f);
                lp.leftMargin = px(18);
                lp.rightMargin = px(18);
                head.addView(searchBox, 1, lp);
                searchRow.setVisibility(GONE);
            } else {
                searchRow.addView(searchBox, new LinearLayout.LayoutParams(-1, px(44)));
                searchRow.setVisibility(VISIBLE);
                ((LinearLayout.LayoutParams) searchRow.getLayoutParams()).topMargin = px(10);
            }
            dockBar.invalidate();
            requestLayout();
        }
        float avail = wdp - 24 - 16 - 20 - (railPanel.getVisibility() == VISIBLE ? 158 : 0) - (right.getVisibility() == VISIBLE ? 198 : 0);
        grid.setNumColumns(Math.max(4, (int) (avail / 76f)));
    }

    void styleAll() {
        for (int k = 0; k < 5; k++) {
            boolean sel = CHIP_MODE[k] == fmode;
            chips[k].setTextColor(sel ? ORANGE : 0xB8FFFFFF);
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(px(8));
            d.setColor(sel ? 0x22FFA31A : 0x10FFFFFF);
            d.setStroke(px(1), sel ? ORANGE : 0x33FFFFFF);
            chips[k].setBackground(d);
            chips[k].setVisibility(k == 4 && !idx.hasWork() ? GONE : VISIBLE);
        }
        for (int k = 0; k < 8; k++) {
            boolean sel = RAIL_MODE[k] == fmode;
            railName[k].setTextColor(sel ? 0xFFFFFFFF : 0xCCFFFFFF);
            railName[k].setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            railNum[k].setTextColor(sel ? ORANGE : 0x99FFFFFF);
            railRow[k].setBackgroundColor(sel ? 0x26FFA31A : 0);
        }
    }

    // ---------------- open / close ----------------
    final Runnable clockTick = new Runnable() {
        @Override public void run() {
            Date d = new Date();
            clockT.setText(new SimpleDateFormat("HH:mm", Locale.US).format(d));
            dateT.setText(new SimpleDateFormat("EEE, d MMM", Locale.US).format(d));
            clockH.postDelayed(this, 20000);
        }
    };

    void onOpen() {
        fmode = ALL;
        search.setText("");
        styleAll();
        refresh();
        grid.setSelection(0);
        clockH.removeCallbacks(clockTick);
        clockTick.run();
    }

    void onClose() {
        clockH.removeCallbacks(clockTick);
        InputMethodManager im = (InputMethodManager) host.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (im != null) im.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    // ---------------- the lists ----------------
    boolean inMode(AppIndex.App a, int m) {
        boolean hid = idx.isHidden(a.key);
        if (m == HIDDEN) return hid;
        if (hid) return false;
        switch (m) {
            case SAMSUNG: return a.samsung;
            case WORK: return !a.mine;
            case FAV: return idx.isFav(a.key);
            case COMM: return a.cat == AppIndex.CAT_COMM;
            case MEDIA: return a.cat == AppIndex.CAT_MEDIA;
            case TOOLS: return a.cat == AppIndex.CAT_TOOLS;
            case GAMES: return a.cat == AppIndex.CAT_GAMES;
            case RECENT: return idx.last(a.key) > 0;
            case FREQ: return idx.count(a.key) > 0;
            default: return true;
        }
    }

    void refresh() {
        String q = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        shown.clear();
        int[] cnt = new int[11];
        ArrayList<AppIndex.App> top = new ArrayList<>();
        for (AppIndex.App a : idx.apps) {
            for (int m = 0; m <= HIDDEN; m++) if (m != RECENT && m != FREQ && inMode(a, m)) cnt[m]++;
            if (!idx.isHidden(a.key)) top.add(a);
            if (inMode(a, fmode) && (q.isEmpty() || a.label.toLowerCase(Locale.getDefault()).contains(q))) shown.add(a);
        }
        if (fmode == RECENT) Collections.sort(shown, (x, y) -> Long.compare(idx.last(y.key), idx.last(x.key)));
        else if (fmode == FREQ) Collections.sort(shown, (x, y) -> Integer.compare(idx.count(y.key), idx.count(x.key)));
        adapter.notifyDataSetChanged();

        for (int k = 0; k < 8; k++) railNum[k].setText(String.valueOf(cnt[RAIL_MODE[k]]));
        String modeName = "";
        for (int k = 0; k < 8; k++) if (RAIL_MODE[k] == fmode) modeName = RAIL_NAMES[k];
        if (fmode == RECENT) modeName = "Recent";
        if (fmode == FREQ) modeName = "Frequent";
        if (fmode == WORK) modeName = "Work / Dual";
        boolean alpha = fmode != RECENT && fmode != FREQ;
        title.setText(fmode == ALL ? "All apps" : modeName);
        count.setText(shown.size() + " apps · " + (alpha ? "A–Z" : fmode == RECENT ? "latest first" : "most used"));
        allPanel.setTitle((modeName.isEmpty() ? "ALL APPS" : modeName.toUpperCase(Locale.US)) + (alpha ? " · A–Z" : ""));

        // recent / top used / suggested
        recentKeys.clear();
        topKeys.clear();
        sugKeys.clear();
        ArrayList<AppIndex.App> t = new ArrayList<>(top);
        Collections.sort(t, (x, y) -> Long.compare(idx.last(y.key), idx.last(x.key)));
        for (AppIndex.App a : t) if (idx.last(a.key) > 0 && recentKeys.size() < 5) recentKeys.add(a.key);
        Collections.sort(t, (x, y) -> Integer.compare(idx.count(y.key), idx.count(x.key)));
        for (AppIndex.App a : t) if (idx.count(a.key) > 0 && topKeys.size() < 4) topKeys.add(a.key);
        final int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        Collections.sort(t, (x, y) -> Integer.compare(hourScore(y.key, hr), hourScore(x.key, hr)));
        for (AppIndex.App a : t) if (hourScore(a.key, hr) > 0 && sugKeys.size() < 3) sugKeys.add(a.key);
        recentRow.invalidate();
        topUsed.invalidate();
        suggested.invalidate();
        miniHud.invalidate();
        dockBar.invalidate();
        rail.invalidate();
    }

    int hourScore(String key, int hr) {
        return idx.hourCount(key, hr) * 2 + idx.hourCount(key, (hr + 23) % 24) + idx.hourCount(key, (hr + 1) % 24);
    }

    // ---------------- long-press menu ----------------
    void itemMenu(final AppIndex.App a) {
        final boolean fav = idx.isFav(a.key), hid = idx.isHidden(a.key);
        final String[] items = {"Add to dock", "Add to quick open", fav ? "Remove from favorites" : "Add to favorites",
                hid ? "Unhide" : "Hide", "App info", a.mine ? "Uninstall" : "Manage (dual app)"};
        new AlertDialog.Builder(host, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle(a.label + (a.mine ? "" : "  (dual)"))
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dlg, int which) {
                        if (which == 0) pickSlot(a);
                        else if (which == 1) pickTile(a);
                        else if (which == 2) { idx.toggleFav(a.key); refresh(); }
                        else if (which == 3) { idx.toggleHidden(a.key); refresh(); styleAll(); }
                        else if (which == 4) idx.appInfo(a);
                        else if (a.mine) Open.start(host, new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg)));
                        else idx.appInfo(a);
                    }
                }).show();
    }

    void pickTile(final AppIndex.App a) {
        new AlertDialog.Builder(host, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Put " + a.label + " in quick open")
                .setItems(new CharSequence[]{"Tile 1", "Tile 2", "Tile 3", "Tile 4"}, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dlg, int which) { host.setTile(which, a.key); }
                }).show();
    }

    void pickSlot(final AppIndex.App a) {
        new AlertDialog.Builder(host, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Put " + a.label + " in dock slot")
                .setItems(new CharSequence[]{"Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5 (not shown on cover screen)"}, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dlg, int which) { host.setDock(which, a.key); }
                }).show();
    }

    // ---------------- HUD panel with corner brackets ----------------
    final class Panel extends FrameLayout {
        String title;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Panel(Context c, String t) {
            super(c);
            title = t;
            setWillNotDraw(false);
            setPadding(px(8), px(24), px(8), px(8));
            p.setTypeface(Typeface.MONOSPACE);
        }

        void setTitle(String t) { title = t; invalidate(); }

        @Override protected void dispatchDraw(Canvas c) {
            super.dispatchDraw(c);
            float w = getWidth(), h = getHeight(), L = px(11);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f);
            p.setColor(0x24FFFFFF);
            c.drawRect(0.5f, 0.5f, w - 0.5f, h - 0.5f, p);
            p.setStrokeWidth(px(1.6f));
            p.setColor(0xF0FFFFFF);
            c.drawLine(0, 0, L, 0, p); c.drawLine(0, 0, 0, L, p);
            c.drawLine(w, 0, w - L, 0, p); c.drawLine(w, 0, w, L, p);
            c.drawLine(0, h, L, h, p); c.drawLine(0, h, 0, h - L, p);
            c.drawLine(w, h, w - L, h, p); c.drawLine(w, h, w, h - L, p);
            p.setStyle(Paint.Style.FILL);
            p.setTextSize(px(9.5f));
            p.setColor(0xCCFFFFFF);
            c.drawText(title, px(10), px(16), p);
        }
    }

    // ---------------- recent row ----------------
    final class RecentRow extends View {
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        int cap = 4;

        RecentRow(Context c) {
            super(c);
            tp.setTextSize(px(10));
            tp.setColor(0xCCFFFFFF);
            tp.setTextAlign(Paint.Align.CENTER);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(px(1.6f));
            ring.setColor(ORANGE);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth();
            cap = Math.max(3, Math.min(5, (int) (w / px(74))));
            float slot = w / cap;
            if (recentKeys.isEmpty()) {
                tp.setTextAlign(Paint.Align.LEFT);
                tp.setColor(0x88FFFFFF);
                c.drawText("Apps you open will show up here", px(4), px(34), tp);
                tp.setTextAlign(Paint.Align.CENTER);
                tp.setColor(0xCCFFFFFF);
                return;
            }
            for (int i = 0; i < Math.min(cap, recentKeys.size()); i++) {
                String k = recentKeys.get(i);
                float cx = slot * (i + 0.5f), x = cx - px(23), y = px(4);
                drawIcon(c, idx.icon(k), x, y, px(46));
                if (i == 0) {
                    rf.set(x - px(3), y - px(3), x + px(49), y + px(49));
                    c.drawRoundRect(rf, px(16), px(16), ring);
                }
                c.drawText(TextUtils.ellipsize(idx.label(k), tp, slot - px(6), TextUtils.TruncateAt.END).toString(), cx, px(66), tp);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
            if (e.getAction() == MotionEvent.ACTION_UP) {
                int i = (int) (e.getX() / (getWidth() / (float) cap));
                if (i >= 0 && i < recentKeys.size() && i < cap) { idx.launchPkg(recentKeys.get(i)); host.closeDrawer(); }
            }
            return true;
        }
    }

    // ---------------- top used ----------------
    final class TopUsed extends View {
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);

        TopUsed(Context c) { super(c); tp.setTextSize(px(11)); }

        @Override protected void onDraw(Canvas c) {
            if (topKeys.isEmpty()) {
                tp.setColor(0x88FFFFFF);
                tp.setTextAlign(Paint.Align.LEFT);
                c.drawText("Open some apps", px(2), px(16), tp);
                c.drawText("to see usage", px(2), px(32), tp);
                return;
            }
            int mx = Math.max(1, idx.count(topKeys.get(0)));
            float w = getWidth(), rowH = Math.min(px(34), getHeight() / 4f);
            for (int i = 0; i < topKeys.size(); i++) {
                String k = topKeys.get(i);
                float y = px(12) + i * rowH;
                tp.setColor(0xFFFFFFFF);
                tp.setTextAlign(Paint.Align.LEFT);
                c.drawText(TextUtils.ellipsize(idx.label(k), tp, w - px(44), TextUtils.TruncateAt.END).toString(), px(2), y, tp);
                tp.setColor(0xB0FFFFFF);
                tp.setTextAlign(Paint.Align.RIGHT);
                c.drawText(idx.count(k) + "×", w - px(2), y, tp);
                bar.setColor(0x26FFFFFF);
                c.drawRect(px(2), y + px(5), w - px(2), y + px(8), bar);
                bar.setColor(ORANGE);
                c.drawRect(px(2), y + px(5), px(2) + (w - px(4)) * idx.count(k) / mx, y + px(8), bar);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float rowH = Math.min(px(34), getHeight() / 4f);
                int i = (int) ((e.getY() - px(2)) / rowH);
                if (i >= 0 && i < topKeys.size()) { idx.launchPkg(topKeys.get(i)); host.closeDrawer(); }
            }
            return true;
        }
    }

    // ---------------- suggested ----------------
    final class Suggested extends View {
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        Suggested(Context c) { super(c); tp.setTextSize(px(10)); tp.setTextAlign(Paint.Align.CENTER); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            tp.setColor(0x88FFFFFF);
            if (sugKeys.isEmpty()) {
                tp.setTextAlign(Paint.Align.LEFT);
                c.drawText("Learns from what you open", px(2), px(16), tp);
                c.drawText("at each time of day", px(2), px(30), tp);
                tp.setTextAlign(Paint.Align.CENTER);
                return;
            }
            float slot = Math.min(px(58), w / 3f);
            for (int i = 0; i < sugKeys.size(); i++) {
                String k = sugKeys.get(i);
                float cx = slot * (i + 0.5f);
                drawIcon(c, idx.icon(k), cx - px(20), px(2), px(40));
                tp.setColor(0xCCFFFFFF);
                c.drawText(TextUtils.ellipsize(idx.label(k), tp, slot - px(2), TextUtils.TruncateAt.END).toString(), cx, px(56), tp);
            }
            tp.setColor(0x77FFFFFF);
            tp.setTextAlign(Paint.Align.LEFT);
            c.drawText("Based on time of day", px(2), h - px(2), tp);
            tp.setTextAlign(Paint.Align.CENTER);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float slot = Math.min(px(58), getWidth() / 3f);
                int i = (int) (e.getX() / slot);
                if (i >= 0 && i < sugKeys.size() && e.getY() < px(62)) { idx.launchPkg(sugKeys.get(i)); host.closeDrawer(); }
            }
            return true;
        }
    }

    // ---------------- mini HUD: still globe + time + temperature ----------------
    final class MiniHud extends View {
        final GlobeRenderer globe = new GlobeRenderer();
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

        MiniHud(Context c) { super(c); tp.setTextAlign(Paint.Align.CENTER); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            Data d = host.data;
            float R = Math.min(w * 0.36f, (h - px(40)) / 2f);
            if (R < px(12)) return;
            globe.gLon = Double.isNaN(d.lon) ? 55f : (float) d.lon;
            globe.gLat = Double.isNaN(d.lat) ? 25f : (float) d.lat;
            globe.setView();
            float cx = w / 2f, cy = R + px(6);
            globe.draw(c, cx, cy, R, true);
            dot.setColor(0xFFFF3B3B);
            c.drawCircle(cx, cy, px(3), dot);
            tp.setColor(0xFFFFFFFF);
            tp.setTypeface(Typeface.DEFAULT_BOLD);
            tp.setTextSize(px(12));
            String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date())
                    + (d.hasW ? "  ·  " + Math.round(d.temp) + "°C" : "");
            c.drawText(t, cx, cy + R + px(18), tp);
            tp.setColor(0x99FFFFFF);
            tp.setTypeface(Typeface.MONOSPACE);
            tp.setTextSize(px(9));
            c.drawText(d.city == null ? "" : d.city.toUpperCase(Locale.US), cx, cy + R + px(31), tp);
        }
    }

    // ---------------- dock at the bottom ----------------
    final class DockBar extends View {
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
        int n = 5;
        float x0, step;

        DockBar(Context c) { super(c); tp.setTextSize(px(9)); tp.setTextAlign(Paint.Align.CENTER); }

        @Override protected void onDraw(Canvas c) {
            n = lay == 0 ? 4 : 5;
            step = px(62);
            float total = n * step, w = getWidth();
            x0 = (w - total) / 2f;
            rf.set(x0 - px(10), px(2), x0 + total + px(10), getHeight() - px(2));
            pill.setStyle(Paint.Style.FILL);
            pill.setColor(0xFF14171C);
            c.drawRoundRect(rf, px(28), px(28), pill);
            pill.setStyle(Paint.Style.STROKE);
            pill.setStrokeWidth(1f);
            pill.setColor(0x44FFFFFF);
            c.drawRoundRect(rf, px(28), px(28), pill);
            for (int i = 0; i < n; i++) {
                String k = host.dock[i];
                if (k == null) continue;
                float cx = x0 + step * (i + 0.5f);
                drawIcon(c, idx.icon(k), cx - px(22), px(8), px(44));
                tp.setColor(0xB0FFFFFF);
                c.drawText(TextUtils.ellipsize(idx.label(k), tp, step - px(4), TextUtils.TruncateAt.END).toString(), cx, px(66), tp);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
            if (e.getAction() == MotionEvent.ACTION_UP) {
                int i = (int) ((e.getX() - x0) / step);
                if (e.getX() >= x0 && i >= 0 && i < n && host.dock[i] != null) { idx.launchPkg(host.dock[i]); host.closeDrawer(); }
            }
            return true;
        }
    }

    // ---------------- A-Z strip ----------------
    final class Rail extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rail(Context c) { super(c); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.MONOSPACE); }

        @Override protected void onDraw(Canvas c) {
            boolean alpha = fmode != RECENT && fmode != FREQ;
            p.setColor(alpha ? 0xC0FFA31A : 0x40FFA31A);
            p.setTextSize(px(9));
            float step = getHeight() / 26f;
            for (int i = 0; i < 26; i++) c.drawText(String.valueOf((char) ('A' + i)), getWidth() / 2f, step * (i + 0.75f), p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (fmode == RECENT || fmode == FREQ) return true;
            int i = (int) (e.getY() / (getHeight() / 26f));
            i = Math.max(0, Math.min(25, i));
            char ch = (char) ('A' + i);
            for (int k = 0; k < shown.size(); k++) {
                String l = shown.get(k).label;
                if (!l.isEmpty() && Character.toUpperCase(l.charAt(0)) >= ch) { grid.setSelection(k); break; }
            }
            return true;
        }
    }
}

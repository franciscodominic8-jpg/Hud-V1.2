package com.julian.foldhud;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** The app drawer: search, All / Recent / Frequent, and an A-Z rail. Uses a recycling GridView, so only visible icons exist. */
final class DrawerView extends LinearLayout {
    final HomeActivity host;
    final AppIndex idx;
    final GridView grid;
    final EditText search;
    final TextView[] chips = new TextView[3];
    final ArrayList<AppIndex.App> shown = new ArrayList<>();
    final BaseAdapter adapter;
    final float dp;
    int fmode;

    DrawerView(HomeActivity a, AppIndex i) {
        super(a);
        host = a;
        idx = i;
        dp = a.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setBackgroundColor(0xF5000000);
        setPadding(px(12), px(36), px(12), px(8));

        search = new EditText(a);
        search.setHint("Search apps");
        search.setHintTextColor(0x99FFFFFF);
        search.setTextColor(0xFFFFFFFF);
        search.setSingleLine(true);
        search.setTextSize(14);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF202124);
        g.setCornerRadius(px(22));
        g.setStroke(1, 0x30FFFFFF);
        search.setBackground(g);
        search.setPadding(px(18), px(8), px(18), px(8));
        addView(search, new LinearLayout.LayoutParams(-1, px(46)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { refresh(); }
            @Override public void afterTextChanged(Editable e) { }
        });

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(HORIZONTAL);
        String[] names = {"All", "Recent", "Frequent"};
        for (int k = 0; k < 3; k++) {
            final int mm = k;
            TextView t = new TextView(a);
            t.setText(names[k]);
            t.setTextSize(12);
            t.setPadding(px(14), px(8), px(14), px(8));
            t.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) { fmode = mm; refresh(); styleChips(); }
            });
            chips[k] = t;
            row.addView(t);
        }
        addView(row, new LinearLayout.LayoutParams(-2, -2));

        FrameLayout body = new FrameLayout(a);
        grid = new GridView(a);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(px(10));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(new ColorDrawable(0));
        grid.setVerticalScrollBarEnabled(false);
        body.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        Rail rail = new Rail(a);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(px(22), -1);
        rp.gravity = Gravity.END;
        body.addView(rail, rp);
        addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int p) { return shown.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View cv, ViewGroup parent) {
                if (cv == null) {
                    LinearLayout r = new LinearLayout(host);
                    r.setOrientation(VERTICAL);
                    r.setGravity(Gravity.CENTER_HORIZONTAL);
                    r.setPadding(0, px(4), 0, px(4));
                    ImageView iv = new ImageView(host);
                    r.addView(iv, new LinearLayout.LayoutParams(px(46), px(46)));
                    TextView tv = new TextView(host);
                    tv.setTextColor(0xFFFFFFFF);
                    tv.setTextSize(11);
                    tv.setGravity(Gravity.CENTER);
                    tv.setSingleLine(true);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    r.addView(tv, new LinearLayout.LayoutParams(-1, -2));
                    r.setTag(new View[]{iv, tv});
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
        styleChips();
        refresh();
    }

    int px(float v) { return (int) (v * dp + 0.5f); }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        grid.setNumColumns(Math.max(4, (int) (w / dp / 84f)));
    }

    void styleChips() {
        for (int k = 0; k < 3; k++) chips[k].setTextColor(k == fmode ? 0xFFFFA31A : 0xB8FFFFFF);
    }

    void onOpen() {
        fmode = 0;
        search.setText("");
        styleChips();
        refresh();
        grid.setSelection(0);
    }

    void onClose() {
        InputMethodManager im = (InputMethodManager) host.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (im != null) im.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    void refresh() {
        shown.clear();
        String q = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        ArrayList<AppIndex.App> src = new ArrayList<>(idx.apps);
        if (fmode == 1) {
            ArrayList<AppIndex.App> r = new ArrayList<>();
            for (AppIndex.App a : src) if (idx.last(a.pkg) > 0) r.add(a);
            Collections.sort(r, (x, y) -> Long.compare(idx.last(y.pkg), idx.last(x.pkg)));
            src = r;
        } else if (fmode == 2) {
            ArrayList<AppIndex.App> r = new ArrayList<>();
            for (AppIndex.App a : src) if (idx.count(a.pkg) > 0) r.add(a);
            Collections.sort(r, (x, y) -> Integer.compare(idx.count(y.pkg), idx.count(x.pkg)));
            src = r;
        }
        for (AppIndex.App a : src) if (q.isEmpty() || a.label.toLowerCase(Locale.getDefault()).contains(q)) shown.add(a);
        adapter.notifyDataSetChanged();
    }

    void itemMenu(final AppIndex.App a) {
        new AlertDialog.Builder(host).setTitle(a.label)
                .setItems(new CharSequence[]{"Add to dock", "Add to quick open", "App info", "Uninstall"}, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dlg, int which) {
                        if (which == 0) pickSlot(a);
                        else if (which == 1) pickTile(a);
                        else if (which == 2) Open.start(host, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.pkg)));
                        else Open.start(host, new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg)));
                    }
                }).show();
    }

    void pickTile(final AppIndex.App a) {
        new AlertDialog.Builder(host).setTitle("Put " + a.label + " in quick open tile")
                .setItems(new CharSequence[]{"Tile 1", "Tile 2", "Tile 3", "Tile 4"}, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dlg, int which) { host.setTile(which, a.pkg); }
                }).show();
    }

    void pickSlot(final AppIndex.App a) {
        new AlertDialog.Builder(host).setTitle("Put " + a.label + " in dock slot")
                .setItems(new CharSequence[]{"Slot 1", "Slot 2", "Slot 3", "Slot 4 (cover screen shows 1-4)", "Slot 5"}, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dlg, int which) { host.setDock(which, a.pkg); }
                }).show();
    }

    /** A-Z strip on the right edge: drag a finger along it to jump to a letter. */
    final class Rail extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rail(Context c) { super(c); p.setTextAlign(Paint.Align.CENTER); }

        @Override protected void onDraw(Canvas c) {
            p.setColor(0xB0FFA31A);
            p.setTextSize(px(9));
            float step = getHeight() / 26f;
            for (int i = 0; i < 26; i++) c.drawText(String.valueOf((char) ('A' + i)), getWidth() / 2f, step * (i + 0.75f), p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
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

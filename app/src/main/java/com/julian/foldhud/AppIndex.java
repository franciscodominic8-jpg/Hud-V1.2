package com.julian.foldhud;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The list of installed apps, their small cached icons (restyled to match the HUD), and how often you open them. */
final class AppIndex {
    static final class App { String label, pkg; ComponentName cn; }
    static final String[] STYLE_NAMES = {"HUD orange", "HUD white", "HUD colours", "Original"};

    final Context ctx;
    final PackageManager pm;
    final SharedPreferences sp;
    ArrayList<App> apps = new ArrayList<>();
    int style;   // 0 HUD orange, 1 HUD white, 2 HUD colours, 3 original icons
    private final LruCache<String, Bitmap> icons = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };

    AppIndex(Context c) {
        ctx = c;
        pm = c.getPackageManager();
        sp = Prefs.sp(c);
        style = sp.getInt("icons", 0);
        reload();
    }

    void setStyle(int s) {
        style = s;
        sp.edit().putInt("icons", s).apply();
        icons.evictAll();
    }

    void reload() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> l = pm.queryIntentActivities(i, 0);
        ArrayList<App> out = new ArrayList<>();
        for (ResolveInfo r : l) {
            ActivityInfo ai = r.activityInfo;
            if (ai.packageName.equals(ctx.getPackageName())) continue;
            App a = new App();
            a.label = String.valueOf(r.loadLabel(pm));
            a.pkg = ai.packageName;
            a.cn = new ComponentName(ai.packageName, ai.name);
            out.add(a);
        }
        Collections.sort(out, (x, y) -> x.label.compareToIgnoreCase(y.label));
        apps = out;
    }

    Bitmap icon(String pkg, ComponentName cn) {
        Bitmap b = icons.get(pkg);
        if (b != null) return b;
        Drawable d;
        try {
            d = cn != null ? pm.getActivityIcon(cn) : pm.getApplicationIcon(pkg);
        } catch (Exception e) { return null; }
        int sz = 96;
        if (style == 3) {
            b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, sz, sz);
            d.draw(new Canvas(b));
        } else {
            b = themed(d, sz, style);
        }
        icons.put(pkg, b);
        return b;
    }

    Bitmap icon(App a) { return icon(a.pkg, a.cn); }
    Bitmap icon(String pkg) { return icon(pkg, null); }

    /** Dark rounded tile with a thin border and the app's symbol in the HUD colour, like the widgets. */
    private Bitmap themed(Drawable d, int sz, int st) {
        Bitmap b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = sz * 0.28f;
        p.setColor(0xFF171B22);
        c.drawRoundRect(0f, 0f, sz, sz, r, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sz * 0.03f);
        p.setColor(0x44FFFFFF);
        c.drawRoundRect(sz * 0.015f, sz * 0.015f, sz * 0.985f, sz * 0.985f, r, r, p);
        int tint = st == 0 ? 0xFFFFA31A : st == 1 ? 0xFFFFFFFF : 0;
        boolean done = false;
        if (d instanceof AdaptiveIconDrawable) {
            Drawable fg = ((AdaptiveIconDrawable) d).getForeground();
            if (fg != null) {
                Bitmap g = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
                fg.setBounds(0, 0, sz, sz);
                fg.draw(new Canvas(g));
                if (coverage(g) < 0.8f) {                      // a real symbol, not a full coloured square
                    Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                    if (tint != 0) gp.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN));
                    c.drawBitmap(g, 0f, 0f, gp);
                    done = true;
                }
                g.recycle();
            }
        }
        if (!done) {                                            // old-style or full-colour icon: shrink it into the tile
            Bitmap f = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, sz, sz);
            d.draw(new Canvas(f));
            Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            if (tint != 0) {
                float tr = ((tint >> 16) & 255) / 255f, tg = ((tint >> 8) & 255) / 255f, tb = (tint & 255) / 255f;
                float lr = 0.30f * 1.35f, lg = 0.59f * 1.35f, lb = 0.11f * 1.35f;
                fp.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                        lr * tr, lg * tr, lb * tr, 0, 40 * tr,
                        lr * tg, lg * tg, lb * tg, 0, 40 * tg,
                        lr * tb, lg * tb, lb * tb, 0, 40 * tb,
                        0, 0, 0, 1, 0})));
            }
            float in = sz * 0.17f;
            c.drawBitmap(f, null, new RectF(in, in, sz - in, sz - in), fp);
            f.recycle();
        }
        return b;
    }

    private static float coverage(Bitmap g) {
        int n = 0, solid = 0, w = g.getWidth();
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                int x = (int) (w * (0.2f + 0.6f * i / 19f)), y = (int) (w * (0.2f + 0.6f * j / 19f));
                n++;
                if ((g.getPixel(x, y) >>> 24) > 200) solid++;
            }
        }
        return solid / (float) n;
    }

    int count(String pkg) { return sp.getInt("n_" + pkg, 0); }
    long last(String pkg) { return sp.getLong("l_" + pkg, 0); }

    void launchPkg(String pkg) {
        Intent i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return;
        sp.edit().putInt("n_" + pkg, count(pkg) + 1).putLong("l_" + pkg, System.currentTimeMillis()).apply();
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(i); } catch (Exception e) { /* app cannot be opened */ }
    }

    void launch(App a) { launchPkg(a.pkg); }
}

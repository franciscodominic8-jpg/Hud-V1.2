package com.julian.foldhud;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
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
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.LruCache;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The list of installed apps (including Dual Messenger / work-profile copies), their small cached icons drawn in the
 * HUD icon pack, and how often you open them. An app is identified by its "key": the package name, or
 * "package#profile" for the copy that lives in another profile.
 */
final class AppIndex {
    static final int CAT_TOOLS = 4, CAT_COMM = 2, CAT_MEDIA = 3, CAT_GAMES = 6;

    static final class App {
        String label, pkg, key;
        ComponentName cn;
        UserHandle user;
        boolean mine = true;       // false = the copy in the Dual Messenger / work profile
        boolean samsung;
        int cat = CAT_TOOLS;
    }

    // icon packs
    static final String[] STYLE_NAMES = {"HUD icons · orange", "HUD icons · white", "HUD letters",
            "Tinted originals", "Original"};

    final Context ctx;
    final PackageManager pm;
    final SharedPreferences sp;
    ArrayList<App> apps = new ArrayList<>();
    final HashMap<String, App> byKey = new HashMap<>();
    int style;
    final HashSet<String> hidden = new HashSet<>(), favs = new HashSet<>();
    private final LruCache<String, Bitmap> icons = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };

    AppIndex(Context c) {
        ctx = c;
        pm = c.getPackageManager();
        sp = Prefs.sp(c);
        style = Math.max(0, Math.min(STYLE_NAMES.length - 1, sp.getInt("pack", 2)));
        loadSet("hidden", hidden);
        loadSet("favs", favs);
        reload();
    }

    private void loadSet(String k, HashSet<String> s) {
        s.clear();
        String v = sp.getString(k, "");
        if (!v.isEmpty()) Collections.addAll(s, v.split(","));
    }

    private void saveSet(String k, HashSet<String> s) {
        StringBuilder b = new StringBuilder();
        for (String x : s) b.append(b.length() > 0 ? "," : "").append(x);
        sp.edit().putString(k, b.toString()).apply();
    }

    boolean isHidden(String key) { return hidden.contains(key); }
    boolean isFav(String key) { return favs.contains(key); }

    void toggleHidden(String key) {
        if (!hidden.remove(key)) hidden.add(key);
        saveSet("hidden", hidden);
    }

    void toggleFav(String key) {
        if (!favs.remove(key)) favs.add(key);
        saveSet("favs", favs);
    }

    void setStyle(int s) {
        style = s;
        sp.edit().putInt("pack", s).apply();
        icons.evictAll();
    }

    boolean hasWork() {
        for (App a : apps) if (!a.mine) return true;
        return false;
    }

    // ---------------- the list ----------------
    private String sig = "";

    /** Like reload(), but also redraws icons (an app was updated or changed). */
    boolean forceReload() { sig = ""; return reload(); }

    /** Re-reads the installed apps. Returns true when the list changed (new, removed or updated app). */
    boolean reload() {
        ArrayList<App> out = new ArrayList<>();
        HashMap<String, App> map = new HashMap<>();
        try {
            LauncherApps la = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager um = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            UserHandle me = Process.myUserHandle();
            for (UserHandle uh : um.getUserProfiles()) {
                boolean mine = uh.equals(me);
                long serial = um.getSerialNumberForUser(uh);
                List<LauncherActivityInfo> l = la.getActivityList(null, uh);
                for (LauncherActivityInfo li : l) {
                    String pkg = li.getComponentName().getPackageName();
                    if (pkg.equals(ctx.getPackageName())) continue;
                    String key = mine ? pkg : pkg + "#" + serial;
                    String lab = String.valueOf(li.getLabel());
                    if (map.containsKey(key)) {                 // a second launcher entry of the same package (different name): list it too
                        boolean same = false;
                        for (App o : out) if (o.pkg.equals(pkg) && o.mine == mine && o.label.equals(lab)) same = true;
                        if (same) continue;
                        key = key + "/" + li.getComponentName().getClassName();
                        if (map.containsKey(key)) continue;
                    }
                    App a = new App();
                    a.label = lab;
                    a.pkg = pkg;
                    a.key = key;
                    a.cn = li.getComponentName();
                    a.user = uh;
                    a.mine = mine;
                    classify(a, li.getApplicationInfo());
                    out.add(a);
                    map.put(key, a);
                }
            }
        } catch (Exception e) { /* fall back below */ }
        if (out.isEmpty()) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
                ActivityInfo ai = r.activityInfo;
                if (ai.packageName.equals(ctx.getPackageName()) || map.containsKey(ai.packageName)) continue;
                App a = new App();
                a.label = String.valueOf(r.loadLabel(pm));
                a.pkg = ai.packageName;
                a.key = ai.packageName;
                a.cn = new ComponentName(ai.packageName, ai.name);
                classify(a, ai.applicationInfo);
                out.add(a);
                map.put(a.key, a);
            }
        }
        Collections.sort(out, (x, y) -> {
            int c = x.label.compareToIgnoreCase(y.label);
            return c != 0 ? c : Boolean.compare(!x.mine, !y.mine);
        });
        StringBuilder sg = new StringBuilder();
        for (App a : out) sg.append(a.key).append('|').append(a.label).append(';');
        if (sg.toString().equals(sig)) return false;
        sig = sg.toString();
        apps = out;
        byKey.clear();
        byKey.putAll(map);
        icons.evictAll();
        return true;
    }

    private void classify(App a, ApplicationInfo ai) {
        a.samsung = a.pkg.startsWith("com.samsung") || a.pkg.startsWith("com.sec.");
        int cat = ApplicationInfo.CATEGORY_UNDEFINED;
        boolean game = false;
        if (ai != null) {
            cat = ai.category;
            game = (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0 || cat == ApplicationInfo.CATEGORY_GAME;
        }
        String g = Glyphs.match(a.pkg, a.label);
        if (game) a.cat = CAT_GAMES;
        else if (cat == ApplicationInfo.CATEGORY_SOCIAL || cat == ApplicationInfo.CATEGORY_NEWS
                || "phone".equals(g) || "messages".equals(g) || "mail".equals(g) || "contacts".equals(g)) a.cat = CAT_COMM;
        else if (cat == ApplicationInfo.CATEGORY_AUDIO || cat == ApplicationInfo.CATEGORY_VIDEO
                || cat == ApplicationInfo.CATEGORY_IMAGE || "music".equals(g) || "video".equals(g)
                || "gallery".equals(g) || "camera".equals(g)) a.cat = CAT_MEDIA;
        else a.cat = CAT_TOOLS;
    }

    // ---------------- icons ----------------
    Bitmap icon(App a) { return icon(a.key); }

    Bitmap icon(String key) {
        Bitmap b = icons.get(key);
        if (b != null) return b;
        App a = byKey.get(key);
        String pkg = a != null ? a.pkg : key.split("#")[0];
        String label = a != null ? a.label : pkg;
        boolean dual = a != null && !a.mine;
        Drawable d = null;
        try {
            if (style >= 3) {
                if (a != null && a.cn != null) d = pm.getActivityIcon(a.cn);
                else d = pm.getApplicationIcon(pkg);
            }
        } catch (Exception e) { d = null; }
        int sz = 96;
        if (style == 4 && d != null) {
            b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, sz, sz);
            d.draw(new Canvas(b));
        } else if (style == 3 && d != null) {
            b = themed(d, sz, 0xFFFFA31A);
        } else {
            b = hud(sz, Glyphs.match(pkg, label), label, style == 1 ? 0xFFFFFFFF : 0xFFFFA31A, style == 2);
        }
        if (dual) badge(b);
        icons.put(key, b);
        return b;
    }

    private static Paint tilePaint() { return new Paint(Paint.ANTI_ALIAS_FLAG); }

    private static void tile(Canvas c, int sz) {
        Paint p = tilePaint();
        float r = sz * 0.28f;
        p.setColor(0xFF171B22);
        c.drawRoundRect(0f, 0f, sz, sz, r, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sz * 0.03f);
        p.setColor(0x55FFFFFF);
        c.drawRoundRect(sz * 0.015f, sz * 0.015f, sz * 0.985f, sz * 0.985f, r, r, p);
    }

    /** HUD pack: a thin line symbol for known apps, or a two-letter monogram, on a dark bordered tile. */
    private Bitmap hud(int sz, String glyph, String label, int color, boolean lettersOnly) {
        Bitmap b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        tile(c, sz);
        if (!lettersOnly && glyph != null && Glyphs.draw(c, glyph, sz, color)) return b;
        Paint p = tilePaint();
        p.setColor(color);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(sz * 0.40f);
        c.drawText(monogram(label), sz / 2f, sz / 2f + sz * 0.14f, p);
        return b;
    }

    static String monogram(String label) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < label.length() && s.length() < 2; ) {
            int cp = label.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isLetterOrDigit(cp)) continue;
            if (cp > 127) { if (s.length() == 0) s.appendCodePoint(cp); break; }   // non-Latin scripts: one letter
            s.appendCodePoint(s.length() == 0 ? Character.toUpperCase(cp) : Character.toLowerCase(cp));
        }
        return s.length() == 0 ? "?" : s.toString();
    }

    private void badge(Bitmap b) {
        Canvas c = new Canvas(b);
        Paint p = tilePaint();
        float w = b.getWidth(), r = w * 0.17f;
        p.setColor(0xFFFFA31A);
        c.drawCircle(w - r - w * 0.02f, w - r - w * 0.02f, r, p);
        p.setColor(0xFF000000);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(r * 1.35f);
        c.drawText("2", w - r - w * 0.02f, w - r - w * 0.02f + r * 0.48f, p);
    }

    /** Tinted originals: the app's own symbol recoloured to orange on a dark tile. */
    private Bitmap themed(Drawable d, int sz, int tint) {
        Bitmap b = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        tile(c, sz);
        boolean done = false;
        if (d instanceof AdaptiveIconDrawable) {
            Drawable fg = ((AdaptiveIconDrawable) d).getForeground();
            if (fg != null) {
                Bitmap g = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
                fg.setBounds(0, 0, sz, sz);
                fg.draw(new Canvas(g));
                if (coverage(g) < 0.8f) {                      // a real symbol, not a full coloured square
                    Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                    gp.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN));
                    c.drawBitmap(g, 0f, 0f, gp);
                    done = true;
                }
                g.recycle();
            }
        }
        if (!done) {
            Bitmap f = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, sz, sz);
            d.draw(new Canvas(f));
            Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            float tr = ((tint >> 16) & 255) / 255f, tg = ((tint >> 8) & 255) / 255f, tb = (tint & 255) / 255f;
            float lr = 0.30f * 1.35f, lg = 0.59f * 1.35f, lb = 0.11f * 1.35f;
            fp.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                    lr * tr, lg * tr, lb * tr, 0, 40 * tr,
                    lr * tg, lg * tg, lb * tg, 0, 40 * tg,
                    lr * tb, lg * tb, lb * tb, 0, 40 * tb,
                    0, 0, 0, 1, 0})));
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

    // ---------------- usage ----------------
    int count(String key) { return sp.getInt("n_" + key, 0); }
    long last(String key) { return sp.getLong("l_" + key, 0); }
    int hourCount(String key, int hour) { return sp.getInt("h_" + key + "_" + hour, 0); }

    String label(String key) {
        App a = byKey.get(key);
        return a != null ? a.label : key.split("#")[0];
    }

    void launchPkg(String key) {
        App a = byKey.get(key);
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        sp.edit().putInt("n_" + key, count(key) + 1).putLong("l_" + key, System.currentTimeMillis())
                .putInt("h_" + key + "_" + hr, hourCount(key, hr) + 1).apply();
        try {
            if (a != null && !a.mine && a.user != null) {
                ((LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE)).startMainActivity(a.cn, a.user, null, null);
                return;
            }
            Intent i = a != null && a.cn != null
                    ? new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(a.cn)
                    : pm.getLaunchIntentForPackage(key.split("#")[0]);
            if (i == null) return;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            ctx.startActivity(i);
        } catch (Exception e) { /* app cannot be opened */ }
    }

    void launch(App a) { launchPkg(a.key); }

    /** App info page; works for the dual copy too. */
    void appInfo(App a) {
        try {
            if (!a.mine && a.user != null) {
                ((LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE)).startAppDetailsActivity(a.cn, a.user, null, null);
                return;
            }
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + a.pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) { /* not available */ }
    }
}

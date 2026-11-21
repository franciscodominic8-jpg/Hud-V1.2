package com.julian.foldhud;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/** Draws the whole Earth (continents, grid, glow) with plain canvas calls. Nothing is allocated while drawing. */
final class GlobeRenderer {
    float gLon = 40f, gLat = 22f;           // the point on Earth facing you
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] tmp = new float[3];
    private float ct = 1f, st = 0f, rot = 0f;

    void setView() {
        double t = Math.toRadians(gLat);
        ct = (float) Math.cos(t);
        st = (float) Math.sin(t);
        rot = -gLon;
    }

    /** o = {x, y, depth}: x right and y up in -1..1, depth &gt; 0 means facing you. */
    void proj(float lon, float lat, float[] o) {
        double a = Math.toRadians(lat), b = Math.toRadians(lon + rot);
        float X = (float) (Math.cos(a) * Math.sin(b)), Y = (float) Math.sin(a), Z = (float) (Math.cos(a) * Math.cos(b));
        o[0] = X;
        o[1] = Y * ct - Z * st;
        o[2] = Z * ct + Y * st;
    }

    /** Screen offset (unit circle) to latitude/longitude in out[0], out[1]. */
    boolean unproject(float dx, float dy, float[] out) {
        float r2 = dx * dx + dy * dy;
        if (r2 > 1f) return false;
        float z = (float) Math.sqrt(1f - r2);
        float Y = dy * ct + z * st, Z = z * ct - dy * st;
        out[0] = (float) Math.toDegrees(Math.asin(Math.max(-1f, Math.min(1f, Y))));
        float lon = (float) Math.toDegrees(Math.atan2(dx, Z)) - rot;
        out[1] = ((lon % 360f) + 540f) % 360f - 180f;
        return true;
    }

    void draw(Canvas c, float cx, float cy, float R, boolean grid) {
        WorldData.init();
        // blue glow around the planet
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 8; i++) {
            float f = 1f - i / 8f;
            p.setStrokeWidth(R * 0.022f);
            p.setColor(Color.argb((int) (150 * f * f), 70, 150, 255));
            c.drawCircle(cx, cy, R * (1.0f + 0.02f * i), p);
        }
        // ocean
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF060C13);
        c.drawCircle(cx, cy, R, p);
        // latitude / longitude lines
        if (grid) {
            path.reset();
            for (int la = -60; la <= 60; la += 30) {
                boolean on = false;
                for (int lo = -180; lo <= 180; lo += 6) {
                    proj(lo, la, tmp);
                    if (tmp[2] > 0) {
                        float x = cx + R * tmp[0], y = cy - R * tmp[1];
                        if (on) path.lineTo(x, y); else path.moveTo(x, y);
                        on = true;
                    } else on = false;
                }
            }
            for (int lo = -180; lo < 180; lo += 30) {
                boolean on = false;
                for (int la = -90; la <= 90; la += 6) {
                    proj(lo, la, tmp);
                    if (tmp[2] > 0) {
                        float x = cx + R * tmp[0], y = cy - R * tmp[1];
                        if (on) path.lineTo(x, y); else path.moveTo(x, y);
                        on = true;
                    } else on = false;
                }
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(0.8f);
            p.setColor(0x337CC2FF);
            c.drawPath(path, p);
        }
        // continents
        path.reset();
        for (float[] P : WorldData.DENSE) {
            int n = P.length / 2;
            boolean any = false;
            for (int i = 0; i < n && !any; i++) {
                proj(P[2 * i], P[2 * i + 1], tmp);
                if (tmp[2] > 0) any = true;
            }
            if (!any) continue;
            boolean first = true, prevHid = false;
            float prevAng = 0f;
            for (int i = 0; i < n; i++) {
                proj(P[2 * i], P[2 * i + 1], tmp);
                boolean hid = tmp[2] < 0;
                float x = tmp[0], y = tmp[1];
                if (hid) {                                   // behind the globe: slide the point onto the edge
                    float m = (float) Math.hypot(x, y);
                    if (m < 1e-4f) m = 1e-4f;
                    x /= m;
                    y /= m;
                }
                float ang = (float) Math.atan2(y, x);
                if (first) {
                    path.moveTo(cx + R * x, cy - R * y);
                    first = false;
                } else if (hid && prevHid) {                  // follow the edge between two hidden points
                    float d = ang - prevAng;
                    while (d > Math.PI) d -= 2 * Math.PI;
                    while (d < -Math.PI) d += 2 * Math.PI;
                    int nn = (int) Math.ceil(Math.abs(d) / 0.1f);
                    for (int s = 1; s <= nn; s++) {
                        float a = prevAng + d * s / nn;
                        path.lineTo(cx + R * (float) Math.cos(a), cy - R * (float) Math.sin(a));
                    }
                } else {
                    path.lineTo(cx + R * x, cy - R * y);
                }
                prevHid = hid;
                prevAng = ang;
            }
            path.close();
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF2F3743);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.9f);
        p.setColor(0x669FB0C4);
        c.drawPath(path, p);
        // darker edge for a round look
        for (int i = 0; i < 8; i++) {
            float f = i / 7f;
            p.setStrokeWidth(R * 0.05f);
            p.setColor(Color.argb((int) (150 * f * f), 0, 0, 0));
            c.drawCircle(cx, cy, R * (0.64f + 0.05f * i), p);
        }
        p.setStrokeWidth(2f);
        p.setColor(0xE682C3FF);
        c.drawCircle(cx, cy, R, p);
    }
}

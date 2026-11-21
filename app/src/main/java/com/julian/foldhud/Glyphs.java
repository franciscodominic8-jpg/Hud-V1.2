package com.julian.foldhud;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.util.Locale;

/** The HUD icon pack: thin orange line symbols drawn from small vector recipes (24 x 24 grid). Nothing is stored as images. */
final class Glyphs {
    // recipe language: M x y, L x y, C x1 y1 x2 y2 x y, Q x1 y1 x y, Z, O cx cy r (ring), D cx cy r (dot), R x y w h r (rounded box)
    static final String[][] SPEC = {
        {"phone", "M 6.2 3.8 L 9 3.8 L 10.4 8 L 8.4 9.6 C 9.6 12.2 11.8 14.4 14.4 15.6 L 16 13.6 L 20.2 15 L 20.2 17.8 C 20.2 19.2 19.2 20.2 17.8 20.2 C 10 20 4 14 3.8 6.2 C 3.8 4.8 4.8 3.8 6.2 3.8 Z"},
        {"messages", "R 3.5 4.5 17 12.5 3 M 8 17 L 8 20.5 L 12 17 M 8 9 L 16 9 M 8 12.5 L 13 12.5"},
        {"camera", "R 3 7 18 12.5 2.5 O 12 13.2 3.6 M 8.5 7 L 9.6 4.5 L 14.4 4.5 L 15.5 7"},
        {"browser", "O 12 12 8.6 M 3.4 12 L 20.6 12 M 12 3.4 C 7.6 8 7.6 16 12 20.6 M 12 3.4 C 16.4 8 16.4 16 12 20.6"},
        {"maps", "M 12 21 C 6 14 5 12 5 9.5 C 5 5.5 8 3 12 3 C 16 3 19 5.5 19 9.5 C 19 12 18 14 12 21 Z O 12 9.5 2.6"},
        {"mail", "R 3 5.5 18 13 2 M 3.8 7.2 L 12 13.4 L 20.2 7.2"},
        {"gallery", "R 3.5 4.5 17 15 2.5 D 9 9.5 1.5 M 4 17 L 9.5 12.2 L 13 15 L 16 12.5 L 20 16.4"},
        {"clock", "O 12 12 8.6 M 12 7 L 12 12 L 15.6 14.2"},
        {"calendar", "R 3.5 5.5 17 15 2.5 M 3.5 10 L 20.5 10 M 8 3.5 L 8 7 M 16 3.5 L 16 7 D 8.5 14 0.9 D 12 14 0.9 D 15.5 14 0.9 D 8.5 17.2 0.9 D 12 17.2 0.9"},
        {"settings", "O 12 12 3.2 O 12 12 6.8 M 18.80 12.00 L 21.30 12.00 M 16.81 16.81 L 18.58 18.58 M 12.00 18.80 L 12.00 21.30 M 7.19 16.81 L 5.42 18.58 M 5.20 12.00 L 2.70 12.00 M 7.19 7.19 L 5.42 5.42 M 12.00 5.20 L 12.00 2.70 M 16.81 7.19 L 18.58 5.42 "},
        {"files", "M 3.5 7 L 3.5 18 C 3.5 19 4 19.5 5 19.5 L 19 19.5 C 20 19.5 20.5 19 20.5 18 L 20.5 9.5 C 20.5 8.5 20 8 19 8 L 11.5 8 L 9.5 5.2 L 5 5.2 C 4 5.2 3.5 5.7 3.5 6.7 Z"},
        {"calc", "R 5 3.5 14 17 2.5 R 7.6 6.2 8.8 3.4 0.6 D 8.6 13 0.9 D 12 13 0.9 D 15.4 13 0.9 D 8.6 16.6 0.9 D 12 16.6 0.9 D 15.4 16.6 0.9"},
        {"music", "M 9 17.5 L 9 5.8 L 19 3.8 L 19 15.5 O 6.6 17.5 2.4 O 16.6 15.5 2.4"},
        {"store", "M 5.2 8 L 18.8 8 L 19.6 20 L 4.4 20 Z M 8.8 8 C 8.8 3.6 15.2 3.6 15.2 8"},
        {"contacts", "O 12 8.4 3.6 M 4.8 20 C 4.8 15 8.4 13.4 12 13.4 C 15.6 13.4 19.2 15 19.2 20"},
        {"health", "M 12 20 C 4 14.2 3 9.6 5.6 6.6 C 8 4 11 5 12 7.6 C 13 5 16 4 18.4 6.6 C 21 9.6 20 14.2 12 20 Z"},
        {"notes", "R 5 3.5 14 17 2 M 8.4 8.4 L 15.6 8.4 M 8.4 12 L 15.6 12 M 8.4 15.6 L 12.6 15.6"},
        {"weather", "O 8.6 8.6 2.8 M 8.6 3 L 8.6 4 M 3 8.6 L 4 8.6 M 4.6 4.6 L 5.3 5.3 M 12.6 4.6 L 11.9 5.3 M 7 19.5 C 4.2 19.5 3.2 16.4 5.6 14.8 C 6 12.2 10 11.4 11.6 13.6 C 13.6 11.8 17.4 12.8 17.4 15.8 C 20.2 15.8 20.8 19.5 17.6 19.5 Z"},
        {"video", "R 3 6 18 12 3.5 M 10 9.4 L 15 12 L 10 14.6 Z"},
        {"game", "M 6.2 8.8 L 17.8 8.8 C 20.4 8.8 21.6 14 20.6 17 C 19.6 19.2 17.6 18.6 16.6 17 L 15 15.4 L 9 15.4 L 7.4 17 C 6.4 18.6 4.4 19.2 3.4 17 C 2.4 14 3.6 8.8 6.2 8.8 Z M 7.6 11.4 L 7.6 14 M 6.3 12.7 L 8.9 12.7 D 15.6 11.6 0.8 D 17.6 13.6 0.8"},
        {"mic", "R 9.5 3.5 5 10 2.5 M 6.5 11.4 C 6.5 17 17.5 17 17.5 11.4 M 12 16.6 L 12 20.5"},
        {"tasks", "R 4 4 16 16 3.5 M 8 12.2 L 11 15.2 L 16.4 9"},
        {"wallet", "R 3.5 6.5 17 13 2.5 M 3.5 10.4 L 20.5 10.4 D 16.4 14.6 1"},
        {"lock", "R 5.5 10.5 13 10 2 M 8.6 10.5 L 8.6 8 C 8.6 3.6 15.4 3.6 15.4 8 L 15.4 10.5 D 12 15.4 1.1"},
        {"search", "O 10.5 10.5 6.4 M 15.4 15.4 L 20.4 20.4"}};

    // which app gets which symbol: first match wins, tested against "package label" in lower case
    static final String[][] MAP = {
            {"secure folder|securefolder|knox", "lock"},
            {"contacts", "contacts"},
            {"dialer|incallui|telecom|phone", "phone"},
            {"mms|messag|sms|whatsapp|telegram|signal|viber|messenger|chat", "messages"},
            {"camera", "camera"},
            {"gmail|mail|outlook|email", "mail"},
            {"maps|navigation|waze|location", "maps"},
            {"chrome|browser|sbrowser|firefox|opera|brave|msedge|internet", "browser"},
            {"gallery|photos|photo|album", "gallery"},
            {"clock|alarm|deskclock", "clock"},
            {"calendar", "calendar"},
            {"calculator", "calc"},
            {"myfiles|documentsui|files|filemanager", "files"},
            {"shealth|health|fitness|steps", "health"},
            {"weather|daemonapp", "weather"},
            {"music|spotify|audio|podcast|radio", "music"},
            {"youtube|video|netflix|vlc|player|movie", "video"},
            {"vending|galaxystore|samsungapps|play store|app store", "store"},
            {"recorder|voice", "mic"},
            {"wallet|spay|gpay|bank", "wallet"},
            {"notes|memo|google.android.keep|note", "notes"},
            {"reminder|tasks|todo|task", "tasks"},
            {"game", "game"},
            {"settings", "settings"},
            {"quicksearchbox|search|assistant", "search"}};

    static String match(String pkg, String label) {
        String s = (pkg + " " + label).toLowerCase(Locale.US);
        for (String[] m : MAP) for (String k : m[0].split("\\|")) if (s.contains(k)) return m[1];
        return null;
    }

    private static String spec(String id) {
        for (String[] s : SPEC) if (s[0].equals(id)) return s[1];
        return null;
    }

    /** Draws symbol {@code id} centred in a square of {@code sz} pixels. */
    static boolean draw(Canvas c, String id, float sz, int color) {
        String sp = spec(id);
        if (sp == null) return false;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        Path path = new Path();
        RectF r = new RectF();
        float k = sz / 24f * 0.58f;
        c.save();
        c.translate(sz * 0.21f, sz * 0.21f);
        c.scale(k, k);
        String[] t = sp.split(" ");
        float cx = 0, cy = 0;
        for (int i = 0; i < t.length; ) {
            char op = t[i].charAt(0);
            if (op == 'M') { path.moveTo(f(t[i + 1]), f(t[i + 2])); cx = f(t[i + 1]); cy = f(t[i + 2]); i += 3; }
            else if (op == 'L') { path.lineTo(f(t[i + 1]), f(t[i + 2])); i += 3; }
            else if (op == 'C') { path.cubicTo(f(t[i + 1]), f(t[i + 2]), f(t[i + 3]), f(t[i + 4]), f(t[i + 5]), f(t[i + 6])); i += 7; }
            else if (op == 'Q') { path.quadTo(f(t[i + 1]), f(t[i + 2]), f(t[i + 3]), f(t[i + 4])); i += 5; }
            else if (op == 'Z') { path.close(); i += 1; }
            else if (op == 'O') { path.addCircle(f(t[i + 1]), f(t[i + 2]), f(t[i + 3]), Path.Direction.CW); i += 4; }
            else if (op == 'R') {
                r.set(f(t[i + 1]), f(t[i + 2]), f(t[i + 1]) + f(t[i + 3]), f(t[i + 2]) + f(t[i + 4]));
                path.addRoundRect(r, f(t[i + 5]), f(t[i + 5]), Path.Direction.CW);
                i += 6;
            } else if (op == 'D') {
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(f(t[i + 1]), f(t[i + 2]), f(t[i + 3]), p);
                i += 4;
            } else i++;
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.7f);
        c.drawPath(path, p);
        c.restore();
        return true;
    }

    private static float f(String s) { return Float.parseFloat(s); }
}

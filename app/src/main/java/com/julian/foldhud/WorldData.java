package com.julian.foldhud;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/** Coarse world outlines, country reference points and news name matching. */
final class WorldData {
    static final float[][] POLY = {
        {-168.0f,66.0f,-162.0f,70.0f,-156.0f,71.0f,-141.0f,70.0f,-128.0f,70.0f,-115.0f,68.0f,-95.0f,72.0f,-85.0f,70.0f,-82.0f,66.0f,-94.0f,60.0f,-93.0f,57.0f,-85.0f,55.0f,-80.0f,52.0f,-79.0f,57.0f,-77.0f,62.0f,-72.0f,61.0f,-65.0f,60.0f,-62.0f,57.0f,-56.0f,53.0f,-60.0f,48.0f,-66.0f,44.0f,-70.0f,43.0f,-70.0f,41.0f,-74.0f,40.0f,-76.0f,36.0f,-81.0f,31.0f,-80.0f,25.0f,-82.0f,26.0f,-83.0f,29.0f,-89.0f,30.0f,-94.0f,29.0f,-97.0f,26.0f,-97.0f,22.0f,-94.0f,18.0f,-90.0f,21.0f,-87.0f,21.0f,-88.0f,16.0f,-83.0f,15.0f,-83.0f,10.0f,-80.0f,9.0f,-77.0f,8.0f,-80.0f,7.0f,-84.0f,9.0f,-87.0f,13.0f,-92.0f,15.0f,-96.0f,16.0f,-105.0f,20.0f,-108.0f,25.0f,-113.0f,31.0f,-115.0f,32.0f,-117.0f,32.0f,-120.0f,34.0f,-124.0f,40.0f,-124.0f,47.0f,-128.0f,51.0f,-133.0f,56.0f,-140.0f,60.0f,-148.0f,61.0f,-152.0f,58.0f,-158.0f,57.0f,-163.0f,55.0f,-158.0f,59.0f,-162.0f,60.0f,-166.0f,62.0f,-165.0f,65.0f},
        {-77.0f,8.0f,-72.0f,12.0f,-64.0f,10.0f,-60.0f,8.0f,-52.0f,5.0f,-50.0f,0.0f,-44.0f,-2.0f,-35.0f,-5.0f,-35.0f,-9.0f,-39.0f,-14.0f,-41.0f,-22.0f,-48.0f,-26.0f,-53.0f,-34.0f,-58.0f,-38.0f,-62.0f,-40.0f,-65.0f,-45.0f,-68.0f,-50.0f,-69.0f,-54.0f,-72.0f,-53.0f,-74.0f,-47.0f,-73.0f,-40.0f,-71.0f,-30.0f,-70.0f,-20.0f,-76.0f,-14.0f,-81.0f,-6.0f,-80.0f,-2.0f,-78.0f,2.0f},
        {-9.0f,37.0f,-9.0f,43.0f,-2.0f,43.5f,-1.0f,46.0f,-4.5f,48.5f,2.0f,51.0f,5.0f,53.0f,8.0f,54.0f,8.0f,57.0f,10.0f,57.5f,11.0f,54.5f,14.0f,54.0f,20.0f,55.0f,21.0f,57.0f,24.0f,59.5f,30.0f,60.0f,23.0f,60.5f,21.0f,63.0f,25.0f,65.5f,22.0f,65.8f,17.0f,62.0f,19.0f,59.5f,16.0f,56.0f,13.0f,55.5f,11.0f,59.0f,5.0f,59.0f,5.0f,62.0f,14.0f,67.0f,20.0f,70.0f,28.0f,71.0f,40.0f,67.0f,44.0f,68.0f,60.0f,69.0f,68.0f,73.0f,80.0f,73.0f,90.0f,76.0f,105.0f,77.5f,113.0f,74.0f,130.0f,71.0f,142.0f,72.5f,160.0f,70.0f,170.0f,70.0f,180.0f,68.0f,180.0f,65.0f,177.0f,63.0f,170.0f,60.0f,163.0f,58.0f,160.0f,54.0f,156.0f,51.0f,156.0f,57.0f,162.0f,61.0f,155.0f,59.0f,143.0f,59.0f,137.0f,54.0f,141.0f,52.0f,140.0f,48.0f,132.0f,43.0f,129.0f,41.0f,127.0f,39.0f,129.0f,35.0f,126.0f,35.0f,126.0f,38.0f,122.0f,40.0f,118.0f,39.0f,122.0f,37.0f,119.0f,35.0f,122.0f,31.0f,121.0f,28.0f,117.0f,23.0f,110.0f,21.0f,108.0f,21.5f,106.0f,19.0f,109.0f,14.0f,107.0f,10.5f,105.0f,8.7f,103.0f,10.5f,100.0f,13.5f,100.0f,8.0f,103.0f,1.5f,100.0f,4.0f,98.0f,8.0f,98.0f,16.0f,94.0f,17.0f,94.0f,22.0f,91.0f,22.5f,87.0f,21.5f,80.0f,15.0f,80.0f,10.0f,77.0f,8.0f,73.0f,16.0f,72.0f,21.0f,67.0f,24.5f,61.0f,25.0f,57.0f,25.5f,56.0f,27.0f,50.0f,30.0f,48.0f,29.0f,50.0f,26.0f,52.0f,24.0f,56.0f,24.5f,56.0f,26.5f,59.0f,22.5f,55.0f,17.0f,52.0f,16.0f,45.0f,13.0f,43.0f,13.0f,39.0f,21.0f,35.0f,28.0f,34.5f,31.0f,36.0f,34.5f,36.0f,36.5f,32.0f,36.2f,27.0f,37.0f,26.0f,40.0f,29.0f,41.0f,31.0f,41.0f,36.0f,41.5f,41.0f,41.0f,41.5f,43.0f,38.0f,45.0f,35.0f,45.0f,33.0f,46.0f,30.0f,46.0f,28.0f,44.0f,28.5f,41.5f,26.0f,40.8f,23.0f,40.5f,24.0f,38.0f,22.0f,36.5f,21.0f,38.5f,19.5f,41.5f,16.0f,43.5f,13.0f,45.5f,12.5f,44.0f,16.0f,41.5f,18.5f,40.0f,16.0f,38.0f,15.5f,40.0f,12.0f,42.0f,10.0f,44.0f,7.0f,43.5f,3.0f,43.0f,3.0f,41.5f,0.0f,39.5f,-0.5f,38.0f,-2.0f,36.7f,-5.5f,36.0f},
        {-17.0f,15.0f,-17.0f,21.0f,-13.0f,27.5f,-9.5f,30.0f,-6.0f,35.8f,-2.0f,35.2f,10.0f,37.0f,11.0f,33.0f,15.0f,32.5f,20.0f,30.5f,25.0f,32.0f,32.0f,31.2f,34.0f,29.5f,35.0f,24.0f,37.0f,19.0f,39.0f,15.5f,43.0f,12.5f,51.0f,11.8f,51.0f,10.0f,48.0f,5.0f,41.0f,-2.0f,39.0f,-7.0f,40.0f,-11.0f,40.5f,-15.0f,35.0f,-20.0f,35.0f,-24.0f,32.5f,-26.0f,30.0f,-31.0f,27.0f,-33.5f,20.0f,-35.0f,18.0f,-32.0f,15.0f,-27.0f,12.0f,-18.0f,13.5f,-12.0f,12.0f,-6.0f,9.0f,-1.0f,9.5f,4.0f,6.0f,4.3f,4.0f,6.3f,-2.0f,5.0f,-8.0f,4.5f,-13.0f,8.0f,-15.0f,11.0f},
        {44.0f,-25.0f,47.0f,-25.0f,50.0f,-15.5f,49.0f,-12.0f,47.0f,-15.0f,44.0f,-17.0f,43.5f,-22.0f},
        {-73.0f,78.0f,-60.0f,82.0f,-30.0f,83.5f,-18.0f,80.0f,-20.0f,72.0f,-25.0f,68.0f,-40.0f,65.0f,-44.0f,60.0f,-50.0f,62.0f,-54.0f,67.0f,-58.0f,75.0f},
        {114.0f,-22.0f,122.0f,-18.0f,129.0f,-15.0f,132.0f,-11.5f,137.0f,-12.0f,136.0f,-15.0f,140.0f,-17.5f,142.0f,-10.8f,146.0f,-19.0f,153.0f,-26.0f,151.0f,-34.0f,147.0f,-38.5f,141.0f,-38.0f,135.0f,-35.0f,131.0f,-31.5f,124.0f,-33.5f,115.0f,-34.5f,114.0f,-28.0f},
        {-5.5f,50.0f,1.5f,51.0f,1.7f,52.8f,-0.5f,54.5f,-2.0f,56.0f,-2.0f,57.6f,-4.0f,58.6f,-6.0f,57.0f,-5.0f,55.0f,-3.0f,54.0f,-4.5f,52.5f,-5.5f,51.5f},
        {-10.0f,51.5f,-6.0f,52.0f,-6.0f,54.5f,-8.0f,55.0f,-10.0f,54.0f},
        {130.5f,31.0f,131.0f,34.0f,135.0f,35.5f,140.0f,38.0f,142.0f,40.0f,141.0f,41.5f,139.5f,38.0f,137.0f,36.5f,135.0f,34.5f,132.0f,34.0f},
        {140.0f,42.0f,145.0f,43.5f,142.0f,45.5f,140.0f,43.5f},
        {95.0f,5.5f,98.0f,4.0f,104.0f,-1.0f,106.0f,-6.0f,102.0f,-4.0f,98.0f,0.0f},
        {109.0f,1.5f,113.0f,3.5f,117.0f,7.0f,119.0f,5.0f,118.0f,0.0f,116.0f,-3.5f,111.0f,-3.0f,109.0f,-1.0f},
        {105.0f,-6.0f,114.0f,-7.5f,114.0f,-8.5f,106.0f,-7.0f},
        {131.0f,-1.0f,141.0f,-2.5f,148.0f,-6.0f,150.0f,-10.5f,141.0f,-9.0f,138.0f,-8.0f,132.0f,-4.0f},
        {120.0f,18.5f,122.0f,18.5f,122.0f,14.0f,124.0f,13.0f,121.0f,13.5f,120.0f,15.0f},
        {122.0f,7.0f,126.0f,7.0f,126.0f,6.0f,124.0f,6.0f},
        {173.0f,-35.0f,178.0f,-37.5f,175.0f,-41.5f,173.0f,-39.0f},
        {172.5f,-40.5f,174.0f,-41.5f,170.5f,-46.0f,167.0f,-46.0f,171.0f,-42.0f},
        {-24.0f,65.5f,-14.0f,66.0f,-14.0f,64.5f,-22.0f,63.5f},
        {-85.0f,22.0f,-80.0f,23.0f,-74.0f,20.0f,-78.0f,20.0f,-84.0f,21.5f},
        {-80.0f,73.0f,-68.0f,70.0f,-62.0f,66.0f,-65.0f,63.0f,-78.0f,64.5f,-90.0f,70.0f},
        {-118.0f,72.0f,-105.0f,73.0f,-101.0f,69.0f,-115.0f,68.0f},
        {120.0f,22.0f,121.9f,25.0f,121.0f,25.2f,120.0f,23.0f},
        {80.0f,6.0f,82.0f,7.5f,80.0f,9.5f,79.8f,8.0f},
        {119.0f,-5.5f,120.0f,-1.0f,125.0f,1.5f,121.0f,0.0f,123.0f,-4.0f,121.0f,-5.0f}
    };
    private static final String CN = "United States,39,-98;United States,37,-120;United States,42,-76;United States,32,-90;United States,64,-150;Canada,56,-106;Canada,60,-122;Canada,50,-80;Canada,64,-96;Canada,46,-72;Mexico,23.6,-102.5;Mexico,17,-93;Guatemala,15.5,-90.3;Belize,17.2,-88.7;Honduras,14.8,-86.6;El Salvador,13.8,-88.9;Nicaragua,12.9,-85;Costa Rica,9.9,-84;Panama,8.5,-80;Cuba,21.6,-79;Jamaica,18.1,-77.3;Haiti,19,-72.7;Dominican Republic,18.9,-70.5;Colombia,4.6,-74;Venezuela,7.1,-66;Guyana,4.9,-58.9;Suriname,4,-56;Ecuador,-1.4,-78.4;Peru,-9.2,-75;Brazil,-10,-52;Brazil,-4,-62;Brazil,-22,-47;Brazil,-8,-40;Bolivia,-16.7,-64.7;Paraguay,-23.2,-58.4;Chile,-33,-71;Chile,-45,-72;Chile,-23,-69;Argentina,-35,-65;Argentina,-45,-69;Argentina,-27,-62;Uruguay,-32.8,-56;Greenland,72,-40;Iceland,64.9,-18.6;United Kingdom,54,-2.5;Ireland,53.2,-8;France,46.6,2.4;Spain,40.2,-3.7;Portugal,39.6,-8;Germany,51.1,10.3;Netherlands,52.2,5.5;Belgium,50.6,4.6;Switzerland,46.8,8.2;Austria,47.5,14.1;Italy,42.8,12.5;Poland,52,19.4;Czechia,49.8,15.5;Slovakia,48.7,19.5;Hungary,47.1,19.4;Romania,45.9,25;Bulgaria,42.7,25.2;Greece,39,22;Serbia,44,20.8;Croatia,45.1,15.5;Bosnia and Herzegovina,44,17.8;Albania,41.1,20;North Macedonia,41.6,21.7;Slovenia,46.1,14.8;Denmark,56,9.5;Norway,62,9;Norway,69,19;Sweden,62.8,16.7;Finland,64.5,26.3;Estonia,58.7,25.5;Latvia,56.9,24.9;Lithuania,55.3,23.9;Belarus,53.7,28;Ukraine,49,31.4;Moldova,47.2,28.5;Russia,56,38;Russia,60,60;Russia,62,100;Russia,62,135;Russia,68,160;Russia,52,85;Cyprus,35,33;Turkey,39,35.2;Syria,35,38.5;Lebanon,33.9,35.9;Israel,31,35;Jordan,31.2,36.5;Iraq,33,43.7;Iran,32.4,53.7;Saudi Arabia,24,45;Yemen,15.6,48;Oman,21,57;United Arab Emirates,24.3,54.3;Kuwait,29.3,47.6;Qatar,25.3,51.2;Afghanistan,33.9,67.7;Pakistan,30,69.3;India,22,79;India,28,77;India,12,77;India,26,92;Nepal,28.4,84;Bhutan,27.4,90.4;Bangladesh,23.7,90.3;Sri Lanka,7.9,80.7;Myanmar,21,96.5;Myanmar,17,96.5;Myanmar,25,97;Thailand,15,101;Laos,18.2,103.9;Vietnam,16,107;Vietnam,21,105.8;Cambodia,12.6,104.9;Malaysia,4.2,102;Malaysia,2.5,113;Singapore,1.35,103.8;Philippines,12.9,122;Philippines,7.5,125;Indonesia,-2,115;Indonesia,-7.5,110;Indonesia,-2,102;Indonesia,-4,138;Indonesia,1,99;Brunei,4.5,114.7;East Timor,-8.8,125.9;Papua New Guinea,-6.3,145;China,35,103;China,40,115;China,30,112;China,43,87;China,31,90;China,25,102;China,46,126;Mongolia,46.8,103;North Korea,40,127;South Korea,36.5,127.9;Japan,36.5,138;Japan,43,142.5;Japan,33,131;Taiwan,23.7,121;Kazakhstan,48,67;Kazakhstan,48,80;Uzbekistan,41.4,64.6;Turkmenistan,39,59.5;Kyrgyzstan,41.2,74.6;Tajikistan,38.9,71;Azerbaijan,40.3,47.7;Armenia,40.2,45;Georgia,42.2,43.5,Georgia country;Australia,-25,134;Australia,-33,147;Australia,-22,122;Australia,-30,121;New Zealand,-41.5,172.5;Fiji,-17.7,178;Morocco,31.8,-7;Algeria,28,2.6;Algeria,35,3;Tunisia,33.9,9.5;Libya,27,17;Egypt,26.8,30;Sudan,15.5,30;South Sudan,7.3,30.2;Chad,15.5,18.7;Niger,17.6,9.4;Mali,17.6,-4;Mauritania,20.3,-10.4;Nigeria,9,8;Ghana,7.9,-1;Ivory Coast,7.5,-5.5;Senegal,14.5,-14.5;Guinea,10.9,-10.9;Sierra Leone,8.5,-11.8;Liberia,6.4,-9.4;Burkina Faso,12.2,-1.6;Benin,9.3,2.3;Togo,8.6,.8;Cameroon,5.7,12.4;Central African Republic,6.6,20.9;Ethiopia,9,39.5;Eritrea,15.2,39.8;Somalia,5.2,46.2;Kenya,.2,37.9;Uganda,1.4,32.3;Tanzania,-6.4,34.9;Rwanda,-2,29.9;Burundi,-3.4,29.9;DR Congo,-2.9,23.6,Democratic Republic of the Congo;Republic of the Congo,-.7,15.2,Republic of the Congo;Gabon,-.8,11.6;Angola,-12.3,17.5;Zambia,-13.1,27.8;Malawi,-13.3,34.3;Mozambique,-18.7,35.5;Zimbabwe,-19,29.8;Botswana,-22.3,24.7;Namibia,-22.1,17.2;South Africa,-29,24.5;Madagascar,-19.4,46.7;Lesotho,-29.6,28.2;Eswatini,-26.5,31.5;Nigeria,6.5,3.6;Nigeria,6.5,7.2;Nigeria,11.5,8.5;France,48.85,2.35;France,44,2;France,48.6,7.7;France,43.3,5.4;Germany,52.5,13.4;Germany,48.1,11.6;Germany,50.1,8.7;Spain,41.4,2.2;Spain,37.4,-5.9;Italy,45.5,9.2;Italy,40.9,14.3;Turkey,41,29;Turkey,39.9,32.9;United Kingdom,51.5,-.1;United Kingdom,52.5,-1.9;United Kingdom,55.9,-3.2;United Kingdom,53.5,-2.2;Poland,52.2,21;Egypt,30,31.2;Iran,35.7,51.4;Pakistan,24.9,67;Pakistan,31.5,74.3;Saudi Arabia,21.5,39.2;Saudi Arabia,24.7,46.7;Iraq,33.3,44.4;South Africa,-26.2,28;South Africa,-33.9,18.4;Kenya,-1.3,36.8;Ethiopia,9,38.7;Indonesia,-6.2,106.8;Philippines,14.6,121;South Korea,37.5,127;Argentina,-34.6,-58.4;Mexico,19.4,-99.1;Peru,-12,-77;Canada,45.4,-75.7;Canada,43.7,-79.4;Canada,49.3,-123.1;United States,40.7,-74;United States,34,-118;United States,41.9,-87.6;United States,29.8,-95.4;United States,25.8,-80.2;United States,47.6,-122.3;United States,38.9,-77;Russia,59.9,30.3;Brazil,-23.5,-46.6;Brazil,-22.9,-43.2;India,19,72.8;India,22.6,88.4;China,31.2,121.5;China,23.1,113.3;Australia,-37.8,145;Australia,-31.9,115.9;Ukraine,50.4,30.5;Netherlands,52.4,4.9;Belgium,50.85,4.35;Switzerland,47.4,8.5;Sweden,59.3,18.1;Thailand,13.75,100.5;Vietnam,10.8,106.7;Malaysia,3.1,101.7;Palestine,31.9,35.2;Palestine,31.4,34.4";
    private static final String ALIAS = "United States=U.S.|US|America|American;United Kingdom=UK|U.K.|Britain|British;Russia=Russian|Kremlin|Moscow;Ukraine=Ukrainian|Kyiv|Kiev;China=Chinese|Beijing;Israel=Israeli;Iran=Iranian|Tehran;North Korea=Pyongyang;South Korea=Seoul;Palestine=Gaza|West Bank|Palestinian;DR Congo=Congo|DRC;United Arab Emirates=UAE|Emirati;Saudi Arabia=Saudi;Myanmar=Burma|Burmese;Syria=Syrian|Damascus;Turkey=Turkish|Türkiye;India=Indian|New Delhi;Pakistan=Pakistani;Taiwan=Taiwanese;Japan=Japanese|Tokyo;France=French|Paris;Germany=German|Berlin;Venezuela=Venezuelan;Sudan=Sudanese;Lebanon=Lebanese|Beirut;Yemen=Yemeni|Houthi|Houthis;Iraq=Iraqi|Baghdad;Afghanistan=Afghan|Kabul;Nigeria=Nigerian;Ethiopia=Ethiopian;Somalia=Somali;Mexico=Mexican;Brazil=Brazilian;Argentina=Argentine|Argentinian;Egypt=Egyptian|Cairo;Italy=Italian|Rome;Spain=Spanish|Madrid;Poland=Polish|Warsaw;Australia=Australian;Canada=Canadian;Thailand=Thai|Bangkok;Vietnam=Vietnamese|Hanoi;Indonesia=Indonesian|Jakarta;Philippines=Philippine|Filipino|Manila;Bangladesh=Bangladeshi|Dhaka;Cuba=Cuban|Havana;Greece=Greek|Athens;Netherlands=Dutch;Switzerland=Swiss;Sweden=Swedish;Norway=Norwegian;Finland=Finnish;Ireland=Irish;Kenya=Kenyan;South Africa=South African;Libya=Libyan;Haiti=Haitian;Colombia=Colombian";

    static float[][] DENSE;          // outlines with extra points so edges follow the curve of the globe
    static String[] NAMES, QUERY;    // country names and the text used when searching for their news
    static float[] CLAT, CLON;       // one reference point per country
    static Pattern[] RE;             // matches a country (or its adjective) inside a headline
    private static float[][] CV;
    private static int[] CID;

    static synchronized void init() {
        if (DENSE != null) return;
        float[][] dense = new float[POLY.length][];
        for (int k = 0; k < POLY.length; k++) {
            float[] p = POLY[k];
            int n = p.length / 2, total = 0;
            int[] steps = new int[n];
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float dx = Math.abs(p[2 * j] - p[2 * i]), dy = Math.abs(p[2 * j + 1] - p[2 * i + 1]);
                steps[i] = Math.max(1, (int) Math.ceil(Math.max(dx, dy) / 2.5f));
                total += steps[i];
            }
            float[] o = new float[total * 2];
            int w = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                for (int s = 0; s < steps[i]; s++) {
                    float t = (float) s / steps[i];
                    o[w++] = p[2 * i] + (p[2 * j] - p[2 * i]) * t;
                    o[w++] = p[2 * i + 1] + (p[2 * j + 1] - p[2 * i + 1]) * t;
                }
            }
            dense[k] = o;
        }
        String[] ent = CN.split(";");
        ArrayList<String> names = new ArrayList<>(), query = new ArrayList<>();
        ArrayList<Float> la = new ArrayList<>(), lo = new ArrayList<>();
        float[][] cv = new float[ent.length][3];
        int[] cid = new int[ent.length];
        for (int i = 0; i < ent.length; i++) {
            String[] a = ent[i].split(",");
            int id = names.indexOf(a[0]);
            float lat = Float.parseFloat(a[1]), lon = Float.parseFloat(a[2]);
            if (id < 0) {
                id = names.size();
                names.add(a[0]);
                query.add(a.length > 3 ? a[3] : a[0]);
                la.add(lat);
                lo.add(lon);
            }
            double rl = Math.toRadians(lat), rn = Math.toRadians(lon);
            cv[i][0] = (float) (Math.cos(rl) * Math.cos(rn));
            cv[i][1] = (float) (Math.cos(rl) * Math.sin(rn));
            cv[i][2] = (float) Math.sin(rl);
            cid[i] = id;
        }
        int n = names.size();
        String[] nm = names.toArray(new String[0]);
        float[] clat = new float[n], clon = new float[n];
        for (int i = 0; i < n; i++) { clat[i] = la.get(i); clon[i] = lo.get(i); }
        HashMap<String, String[]> al = new HashMap<>();
        for (String e : ALIAS.split(";")) {
            int k = e.indexOf('=');
            if (k > 0) al.put(e.substring(0, k), e.substring(k + 1).split("\\|"));
        }
        Pattern[] re = new Pattern[n];
        for (int i = 0; i < n; i++) {
            StringBuilder sb = new StringBuilder("(?<![A-Za-z])(").append(Pattern.quote(nm[i]));
            String[] x = al.get(nm[i]);
            if (x != null) for (String s : x) sb.append('|').append(Pattern.quote(s));
            re[i] = Pattern.compile(sb.append(")(?![A-Za-z])").toString());
        }
        CV = cv; CID = cid; NAMES = nm; QUERY = query.toArray(new String[0]); CLAT = clat; CLON = clon; RE = re;
        DENSE = dense;
    }

    /** Index of the country whose reference point is nearest, or -1 when none is close. */
    static int nearest(float lat, float lon) {
        init();
        double la = Math.toRadians(lat), lo = Math.toRadians(lon);
        double x = Math.cos(la) * Math.cos(lo), y = Math.cos(la) * Math.sin(lo), z = Math.sin(la);
        double best = -2;
        int bi = -1;
        for (int i = 0; i < CV.length; i++) {
            double d = x * CV[i][0] + y * CV[i][1] + z * CV[i][2];
            if (d > best) { best = d; bi = i; }
        }
        return best > Math.cos(4000 / 6371.0) ? CID[bi] : -1;
    }

    static boolean onLand(float lat, float lon) {
        init();
        float[] o = {0f, 1.5f, -1.5f};
        for (float a : o) for (float b : o) if (inAny(lon + b, lat + a)) return true;
        return false;
    }

    private static boolean inAny(float lo, float la) {
        for (float[] p : POLY) {
            boolean c = false;
            int n = p.length / 2;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                float xi = p[2 * i], yi = p[2 * i + 1], xj = p[2 * j], yj = p[2 * j + 1];
                if ((yi > la) != (yj > la) && lo < (xj - xi) * (la - yi) / (yj - yi) + xi) c = !c;
            }
            if (c) return true;
        }
        return false;
    }
}

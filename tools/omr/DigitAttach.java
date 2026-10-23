import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Stage 3: attach named digit blobs to Audiveris note heads.
 *
 * Usage: java DigitAttach page.png book.omr blobs.tsv assign.tsv out-prefix [x0 y0 w h]
 * Run with -Dlabels=FILE: lines "cluster<TAB>N<TAB>digit" (read off the DigitClusters sheet by eye)
 * and "blob<TAB>idx<TAB>digit|-" to override single blobs ("-" = not a digit).
 * Heads are grouped into chord columns (same staff, centres within 8 px). A digit
 * belongs to the nearest column within COL_DX horizontally and COL_DY vertically of the
 * column's extent.  Digits above a column are matched to its heads from the top,
 * digits below from the bottom; digits beside a head go to that head.
 * Writes <out>.tsv (digit, x, y, headId, pitch, staff, how) and, if a region is
 * given, <out>.png with the region, a coloured line from each digit to its head.
 *
 * Complexity: O(D * C) for D digits and C columns.
 */
public class DigitAttach {
    static final double COL_DX = 26, COL_DY_ABOVE = 70, COL_DY_BELOW = 130;
    /** cluster -> digit, and blob index -> digit ("-" = not a digit); read from the -Dlabels file. */
    static final Map<Integer,String> LABEL = new HashMap<>(), BLOB = new HashMap<>();
    static void readLabels(String path) throws Exception {
        for (String l : Files.readAllLines(new File(path).toPath())) {
            String[] t = l.split("\t");
            if (t.length == 3 && t[0].equals("cluster")) LABEL.put(Integer.parseInt(t[1]), t[2]);
            else if (t.length == 3 && t[0].equals("blob")) BLOB.put(Integer.parseInt(t[1]), t[2]);
        }
    }
    static class Head { int id, x, y, w, h, staff; String pitch; double cx() { return x + w / 2.0; } double cy() { return y + h / 2.0; } }

    public static void main(String[] a) throws Exception {
        readLabels(System.getProperty("labels"));
        BufferedImage img = ImageIO.read(new File(a[0]));
        List<Head> heads = new ArrayList<>();
        try (ZipFile z = new ZipFile(a[1])) {
            String xml = new String(z.getInputStream(z.getEntry("sheet#1/sheet#1.xml")).readAllBytes(), "UTF-8");
            Matcher m = Pattern.compile("<head ([^>]*)>\\s*<bounds x=\"(\\d+)\" y=\"(\\d+)\" w=\"(\\d+)\" h=\"(\\d+)\"").matcher(xml);
            while (m.find()) {
                String at = m.group(1);
                Matcher sh = Pattern.compile("shape=\"(\\w+)\"").matcher(at);
                if (!sh.find() || !sh.group(1).startsWith("NOTEHEAD")) continue;
                Head h = new Head();
                h.id = Integer.parseInt(find(at, "id=\"(\\d+)\"")); h.staff = Integer.parseInt(find(at, "staff=\"(\\d+)\""));
                h.pitch = find(at, "pitch=\"(-?\\d+)\"");
                h.x = Integer.parseInt(m.group(2)); h.y = Integer.parseInt(m.group(3)); h.w = Integer.parseInt(m.group(4)); h.h = Integer.parseInt(m.group(5));
                heads.add(h);
            }
        }
        // chord columns
        List<List<Head>> cols = new ArrayList<>();
        heads.sort(Comparator.comparingInt((Head h) -> h.staff).thenComparingDouble(Head::cx));
        for (Head h : heads) {
            List<Head> last = cols.isEmpty() ? null : cols.get(cols.size() - 1);
            if (last != null && last.get(0).staff == h.staff && Math.abs(last.get(0).cx() - h.cx()) < 8) last.add(h); else { List<Head> n = new ArrayList<>(); n.add(h); cols.add(n); }
        }
        for (List<Head> c : cols) c.sort(Comparator.comparingDouble(Head::cy));  // top first

        List<String> bl = Files.readAllLines(new File(a[2]).toPath()), as = Files.readAllLines(new File(a[3]).toPath());
        List<double[]> dig = new ArrayList<>(); List<String> lab = new ArrayList<>();
        for (int i = 1; i < bl.size(); i++) {
            String[] t = bl.get(i).split("\t"), u = as.get(i).split("\t");
            String l = BLOB.getOrDefault(Integer.parseInt(t[0]), LABEL.get(Integer.parseInt(u[1]))); if (l == null || l.equals("-")) continue;
            dig.add(new double[]{Integer.parseInt(t[1]) + Integer.parseInt(t[3]) / 2.0, Integer.parseInt(t[2]) + Integer.parseInt(t[4]) / 2.0});
            lab.add(l);
        }
        // assign each digit to a column, then to a head within it
        Map<Integer, List<Integer>> byCol = new HashMap<>();
        int[] colOf = new int[dig.size()];
        for (int i = 0; i < dig.size(); i++) {
            double bd = 1e9; int bc = -1;
            for (int c = 0; c < cols.size(); c++) {
                List<Head> col = cols.get(c);
                double dx = Math.abs(dig.get(i)[0] - col.get(0).cx());
                double top = col.get(0).y, bot = col.get(col.size() - 1).y + col.get(col.size() - 1).h;
                double dy = dig.get(i)[1] < top ? top - dig.get(i)[1] : dig.get(i)[1] > bot ? dig.get(i)[1] - bot : 0;
                if (dx > COL_DX || dy > (dig.get(i)[1] < top ? COL_DY_ABOVE : COL_DY_BELOW)) continue;
                double d = dx * 1.5 + dy;
                if (d < bd) { bd = d; bc = c; }
            }
            colOf[i] = bc;
            if (bc >= 0) byCol.computeIfAbsent(bc, k -> new ArrayList<>()).add(i);
        }
        String[][] res = new String[dig.size()][];
        for (int i = 0; i < dig.size(); i++) res[i] = new String[]{"", "", "unmatched"};
        for (Map.Entry<Integer, List<Integer>> e : byCol.entrySet()) {
            List<Head> col = cols.get(e.getKey());
            double top = col.get(0).y, bot = col.get(col.size() - 1).y + col.get(col.size() - 1).h;
            List<Integer> above = new ArrayList<>(), below = new ArrayList<>(), beside = new ArrayList<>();
            for (int i : e.getValue()) { double y = dig.get(i)[1]; if (y < top) above.add(i); else if (y > bot) below.add(i); else beside.add(i); }
            above.sort(Comparator.comparingDouble(i -> -dig.get(i)[1]));   // nearest the chord first
            for (int k = 0; k < above.size(); k++) { Head h = col.get(Math.min(k, col.size() - 1)); res[above.get(k)] = new String[]{"" + h.id, h.pitch + "/" + h.staff, k == 0 ? "above" : "above-stack"}; }
            below.sort(Comparator.comparingDouble(i -> dig.get(i)[1]));
            for (int k = 0; k < below.size(); k++) { Head h = col.get(Math.max(col.size() - 1 - k, 0)); res[below.get(k)] = new String[]{"" + h.id, h.pitch + "/" + h.staff, k == 0 ? "below" : "below-stack"}; }
            for (int i : beside) { Head best = col.get(0); for (Head h : col) if (Math.abs(h.cy() - dig.get(i)[1]) < Math.abs(best.cy() - dig.get(i)[1])) best = h; res[i] = new String[]{"" + best.id, best.pitch + "/" + best.staff, "beside"}; }
        }
        Map<Integer, Head> byId = new HashMap<>(); for (Head h : heads) byId.put(h.id, h);
        try (PrintWriter pw = new PrintWriter(a[4] + ".tsv")) {
            pw.println("digit\tx\ty\thead\tpitch/staff\thow");
            for (int i = 0; i < dig.size(); i++) pw.println(lab.get(i) + "\t" + (int) dig.get(i)[0] + "\t" + (int) dig.get(i)[1] + "\t" + res[i][0] + "\t" + res[i][1] + "\t" + res[i][2]);
        }
        int un = 0; for (String[] r : res) if (r[2].equals("unmatched")) un++;
        System.out.println("digits=" + dig.size() + " unmatched=" + un + " columns=" + cols.size());
        if (a.length >= 10) {
            int x0 = Integer.parseInt(a[5]), y0 = Integer.parseInt(a[6]), w = Integer.parseInt(a[7]), h = Integer.parseInt(a[8]);
            int s = Integer.parseInt(a[9]);
            BufferedImage out = new BufferedImage(w * s, h * s, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics(); g.drawImage(img.getSubimage(x0, y0, w, h).getScaledInstance(w * s, h * s, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            for (int i = 0; i < dig.size(); i++) {
                double x = dig.get(i)[0], y = dig.get(i)[1];
                if (x < x0 || x > x0 + w || y < y0 || y > y0 + h) continue;
                g.setColor(res[i][2].equals("unmatched") ? Color.ORANGE : Color.RED);
                g.drawOval((int) ((x - x0 - 10) * s), (int) ((y - y0 - 10) * s), 20 * s, 20 * s);
                if (!res[i][0].isEmpty()) { Head hd = byId.get(Integer.parseInt(res[i][0])); g.setColor(Color.BLUE);
                    g.drawLine((int) ((x - x0) * s), (int) ((y - y0) * s), (int) ((hd.cx() - x0) * s), (int) ((hd.cy() - y0) * s)); }
            }
            g.dispose(); ImageIO.write(out, "png", new File(a[4] + ".png"));
        }
    }
    static String find(String s, String re) { Matcher m = Pattern.compile(re).matcher(s); return m.find() ? m.group(1) : "0"; }
}

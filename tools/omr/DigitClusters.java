import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Stage 2: group the DigitBlobs candidates by shape (k-means on a 12x16
 * area-averaged bitmap) so each group can be named by eye once.
 *
 * Usage: java DigitClusters page.png blobs.tsv k out-prefix
 * Writes <out>.clusters.png (one row per cluster, members nearest the centre
 * first, cluster number and size at the left) and <out>.assign.tsv
 * (idx cluster dist).
 *
 * Complexity: O(iter * N * k * 192) for N blobs; N is a few hundred per page.
 */
public class DigitClusters {

    static final int GW = 12, GH = 16, D = GW * GH;

    static double[] feature(boolean[][] ink, int x, int y, int w, int h) {
        double[] f = new double[D];
        for (int gy = 0; gy < GH; gy++) for (int gx = 0; gx < GW; gx++) {
            int x0 = x + gx * w / GW, x1 = Math.max(x0 + 1, x + (gx + 1) * w / GW);
            int y0 = y + gy * h / GH, y1 = Math.max(y0 + 1, y + (gy + 1) * h / GH);
            int n = 0, s = 0;
            for (int yy = y0; yy < y1 && yy < y + h; yy++) for (int xx = x0; xx < x1 && xx < x + w; xx++) {
                n++; if (ink[yy][xx]) s++;
            }
            f[gy * GW + gx] = n == 0 ? 0 : (double) s / n;
        }
        // keep the aspect ratio as two extra-weighted cells folded into the sum
        return f;
    }

    static double dist(double[] a, double[] b) {
        double s = 0; for (int i = 0; i < D; i++) { double d = a[i] - b[i]; s += d * d; } return Math.sqrt(s);
    }

    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        List<String> lines = Files.readAllLines(new File(a[1]).toPath());
        int k = Integer.parseInt(a[2]);
        int W = img.getWidth(), H = img.getHeight();
        boolean[][] ink = new boolean[H][W];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) ink[y][x] = (img.getRGB(x, y) & 0xFF) < 128;

        int n = lines.size() - 1;
        int[][] box = new int[n][4];
        double[][] f = new double[n][];
        double[] aspect = new double[n];
        for (int i = 0; i < n; i++) {
            String[] t = lines.get(i + 1).split("\t");
            for (int j = 0; j < 4; j++) box[i][j] = Integer.parseInt(t[j + 1]);
            f[i] = feature(ink, box[i][0], box[i][1], box[i][2], box[i][3]);
        }
        // k-means++ seeding, fixed seed
        Random r = new Random(7);
        double[][] c = new double[k][];
        c[0] = f[r.nextInt(n)].clone();
        for (int q = 1; q < k; q++) {
            double[] d2 = new double[n]; double tot = 0;
            for (int i = 0; i < n; i++) {
                double m = 1e9; for (int j = 0; j < q; j++) m = Math.min(m, dist(f[i], c[j]));
                d2[i] = m * m; tot += d2[i];
            }
            double pick = r.nextDouble() * tot, acc = 0; int sel = n - 1;
            for (int i = 0; i < n; i++) { acc += d2[i]; if (acc >= pick) { sel = i; break; } }
            c[q] = f[sel].clone();
        }
        int[] as = new int[n];
        for (int it = 0; it < 60; it++) {
            for (int i = 0; i < n; i++) {
                double m = 1e9; int b = 0;
                for (int j = 0; j < k; j++) { double d = dist(f[i], c[j]); if (d < m) { m = d; b = j; } }
                as[i] = b;
            }
            double[][] s = new double[k][D]; int[] cnt = new int[k];
            for (int i = 0; i < n; i++) { cnt[as[i]]++; for (int d = 0; d < D; d++) s[as[i]][d] += f[i][d]; }
            for (int j = 0; j < k; j++) if (cnt[j] > 0) for (int d = 0; d < D; d++) c[j][d] = s[j][d] / cnt[j];
        }

        try (PrintWriter pw = new PrintWriter(a[3] + ".assign.tsv")) {
            pw.println("idx\tcluster\tdist");
            for (int i = 0; i < n; i++) pw.println(i + "\t" + as[i] + "\t" + String.format("%.3f", dist(f[i], c[as[i]])));
        }

        int per = 22, cell = 40, scale = 1, left = 70;
        BufferedImage sheet = new BufferedImage(left + per * cell, k * (cell + 4), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int j = 0; j < k; j++) {
            List<Integer> mem = new ArrayList<>();
            for (int i = 0; i < n; i++) if (as[i] == j) mem.add(i);
            final double[] cj = c[j];
            mem.sort((p, q) -> Double.compare(dist(f[p], cj), dist(f[q], cj)));
            g.setColor(Color.RED); g.drawString("#" + j + "  n=" + mem.size(), 2, j * (cell + 4) + 16);
            for (int m = 0; m < Math.min(per, mem.size()); m++) {
                int[] b = box[mem.get(m)];
                int ox = left + m * cell, oy = j * (cell + 4);
                int s2 = Math.max(1, Math.min(cell / Math.max(b[2], b[3]), 2));
                for (int y = 0; y < b[3]; y++) for (int x = 0; x < b[2]; x++) {
                    if (!ink[b[1] + y][b[0] + x]) continue;
                    g.setColor(Color.BLACK); g.fillRect(ox + 2 + x * s2, oy + 2 + y * s2, s2, s2);
                }
            }
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(a[3] + ".clusters.png"));
        System.out.println("n=" + n + " k=" + k);
    }
}

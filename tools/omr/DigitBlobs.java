import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Stage 1 of fingering-digit recovery from OMR'd scans.
 *
 * Finds small connected ink components in the page image that are candidate
 * finger digits, away from every Audiveris note head, and writes
 *   <out>.tsv   one candidate per line: id x y w h nearestHeadId side dist
 *   <out>.png   contact sheet of the candidates (index printed under each)
 *
 * Usage: java DigitBlobs page.png book.omr out-prefix
 *
 * Complexity: O(W*H) labelling + O(B*H) nearest-head search (B blobs, H heads).
 * Reads only the page image and head bounds; writes nothing else.
 */
public class DigitBlobs {

    static final int MIN_H = 9, MAX_H = 34, MIN_W = 4, MAX_W = 28, MIN_AREA = 25;

    static class Box { int id, x, y, w, h; }

    static List<Box> readHeads(String omr) throws Exception {
        List<Box> heads = new ArrayList<>();
        try (ZipFile z = new ZipFile(omr)) {
            String xml = new String(z.getInputStream(z.getEntry("sheet#1/sheet#1.xml")).readAllBytes(), "UTF-8");
            Matcher m = Pattern.compile(
                "<head [^>]*?id=\"(\\d+)\">\\s*<bounds x=\"(\\d+)\" y=\"(\\d+)\" w=\"(\\d+)\" h=\"(\\d+)\"").matcher(xml);
            while (m.find()) {
                Box b = new Box();
                b.id = Integer.parseInt(m.group(1));
                b.x = Integer.parseInt(m.group(2)); b.y = Integer.parseInt(m.group(3));
                b.w = Integer.parseInt(m.group(4)); b.h = Integer.parseInt(m.group(5));
                heads.add(b);
            }
        }
        return heads;
    }

    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        List<Box> heads = readHeads(a[1]);
        int W = img.getWidth(), H = img.getHeight();
        boolean[] ink = new boolean[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                ink[y * W + x] = (img.getRGB(x, y) & 0xFF) < 128;

        int[] label = new int[W * H];
        List<Box> blobs = new ArrayList<>();
        int[] qx = new int[W * H], qy = new int[W * H];
        int next = 0;
        for (int y0 = 0; y0 < H; y0++) for (int x0 = 0; x0 < W; x0++) {
            if (!ink[y0 * W + x0] || label[y0 * W + x0] != 0) continue;
            next++;
            int head = 0, tail = 0, minx = x0, maxx = x0, miny = y0, maxy = y0, area = 0;
            qx[tail] = x0; qy[tail++] = y0; label[y0 * W + x0] = next;
            while (head < tail) {
                int x = qx[head], y = qy[head++]; area++;
                if (x < minx) minx = x; if (x > maxx) maxx = x;
                if (y < miny) miny = y; if (y > maxy) maxy = y;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
                    int k = ny * W + nx;
                    if (ink[k] && label[k] == 0) { label[k] = next; qx[tail] = nx; qy[tail++] = ny; }
                }
            }
            int w = maxx - minx + 1, h = maxy - miny + 1;
            if (h < MIN_H || h > MAX_H || w < MIN_W || w > MAX_W || area < MIN_AREA) continue;
            Box b = new Box(); b.x = minx; b.y = miny; b.w = w; b.h = h; b.id = blobs.size();
            blobs.add(b);
        }

        // drop blobs that sit on a note head; attach the rest to the nearest head
        List<Box> keep = new ArrayList<>();
        List<int[]> att = new ArrayList<>();   // headId, side(-1 above,+1 below), dist
        for (Box b : blobs) {
            boolean onHead = false;
            Box best = null; double bd = 1e9;
            double cx = b.x + b.w / 2.0, cy = b.y + b.h / 2.0;
            for (Box hd : heads) {
                if (b.x < hd.x + hd.w + 2 && b.x + b.w > hd.x - 2 && b.y < hd.y + hd.h + 2 && b.y + b.h > hd.y - 2) {
                    onHead = true; break;
                }
                double d = Math.hypot(cx - (hd.x + hd.w / 2.0), cy - (hd.y + hd.h / 2.0));
                if (d < bd) { bd = d; best = hd; }
            }
            if (onHead || best == null) continue;
            keep.add(b);
            att.add(new int[]{best.id, cy < best.y + best.h / 2.0 ? -1 : 1, (int) bd});
        }

        try (PrintWriter pw = new PrintWriter(a[2] + ".tsv")) {
            pw.println("idx\tx\ty\tw\th\thead\tside\tdist");
            for (int i = 0; i < keep.size(); i++) {
                Box b = keep.get(i); int[] t = att.get(i);
                pw.println(i + "\t" + b.x + "\t" + b.y + "\t" + b.w + "\t" + b.h + "\t" + t[0] + "\t" + t[1] + "\t" + t[2]);
            }
        }

        int cols = 24, cell = 64, scale = 2, rows = (keep.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * cell, Math.max(1, rows) * (cell + 14), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < keep.size(); i++) {
            Box b = keep.get(i);
            int ox = (i % cols) * cell, oy = (i / cols) * (cell + 14);
            for (int y = 0; y < b.h; y++) for (int x = 0; x < b.w; x++) {
                if (!ink[(b.y + y) * W + b.x + x]) continue;
                g.setColor(Color.BLACK);
                g.fillRect(ox + 4 + x * scale, oy + 4 + y * scale, scale, scale);
            }
            g.setColor(Color.RED); g.drawString(Integer.toString(i), ox + 4, oy + cell + 11);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(a[2] + ".png"));
        System.out.println("heads=" + heads.size() + " candidates=" + keep.size());
    }
}

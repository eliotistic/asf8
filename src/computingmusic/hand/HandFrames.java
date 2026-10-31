package computingmusic.hand;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import computingmusic.instrument.InstrumentSpec;

/**
 * Where can the hand move?  A frame is a run of notes one hand position can play: there is some set of at most four stopped
 * heights (semitones above the open string, FrameTest says whether the hand can hold them at once) such that every note of
 * the run is either an open string or lies at one of those heights on some string.  No frame is chosen as the longest: the
 * report gives, for the fewest frames that cover the line, every place each shift may fall (the shifts of every partition
 * with that many frames, found with the shortest cover of the notes before the place and of the notes after it).
 *
 *   java -cp build/classes computingmusic.hand.HandFrames SCORE.musicxml FROM_MEASURE TO_MEASURE [absolute] [png=FILE]
 *
 * Pitches only: no fingering of the score is read.  Single notes only (chords are not handled yet).
 */
public final class HandFrames {
	final Geometry g;
	final FrameTest test;
	final int maxHeight;
	final int[] pitch;
	final String[] label;
	final List<BitSet> cover = new ArrayList<BitSet>();
	final List<int[]> sets = new ArrayList<int[]>();

	HandFrames(Geometry g, FrameTest test, int maxHeight, int[] pitch, String[] label) {
		this.g = g;
		this.test = test;
		this.maxHeight = maxHeight;
		this.pitch = pitch;
		this.label = label;
		enumerate(new int[4], 0, 1);
	}

	/** Every set of up to four distinct stopped heights the hand can hold, with the pitches they reach.  Complexity: O(S) for S feasible sets. */
	private void enumerate(int[] cur, int k, int from) {
		int[] s = java.util.Arrays.copyOf(cur, k);
		if (k == 0 || test.feasible(s)) {
			sets.add(s);
			cover.add(reach(s));
		} else {
			return;   // a superset of an infeasible set is infeasible
		}
		if (k == 4) {
			return;
		}
		for (int h = from; h <= maxHeight; h++) {
			cur[k] = h;
			enumerate(cur, k + 1, h + 1);
		}
	}

	private BitSet reach(int[] heights) {
		BitSet b = new BitSet();
		for (int o : g.open) {
			b.set(o);
			for (int h : heights) {
				b.set(o + h);
			}
		}
		return b;
	}

	/** The smallest sets of heights that carry notes i..j, or empty when no hand position does.  Complexity: O(S). */
	List<int[]> frame(int i, int j) {
		BitSet want = new BitSet();
		for (int n = i; n <= j; n++) {
			want.set(pitch[n]);
		}
		List<Integer> hit = new ArrayList<Integer>();
		for (int s = 0; s < sets.size(); s++) {
			BitSet w = (BitSet) want.clone();
			w.andNot(cover.get(s));
			if (w.isEmpty()) {
				hit.add(s);
			}
		}
		List<int[]> minimal = new ArrayList<int[]>();
		for (int a : hit) {
			boolean smaller = false;
			for (int b : hit) {
				if (b != a && sets.get(b).length < sets.get(a).length && contains(sets.get(a), sets.get(b))) {
					smaller = true;
					break;
				}
			}
			if (!smaller) {
				minimal.add(sets.get(a));
			}
		}
		return minimal;
	}

	private static boolean contains(int[] big, int[] small) {
		for (int x : small) {
			boolean in = false;
			for (int y : big) {
				in |= x == y;
			}
			if (!in) {
				return false;
			}
		}
		return true;
	}

	boolean[][] feasibleWindows() {
		int n = pitch.length;
		boolean[][] ok = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = i; j < n; j++) {
				ok[i][j] = !frame(i, j).isEmpty();
				if (!ok[i][j]) {
					break;   // a longer run holds the notes of this one
				}
			}
		}
		return ok;
	}

	static String show(int[] s) {
		StringBuilder b = new StringBuilder("{");
		for (int i = 0; i < s.length; i++) {
			b.append(i == 0 ? "" : " ").append(s[i]);
		}
		return b.append("}").toString();
	}

	public static void main(String[] a) throws Exception {
		String file = a[0];
		int from = Integer.parseInt(a[1]), to = Integer.parseInt(a[2]);
		boolean absolute = false;
		String png = null;
		for (int i = 3; i < a.length; i++) {
			if (a[i].equals("absolute")) {
				absolute = true;
			} else if (a[i].startsWith("png=")) {
				png = a[i].substring(4);
			}
		}
		List<Integer> ps = new ArrayList<Integer>();
		List<String> ls = new ArrayList<String>();
		List<Integer> bars = new ArrayList<Integer>();
		read(file, from, to, ps, ls, bars);
		int n = ps.size();
		int[] pitch = new int[n];
		String[] label = new String[n];
		for (int i = 0; i < n; i++) {
			pitch[i] = ps.get(i);
			label[i] = ls.get(i);
		}
		Geometry g = Geometry.VIOLIN;
		HandFrames hf = new HandFrames(g, new FrameTest(g, HandParams.forInstrument(InstrumentSpec.VIOLIN), absolute), 24, pitch, label);
		boolean[][] ok = hf.feasibleWindows();
		int[] f = new int[n + 1], gg = new int[n + 1];
		final int INF = 1000;
		java.util.Arrays.fill(f, INF);
		java.util.Arrays.fill(gg, INF);
		f[0] = 0;
		gg[n] = 0;
		for (int j = 1; j <= n; j++) {
			for (int i = 0; i < j; i++) {
				if (ok[i][j - 1] && f[i] + 1 < f[j]) {
					f[j] = f[i] + 1;
				}
			}
		}
		for (int i = n - 1; i >= 0; i--) {
			for (int j = i + 1; j <= n; j++) {
				if (ok[i][j - 1] && gg[j] + 1 < gg[i]) {
					gg[i] = gg[j] + 1;
				}
			}
		}
		int m = f[n];
		System.out.println(n + " notes, " + (absolute ? "absolute" : "comfortable") + " reach: the hand needs at least " + m + " positions");
		List<List<Integer>> allowed = new ArrayList<List<Integer>>();
		for (int t = 1; t < m; t++) {
			List<Integer> cuts = new ArrayList<Integer>();
			for (int k = 1; k < n; k++) {
				if (f[k] == t && f[k] + gg[k] == m) {
					cuts.add(k);
				}
			}
			allowed.add(cuts);
			StringBuilder sb = new StringBuilder("shift " + t + " may come before note:");
			for (int k : cuts) {
				sb.append(" ").append(label[k]);
			}
			System.out.println(sb);
		}
		// the frames of the partition that shifts as early as it can and as late as it can
		int[][] early = partition(ok, f, gg, n, m, true), late = partition(ok, f, gg, n, m, false);
		for (int pass = 0; pass < 2; pass++) {
			int[][] p = pass == 0 ? early : late;
			System.out.println(pass == 0 ? "shifting as early as possible:" : "shifting as late as possible:");
			for (int[] w : p) {
				List<int[]> fr = hf.frame(w[0], w[1]);
				StringBuilder sb = new StringBuilder("  " + label[w[0]] + " .. " + label[w[1]] + "  heights");
				for (int i = 0; i < fr.size() && i < 8; i++) {
					sb.append(" ").append(show(fr.get(i)));
				}
				System.out.println(sb);
			}
		}
		if (png != null) {
			draw(png, pitch, bars, allowed, early, late, n);
		}
	}

	/** The partition into m frames whose every frame is as short (earlyShifts) or as long as the optimum allows at each shift. */
	static int[][] partition(boolean[][] ok, int[] f, int[] gg, int n, int m, boolean earlyShifts) {
		int[][] out = new int[m][2];
		int start = 0;
		for (int t = 1; t <= m; t++) {
			int end = n;
			if (t < m) {
				end = -1;
				for (int k = start + 1; k < n; k++) {
					if (ok[start][k - 1] && f[k] == t && f[k] + gg[k] == m) {
						end = k;
						if (earlyShifts) {
							break;
						}
					}
				}
			}
			out[t - 1][0] = start;
			out[t - 1][1] = end - 1;
			start = end;
		}
		return out;
	}

	static void read(String file, int from, int to, List<Integer> ps, List<String> ls, List<Integer> bars) throws Exception {
		org.w3c.dom.Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(file));
		NodeList ms = d.getElementsByTagName("measure");
		for (int i = 0; i < ms.getLength(); i++) {
			Element m = (Element) ms.item(i);
			int num = Integer.parseInt(m.getAttribute("number"));
			if (num < from || num > to) {
				continue;
			}
			bars.add(ps.size());
			int onset = 0;
			NodeList ns = m.getElementsByTagName("note");
			for (int k = 0; k < ns.getLength(); k++) {
				Element e = (Element) ns.item(k);
				int dur = Integer.parseInt(e.getElementsByTagName("duration").item(0).getTextContent());
				NodeList p = e.getElementsByTagName("pitch");
				if (p.getLength() > 0 && e.getElementsByTagName("chord").getLength() == 0) {
					Element pe = (Element) p.item(0);
					String s = pe.getElementsByTagName("step").item(0).getTextContent();
					int oct = Integer.parseInt(pe.getElementsByTagName("octave").item(0).getTextContent());
					int alter = pe.getElementsByTagName("alter").getLength() > 0 ? Integer.parseInt(pe.getElementsByTagName("alter").item(0).getTextContent()) : 0;
					int pc = "CDEFGAB".indexOf(s) < 0 ? 0 : new int[] {0, 2, 4, 5, 7, 9, 11}["CDEFGAB".indexOf(s)];
					ps.add(12 * (oct + 1) + pc + alter);
					double beat = 1 + onset / 10080.0;   // divisions: quarter = 10080
					ls.add(s + (alter == 1 ? "#" : alter == -1 ? "b" : "") + oct + " (bar " + num + ", beat " + String.format("%.3f", beat).replaceAll("0+$", "").replaceAll("\\.$", "") + ")");
				}
				onset += dur;
			}
		}
	}

	static void draw(String png, int[] pitch, List<Integer> bars, List<List<Integer>> allowed, int[][] early, int[][] late, int n) throws Exception {
		int cw = 34, left = 30, top = 40;
		int lo = 99, hi = 0;
		for (int p : pitch) {
			lo = Math.min(lo, p);
			hi = Math.max(hi, p);
		}
		int ph = 9;
		int w = left * 2 + n * cw, h = top + (hi - lo + 1) * ph + 150;
		BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = im.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		int base = top + (hi - lo + 1) * ph;
		for (List<Integer> cuts : allowed) {
			if (cuts.isEmpty()) {
				continue;
			}
			int x0 = left + cuts.get(0) * cw - cw / 2, x1 = left + cuts.get(cuts.size() - 1) * cw + cw / 2;
			g.setColor(new Color(255, 214, 140));
			g.fillRect(x0, top - 10, x1 - x0, base - top + 20);
		}
		g.setColor(new Color(60, 60, 60));
		g.setStroke(new BasicStroke(1f));
		for (int b : bars) {
			g.drawLine(left + b * cw - cw / 2, top - 14, left + b * cw - cw / 2, base + 10);
		}
		int[] opens = Geometry.VIOLIN.open;
		for (int o : opens) {
			if (o >= lo && o <= hi) {
				g.setColor(new Color(200, 200, 230));
				int y = top + (hi - o) * ph + ph / 2;
				g.drawLine(left - 10, y, w - left, y);
			}
		}
		g.setColor(Color.BLACK);
		for (int i = 0; i < n; i++) {
			int y = top + (hi - pitch[i]) * ph;
			g.fillRoundRect(left + i * cw - cw / 2 + 3, y, cw - 6, ph - 2, 4, 4);
		}
		for (int pass = 0; pass < 2; pass++) {
			int[][] p = pass == 0 ? early : late;
			int y = base + 30 + pass * 40;
			g.setColor(Color.DARK_GRAY);
			g.drawString(pass == 0 ? "shifts as early as possible" : "shifts as late as possible", left, y - 8);
			for (int[] fr : p) {
				int x0 = left + fr[0] * cw - cw / 2 + 2, x1 = left + (fr[1] + 1) * cw - cw / 2 - 2;
				g.setColor(pass == 0 ? new Color(120, 170, 220) : new Color(150, 200, 150));
				g.fillRect(x0, y, x1 - x0, 14);
			}
		}
		ImageIO.write(im, "png", new File(png));
	}
}

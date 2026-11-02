package computingmusic.hand;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import computingmusic.TopologyBridge;
import computingmusic.instrument.InstrumentSpec;

/**
 * FINGER FRAMES, strict.  A frame is an ARRANGEMENT of the strings: each string is open (0) or stopped at a height
 * (semitones above the open string), and the hand holds all of it, immobile.  A frame plays exactly the pitches the
 * arrangement gives (an open string, or the stopped height on its own string): no finger moves to another string.
 *
 * Which arrangements are possible is the engine's fingering Topology:
 *   - a name beginning BAD_ is impossible;
 *   - UNKNOWN is NOT decided: it is reported (arrangement, heights, the provisional outline name) and left out;
 *   - any other name is possible, provided the hand can hold the distinct stopped heights (FrameTest).
 *
 * A run of notes is covered by a frame when every pitch of the run is one the frame plays.  For each start note the run is
 * extended until no frame covers it.  A frame is reported as MINIMAL for a run when no possible arrangement made by lifting
 * some of its stopped fingers (setting those strings open) also covers the run.  Nothing is ranked or chosen.
 *
 *   java -cp build/classes computingmusic.hand.FingerFrames SCORE.musicxml FROM_MEASURE TO_MEASURE [absolute]
 *
 * Violin; single notes only (chords not handled yet).  Complexity: candidate arrangements C (at most the product over the
 * strings of the heights the piece's pitches reach, about 10^3-10^4); extending a run costs O(C) per note.
 */
public final class FingerFrames {
	public static final class Frame {
		public final int[] height;      // per string, 0 = open
		public final String topology;   // the engine's name
		final BitSet plays = new BitSet();

		Frame(int[] height, String topology, int[] open) {
			this.height = height;
			this.topology = topology;
			for (int s = 0; s < height.length; s++) {
				plays.set(open[s] + height[s]);
			}
		}

		boolean liftedFrom(Frame a) {   // this = a with some stopped strings set open
			boolean different = false;
			for (int s = 0; s < height.length; s++) {
				if (height[s] != a.height[s]) {
					if (height[s] != 0) {
						return false;
					}
					different = true;
				}
			}
			return different;
		}
	}

	/** Names the Topology gives that EH has not yet ruled possible (all flaps FLAPLO/FLAPMID/FLAPHI are possible by his ruling): reported, left out of the frames. */
	static final java.util.Set<String> NOT_YET_RULED = new java.util.HashSet<String>(Arrays.asList("TWINFLAP", "TRIANGLEHI"));

	final Geometry g;
	final int[][] event;     // the pitches sounding together at each moment: one for a note, several for a chord
	final String[] label;
	final List<Frame> frames = new ArrayList<Frame>();
	/** UNKNOWN to the engine: the rank pattern -> {provisional name, an example "heights", how many}. */
	final TreeMap<String, String[]> unknown = new TreeMap<String, String[]>();
	final TreeMap<String, Integer> unknownCount = new TreeMap<String, Integer>();
	final TreeMap<String, List<int[]>> examples = new TreeMap<String, List<int[]>>();
	int candidates, impossible;

	FingerFrames(Geometry g, FrameTest test, int maxHeight, int[][] event, String[] label) {
		this.g = g;
		this.event = event;
		this.label = label;
		int n = g.open.length;
		List<List<Integer>> options = new ArrayList<List<Integer>>();
		for (int s = 0; s < n; s++) {
			TreeSet<Integer> hs = new TreeSet<Integer>();
			hs.add(0);
			for (int[] e : event) {
				for (int p : e) {
					int h = p - g.open[s];
					if (h >= 1 && h <= maxHeight) {
						hs.add(h);
					}
				}
			}
			options.add(new ArrayList<Integer>(hs));
		}
		build(test, options, new int[n], 0);
	}

	private void build(FrameTest test, List<List<Integer>> options, int[] cur, int s) {
		if (s == cur.length) {
			candidates++;
			TreeSet<Integer> distinct = new TreeSet<Integer>();
			for (int h : cur) {
				if (h > 0) {
					distinct.add(h);
				}
			}
			int[] hs = new int[distinct.size()];
			int i = 0;
			for (int h : distinct) {
				hs[i++] = h;
			}
			if (hs.length > 0 && !test.feasible(hs)) {
				impossible++;
				return;
			}
			Point[] points = new Point[cur.length];
			for (int k = 0; k < cur.length; k++) {
				points[k] = new Point(k, cur[k]);
			}
			String name = TopologyBridge.classify(points);
			if (name.startsWith("BAD_")) {
				impossible++;
			} else if (name.equals("UNKNOWN") || NOT_YET_RULED.contains(name)) {
				String pattern = pattern(cur);
				unknownCount.merge(pattern, 1, Integer::sum);
				List<int[]> ex = examples.computeIfAbsent(name, k -> new ArrayList<int[]>());
				if (ex.size() < 4 && !ex.contains(cur)) {
					ex.add(cur.clone());
				}
				unknown.putIfAbsent(pattern, new String[] {name, Arrays.toString(cur)});
			} else {
				frames.add(new Frame(cur.clone(), name, g.open));
			}
			return;
		}
		for (int h : options.get(s)) {
			cur[s] = h;
			build(test, options, cur, s + 1);
		}
	}

	/** The arrangement as OutlineNames reads it: 'o' for open, else the rank of the height among the stopped heights. */
	static String pattern(int[] height) {
		TreeSet<Integer> distinct = new TreeSet<Integer>();
		for (int h : height) {
			if (h > 0) {
				distinct.add(h);
			}
		}
		List<Integer> ranked = new ArrayList<Integer>(distinct);
		StringBuilder b = new StringBuilder();
		for (int s = 0; s < height.length; s++) {
			b.append(s == 0 ? "" : " ").append(height[s] == 0 ? "o" : String.valueOf(ranked.indexOf(height[s]) + 1));
		}
		return b.toString();
	}

	/** The frames that cover notes i..j and are minimal. */
	List<Frame> minimal(int i, int j) {
		List<Frame> hit = new ArrayList<Frame>();
		for (Frame f : frames) {
			boolean all = true;
			for (int n = i; n <= j && all; n++) {
				all = plays(f, event[n]);
			}
			if (all) {
				hit.add(f);
			}
		}
		List<Frame> out = new ArrayList<Frame>();
		for (Frame a : hit) {
			boolean larger = false;
			for (Frame b : hit) {
				if (b != a && b.liftedFrom(a)) {
					larger = true;
					break;
				}
			}
			if (!larger) {
				out.add(a);
			}
		}
		return out;
	}

	/** Whether the frame plays all of the pitches at once, each on a string of its own.  Complexity: O(k!) for k <= 4 pitches. */
	boolean plays(Frame f, int[] pitches) {
		for (int p : pitches) {
			if (!f.plays.get(p)) {
				return false;
			}
		}
		return assign(f, pitches, 0, 0);
	}

	private boolean assign(Frame f, int[] pitches, int k, int usedStrings) {
		if (k == pitches.length) {
			return true;
		}
		for (int s = 0; s < f.height.length; s++) {
			if ((usedStrings & (1 << s)) == 0 && g.open[s] + f.height[s] == pitches[k] && assign(f, pitches, k + 1, usedStrings | (1 << s))) {
				return true;
			}
		}
		return false;
	}

	/** reach[i] = the last note of the longest run from i that some frame covers; i - 1 when not even note i is. */
	int[] reach() {
		int n = event.length;
		int[] r = new int[n];
		for (int i = 0; i < n; i++) {
			int j = i - 1;
			while (j + 1 < n && !minimal(i, j + 1).isEmpty()) {
				j++;
			}
			r[i] = j;
		}
		return r;
	}

	String show(Frame f) {
		StringBuilder b = new StringBuilder();
		for (int s = 0; s < f.height.length; s++) {
			b.append(g.stringNames[s]).append(':').append(f.height[s] == 0 ? "o" : String.valueOf(f.height[s])).append(' ');
		}
		return b.append(' ').append(f.topology).toString();
	}

	/**
	 * The moments of the score: every note struck at one onset, whatever its voice, is one moment (a chord, written either with
	 * <chord/> or as voices with <backup>).  Notes held over from an earlier moment are NOT counted yet.
	 */
	static void readEvents(String file, int from, int to, List<int[]> events, List<String> labels) throws Exception {
		org.w3c.dom.Document d = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.File(file));
		double divisions = Double.parseDouble(d.getElementsByTagName("divisions").item(0).getTextContent().trim());
		org.w3c.dom.NodeList ms = d.getElementsByTagName("measure");
		for (int i = 0; i < ms.getLength(); i++) {
			org.w3c.dom.Element m = (org.w3c.dom.Element) ms.item(i);
			int num = Integer.parseInt(m.getAttribute("number"));
			if (num < from || num > to) {
				continue;
			}
			java.util.TreeMap<Integer, List<Integer>> at = new java.util.TreeMap<Integer, List<Integer>>();
			int onset = 0, lastStart = 0;
			org.w3c.dom.NodeList kids = m.getChildNodes();
			for (int k = 0; k < kids.getLength(); k++) {
				if (!(kids.item(k) instanceof org.w3c.dom.Element)) {
					continue;
				}
				org.w3c.dom.Element e = (org.w3c.dom.Element) kids.item(k);
				String tag = e.getTagName();
				if (tag.equals("backup") || tag.equals("forward")) {
					int dur = Integer.parseInt(e.getElementsByTagName("duration").item(0).getTextContent().trim());
					onset += tag.equals("backup") ? -dur : dur;
				} else if (tag.equals("note")) {
					boolean chordMember = e.getElementsByTagName("chord").getLength() > 0;
					int start = chordMember ? lastStart : onset;
					org.w3c.dom.NodeList p = e.getElementsByTagName("pitch");
					if (p.getLength() > 0) {
						org.w3c.dom.Element pe = (org.w3c.dom.Element) p.item(0);
						String st = pe.getElementsByTagName("step").item(0).getTextContent();
						int oct = Integer.parseInt(pe.getElementsByTagName("octave").item(0).getTextContent());
						int alter = pe.getElementsByTagName("alter").getLength() > 0 ? (int) Math.round(Double.parseDouble(pe.getElementsByTagName("alter").item(0).getTextContent())) : 0;
						int pc = new int[] {0, 2, 4, 5, 7, 9, 11}["CDEFGAB".indexOf(st)];
						List<Integer> l = at.get(start);
						if (l == null) {
							l = new ArrayList<Integer>();
							at.put(start, l);
						}
						int midi = 12 * (oct + 1) + pc + alter;
						if (!l.contains(midi)) {
							l.add(midi);
						}
					}
					lastStart = start;
					if (!chordMember) {
						onset += Integer.parseInt(e.getElementsByTagName("duration").item(0).getTextContent().trim());
					}
				}
			}
			for (java.util.Map.Entry<Integer, List<Integer>> en : at.entrySet()) {
				int[] ev = new int[en.getValue().size()];
				for (int q = 0; q < ev.length; q++) {
					ev[q] = en.getValue().get(q);
				}
				java.util.Arrays.sort(ev);
				events.add(ev);
				String beat = String.format("%.3f", 1 + en.getKey() / divisions).replaceAll("0+$", "").replaceAll("\\.$", "");
				labels.add(name(ev[0]) + (ev.length > 1 ? "+" + (ev.length - 1) : "") + " (bar " + num + ", beat " + beat + ")");
			}
		}
	}

	/** ABC name, C = 60 -> C, c = 72 -> c, C, = 48 (sharps ^, flats _). */
	public static String name(int midi) {
		String[] pcs = {"C", "^C", "D", "^D", "E", "F", "^F", "G", "^G", "A", "^A", "B"};
		int oct = midi / 12 - 5;                       // 60 -> 0
		String n = pcs[midi % 12];
		String letter = n.substring(n.length() - 1);
		String acc = n.length() > 1 ? n.substring(0, 1) : "";
		if (oct >= 1) {
			StringBuilder b = new StringBuilder(acc + letter.toLowerCase());
			for (int q = 1; q < oct; q++) {
				b.append("'");
			}
			return b.toString();
		}
		StringBuilder b = new StringBuilder(acc + letter);
		for (int q = 0; q > oct; q--) {
			b.append(",");
		}
		return b.toString();
	}

	/** An arrangement drawn: one column per string (lowest left), heights downward from the nut, X = a finger, o = open; with the pitches. */
	String draw(int[] h) {
		int max = 0;
		for (int x : h) {
			max = Math.max(max, x);
		}
		StringBuilder b = new StringBuilder();
		b.append("        ");
		for (int s = 0; s < h.length; s++) {
			b.append(String.format("%-5s", g.stringNames[s]));
		}
		b.append("\n");
		for (int row = 0; row <= max; row++) {
			boolean any = row == 0;
			for (int x : h) {
				any |= x == row;
			}
			if (!any) {
				continue;
			}
			b.append(String.format("   %2d   ", row));
			for (int s = 0; s < h.length; s++) {
				b.append(String.format("%-5s", row == 0 ? (h[s] == 0 ? "o" : "|") : (h[s] == row ? "X" : "|")));
			}
			b.append("\n");
		}
		b.append("        ");
		for (int s = 0; s < h.length; s++) {
			b.append(String.format("%-5s", name(g.open[s] + h[s])));
		}
		return b.append("   (pitches)").toString();
	}

	/** A maximal run: notes start..end (indexes of the moments) and its minimal frames. */
	public static final class Run {
		public final int start, end;
		public final List<Frame> frames;

		Run(int start, int end, List<Frame> frames) {
			this.start = start;
			this.end = end;
			this.frames = frames;
		}
	}

	/** The measure numbers of a MusicXML file: {first, last}. */
	public static int[] measureRange(String file) throws Exception {
		org.w3c.dom.NodeList ms = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.File(file)).getElementsByTagName("measure");
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		for (int i = 0; i < ms.getLength(); i++) {
			try {
				int n = Integer.parseInt(((org.w3c.dom.Element) ms.item(i)).getAttribute("number"));
				lo = Math.min(lo, n);
				hi = Math.max(hi, n);
			} catch (NumberFormatException e) {
				// an implicit or text measure number: skipped
			}
		}
		return new int[] {lo, hi};
	}

	/** The frames of bars from..to of a violin MusicXML score.  Complexity: see the class comment. */
	public static FingerFrames analyse(String file, int from, int to, boolean absolute) throws Exception {
		List<int[]> evs = new ArrayList<int[]>();
		List<String> ls = new ArrayList<String>();
		readEvents(file, from, to, evs, ls);
		Geometry g = Geometry.VIOLIN;
		return new FingerFrames(g, new FrameTest(g, HandParams.forInstrument(InstrumentSpec.VIOLIN), absolute), 24, evs.toArray(new int[0][]), ls.toArray(new String[0]));
	}

	public String[] labels() {
		return label;
	}

	public int[][] events() {
		return event;
	}

	public Geometry geometry() {
		return g;
	}

	/** The topology names left undecided (UNKNOWN, TWINFLAP, TRIANGLEHI): how many arrangement patterns were left out. */
	public int leftOutPatterns() {
		return unknown.size();
	}

	/** The maximal runs in order: a run no earlier start reaches as far as.  Runs no frame holds (even alone) are left out. */
	public List<Run> maximalRuns() {
		int[] reach = reach();
		List<Run> out = new ArrayList<Run>();
		int lastEnd = -1;
		for (int i = 0; i < reach.length; i++) {
			if (reach[i] <= lastEnd || reach[i] < i) {
				continue;
			}
			lastEnd = reach[i];
			out.add(new Run(i, reach[i], minimal(i, reach[i])));
		}
		return out;
	}

	public static void main(String[] a) throws Exception {
		String file = a[0];
		int from = Integer.parseInt(a[1]), to = Integer.parseInt(a[2]);
		boolean absolute = a.length > 3 && a[3].equals("absolute");
		List<int[]> evs = new ArrayList<int[]>();
		List<String> ls = new ArrayList<String>();
		readEvents(file, from, to, evs, ls);
		int n = evs.size();
		int[][] pitch = evs.toArray(new int[0][]);
		String[] label = ls.toArray(new String[0]);
		Geometry g = Geometry.VIOLIN;
		FingerFrames ff = new FingerFrames(g, new FrameTest(g, HandParams.forInstrument(InstrumentSpec.VIOLIN), absolute), 24, pitch, label);
		System.out.println(n + " notes, " + (absolute ? "absolute" : "comfortable") + " reach.  " + ff.candidates + " arrangements tried: "
				+ ff.frames.size() + " possible, " + ff.impossible + " impossible (BAD_ or beyond the hand), "
				+ ff.unknown.size() + " patterns the engine cannot name.");
		int[] reach = ff.reach();
		System.out.println();
		System.out.println("FROM EACH NOTE, how far one immobile hand reaches:");
		for (int i = 0; i < n; i++) {
			int len = reach[i] - i + 1;
			System.out.printf("  %-26s %3d notes%s%n", label[i], len, len == 0 ? "   (no frame holds even this note)" : len == 1 ? "   (alone)" : "   to " + label[reach[i]]);
		}
		System.out.println();
		System.out.println("MAXIMAL RUNS (no earlier start reaches as far) and their minimal frames:");
		int lastEnd = -1;
		for (int i = 0; i < n; i++) {
			if (reach[i] <= lastEnd || reach[i] < i) {
				continue;
			}
			lastEnd = reach[i];
			List<Frame> fr = ff.minimal(i, reach[i]);
			TreeMap<String, List<Frame>> shapes = new TreeMap<String, List<Frame>>();
			for (Frame f : fr) {
				shapes.computeIfAbsent(f.topology + "  " + pattern(f.height), k -> new ArrayList<Frame>()).add(f);
			}
			System.out.println("  " + label[i] + " .. " + label[reach[i]] + "  (" + (reach[i] - i + 1) + " notes): " + fr.size() + " frames, " + shapes.size() + " shapes");
			for (Map.Entry<String, List<Frame>> e : shapes.entrySet()) {
				StringBuilder sb = new StringBuilder("      " + e.getKey() + "   ");
				for (Frame f : e.getValue()) {
					StringBuilder h = new StringBuilder();
					for (int s = 0; s < f.height.length; s++) {
						h.append(f.height[s] == 0 ? "o" : String.valueOf(f.height[s])).append(s + 1 < f.height.length ? "," : "");
					}
					sb.append(" [").append(h).append("]");
				}
				System.out.println(sb);
			}
		}
		System.out.println();
		System.out.println("NOT YET RULED POSSIBLE (UNKNOWN, TWINFLAP or TRIANGLEHI; left out; look at these):");
		if (ff.unknown.isEmpty()) {
			System.out.println("  none");
		}
		for (Map.Entry<String, String[]> e : ff.unknown.entrySet()) {
			System.out.println("  strings " + String.join(",", g.stringNames) + "  pattern " + e.getKey() + "   named: " + e.getValue()[0]
					+ "   e.g. heights " + e.getValue()[1] + "   (" + ff.unknownCount.get(e.getKey()) + " arrangements)");
		}
		if (a.length > 4 && a[4].equals("examples")) {
			for (Map.Entry<String, List<int[]>> e : ff.examples.entrySet()) {
				System.out.println();
				System.out.println("EXAMPLES of " + e.getKey() + ":");
				for (int[] h : e.getValue()) {
					System.out.println(ff.draw(h));
					System.out.println();
				}
			}
		}
	}
}

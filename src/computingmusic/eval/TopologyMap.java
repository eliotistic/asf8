package computingmusic.eval;

import java.awt.Point;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import computingmusic.TopologyBridge;
import computingmusic.TrailRun;

/**
 * A MAP of the engine's fingering topology: for every ARRANGEMENT of the four strings (which are open, and for the stopped
 * ones the order of their heights, ties allowed) the names the engine's Topology class gives it.  The arrangement leaves out
 * the size of the gaps between heights, which some of the engine's rules read (the triangles: a gap of 3 or more, a gap over
 * 2), so each arrangement is tried with every combination of gaps from {1, 2, 3, 5} and the set of names is reported:
 * one name = the arrangement is named whatever the gaps; several = the name depends on the gaps; UNKNOWN alone = the
 * topology does not name this arrangement at all.
 * An arrangement is written as the four strings from the lowest, 'o' for open and a rank for a stopped string (1 = lowest
 * height, equal ranks = the same height): "1 2 3 4" is heights rising from the lowest string to the highest.
 * Usage: TopologyMap   Output: a summary, and ~/.string_fingering/chords/topology_map.csv.
 * Complexity: arrangements (at most 4 x 75 + ...) x gap combinations (4^3) calls to Topology.
 */
public class TopologyMap {
	static final int[] GAPS = {1, 2, 3, 5};

	public static void main(String[] args) throws Exception {
		TrailRun.violin();                                          // loads SFProperties.xml: the triangle thresholds
		TreeMap<String, TreeSet<String>> names = new TreeMap<String, TreeSet<String>>();
		TreeMap<String, Integer> openCount = new TreeMap<String, Integer>();
		for (int open = 0; open < 16; open++) {                     // bit s set: string s is open
			List<Integer> stopped = new ArrayList<Integer>();
			for (int s = 0; s < 4; s++) {
				if ((open & (1 << s)) == 0) {
					stopped.add(s);
				}
			}
			int k = stopped.size();
			for (int[] ranks : weakOrders(k)) {
				int distinct = 0;
				for (int r : ranks) {
					distinct = Math.max(distinct, r);
				}
				String arrangement = arrangement(open, stopped, ranks);
				TreeSet<String> found = new TreeSet<String>();
				for (int[] gap : gapCombinations(distinct - 1)) {
					int[] height = new int[Math.max(distinct, 1) + 1];
					height[1] = 1;
					for (int r = 2; r <= distinct; r++) {
						height[r] = height[r - 1] + gap[r - 2];
					}
					Point[] points = new Point[4];
					for (int s = 0; s < 4; s++) {
						points[s] = new Point(s, 0);
					}
					for (int i = 0; i < k; i++) {
						points[stopped.get(i)] = new Point(stopped.get(i), height[ranks[i]]);
					}
					found.add(TopologyBridge.classify(points));
				}
				TreeSet<String> provisional = new TreeSet<String>();
				for (String n : found) {
					provisional.add(OutlineNames.name(arrangement, n));
				}
				names.put(arrangement, provisional);
				openCount.put(arrangement, Integer.bitCount(open));
			}
		}
		File dir = new File(System.getProperty("user.home"), ".string_fingering/chords");
		dir.mkdirs();
		TreeMap<String, List<String>> bySet = new TreeMap<String, List<String>>();
		try (PrintWriter out = new PrintWriter(new File(dir, "topology_map.csv"))) {
			out.println("arrangement,open_strings,names_the_engine_gives");
			for (String a : names.keySet()) {
				out.println(a.replace(' ', '|') + "," + openCount.get(a) + "," + String.join("/", names.get(a)));
				bySet.computeIfAbsent(String.join("/", names.get(a)), x -> new ArrayList<String>()).add(a);
			}
		}
		System.out.println("ARRANGEMENTS of the four strings (open / ranked heights) and what the engine's Topology calls them: " + names.size() + " arrangements");
		for (java.util.Map.Entry<String, List<String>> e : bySet.entrySet()) {
			System.out.printf("%n%s: %d arrangements%n", e.getKey(), e.getValue().size());
			int shown = 0;
			for (String a : e.getValue()) {
				if (shown++ < 14) {
					System.out.print("   " + a);
				}
			}
			System.out.println(e.getValue().size() > 14 ? "   ..." : "");
		}
		int total4 = 0, unknown4 = 0;
		for (String a : names.keySet()) {
			if (openCount.get(a) == 0) {
				total4++;
				if (names.get(a).equals(new TreeSet<String>(java.util.Arrays.asList("UNKNOWN")))) {
					unknown4++;
				}
			}
		}
		System.out.printf("%nall four strings stopped: %d arrangements, %d of them never named (UNKNOWN whatever the gaps)%n", total4, unknown4);
		System.out.println("written: " + new File(dir, "topology_map.csv"));
	}

	static String arrangement(int open, List<Integer> stopped, int[] ranks) {
		StringBuilder sb = new StringBuilder();
		for (int s = 0; s < 4; s++) {
			String part = "o";
			int i = stopped.indexOf(s);
			if (i >= 0) {
				part = String.valueOf(ranks[i]);
			}
			sb.append(s > 0 ? " " : "").append(part);
		}
		return sb.toString();
	}

	/** Every assignment of ranks 1..m to k items using each rank at least once.  Complexity: O(m^k). */
	static List<int[]> weakOrders(int k) {
		List<int[]> out = new ArrayList<int[]>();
		if (k == 0) {
			out.add(new int[0]);
			return out;
		}
		int total = 1;
		for (int i = 0; i < k; i++) {
			total *= k;
		}
		for (int code = 0; code < total; code++) {
			int[] r = new int[k];
			int c = code, max = 0;
			for (int i = 0; i < k; i++) {
				r[i] = c % k + 1;
				c /= k;
				max = Math.max(max, r[i]);
			}
			boolean ok = true;
			for (int rank = 1; rank <= max; rank++) {
				boolean used = false;
				for (int x : r) {
					used |= x == rank;
				}
				ok &= used;
			}
			if (ok) {
				out.add(r);
			}
		}
		return out;
	}

	static List<int[]> gapCombinations(int count) {
		List<int[]> out = new ArrayList<int[]>();
		int total = 1;
		for (int i = 0; i < count; i++) {
			total *= GAPS.length;
		}
		for (int code = 0; code < total; code++) {
			int[] g = new int[Math.max(count, 0)];
			int c = code;
			for (int i = 0; i < count; i++) {
				g[i] = GAPS[c % GAPS.length];
				c /= GAPS.length;
			}
			out.add(g);
		}
		return out;
	}
}

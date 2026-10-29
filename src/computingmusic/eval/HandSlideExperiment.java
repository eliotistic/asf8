package computingmusic.eval;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.Json;
import computingmusic.jj.MusicXmlSession;

/**
 * An experiment, read-only: how often can the HAND SLIDE?  A hand slide is the whole hand moving to a new place with
 * the fingers arranged as before, so that the notes of one unit are the notes of the one before moved by one
 * distance, on the same strings.  Nothing of the engine or the solver is changed: the experiment reads the score
 * (JJ) and the placements the solver wrote (~/.string_fingering/solutions/NAME.solution.json; run solve.Solve first).
 *
 * For each pair of neighbouring measures U, V (units are measures; a boundary of the solver's stretches is a second kind
 * of unit, see below) it reports:
 *   PITCH SET   best distance d (-12..12, not 0) with the most pitches of V among U's moved by d;
 *   SEQUENCE    if U and V have as many notes, how many notes of V are the note of U at the same place moved by d;
 *   STRINGS     of those, how many are on the same string as in U, with the height moved by the same d
 *               (the fingers keep their strings and move together).
 * A pair is a SLIDE CANDIDATE when SEQUENCE >= 0.75 and STRINGS >= 0.75 for one d with 0 < |d| <= SPAN; a REPEAT when
 * d = 0 would do; otherwise the shapes differ.  Also counted: neighbouring single notes the solver put on the same
 * string a small distance apart (where one finger could have carried), and how many of them the solver gave the same finger.
 *
 * Usage: HandSlideExperiment FILE.musicxml [span]   Complexity: O(notes x 25) per pair.
 */
public class HandSlideExperiment {
	static final double THRESHOLD = 0.75;

	/** One placed note. */
	static class N {
		int item, pitch, string, height, finger, measure;
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		int span = args.length > 1 ? Integer.parseInt(args[1]) : 7;
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.instrumentNamed(null);
		TrailRun.Items items = TrailRun.items(score, ins);
		File sf = new File(System.getProperty("user.home"), ".string_fingering/solutions/" + base + ins.spec.solutionFileSuffix() + ".solution.json");
		List<Object> stretches = (List<Object>) Json.parse(new String(Files.readAllBytes(sf.toPath()), StandardCharsets.UTF_8));
		java.util.List<String> names = java.util.Arrays.asList(ins.spec.stringNames);
		List<N> notes = new ArrayList<N>();
		List<int[]> stretchEnds = new ArrayList<int[]>();      // first and last item of each stretch
		for (Object so : stretches) {
			Map<String, Object> s = (Map<String, Object>) so;
			stretchEnds.add(new int[] {((Number) s.get("firstItem")).intValue(), ((Number) s.get("lastItem")).intValue()});
			for (Object no : (List<Object>) s.get("notes")) {
				Map<String, Object> m = (Map<String, Object>) no;
				N n = new N();
				n.item = ((Number) m.get("item")).intValue();
				n.pitch = ((Number) m.get("pitch")).intValue();
				n.string = names.indexOf((String) m.get("string"));
				n.height = ((Number) m.get("height")).intValue();
				n.finger = m.containsKey("finger") ? ((Number) m.get("finger")).intValue() : -1;
				n.measure = items.notes.get(n.item).get(0).measure();
				notes.add(n);
			}
		}
		notes.sort((a, b) -> a.item != b.item ? Integer.compare(a.item, b.item) : Integer.compare(a.pitch, b.pitch));
		TreeMap<Integer, List<N>> byMeasure = new TreeMap<Integer, List<N>>();
		for (N n : notes) {
			byMeasure.computeIfAbsent(n.measure, k -> new ArrayList<N>()).add(n);
		}
		System.out.printf("%s: %d notes, %d measures; slide span %d semitones%n", base, notes.size(), byMeasure.size(), span);
		System.out.println("measure pair | pitch-set d (share) | sequence d (share) | same strings (share) | verdict");
		int candidates = 0, repeats = 0, differ = 0, pairs = 0;
		TreeMap<Integer, Integer> candidateShifts = new TreeMap<Integer, Integer>();
		Integer previous = null;
		for (int m : byMeasure.keySet()) {
			if (previous != null && m == previous + 1) {
				pairs++;
				List<N> u = byMeasure.get(previous), v = byMeasure.get(m);
				// PITCH SET
				TreeSet<Integer> pu = new TreeSet<Integer>(), pv = new TreeSet<Integer>();
				for (N n : u) pu.add(n.pitch);
				for (N n : v) pv.add(n.pitch);
				int bestSetD = 0;
				double bestSet = -1;
				for (int d = -12; d <= 12; d++) {
					int common = 0;
					for (int p : pu) {
						if (pv.contains(p + d)) {
							common++;
						}
					}
					double share = (double) common / pv.size();
					if (share > bestSet + 1e-9 || (Math.abs(share - bestSet) < 1e-9 && d != 0 && Math.abs(d) < Math.abs(bestSetD))) {
						if (d != 0 || bestSet < 0) {
							bestSet = share;
							bestSetD = d;
						}
					}
				}
				// SEQUENCE and STRINGS (only when the two measures have as many notes)
				int bestSeqD = 0;
				double bestSeq = 0, bestStr = 0;
				if (u.size() == v.size()) {
					for (int d = -span; d <= span; d++) {
						if (d == 0) {
							continue;
						}
						int seq = 0, str = 0;
						for (int k = 0; k < u.size(); k++) {
							if (v.get(k).pitch - u.get(k).pitch == d) {
								seq++;
								if (v.get(k).string == u.get(k).string && v.get(k).height - u.get(k).height == d) {
									str++;
								}
							}
						}
						double s = (double) seq / u.size();
						if (s > bestSeq) {
							bestSeq = s;
							bestSeqD = d;
							bestStr = seq == 0 ? 0 : (double) str / u.size();
						}
					}
				}
				boolean same = u.size() == v.size();
				int zero = 0;
				if (same) {
					for (int k = 0; k < u.size(); k++) {
						if (u.get(k).pitch == v.get(k).pitch) {
							zero++;
						}
					}
				}
				String verdict;
				if (same && (double) zero / u.size() >= THRESHOLD) {
					verdict = "REPEAT";
					repeats++;
				} else if (same && bestSeq >= THRESHOLD && bestStr >= THRESHOLD) {
					verdict = "SLIDE candidate (d=" + bestSeqD + ")";
					candidates++;
					candidateShifts.merge(bestSeqD, 1, Integer::sum);
				} else {
					verdict = "shapes differ";
					differ++;
				}
				System.out.printf("m%-3d -> m%-3d | %+3d (%3.0f%%) | %s | %s | %s%n", previous, m, bestSetD, 100 * bestSet,
						same ? String.format("%+3d (%3.0f%%)", bestSeqD, 100 * bestSeq) : "different lengths " + u.size() + "/" + v.size(),
						same ? String.format("%3.0f%%", 100 * bestStr) : "-", verdict);
			}
			previous = m;
		}
		System.out.printf("%nneighbouring measures: %d pairs: %d SLIDE candidates %s, %d repeats, %d with different shapes%n", pairs, candidates, candidateShifts, repeats, differ);
		// neighbouring single notes on the same string a small distance apart
		int adjacent = 0, sameString = 0, withinSpan = 0, sameFinger = 0, sameFingerSmall = 0;
		TreeMap<Integer, Integer> distHist = new TreeMap<Integer, Integer>();
		for (int i = 0; i + 1 < notes.size(); i++) {
			N a = notes.get(i), b = notes.get(i + 1);
			if (b.item != a.item + 1) {
				continue;
			}
			boolean singleA = i == 0 || notes.get(i - 1).item != a.item, singleB = i + 2 >= notes.size() || notes.get(i + 2).item != b.item;
			if (!singleA || !singleB) {
				continue;
			}
			adjacent++;
			if (a.string == b.string && a.height != b.height) {
				sameString++;
				int d = Math.abs(b.height - a.height);
				if (d <= 4) {
					withinSpan++;
					distHist.merge(d, 1, Integer::sum);
					if (a.finger >= 0 && a.finger == b.finger) {
						sameFingerSmall++;
					}
				}
				if (a.finger >= 0 && a.finger == b.finger) {
					sameFinger++;
				}
			}
		}
		System.out.printf("neighbouring single notes: %d; on the same string, different height: %d (within 4 semitones: %d %s); the solver gave them the same finger: %d (within 4: %d)%n",
				adjacent, sameString, withinSpan, distHist, sameFinger, sameFingerSmall);
		// the solver's own stretch boundaries: does the guide finger carry across?
		int boundaries = 0, guide = 0;
		for (int k = 0; k + 1 < stretchEnds.size(); k++) {
			int last = stretchEnds.get(k)[1], first = stretchEnds.get(k + 1)[0];
			N a = null, b = null;
			for (N n : notes) {
				if (n.item == last) a = n;
				if (n.item == first) { b = n; break; }
			}
			if (a == null || b == null) {
				continue;
			}
			boundaries++;
			if (a.string == b.string && a.finger >= 0 && a.finger == b.finger && a.height != b.height && Math.abs(a.height - b.height) <= span) {
				guide++;
			}
		}
		System.out.printf("the solver's stretch boundaries: %d; at %d the same finger on the same string carries across (within %d semitones)%n", boundaries, guide, span);
		windowTest(notes, byMeasure, span);
	}

	/**
	 * THE WHOLE-HAND TEST at every place between two items: the placements (string, height) of the W items before the place
	 * and of the W items after it.  The hand can slide there if the "after" placements are the "before" ones moved by one
	 * distance d (0 < |d| <= span) on the same strings: the share of the "after" placements that are a "before"
	 * placement moved by d.  A place counts when that share is at least THRESHOLD and the two notes next to the place are on
	 * one string (a finger can carry across).  Complexity: O(places x 2 span x W).
	 */
	static void windowTest(List<N> notes, TreeMap<Integer, List<N>> byMeasure, int span) {
		int maxItem = notes.get(notes.size() - 1).item;
		for (int w : new int[] {4, 8}) {
			int places = 0, counted = 0, strongAny = 0;
			TreeMap<Integer, Integer> byD = new TreeMap<Integer, Integer>();
			StringBuilder where = new StringBuilder();
			double firstToSecond = -1;
			int firstToSecondD = 0;
			for (int c = w; c + w <= maxItem + 1; c++) {
				TreeSet<Long> before = new TreeSet<Long>(), after = new TreeSet<Long>();
				N last = null, first = null;
				for (N n : notes) {
					if (n.item >= c - w && n.item < c) {
						before.add(n.string * 100L + n.height);
						if (n.item == c - 1) last = n;
					} else if (n.item >= c && n.item < c + w) {
						after.add(n.string * 100L + n.height);
						if (n.item == c && first == null) first = n;
					}
				}
				if (before.isEmpty() || after.isEmpty() || last == null || first == null) {
					continue;
				}
				places++;
				double best = 0;
				int bestD = 0;
				for (int d = -span; d <= span; d++) {
					if (d == 0) {
						continue;
					}
					int common = 0;
					for (long b : before) {
						if (after.contains(b + d)) {
							common++;
						}
					}
					double share = (double) common / after.size();
					if (share > best + 1e-9) {
						best = share;
						bestD = d;
					}
				}
				if (best >= THRESHOLD) {
					strongAny++;
					if (last.string == first.string) {
						counted++;
						byD.merge(bestD, 1, Integer::sum);
						if (where.length() < 400) {
							where.append(" m").append(first.measure).append(String.format("(i%d,d%+d,%.0f%%)", c, bestD, 100 * best));
						}
					}
				}
				if (first.measure == 2 && last.measure == 1 && firstToSecond < 0) {
					firstToSecond = best;
					firstToSecondD = bestD;
				}
			}
			System.out.printf("%nwhole-hand test, windows of %d items: %d places; %d with a >=75%% translation; of those %d where the notes next to the place share a string %s%n",
					w, places, strongAny, counted, byD);
			System.out.println("  first places:" + where);
			System.out.printf("  the place between measure 1 and measure 2: best share %.0f%% at d=%+d%n", 100 * Math.max(firstToSecond, 0), firstToSecondD);
		}
	}
}

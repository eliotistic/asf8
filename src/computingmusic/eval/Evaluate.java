package computingmusic.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import computingmusic.FingerTrail;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.MusicXmlSession;

/**
 * Agreements between the printed fingerings, the trails, and the string/pitch patterns of the piece.
 * Definitions are in doc/EVALUATION.md (measures M0-M5).  Stretches are found NOTE BY NOTE (NoteByNote), never
 * from a grouped arpeggio start; TrailRun.run is called only to compare (M0).
 *
 * A note's "candidates" are the (string, height) spots it has over ALL live trails of its stretch; it is
 * "determined" when there is exactly one.  Nothing here picks a trail.
 *
 * Usage: Evaluate FILE.musicxml [WINDOW]   (reads ~/.string_fingering/cache/NAME/dump.json if present, else asks JJ)
 */
public class Evaluate {
	static final int SLACK = 1;   // PrintedCheck's slack, kept so the numbers are comparable
	static String[] STRING = { "G", "D", "A", "E" };

	/** What the live trails say about one note. */
	static class Cand {
		final JJNote note;
		final Set<Integer> spots = new TreeSet<Integer>();   // string * 100 + height
		int stretch;

		Cand(JJNote n) {
			note = n;
		}

		boolean determined() {
			return spots.size() == 1;
		}

		int string() {
			return spots.iterator().next() / 100;
		}
	}

	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		int window = args.length > 1 ? Integer.parseInt(args[1]) : 4;
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.instrumentNamed(null);
		STRING = ins.spec.stringNames;
		TrailRun.Items items = TrailRun.items(score, ins);
		System.out.println("piece " + base + ": " + score.notes.size() + " notes, " + items.chords.size() + " items; start rule: each stretch starts at ONE item, extended note by note");

		List<NoteByNote.Stretch> stretches = NoteByNote.run(ins, items.chords);
		m0(ins, items, stretches);
		Map<JJNote, Cand> cands = candidates(ins, items, stretches);
		m1m2(ins, items, stretches, cands);
		m3(items, cands);
		m4(items, cands, window);
		m5(ins, items, cands);
	}

	// ---- M0: how many trails remain per stretch, by start rule.  Complexity: O(stretches).
	static void m0(Instrument ins, TrailRun.Items items, List<NoteByNote.Stretch> st) {
		System.out.println("\nM0 trails alive per stretch");
		TreeMap<String, Integer> hist = new TreeMap<String, Integer>();
		int one = 0;
		for (NoteByNote.Stretch s : st) {
			if (s.end == NoteByNote.End.IMPOSSIBLE) {
				continue;
			}
			int n = s.alive.size();
			one += n == 1 ? 1 : 0;
			String b = n == 1 ? "1" : n <= 5 ? "2-5" : n <= 30 ? "6-30" : n <= 100 ? "31-100" : ">100";
			hist.put(b, hist.containsKey(b) ? hist.get(b) + 1 : 1);
		}
		int imp = 0, cap = 0;
		for (NoteByNote.Stretch s : st) {
			imp += s.end == NoteByNote.End.IMPOSSIBLE ? 1 : 0;
			cap += s.end == NoteByNote.End.CAP ? 1 : 0;
		}
		System.out.println("  note by note: " + st.size() + " stretches (" + imp + " impossible, " + cap + " stopped at the cap of " + NoteByNote.CAP + "), exactly one trail: " + one + "; histogram " + hist);
		List<TrailRun.Segment> grouped = TrailRun.run(ins, items.chords);
		int gone = 0;
		for (TrailRun.Segment s : grouped) {
			gone += s.trails == 1 ? 1 : 0;
		}
		System.out.println("  grouped arpeggio start (TrailRun.run): " + grouped.size() + " stretches, exactly one trail: " + gone);
	}

	/** Each note's candidate spots over the live trails of its stretch.  Complexity: O(trails x notes) per stretch. */
	static Map<JJNote, Cand> candidates(Instrument ins, TrailRun.Items items, List<NoteByNote.Stretch> st) throws Exception {
		Map<JJNote, Cand> out = new java.util.IdentityHashMap<JJNote, Cand>();
		for (int k = 0; k < st.size(); k++) {
			NoteByNote.Stretch s = st.get(k);
			for (int item = s.start; item <= s.stop; item++) {
				for (JJNote n : items.notes.get(item)) {
					Cand c = new Cand(n);
					c.stretch = k;
					out.put(n, c);
				}
			}
			for (FingerTrail t : s.alive) {
				List<List<NoteByNote.Spot>> rows = NoteByNote.spots(ins, items, s, t);
				for (int r = 0; r < rows.size(); r++) {
					List<JJNote> ns = items.notes.get(s.start + r);
					for (int q = 0; q < ns.size(); q++) {
						NoteByNote.Spot p = rows.get(r).get(q);
						if (p != null) {
							out.get(ns.get(q)).spots.add(p.string * 100 + p.height);
						}
					}
				}
			}
		}
		return out;
	}

	static int printedDigit(JJNote n) {
		String f = n.fingering();
		return f.isEmpty() || f.indexOf(',') >= 0 ? -1 : Integer.parseInt(f);
	}

	// ---- M1 (per note) and M2 (per stretch): printed digit against the live trails, rules OPEN and ORDER of PrintedCheck.
	// Complexity: O(trails x p^2) per stretch, p = printed notes in the stretch.
	static void m1m2(Instrument ins, TrailRun.Items items, List<NoteByNote.Stretch> st, Map<JJNote, Cand> cands) throws Exception {
		int printed = 0, two = 0, allOk = 0, someOk = 0, noneOk = 0, unplaced = 0;
		for (JJNote n : score(items)) {
			String f = n.fingering();
			if (f.isEmpty()) {
				continue;
			}
			if (f.indexOf(',') >= 0) {
				two++;
				continue;
			}
			printed++;
			Cand c = cands.get(n);
			if (c == null || c.spots.isEmpty()) {
				unplaced++;
				continue;
			}
			int d = printedDigit(n), ok = 0;
			for (int spot : c.spots) {
				ok += (d == 0) == (spot % 100 == 0) ? 1 : 0;
			}
			if (ok == c.spots.size()) {
				allOk++;
			} else if (ok > 0) {
				someOk++;
			} else {
				noneOk++;
			}
		}
		System.out.println("\nM1 printed digit vs trails, per note (OPEN only: 0 = open string, 1-4 = stopped); candidates over all live trails");
		System.out.println("  printed single digits " + printed + " (+" + two + " two-number, not read); not placed " + unplaced
				+ "; every candidate agrees " + allOk + ", some " + someOk + ", none " + noneOk);

		int withPrinted = 0, anyPass = 0, allPass = 0, nonePass = 0;
		long trailsTotal = 0, trailsPass = 0;
		int[] firstFail = new int[3];
		List<String> failures = new ArrayList<String>();
		for (int k = 0; k < st.size(); k++) {
			NoteByNote.Stretch s = st.get(k);
			List<int[]> pr = new ArrayList<int[]>();   // item, digit, noteIndex
			for (int item = s.start; item <= s.stop; item++) {
				List<JJNote> ns = items.notes.get(item);
				for (int q = 0; q < ns.size(); q++) {
					if (printedDigit(ns.get(q)) >= 0) {
						pr.add(new int[] { item, printedDigit(ns.get(q)), q });
					}
				}
			}
			if (pr.isEmpty() || s.alive.isEmpty()) {
				continue;
			}
			withPrinted++;
			int pass = 0;
			for (FingerTrail t : s.alive) {
				List<List<NoteByNote.Spot>> rows = NoteByNote.spots(ins, items, s, t);
				boolean good = true;
				for (int[] a : pr) {
					NoteByNote.Spot sa = rows.get(a[0] - s.start).get(a[2]);
					if (sa == null || (a[1] == 0) != (sa.height == 0)) {
						good = false;
						break;
					}
					if (a[1] == 0 || sa.height == 0) {
						continue;
					}
					for (int[] b : pr) {
						NoteByNote.Spot sb = rows.get(b[0] - s.start).get(b[2]);
						if (a == b || b[1] == 0 || sb == null || sb.height == 0 || a[1] == b[1]) {
							continue;
						}
						int slack = a[0] == b[0] ? 0 : SLACK;
						int gap = a[1] < b[1] ? sb.height - sa.height : sa.height - sb.height;
						if (gap < -slack) {
							good = false;
						}
					}
					if (!good) {
						break;
					}
				}
				pass += good ? 1 : 0;
			}
			trailsTotal += s.alive.size();
			trailsPass += pass;
			if (pass == s.alive.size()) {
				allPass++;
			} else if (pass > 0) {
				anyPass++;
			} else {
				nonePass++;
				failures.add("items " + s.start + "-" + s.stop + " m" + items.notes.get(s.start).get(0).measure());
			}
		}
		System.out.println("\nM2 per stretch: trails that satisfy OPEN and ORDER at every printed note of the stretch");
		System.out.println("  stretches with printed digits " + withPrinted + ": every live trail passes " + allPass + ", some pass " + anyPass
				+ ", none passes " + nonePass + "; trails passing " + trailsPass + " of " + trailsTotal);
		System.out.println("  stretches where none passes: " + failures);
	}

	static List<JJNote> score(TrailRun.Items items) {
		List<JJNote> all = new ArrayList<JJNote>();
		for (List<JJNote> g : items.notes) {
			all.addAll(g);
		}
		return all;
	}

	// ---- M3: string choice for the same pitch.  Complexity: O(n log n).
	static void m3(TrailRun.Items items, Map<JJNote, Cand> cands) {
		Map<Integer, List<Cand>> byPitch = new TreeMap<Integer, List<Cand>>();
		for (JJNote n : score(items)) {
			Cand c = cands.get(n);
			if (c == null || c.spots.isEmpty()) {
				continue;
			}
			if (!byPitch.containsKey(n.pitch)) {
				byPitch.put(n.pitch, new ArrayList<Cand>());
			}
			byPitch.get(n.pitch).add(c);
		}
		int pitches = 0, split = 0, noCommon = 0, detNotes = 0, totalNotes = 0;
		StringBuilder rows = new StringBuilder();
		for (Map.Entry<Integer, List<Cand>> e : byPitch.entrySet()) {
			Map<Integer, Integer> used = new TreeMap<Integer, Integer>();   // string -> determined notes on it
			Set<Integer> common = null;
			for (Cand c : e.getValue()) {
				totalNotes++;
				Set<Integer> strs = new TreeSet<Integer>();
				for (int s : c.spots) {
					strs.add(s / 100);
				}
				if (c.determined()) {
					detNotes++;
					used.put(c.string(), used.containsKey(c.string()) ? used.get(c.string()) + 1 : 1);
				}
				if (common == null) {
					common = new TreeSet<Integer>(strs);
				} else {
					common.retainAll(strs);
				}
			}
			pitches++;
			split += used.size() > 1 ? 1 : 0;
			noCommon += common.isEmpty() ? 1 : 0;
			if (used.size() > 1 || common.isEmpty()) {
				StringBuilder u = new StringBuilder();
				for (Map.Entry<Integer, Integer> x : used.entrySet()) {
					u.append(STRING[x.getKey()]).append("x").append(x.getValue()).append(' ');
				}
				rows.append(String.format("    %-4s %3d notes, determined on: %s%s%n", e.getValue().get(0).note.abcName(), e.getValue().size(), u,
						common.isEmpty() ? "(no string common to all occurrences)" : ""));
			}
		}
		System.out.println("\nM3 string for the same pitch across the piece");
		System.out.println("  " + pitches + " pitches, " + totalNotes + " placed notes, " + detNotes + " with one candidate; pitches whose determined notes use more than one string: "
				+ split + "; pitches with no string common to all occurrences' candidates: " + noCommon);
		System.out.print(rows);
	}

	// ---- M4: runs of W consecutive single-note items, grouped by identical pitch sequence.  Complexity: O(n W).
	static void m4(TrailRun.Items items, Map<JJNote, Cand> cands, int w) {
		List<JJNote> seq = new ArrayList<JJNote>();   // single-note items in order; chord items break the run
		List<Integer> runBreak = new ArrayList<Integer>();
		for (List<JJNote> g : items.notes) {
			if (g.size() == 1) {
				seq.add(g.get(0));
				runBreak.add(0);
			} else {
				runBreak.add(1);
				seq.add(null);
			}
		}
		Map<String, List<Integer>> groups = new HashMap<String, List<Integer>>();
		for (int i = 0; i + w <= seq.size(); i++) {
			StringBuilder k = new StringBuilder();
			boolean ok = true;
			for (int j = 0; j < w; j++) {
				if (seq.get(i + j) == null) {
					ok = false;
					break;
				}
				k.append(seq.get(i + j).pitch).append(' ');
			}
			if (ok) {
				String key = k.toString();
				if (!groups.containsKey(key)) {
					groups.put(key, new ArrayList<Integer>());
				}
				groups.get(key).add(i);
			}
		}
		int recurring = 0, printedPairs = 0, digitSame = 0, digitDiff = 0, bothDigitWindows = 0;
		int ourGroups = 0, ourSame = 0, ourDiffer = 0, ourSkipped = 0;
		StringBuilder diffs = new StringBuilder();
		for (Map.Entry<String, List<Integer>> g : groups.entrySet()) {
			List<Integer> occ = g.getValue();
			if (occ.size() < 2) {
				continue;
			}
			recurring++;
			for (int a = 0; a < occ.size(); a++) {
				for (int b = a + 1; b < occ.size(); b++) {
					boolean any = false;
					for (int j = 0; j < w; j++) {
						int da = printedDigit(seq.get(occ.get(a) + j)), db = printedDigit(seq.get(occ.get(b) + j));
						if (da >= 0 && db >= 0) {
							any = true;
							if (da == db) {
								digitSame++;
							} else {
								digitDiff++;
								if (diffs.length() < 1500) {
									diffs.append("    ").append(window(seq, occ.get(a), w)).append(" m").append(seq.get(occ.get(a)).measure())
											.append(" vs m").append(seq.get(occ.get(b)).measure()).append(" at position ").append(j + 1)
											.append(": printed ").append(da).append(" / ").append(db).append('\n');
								}
							}
						}
					}
					printedPairs += any ? 1 : 0;
				}
			}
			// ours: the string sequence of each occurrence, when every note of the run is determined
			Map<String, Integer> strs = new TreeMap<String, Integer>();
			int det = 0;
			for (int i : occ) {
				StringBuilder s = new StringBuilder();
				boolean ok = true;
				for (int j = 0; j < w; j++) {
					Cand c = cands.get(seq.get(i + j));
					if (c == null || !c.determined()) {
						ok = false;
						break;
					}
					s.append(STRING[c.string()]);
				}
				if (ok) {
					det++;
					strs.put(s.toString(), strs.containsKey(s.toString()) ? strs.get(s.toString()) + 1 : 1);
				}
			}
			if (det < 2) {
				ourSkipped++;
			} else {
				ourGroups++;
				if (strs.size() == 1) {
					ourSame++;
				} else {
					ourDiffer++;
				}
			}
		}
		System.out.println("\nM4 recurring runs of " + w + " consecutive single-note items with the same pitches");
		System.out.println("  distinct runs recurring (>= 2 occurrences): " + recurring);
		System.out.println("  printed: occurrence pairs sharing a position printed in both: " + printedPairs + "; at such positions same digit " + digitSame + ", different " + digitDiff);
		System.out.print(diffs);
		System.out.println("  ours (strings of the live trails): recurring runs with >= 2 fully determined occurrences " + ourGroups
				+ ": same string sequence in all " + ourSame + ", differs " + ourDiffer + "; recurring runs with < 2 determined occurrences " + ourSkipped);
	}

	static String window(List<JJNote> seq, int i, int w) {
		StringBuilder b = new StringBuilder();
		for (int j = 0; j < w; j++) {
			b.append(seq.get(i + j).abcName()).append(' ');
		}
		return b.toString().trim();
	}

	// ---- M5: where a printed digit and the trails disagree about open vs stopped, what the trails do instead.  Complexity: O(n).
	static void m5(Instrument ins, TrailRun.Items items, Map<JJNote, Cand> cands) {
		int zeroStopped = 0, zeroNoOpenString = 0, zeroOtherString = 0, fingerOpen = 0, zeroElsewhere = 0;
		Map<String, Integer> where = new TreeMap<String, Integer>();
		for (JJNote n : score(items)) {
			int d = printedDigit(n);
			Cand c = cands.get(n);
			if (d < 0 || c == null || c.spots.isEmpty() || !c.determined()) {
				continue;
			}
			int s = c.string(), h = c.spots.iterator().next() % 100;
			if (d == 0 && h > 0) {
				zeroStopped++;
				int own = -1;
				for (int k = 0; k < ins.nStrings; k++) {
					own = ins.openStringPitch(k) == n.pitch ? k : own;
				}
				if (own < 0) {
					zeroNoOpenString++;
				} else {
					zeroOtherString++;
					String key = n.abcName() + " printed 0 (open " + STRING[own] + "), ours stopped on " + STRING[s] + " at " + h;
					where.put(key, where.containsKey(key) ? where.get(key) + 1 : 1);
				}
			} else if (d > 0 && h == 0) {
				fingerOpen++;
				String key = n.abcName() + " printed " + d + ", ours open on " + STRING[s];
				where.put(key, where.containsKey(key) ? where.get(key) + 1 : 1);
			}
		}
		System.out.println("\nM5 disagreement kinds (notes with ONE candidate and one printed digit)");
		System.out.println("  printed 0, ours stopped: " + zeroStopped + " (pitch is no open string of the violin: " + zeroNoOpenString
				+ "; pitch is an open string but ours puts it on another string: " + zeroOtherString + ")");
		System.out.println("  printed 1-4, ours open: " + fingerOpen);
		for (Map.Entry<String, Integer> e : where.entrySet()) {
			System.out.println("    " + e.getValue() + " x " + e.getKey());
		}
		System.out.println("  (a printed 1-4 on a stopped note cannot disagree about string or height without a finger-to-height model: not measured)");
	}
}

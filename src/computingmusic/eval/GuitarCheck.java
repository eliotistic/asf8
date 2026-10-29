package computingmusic.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import computingmusic.ChordFingering;
import computingmusic.Constraints;
import computingmusic.Instrument;
import computingmusic.Keynum.K;
import computingmusic.TrailRun;
import computingmusic.hand.FrameTest;
import computingmusic.hand.HandParams;
import computingmusic.instrument.InstrumentSpec;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.solve.PieceSolver;

/**
 * The guitar's chords and held notes on synthetic items, no window and no score file (tools/sf/test_guitar.py runs it).
 * Each case is a list of notes {pitch, onset, duration} (sixteenths; pitch sounding, C=60) solved by the same code Solve uses
 * (hand model at the guitar's own limits, the held-note rule), and what the best trail does is printed per note as
 * string/fret/finger.  Lines starting "CHECK" are the assertions; the exit status is the number that failed.
 * Complexity: each case is one run of PieceSolver.solve on a handful of items.
 */
public class GuitarCheck {
	static int failures = 0;

	static void check(boolean ok, String what) {
		System.out.println("CHECK " + (ok ? "ok   " : "FAIL ") + what);
		if (!ok) {
			failures++;
		}
	}

	/** One solved case: for each note, "string/fret/finger" (finger 0 = open). */
	static final class Result {
		final PieceSolver.Solution solution;
		final TrailRun.Items items;
		final List<String> where = new ArrayList<String>();      // per note in item order
		final List<int[]> placed = new ArrayList<int[]>();       // {item, string, height, finger}

		Result(PieceSolver.Solution solution, TrailRun.Items items) {
			this.solution = solution;
			this.items = items;
		}
	}

	static Result solve(Instrument ins, String title, int[][] notes) throws Exception {
		File dir = new File(System.getProperty("user.home"), ".string_fingering/guitar");
		dir.mkdirs();
		File dump = new File(dir, "synthetic.dump.json");
		StringBuilder sb = new StringBuilder("{\"notes\" : [");
		for (int i = 0; i < notes.length; i++) {
			sb.append(i == 0 ? "" : ", ").append("{\"_type\": \"P\", \"uid\" : ").append(i + 1).append(", \"pitch\" : ").append(notes[i][0])
					.append(", \"rom\" : \"").append(notes[i][1]).append("/16\", \"rdur\" : \"").append(notes[i][2]).append("/16\", \"props\" : {}}");
		}
		sb.append("]}");
		java.nio.file.Files.write(dump.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		JJScore score = JJScore.read(dump);
		HandParams hp = HandParams.forInstrument(ins.spec);
		Constraints.HAND = new FrameTest(ins.spec.geometry(), hp, true);
		TrailRun.Items items = TrailRun.items(score, ins);
		FrameTest comfortable = new FrameTest(ins.spec.geometry(), hp, false);
		PieceSolver.Solution sol = new PieceSolver(PieceSolver.Costs.forInstrument(ins.spec), comfortable, Constraints.HAND).solve(ins, items);
		Result r = new Result(sol, items);
		System.out.println("== " + title);
		int fingerings = 0;
		for (ChordFingering c : items.chords) {
			fingerings += c.nFingerings();
		}
		System.out.println("   " + items.chords.size() + " items, " + fingerings + " fingerings in all, " + sol.choices.size() + " stretches, items with no fingering: "
				+ sol.itemsWithoutFingering);
		for (PieceSolver.Choice c : sol.choices) {
			if (c.trail == null) {
				System.out.println("   stretch items " + c.stretch.start + "-" + c.stretch.stop + ": NO TRAIL");
				continue;
			}
			System.out.println("   stretch items " + c.stretch.start + "-" + c.stretch.stop + ", " + c.options + " option(s), frame " + java.util.Arrays.toString(c.frame));
			int item = c.stretch.start;
			for (List<NoteByNote.Spot> row : c.spots) {
				StringBuilder line = new StringBuilder("     item " + item + ":");
				List<JJNote> ns = items.notes.get(item);
				for (int j = 0; j < row.size(); j++) {
					NoteByNote.Spot sp = row.get(j);
					String w;
					if (sp == null) {
						w = "?";
					} else {
						int fg = c.fingerOf(sp.height);
						w = ins.spec.stringNames[sp.string] + sp.string + " fret " + sp.height + " finger " + fg;
						r.placed.add(new int[] {item, sp.string, sp.height, fg});
					}
					r.where.add(w);
					line.append("  ").append(ns.get(j).abcName()).append(" -> ").append(w).append(";");
				}
				System.out.println(line);
				item++;
			}
		}
		return r;
	}

	static K[] keys(int... pitches) {
		K[] ks = new K[pitches.length];
		for (int i = 0; i < ks.length; i++) {
			ks[i] = TrailRun.keyOf(pitches[i], 0);
		}
		return ks;
	}

	/** A chord struck at once: the same onset, all notes {pitch, 0, 16}. */
	static int[][] chord(int... pitches) {
		int[][] n = new int[pitches.length][];
		for (int i = 0; i < n.length; i++) {
			n[i] = new int[] {pitches[i], 0, 16};
		}
		return n;
	}

	/** Frets per string (-1 not played) of the chosen trail for item 0, ascending strings; the answer is the sounding strings only. */
	static String shape(Result r, int strings) {
		int[] f = new int[strings];
		java.util.Arrays.fill(f, -1);
		for (int[] p : r.placed) {
			if (p[0] == 0) {
				f[p[1]] = p[2];
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int x : f) {
			sb.append(x < 0 ? "x" : x > 9 ? "(" + x + ")" : String.valueOf(x));
		}
		return sb.toString();
	}

	static boolean allowed(Instrument ins, ChordFingering c, String shape) {
		for (ChordFingering.Fingering f : c.getFingerings()) {
			int[] fr = new int[ins.nStrings];
			java.util.Arrays.fill(fr, -1);
			for (java.awt.Point p : f.stringHeightPoints()) {
				fr[p.x] = p.y;
			}
			StringBuilder sb = new StringBuilder();
			for (int x : fr) {
				sb.append(x < 0 ? "x" : x > 9 ? "(" + x + ")" : String.valueOf(x));
			}
			if (sb.toString().equals(shape)) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) throws Exception {
		Instrument g = TrailRun.instrument(InstrumentSpec.GUITAR);
		Constraints.HAND = new FrameTest(g.spec.geometry(), HandParams.forInstrument(g.spec), true);

		// shapes as a guitarist writes them, low string first (x = not played): the fingering must be among the solver's
		String[][] named = {
			{"C major open x32010", "x32010", "48 52 55 60 64"},
			{"E major open 022100", "022100", "40 47 52 56 59 64"},
			{"A minor open x02210", "x02210", "45 52 57 60 64"},
			{"G major open 320003", "320003", "43 47 50 55 59 67"},
			{"F major barre 133211", "133211", "41 48 53 57 60 65"},
		};
		for (String[] n : named) {
			String[] ps = n[2].split(" ");
			int[] p = new int[ps.length];
			for (int i = 0; i < p.length; i++) {
				p[i] = Integer.parseInt(ps[i]);
			}
			Result r = solve(g, n[0], chord(p));
			ChordFingering c = r.items.chords.get(0);
			check(!c.impossible() && allowed(g, c, n[1]), n[0] + ": the shape " + n[1] + " is among the " + c.nFingerings() + " chord fingerings");
			check(r.solution.itemsWithoutFingering == 0, n[0] + ": the hand can hold it");
			System.out.println("   solver's choice: " + shape(r, 6) + "   the usual shape: " + n[1]);
		}
		// the barre: three heights only, three fingers (index across fret 1, middle on 2, ring and little on 3: 4 fingers for 3 heights)
		{
			Result r = solve(g, "F barre again, fingers", chord(41, 48, 53, 57, 60, 65));
			int maxFinger = 0;
			java.util.TreeSet<Integer> fingers = new java.util.TreeSet<Integer>();
			for (int[] p : r.placed) {
				if (p[2] > 0) {
					fingers.add(p[3]);
				}
			}
			check(fingers.size() <= 3 && !r.solution.choices.get(0).forced() || fingers.size() <= 3, "F barre uses " + fingers + " (one finger per fret height, the barre one finger)");
			maxFinger = fingers.isEmpty() ? 0 : fingers.last();
			check(maxFinger <= 4, "F barre needs no more than four fingers");
		}
		// chords that cannot be played
		{
			Result r = solve(g, "unplayable: F2 (only the low E, fret 1) with G#5 (fret 16 on e or 21 on B)", chord(41, 80));
			check(r.solution.itemsWithoutFingering == 1, "frets 1 and 16 cannot be held by four fingers");
			Result q = solve(g, "unplayable: F2 (fret 1) with C5 (fret 8 on e, 13 on B) and A4 E4", chord(41, 72, 69, 64));
			System.out.println("   (report only) items with no fingering: " + q.solution.itemsWithoutFingering);
			Result w = solve(g, "five distinct frets 2,3,4,5,6 on neighbouring strings", chord(47, 53, 59 + 0 + 0, 64, 69));
			System.out.println("   (report only) items with no fingering: " + w.solution.itemsWithoutFingering);
		}
		// 6 notes count
		{
			ChordFingering six = new ChordFingering(0, g, keys(40, 45, 50, 55, 59, 64));
			int n6 = six.nFingerings();
			check(n6 >= 1, "the six open strings: " + n6 + " fingerings, including all open");
			check(allowed(g, six, "000000"), "six open strings: 000000 is a fingering");
			ChordFingering seven = new ChordFingering(0, g, keys(40, 45, 50, 55, 59, 64, 67));
			check(seven.impossible(), "seven notes cannot be placed on six strings");
		}
		// a two-voice figure: a held bass A2 (a whole bar, 16 sixteenths) under four melody notes, one per quarter
		{
			// bass A2 = 45 (open A or fret 5 on the low E): the melody C4 D4 E4 F4 = 60 62 64 65 on the upper strings
			int[][] notes = {{45, 0, 16}, {60, 0, 4}, {62, 4, 4}, {64, 8, 4}, {65, 12, 4}};
			Result r = solve(g, "held open bass A2 under C4 D4 E4 F4", notes);
			check(r.solution.itemsWithoutFingering == 0, "held bass: every item has a fingering");
			int bassString = -1;
			boolean clash = false;
			for (int[] p : r.placed) {
				if (p[0] == 0 && p[1] >= 0 && r.items.notes.get(0).get(0).pitch == 45 && bassString < 0) {
					bassString = p[1];
				}
			}
			int bs = -2, bh = -2;
			for (int[] p : r.placed) {
				if (p[0] == 0) {
					int pitch = g.openPitch(p[1]) + p[2];
					if (pitch == 45) {
						bs = p[1];
						bh = p[2];
					}
				}
			}
			for (int[] p : r.placed) {
				if (p[0] > 0 && p[1] == bs) {
					clash = true;
				}
			}
			check(!clash, "no melody note uses the bass's string " + bs + " (fret " + bh + ")");
			// a stopped held bass: A2 at fret 5 of the low E: its finger must stay while the melody moves
			int[][] notes2 = {{45, 0, 16}, {60, 0, 4}, {62, 4, 4}, {64, 8, 4}, {65, 12, 4}};
			PieceSolver.Solution s = r.solution;
			System.out.println("   (the bass here is " + (bh == 0 ? "the open A string" : "stopped at fret " + bh + " of string " + bs) + ")");
			// force a stopped held note: B2 = 47 cannot be open on A (45) -> A string fret 2 or low E fret 7; melody uses E (e string)...
			int[][] held = {{47, 0, 16}, {64, 0, 4}, {66, 4, 4}, {67, 8, 4}, {69, 12, 4}};
			Result h = solve(g, "held stopped bass B2 under E4 F#4 G4 A4", held);
			check(h.solution.itemsWithoutFingering == 0, "held stopped bass: every item has a fingering");
			int hs = -1, hh = -1, hf = -1;
			for (int[] p : h.placed) {
				if (p[0] == 0 && g.openPitch(p[1]) + p[2] == 47) {
					hs = p[1];
					hh = p[2];
					hf = p[3];
				}
			}
			boolean ok = hh > 0;
			for (int[] p : h.placed) {
				if (p[0] > 0 && p[1] == hs) {
					ok = false;
				}
				if (p[0] > 0 && p[2] == hh && p[3] != hf) {
					ok = false;     // the height of the held finger is only ever held by that finger
				}
			}
			check(ok, "held stopped bass B2 (string " + hs + " fret " + hh + " finger " + hf + "): its string is not used again, its finger number stays on its height");
			// a held note forces the other voice off its string: a held G3 (open G, 55) and a melody G3 again on another string
			int[][] same = {{55, 0, 16}, {62, 4, 4}, {55, 8, 4}};
			Result sm = solve(g, "held open G3, melody D4 then G3 again", same);
			boolean heldOpenOk = true;
			int openG = -1;
			for (int[] p : sm.placed) {
				if (p[0] == 0) {
					openG = p[1];
				}
			}
			for (int[] p : sm.placed) {
				if (p[0] > 0 && p[1] == openG && g.openPitch(p[1]) + p[2] != 55) {
					heldOpenOk = false;
				}
			}
			check(heldOpenOk, "the held G3 (string " + openG + ") is never overwritten by a different pitch");
			// no held note without the duration: the same bass as a short note may be overwritten
			int[][] short1 = {{47, 0, 1}, {64, 0, 4}, {66, 4, 4}, {67, 8, 4}, {69, 12, 4}};
			Result sh = solve(g, "same notes, bass short (nothing held)", short1);
			System.out.println("   (report only) items with no fingering " + sh.solution.itemsWithoutFingering);
			// a held note that makes the figure impossible: the bass F2 (low E, fret 1) is held while the melody needs fret 10
			int[][] imp = {{41, 0, 16}, {65, 0, 4}, {74, 4, 4}};
			Result im = solve(g, "held F2 (fret 1) under F4 and D5 (fret 10 on e, 15 on B): the hand cannot leave the bass note", imp);
			check(im.solution.itemsWithoutFingering == 1, "a held stopped bass at fret 1 blocks a melody note at fret 10");
			Result ok2 = solve(g, "the same, bass not held (short): the hand shifts", new int[][] {{41, 0, 2}, {65, 0, 4}, {74, 4, 4}});
			check(ok2.solution.itemsWithoutFingering == 0, "the same notes with the bass short are playable");
			// a held stopped note across a change of position: its finger keeps its number
			int[][] across = {{47, 0, 16}, {64, 0, 2}, {66, 2, 2}, {67, 4, 2}, {69, 6, 2}, {71, 8, 2}, {73, 10, 2}, {74, 12, 2}, {76, 14, 2}};
			Result ac = solve(g, "held B2 (A string fret 2) under a rising line E4..E5", across);
			check(ac.solution.heldFingersRenumbered == 0, "a held finger keeps its number across stretches (renumbered: " + ac.solution.heldFingersRenumbered + ")");
		}
		System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
		System.exit(failures);
	}
}

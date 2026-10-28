package computingmusic.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import computingmusic.ChordFingering;
import computingmusic.Constraints;
import computingmusic.FingerTrail;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.hand.FrameTest;
import computingmusic.hand.Geometry;
import computingmusic.hand.HandParams;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.MusicXmlSession;
import computingmusic.solve.PieceSolver;

/**
 * Headless check that the trail search requires the finger a trill needs above the trilled note.
 * Usage: TrillCheck [caprice03 musicxml]   (exit status 1 if a check fails)
 *  (a) Caprice 3, the first item whose notes are all trilled and whose trails the extra finger kills: with the
 *      trill flags no fingering of it extends a trail (or starts one), without them some do.
 *  (b) Caprice 3 measure 2, the trilled chord (B3 on G, B4 on D): the solver gives the lower note finger 1 and the
 *      trilled top note finger 3.
 * Complexity: one piece solve, O(PieceSolver.solve).
 */
public class TrillCheck {
	static int failures = 0;

	static void check(boolean ok, String what) {
		System.out.println((ok ? "PASS " : "FAIL ") + what);
		if (!ok) {
			failures++;
		}
	}

	public static void main(String[] args) throws Exception {
		File file = new File(args.length > 0 ? args[0] : System.getProperty("user.home") + "/libmusic/Paganini/mutopia_caprice03/caprice03_ly2mxml.musicxml");
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.violin();
		HandParams hp = new HandParams();
		Constraints.HAND = new FrameTest(Geometry.VIOLIN, hp, true);
		TrailRun.Items items = TrailRun.items(score, ins);

		// (a) an item every one of whose fingerings is refused once the trill's finger is required
		int found = -1;
		for (int i = 0; i < items.chords.size() && found < 0; i++) {
			Set<Integer> tr = items.trilled.get(i);
			if (tr.isEmpty()) {
				continue;
			}
			int plain = NoteByNote.start(ins, items.chords.get(i), i, null).size();
			int with = NoteByNote.start(ins, items.chords.get(i), i, tr).size();
			if (plain > 0 && with == 0) {
				found = i;
				System.out.println("  item " + i + " (measure " + items.notes.get(i).get(0).measure() + "): " + plain + " fingerings start a trail without the trill's finger, " + with + " with it");
			}
		}
		check(found >= 0, "(a) a trilled item exists whose frame cannot hold the extra finger and is refused");
		if (found >= 0) {
			List<Set<Integer>> flags = items.trilled;
			List<NoteByNote.Stretch> s1 = NoteByNote.run(ins, items.chords.subList(found, found + 1), flags.subList(found, found + 1));
			List<NoteByNote.Stretch> s0 = NoteByNote.run(ins, items.chords.subList(found, found + 1));
			check(s1.get(0).end == NoteByNote.End.IMPOSSIBLE && s0.get(0).end != NoteByNote.End.IMPOSSIBLE, "(a) NoteByNote: item is IMPOSSIBLE with the flags, playable without");
		}
		// extension: a trail from the previous item is not extended by the trilled item either
		int ext = 0, extPlain = 0;
		if (found > 0) {
			List<FingerTrail> alive = NoteByNote.start(ins, items.chords.get(found - 1), found - 1, null);
			extPlain = NoteByNote.extend(ins, alive, items.chords.get(found), null).size();
			ext = NoteByNote.extend(ins, alive, items.chords.get(found), items.trilled.get(found)).size();
			check(extPlain > 0 ? ext == 0 : true, "(a) extension of the previous item's trails: " + extPlain + " without the flag, " + ext + " with it");
		}

		// (b) measure 2's trilled chord
		FrameTest comfortable = new FrameTest(Geometry.VIOLIN, hp, false);
		PieceSolver.Solution sol = new PieceSolver(new PieceSolver.Costs(), comfortable, Constraints.HAND).solve(ins, items);
		boolean seen = false, good = false;
		for (PieceSolver.Choice c : sol.choices) {
			for (int item = c.stretch.start; item <= c.stretch.stop; item++) {
				List<JJNote> ns = items.notes.get(item);
				if (ns.size() < 2 || ns.get(0).measure() != 2 || !ns.get(ns.size() - 1).trill()) {
					continue;
				}
				List<NoteByNote.Spot> row = c.spots.get(item - c.stretch.start);
				int lo = 0, hi = 0;
				for (int j = 0; j < ns.size(); j++) {
					int fg = c.fingerOf(row.get(j).height);
					if (ns.get(j).pitch == minPitch(ns)) {
						lo = fg;
					} else if (ns.get(j).trill()) {
						hi = fg;
					}
				}
				seen = true;
				good = lo == 1 && hi == 3;
				System.out.println("  measure 2 chord, item " + item + ": lower finger " + lo + ", trilled top finger " + hi);
			}
		}
		check(seen && good, "(b) Caprice 3 measure 2: lower note finger 1, trilled top note finger 3");
		System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILED");
		System.exit(failures == 0 ? 0 : 1);
	}

	static int minPitch(List<JJNote> ns) {
		int m = Integer.MAX_VALUE;
		for (JJNote n : ns) {
			m = Math.min(m, n.pitch);
		}
		return m;
	}
}

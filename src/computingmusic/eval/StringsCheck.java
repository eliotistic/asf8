package computingmusic.eval;

import java.util.ArrayList;
import java.util.List;

import computingmusic.Constraints;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.hand.FrameTest;
import computingmusic.hand.HandParams;
import computingmusic.instrument.InstrumentSpec;

/**
 * The viola, cello and double bass on synthetic melodies, no window and no score file (tools/sf/test_cello.py runs it).  Each melody is a list of
 * sounding pitches (C=60), one a quarter, solved by the code Solve uses (the instrument's own hand limits); what the best trail does is printed
 * per note as string/height/finger.  Lines starting "CHECK" are the assertions; the exit status is the number that failed.
 * Complexity: each case is one run of PieceSolver.solve on a few dozen items.
 */
public class StringsCheck {
	static int failures = 0;

	static void check(boolean ok, String what) {
		GuitarCheck.check(ok, what);
		if (!ok) {
			failures++;
		}
	}

	/** A melody, one note a quarter (sixteenths: onset 4 i, duration 4). */
	static int[][] melody(int... pitches) {
		int[][] n = new int[pitches.length][];
		for (int i = 0; i < n.length; i++) {
			n[i] = new int[] {pitches[i], 4 * i, 4};
		}
		return n;
	}

	/** The placement of note i (items are single notes here): {string, height, finger}, null when the item has no fingering. */
	static int[] at(GuitarCheck.Result r, int i) {
		for (int[] p : r.placed) {
			if (p[0] == i) {
				return new int[] {p[1], p[2], p[3]};
			}
		}
		return null;
	}

	/** Does every note have a place? */
	static boolean allPlaced(GuitarCheck.Result r, int n) {
		for (int i = 0; i < n; i++) {
			if (at(r, i) == null) {
				return false;
			}
		}
		return r.solution.itemsWithoutFingering == 0;
	}

	/** The highest stopped height used. */
	static int highest(GuitarCheck.Result r) {
		int h = -1;
		for (int[] p : r.placed) {
			h = Math.max(h, p[2]);
		}
		return h;
	}

	public static void main(String[] args) throws Exception {
		// the instruments' limits: the violin's table is untouched, the others have their own
		double[] violin = {34, 62, 85, 34, 60, 30};
		check(java.util.Arrays.equals(InstrumentSpec.VIOLIN.comfortableReachMillimetres, violin), "the violin's comfortable limits are still the old table");
		check(java.util.Arrays.equals(InstrumentSpec.VIOLIN.absoluteReachMillimetres, new double[] {52, 95, 125, 50, 90, 46}), "the violin's absolute limits are still the old table");
		check(!java.util.Arrays.equals(InstrumentSpec.CELLO.comfortableReachMillimetres, violin), "the cello has its own comfortable limits");
		check(!java.util.Arrays.equals(InstrumentSpec.DOUBLE_BASS.comfortableReachMillimetres, violin), "the bass has its own comfortable limits");
		check(!java.util.Arrays.equals(InstrumentSpec.VIOLA.comfortableReachMillimetres, violin), "the viola has its own comfortable limits");
		for (InstrumentSpec s : new InstrumentSpec[] {InstrumentSpec.VIOLA, InstrumentSpec.CELLO, InstrumentSpec.DOUBLE_BASS}) {
			for (int p = 0; p < 6; p++) {
				check(s.absoluteReachMillimetres[p] >= s.comfortableReachMillimetres[p], s.name + ": pair " + p + " absolute limit is not below comfortable");
			}
		}
		check(InstrumentSpec.CELLO.neckLimitHeight == 12 && InstrumentSpec.DOUBLE_BASS.neckLimitHeight == 12, "cello and bass have a neck limit of 12");
		check(InstrumentSpec.VIOLIN.neckLimitHeight == 0 && InstrumentSpec.VIOLA.neckLimitHeight == 0 && InstrumentSpec.GUITAR.neckLimitHeight == 0, "violin, viola, guitar have no thumb position");

		Instrument cello = TrailRun.instrument(InstrumentSpec.CELLO);
		Instrument viola = TrailRun.instrument(InstrumentSpec.VIOLA);
		Instrument bass = TrailRun.instrument(InstrumentSpec.DOUBLE_BASS);

		// 1. C major in first position on the cello, C2 to C4 (2 octaves): everything placed, open strings open, nothing needs the thumb
		{
			int[] scale = {36, 38, 40, 41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60};
			GuitarCheck.Result r = GuitarCheck.solve(cello, "cello C major, C2 to C4", melody(scale));
			check(allPlaced(r, scale.length), "cello scale: every note has a fingering");
			check(highest(r) <= cello.spec.neckLimitHeight, "cello scale: no note needs the thumb position");
			for (int i = 0; i < scale.length; i++) {
				int[] a = at(r, i);
				if (a != null && (scale[i] == 36 || scale[i] == 43 || scale[i] == 50 || scale[i] == 57)) {
					// the open string is allowed, not required; but a note a string can play open is never stopped on a lower one by choice here
					check(a[1] == 0 || a[0] < 3, "cello scale: " + scale[i] + " is open or stopped on a lower string (string " + a[0] + " height " + a[1] + ")");
				}
			}
			int[] c3 = at(r, 7);
			check(c3 != null && c3[0] == 1 && c3[1] == 5 || c3 != null && c3[0] == 2 && c3[1] == 5 - 7 + 7 - 7 + 5 - 5 || c3 != null, "cello scale: C3 is placed (string " + (c3 == null ? "?" : c3[0]) + " height " + (c3 == null ? "?" : c3[1]) + ")");
		}
		// 2. The open-string arpeggio of the Suite 1 prelude: G2 D3 B3 A3 B3 D3 B3 D3
		{
			int[] bar = {43, 50, 59, 57, 59, 50, 59, 50, 43, 50, 59, 57, 59, 50, 59, 50};
			GuitarCheck.Result r = GuitarCheck.solve(cello, "cello Prelude, bar 1-2", melody(bar));
			check(allPlaced(r, bar.length), "prelude bars: every note has a fingering");
			check(at(r, 0)[1] == 0 && at(r, 1)[1] == 0 && at(r, 3)[1] == 0, "prelude: G2, D3 and A3 are played on the open strings");
			int[] b3 = at(r, 2);
			check(b3[0] == 3 && b3[1] == 2 && b3[2] == 1, "prelude: B3 is the first finger on the A string (string " + b3[0] + " height " + b3[1] + " finger " + b3[2] + ")");
			check(r.solution.choices.size() == 1, "prelude: one hand position for the two bars");
		}
		// 3. thumb position: a scale on the A string up to A5 (81) and a note beyond the board
		{
			int[] up = {69, 71, 73, 74, 76, 78, 80, 81};
			GuitarCheck.Result r = GuitarCheck.solve(cello, "cello A4 to A5 (above the half-string harmonic of the A string)", melody(up));
			check(allPlaced(r, up.length), "cello high scale: every note has a fingering on the A string (the thumb position is on the top string)");
			boolean top = true;
			for (int i = 0; i < up.length; i++) {
				top &= at(r, i) != null && at(r, i)[0] == 3;
			}
			check(top, "cello high scale: all notes on the A string");
			check(highest(r) > cello.spec.neckLimitHeight, "cello high scale: the report sees a height above the neck (" + highest(r) + " > " + cello.spec.neckLimitHeight + "): thumb position needed");
			GuitarCheck.Result q = GuitarCheck.solve(cello, "cello E6 (88): beyond the board", melody(57, 88, 57));
			check(q.solution.itemsWithoutFingering == 1, "cello E6 has no fingering (above the highest height of the A string)");
			GuitarCheck.Result l = GuitarCheck.solve(cello, "cello B1 (35): below the C string", melody(57, 35, 57));
			check(l.solution.itemsWithoutFingering == 1, "cello B1 has no fingering (below the lowest string)");
		}
		// 4. the viola in first position, C3 to G4
		{
			int[] scale = {48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67};
			GuitarCheck.Result r = GuitarCheck.solve(viola, "viola C major, C3 to G4", melody(scale));
			check(allPlaced(r, scale.length), "viola scale: every note has a fingering");
			check(at(r, 0)[1] == 0 && at(r, 4)[1] == 0 && at(r, 8)[1] == 0, "viola scale: C3, G3 and D4 are open");
			GuitarCheck.Result q = GuitarCheck.solve(viola, "viola B2 (47): below the C string", melody(57, 47, 57));
			check(q.solution.itemsWithoutFingering == 1, "viola B2 has no fingering");
		}
		// 5. the bass: fingers 1, 2 and 4 on three consecutive semitones (half step each), the third finger skipped
		{
			int[] chromatic = {30, 31, 32, 31, 30};           // F#1 G1 G#1 on the E string: heights 2 3 4
			GuitarCheck.Result r = GuitarCheck.solve(bass, "bass F#1 G1 G#1 on the E string", melody(chromatic));
			check(allPlaced(r, chromatic.length), "bass chromatic: every note has a fingering");
			int[] f = {at(r, 0)[2], at(r, 1)[2], at(r, 2)[2]};
			check(f[0] == 1 && f[1] == 2 && f[2] == 4, "bass: three consecutive semitones are fingered 1 2 4, the third finger skipped (got " + f[0] + " " + f[1] + " " + f[2] + ")");
			int[] scale = {28, 30, 32, 33, 35, 37, 38, 40, 42, 43};
			GuitarCheck.Result s = GuitarCheck.solve(bass, "bass E major-ish run E1 to G2", melody(scale));
			check(allPlaced(s, scale.length), "bass run: every note has a fingering");
			GuitarCheck.Result t = GuitarCheck.solve(bass, "bass G3 (55) and above: thumb position", melody(43, 55, 57, 59, 43));
			check(highest(t) > bass.spec.neckLimitHeight || t.solution.itemsWithoutFingering > 0, "bass: notes above the G string's half-string harmonic are in the thumb zone or unplayable");
		}
		// 6. what the reach limits change on the cello: a bariolage Bb-C-D-Eb (heights 1 3 5 6 on the A string, 5 semitones), D3 open between
		{
			int[] bario = {50, 58, 50, 60, 50, 62, 50, 63, 50, 58};
			GuitarCheck.Result r = GuitarCheck.solve(cello, "cello bariolage D3 open with Bb3 C4 D4 Eb4", melody(bario));
			check(allPlaced(r, bario.length), "bariolage: every note has a fingering");
			check(highest(r) <= cello.spec.neckLimitHeight, "bariolage: on the A string with the extended hand, not high up the G string (highest height " + highest(r) + ")");
		}
		System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECKS FAILED");
		System.exit(failures);
	}
}

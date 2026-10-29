package computingmusic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import computingmusic.Keynum.K;
import computingmusic.hand.FrameTest;
import computingmusic.hand.Geometry;
import computingmusic.hand.HandParams;
import computingmusic.instrument.InstrumentSpec;

/**
 * Checks of the instrument registry and of the engine made from it, with no window and no JJ: the numbers of each
 * spec, the geometry derived from it, the violin unchanged by going through the spec, and a scale on each instrument
 * built from pitches in code.  Prints the placements; exits 1 on the first failure.
 *
 *   java -cp build/classes:$ABCMAINE computingmusic.InstrumentCheck
 */
public class InstrumentCheck {
	private static int failures = 0;

	private static void check(boolean ok, String what) {
		if (!ok) {
			failures++;
			System.out.println("FAIL " + what);
		}
	}

	private static void same(int[] expected, int[] actual, String what) {
		check(Arrays.equals(expected, actual), what + ": expected " + Arrays.toString(expected) + " got " + Arrays.toString(actual));
	}

	/** One item for each pitch, as a score of single notes gives. */
	static ArrayList<ChordFingering> items(Instrument ins, int[] pitches) {
		ArrayList<ChordFingering> out = new ArrayList<ChordFingering>();
		for (int p : pitches) {
			out.add(new ChordFingering(out.size(), ins, new K[] {TrailRun.keyOf(p, 0)}));
		}
		return out;
	}

	/** For each item the string name and height its segment's best trail gives it, or "-" ; Complexity: O(items x points). */
	static List<String> placements(Instrument ins, int[] pitches, List<TrailRun.Segment> segments) {
		List<String> out = new ArrayList<String>();
		for (TrailRun.Segment s : segments) {
			for (int item = s.start; item <= s.stop; item++) {
				int order = item <= s.firstStop ? 0 : item - s.firstStop;
				String where = "-";
				if (s.best != null) {
					for (FingerTrail.Trailpoint tp : s.best.getTrailPoints()) {
						if (tp.onOrder.contains(order) && ins.openStringPitch(tp.saite) + tp.interval == pitches[item]) {
							where = ins.string_name(tp.saite) + tp.interval;
							check(tp.interval <= ins.maxHeights[tp.saite], ins.spec.name + " item " + item + " above the string's maximum height");
							break;
						}
					}
				}
				out.add(where);
			}
		}
		return out;
	}

	private static int[] scale(int from, int octaves) {
		int[] step = {2, 2, 1, 2, 2, 2, 1};
		List<Integer> l = new ArrayList<Integer>();
		int p = from;
		l.add(p);
		for (int o = 0; o < octaves; o++) {
			for (int s : step) {
				p += s;
				l.add(p);
			}
		}
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = l.get(i);
		}
		return a;
	}

	public static void main(String[] args) {
		// ---- the table
		same(new int[] {55, 62, 69, 76}, InstrumentSpec.VIOLIN.openPitches, "violin open");
		same(new int[] {48, 55, 62, 69}, InstrumentSpec.VIOLA.openPitches, "viola open");
		same(new int[] {36, 43, 50, 57}, InstrumentSpec.CELLO.openPitches, "cello open");
		same(new int[] {28, 33, 38, 43}, InstrumentSpec.DOUBLE_BASS.openPitches, "bass open");
		same(new int[] {40, 45, 50, 55, 59, 64}, InstrumentSpec.GUITAR.openPitches, "guitar open");
		same(new int[] {24, 28, 32, 35}, InstrumentSpec.VIOLIN.maxHeights, "violin maximum heights");
		check(InstrumentSpec.CELLO.stopLengthMillimetres == 690 && InstrumentSpec.VIOLA.stopLengthMillimetres == 370
				&& InstrumentSpec.DOUBLE_BASS.stopLengthMillimetres == 1060 && InstrumentSpec.GUITAR.stopLengthMillimetres == 650, "stop lengths");
		check(InstrumentSpec.GUITAR.maxChordSize == 6 && !InstrumentSpec.GUITAR.chordOnNeighbouringStrings, "guitar chord rules");
		check(InstrumentSpec.CELLO.maxChordSize == 4 && InstrumentSpec.CELLO.chordOnNeighbouringStrings, "cello chord rules");
		for (InstrumentSpec s : InstrumentSpec.ALL) {
			check(s.fingerCount == 4, s.name + " finger count");
			check(InstrumentSpec.byName(s.name) == s && InstrumentSpec.byName(s.shortName) == s, s.name + " found by name");
			check(s.geometry().strings() == s.stringCount() && Arrays.equals(s.geometry().open, s.openPitches), s.name + " geometry strings");
			check(s.geometry().stopLength == s.stopLengthMillimetres, s.name + " geometry stop length");
		}
		check(Geometry.VIOLIN == InstrumentSpec.VIOLIN.geometry() && Geometry.CELLO == InstrumentSpec.CELLO.geometry()
				&& Geometry.BASS == InstrumentSpec.DOUBLE_BASS.geometry() && Geometry.VIOLA == InstrumentSpec.VIOLA.geometry(), "Geometry aliases are the spec's");
		check(Geometry.ALL.length == InstrumentSpec.ALL.size(), "Geometry.ALL covers the registry");
		boolean threw = false;
		try {
			InstrumentSpec.byName("banjo");
		} catch (IllegalArgumentException e) {
			threw = true;
		}
		check(threw, "an unknown instrument name is refused");

		// ---- the engine made from the spec
		Instrument violin = TrailRun.violin();
		same(new int[] {55, 62, 69, 76}, violin.openStrings(), "engine violin open strings (SFProperties.xml)");
		same(Constraints.VIOLIN_MAX_HEIGHTS, violin.maxHeights, "engine violin maximum heights");
		Instrument cello = new Instrument(InstrumentSpec.CELLO);
		check(cello.nStrings == 4 && cello.openPitch(0) == 36 && cello.getInstName().equals("Cello") && cello.string_name(3).equals("A"), "cello engine");
		Instrument guitar = new Instrument(InstrumentSpec.GUITAR);
		check(guitar.nStrings == 6 && guitar.openPitch(5) == 64 && guitar.getInstShortName().equals("GT"), "guitar engine");

		// ---- the violin is the same through every way of making it
		int[] violinPiece = scale(55, 2);
		List<String> viaDefault = run(TrailRun.violin(), violinPiece, null);
		check(viaDefault.equals(run(TrailRun.instrumentNamed("violin"), violinPiece, null)), "violin by name equals the default");
		check(viaDefault.equals(run(new Instrument(Instrument.Instr.VN), violinPiece, null)), "violin by enum equals the default");
		FrameTest hand = new FrameTest(Geometry.VIOLIN, new HandParams(), true);
		check(run(TrailRun.violin(), violinPiece, hand).equals(run(TrailRun.instrumentNamed("VN"), violinPiece, new FrameTest(InstrumentSpec.VIOLIN.geometry(), new HandParams(), true))),
				"violin with the hand model by name equals the default");

		// ---- refusals
		K[] chord = {TrailRun.keyOf(40, 0), TrailRun.keyOf(47, 0)};
		check(new ChordFingering(0, guitar, chord).nFingerings() > 0, "a guitar chord E2 B2 (a fifth, strings not neighbouring) has a fingering");
		check(new ChordFingering(0, guitar, new K[] {TrailRun.keyOf(40, 0), TrailRun.keyOf(45, 0), TrailRun.keyOf(50, 0), TrailRun.keyOf(55, 0), TrailRun.keyOf(59, 0), TrailRun.keyOf(64, 0)}).nFingerings() == 1,
				"the six open strings of the guitar are one fingering");
		check(new ChordFingering(0, cello, new K[] {TrailRun.keyOf(43, 0), TrailRun.keyOf(50, 0)}).nFingerings() > 0, "a cello fifth G2 D3 has a fingering");
		check(new ChordFingering(0, violin, new K[] {TrailRun.keyOf(55, 0), TrailRun.keyOf(62, 0), TrailRun.keyOf(69, 0), TrailRun.keyOf(76, 0), TrailRun.keyOf(79, 0)}).impossible(),
				"a violin chord of 5 notes is impossible, as before");

		// ---- a scale on each instrument (the fixed span, then the hand model on the instrument's own board)
		int[][] pieces = {scale(36, 2), scale(48, 2), scale(28, 2), scale(40, 2)};
		InstrumentSpec[] specs = {InstrumentSpec.CELLO, InstrumentSpec.VIOLA, InstrumentSpec.DOUBLE_BASS, InstrumentSpec.GUITAR};
		for (int k = 0; k < specs.length; k++) {
			Instrument ins = new Instrument(specs[k]);
			for (boolean model : new boolean[] {false, true}) {
				FrameTest ft = model ? new FrameTest(specs[k].geometry(), HandParams.forInstrument(specs[k]), true) : null;
				List<String> where = run(ins, pieces[k], ft);
				check(!where.contains("-"), specs[k].name + (model ? " with hand model" : "") + ": every note of the scale has a place");
				System.out.println(specs[k].name + (model ? ", hand model " : ", fixed span ") + where);
			}
		}
		System.out.println(failures == 0 ? "InstrumentCheck: all passed" : "InstrumentCheck: " + failures + " failed");
		System.exit(failures == 0 ? 0 : 1);
	}

	/** The places the best trails give the notes of a piece of single notes.  Complexity: see TrailRun.run. */
	static List<String> run(Instrument ins, int[] pitches, FrameTest hand) {
		FrameTest before = Constraints.HAND;
		try {
			Constraints.HAND = hand;
			List<TrailRun.Segment> segments = TrailRun.run(ins, items(ins, pitches));
			return placements(ins, pitches, segments);
		} finally {
			Constraints.HAND = before;
		}
	}
}

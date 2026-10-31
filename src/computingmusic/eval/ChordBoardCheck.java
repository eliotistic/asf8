package computingmusic.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import computingmusic.AbcPitch;
import computingmusic.Constraints;
import computingmusic.SFModel;
import computingmusic.chord.ChordBoard;
import computingmusic.hand.FrameTest;
import computingmusic.hand.HandParams;
import computingmusic.instrument.InstrumentSpec;

/**
 * ChordBoard (the phone's chord solver) against the desktop's, SFModel.chord (ChordFingering + the trail's hand test): the same chords must
 * be playable, and each in the same places, for random two-, three- and four-note chords on the violin, viola and cello.
 *   java -cp build/classes:abcMaine.jar computingmusic.eval.ChordBoardCheck
 */
public class ChordBoardCheck {
	private static final java.util.Set<Integer> BLACK = new java.util.HashSet<Integer>(java.util.Arrays.asList(1, 3, 6, 8, 10));

	@SuppressWarnings("unchecked")
	public static void main(String[] a) throws Exception {
		InstrumentSpec[] specs = { InstrumentSpec.VIOLIN, InstrumentSpec.VIOLA, InstrumentSpec.CELLO };
		Random rnd = new Random(7);
		int checked = 0, playable = 0, bad = 0;
		for (InstrumentSpec spec : specs) {
			Constraints.HAND = new FrameTest(spec.geometry(), HandParams.forInstrument(spec), true);
			SFModel model = new SFModel();
			model.setInstrument(spec.shortName);
			ChordBoard board = new ChordBoard(spec);
			int lo = spec.openPitches[0], hi = spec.openPitches[spec.openPitches.length - 1] + 14;
			for (int t = 0; t < 400; t++) {
				int n = 2 + rnd.nextInt(3);
				TreeSet<Integer> set = new TreeSet<Integer>();
				int base = lo + rnd.nextInt(hi - lo - 12);
				while (set.size() < n) {
					set.add(base + rnd.nextInt(14));
				}
				int[] midi = new int[n];
				List<Object> names = new ArrayList<Object>();
				int i = 0;
				for (int m : set) {
					midi[i++] = m;
					names.add(AbcPitch.name(m, BLACK.contains(m % 12) ? 1 : 0));
				}
				TreeSet<String> mine = new TreeSet<String>();
				for (ChordBoard.Option o : board.solve(midi)) {
					StringBuilder sb = new StringBuilder();
					for (ChordBoard.Place p : o.places) {
						sb.append(p.midi).append(':').append(p.string).append(':').append(p.height).append(' ');
					}
					mine.add(sb.toString());
				}
				Map<String, Object> r = model.chord(names, 100000);
				TreeSet<String> theirs = new TreeSet<String>();
				for (Map<String, Object> f : (List<Map<String, Object>>) r.get("list")) {
					List<Map<String, Object>> notes = (List<Map<String, Object>>) f.get("notes");
					StringBuilder sb = new StringBuilder();
					for (int k = 0; k < notes.size(); k++) {
						Map<String, Object> nt = notes.get(k);
						int s = indexOf(spec, (String) nt.get("string"));
						sb.append(AbcPitch.parse((String) nt.get("abc")).midi).append(':').append(s).append(':').append(nt.get("height")).append(' ');
					}
					theirs.add(sb.toString());
				}
				// the desktop lists a placement's notes in string order; compare as sets of (pitch:string:height) triples
				TreeSet<String> m2 = normalise(mine), t2 = normalise(theirs);
				checked++;
				playable += mine.isEmpty() ? 0 : 1;
				if (!m2.equals(t2)) {
					bad++;
					if (bad <= 8) {
						System.out.println("DIFFERENT " + spec.name + " " + names + "\n  mine   " + m2 + "\n  theirs " + t2);
					}
				}
			}
		}
		System.out.println("chords checked: " + checked + ", playable: " + playable + ", differing: " + bad);
		System.out.println(bad == 0 ? "ChordBoardCheck: all passed" : "ChordBoardCheck: FAILED");
		System.exit(bad == 0 ? 0 : 1);
	}

	static int indexOf(InstrumentSpec spec, String name) {
		for (int s = 0; s < spec.stringNames.length; s++) {
			if (spec.stringNames[s].equals(name)) {
				return s;
			}
		}
		throw new IllegalArgumentException(name);
	}

	static TreeSet<String> normalise(TreeSet<String> in) {
		TreeSet<String> out = new TreeSet<String>();
		for (String s : in) {
			String[] t = s.trim().split(" ");
			java.util.Arrays.sort(t);
			out.add(String.join(" ", t));
		}
		return out;
	}
}

package computingmusic.chord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import computingmusic.hand.FrameTest;
import computingmusic.hand.HandParams;
import computingmusic.instrument.InstrumentSpec;

/**
 * Where can one chord be played on a bowed instrument, and with which fingers?  Pure Java (no window, no score), so the phone's app
 * and the desktop use the same code.
 *
 * Every way of giving the notes distinct neighbouring strings, a note stopped within its string's range, is a PLACEMENT.  The hand
 * must be able to hold the stopped heights at once (FrameTest, at the absolute limits).  Each placement has one or more FINGERINGS:
 * the finger assignments the hand allows, ranked by FrameTest.rank (strain, plus the soft cost of a fourth finger in a chord and of
 * skipped fingers).  The result lists every (placement, fingers) pair cheapest first; nothing is dropped for being ranked low.
 * The guitar (chords on any strings, barres) is not handled.
 * Complexity: O(strings x notes! x notes) placements, each O(1) to rank (at most 15 assignments).
 */
public final class ChordBoard {
	/** One note of a chord on the board. */
	public static final class Place {
		public final int midi;      // the sounding pitch
		public final int string;    // 0 = the lowest string
		public final int height;    // semitones above the open string; 0 = open

		Place(int midi, int string, int height) {
			this.midi = midi;
			this.string = string;
			this.height = height;
		}
	}

	/** A way to play the chord: where each note goes (in the order the notes were given) and the finger on each (0 = open). */
	public static final class Option {
		public final Place[] places;
		public final int[] fingers;
		public final double cost;       // strain plus the soft costs; lower is easier
		public final double strain;     // the largest finger-pair distance over its comfortable limit
		public final int allowed;       // how many assignments of fingers the hand allows to these heights

		Option(Place[] places, int[] fingers, double cost, double strain, int allowed) {
			this.places = places;
			this.fingers = fingers;
			this.cost = cost;
			this.strain = strain;
			this.allowed = allowed;
		}
	}

	private final InstrumentSpec spec;
	private final FrameTest hand;

	public ChordBoard(InstrumentSpec spec, HandParams params) {
		if (!spec.chordOnNeighbouringStrings) {
			throw new IllegalArgumentException(spec.name + ": chords on any strings (barres) are not handled");
		}
		this.spec = spec;
		this.hand = new FrameTest(spec.geometry(), params, true);
	}

	public ChordBoard(InstrumentSpec spec) {
		this(spec, HandParams.forInstrument(spec));
	}

	/** Every (placement, fingers) pair for the chord, cheapest first; empty when the chord cannot be played.  Complexity: see the class. */
	public List<Option> solve(int[] midi) {
		int n = midi.length;
		List<Option> out = new ArrayList<Option>();
		if (n == 0 || n > spec.openPitches.length) {
			return out;
		}
		for (int first = 0; first + n <= spec.openPitches.length; first++) {
			assign(midi, first, new int[n], new boolean[n], 0, out);
		}
		Collections.sort(out, (a, b) -> Double.compare(a.cost, b.cost));
		return out;
	}

	/** The window of strings first..first+n-1 takes the notes, one to a string, in every order; a note a string cannot stop ends the branch. */
	private void assign(int[] midi, int first, int[] stringOf, boolean[] used, int placed, List<Option> out) {
		int n = midi.length;
		if (placed == n) {
			finish(midi, stringOf, out);
			return;
		}
		int s = first + placed;
		for (int k = 0; k < n; k++) {
			if (used[k]) {
				continue;
			}
			int h = midi[k] - spec.openPitches[s];
			if (h < 0 || h > spec.maxHeights[s] || (h > 0 && h < spec.minHeights[s])) {
				continue;
			}
			used[k] = true;
			stringOf[k] = s;
			assign(midi, first, stringOf, used, placed + 1, out);
			used[k] = false;
		}
	}

	private void finish(int[] midi, int[] stringOf, List<Option> out) {
		int n = midi.length;
		Place[] places = new Place[n];
		Set<Integer> stopped = new TreeSet<Integer>();
		for (int k = 0; k < n; k++) {
			int h = midi[k] - spec.openPitches[stringOf[k]];
			places[k] = new Place(midi[k], stringOf[k], h);
			if (h > 0) {
				stopped.add(h);
			}
		}
		int[] heights = new int[stopped.size()];
		int i = 0;
		for (int h : stopped) {
			heights[i++] = h;
		}
		if (heights.length > 1 && !hand.feasible(heights)) {
			return;
		}
		if (heights.length == 0) {
			out.add(new Option(places, new int[n], 0, 0, 1));
			return;
		}
		for (FrameTest.Assignment a : hand.rank(heights, null, null, stopped)) {
			int[] fingers = new int[n];
			for (int k = 0; k < n; k++) {
				for (int j = 0; j < heights.length; j++) {
					if (heights[j] == places[k].height) {
						fingers[k] = a.fingers[j];
					}
				}
			}
			out.add(new Option(places, fingers, a.cost, a.strain, a.feasible));
		}
	}

	/**
	 * The pitches of a chord written in ABC notation as the project writes it: C middle C, c the octave above, ^ sharp, _ flat, = natural,
	 * ' up and , down an octave; the notes may be spaced or run together, inside &lt;&lt; &gt;&gt; or [ ] or bare: "gdAD", "&lt;&lt;gdAD&gt;&gt;",
	 * "D, A d ^f".  Throws IllegalArgumentException for text that is not a chord.  Complexity: O(length).
	 */
	public static int[] parse(String text) {
		List<Integer> out = new ArrayList<Integer>();
		String t = text.replace("<<", " ").replace(">>", " ").replace("[", " ").replace("]", " ");
		int i = 0;
		while (i < t.length()) {
			char c = t.charAt(i);
			if (Character.isWhitespace(c)) {
				i++;
				continue;
			}
			int alter = 0;
			while (i < t.length() && "^_=".indexOf(t.charAt(i)) >= 0) {
				alter += t.charAt(i) == '^' ? 1 : t.charAt(i) == '_' ? -1 : 0;
				i++;
			}
			if (i >= t.length() || "CDEFGABcdefgab".indexOf(t.charAt(i)) < 0) {
				throw new IllegalArgumentException("not a note at: " + t.substring(Math.min(i, t.length())).trim());
			}
			char letter = t.charAt(i++);
			int octave = Character.isLowerCase(letter) ? 5 : 4;
			while (i < t.length() && (t.charAt(i) == '\'' || t.charAt(i) == ',')) {
				octave += t.charAt(i) == '\'' ? 1 : -1;
				i++;
			}
			int step = "CDEFGAB".indexOf(Character.toUpperCase(letter));
			out.add(12 * (octave + 1) + new int[] { 0, 2, 4, 5, 7, 9, 11 }[step] + alter);
		}
		int[] r = new int[out.size()];
		for (int k = 0; k < r.length; k++) {
			r[k] = out.get(k);
		}
		return r;
	}
}

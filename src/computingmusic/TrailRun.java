package computingmusic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import computingmusic.Keynum.K;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.instrument.InstrumentSpec;
import computingmusic.jj.MusicXmlSession;

/**
 * The fingering trails run over a whole piece with no window: what pressing Extend in the app does,
 * repeated until the piece is used up.
 *
 * An anchor is the longest group of consecutive items that can be held as one chord (an arpeggio taken
 * as a chord, FingerSeq2).  Each of its fingerings starts a trail.  A trail is extended by every
 * fingering of the next item and kept only while the hand span and the finger constraints hold
 * (FingerTrail.makeCopyWithNewFingering).  When no trail survives, the stretch ends there: a hand
 * shift, or a place that cannot be played in one position.  The next anchor starts at the next item.
 *
 * The input is what the score says, one item per instant at which a note starts, chords as written:
 * nothing is combined here beyond what FingerSeq2 itself decides.
 *
 * Complexity: each extension is O(trails x fingerings of the next item); trails are capped at CAP.
 */
public class TrailRun {
	/** Where the app itself stops extending: more live trails than this is treated as a runaway. */
	static final int CAP = 500;

	public enum End {
		/** the next item has no fingering that keeps any trail within the hand's span */
		BREAK,
		/** the piece ended */
		END,
		/** more than CAP trails were alive: not extended further */
		CAP,
		/** this item has no fingering at all: it cannot be played */
		IMPOSSIBLE
	}

	/** One stretch played without leaving a hand position. */
	public static class Segment {
		public final int start, stop;       // item numbers, inclusive
		public final End end;
		public final int trails;            // trails still alive at the end
		public final FingerTrail best;      // fewest lifts, then smallest span; null if impossible
		/** The first anchor group is one step of the trail (order 0): it holds items start..firstStop. */
		public final int firstStop;
		/** Every trail alive at the end of the segment (best is one of them). */
		public final List<FingerTrail> alive;

		Segment(int start, int stop, End end, int trails, FingerTrail best, int firstStop, List<FingerTrail> alive) {
			this.alive = alive;
			this.start = start;
			this.stop = stop;
			this.end = end;
			this.trails = trails;
			this.best = best;
			this.firstStop = firstStop;
		}
	}

	/** The engine's input and, for each item, the notes of the score it stands for. */
	public static class Items {
		public final ArrayList<ChordFingering> chords = new ArrayList<ChordFingering>();
		public final List<List<JJNote>> notes = new ArrayList<List<JJNote>>();
		/** For each item, the pitches of its notes that carry a trill (each needs a finger above it too); empty when none. */
		public final List<java.util.Set<Integer>> trilled = new ArrayList<java.util.Set<Integer>>();
		/**
		 * For each item, the notes of earlier items still sounding when it begins, as rows {earlier item, pitch}; empty unless the
		 * instrument keeps held notes (InstrumentSpec.heldNotesKeepFingers).  The same rows are in chords.get(i).heldBefore.
		 */
		public final List<int[][]> sustained = new ArrayList<int[][]>();
	}

	/**
	 * Fill Items.sustained: a note sounds from its onset to onset + duration (whole notes; an end within 1e-9 of the next onset is
	 * not sounding).  Complexity: O(items x notes still sounding), the active list being pruned at each onset.
	 */
	static void markSustained(Items items) {
		List<double[]> active = new ArrayList<double[]>();     // {item, pitch, end}
		for (int i = 0; i < items.notes.size(); i++) {
			List<JJNote> group = items.notes.get(i);
			double onset = (double) group.get(0).romNum / group.get(0).romDen;
			for (int k = active.size() - 1; k >= 0; k--) {
				if (active.get(k)[2] <= onset + 1e-9) {
					active.remove(k);
				}
			}
			int[][] rows = new int[active.size()][];
			for (int k = 0; k < rows.length; k++) {
				rows[k] = new int[] {(int) active.get(k)[0], (int) active.get(k)[1]};
			}
			items.sustained.add(rows);
			items.chords.get(i).heldBefore = rows.length == 0 ? null : rows;
			for (JJNote n : group) {
				double on = (double) n.romNum / n.romDen, len = (double) n.rdurNum / n.rdurDen;
				if (on + len > onset + 1e-9) {
					active.add(new double[] {i, n.pitch, on + len});
				}
			}
		}
	}

	/**
	 * One item for each instant at which a note starts; notes starting together form a chord.  Durations and
	 * onsets are given in abc4j's units (a whole note is 768), as the app's ABC reader gives them.
	 * Complexity: O(n log n) for the sort.
	 */
	public static Items items(JJScore score, Instrument ins) {
		Items items = new Items();
		List<JJNote> sorted = score.byOnset();
		int i = 0;
		while (i < sorted.size()) {
			int j = i;
			while (j < sorted.size() && sorted.get(j).compareOnset(sorted.get(i)) == 0) {
				j++;
			}
			List<JJNote> group = new ArrayList<JJNote>(sorted.subList(i, j));
			if (ins.spec.heldNotesKeepFingers) {
				group = mergeUnisons(group);
			}
			K[] ks = new K[group.size()];
			for (int k = 0; k < ks.length; k++) {
				ks[k] = keyOf(group.get(k));
			}
			items.chords.add(new ChordFingering(items.chords.size(), ins, ks));
			items.notes.add(group);
			java.util.Set<Integer> tr = new java.util.TreeSet<Integer>();
			for (JJNote n : group) {
				if (n.trill()) {
					tr.add(n.pitch);
				}
			}
			items.trilled.add(tr);
			i = j;
		}
		if (ins.spec.heldNotesKeepFingers) {
			markSustained(items);
		}
		return items;
	}

	/**
	 * Notes of one onset that have the same pitch (two voices on one unison) are one note for a guitar, which sounds the pitch once:
	 * the longest of them stays.  The group is sorted by pitch.  Complexity: O(n).
	 */
	static List<JJNote> mergeUnisons(List<JJNote> group) {
		List<JJNote> out = new ArrayList<JJNote>();
		for (JJNote n : group) {
			JJNote last = out.isEmpty() ? null : out.get(out.size() - 1);
			if (last != null && last.pitch == n.pitch) {
				if ((double) n.rdurNum / n.rdurDen > (double) last.rdurNum / last.rdurDen) {
					out.set(out.size() - 1, n);
				}
			} else {
				out.add(n);
			}
		}
		return out;
	}

	/** The engine's name for a note: letter, accidental, and octave = (pitch - alteration) / 12. */
	static K keyOf(JJNote n) {
		K k = keyOf(n.pitch, n.alter());
		k.setDuration((short) (768L * n.rdurNum / n.rdurDen));
		k.setOm((int) (768L * n.romNum / n.romDen));
		if (k.pitch != n.pitch) {
			// the engine's note must be the note JJ holds; a wrong accidental label once made flats natural
			throw new IllegalStateException("the engine reads " + k.name + " as pitch " + k.pitch + " but JJ has "
					+ n.pitch + " (accidental \"" + n.acci + "\", measure " + n.measure() + ")");
		}
		return k;
	}

	/** The engine's key for a MIDI pitch written with an alteration (+1 sharp, -1 flat). */
	public static K keyOf(int midi, int alter) {
		if (!whiteKey(midi - alter)) {
			// JJ keeps one accidental sign, so a double sharp or double flat arrives as a single one and
			// names the wrong letter: spell the pitch itself (only the pitch matters to the fingering)
			alter = whiteKey(midi) ? 0 : 1;
		}
		int natural = midi - alter;
		String letters = "CCDDEFFGGAAB";
		String name = letters.charAt(natural % 12) + (alter > 0 ? "#" : alter < 0 ? "b" : "") + (natural / 12);
		return Keynum.parseKey(name);
	}

	public static boolean whiteKey(int midi) {
		switch (((midi % 12) + 12) % 12) {
		case 0: case 2: case 4: case 5: case 7: case 9: case 11:
			return true;
		default:
			return false;
		}
	}

	/** A violin with the constraints of SFProperties.xml, read from the working directory as the app does. */
	public static Instrument violin() {
		return instrument(InstrumentSpec.VIOLIN);
	}

	/** An instrument made from its spec; the violin's limits are those of SFProperties.xml, the others' come from the spec. */
	public static Instrument instrument(InstrumentSpec spec) {
		if (Constraints.VIOLIN_STRINGS == null) {
			computingmusic.utils.XMLPropertiesReader.main(null);
		}
		return new Instrument(spec);
	}

	/** The instrument called `name` (see InstrumentSpec.byName); when it is null, the one named by -Dsf.instrument, else the violin. */
	public static Instrument instrumentNamed(String name) {
		return instrument(name != null ? InstrumentSpec.byName(name) : InstrumentSpec.fromProperty());
	}

	/** Run the trails over the whole input.  Complexity: see the class comment. */
	public static List<Segment> run(Instrument ins, ArrayList<ChordFingering> input) {
		AnchorSequencer sequencer = new AnchorSequencer(null, input, ins);
		List<Segment> out = new ArrayList<Segment>();
		int i = 0;
		while (i < input.size()) {
			if (input.get(i).impossible()) {
				out.add(new Segment(i, i, End.IMPOSSIBLE, 0, null, i, new ArrayList<FingerTrail>()));
				i++;
				continue;
			}
			FingerSeq2 group = new FingerSeq2(false, ins, input, i);
			AnchorSequencer.Anchor anchor = sequencer.new Anchor(group);
			while (anchor.activeTrails.isEmpty() && group.shrinkable()) {
				// the app's Shrink: the longest group may have no trail, a shorter one may
				group.shrink();
				anchor = sequencer.new Anchor(group);
			}
			if (anchor.activeTrails.isEmpty()) {
				// even the first item alone gives no trail that passes the hand and string constraints
				out.add(new Segment(i, i, End.IMPOSSIBLE, 0, null, i, new ArrayList<FingerTrail>()));
				i++;
				continue;
			}
			End end = End.END;
			int firstStop = anchor.stop;
			while (anchor.extendForward()) {
				// the app's Extend, repeated
			}
			if (anchor.stop + 1 < input.size()) {
				end = anchor.extendbyCF(input.get(anchor.stop + 1)).isEmpty() ? End.BREAK : End.CAP;
			}
			out.add(new Segment(anchor.start, anchor.stop, end, anchor.activeTrails.size(), best(anchor.activeTrails), firstStop,
					new ArrayList<FingerTrail>(anchor.activeTrails)));
			i = Math.max(anchor.stop + 1, i + 1);
		}
		return out;
	}

	/** A note of the score and where the segment's best trail plays it: string (0 = highest) and height (semitones above the open string). */
	public static class Placed {
		public final JJNote note;
		public final int string, height;

		Placed(JJNote note, int string, int height) {
			this.note = note;
			this.string = string;
			this.height = height;
		}
	}

	/**
	 * For each item of a segment, the notes with the string and height the given trail has them on.  A trail
	 * records, for each step (order), the points in use; the first step is the whole first anchor group, and
	 * each later item is one step.  A note is matched to the point at its step whose pitch is the note's.
	 * Complexity: O(items x points).
	 */
	public static List<List<Placed>> placements(Instrument ins, Items items, Segment s, FingerTrail trail) {
		List<List<Placed>> out = new ArrayList<List<Placed>>();
		for (int item = s.start; item <= s.stop; item++) {
			int order = item <= s.firstStop ? 0 : item - s.firstStop;
			List<Placed> row = new ArrayList<Placed>();
			for (JJNote n : items.notes.get(item)) {
				for (FingerTrail.Trailpoint tp : trail.getTrailPoints()) {
					if (tp.onOrder.contains(order) && ins.openStringPitch(tp.saite) + tp.interval == n.pitch) {
						row.add(new Placed(n, tp.saite, tp.interval));
						break;
					}
				}
			}
			out.add(row);
		}
		return out;
	}

	static FingerTrail best(List<FingerTrail> trails) {
		FingerTrail best = null;
		for (FingerTrail t : trails) {
			if (best == null || t.lifts < best.lifts || (t.lifts == best.lifts && t.logHandSpan() < best.logHandSpan())) {
				best = t;
			}
		}
		return best;
	}

	/** Load a score and print its segments.  Usage: TrailRun FILE */
	public static void main(String[] args) throws Exception {
		MusicXmlSession session = MusicXmlSession.open(new File(args[0]));
		Instrument ins = instrumentNamed(null);
		Items items = items(session.score, ins);
		int impossible = 0;
		for (ChordFingering c : items.chords) {
			if (c.impossible()) {
				impossible++;
			}
		}
		System.out.println("items " + items.chords.size() + " (from " + session.score.notes.size() + " notes), items with no fingering at all: " + impossible);
		List<Segment> segments = run(ins, items.chords);
		for (Segment s : segments) {
			JJNote first = items.notes.get(s.start).get(0), last = items.notes.get(s.stop).get(0);
			System.out.printf("items %4d-%4d  measures %2d-%2d  %-10s trails %3d  %s%n", s.start, s.stop,
					first.measure(), last.measure(), s.end, s.trails,
					s.best == null ? "" : "heights " + s.best.fbHeightRange().x + "-" + s.best.fbHeightRange().y
							+ " lifts " + s.best.lifts + " bariolations " + s.best.countBariolations());
		}
		java.util.Map<End, Integer> tally = new java.util.EnumMap<End, Integer>(End.class);
		for (Segment s : segments) {
			Integer n = tally.get(s.end);
			tally.put(s.end, n == null ? 1 : n + 1);
		}
		System.out.println("segments " + segments.size() + " " + tally);
		java.util.Map<JJNote, PrintedCheck.Result> checked = PrintedCheck.check(ins, items, segments);
		java.util.Map<PrintedCheck.Verdict, Integer> verdicts = new java.util.EnumMap<PrintedCheck.Verdict, Integer>(PrintedCheck.Verdict.class);
		for (PrintedCheck.Result r : checked.values()) {
			Integer n = verdicts.get(r.verdict);
			verdicts.put(r.verdict, n == null ? 1 : n + 1);
		}
		System.out.println("printed fingerings against the trails: " + verdicts);
		for (PrintedCheck.Result r : checked.values()) {
			if (System.getProperty("show") != null && r.verdict.toString().equals(System.getProperty("show"))) {
				System.out.printf("  %-16s measure %2d  %-6s %s%n", r.verdict, r.note.measure(), r.note.abcName(), r.detail);
			}
		}
		for (Segment s : segments) {
			if (s.end == End.IMPOSSIBLE) {
				StringBuilder b = new StringBuilder();
				for (JJNote n : items.notes.get(s.start)) {
					b.append(n.abcName()).append(' ');
				}
				System.out.printf("impossible: item %4d  measure %2d  alone-impossible %-5s  notes %s%n", s.start,
						items.notes.get(s.start).get(0).measure(), items.chords.get(s.start).impossible(), b);
			}
		}
	}
}

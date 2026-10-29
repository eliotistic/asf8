package computingmusic.solve;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import computingmusic.ChordFingering;
import computingmusic.Constraints;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.eval.NoteByNote;
import computingmusic.hand.FrameTest;
import computingmusic.hand.Geometry;
import computingmusic.hand.HandParams;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.Json;
import computingmusic.jj.MusicXmlSession;

/**
 * Solve a whole piece with no window and write the result.
 * Usage: Solve FILE.musicxml [hand size, default 1.0]
 * Output: ~/.string_fingering/solutions/NAME.solution.json and a summary on standard output.
 */
public class Solve {
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		double size = args.length > 1 ? Double.parseDouble(args[1]) : 1.0;
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.instrumentNamed(args.length > 2 ? args[2] : null);
		HandParams hp = HandParams.forInstrument(ins.spec);
		hp.size = size;
		Constraints.HAND = new FrameTest(ins.spec.geometry(), hp, true);     // before the items: the chord fingerings are made with it
		TrailRun.Items items = TrailRun.items(score, ins);
		FrameTest comfortable = new FrameTest(ins.spec.geometry(), hp, false);
		PieceSolver.Solution sol = new PieceSolver(PieceSolver.Costs.forInstrument(ins.spec), comfortable, Constraints.HAND).solve(ins, items);
		summary(System.out, base, items, sol, size, ins, comfortable);
		write(base + ins.spec.solutionFileSuffix(), items, sol, ins.spec.stringNames);
	}

	static void summary(PrintStream out, String base, TrailRun.Items items, PieceSolver.Solution sol, double size, Instrument ins, FrameTest comfortable) throws Exception {
		int forced = 0, forcedItems = 0, shifts = 0, beyond = 0, ties = 0;
		double shiftCost = 0;
		for (PieceSolver.Choice c : sol.choices) {
			int len = c.stretch.stop - c.stretch.start + 1;
			if (c.forced()) {
				forced++;
				forcedItems += len;
			}
			if (c.onCheapestPath > 1) {
				ties++;
			}
			if (c.transition > 0) {
				shifts++;
				shiftCost += c.transition;
			}
			double neckCost = ins.spec.neckLimitHeight > 0 && c.frame.length > 0 && c.frame[c.frame.length - 1] > ins.spec.neckLimitHeight
					? PieceSolver.Costs.forInstrument(ins.spec).aboveNeck : 0;       // the thumb position's cost is not a frame beyond comfort
			if (c.within - neckCost >= 2.0) {
				beyond++;
			}
		}
		for (PieceSolver.Choice c : sol.choices) {
			if (c.trail == null) {
				for (int it = c.stretch.start; it <= c.stretch.stop; it++) {
					StringBuilder nm = new StringBuilder();
					for (JJNote jn : items.notes.get(it)) {
						nm.append(jn.abcName()).append(' ');
					}
					out.printf("  NO FINGERING for item %d (measure %d): %s%s%n", it, items.notes.get(it).get(0).measure(), nm,
							ins.spec.heldNotesKeepFingers || bowedLowStrings(ins.spec) ? " [" + why(ins, items, it) + "]" : "");
				}
			}
		}
		out.printf("%s, hand size x%.2f%n", base, size);
		out.printf("  %d items in %d stretches; items with no fingering: %d; notes with no spot: %d%n", items.chords.size(), sol.choices.size(), sol.itemsWithoutFingering, sol.notesWithoutSpot);
		out.printf("  forced stretches (one trail): %d, covering %d items; stretches chosen by cost: %d%n", forced, forcedItems, sol.choices.size() - forced);
		out.printf("  stretches with more than one cheapest option: %d%n", ties);
		out.printf("  changes of hand position with a cost: %d, total %.1f; stretches beyond comfort: %d; whole-piece cost %.1f%n", shifts, shiftCost, beyond, sol.total);
		if (bowedLowStrings(ins.spec)) {
			int outside = 0;                // the cost-based count above also counts lifts and bariolations; this one is the frame test alone
			for (PieceSolver.Choice c : sol.choices) {
				if (c.trail != null && !comfortable.feasible(c.trail.frameHeights())) {
					outside++;
				}
			}
			out.printf("  stretches whose frame the comfortable limits do not allow: %d of %d%n", outside, sol.choices.size());
		}
		if (ins.spec.neckLimitHeight > 0) {
			thumbReport(out, items, sol, ins);
		}
		if (ins.spec.heldNotesKeepFingers) {
			out.printf("  held notes: items that begin while a note sounds: %d; held fingers that could not keep their number: %d%n", heldItems(items), sol.heldFingersRenumbered);
		}
	}

	/**
	 * Why an item has no fingering: "range" (a note no string reaches, or chord size), "hand reach" (without the held notes the
	 * item still has no trail starting alone), "held notes" (it has one without them).  Complexity: O(fingerings of the item).
	 */
	static String why(Instrument ins, TrailRun.Items items, int it) throws Exception {
		ChordFingering cf = items.chords.get(it);
		if (cf.impossible()) {
			return rangeReason(ins, items.notes.get(it));
		}
		int[][] held = cf.heldBefore;
		cf.heldBefore = null;
		try {
			boolean alone = !NoteByNote.start(ins, cf, it, items.trilled.get(it).isEmpty() ? null : items.trilled.get(it)).isEmpty();
			return alone ? "held notes: playable alone, not with the notes still sounding" : "hand reach: no placement the four fingers can hold";
		} finally {
			cf.heldBefore = held;
		}
	}

	/**
	 * Items with a note stopped above the neck limit (InstrumentSpec.neckLimitHeight) need the thumb position, which the hand frame does not
	 * model: how many, the measures, and the highest note.  Complexity: O(notes).
	 */
	static void thumbReport(PrintStream out, TrailRun.Items items, PieceSolver.Solution sol, Instrument ins) {
		int count = 0, highest = -1;
		java.util.TreeSet<Integer> measures = new java.util.TreeSet<Integer>();
		for (PieceSolver.Choice c : sol.choices) {
			if (c.spots == null) {
				continue;
			}
			int item = c.stretch.start;
			for (List<NoteByNote.Spot> row : c.spots) {
				boolean thumb = false;
				for (int j = 0; j < row.size(); j++) {
					NoteByNote.Spot sp = row.get(j);
					if (sp != null && sp.height > ins.spec.neckLimitHeight) {
						thumb = true;
						highest = Math.max(highest, items.notes.get(item).get(j).pitch);
					}
				}
				if (thumb) {
					count++;
					measures.add(items.notes.get(item).get(0).measure());
				}
				item++;
			}
		}
		out.printf("  items with a note stopped above height %d (thumb position, not modelled): %d%s%s%n", ins.spec.neckLimitHeight, count,
				count > 0 ? ", measures " + measures : "", count > 0 ? ", highest " + abcOf(items, highest) : "");
	}

	static String abcOf(TrailRun.Items items, int pitch) {
		for (List<JJNote> ns : items.notes) {
			for (JJNote jn : ns) {
				if (jn.pitch == pitch) {
					return jn.abcName();
				}
			}
		}
		return "?";
	}

	/** The bowed instruments other than the violin (viola, cello, bass) print the reason an item has no fingering, as the guitar does. */
	static boolean bowedLowStrings(computingmusic.instrument.InstrumentSpec spec) {
		return spec != computingmusic.instrument.InstrumentSpec.VIOLIN && spec.chordOnNeighbouringStrings;
	}

	/**
	 * Why no placement of the notes of one item exists: a note below the lowest open string, a note above the highest height of
	 * every string (beyond the neck: the thumb position, which is not modelled), or the notes cannot take distinct strings.
	 * Complexity: O(notes x strings).
	 */
	static String rangeReason(Instrument ins, List<JJNote> notes) {
		StringBuilder why = new StringBuilder();
		boolean any = false;
		for (JJNote jn : notes) {
			int lowest = ins.openPitch(0), highest = ins.openPitch(ins.nStrings - 1) + ins.maxHeights[ins.nStrings - 1];
			boolean reachable = false;
			for (int s = 0; s < ins.nStrings; s++) {
				int h = jn.pitch - ins.openPitch(s);
				reachable |= h >= 0 && h <= ins.maxHeights[s];
			}
			if (!reachable) {
				any = true;
				why.append(jn.abcName()).append(jn.pitch < lowest ? " is below the lowest open string"
						: jn.pitch > highest ? (ins.spec.neckLimitHeight > 0 ? " is above the neck of every string but the top one (thumb position needed)" : " is above the highest stopped height") : " lies on no string").append("; ");
			}
		}
		if (any) {
			return "range: " + why.toString().trim();
		}
		return "range: the " + notes.size() + " notes cannot take distinct strings (each string sounds one note; a note reaches only the strings that sound its pitch)";
	}

	static int heldItems(TrailRun.Items items) {
		int n = 0;
		for (int[][] h : items.sustained) {
			if (h.length > 0) {
				n++;
			}
		}
		return n;
	}

	static void write(String base, TrailRun.Items items, PieceSolver.Solution sol, String[] str) throws Exception {
		List<Object> rows = new ArrayList<Object>();
		int k = 0;
		for (PieceSolver.Choice c : sol.choices) {
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("stretch", k++);
			m.put("firstItem", c.stretch.start);
			m.put("lastItem", c.stretch.stop);
			m.put("options", c.options);
			m.put("onCheapestPath", c.onCheapestPath);
			m.put("forced", c.forced());
			m.put("within", c.within);
			m.put("transition", c.transition);
			List<Object> frame = new ArrayList<Object>();
			for (int h : c.frame) {
				frame.add(h);
			}
			m.put("frame", frame);
			List<Object> notes = new ArrayList<Object>();
			if (c.spots != null) {
				int item = c.stretch.start;
				for (List<NoteByNote.Spot> row : c.spots) {
					List<JJNote> ns = items.notes.get(item);
					for (int j = 0; j < row.size(); j++) {
						Map<String, Object> e = new LinkedHashMap<String, Object>();
						e.put("item", item);
						e.put("pitch", ns.get(j).pitch);
						if (row.get(j) != null) {
							e.put("string", str[row.get(j).string]);
							e.put("height", row.get(j).height);
							int fg = c.fingerOf(row.get(j).height);
							if (fg >= 0) {
								e.put("finger", fg);
								e.put("fingerForced", c.fingerIsForced(row.get(j).height));
							}
						}
						notes.add(e);
					}
					item++;
				}
			}
			m.put("notes", notes);
			rows.add(m);
		}
		File dir = new File(System.getProperty("user.home"), ".string_fingering/solutions");
		dir.mkdirs();
		File f = new File(dir, base + ".solution.json");
		java.nio.file.Files.write(f.toPath(), Json.write(rows).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		System.out.println("  written " + f);
	}
}

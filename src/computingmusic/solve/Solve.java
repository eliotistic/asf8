package computingmusic.solve;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		Instrument ins = TrailRun.violin();
		HandParams hp = new HandParams();
		hp.size = size;
		Constraints.HAND = new FrameTest(Geometry.VIOLIN, hp, true);     // before the items: the chord fingerings are made with it
		TrailRun.Items items = TrailRun.items(score, ins);
		FrameTest comfortable = new FrameTest(Geometry.VIOLIN, hp, false);
		PieceSolver.Solution sol = new PieceSolver(new PieceSolver.Costs(), comfortable, Constraints.HAND).solve(ins, items);
		summary(System.out, base, items, sol, size);
		write(base, items, sol);
	}

	static void summary(PrintStream out, String base, TrailRun.Items items, PieceSolver.Solution sol, double size) throws Exception {
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
			if (c.within >= 2.0) {
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
					out.printf("  NO FINGERING for item %d (measure %d): %s%n", it, items.notes.get(it).get(0).measure(), nm);
				}
			}
		}
		out.printf("%s, hand size x%.2f%n", base, size);
		out.printf("  %d items in %d stretches; items with no fingering: %d; notes with no spot: %d%n", items.chords.size(), sol.choices.size(), sol.itemsWithoutFingering, sol.notesWithoutSpot);
		out.printf("  forced stretches (one trail): %d, covering %d items; stretches chosen by cost: %d%n", forced, forcedItems, sol.choices.size() - forced);
		out.printf("  stretches with more than one cheapest option: %d%n", ties);
		out.printf("  changes of hand position with a cost: %d, total %.1f; stretches beyond comfort: %d; whole-piece cost %.1f%n", shifts, shiftCost, beyond, sol.total);
	}

	static void write(String base, TrailRun.Items items, PieceSolver.Solution sol) throws Exception {
		List<Object> rows = new ArrayList<Object>();
		String[] str = {"G", "D", "A", "E"};
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

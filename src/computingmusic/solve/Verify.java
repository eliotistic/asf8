package computingmusic.solve;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import computingmusic.Constraints;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.hand.FrameTest;
import computingmusic.hand.Geometry;
import computingmusic.hand.HandParams;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.Json;
import computingmusic.jj.MusicXmlSession;

/**
 * Check a written solution against the score, independently of the solver: reads only the JSON file.
 *   1. every item of the score appears in exactly one stretch, in order, with no gap;
 *   2. every note has a string and a height, and open-string pitch + height is the note's pitch;
 *   3. the height is within the limits for that string, and each stretch's stopped heights pass the hand
 *      model at its ABSOLUTE limits;
 *   4. where the score prints a finger, 0 means an open string (height 0) and 1-4 a stopped one (height > 0).
 * Usage: Verify FILE.musicxml [hand size]
 */
public class Verify {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		double size = args.length > 1 ? Double.parseDouble(args[1]) : 1.0;
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.violin();
		TrailRun.Items items = TrailRun.items(score, ins);
		File sf = new File(System.getProperty("user.home"), ".string_fingering/solutions/" + base + ".solution.json");
		List<Object> stretches = (List<Object>) Json.parse(new String(Files.readAllBytes(sf.toPath()), StandardCharsets.UTF_8));
		HandParams hp = new HandParams();
		hp.size = size;
		FrameTest hand = new FrameTest(Geometry.VIOLIN, hp, true);
		int[] open = {55, 62, 69, 76};
		String names = "GDAE";
		int[][] confusion = new int[5][5];
		int fingerAgree = 0, fingerDisagree = 0, forcedAgree = 0, forcedDisagree = 0, openStopMiss = 0;
		int expectedItem = 0, bad = 0, notes = 0, wrongPitch = 0, overHeight = 0, frameFail = 0, printed = 0, printedOpenAgree = 0, printedStoppedAgree = 0;
		StringBuilder first = new StringBuilder();
		for (Object so : stretches) {
			Map<String, Object> s = (Map<String, Object>) so;
			int a = ((Number) s.get("firstItem")).intValue(), b = ((Number) s.get("lastItem")).intValue();
			if (a != expectedItem) {
				bad++;
				if (first.length() < 300) {
					first.append("gap or overlap at item ").append(expectedItem).append(" (stretch starts ").append(a).append(")\n");
				}
			}
			expectedItem = b + 1;
			java.util.TreeSet<Integer> heights = new java.util.TreeSet<Integer>();
			for (Object no : (List<Object>) s.get("notes")) {
				Map<String, Object> n = (Map<String, Object>) no;
				notes++;
				int item = ((Number) n.get("item")).intValue(), pitch = ((Number) n.get("pitch")).intValue();
				if (!n.containsKey("string")) {
					wrongPitch++;
					continue;
				}
				int st = names.indexOf((String) n.get("string")), h = ((Number) n.get("height")).intValue();
				if (open[st] + h != pitch) {
					wrongPitch++;
				}
				if (h > Constraints.VIOLIN_MAX_HEIGHTS[st]) {
					overHeight++;
				}
				if (h > 0) {
					heights.add(h);
				}
				// the printed finger, if any, of the note of this item with this pitch
				for (JJNote jn : items.notes.get(item)) {
					String pf = jn.fingering();
					if (jn.pitch == pitch && pf != null && pf.matches("[0-4]")) {
						printed++;
						if (n.containsKey("finger")) {
							confusion[Integer.parseInt(pf)][((Number) n.get("finger")).intValue()]++;
							boolean same = pf.equals(String.valueOf(((Number) n.get("finger")).intValue()));
							boolean forcedF = Boolean.TRUE.equals(n.get("fingerForced"));
							if (same) {
								fingerAgree++;
								if (forcedF) {
									forcedAgree++;
								}
							} else {
								fingerDisagree++;
								if (forcedF) {
									forcedDisagree++;
								}
							}
						}
						if (pf.equals("0") && h == 0) {
							printedOpenAgree++;
						} else if (!pf.equals("0") && h > 0) {
							printedStoppedAgree++;
						}
					}
				}
			}
			int[] hs = new int[heights.size()];
			int i = 0;
			for (int h : heights) {
				hs[i++] = h;
			}
			if (!hand.feasible(hs)) {
				frameFail++;
			}
		}
		if (expectedItem != items.chords.size()) {
			bad++;
			first.append("stretches end at item ").append(expectedItem - 1).append(", score has ").append(items.chords.size()).append(" items\n");
		}
		System.out.printf("%s: %d stretches, %d notes placed (score has %d)%n", base, stretches.size(), notes, score.notes.size());
		System.out.printf("  coverage faults %d | pitch not equal to string+height %d | height over the limit %d | stretch frame failing the hand model %d%n", bad, wrongPitch, overHeight, frameFail);
		System.out.printf("  printed single digits %d: agree on open/stopped %d (open %d, stopped %d)%n", printed, printedOpenAgree + printedStoppedAgree, printedOpenAgree, printedStoppedAgree);
		System.out.printf("  our finger against the printed one: agree %d, differ %d of %d  (where the hand allows no other finger: agree %d, differ %d)%n",
				fingerAgree, fingerDisagree, fingerAgree + fingerDisagree, forcedAgree, forcedDisagree);
		System.out.println("  printed (rows) against ours (columns), fingers 0-4:");
		for (int r = 0; r < 5; r++) {
			System.out.printf("    printed %d: %s%n", r, java.util.Arrays.toString(confusion[r]));
		}
		if (first.length() > 0) {
			System.out.print(first);
		}
	}
}

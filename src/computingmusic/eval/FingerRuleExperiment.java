package computingmusic.eval;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
 * A read-only experiment on the FINGER RULE.  The solver numbers a stretch's stopped heights with the assignment of
 * fingers whose largest pair distance is the least fraction of its comfortable limit (FrameTest.assign).  That prefers
 * a pair with a large limit even for a small gap: two heights a semitone apart come out as fingers 1 and 4.
 * RULE B: use CONSECUTIVE fingers, skip a finger only where the reach demands it: among the assignments the hand
 * allows, take the fewest fingers skipped between the first and the last used; ties: the least strain, then the lowest fingers.
 * The experiment re-numbers every stretch of the solver's solution (read from ~/.string_fingering/solutions) by rule B,
 * with a trilled note's upper finger (a tone above) included as the solver does, and counts how many of the score's
 * printed single digits each rule reproduces.  Nothing in the solver is changed.
 * Usage: FingerRuleExperiment FILE.musicxml   Complexity: O(notes x 15).
 */
public class FingerRuleExperiment {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.violin();
		TrailRun.Items items = TrailRun.items(score, ins);
		File sf = new File(System.getProperty("user.home"), ".string_fingering/solutions/" + base + ".solution.json");
		List<Object> stretches = (List<Object>) Json.parse(new String(Files.readAllBytes(sf.toPath()), StandardCharsets.UTF_8));
		HandParams params = new HandParams();
		FrameTest absolute = new FrameTest(Geometry.VIOLIN, params, true);
		double[] limit = params.absolute;
		int printed = 0, agreeA = 0, agreeB = 0, changed = 0, notes = 0;
		for (Object so : stretches) {
			Map<String, Object> s = (Map<String, Object>) so;
			List<Object> ns = (List<Object>) s.get("notes");
			TreeSet<Integer> heights = new TreeSet<Integer>();
			for (Object no : ns) {
				Map<String, Object> n = (Map<String, Object>) no;
				int h = ((Number) n.get("height")).intValue();
				if (h > 0) {
					heights.add(h);
				}
				int item = ((Number) n.get("item")).intValue(), pitch = ((Number) n.get("pitch")).intValue();
				for (JJNote jn : items.notes.get(item)) {
					if (jn.pitch == pitch && jn.trill()) {
						heights.add(h + 2);
					}
				}
			}
			int[] hs = new int[heights.size()];
			int i = 0;
			for (int h : heights) {
				hs[i++] = h;
			}
			int[] ruleB = hs.length == 0 ? new int[0] : consecutiveFingers(hs, absolute, params);
			for (Object no : ns) {
				Map<String, Object> n = (Map<String, Object>) no;
				int h = ((Number) n.get("height")).intValue(), item = ((Number) n.get("item")).intValue(), pitch = ((Number) n.get("pitch")).intValue();
				int fingerA = n.containsKey("finger") ? ((Number) n.get("finger")).intValue() : -1;
				int fingerB = h == 0 ? 0 : -1;
				if (h > 0 && ruleB != null) {
					for (int k = 0; k < hs.length; k++) {
						if (hs[k] == h) {
							fingerB = ruleB[k];
						}
					}
				}
				notes++;
				if (fingerA != fingerB) {
					changed++;
				}
				for (JJNote jn : items.notes.get(item)) {
					String pf = jn.fingering();
					if (jn.pitch == pitch && pf != null && pf.matches("[0-4]")) {
						printed++;
						if (pf.equals(String.valueOf(fingerA))) {
							agreeA++;
						}
						if (pf.equals(String.valueOf(fingerB))) {
							agreeB++;
						}
					}
				}
			}
		}
		System.out.printf("%s: %d notes; printed single digits %d; rule A (least strain, the solver's): %d agree (%.0f%%); rule B (consecutive fingers): %d agree (%.0f%%); notes whose finger changes: %d%n",
				base, notes, printed, agreeA, 100.0 * agreeA / printed, agreeB, 100.0 * agreeB / printed, changed);
	}

	/** Rule B for ascending distinct stopped heights; null if the hand allows no assignment.  Complexity: O(1) (15 masks). */
	static int[] consecutiveFingers(int[] heights, FrameTest absolute, HandParams params) {
		int k = heights.length;
		if (k > 4) {
			return null;
		}
		Geometry g = Geometry.VIOLIN;
		double bestScore = Double.MAX_VALUE;
		int[] best = null;
		for (int mask = 1; mask < 16; mask++) {
			if (Integer.bitCount(mask) != k) {
				continue;
			}
			int[] finger = new int[k];
			int j = 0;
			for (int f = 1; f <= 4; f++) {
				if ((mask & (1 << (f - 1))) != 0) {
					finger[j++] = f;
				}
			}
			boolean ok = true;
			double strain = 0;
			for (int a = 0; a < k && ok; a++) {
				for (int b = a + 1; b < k && ok; b++) {
					int pair = computingmusic.hand.HandParams.pairIndex(finger[a], finger[b]);
					double d = g.distance(heights[b]) - g.distance(heights[a]);
					ok = d <= params.absolute[pair];
					strain = Math.max(strain, d / params.comfortable[pair]);
				}
			}
			if (!ok) {
				continue;
			}
			int skipped = (finger[k - 1] - finger[0] + 1) - k;
			double score = skipped * 100 + strain + finger[0] * 0.001;
			if (score < bestScore) {
				bestScore = score;
				best = finger;
			}
		}
		return best;
	}
}

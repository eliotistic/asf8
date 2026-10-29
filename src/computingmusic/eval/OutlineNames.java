package computingmusic.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * PROVISIONAL names for the arrangements the engine's Topology leaves UNKNOWN, by the outline the fingers make (EH's term
 * FLAP for a flat level).  An arrangement is the four strings from the lowest: 'o' for open, else the rank of the height
 * among the stopped heights (1 = lowest), equal ranks = the same height.  A LEVEL is neighbouring stopped strings at one
 * height (an open string breaks it).
 *   FLAPLO / FLAPMID / FLAPHI   one flat level of two or more strings, the lowest, between others, or the highest height
 *   TWINFLAP                      two flat levels
 *   BAD_FLAPSKIP                  equal heights on strings with an open or different string between: no finger can do it
 *   TRIANGLEHI                     a single hill: a stopped string higher than its stopped neighbours (the mirror of TRIANGLELO)
 * Names the engine already gives are returned unchanged.  Complexity: O(1).
 */
public final class OutlineNames {
	private OutlineNames() {
	}

	public static String name(String arrangement, String engineName) {
		if (!engineName.equals("UNKNOWN")) {
			return engineName;
		}
		String[] a = arrangement.trim().split("\\s+");
		List<int[]> levels = new ArrayList<int[]>();               // {rank, width} in order along the strings
		boolean skip = false;
		int[] seenAt = new int[10];
		java.util.Arrays.fill(seenAt, -1);
		for (int s = 0; s < a.length; s++) {
			if (a[s].equals("o")) {
				continue;
			}
			int rank = Integer.parseInt(a[s]);
			boolean continues = s > 0 && !a[s - 1].equals("o") && Integer.parseInt(a[s - 1]) == rank;
			if (continues) {
				levels.get(levels.size() - 1)[1]++;
			} else {
				if (seenAt[rank] >= 0) {
					skip = true;                                    // the same height again, not next to its first string
				}
				seenAt[rank] = s;
				levels.add(new int[] {rank, 1});
			}
		}
		if (skip) {
			return "BAD_FLAPSKIP";
		}
		int wide = 0, widest = -1, lowest = Integer.MAX_VALUE, highest = -1;
		for (int[] l : levels) {
			lowest = Math.min(lowest, l[0]);
			highest = Math.max(highest, l[0]);
			if (l[1] > 1) {
				wide++;
				widest = l[0];
			}
		}
		if (wide >= 2) {
			return "TWINFLAP";
		}
		if (wide == 1) {
			return widest == lowest ? "FLAPLO" : widest == highest ? "FLAPHI" : "FLAPMID";
		}
		return "TRIANGLEHI";
	}
}

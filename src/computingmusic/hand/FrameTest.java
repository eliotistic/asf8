package computingmusic.hand;

import java.util.Arrays;

/**
 * Can the hand hold a set of stopped heights at once?  Each distinct height is a different finger, in order from
 * the nut (finger 1 nearest); the heights go to some choice of fingers out of 1-4, and every pair of them must be within
 * the model's reach limit for that pair, measured along the board in millimetres at those heights.  The strings the
 * heights are on are not used: this is the same notion of frame as the trail engine's (the stopped heights of a
 * trail), only with a physical distance in place of a fixed span of semitones.
 * Complexity: O(1) (at most 4 heights, 15 choices of fingers).
 */
public final class FrameTest {
	public final Geometry geometry;
	public final HandParams params;
	public final boolean absolute;

	public FrameTest(Geometry geometry, HandParams params, boolean absolute) {
		this.geometry = geometry;
		this.params = params;
		this.absolute = absolute;
	}

	/** heights: distinct stopped heights in ascending order (semitones above the open string). */
	public boolean feasible(int[] heights) {
		int k = heights.length;
		if (k <= 1) {
			return true;
		}
		if (k > 4) {
			return false;
		}
		double[] mm = new double[k];
		for (int i = 0; i < k; i++) {
			mm[i] = geometry.distance(heights[i]);
		}
		double[] limit = absolute ? params.absolute : params.comfortable;
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
			for (int a = 0; a < k && ok; a++) {
				for (int b = a + 1; b < k && ok; b++) {
					ok = mm[b] - mm[a] <= limit[HandParams.pairIndex(finger[a], finger[b])] * params.size;
				}
			}
			if (ok) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "hand frame, " + (absolute ? "absolute" : "comfortable") + " limits, size x" + params.size + " " + Arrays.toString(absolute ? params.absolute : params.comfortable);
	}
}

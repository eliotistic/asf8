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

	/** Which fingers hold a set of stopped heights: the choice, and how many choices the hand allows. */
	public static final class Assignment {
		public final int[] fingers;      // for each height, in ascending order of height
		public final int feasible;       // how many assignments of fingers the limits allow
		public final double strain;      // of the chosen one: the largest distance / comfortable limit over its pairs

		Assignment(int[] fingers, int feasible, double strain) {
			this.fingers = fingers;
			this.feasible = feasible;
			this.strain = strain;
		}

		public boolean forced() {
			return feasible == 1;
		}
	}

	/**
	 * The fingers for these stopped heights (distinct, ascending), one finger for each height and in order from the nut.  The
	 * assignments the limits allow (absolute or comfortable, as this test was made) are compared by strain, the
	 * largest pair distance as a fraction of its comfortable limit; the least strained wins, and a tie goes to the
	 * lower fingers.  forced() says the hand allows no other.  Null when nothing is allowed.
	 * Complexity: O(1).
	 */
	public Assignment assign(int[] heights) {
		return assign(heights, null, null);
	}

	/**
	 * As assign, with some fingers fixed: only assignments in which the finger at pinnedHeights[i] is pinnedFingers[i] count (a held
	 * finger keeps its number from one stretch to the next).  Null when no assignment allowed has them.  With the params' lowestFingersFirst
	 * (guitar) the choice among the allowed is: within the comfortable limits first, then the lowest finger numbers, then the least strain;
	 * heights may repeat (a barre in parts needs a finger each).  Complexity: O(1).
	 */
	public Assignment assign(int[] heights, int[] pinnedHeights, int[] pinnedFingers) {
		int k = heights.length;
		if (k == 0 || k > 4) {
			return k == 0 ? new Assignment(new int[0], 1, 0) : null;
		}
		double[] mm = new double[k];
		for (int i = 0; i < k; i++) {
			mm[i] = geometry.distance(heights[i]);
		}
		double[] limit = absolute ? params.absolute : params.comfortable;
		int count = 0;
		int[] best = null;
		double bestStrain = Double.MAX_VALUE;
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
					int pair = HandParams.pairIndex(finger[a], finger[b]);
					double d = mm[b] - mm[a];
					ok = d <= limit[pair] * params.size;
					strain = Math.max(strain, d / (params.comfortable[pair] * params.size));
				}
			}
			for (int q = 0; ok && pinnedHeights != null && q < pinnedHeights.length; q++) {
				for (int a = 0; a < k; a++) {
					if (heights[a] == pinnedHeights[q] && finger[a] != pinnedFingers[q]) {
						ok = false;
					}
				}
			}
			if (ok) {
				count++;
				boolean better = strain < bestStrain - 1e-9;
				if (params.lowestFingersFirst && best != null) {
					boolean thisComfy = strain <= 1 + 1e-9, bestComfy = bestStrain <= 1 + 1e-9;
					int sumThis = 0, sumBest = 0;
					for (int a = 0; a < k; a++) {
						sumThis += finger[a];
						sumBest += best[a];
					}
					better = thisComfy != bestComfy ? thisComfy : sumThis != sumBest ? sumThis < sumBest : better;
				}
				if (best == null || better) {
					bestStrain = strain;
					best = finger;
				}
			}
		}
		return best == null ? null : new Assignment(best, count, bestStrain);
	}

	@Override
	public String toString() {
		return "hand frame, " + (absolute ? "absolute" : "comfortable") + " limits, size x" + params.size + " " + Arrays.toString(absolute ? params.absolute : params.comfortable);
	}
}

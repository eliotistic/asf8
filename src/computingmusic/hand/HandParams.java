package computingmusic.hand;

/**
 * The freedoms of the left hand, as parameters.  Every limit is the largest distance, in millimetres on the board,
 * between the tips of two fingers that can both be down.  "comfortable" is what a player at ease can hold,
 * "absolute" the most the hand can do.  All values are CHOSEN placeholders until measured; `size` scales every
 * reach limit (a small hand < 1 < a large one).
 */
public final class HandParams {
	/** The finger pairs, in the order the limit arrays use. */
	public static final int[][] PAIRS = {{1, 2}, {1, 3}, {1, 4}, {2, 3}, {2, 4}, {3, 4}};

	public double size = 1.0;
	public double[] comfortable = {34, 62, 85, 34, 60, 30};
	public double[] absolute = {52, 95, 125, 50, 90, 46};
	/** Two fingers on one string must be at least this far apart along it (about a finger pad's width), mm. */
	public double minSameStringComfortable = 9, minSameStringAbsolute = 6;
	/** May a higher-numbered finger lie nearer the nut than a lower-numbered one? */
	public boolean fingersMayCross = false;

	public static int pairIndex(int a, int b) {
		int lo = Math.min(a, b), hi = Math.max(a, b);
		for (int i = 0; i < PAIRS.length; i++) {
			if (PAIRS[i][0] == lo && PAIRS[i][1] == hi) {
				return i;
			}
		}
		throw new IllegalArgumentException("no finger pair " + a + "," + b);
	}

	public HandParams copy() {
		HandParams c = new HandParams();
		c.size = size;
		c.comfortable = comfortable.clone();
		c.absolute = absolute.clone();
		c.minSameStringComfortable = minSameStringComfortable;
		c.minSameStringAbsolute = minSameStringAbsolute;
		c.fingersMayCross = fingersMayCross;
		return c;
	}
}

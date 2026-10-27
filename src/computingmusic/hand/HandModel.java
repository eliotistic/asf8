package computingmusic.hand;

import java.util.ArrayList;
import java.util.List;

/**
 * Can a hand with these parameters hold these fingers down on this board?  Pure geometry: each placement is a finger
 * (1-4), a string and a height in semitones; the verdict is the worst of the pair tests, so it explains itself.
 */
public final class HandModel {
	public enum Level {
		COMFORTABLE, POSSIBLE, IMPOSSIBLE
	}

	public static final class Placement {
		public final int finger, string, height;

		public Placement(int finger, int string, int height) {
			this.finger = finger;
			this.string = string;
			this.height = height;
		}
	}

	public static final class PairReport {
		public final int a, b;
		public final double distance, comfortableMax, absoluteMax;
		public final Level level;
		public final String note;

		PairReport(int a, int b, double distance, double comfortableMax, double absoluteMax, Level level, String note) {
			this.a = a;
			this.b = b;
			this.distance = distance;
			this.comfortableMax = comfortableMax;
			this.absoluteMax = absoluteMax;
			this.level = level;
			this.note = note;
		}
	}

	public static final class Report {
		public Level level = Level.COMFORTABLE;
		public final List<PairReport> pairs = new ArrayList<PairReport>();
	}

	static Level worse(Level x, Level y) {
		return x.ordinal() >= y.ordinal() ? x : y;
	}

	/** Distance between two fingertips, mm: along the board and across the strings together. */
	public static double tipDistance(Geometry g, Placement p, Placement q) {
		double dp = g.distance(p.height), dq = g.distance(q.height);
		double across = g.lateral(p.string, dp) - g.lateral(q.string, dq);
		double along = dp - dq;
		return Math.sqrt(along * along + across * across);
	}

	/** Complexity: O(f^2) for f placed fingers (at most 6 pairs). */
	public static Report check(Geometry g, HandParams p, List<Placement> placed) {
		Report r = new Report();
		for (int i = 0; i < placed.size(); i++) {
			for (int j = i + 1; j < placed.size(); j++) {
				Placement a = placed.get(i), b = placed.get(j);
				if (a.finger > b.finger) {
					Placement t = a;
					a = b;
					b = t;
				}
				int k = HandParams.pairIndex(a.finger, b.finger);
				double cmax = p.comfortable[k] * p.size, amax = p.absolute[k] * p.size;
				double d = tipDistance(g, a, b);
				Level level = d <= cmax ? Level.COMFORTABLE : d <= amax ? Level.POSSIBLE : Level.IMPOSSIBLE;
				String note = "";
				double da = g.distance(a.height), db = g.distance(b.height);
				if (!p.fingersMayCross && da > db + 1e-6) {
					level = Level.IMPOSSIBLE;
					note = "finger " + b.finger + " is nearer the nut than finger " + a.finger;
				}
				if (a.string == b.string) {
					double gap = Math.abs(da - db);
					if (gap < 1e-6) {
						level = Level.IMPOSSIBLE;
						note = "two fingers on the same spot";
					} else if (gap < p.minSameStringAbsolute) {
						level = worse(level, Level.IMPOSSIBLE);
						note = "fingers closer than " + fmt(p.minSameStringAbsolute) + " mm on one string";
					} else if (gap < p.minSameStringComfortable) {
						level = worse(level, Level.POSSIBLE);
						note = "fingers closer than " + fmt(p.minSameStringComfortable) + " mm on one string";
					}
				}
				r.pairs.add(new PairReport(a.finger, b.finger, d, cmax, amax, level, note));
				r.level = worse(r.level, level);
			}
		}
		return r;
	}

	static String fmt(double x) {
		return String.format("%.0f", x);
	}
}

package computingmusic;

import java.awt.Point;

/**
 * The engine's own fingering topology (OPEN, EASY, BAR, LINEUP, LINEDN, TRIANGLELO, TRIANGLEBAR, the BAD_* shapes, UNKNOWN),
 * made available to tools outside this package.  It adds no rule: it calls Topology and reports its name.
 * Points are (string index, height) with string 0 the lowest string, as the engine's fingerings give them; open strings
 * (height 0) may be included.  Complexity: O(1) (at most four points).
 */
public final class TopologyBridge {
	private TopologyBridge() {
	}

	public static String classify(Point[] points) {
		java.io.PrintStream out = System.out;
		System.setOut(new java.io.PrintStream(new java.io.ByteArrayOutputStream()));     // Topology(Point[]) prints the number of stops
		try {
			return new Topology(points).topos.name();
		} finally {
			System.setOut(out);
		}
	}
}

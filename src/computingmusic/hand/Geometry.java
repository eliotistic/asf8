package computingmusic.hand;

/**
 * The physical board of a bowed string instrument, in millimetres.  A finger stopping a string `height` semitones above
 * the open string sits distance(height) from the nut; the strings are further apart towards the bridge.
 * The numbers are parameters of the model: where one has not been measured it is marked "chosen" and is meant
 * to be replaced (see doc/HAND_MODEL.md).
 */
public final class Geometry {
	public final String name;
	public final double stopLength;     // nut to bridge, mm
	public final double nutGap;         // centre to centre of neighbouring strings at the nut, mm
	public final double bridgeGap;      // ... at the bridge, mm
	public final int[] open;            // open-string pitches in semitones (C=60), lowest string first
	public final String[] stringNames;
	public final String provenance;

	public Geometry(String name, double stopLength, double nutGap, double bridgeGap, int[] open, String[] stringNames, String provenance) {
		this.name = name;
		this.stopLength = stopLength;
		this.nutGap = nutGap;
		this.bridgeGap = bridgeGap;
		this.open = open;
		this.stringNames = stringNames;
		this.provenance = provenance;
	}

	public int strings() {
		return open.length;
	}

	/** Distance from the nut, mm, of a finger `height` semitones up the string: L (1 - 2^(-height/12)). */
	public double distance(double height) {
		return stopLength * (1 - Math.pow(2, -height / 12.0));
	}

	/** The inverse: the height in semitones of a point `mm` from the nut. */
	public double height(double mm) {
		return -12 * Math.log(1 - Math.min(mm, stopLength * 0.99) / stopLength) / Math.log(2);
	}

	/** Spacing of neighbouring strings at `mm` from the nut, interpolated linearly between nut and bridge. */
	public double gap(double mm) {
		return nutGap + (bridgeGap - nutGap) * mm / stopLength;
	}

	/** Sideways position of a string at `mm` from the nut, mm from the board's centre line (lowest string on the left). */
	public double lateral(int string, double mm) {
		return (string - (strings() - 1) / 2.0) * gap(mm);
	}

	public static final Geometry VIOLIN = new Geometry("Violin", 325, 5.5, 11.2, new int[] {55, 62, 69, 76},
			new String[] {"G", "D", "A", "E"},
			"stop 325 mm; outer strings 16-16.5 mm apart at the nut and 33-34 mm at the bridge (luthier and forum sources, not a standard)");
	public static final Geometry VIOLA = new Geometry("Viola", 370, 5.8, 11.8, new int[] {48, 55, 62, 69},
			new String[] {"C", "G", "D", "A"}, "chosen: not verified against a source");
	public static final Geometry CELLO = new Geometry("Cello", 690, 8.0, 16.0, new int[] {36, 43, 50, 57},
			new String[] {"C", "G", "D", "A"}, "chosen: not verified against a source");
	public static final Geometry BASS = new Geometry("Double bass", 1060, 11.0, 22.0, new int[] {28, 33, 38, 43},
			new String[] {"E", "A", "D", "G"}, "chosen: not verified against a source");
	public static final Geometry[] ALL = {VIOLIN, VIOLA, CELLO, BASS};
}

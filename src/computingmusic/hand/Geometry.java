package computingmusic.hand;

import computingmusic.instrument.InstrumentSpec;

/**
 * The physical board of a bowed string instrument, in millimetres.  A finger stopping a string `height` semitones above
 * the open string sits distance(height) from the nut; the strings are further apart towards the bridge.
 * Made from an InstrumentSpec (Geometry.VIOLIN etc. are its aliases).
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

	/** The boards of the instruments, derived from computingmusic.instrument.InstrumentSpec (the numbers live there). */
	public static final Geometry VIOLIN = InstrumentSpec.VIOLIN.geometry();
	public static final Geometry VIOLA = InstrumentSpec.VIOLA.geometry();
	public static final Geometry CELLO = InstrumentSpec.CELLO.geometry();
	public static final Geometry BASS = InstrumentSpec.DOUBLE_BASS.geometry();
	public static final Geometry GUITAR = InstrumentSpec.GUITAR.geometry();
	/** One for each instrument of the registry, in its order. */
	public static final Geometry[] ALL = all();

	private static Geometry[] all() {
		Geometry[] out = new Geometry[InstrumentSpec.ALL.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = InstrumentSpec.ALL.get(i).geometry();
		}
		return out;
	}
}

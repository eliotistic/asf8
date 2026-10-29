package computingmusic;

public class Constraints {
	
	public static boolean TRIANGELO;
	public static int TRIANGELO_LENGTH;
	public static boolean DIAMOND;
	public static boolean TRIBAR;
	
	// The VIOLIN_* values are the violin's defaults, read from SFProperties.xml.  The engine does not read them directly:
	// each Instrument takes its own copy of its limits (Instrument.maxHeights etc.), the violin's from here and every
	// other instrument's from its computingmusic.instrument.InstrumentSpec.
	public static int [] VIOLIN_STRINGS;
	public static int [] VIOLIN_MIN_HEIGHTS;
	public static int [] VIOLIN_MAX_HEIGHTS;
	public static boolean [] VIOLIN_OPEN;
	public static boolean [] VIOLIN_PLAYABLES;
	public static int VIOLIN_HAND_SPAN;
	/** When set, the hand's frame (the stopped heights of a trail) is tested by this model instead of the fixed span. */
	public static computingmusic.hand.FrameTest HAND;
	
	
	
	
}

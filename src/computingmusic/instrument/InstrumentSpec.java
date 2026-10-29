package computingmusic.instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import computingmusic.hand.Geometry;

/**
 * Everything the fingering system needs to know about a stringed instrument, as data: the strings and their open
 * pitches, how high each can be stopped, the physical board (for the hand model), and the rules of the chords
 * the instrument plays.  The engine (computingmusic.Instrument), the hand model (hand.Geometry) and the displays are
 * all made from one of these; to add an instrument, add a spec here (doc/INSTRUMENTS.md).
 *
 * Pitches are sounding semitones with C=60 (G3 = 55); strings are numbered from the lowest (0) up, as the engine does.
 * Where a number has not been measured it is said so in the provenance strings: "CHOSEN" means a placeholder that is
 * meant to be replaced by a measured one.
 */
public final class InstrumentSpec {
	public final String name;                 // for menus and reports, e.g. "Double bass"
	public final String shortName;            // two letters, as the engine's Instr enum: VN VA VC DB GT
	public final String[] stringNames;        // lowest string first
	public final int[] openPitches;           // sounding, C=60, lowest string first
	public final int[] maxHeights;            // per string: the highest stopped height, semitones above the open string
	public final int[] minHeights;            // per string: the lowest stopped height (0 = no restriction)
	public final double stopLengthMillimetres;   // nut to bridge
	public final double nutSpacingMillimetres;   // neighbouring strings, centre to centre, at the nut
	public final double bridgeSpacingMillimetres; // ... at the bridge
	public final int maxChordSize;            // the most notes the instrument sounds at once
	public final boolean chordOnNeighbouringStrings; // must a chord lie on strings next to one another (bowed: yes)
	public final int fingerCount;             // fingers of the left hand that stop strings
	public final String tuningProvenance;
	public final String heightProvenance;
	public final String geometryProvenance;
	/** Reach limits of the fretting/stopping hand, mm between the tips of two fingers, per finger pair in the order of HandParams.PAIRS (12 13 14 23 24 34). */
	public double[] comfortableReachMillimetres = VIOLIN_COMFORTABLE_REACH.clone();
	public double[] absoluteReachMillimetres = VIOLIN_ABSOLUTE_REACH.clone();
	public String handProvenance = "CHOSEN: the violin's placeholder limits (doc/HAND_MODEL.md), not measured for this instrument";
	/**
	 * The highest stopped height (semitones above the open string) the hand reaches in the neck positions; a note stopped above it
	 * needs the THUMB POSITION (cello, bass), which the hand frame does not model: it is only reported (Solve).  0 = no such limit
	 * (violin, viola, guitar).
	 */
	public int neckLimitHeight = 0;
	public String neckProvenance = "no thumb position";
	/**
	 * A sounding stopped (or open) note keeps its string and, if stopped, its finger while it sounds (guitar: a held bass or
	 * melody note under moving notes).  False for the bowed instruments, whose results then do not change.
	 */
	public boolean heldNotesKeepFingers = false;
	/**
	 * One finger across several strings at one height (a barre) covers every string between them: a string between two strings
	 * stopped at one height that sounds lower (open, or a lower fret) breaks the barre into separate fingers.  Guitar only.
	 */
	public boolean barreCoversStrings = false;
	/**
	 * When the hand can hold several assignments of fingers to heights, the guitar's is the one with the lowest finger numbers (index,
	 * middle, ring, little in turn) among those within the comfortable limits; the bowed instruments choose the least strained.
	 */
	public boolean lowestFingersFirst = false;

	private Geometry geometry;                // made on first use, from the numbers above

	private InstrumentSpec(String name, String shortName, String[] stringNames, int[] openPitches, int[] maxHeights,
			double stopLength, double nutSpacing, double bridgeSpacing, int maxChordSize, boolean chordOnNeighbouringStrings,
			int fingerCount, String tuningProvenance, String heightProvenance, String geometryProvenance) {
		if (stringNames.length != openPitches.length || maxHeights.length != openPitches.length) {
			throw new IllegalArgumentException(name + ": string names, open pitches and maximum heights differ in length");
		}
		this.name = name;
		this.shortName = shortName;
		this.stringNames = stringNames;
		this.openPitches = openPitches;
		this.maxHeights = maxHeights;
		this.minHeights = new int[openPitches.length];
		this.stopLengthMillimetres = stopLength;
		this.nutSpacingMillimetres = nutSpacing;
		this.bridgeSpacingMillimetres = bridgeSpacing;
		this.maxChordSize = maxChordSize;
		this.chordOnNeighbouringStrings = chordOnNeighbouringStrings;
		this.fingerCount = fingerCount;
		this.tuningProvenance = tuningProvenance;
		this.heightProvenance = heightProvenance;
		this.geometryProvenance = geometryProvenance;
	}

	public int stringCount() {
		return openPitches.length;
	}

	/** The highest pitch this instrument can stop: the top string at its maximum height. */
	public int highestPitch() {
		return openPitches[stringCount() - 1] + maxHeights[stringCount() - 1];
	}

	/** The physical board for the hand model, derived from this spec (the same object each time).  Complexity: O(1). */
	public synchronized Geometry geometry() {
		if (geometry == null) {
			Geometry made = new Geometry(name, stopLengthMillimetres, nutSpacingMillimetres, bridgeSpacingMillimetres,
					openPitches.clone(), stringNames.clone(), geometryProvenance);
			// making the first Geometry initialises the class Geometry, whose aliases ask for this very object: keep the first
			if (geometry == null) {
				geometry = made;
			}
		}
		return geometry;
	}

	@Override
	public String toString() {
		return name;
	}

	// ---- the instruments ------------------------------------------------------------------------

	/** The violin's finger-pair limits (CHOSEN placeholders, doc/HAND_MODEL.md); the viola, cello and bass use them unchanged. */
	static final double[] VIOLIN_COMFORTABLE_REACH = {34, 62, 85, 34, 60, 30};
	static final double[] VIOLIN_ABSOLUTE_REACH = {52, 95, 125, 50, 90, 46};

	/** Viola: the violin's table x1.10, rounded (see handProvenance). */
	static final double[] VIOLA_COMFORTABLE_REACH = {37, 68, 94, 37, 66, 33};
	static final double[] VIOLA_ABSOLUTE_REACH = {57, 105, 138, 55, 99, 51};
	/** Cello: pairs 12 13 14 23 24 34, mm (see the cello's handProvenance for the reasoning). */
	static final double[] CELLO_COMFORTABLE_REACH = {70, 100, 130, 70, 100, 36};
	static final double[] CELLO_ABSOLUTE_REACH = {80, 145, 175, 100, 140, 62};
	/** Double bass: fingers 1, 2 and 4 on consecutive semitones (see the bass's handProvenance). */
	static final double[] BASS_COMFORTABLE_REACH = {60, 100, 110, 60, 60, 56};
	static final double[] BASS_ABSOLUTE_REACH = {105, 150, 160, 105, 105, 100};

	private static final String CHOSEN_GEOMETRY = "chosen: not verified against a source";

	/** SFProperties.xml holds the violin's values the engine actually uses (they are the violin defaults of Constraints). */
	public static final InstrumentSpec VIOLIN = new InstrumentSpec("Violin", "VN",
			new String[] {"G", "D", "A", "E"}, new int[] {55, 62, 69, 76}, new int[] {24, 28, 32, 35},
			325, 5.5, 11.2, 4, true, 4,
			"standard tuning in fifths G3 D4 A4 E5",
			"as SFProperties.xml (24 28 32 35): the project's own limits",
			"stop 325 mm; outer strings 16-16.5 mm apart at the nut and 33-34 mm at the bridge (luthier and forum sources, not a standard)");

	public static final InstrumentSpec VIOLA = new InstrumentSpec("Viola", "VA",
			new String[] {"C", "G", "D", "A"}, new int[] {48, 55, 62, 69}, new int[] {24, 28, 32, 35},
			370, 5.8, 11.8, 4, true, 4,
			"standard tuning in fifths C3 G3 D4 A4",
			"CHOSEN: the violin's limits, not verified",
			CHOSEN_GEOMETRY)
			.reach(VIOLA_COMFORTABLE_REACH, VIOLA_ABSOLUTE_REACH,
					"CHOSEN: the violin's table x1.10 (a viola's stop 370 mm is 1.14 x the violin's 325 mm, the semitones are 14% wider, and violists report "
					+ "'a little wider' finger spacing than on the violin but the hand is the same size: a compromise); not measured");

	public static final InstrumentSpec CELLO = new InstrumentSpec("Cello", "VC",
			new String[] {"C", "G", "D", "A"}, new int[] {36, 43, 50, 57}, new int[] {12, 12, 12, 30},
			690, 8.0, 16.0, 4, true, 4,
			"standard tuning in fifths C2 G2 D3 A3",
			"CHOSEN: C, G, D 12 (the neck: see neckLimitHeight) and A 30 (the fingerboard is ~570 mm of the 690 mm string, so ~31 semitones); the thumb position is "
					+ "modelled on the top string only, where heights above 12 are reported as thumb position (Solve); on the lower strings it is out of reach (no fingering). "
					+ "Before this the lower strings had 24-28 and the solver took bariolages high up the D or G string instead of shifting on the A string",
			CHOSEN_GEOMETRY)
			.reach(CELLO_COMFORTABLE_REACH, CELLO_ABSOLUTE_REACH,
					"limits CHOSEN from VERIFIED figures (https://cellofun.eu/home/cello-blog-encyclopaedia/mechanics-technique/left-hand-homepage-a-musician-is-an-athlete-of-the-small-muscles/left-hand-finger-spacings/): "
					+ "a semitone in first position is at most 4 cm (the geometry gives 35-39 mm), a whole tone ~7 cm, 'routinely stretched' between fingers 1 and 2 in extended first position (so comfortable 12 = 70 mm, absolute 80 mm); "
					+ "the normal first-position hand 1-2 semitone, 2-3 tone, 3-4 semitone covers 4 semitones from finger 1 to 4 (127 mm at height 2: comfortable 14 = 130 mm); "
					+ "the other comfortable limits are the same counts of semitones in millimetres at the nut end; the absolute limits are the extended hand measured from the nut "
					+ "(1-3: 4 semitones from the nut = 142 mm, 1-4: 5 semitones = 173 mm, 2-4: 4 semitones from height 1 = 134 mm, 2-3: 3 semitones from height 1 = 104 mm, 3-4: a tone); "
					+ "chosen after the Suite 3 gigue's bariolage Bb-C-D-Eb (heights 1 3 5 6, 5 semitones, 163 mm) proved unplayable at the first values; not measured")
			.neck(12, "VERIFIED: the thumb position is introduced on the half-string harmonic, the A on the A string (https://en.wikipedia.org/wiki/Thumb_position); "
					+ "that height, 12, is CHOSEN as the neck limit of every string");

	public static final InstrumentSpec DOUBLE_BASS = new InstrumentSpec("Double bass", "DB",
			new String[] {"E", "A", "D", "G"}, new int[] {28, 33, 38, 43}, new int[] {12, 12, 12, 24},
			1060, 11.0, 22.0, 4, true, 4,
			"standard tuning in fourths E1 A1 D2 G2 (sounding)",
			"CHOSEN: E, A, D 12 (the neck) and G 24 (the fingerboard is ~860 mm of the 1060 mm string, ~28 semitones); as the cello, the thumb position "
					+ "is modelled on the top string only and reported (Solve)",
			CHOSEN_GEOMETRY)
			.reach(BASS_COMFORTABLE_REACH, BASS_ABSOLUTE_REACH,
					"limits CHOSEN: Simandl's half and first positions put fingers 1, 2 and 4 on three consecutive semitones, the third finger skipped "
					+ "(VERIFIED in secondary sources only: https://www.talkbass.com/threads/crab-claw-left-hand-technique.174181/ , https://researchcatalogue.net/view/1053899/1183158), "
					+ "so a semitone is 50-60 mm on this stop length and finger 1 to 4 normally covers 2 semitones (~105 mm); 13 comfortable is set just below 2 semitones so that "
					+ "the engine prefers 1-2-4 to 1-2-3 for such a frame; wider frames are extensions (absolute limits); not measured")
			.neck(12, "VERIFIED (Wikipedia, itself uncited): thumb position is used above the one-lined G, the G string's half-string harmonic (https://en.wikipedia.org/wiki/Thumb_position); "
					+ "that height, 12, is CHOSEN as the neck limit of every string");

	public static final InstrumentSpec GUITAR = new InstrumentSpec("Guitar", "GT",
			new String[] {"E", "A", "D", "G", "B", "e"}, new int[] {40, 45, 50, 55, 59, 64}, new int[] {22, 22, 22, 22, 22, 22},
			650, 7.3, 10.5, 6, false, 4,
			"standard tuning E2 A2 D3 G3 B3 E4 (sounding)",
			"CHOSEN: 22 frets on every string (electric guitars have 21-24, acoustics 19-20), not verified",
			"chosen: scale 650 mm; string spacing ~7.3 mm at the nut and ~10.5 mm at the bridge, not verified against a source")
			.guitarRules();

	/** The guitar with the 6th string tuned down a tone to D2 (Sor op. 1 no. 3 asks for it; op. 2 no. 2 is inferred); everything else as GUITAR. */
	public static final InstrumentSpec GUITAR_DROP_D = new InstrumentSpec("Guitar (drop D)", "GD",
			new String[] {"D", "A", "D", "G", "B", "e"}, new int[] {38, 45, 50, 55, 59, 64}, new int[] {22, 22, 22, 22, 22, 22},
			650, 7.3, 10.5, 6, false, 4,
			"drop-D tuning D2 A2 D3 G3 B3 E4 (sounding): the guitar with its 6th string a tone lower",
			"CHOSEN: 22 frets on every string, as the guitar",
			"chosen: as the guitar")
			.guitarRules();

	/** Set the stopping hand's reach limits (mm per finger pair) and say where they come from. */
	private InstrumentSpec reach(double[] comfortable, double[] absolute, String provenance) {
		comfortableReachMillimetres = comfortable.clone();
		absoluteReachMillimetres = absolute.clone();
		handProvenance = provenance;
		return this;
	}

	/** The neck positions end at this height; above it a note needs the thumb position (reported, not modelled). */
	private InstrumentSpec neck(int limit, String provenance) {
		neckLimitHeight = limit;
		neckProvenance = provenance;
		return this;
	}

	/**
	 * The guitar's behaviour: held notes keep their fingers, barres cover strings, and the fretting hand reaches further than
	 * the violin's in millimetres (a fret near the nut is 30-36 mm, so a stretch of four frets, fret 1 to 5, is 127 mm and five
	 * frets, fret 1 to 6, is 154 mm).  Limits per finger pair 12 13 14 23 24 34, mm, all CHOSEN:
	 * comfortable = one finger per fret plus one fret of stretch between the outer pair (1-4: four frets from fret 1, 128 mm), absolute
	 * = one more fret (1-4: five frets, 158 mm); the inner pairs a fret less each, the ring-little pair the weakest.
	 */
	private InstrumentSpec guitarRules() {
		heldNotesKeepFingers = true;
		barreCoversStrings = true;
		lowestFingersFirst = true;
		comfortableReachMillimetres = new double[] {68, 100, 128, 68, 98, 66};
		absoluteReachMillimetres = new double[] {100, 130, 158, 100, 128, 82};
		handProvenance = "CHOSEN, not measured: fret 1-5 is 127 mm on a 650 mm scale; comfortable reach of the outer pair 128 mm (4 frets from fret 1), "
				+ "absolute 158 mm (5 frets); neighbouring fingers 68/100 mm (2/3 frets), ring-little 66/82 mm; the thumb is ignored";
		return this;
	}

	/** Every instrument, in the order menus list them. */
	public static final List<InstrumentSpec> ALL;
	/** The instruments by lower-case name ("double bass") and by short name ("db"). */
	public static final Map<String, InstrumentSpec> INSTRUMENTS;

	static {
		List<InstrumentSpec> all = new ArrayList<InstrumentSpec>();
		Collections.addAll(all, VIOLIN, VIOLA, CELLO, DOUBLE_BASS, GUITAR, GUITAR_DROP_D);
		ALL = Collections.unmodifiableList(all);
		Map<String, InstrumentSpec> byName = new LinkedHashMap<String, InstrumentSpec>();
		for (InstrumentSpec s : all) {
			byName.put(s.name.toLowerCase(), s);
			byName.put(s.shortName.toLowerCase(), s);
		}
		byName.put("bass", DOUBLE_BASS);
		byName.put("doublebass", DOUBLE_BASS);
		byName.put("double_bass", DOUBLE_BASS);
		byName.put("violoncello", CELLO);
		byName.put("guitar-dropd", GUITAR_DROP_D);
		byName.put("guitar-drop-d", GUITAR_DROP_D);
		byName.put("dropd", GUITAR_DROP_D);
		byName.put("drop d", GUITAR_DROP_D);
		INSTRUMENTS = Collections.unmodifiableMap(byName);
	}

	/** The instrument called this (name, short name or common alias, any case); null or empty gives the violin. Throws for an unknown name. */
	public static InstrumentSpec byName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return VIOLIN;
		}
		InstrumentSpec s = INSTRUMENTS.get(name.trim().toLowerCase());
		if (s == null) {
			throw new IllegalArgumentException("unknown instrument '" + name + "'; known: " + names());
		}
		return s;
	}

	/** The names of the instruments, for messages. */
	public static List<String> names() {
		List<String> out = new ArrayList<String>();
		for (InstrumentSpec s : ALL) {
			out.add(s.name.toLowerCase());
		}
		return out;
	}

	/** What the solution file of a piece is called for this instrument: nothing added for the violin, ".cello" etc. for the others. */
	public String solutionFileSuffix() {
		return this == VIOLIN ? "" : "." + shortName.toLowerCase();
	}

	/** The instrument named by the system property sf.instrument (default violin). */
	public static InstrumentSpec fromProperty() {
		return byName(System.getProperty("sf.instrument"));
	}
}

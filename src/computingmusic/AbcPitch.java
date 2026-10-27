package computingmusic;

/**
 * ABC pitch notation as the project writes it: C is middle C (60), c the octave above, C, the octave below;
 * ^ sharp, _ flat, = natural (doubled for double accidentals); ' raises and , lowers an octave.
 * Complexity: O(length of the name).
 */
public final class AbcPitch {
	/** MIDI number of the sounding pitch. */
	public final int midi;
	/** Written alteration in semitones: +1 sharp, -1 flat, 0 natural or none. */
	public final int alter;

	private AbcPitch(int midi, int alter) {
		this.midi = midi;
		this.alter = alter;
	}

	public static AbcPitch parse(String name) {
		int i = 0, alter = 0;
		while (i < name.length() && "^_=".indexOf(name.charAt(i)) >= 0) {
			alter += name.charAt(i) == '^' ? 1 : name.charAt(i) == '_' ? -1 : 0;
			i++;
		}
		if (i >= name.length()) {
			throw new IllegalArgumentException("not a pitch: '" + name + "'");
		}
		char letter = name.charAt(i++);
		int step = "CDEFGAB".indexOf(Character.toUpperCase(letter));
		if (step < 0) {
			throw new IllegalArgumentException("not a pitch: '" + name + "'");
		}
		int octave = Character.isLowerCase(letter) ? 5 : 4;
		for (; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == '\'') {
				octave++;
			} else if (c == ',') {
				octave--;
			} else {
				throw new IllegalArgumentException("not a pitch: '" + name + "'");
			}
		}
		return new AbcPitch(12 * (octave + 1) + new int[] { 0, 2, 4, 5, 7, 9, 11 }[step] + alter, alter);
	}

	/** The name for a MIDI pitch with a written alteration (the inverse of parse). */
	public static String name(int midi, int alter) {
		int natural = midi - alter;
		char letter = "CCDDEFFGGAAB".charAt(natural % 12);
		int octave = natural / 12 - 1;
		StringBuilder b = new StringBuilder();
		b.append(alter > 0 ? "^" : alter < 0 ? "_" : "");
		b.append(octave >= 5 ? Character.toLowerCase(letter) : letter);
		for (int k = 5; k < octave; k++) {
			b.append('\'');
		}
		for (int k = octave; k < 4; k++) {
			b.append(',');
		}
		return b.toString();
	}
}

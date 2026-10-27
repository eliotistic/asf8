package computingmusic.jj;

import java.util.Map;

/**
 * One note as JJ holds it (a P): pitch, the accidental it was written with, its onset and length as exact
 * fractions of a whole note, and the property bag with everything else (fingering, chord_serial, ...).
 */
public class JJNote {
	public final int uid;
	public final int pitch;         // MIDI number: JJ's own value; shown to the user only as an ABC name
	/** JJ's accidental as p.ml's string_of_acci writes it: "s" sharp, "b" flat, "n" natural; "" when the P has none (the field is then absent from the dump). */
	public final String acci;
	public final long romNum, romDen;    // onset, whole notes
	public final long rdurNum, rdurDen;  // duration, whole notes
	public final Map<String, Object> props;

	JJNote(Map<String, Object> p) {
		uid = Integer.parseInt(String.valueOf(p.get("uid")));
		pitch = Integer.parseInt(String.valueOf(p.get("pitch")));
		acci = p.containsKey("acci") ? String.valueOf(p.get("acci")) : "";
		long[] rom = fraction(String.valueOf(p.get("rom")));
		long[] rdur = fraction(String.valueOf(p.get("rdur")));
		romNum = rom[0];
		romDen = rom[1];
		rdurNum = rdur[0];
		rdurDen = rdur[1];
		props = Json.object(p.get("props"));
	}

	private static long[] fraction(String s) {
		int slash = s.indexOf('/');
		if (slash < 0) {
			return new long[] { Long.parseLong(s), 1 };
		}
		return new long[] { Long.parseLong(s.substring(0, slash)), Long.parseLong(s.substring(slash + 1)) };
	}

	/** Onset in quarter notes.  Complexity: O(1). */
	public double quarters() {
		return 4.0 * romNum / romDen;
	}

	public int compareOnset(JJNote o) {
		return Long.compare(romNum * o.romDen, o.romNum * romDen);
	}

	/** Semitones the written accidental moves the letter name: +1 for JJ's "s" (sharp), -1 for "b" (flat). */
	public int alter() {
		return acci.equals("s") ? 1 : acci.equals("b") ? -1 : 0;
	}

	/** ABC pitch notation: C is middle C, c the octave above, C, the octave below; ^ sharp, _ flat. */
	public String abcName() {
		int alter = alter();
		if (!white(pitch - alter)) {              // a double sharp or flat: JJ holds one sign only, so spell the pitch itself
			alter = white(pitch) ? 0 : 1;
		}
		int natural = pitch - alter;
		String letters = "CCDDEFFGGAAB";
		int pc = natural % 12;
		char letter = letters.charAt(pc);
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

	private static boolean white(int midi) {
		int pc = ((midi % 12) + 12) % 12;
		return pc == 0 || pc == 2 || pc == 4 || pc == 5 || pc == 7 || pc == 9 || pc == 11;
	}

	public String fingering() {
		Object f = props.get("fingering");
		return f == null ? "" : String.valueOf(f);
	}

	/** The chord this note belongs to, as numbered by the reader, or -1 for a note on its own. */
	public int chordSerial() {
		Object c = props.get("chord_serial");
		return c == null ? -1 : ((Number) c).intValue();
	}

	public int measure() {
		Object m = props.get("notated_measure");
		return m == null ? -1 : ((Number) m).intValue();
	}
}

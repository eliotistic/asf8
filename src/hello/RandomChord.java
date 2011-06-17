package hello;

import java.util.Random;

public class RandomChord {
	
	private Random generator = new Random();
	static String[] chroma = { "a", "b", "c", "d", "e", "f", "g" };
	static String[] acci = { "", "#", "f" };
	private static final String[] Kchroma = { "C", "C#", "D", "Ef", "E", "F",
			"F#", "G", "G#", "A", "Bb", "B" };
	private Cfinger2 cfinger2;
	
	public RandomChord(Cfinger2 c) {
		cfinger2 = c;
		generator = new Random();
	}
	public String randChroma() {

		return chroma[generator.nextInt(6)];

	}

	public  String randKChroma() {
		return Kchroma[generator.nextInt(12)];
	}

	public  String randAcc() {
		return acci[generator.nextInt(3)];
	}

	public  int randomOctave(int pc) {
		int[] allnotes = AllNotes.ofInstr(cfinger2.getInstrument(), 3);
		
		int i = generator.nextInt(allnotes.length);
		int pitchnum = allnotes[i];
		return pitchnum / 12;
	}

	public  String randPitch() {
		String note = randChroma() + randAcc();
		Keynum.K k = Keynum.parseKey(note);
		int pc = k.pitch;

		int oct = randomOctave(pc);

		// randomly emit pc or octavized pitch
		if (generator.nextBoolean()) {
			return note;
		} else {
			return (note + oct);
		}
	}

	public  String randPitch2() {
		int pc = generator.nextInt(12);
		String note = Kchroma[pc];
		if (generator.nextBoolean()) {
			return note;
		}
		else {
			int oct = randomOctave(pc);
			// randomly emit pc or octavized pitch
			return (note + oct);
		}
	}

	public  String gen() {
		int n = 2 + generator.nextInt(3);
		String s = "";
		for (int i = 0; i < n; i++) {
			s = randPitch2() + " " + s;
		}
		return s;

	}
	public  String gen(int n) {
		//int n = 2 + generator.nextInt(3);
		String s = "";
		for (int i = 0; i < n; i++) {
			s = randPitch2() + " " + s;
		}
		return s;

	}

	public static void main(String[] args) {

		//String r = gen();
		//System.out.println(r);
	}

}

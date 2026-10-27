package computingmusic.hand;

import java.io.File;
import java.util.List;

import computingmusic.Constraints;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.eval.NoteByNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.MusicXmlSession;

/**
 * How does a piece fare under the hand model?  Runs the engine note by note over the piece with the fixed span
 * (the baseline) and with the hand model at each requested size, comfortable and absolute limits, and prints for
 * each: the items no trail can finger, the stretches, and how many stretches keep exactly one trail.
 * Usage: Calibrate FILE.musicxml [size ...]   (reads the cached JJ dump if there is one)
 */
public class Calibrate {
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.violin();
		TrailRun.Items items = TrailRun.items(score, ins);
		System.out.println("piece " + base + ": " + items.chords.size() + " items");
		report("baseline: fixed span (Constraints.VIOLIN_HAND_SPAN)", ins, items);
		items = null;
		double[] sizes = {1.0};
		if (args.length > 1) {
			sizes = new double[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				sizes[i - 1] = Double.parseDouble(args[i]);
			}
		}
		for (double size : sizes) {
			for (boolean absolute : new boolean[] {false, true}) {
				HandParams p = new HandParams();
				p.size = size;
				Constraints.HAND = new FrameTest(Geometry.VIOLIN, p, absolute);
				report(Constraints.HAND.toString(), ins, TrailRun.items(score, ins));   // items made with the model set
			}
		}
		Constraints.HAND = null;
	}

	static void report(String label, Instrument ins, TrailRun.Items items) throws Exception {
		List<NoteByNote.Stretch> st = NoteByNote.run(ins, items.chords);
		int impossible = 0, unique = 0;
		for (NoteByNote.Stretch s : st) {
			if (s.end == NoteByNote.End.IMPOSSIBLE) {
				impossible++;
			} else if (s.alive.size() == 1) {
				unique++;
			}
		}
		System.out.printf("%-90s items with no trail %4d | stretches %4d | one trail %3d%n", label, impossible, st.size(), unique);
	}
}

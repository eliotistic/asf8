package computingmusic.eval;

import java.io.File;
import java.util.List;
import java.util.TreeMap;

import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.MusicXmlSession;

/**
 * Where the printed fingerings show a finger SLIDING: two single notes next to each other (no chord, nothing between)
 * that both carry a printed finger, the same one, on different pitches.  The finger cannot have lifted unless the
 * edition printed the same digit again for a re-placed finger, so this is the evidence for a slide (or for a
 * repeated placement; the print does not say which).  Prints a histogram of the interval in semitones.
 * Usage: SlideEvidence FILE.musicxml [measure ...]  (the measures named are listed note by note)
 * Complexity: O(notes).
 */
public class SlideEvidence {
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		Instrument ins = TrailRun.violin();
		TrailRun.Items items = TrailRun.items(score, ins);
		TreeMap<Integer, Integer> up = new TreeMap<Integer, Integer>(), down = new TreeMap<Integer, Integer>();
		int pairs = 0, printedPairs = 0, same = 0, samePitch = 0;
		java.util.Set<Integer> show = new java.util.TreeSet<Integer>();
		for (int i = 1; i < args.length; i++) {
			show.add(Integer.parseInt(args[i]));
		}
		for (int i = 0; i + 1 < items.notes.size(); i++) {
			List<JJNote> a = items.notes.get(i), b = items.notes.get(i + 1);
			if (a.size() != 1 || b.size() != 1) {
				continue;
			}
			pairs++;
			String fa = a.get(0).fingering(), fb = b.get(0).fingering();
			if (fa.matches("[1-4]") && fb.matches("[1-4]")) {
				printedPairs++;
				if (fa.equals(fb)) {
					if (a.get(0).pitch == b.get(0).pitch) {
						samePitch++;
					} else {
						same++;
						int d = b.get(0).pitch - a.get(0).pitch;
						(d > 0 ? up : down).merge(Math.abs(d), 1, Integer::sum);
					}
				}
			}
		}
		System.out.printf("%s: %d adjacent single-note pairs; %d with a printed finger on both; same finger, same pitch %d; same finger, different pitch %d%n",
				base, pairs, printedPairs, samePitch, same);
		System.out.println("  same finger, pitch UP by semitones: " + up + "; DOWN: " + down);
		for (int i = 0; i < items.notes.size(); i++) {
			for (JJNote n : items.notes.get(i)) {
				if (show.contains(n.measure())) {
					System.out.printf("  m%d item %d %s printed '%s'%n", n.measure(), i, n.abcName(), n.fingering());
				}
			}
		}
	}
}

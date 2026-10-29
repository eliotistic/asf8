package computingmusic.eval;

import java.awt.Point;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import computingmusic.TopologyBridge;
import computingmusic.TrailRun;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.Json;
import computingmusic.jj.MusicXmlSession;

/**
 * An INDEPENDENT test of the fingering topology: every chord (two notes or more at one instant) of a score in which the
 * edition prints a finger on EVERY note.  A violinist fingered these, so each is a shape someone plays.  The strings and
 * heights are those the solver placed the notes on (the print gives pitches and fingers, not strings), and the engine's Topology
 * names the shape.  A printed chord the topology calls BAD is evidence against the rule (or against the placement).
 * Usage: PrintedChordShapes FILE.musicxml   Needs the solver's solution (computingmusic.solve.Solve).  Complexity: O(notes).
 */
public class PrintedChordShapes {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
		JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
		TrailRun.Items items = TrailRun.items(score, TrailRun.violin());
		File sf = new File(System.getProperty("user.home"), ".string_fingering/solutions/" + base + ".solution.json");
		List<Object> stretches = (List<Object>) Json.parse(new String(Files.readAllBytes(sf.toPath()), StandardCharsets.UTF_8));
		String names = "GDAE";
		TreeMap<Integer, List<Map<String, Object>>> byItem = new TreeMap<Integer, List<Map<String, Object>>>();
		for (Object so : stretches) {
			for (Object no : (List<Object>) ((Map<String, Object>) so).get("notes")) {
				Map<String, Object> n = (Map<String, Object>) no;
				byItem.computeIfAbsent(((Number) n.get("item")).intValue(), k -> new ArrayList<Map<String, Object>>()).add(n);
			}
		}
		int chords = 0, printedAll = 0, monotone = 0, notMonotone = 0, barred = 0;
		TreeMap<Integer, Integer> barFinger = new TreeMap<Integer, Integer>();
		List<String> notMonotoneList = new ArrayList<String>();
		TreeMap<String, Integer> byTopology = new TreeMap<String, Integer>();
		List<String> shown = new ArrayList<String>();
		for (Map.Entry<Integer, List<Map<String, Object>>> e : byItem.entrySet()) {
			List<Map<String, Object>> notes = e.getValue();
			if (notes.size() < 2) {
				continue;
			}
			chords++;
			boolean all = true;
			StringBuilder printed = new StringBuilder(), placed = new StringBuilder();
			int[] fingerOfString = new int[4];
			java.util.Arrays.fill(fingerOfString, -1);
			Point[] pts = new Point[4];
			for (int s = 0; s < 4; s++) {
				pts[s] = new Point(s, 0);
			}
			for (Map<String, Object> n : notes) {
				int pitch = ((Number) n.get("pitch")).intValue(), height = ((Number) n.get("height")).intValue();
				int string = names.indexOf((String) n.get("string"));
				String pf = "";
				for (JJNote jn : items.notes.get(e.getKey())) {
					if (jn.pitch == pitch && jn.fingering() != null && jn.fingering().matches("[0-4]")) {
						pf = jn.fingering();
					}
				}
				if (pf.isEmpty()) {
					all = false;
				}
				if (!pf.isEmpty()) {
					fingerOfString[names.indexOf((String) n.get("string"))] = Integer.parseInt(pf);
				}
				printed.append(pf.isEmpty() ? "?" : pf).append(' ');
				placed.append(n.get("string")).append(height).append(' ');
				pts[string] = new Point(string, height);
			}
			if (!all) {
				continue;
			}
			printedAll++;
			// the printed fingers of the STOPPED strings, from the lowest string: do they rise (or fall) steadily?  a tie (one finger on two strings) is a bar
			List<Integer> seq = new ArrayList<Integer>();
			for (int f : fingerOfString) {
				if (f > 0) {
					seq.add(f);
				}
			}
			boolean up = true, down = true;
			for (int i = 1; i < seq.size(); i++) {
				up &= seq.get(i) >= seq.get(i - 1);
				down &= seq.get(i) <= seq.get(i - 1);
			}
			if (up || down) {
				monotone++;
			} else {
				notMonotone++;
				notMonotoneList.add("m" + items.notes.get(e.getKey()).get(0).measure() + " fingers by string " + seq);
			}
			for (int i = 1; i < seq.size(); i++) {
				if (seq.get(i).equals(seq.get(i - 1))) {
					barred++;
					barFinger.merge(seq.get(i), 1, Integer::sum);
				}
			}
			String topology = TopologyBridge.classify(pts);
			byTopology.merge(topology, 1, Integer::sum);
			if (shown.size() < 400) {
				shown.add(String.format("  m%-3d item %-4d printed fingers %s| placed %s| %s", items.notes.get(e.getKey()).get(0).measure(), e.getKey(), printed, placed, topology));
			}
		}
		System.out.printf("%s: %d chords; %d with a printed finger on every note; their topology: %s%n", base, chords, printedAll, byTopology);
		System.out.printf("  printed fingers along the strings rise or fall steadily: %d of %d; not so: %d %s; chords with one finger across two strings (a bar): %d, by finger %s%n", monotone, printedAll, notMonotone, notMonotoneList, barred, barFinger);
		for (String line : shown) {
			if (line.endsWith("UNKNOWN") || line.contains("BAD_")) {
				System.out.println(line);
			}
		}
	}
}

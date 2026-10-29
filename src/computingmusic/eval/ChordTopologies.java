package computingmusic.eval;

import java.awt.Point;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import computingmusic.ChordFingering;
import computingmusic.Constraints;
import computingmusic.Instrument;
import computingmusic.TopologyBridge;
import computingmusic.TrailRun;
import computingmusic.hand.FrameTest;
import computingmusic.hand.Geometry;
import computingmusic.hand.HandParams;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.Json;
import computingmusic.jj.MusicXmlSession;

/**
 * Collect the TOPOLOGIES of a piece's chords.  For every chord of at least MIN notes (default 4) at one instant: its notes in ABC
 * notation; every way the engine can place it on the strings (ChordFingering's fingerings, as the solver builds them with the
 * hand model on) with the arrangement of each (open strings and the rank order of the stopped heights: "1 2 4 3") and the
 * name the engine's Topology gives it; and the placement the solver chose.  Writes one CSV row per chord and prints
 * a summary: which arrangements and names the CHOSEN placements fall in, and the chords none of whose placements is a named,
 * allowed topology.
 * Usage: ChordTopologies OUT.csv [-min N] FILE.musicxml ...   (each file needs its solver solution; run computingmusic.solve.Solve first)
 * Complexity: O(chords x placements).
 */
public class ChordTopologies {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		File out = new File(args[0]);
		int min = 4;
		List<File> files = new ArrayList<File>();
		for (int i = 1; i < args.length; i++) {
			if (args[i].equals("-min")) {
				min = Integer.parseInt(args[++i]);
			} else {
				files.add(new File(args[i]));
			}
		}
		HandParams params = new HandParams();
		Constraints.HAND = new FrameTest(Geometry.VIOLIN, params, true);          // before the items: the chord fingerings are made with it
		Instrument ins = TrailRun.violin();
		String names = "GDAE";
		TreeMap<String, Integer> chosenByName = new TreeMap<String, Integer>(), chosenByArrangement = new TreeMap<String, Integer>();
		TreeMap<String, List<String>> where = new TreeMap<String, List<String>>();
		int chords = 0, noGoodPlacement = 0;
		try (PrintWriter csv = new PrintWriter(out)) {
			csv.println("piece,measure,item,notes,chosen_placement,chosen_arrangement,chosen_topology,all_placements(arrangement:topology)");
			for (File file : files) {
				String base = file.getName().replaceAll("\\.[^.]*$", "");
				File dump = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base + "/dump.json");
				JJScore score = dump.isFile() ? JJScore.read(dump) : MusicXmlSession.loadNotes(file);
				TrailRun.Items items = TrailRun.items(score, ins);
				File sf = new File(System.getProperty("user.home"), ".string_fingering/solutions/" + base + ".solution.json");
				TreeMap<Integer, List<Map<String, Object>>> chosen = new TreeMap<Integer, List<Map<String, Object>>>();
				for (Object so : (List<Object>) Json.parse(new String(Files.readAllBytes(sf.toPath()), StandardCharsets.UTF_8))) {
					for (Object no : (List<Object>) ((Map<String, Object>) so).get("notes")) {
						Map<String, Object> n = (Map<String, Object>) no;
						chosen.computeIfAbsent(((Number) n.get("item")).intValue(), k -> new ArrayList<Map<String, Object>>()).add(n);
					}
				}
				for (int item = 0; item < items.notes.size(); item++) {
					List<JJNote> group = items.notes.get(item);
					if (group.size() < min) {
						continue;
					}
					chords++;
					StringBuilder notes = new StringBuilder();
					for (JJNote n : group) {
						notes.append(n.abcName()).append(' ');
					}
					Point[] pick = new Point[4];
					for (int s = 0; s < 4; s++) {
						pick[s] = new Point(s, 0);
					}
					StringBuilder placed = new StringBuilder();
					List<Map<String, Object>> picked = chosen.get(item);
					if (picked != null) {
						for (Map<String, Object> n : picked) {
							int s = names.indexOf((String) n.get("string")), h = ((Number) n.get("height")).intValue();
							pick[s] = new Point(s, h);
							placed.append(n.get("string")).append(h).append(' ');
						}
					}
					String chosenArrangement = arrangement(pick), chosenTopology = OutlineNames.name(chosenArrangement, TopologyBridge.classify(pick));
					StringBuilder all = new StringBuilder();
					boolean good = false;
					ChordFingering cf = items.chords.get(item);
					for (Object o : cf.getFingerings()) {
						Point[] pts = new Point[4];
						for (int s = 0; s < 4; s++) {
							pts[s] = new Point(s, 0);
						}
						for (Point p : ((ChordFingering.Fingering) o).getCoords()) {
							pts[p.x] = p;
						}
						String name = OutlineNames.name(arrangement(pts), TopologyBridge.classify(pts));
						all.append(arrangement(pts)).append(':').append(name).append(" | ");
						if (!name.startsWith("BAD_")) {
							good = true;
						}
					}
					if (!good) {
						noGoodPlacement++;
					}
					chosenByName.merge(chosenTopology, 1, Integer::sum);
					chosenByArrangement.merge(chosenArrangement + "  " + chosenTopology, 1, Integer::sum);
					where.computeIfAbsent(chosenArrangement + "  " + chosenTopology, k -> new ArrayList<String>()).add(base.replace("_ly2mxml", "") + " m" + group.get(0).measure());
					csv.printf("%s,%d,%d,\"%s\",\"%s\",%s,%s,\"%s\"%n", base, group.get(0).measure(), item, notes.toString().trim(), placed.toString().trim(),
							chosenArrangement, chosenTopology, all);
				}
			}
		}
		System.out.printf("chords of %d or more notes: %d; chords with no placement that is not BAD: %d%n", min, chords, noGoodPlacement);
		System.out.println("the CHOSEN placements by the engine's name: " + chosenByName);
		System.out.println("the CHOSEN placements by arrangement (open / rank order of heights) and name, with where:");
		for (Map.Entry<String, Integer> e : chosenByArrangement.entrySet()) {
			List<String> w = where.get(e.getKey());
			System.out.printf("  %-26s %4d   e.g. %s%n", e.getKey(), e.getValue(), w.subList(0, Math.min(4, w.size())));
		}
		System.out.println("written " + out);
	}

	/** The four strings from the lowest, 'o' for open, otherwise the rank of the height among the stopped heights (1 = lowest). */
	static String arrangement(Point[] pts) {
		TreeSet<Integer> heights = new TreeSet<Integer>();
		for (Point p : pts) {
			if (p.y > 0) {
				heights.add(p.y);
			}
		}
		List<Integer> sorted = new ArrayList<Integer>(heights);
		StringBuilder sb = new StringBuilder();
		for (int s = 0; s < 4; s++) {
			sb.append(s > 0 ? " " : "").append(pts[s].y == 0 ? "o" : String.valueOf(sorted.indexOf(pts[s].y) + 1));
		}
		return sb.toString();
	}
}

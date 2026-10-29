package computingmusic.eval;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import computingmusic.hand.FrameTest;
import computingmusic.hand.HandParams;
import computingmusic.instrument.InstrumentSpec;

/**
 * The catalogue of every FOUR-STRING chord the hand model allows: one note on each string, so a placement is the four
 * heights (h0 h1 h2 h3), lowest string first, each from 0 (open) to the string's top height.  A placement is playable when
 * the distinct stopped heights can be held at once (FrameTest, at the comfortable or the absolute limits).
 *
 * SHAPE: the placements that are one another MOVED.  Moving the hand by d semitones adds d to every stopped height and
 * leaves open strings where they are, so a placement's shape is (which strings are open; the stopped heights minus the
 * lowest stopped one).  The placements of one shape differ only by where on the neck the hand is: the slides between
 * them are the shifts by the difference of that position.
 *
 * Usage: ChordCatalog [instrument name, default violin]   Output: ~/.string_fingering/chords/NAME.shapes.csv and a summary.
 * Complexity: O(product of the string ranges) placements, each tested in O(1): about 860,000 for the violin.
 */
public class ChordCatalog {
	/** The height of finger 1 at the highest form of each position, 1st to 7th: a second, a third, a fourth, a fifth, a sixth, a seventh, an octave above the open string. */
	static final int[] POSITION_HEIGHT = {2, 4, 5, 7, 9, 11, 12};

	public static void main(String[] args) throws Exception {
		String name = args.length > 0 ? args[0] : "violin";
		InstrumentSpec spec = InstrumentSpec.byName(name);
		int strings = spec.stringCount();
		if (strings != 4) {
			throw new IllegalArgumentException("this catalogue is for four-string instruments: " + name);
		}
		HandParams params = HandParams.forInstrument(spec);
		FrameTest absolute = new FrameTest(spec.geometry(), params, true), comfortable = new FrameTest(spec.geometry(), params, false);
		int[] top = new int[strings];
		for (int s = 0; s < strings; s++) {
			top[s] = spec.maxHeights[s];
		}
		long placements = 0, playableAbsolute = 0, playableComfortable = 0;
		int[] capHeights = POSITION_HEIGHT;
		int[] capPlayable = new int[capHeights.length], capComfortable = new int[capHeights.length];
		List<TreeSet<String>> capPitchSets = new ArrayList<TreeSet<String>>(), capShapes = new ArrayList<TreeSet<String>>(), capShapesComfortable = new ArrayList<TreeSet<String>>();
		for (int c = 0; c < capHeights.length; c++) {
			capPitchSets.add(new TreeSet<String>());
			capShapes.add(new TreeSet<String>());
			capShapesComfortable.add(new TreeSet<String>());
		}
		TreeMap<String, String> topologyOfShape = new TreeMap<String, String>();
		TreeMap<String, Shape> shapes = new TreeMap<String, Shape>();
		TreeSet<String> pitchSets = new TreeSet<String>(), pitchSetsComfortable = new TreeSet<String>();
		int[] h = new int[strings];
		for (h[0] = 0; h[0] <= top[0]; h[0]++) {
			for (h[1] = 0; h[1] <= top[1]; h[1]++) {
				for (h[2] = 0; h[2] <= top[2]; h[2]++) {
					for (h[3] = 0; h[3] <= top[3]; h[3]++) {
						placements++;
						int[] stopped = stoppedHeights(h);
						if (!absolute.feasible(stopped)) {
							continue;
						}
						playableAbsolute++;
						boolean easy = comfortable.feasible(stopped);
						if (easy) {
							playableComfortable++;
						}
						int lowestStopped = stopped.length == 0 ? 0 : stopped[0];
						String shape = shapeKey(h);
						String pitches = pitchesOf(spec, h);
						for (int c = 0; c < capHeights.length; c++) {
							if (lowestStopped <= capHeights[c]) {
								capPlayable[c]++;
								if (easy) {
									capComfortable[c]++;
								}
								capPitchSets.get(c).add(pitches);
								capShapes.get(c).add(shape);
								if (easy) {
									capShapesComfortable.get(c).add(shape);
								}
							}
						}
						if (!topologyOfShape.containsKey(shape)) {
							java.awt.Point[] pts = new java.awt.Point[strings];
							for (int s = 0; s < strings; s++) {
								pts[s] = new java.awt.Point(s, h[s]);
							}
							topologyOfShape.put(shape, computingmusic.TopologyBridge.classify(pts));
						}
						pitchSets.add(pitches);
						if (easy) {
							pitchSetsComfortable.add(pitches);
						}
						Shape sh = shapes.computeIfAbsent(shapeKey(h), k -> new Shape(k, stopped.length));
						sh.add(h.clone(), easy, stopped.length == 0 ? null : absolute.assign(stopped));
					}
				}
			}
		}
		File dir = new File(System.getProperty("user.home"), ".string_fingering/chords");
		dir.mkdirs();
		int moved = 0, movedComfortable = 0, withOpen = 0, alone = 0, maxPositions = 0;
		TreeMap<Integer, Integer> sizes = new TreeMap<Integer, Integer>();
		try (PrintWriter out = new PrintWriter(new File(dir, name + ".shapes.csv"))) {
			out.println("shape,stopped_fingers,open_strings,positions,comfortable_positions,lowest_position,highest_position,positions_contiguous,fingers_same_everywhere,fingers_by_string_at_lowest");
			for (Shape sh : shapes.values()) {
				boolean open = sh.key.contains("o");
				if (open) {
					withOpen++;
				}
				sizes.merge(sh.positions.size(), 1, Integer::sum);
				if (sh.positions.size() > 1) {
					moved++;
				} else {
					alone++;
				}
				if (sh.comfortable > 1) {
					movedComfortable++;
				}
				maxPositions = Math.max(maxPositions, sh.positions.size());
				out.printf("%s,%d,%d,%d,%d,%d,%d,%s,%s,%s%n", sh.key, sh.stoppedCount, 4 - sh.stoppedCount, sh.positions.size(), sh.comfortable,
						sh.positions.first(), sh.positions.last(), sh.contiguous(), sh.sameFingers, sh.fingersAtLowest);
			}
		}
		System.out.printf("%s: %d placements (one note on each string, heights 0..%s)%n", name, placements, Arrays.toString(top));
		System.out.printf("  playable at the absolute limits: %d (%.1f%%); at the comfortable limits: %d%n", playableAbsolute, 100.0 * playableAbsolute / placements, playableComfortable);
		System.out.printf("  as distinct chords (sets of four pitches): %d absolute, %d comfortable%n", pitchSets.size(), pitchSetsComfortable.size());
		System.out.printf("  SHAPES (a chord and its moves count as one): %d; of these %d occur at more than one position, %d at only one; %d have an open string%n", shapes.size(), moved, alone, withOpen);
		System.out.printf("  shapes with more than one COMFORTABLE position: %d; most positions of one shape: %d%n", movedComfortable, maxPositions);
		System.out.println("  number of shapes by how many positions they occur at: " + sizes);
		int sameFingers = 0, contiguous = 0;
		for (Shape sh : shapes.values()) {
			if (sh.sameFingers) {
				sameFingers++;
			}
			if (sh.contiguous()) {
				contiguous++;
			}
		}
		System.out.printf("  shapes whose finger numbers are the same at every position: %d of %d; shapes whose positions form one unbroken run: %d%n", sameFingers, shapes.size(), contiguous);
		System.out.println("  written: " + new File(dir, name + ".shapes.csv"));
		System.out.println();
		System.out.println("BY THE POSITION OF THE HAND: the lowest stopped height (finger 1) at most the position's height; open strings alone count as position 0");
		System.out.println("  position (highest finger-1 height) | placements playable (absolute / comfortable) | distinct chords | shapes");
		for (int c = 0; c < capHeights.length; c++) {
			System.out.printf("  up to position %d (height %2d) | %6d / %6d | %6d | %5d%n", c + 1, capHeights[c], capPlayable[c], capComfortable[c],
					capPitchSets.get(c).size(), capShapes.get(c).size());
		}
		int cap = Integer.getInteger("sf.catalog.position", 5) - 1;
		System.out.println();
		System.out.printf("THE SHAPES BY THE ENGINE'S TOPOLOGY, hand up to position %d (finger 1 at height <= %d):%n", cap + 1, capHeights[cap]);
		TreeMap<String, Integer> byTopology = new TreeMap<String, Integer>(), byTopologyComfortable = new TreeMap<String, Integer>();
		for (String shape : capShapes.get(cap)) {
			byTopology.merge(topologyOfShape.get(shape), 1, Integer::sum);
			if (capShapesComfortable.get(cap).contains(shape)) {
				byTopologyComfortable.merge(topologyOfShape.get(shape), 1, Integer::sum);
			}
		}
		System.out.println("  topology        | shapes the hand allows | of these comfortable somewhere in the cap");
		int bad = 0, badComfortable = 0;
		for (java.util.Map.Entry<String, Integer> e : byTopology.entrySet()) {
			int easy = byTopologyComfortable.getOrDefault(e.getKey(), 0);
			System.out.printf("  %-15s | %5d | %5d%n", e.getKey(), e.getValue(), easy);
			if (e.getKey().startsWith("BAD_")) {
				bad += e.getValue();
				badComfortable += easy;
			}
		}
		System.out.printf("  the engine's BAD topologies: %d shapes allowed by the hand model (%d of them comfortable); the others: %d (%d comfortable)%n",
				bad, badComfortable, capShapes.get(cap).size() - bad, capShapesComfortable.get(cap).size() - badComfortable);
		try (PrintWriter out = new PrintWriter(new File(dir, name + ".position" + (cap + 1) + ".shapes.csv"))) {
			out.println("shape,topology,positions_up_to_the_cap,comfortable_positions,lowest_position,fingers_by_string_at_lowest");
			for (String shape : capShapes.get(cap)) {
				Shape sh = shapes.get(shape);
				int within = 0;
				for (int pos : sh.positions) {
					if (pos <= capHeights[cap]) {
						within++;
					}
				}
				out.printf("%s,%s,%d,%d,%d,%s%n", shape, topologyOfShape.get(shape), within, sh.comfortable, sh.positions.first(), sh.fingersAtLowest);
			}
		}
	}

	static int[] stoppedHeights(int[] h) {
		TreeSet<Integer> s = new TreeSet<Integer>();
		for (int x : h) {
			if (x > 0) {
				s.add(x);
			}
		}
		int[] out = new int[s.size()];
		int i = 0;
		for (int x : s) {
			out[i++] = x;
		}
		return out;
	}

	/** The pitches of a placement, sorted, as a string: two placements with the same pitches are the same chord. */
	static String pitchesOf(InstrumentSpec spec, int[] h) {
		int[] p = new int[h.length];
		for (int s = 0; s < h.length; s++) {
			p[s] = spec.openPitches[s] + h[s];
		}
		Arrays.sort(p);
		return Arrays.toString(p);
	}

	/** Open strings as 'o', stopped strings as their height above the lowest stopped height: "o,0,2,4" = G open, then 0 2 4. */
	static String shapeKey(int[] h) {
		int lowest = Integer.MAX_VALUE;
		for (int x : h) {
			if (x > 0) {
				lowest = Math.min(lowest, x);
			}
		}
		StringBuilder k = new StringBuilder();
		for (int s = 0; s < h.length; s++) {
			k.append(s > 0 ? "|" : "").append(h[s] == 0 ? "o" : String.valueOf(h[s] - lowest));
		}
		return k.toString();
	}

	static class Shape {
		final String key;
		final int stoppedCount;
		final TreeSet<Integer> positions = new TreeSet<Integer>();        // the lowest stopped height: where the hand is
		int comfortable;
		boolean sameFingers = true;
		String fingersAtLowest = "";
		private int[] firstFingers;

		Shape(String key, int stoppedCount) {
			this.key = key;
			this.stoppedCount = stoppedCount;
		}

		void add(int[] h, boolean easy, FrameTest.Assignment a) {
			int lowest = Integer.MAX_VALUE;
			for (int x : h) {
				if (x > 0) {
					lowest = Math.min(lowest, x);
				}
			}
			positions.add(lowest == Integer.MAX_VALUE ? 0 : lowest);
			if (easy) {
				comfortable++;
			}
			if (a != null) {
				if (firstFingers == null) {
					firstFingers = a.fingers;
					StringBuilder f = new StringBuilder();
					for (int s = 0; s < h.length; s++) {
						int k = 0;
						TreeSet<Integer> heights = new TreeSet<Integer>();
						for (int x : h) if (x > 0) heights.add(x);
						for (int x : heights) {
							if (x == h[s]) break;
							k++;
						}
						f.append(s > 0 ? " " : "").append(h[s] == 0 ? "0" : String.valueOf(a.fingers[k]));
					}
					fingersAtLowest = f.toString();
				} else if (!Arrays.equals(firstFingers, a.fingers)) {
					sameFingers = false;
				}
			}
		}

		boolean contiguous() {
			return positions.last() - positions.first() + 1 == positions.size();
		}
	}
}

package computingmusic.jj;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import computingmusic.Instrument;
import computingmusic.PrintedCheck;
import computingmusic.TrailRun;

/**
 * A run of the fingering trails over a loaded piece, arranged for the notation: which segment each drawn note
 * belongs to, and how to colour and describe it.
 */
public class TrailResult {
	private static final Color[] SEGMENT = {
		new Color(120, 170, 255, 70), new Color(255, 200, 80, 90), new Color(120, 210, 150, 80), new Color(220, 140, 230, 80)
	};
	private static final Color IMPOSSIBLE = new Color(230, 20, 20, 150);

	final TrailRun.Items items;
	final List<TrailRun.Segment> segments;
	private String[] STRING_NAME = { "G", "D", "A", "E" };   // string 0 is the lowest; set from the instrument by of()

	private final int[] itemOfLink;       // link (note in onset order) -> item
	private final int[] segmentOfItem;
	private final JJNote[] noteOfLink;
	private final Map<JJNote, PrintedCheck.Result> checks;

	private TrailResult(TrailRun.Items items, List<TrailRun.Segment> segments, int[] itemOfLink, int[] segmentOfItem,
			JJNote[] noteOfLink, Map<JJNote, PrintedCheck.Result> checks) {
		this.items = items;
		this.segments = segments;
		this.itemOfLink = itemOfLink;
		this.segmentOfItem = segmentOfItem;
		this.noteOfLink = noteOfLink;
		this.checks = checks;
	}

	/** Run the trails over a session's notes.  Complexity: see TrailRun. */
	public static TrailResult of(MusicXmlSession s) {
		Instrument ins = TrailRun.instrumentNamed(null);
		TrailRun.Items items = TrailRun.items(s.score, ins);
		List<TrailRun.Segment> segments = TrailRun.run(ins, items.chords);
		int[] segmentOfItem = new int[items.chords.size()];
		for (int k = 0; k < segments.size(); k++) {
			for (int i = segments.get(k).start; i <= segments.get(k).stop; i++) {
				segmentOfItem[i] = k;
			}
		}
		// the links are in onset order, and so are the items: an item ends where the onset changes
		int[] itemOfLink = new int[s.links.size()];
		int item = 0;
		for (int i = 0; i < itemOfLink.length; i++) {
			if (i > 0 && s.links.get(i).note.compareOnset(s.links.get(i - 1).note) != 0) {
				item++;
			}
			itemOfLink[i] = item;
		}
		JJNote[] noteOfLink = new JJNote[itemOfLink.length];
		for (int i = 0; i < noteOfLink.length; i++) {
			noteOfLink[i] = s.links.get(i).note;
		}
		TrailResult result = new TrailResult(items, segments, itemOfLink, segmentOfItem, noteOfLink,
				PrintedCheck.check(ins, items, segments));
		result.STRING_NAME = ins.spec.stringNames;
		return result;
	}

	/** 1 if the printed fingering of this note fits a trail of its segment, 2 if it does not, 0 if nothing was checked. */
	int mark(int link) {
		PrintedCheck.Result r = checks.get(noteOfLink[link]);
		if (r == null) {
			return 0;
		}
		switch (r.verdict) {
		case OPEN_AGREES:
		case CONSISTENT:
			return 1;
		case OPEN_DISAGREES:
		case STOPPED_DISAGREES:
		case ORDER_BROKEN:
			return 2;
		default:
			return 0;
		}
	}

	public String summary() {
		Map<TrailRun.End, Integer> tally = new EnumMap<TrailRun.End, Integer>(TrailRun.End.class);
		for (TrailRun.Segment g : segments) {
			Integer n = tally.get(g.end);
			tally.put(g.end, n == null ? 1 : n + 1);
		}
		int agree = 0, disagree = 0;
		for (int i = 0; i < noteOfLink.length; i++) {
			int m = mark(i);
			agree += m == 1 ? 1 : 0;
			disagree += m == 2 ? 1 : 0;
		}
		return segments.size() + " segments over " + items.chords.size() + " items " + tally + "; printed fingerings that fit a trail: "
				+ agree + ", that do not: " + disagree + ". Shading = segments, red = cannot be played; green dot = printed fingering fits, magenta = it does not.";
	}

	Color colour(int link) {
		TrailRun.Segment g = segments.get(segmentOfItem[itemOfLink[link]]);
		return g.end == TrailRun.End.IMPOSSIBLE ? IMPOSSIBLE : SEGMENT[segmentOfItem[itemOfLink[link]] % SEGMENT.length];
	}

	/** Text for the info pane about the segment that holds a note.  Complexity: O(1). */
	String describe(int link) {
		int item = itemOfLink[link];
		TrailRun.Segment g = segments.get(segmentOfItem[item]);
		StringBuilder b = new StringBuilder("trails: item ").append(item).append(" of ").append(items.chords.size());
		if (g.end == TrailRun.End.IMPOSSIBLE) {
			b.append(" cannot be fingered in any position the constraints allow\n");
			return b.toString();
		}
		List<String> measures = new ArrayList<String>();
		measures.add(String.valueOf(items.notes.get(g.start).get(0).measure()));
		measures.add(String.valueOf(items.notes.get(g.stop).get(0).measure()));
		b.append("; segment ").append(segmentOfItem[item]).append(" = items ").append(g.start).append("-").append(g.stop)
				.append(", measures ").append(measures.get(0)).append("-").append(measures.get(1)).append('\n');
		b.append("  ends: ").append(g.end == TrailRun.End.BREAK ? "no fingering of the next item keeps the hand in this position"
				: g.end == TrailRun.End.END ? "end of the piece" : "more trails than the engine follows").append('\n');
		b.append("  trails alive at the end: ").append(g.trails).append("; best trail: heights ")
				.append(g.best.fbHeightRange().x).append("-").append(g.best.fbHeightRange().y)
				.append(", lifts ").append(g.best.lifts).append(", bariolations ").append(g.best.countBariolations()).append('\n');
		PrintedCheck.Result r = checks.get(noteOfLink[link]);
		if (r != null && r.string >= 0) {
			b.append("  the trail that fits the printed fingering best puts this note on the ").append(STRING_NAME[r.string])
					.append(" string at height ").append(r.height).append(r.height == 0 ? " (open)" : "").append('\n');
		}
		if (r != null && !r.detail.isEmpty()) {
			b.append("  printed fingering against it: ").append(r.verdict).append(" - ").append(r.detail).append('\n');
		}
		return b.toString();
	}
}

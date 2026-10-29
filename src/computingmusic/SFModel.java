package computingmusic;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import computingmusic.Keynum.K;
import computingmusic.instrument.InstrumentSpec;
import computingmusic.jj.JJNote;
import computingmusic.jj.JJScore;
import computingmusic.jj.MusicXmlSession;
import computingmusic.utils.StringFingeringConstants;

/**
 * The String Fingering model with no window: load a piece, set the constraints, ask about a chord, run the
 * trails over the whole piece, and read the results as plain data (maps and lists, ready for JSON).
 *
 * Pitches are given and reported in ABC notation.  One model holds one piece.  Not thread-safe; callers
 * take turns.
 */
public class SFModel {

	private File file;
	private JJScore score;
	private int stringRange = StringFingeringConstants.STRING_RANGE;
	/** The instrument (default violin); changed by load's "instrument" or by setInstrument. */
	private InstrumentSpec spec = InstrumentSpec.VIOLIN;
	/** Constraint changes for an instrument other than the violin (the violin's are kept in Constraints, as before). */
	private int[] maxHeightsChanged, minHeightsChanged;
	private Integer handSpanChanged;
	private TrailRun.Items items;
	private List<TrailRun.Segment> segments;
	private Map<JJNote, PrintedCheck.Result> checks;

	private static Map<String, Object> map(Object... kv) {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put((String) kv[i], kv[i + 1]);
		}
		return m;
	}

	/** The instrument with the current constraints.  Built afresh so a constraint change is always seen. */
	Instrument instrument() {
		Instrument ins = TrailRun.instrument(spec);
		ins.stringRange = stringRange;
		if (spec != InstrumentSpec.VIOLIN) {
			ins.maxHeights = maxHeightsChanged != null ? maxHeightsChanged : ins.maxHeights;
			ins.minHeights = minHeightsChanged != null ? minHeightsChanged : ins.minHeights;
			ins.handSpan = handSpanChanged != null ? handSpanChanged : ins.handSpan;
		}
		return ins;
	}

	private String stringName(int string) {
		return spec.stringNames[string];
	}

	/** Choose the instrument (by name); a loaded piece is read again for it and any earlier run is dropped. */
	public Map<String, Object> setInstrument(String name) {
		InstrumentSpec chosen = InstrumentSpec.byName(name);
		if (chosen != spec) {
			spec = chosen;
			maxHeightsChanged = minHeightsChanged = null;
			handSpanChanged = null;
			if (score != null) {
				items = TrailRun.items(score, instrument());
			}
			segments = null;
			checks = null;
		}
		return map("instrument", spec.name, "strings", java.util.Arrays.asList(spec.stringNames));
	}

	// ---- piece ----------------------------------------------------------------------------------

	/** Load a score through JJ (MusicXML, MEI, kern, ...).  Clears any earlier run. */
	public Map<String, Object> load(String path) throws Exception {
		return load(path, null);
	}

	/** As load(path), for the instrument named (null keeps the current one). */
	public Map<String, Object> load(String path, String instrumentName) throws Exception {
		if (instrumentName != null) {
			spec = InstrumentSpec.byName(instrumentName);
			maxHeightsChanged = minHeightsChanged = null;
			handSpanChanged = null;
		}
		file = new File(path);
		score = MusicXmlSession.loadNotes(file);
		items = TrailRun.items(score, instrument());
		segments = null;
		checks = null;
		int chords = 0, fingered = 0;
		for (List<JJNote> group : items.notes) {
			chords += group.size() > 1 ? 1 : 0;
		}
		for (JJNote n : score.notes) {
			fingered += n.fingering().isEmpty() ? 0 : 1;
		}
		return map("file", file.getName(), "instrument", spec.name, "notes", score.notes.size(), "items", items.chords.size(),
				"chords", chords, "printedFingerings", fingered);
	}

	private void needPiece() {
		if (items == null) {
			throw new IllegalStateException("no piece loaded: send load first");
		}
	}

	// ---- constraints ----------------------------------------------------------------------------

	public Map<String, Object> constraints() {
		Instrument ins = instrument();
		return map("instrument", spec.name, "openStrings", ins.openStrings(), "minHeights", ins.minHeights,
				"maxHeights", ins.maxHeights, "handSpan", ins.handSpan,
				"stringRange", stringRange);
	}

	/**
	 * Change constraints.  Keys: maxHeights, minHeights (four numbers, lowest string first), handSpan, stringRange.
	 * A loaded piece is re-read into the engine, and any earlier run is dropped.
	 */
	public Map<String, Object> setConstraints(Map<String, Object> m) {
		instrument();
		boolean violin = spec == InstrumentSpec.VIOLIN;
		if (m.containsKey("maxHeights")) {
			int[] v = ints(m.get("maxHeights"), spec.stringCount(), "maxHeights");
			if (violin) {
				Constraints.VIOLIN_MAX_HEIGHTS = v;
			} else {
				maxHeightsChanged = v;
			}
		}
		if (m.containsKey("minHeights")) {
			int[] v = ints(m.get("minHeights"), spec.stringCount(), "minHeights");
			if (violin) {
				Constraints.VIOLIN_MIN_HEIGHTS = v;
			} else {
				minHeightsChanged = v;
			}
		}
		if (m.containsKey("handSpan")) {
			int v = ((Number) m.get("handSpan")).intValue();
			if (violin) {
				Constraints.VIOLIN_HAND_SPAN = v;
			} else {
				handSpanChanged = v;
			}
		}
		if (m.containsKey("stringRange")) {
			int r = ((Number) m.get("stringRange")).intValue();
			if (r < 1 || r >= StringFingeringConstants.SPANNING_VECTOR_SIZE_IN_INSTRUMENT) {
				throw new IllegalArgumentException("stringRange must be 1.." + (StringFingeringConstants.SPANNING_VECTOR_SIZE_IN_INSTRUMENT - 1));
			}
			stringRange = r;
		}
		if (score != null) {
			items = TrailRun.items(score, instrument());
		}
		segments = null;
		checks = null;
		return constraints();
	}

	private static int[] ints(Object v, int length, String what) {
		List<Object> l = computingmusic.jj.Json.array(v);
		if (l.size() != length) {
			throw new IllegalArgumentException(what + " needs " + length + " numbers");
		}
		int[] a = new int[length];
		for (int i = 0; i < length; i++) {
			a[i] = ((Number) l.get(i)).intValue();
		}
		return a;
	}

	// ---- one chord ------------------------------------------------------------------------------

	/**
	 * What the engine makes of one chord (or one note).  The chord solver finds every way to put the notes on
	 * different strings; each is then tried as the start of a trail, which is where the height limits, the hand
	 * span and the finger constraints apply (the same test an anchor uses).  Only those that pass are listed:
	 * "playable" means at least one passes.  "eachNoteCouldBe" gives, for every note alone, the places the limits
	 * allow, which shows what rules a chord out.
	 */
	public Map<String, Object> chord(List<Object> names, int limit) {
		Instrument ins = instrument();
		List<AbcPitch> pitches = new ArrayList<AbcPitch>();
		for (Object o : names) {
			pitches.add(AbcPitch.parse(String.valueOf(o)));
		}
		K[] ks = new K[pitches.size()];
		for (int i = 0; i < ks.length; i++) {
			ks[i] = TrailRun.keyOf(pitches.get(i).midi, pitches.get(i).alter);
		}
		ChordFingering cf = new ChordFingering(0, ins, ks);
		List<Map<String, Object>> fingerings = new ArrayList<Map<String, Object>>();
		int valid = 0;
		for (ChordFingering.Fingering f : cf.getFingerings()) {
			if (new FingerTrail(ins).makeCopyWithNewFingering(f) == null) {
				continue;
			}
			valid++;
			if (fingerings.size() >= limit) {
				continue;
			}
			List<Map<String, Object>> notes = new ArrayList<Map<String, Object>>();
			for (ChordFingering.XCase x : f.fingering) {
				AbcPitch p = nearest(pitches, x.pitch);
				notes.add(map("abc", AbcPitch.name(x.pitch, p == null ? 0 : p.alter), "string", stringName(x.saite), "height", x.height));
			}
			fingerings.add(map("notes", notes));
		}
		List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
		for (AbcPitch p : pitches) {
			List<Map<String, Object>> where = new ArrayList<Map<String, Object>>();
			for (int s = 0; s < ins.nStrings; s++) {
				int h = p.midi - ins.openStringPitch(s);
				if (h >= 0 && h <= stringRange && h <= ins.maxHeights[s] && h >= ins.minHeights[s]) {
					where.add(map("string", stringName(s), "height", h));
				}
			}
			options.add(map("abc", AbcPitch.name(p.midi, p.alter), "places", where));
		}
		return map("playable", valid > 0, "onDifferentStrings", cf.nFingerings(), "passingTheConstraints", valid,
				"shown", fingerings.size(), "list", fingerings, "eachNoteCouldBe", options);
	}

	private static AbcPitch nearest(List<AbcPitch> pitches, int midi) {
		for (AbcPitch p : pitches) {
			if (p.midi == midi) {
				return p;
			}
		}
		return null;
	}

	// ---- whole piece ----------------------------------------------------------------------------

	/** Run the trails over the loaded piece and the printed-fingering check.  Complexity: see TrailRun. */
	public Map<String, Object> run() {
		needPiece();
		Instrument ins = instrument();
		items = TrailRun.items(score, ins);
		segments = TrailRun.run(ins, items.chords);
		checks = PrintedCheck.check(ins, items, segments);
		Map<TrailRun.End, Integer> ends = new EnumMap<TrailRun.End, Integer>(TrailRun.End.class);
		for (TrailRun.Segment s : segments) {
			Integer n = ends.get(s.end);
			ends.put(s.end, n == null ? 1 : n + 1);
		}
		Map<String, Object> endCounts = new LinkedHashMap<String, Object>();
		for (Map.Entry<TrailRun.End, Integer> e : ends.entrySet()) {
			endCounts.put(e.getKey().toString(), e.getValue());
		}
		return map("items", items.chords.size(), "segments", segments.size(), "ends", endCounts, "printed", verdictCounts());
	}

	private Map<String, Object> verdictCounts() {
		Map<PrintedCheck.Verdict, Integer> v = new EnumMap<PrintedCheck.Verdict, Integer>(PrintedCheck.Verdict.class);
		for (PrintedCheck.Result r : checks.values()) {
			Integer n = v.get(r.verdict);
			v.put(r.verdict, n == null ? 1 : n + 1);
		}
		Map<String, Object> out = new LinkedHashMap<String, Object>();
		for (Map.Entry<PrintedCheck.Verdict, Integer> e : v.entrySet()) {
			if (e.getKey() != PrintedCheck.Verdict.NONE) {
				out.put(e.getKey().toString(), e.getValue());
			}
		}
		return out;
	}

	private void needRun() {
		if (segments == null) {
			throw new IllegalStateException("no run yet: send run first");
		}
	}

	/** Segments number `from` up to (not including) `to`. */
	public Map<String, Object> segments(int from, int to) {
		needRun();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (int k = Math.max(from, 0); k < Math.min(to, segments.size()); k++) {
			TrailRun.Segment s = segments.get(k);
			Map<String, Object> m = map("segment", k, "firstItem", s.start, "lastItem", s.stop, "firstMeasure",
					items.notes.get(s.start).get(0).measure(), "lastMeasure", items.notes.get(s.stop).get(0).measure(),
					"end", s.end.toString(), "trails", s.trails);
			if (s.best != null) {
				m.put("lowestHeight", s.best.fbHeightRange().x);
				m.put("highestHeight", s.best.fbHeightRange().y);
				m.put("lifts", s.best.lifts);
				m.put("bariolations", s.best.countBariolations());
			}
			out.add(m);
		}
		return map("total", segments.size(), "list", out);
	}

	private int segmentOf(int item) {
		for (int k = 0; k < segments.size(); k++) {
			if (item >= segments.get(k).start && item <= segments.get(k).stop) {
				return k;
			}
		}
		return -1;
	}

	/** One item: its notes, printed fingerings, segment, and where the best-fitting trail puts each note. */
	public Map<String, Object> item(int index) {
		needRun();
		if (index < 0 || index >= items.chords.size()) {
			throw new IllegalArgumentException("item " + index + " is outside 0.." + (items.chords.size() - 1));
		}
		List<Map<String, Object>> notes = new ArrayList<Map<String, Object>>();
		for (JJNote n : items.notes.get(index)) {
			PrintedCheck.Result r = checks.get(n);
			Map<String, Object> m = map("abc", n.abcName(), "measure", n.measure(), "printedFingering", n.fingering());
			if (r != null && r.string >= 0) {
				m.put("string", stringName(r.string));
				m.put("height", r.height);
			}
			if (r != null && r.verdict != PrintedCheck.Verdict.NONE) {
				m.put("verdict", r.verdict.toString());
				m.put("detail", r.detail);
			}
			notes.add(m);
		}
		return map("item", index, "segment", segmentOf(index), "playable", !items.chords.get(index).impossible(), "notes", notes);
	}

	/** Notes whose printed fingering got this verdict (all printed notes if verdict is null). */
	public Map<String, Object> check(String verdict, int limit) {
		needRun();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Map.Entry<JJNote, PrintedCheck.Result> e : checks.entrySet()) {
			PrintedCheck.Result r = e.getValue();
			if (r.verdict == PrintedCheck.Verdict.NONE || (verdict != null && !r.verdict.toString().equals(verdict))) {
				continue;
			}
			if (out.size() < limit) {
				out.add(map("abc", e.getKey().abcName(), "measure", e.getKey().measure(), "verdict", r.verdict.toString(), "detail", r.detail));
			}
		}
		return map("counts", verdictCounts(), "list", out);
	}
}

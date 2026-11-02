package computingmusic.solve;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import computingmusic.FingerTrail;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.eval.NoteByNote;
import computingmusic.hand.FrameTest;
import computingmusic.jj.JJNote;

/**
 * One fingering for a whole piece.
 *
 * 1. The piece is cut into STRETCHES note by note (NoteByNote): each stretch is extended while at least one trail
 *    (a fingering of all its notes that the hand can hold at once) survives.  A set of notes the hand can hold
 *    is still holdable when notes are taken away, so cutting as late as possible gives the fewest stretches.
 * 2. Every stretch keeps all its surviving trails.  A stretch with ONE trail is forced: no choice.  Any other has
 *    options, and the options are chosen between by cost.
 * 3. The cost of a choice is within a stretch (finger lifts, bariolations, a frame beyond comfort) and between
 *    stretches (a change of hand position, see transition()).  The cheapest sequence of choices is found by dynamic
 *    programming (Viterbi) over the stretches.  A forward and a backward pass also tell, for each stretch, how many of its
 *    options lie on some cheapest path.
 *
 * The costs are CHOSEN parameters (Costs), indications only: moving the whole hand with its fingers in the same
 * relative places is cheap; rearranging the fingers is dearer; shifting and rearranging together dearer still,
 * and most when no height of the old frame is shared with the new.
 * Complexity: O(sum over consecutive stretches of (options x options) x cost of one transition (at most 4 heights)).
 */
public final class PieceSolver {
	public static final class Costs {
		public double lift = 0.15;                // each finger lift inside a stretch
		public double bariolation = 0.4;          // each pitch played in more than one way inside a stretch
		public double beyondComfort = 2.0;        // a stretch whose frame the comfortable limits do not allow
		public double shiftEasy = 0.3;            // per ln(1 + semitones) when the whole hand moves, fingers unchanged
		public double rearrange = 0.6;            // per finger whose place in the frame changes
		public double shiftAndRearrange = 1.5;    // multiplier on the rearranging when the hand also shifts
		public double noShare = 1.0;              // shifting while rearranging with no height kept
		/** Each stopped height both of two neighbouring stretches hold, which one gives a different finger than the other.  CHOSEN: 0.3, half a rearranged finger. */
		public double fingerRenumber = 0.3;
		/** A note held across the stretches whose finger changes number (the hand cannot move a finger that is down).  CHOSEN: 3.0, above a shift. */
		public double heldFingerRenumbered = 3.0;
		/** Per semitone of the lowest stopped height of a stretch: nearer the nut preferred (guitar: only a tie-break between equal positions). */
		public double highPosition = 0.0;
		/**
		 * A stretch with a stopped height above neckLimit (the cello's and bass's thumb position, not modelled) costs this much, so that a note the
		 * neck reaches on a higher string is not taken high up a lower one.  0 = off (violin, viola, guitar).  CHOSEN: 3.0, more than a frame beyond comfort.
		 */
		public double aboveNeck = 0.0;
		/**
		 * Each distinct stopped place (string, height) that sounds the pitch of an open string costs this much: the open string was there to be used (the
		 * cello's G2 D3 A3 of the Suite 1 prelude).  0 = off (violin, guitar).  CHOSEN: 0.25, about two finger lifts.
		 */
		public double stoppedAnOpenPitch = 0.0;
		public int[] openPitches = null;
		public int neckLimit = 0;

		/** The costs for an instrument: the guitar and the viola, cello and bass (InstrumentSpec.lowestFingersFirst) prefers low positions a little (CHOSEN 0.02 per fret, a seventh of a finger lift at fret 7). */
		public static Costs forInstrument(computingmusic.instrument.InstrumentSpec spec) {
			Costs c = new Costs();
			if (spec.lowestFingersFirst) {
				c.highPosition = 0.02;
			}
			if (spec != computingmusic.instrument.InstrumentSpec.VIOLIN && spec.chordOnNeighbouringStrings) {
				c.highPosition = 0.02;     // viola, cello, bass: a tie-break for the low position (CHOSEN, as the guitar's); the violin's results do not change
			}
			if (spec != computingmusic.instrument.InstrumentSpec.VIOLIN && spec.chordOnNeighbouringStrings) {
				c.stoppedAnOpenPitch = 0.25;
				c.openPitches = spec.openPitches;
			}
			if (spec.neckLimitHeight > 0) {
				c.neckLimit = spec.neckLimitHeight;
				c.aboveNeck = 3.0;
			}
			return c;
		}
	}

	/** The option chosen for one stretch. */
	public static final class Choice {
		public final NoteByNote.Stretch stretch;
		public final FingerTrail trail;           // null when no trail exists for the stretch
		public final int options;                 // trails the stretch had
		public final int onCheapestPath;          // options that lie on some cheapest path
		public final double within, transition;   // cost inside this stretch, and from the stretch before
		public final int[] frame;                 // distinct stopped heights of the chosen trail, ascending
		public int[] fingers;                     // the finger for each height of fingerHeights (null if the hand model gives none)
		public int[] fingerHeights;               // the frame's heights, plus the height a trill's upper finger needs on each trilled note
		public boolean fingersForced;             // the hand allows no other assignment
		public List<List<NoteByNote.Spot>> spots; // for each item of the stretch, the spot of each note

		Choice(NoteByNote.Stretch stretch, FingerTrail trail, int options, int onCheapestPath, double within, double transition, int[] frame) {
			this.stretch = stretch;
			this.trail = trail;
			this.options = options;
			this.onCheapestPath = onCheapestPath;
			this.within = within;
			this.transition = transition;
			this.frame = frame;
		}

		public boolean forced() {
			return options == 1;
		}

		/** The finger our model gives a note at this height: 0 for an open string, -1 if there is none. */
		public int fingerOf(int height) {
			if (height == 0) {
				return 0;
			}
			if (fingers != null) {
				for (int i = 0; i < fingerHeights.length; i++) {
					if (fingerHeights[i] == height) {
						return fingers[i];
					}
				}
			}
			return -1;
		}

		/** True when the hand allows only this finger there: an open string, or a frame with only one assignment. */
		public boolean fingerIsForced(int height) {
			return height == 0 || fingersForced;
		}
	}

	public static final class Solution {
		public final List<Choice> choices = new ArrayList<Choice>();
		public double total;
		public int itemsWithoutFingering, notesWithoutSpot, framesRejected;
		/** Items (indexes) of stretches where no trail and finger assignment gives a finger the score constraints (fing: in ABCD, a printed finger). */
		public final List<Integer> constraintsUnsatisfied = new ArrayList<Integer>();
		/** Stretches where a held finger could not keep its number from the stretch before (the constrained assignment was not allowed). */
		public int heldFingersRenumbered;
	}

	private final Costs costs;
	private final FrameTest comfortable, absolute;
	private List<NoteByNote.Stretch> stretchesNow;     // of the solve in progress, for link()
	private TrailRun.Items itemsNow;
	/** Cost of putting a stretch after one that placed its held notes differently: never chosen if any other pair exists. */
	static final double INCOMPATIBLE = 1e6;

	/** comfortable: the model at its comfortable limits (a cost); absolute: at its absolute limits (which fingers are possible). */
	public PieceSolver(Costs costs, FrameTest comfortable, FrameTest absolute) {
		this.costs = costs;
		this.comfortable = comfortable;
		this.absolute = absolute;
	}

	static int[] frameOf(FingerTrail t) {
		TreeSet<Integer> hs = new TreeSet<Integer>();
		for (Point p : t.getStoppedPoints()) {
			hs.add(p.y);
		}
		int[] out = new int[hs.size()];
		int i = 0;
		for (int h : hs) {
			out[i++] = h;
		}
		return out;
	}

	/** How many distinct stopped places of the trail sound the pitch of an open string.  Complexity: O(stopped places x strings). */
	int countStoppedOpenPitches(FingerTrail t) {
		java.util.HashSet<Long> seen = new java.util.HashSet<Long>();
		int n = 0;
		for (Point p : t.getStoppedPoints()) {
			int pitch = costs.openPitches[p.x] + p.y;
			if (seen.add(1000L * p.x + p.y)) {
				for (int open : costs.openPitches) {
					if (open == pitch) {
						n++;
						break;
					}
				}
			}
		}
		return n;
	}

	double within(FingerTrail t, int[] frame) {
		double c = costs.lift * t.lifts + costs.bariolation * t.countBariolations() + (frame.length > 0 ? costs.highPosition * frame[0] : 0);
		if (costs.stoppedAnOpenPitch > 0) {
			c += costs.stoppedAnOpenPitch * countStoppedOpenPitches(t);
		}
		if (costs.neckLimit > 0 && frame.length > 0 && frame[frame.length - 1] > costs.neckLimit) {
			c += costs.aboveNeck;
		}
		if (comfortable != null && !comfortable.feasible(t.frameHeights())) {   // the trills' upper fingers are held too
			c += costs.beyondComfort;
		}
		return c;
	}

	/**
	 * Is option a of stretch i (counting from 0) compatible with option b of stretch i-1?  A trail seeded by a held note of the stretch
	 * before fits only the options that placed it as the seed did.  INCOMPATIBLE when not, else 0.  Complexity: O(held).
	 */
	double link(List<List<FingerTrail>> opts, int i, int b, int a) {
		if (a >= opts.get(i).size() || b >= opts.get(i - 1).size()) {
			return 0;
		}
		FingerTrail next = opts.get(i).get(a);
		if (next.seedKey == null) {
			return 0;
		}
		int[][] held = itemsNow.chords.get(stretchesNow.get(i).start).heldBefore;
		String key = held == null ? null : java.util.Arrays.deepToString(FingerTrail.heldRows(opts.get(i - 1).get(b), held));
		return next.seedKey.equals(key) ? 0 : INCOMPATIBLE;
	}

	/** The cost of moving the hand from one frame to the next.  Complexity: O(frame size). */
	double transition(int[] a, int[] b) {
		if (a.length == 0 || b.length == 0) {
			return 0;                              // open strings only: no hand position
		}
		int shift = b[0] - a[0];
		int rearranged = 0, shared = 0;
		int[] na = normalise(a), nb = normalise(b);
		for (int h : na) {
			if (Arrays.binarySearch(nb, h) < 0) {
				rearranged++;
			}
		}
		for (int h : nb) {
			if (Arrays.binarySearch(na, h) < 0) {
				rearranged++;
			}
		}
		rearranged = (rearranged + 1) / 2;
		for (int h : a) {
			if (Arrays.binarySearch(b, h) >= 0) {
				shared++;
			}
		}
		double move = costs.shiftEasy * Math.log(1 + Math.abs(shift));
		if (rearranged == 0) {
			return move;                           // the whole hand moves, fingers in the same relative places
		}
		if (shift == 0) {
			return costs.rearrange * rearranged;   // fingers move, the hand stays
		}
		return move + costs.rearrange * rearranged * costs.shiftAndRearrange + (shared == 0 ? costs.noShare : 0);
	}

	private static int[] normalise(int[] f) {
		int[] out = new int[f.length];
		for (int i = 0; i < f.length; i++) {
			out[i] = f[i] - f[0];
		}
		return out;
	}

	/** One state of the search: a trail for the stretch and the finger assignment given to its frame (the fingers are part of the choice). */
	static final class State {
		final int trail;                      // index into the stretch's alive trails, -1 when it has none
		final FrameTest.Assignment asg;       // null when the hand gives none
		final int[] heights;                  // the heights the assignment is over (the frame, plus the trills' upper fingers)
		final double unary;                   // cost inside the stretch, with the soft cost of the fingers
		final List<List<NoteByNote.Spot>> spots;
		int[] frame;                          // the trail's distinct stopped heights
		FingerTrail ft;                       // the trail of the stretch this state stands for
		int trailIndex = -1;                  // its place among the stretch's alive trails

		State(int trail, FrameTest.Assignment asg, int[] heights, double unary, List<List<NoteByNote.Spot>> spots) {
			this.trail = trail;
			this.asg = asg;
			this.heights = heights;
			this.unary = unary;
			this.spots = spots;
		}

		int fingerAt(int height) {
			if (height == 0) {
				return 0;
			}
			if (asg != null) {
				for (int i = 0; i < heights.length; i++) {
					if (heights[i] == height) {
						return asg.fingers[i];
					}
				}
			}
			return -1;
		}
	}

	private static boolean hasFingerConstraint(TrailRun.Items items, int item) {
		for (JJNote jn : items.notes.get(item)) {
			if (jn.givenFinger().matches("[0-4]")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Can some finger assignment the hand allows to the trail's frame over items first..last give every constrained note its finger?
	 * True when the stretch constraints nothing.  O(items x notes + 15).
	 */
	private boolean constraintsCanHold(Instrument ins, TrailRun.Items items, FingerTrail t, int first, int last) {
		boolean any = false;
		for (int it = first; it <= last && !any; it++) {
			any = hasFingerConstraint(items, it);
		}
		if (!any) {
			return true;
		}
		try {
			NoteByNote.Stretch tmp = new NoteByNote.Stretch(first, last, NoteByNote.End.BREAK, java.util.Collections.singletonList(t));
			List<List<NoteByNote.Spot>> spots = NoteByNote.spots(ins, items, tmp, t);
			List<int[]> constraints = fingerConstraints(items, tmp, spots);
			int[] frame = frameOf(t);
			if (absolute == null) {
				return constraintsHold(constraints, frame, null);
			}
			List<FrameTest.Assignment> ranked = absolute.rank(frame, null, null, new TreeSet<Integer>());
			if (ranked.isEmpty()) {
				return constraintsHold(constraints, frame, null);
			}
			for (FrameTest.Assignment as : ranked) {
				if (constraintsHold(constraints, frame, as)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** Stretches (first item) where the finger constraints left no state; copied into the Solution. */
	private final List<Integer> constraintsFailedAt = new ArrayList<Integer>();

	/**
	 * The fingers the score's author fixes (the property "fingering_given", one number 0-4: fing: in ABCD; a printed finger is not one) for the notes this
	 * trail places: {height, finger}.  0 means an open string.  A note with no finger, or several, gives no constraint.  O(notes).
	 */
	private static List<int[]> fingerConstraints(TrailRun.Items items, NoteByNote.Stretch s, List<List<NoteByNote.Spot>> spots) {
		List<int[]> constraints = new ArrayList<int[]>();
		int itemNo = s.start;
		for (List<NoteByNote.Spot> row : spots) {
			List<JJNote> ns = items.notes.get(itemNo);
			for (int j = 0; j < row.size() && j < ns.size(); j++) {
				String f = ns.get(j).givenFinger();
				if (row.get(j) != null && f.matches("[0-4]")) {
					constraints.add(new int[] { row.get(j).height, Integer.parseInt(f) });
				}
			}
			itemNo++;
		}
		return constraints;
	}

	/**
	 * Does the assignment give every constrained note its finger?  An open string is finger 0, and only an open string is.  With no
	 * assignment (the hand gives none) only constraints that an open string settles can hold.  O(constraints x heights).
	 */
	private static boolean constraintsHold(List<int[]> constraints, int[] heights, FrameTest.Assignment as) {
		for (int[] p : constraints) {
			int h = p[0], want = p[1];
			if (h == 0 || want == 0) {
				if ((h == 0) != (want == 0)) {
					return false;
				}
				continue;
			}
			int got = -1;
			if (as != null) {
				for (int i = 0; i < heights.length; i++) {
					if (heights[i] == h) {
						got = as.fingers[i];
					}
				}
			}
			if (got != want) {
				return false;
			}
		}
		return true;
	}

	/** The states of stretch s: every trail with every finger assignment the hand allows, the soft costs in unary.  Complexity: O(trails x 15). */
	private List<State> statesOf(Instrument ins, TrailRun.Items items, NoteByNote.Stretch s, List<FingerTrail> alive, double[] unary, int[][] frames) throws Exception {
		List<State> out = new ArrayList<State>();
		if (alive.isEmpty()) {
			out.add(new State(-1, null, new int[0], 0, null));
			return out;
		}
		for (int a = 0; a < alive.size(); a++) {
			out.addAll(trailStates(ins, items, s, a, alive.get(a), frames[a], unary[a]));
		}
		if (out.isEmpty()) {      // every trail and assignment contradicts a finger constraint: the stretch has no fingering to offer
			for (int it = s.start; it <= s.stop; it++) {      // name the items that carry a constraint
				for (JJNote jn : items.notes.get(it)) {
					if (jn.givenFinger().matches("[0-4]")) {
						constraintsFailedAt.add(it);
						break;
					}
				}
			}
			out.add(new State(-1, null, new int[0], 0, null));
		}
		return out;
	}

	/** The states of one trail of stretch s: one for each finger assignment the hand allows to its frame (the soft costs of the fingers in unary). */
	private List<State> trailStates(Instrument ins, TrailRun.Items items, NoteByNote.Stretch s, int a, FingerTrail t, int[] frame, double within) throws Exception {
		List<State> out = new ArrayList<State>();
		List<List<NoteByNote.Spot>> spots = NoteByNote.spots(ins, items, s, t);
		// a trill is held by two fingers, the note's and the one above it: the height of the finger above (a tone
		// up the same string; JJ does not know whether the trill is a tone or a semitone) is part of the frame
		TreeSet<Integer> held = new TreeSet<Integer>();
		for (int h : frame) {
			held.add(h);
		}
		int itemNo = s.start;
		for (List<NoteByNote.Spot> row : spots) {
			List<JJNote> ns = items.notes.get(itemNo);
			for (int j = 0; j < row.size(); j++) {
				if (row.get(j) != null && ns.get(j).trill()) {
					held.add(row.get(j).height + 2);
				}
			}
			itemNo++;
		}
		int[] withTrills = new int[held.size()];
		int ti = 0;
		for (int h : held) {
			withTrills[ti++] = h;
		}
		// the heights of the stretch's chords (an item of two notes or more): the fourth finger there costs a little (soft, HandParams)
		java.util.Set<Integer> chordHeights = new TreeSet<Integer>();
		for (List<NoteByNote.Spot> row : spots) {
			int placed = 0;
			for (NoteByNote.Spot sp : row) {
				placed += sp != null ? 1 : 0;
			}
			for (NoteByNote.Spot sp : row) {
				if (sp != null && placed > 1 && sp.height > 0) {
					chordHeights.add(sp.height);
				}
			}
		}
		int[] heights = withTrills;
		List<FrameTest.Assignment> ranked = absolute == null ? new ArrayList<FrameTest.Assignment>() : absolute.rank(heights, null, null, chordHeights);
		if (ranked.isEmpty() && absolute != null) {        // the hand cannot also hold the trills' fingers: the frame's own
			heights = frame;
			ranked = absolute.rank(heights, null, null, chordHeights);
		}
		List<int[]> constraints = fingerConstraints(items, s, spots);      // {height, finger} for each note the score gives a finger
		if (ranked.isEmpty() && constraintsHold(constraints, heights, null)) {
			out.add(new State(a, null, heights, within, spots));
		}
		for (FrameTest.Assignment as : ranked) {
			if (constraintsHold(constraints, heights, as)) {
				out.add(new State(a, as, heights, within + as.soft, spots));
			}
		}
		for (State st : out) {
			st.frame = frame;
			st.ft = t;
			st.trailIndex = a;
		}
		return out;
	}

	/**
	 * The cost of going from state p of stretch i-1 to state q of stretch i: the move of the hand (transition), a seeded trail's fit (link), and the fingers.
	 * A height both frames stop that the two assignments give different fingers costs fingerRenumber; a note held across that changes finger,
	 * heldFingerRenumbered.  Complexity: O(frame size).
	 */
	double step(List<List<FingerTrail>> opts, List<int[][]> frames, int i, State p, State q) {
		if (p.trail < 0 || q.trail < 0) {
			return 0;
		}
		double c = transition(frames.get(i - 1)[p.trail], frames.get(i)[q.trail]) + link(opts, i, p.trail, q.trail);
		if (p.asg == null || q.asg == null) {
			return c;
		}
		FingerTrail t = opts.get(i).get(q.trail);
		java.util.Set<Integer> heldHeights = new java.util.HashSet<Integer>();
		for (int[] cf : t.carriedFingers()) {
			heldHeights.add(cf[1]);
		}
		for (int h : q.heights) {
			int before = p.fingerAt(h), now = q.fingerAt(h);
			if (before > 0 && now > 0 && before != now) {
				c += heldHeights.contains(h) ? costs.heldFingerRenumbered : costs.fingerRenumber;
			}
		}
		return c;
	}

	/** One fingering for the piece: each stretch is extended as far as a trail survives.  Complexity: see solveGreedy. */
	public Solution solve(Instrument ins, TrailRun.Items items) throws Exception {
		return solveGreedy(ins, items);
	}

	public Solution solveGreedy(Instrument ins, TrailRun.Items items) throws Exception {
		constraintsFailedAt.clear();
		// a trail that no finger assignment of the hand can give the fingers the score constraints is dead, as any impossible fingering is
		NoteByNote.Keep keep = new NoteByNote.Keep() {
			public boolean ok(FingerTrail t, int first, int last) {
				return constraintsCanHold(ins, items, t, first, last);
			}
		};
		List<NoteByNote.Stretch> st = NoteByNote.run(ins, items.chords, items.trilled, keep);
		for (NoteByNote.Stretch s0 : st) {          // an item no trail survives, though one would without the constraint: say so
			if (s0.alive.isEmpty() && s0.start == s0.stop && hasFingerConstraint(items, s0.start)
					&& !NoteByNote.start(ins, items.chords.get(s0.start), s0.start, NoteByNote.trilledAt(items.trilled, s0.start), null).isEmpty()) {
				constraintsFailedAt.add(s0.start);
			}
		}
		stretchesNow = st;
		itemsNow = items;
		int n = st.size();
		List<List<FingerTrail>> opts = new ArrayList<List<FingerTrail>>();
		List<int[][]> frames = new ArrayList<int[][]>();
		List<List<State>> states = new ArrayList<List<State>>();
		for (NoteByNote.Stretch s : st) {
			List<FingerTrail> alive = s.alive;
			opts.add(alive);
			int[][] fr = new int[alive.size()][];
			double[] u = new double[alive.size()];
			for (int k = 0; k < alive.size(); k++) {
				fr[k] = frameOf(alive.get(k));
				u[k] = within(alive.get(k), fr[k]);
			}
			frames.add(fr);
			states.add(statesOf(ins, items, s, alive, u, fr));
		}
		// forward pass: f[i][a] = cheapest cost of stretches 0..i ending with state a at i
		double[][] f = new double[n][];
		int[][] back = new int[n][];
		for (int i = 0; i < n; i++) {
			int m = states.get(i).size();
			f[i] = new double[m];
			back[i] = new int[m];
			for (int a = 0; a < m; a++) {
				double ua = states.get(i).get(a).unary;
				if (i == 0) {
					f[i][a] = ua;
					continue;
				}
				double best = Double.MAX_VALUE;
				int arg = 0;
				for (int b = 0; b < states.get(i - 1).size(); b++) {
					double v = f[i - 1][b] + step(opts, frames, i, states.get(i - 1).get(b), states.get(i).get(a));
					if (v < best - 1e-12) {
						best = v;
						arg = b;
					}
				}
				f[i][a] = best + ua;
				back[i][a] = arg;
			}
		}
		// backward pass: g[i][a] = cheapest cost of stretches i+1.. given state a at i
		double[][] g = new double[n][];
		for (int i = n - 1; i >= 0; i--) {
			int m = f[i].length;
			g[i] = new double[m];
			if (i == n - 1) {
				continue;
			}
			for (int a = 0; a < m; a++) {
				double best = Double.MAX_VALUE;
				for (int b = 0; b < f[i + 1].length; b++) {
					State qb = states.get(i + 1).get(b);
					best = Math.min(best, step(opts, frames, i + 1, states.get(i).get(a), qb) + qb.unary + g[i + 1][b]);
				}
				g[i][a] = best;
			}
		}
		Solution sol = new Solution();
		sol.constraintsUnsatisfied.addAll(constraintsFailedAt);
		int[] pick = new int[n];
		double best = Double.MAX_VALUE;
		for (int a = 0; a < f[n - 1].length; a++) {
			if (f[n - 1][a] < best - 1e-12) {
				best = f[n - 1][a];
				pick[n - 1] = a;
			}
		}
		for (int i = n - 1; i > 0; i--) {
			pick[i - 1] = back[i][pick[i]];
		}
		sol.total = best;
		for (int i = 0; i < n; i++) {
			NoteByNote.Stretch s = st.get(i);
			State chosen = states.get(i).get(pick[i]);
			boolean has = chosen.trail >= 0;
			int cheapest = 0;
			java.util.Set<Integer> onPath = new java.util.HashSet<Integer>();
			for (int a = 0; a < f[i].length; a++) {
				if (f[i][a] + g[i][a] <= best + 1e-9) {
					onPath.add(states.get(i).get(a).trail);
				}
			}
			cheapest = onPath.size();
			FingerTrail t = has ? opts.get(i).get(chosen.trail) : null;
			int[] fr = has ? frames.get(i)[chosen.trail] : new int[0];
			double tr = 0;
			if (i > 0 && has && states.get(i - 1).get(pick[i - 1]).trail >= 0) {
				tr = transition(frames.get(i - 1)[states.get(i - 1).get(pick[i - 1]).trail], fr);
			}
			Choice c = new Choice(s, t, opts.get(i).size(), cheapest, has ? chosen.unary - (chosen.asg == null ? 0 : chosen.asg.soft) : 0, tr, fr);
			if (has) {
				c.spots = chosen.spots;
				c.fingerHeights = chosen.heights;
				if (chosen.asg != null) {
					c.fingers = chosen.asg.fingers;
					c.fingersForced = chosen.asg.feasible == 1;
				}
				if (i > 0 && states.get(i - 1).get(pick[i - 1]).trail >= 0 && chosen.asg != null && !t.carriedFingers().isEmpty()) {
					State prev = states.get(i - 1).get(pick[i - 1]);
					for (int[] cf : t.carriedFingers()) {
						int before = prev.fingerAt(cf[1]), now = chosen.fingerAt(cf[1]);
						if (before > 0 && now > 0 && before != now) {
							sol.heldFingersRenumbered++;
						}
					}
				}
				for (List<NoteByNote.Spot> row : c.spots) {
					for (NoteByNote.Spot sp : row) {
						if (sp == null) {
							sol.notesWithoutSpot++;
						}
					}
				}
			} else {
				sol.itemsWithoutFingering += s.stop - s.start + 1;
			}
			sol.choices.add(c);
		}
		return sol;
	}
}

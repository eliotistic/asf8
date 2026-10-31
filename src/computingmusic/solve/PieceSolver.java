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
		/** Each stretch after the first when the solver chooses where the stretches end: a cut costs a little, so a cut that changes nothing is not made.  CHOSEN: 0.1. */
		public double cut = 0.1;
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
		/** Stretches where a held finger could not keep its number from the stretch before (the pinned assignment was not allowed). */
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
		if (ALIGNED) {
			return transitionAligned(a, b);
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

	/** EXPERIMENT (-Dsf.transition=aligned): the hand may stay where it is or move by any amount that lines a finger of the old frame up with one of the new; the cheapest. */
	static final boolean ALIGNED = "aligned".equals(System.getProperty("sf.transition"));

	double transitionAligned(int[] a, int[] b) {
		java.util.TreeSet<Integer> shifts = new java.util.TreeSet<Integer>();
		shifts.add(0);
		for (int x : a) {
			for (int y : b) {
				shifts.add(y - x);
			}
		}
		double best = Double.MAX_VALUE;
		for (int d : shifts) {
			int onlyA = 0, shared = 0;
			for (int h : a) {
				if (Arrays.binarySearch(b, h + d) >= 0) {
					shared++;
				} else {
					onlyA++;
				}
			}
			int onlyB = b.length - shared;
			int rearranged = (onlyA + onlyB + 1) / 2;
			double move = costs.shiftEasy * Math.log(1 + Math.abs(d));
			double c = rearranged == 0 ? move : d == 0 ? costs.rearrange * rearranged : move + costs.rearrange * rearranged * costs.shiftAndRearrange + (shared == 0 ? costs.noShare : 0);
			best = Math.min(best, c);
		}
		return best;
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
		if (ranked.isEmpty()) {
			out.add(new State(a, null, heights, within, spots));
		}
		for (FrameTest.Assignment as : ranked) {
			out.add(new State(a, as, heights, within + as.soft, spots));
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

	/**
	 * One fingering for the piece.  The stretches end where the solver chooses (solveWithCuts) unless the instrument keeps held notes on their fingers
	 * (the guitar) or -Dsf.cuts=greedy: then each stretch is extended as far as a trail survives (solveGreedy).
	 */
	public Solution solve(Instrument ins, TrailRun.Items items) throws Exception {
		if (ins.spec.heldNotesKeepFingers || "greedy".equals(System.getProperty("sf.cuts"))) {
			return solveGreedy(ins, items);
		}
		return solveWithCuts(ins, items);
	}

	public Solution solveGreedy(Instrument ins, TrailRun.Items items) throws Exception {
		List<NoteByNote.Stretch> st = NoteByNote.run(ins, items.chords, items.trilled);
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

	/** The best way to reach the end of one stretch in a given state: the cost so far, and the way it was reached. */
	static final class Node {
		final double cost;
		final State state;
		final NoteByNote.Stretch window;
		final Node prev;

		Node(double cost, State state, NoteByNote.Stretch window, Node prev) {
			this.cost = cost;
			this.state = state;
			this.window = window;
			this.prev = prev;
		}
	}

	private static String keyOf(State s) {
		return Arrays.toString(s.frame == null ? new int[0] : s.frame) + Arrays.toString(s.heights) + (s.asg == null ? "none" : Arrays.toString(s.asg.fingers));
	}

	/** Like step, between two states of consecutive stretches that need no link (no note is held across a cut).  Complexity: O(frame size). */
	double stepCut(State p, State q) {
		if (p.trail < 0 || q.trail < 0) {
			return 0;
		}
		double c = transition(p.frame, q.frame);
		if (p.asg != null && q.asg != null) {
			for (int h : q.heights) {
				int before = p.fingerAt(h), now = q.fingerAt(h);
				if (before > 0 && now > 0 && before != now) {
					c += costs.fingerRenumber;
				}
			}
		}
		return c;
	}

	/**
	 * Where each stretch ends is chosen too.  For every item s a stretch may START there and be extended as far as a trail survives; every length is a
	 * candidate (a hand position can be left before it has to be).  A candidate stretch has one state for each frame the trails give it (the cheapest
	 * trail of the frame stands for it) and each finger assignment of that frame.  best[e] keeps, for each state a candidate ending at item e can end in,
	 * the cheapest cost of the piece up to e and how it was reached; a candidate starting at s is reached from any state at s-1.  A cut costs Costs.cut.
	 * Not counted: how many options lie on a cheapest path (onCheapestPath is 1).
	 * Complexity: O(sum over candidates of (states x states at the item before)).
	 */
	public Solution solveWithCuts(Instrument ins, TrailRun.Items items) throws Exception {
		int n = items.chords.size();
		itemsNow = items;
		stretchesNow = null;
		List<java.util.Map<String, Node>> best = new ArrayList<java.util.Map<String, Node>>();
		for (int e = 0; e < n; e++) {
			best.add(new java.util.HashMap<String, Node>());
		}
		for (int s = 0; s < n; s++) {
			List<Node> preds = s == 0 ? null : new ArrayList<Node>(best.get(s - 1).values());
			List<FingerTrail> alive = NoteByNote.start(ins, items.chords.get(s), s, NoteByNote.trilledAt(items.trilled, s));
			if (alive.isEmpty()) {
				relax(best, preds, new NoteByNote.Stretch(s, s, NoteByNote.End.IMPOSSIBLE, alive), ins, items);
				continue;
			}
			int stop = s;
			while (true) {
				relax(best, preds, new NoteByNote.Stretch(s, stop, NoteByNote.End.END, alive), ins, items);
				if (stop + 1 >= n) {
					break;
				}
				List<FingerTrail> next = NoteByNote.extend(ins, alive, items.chords.get(stop + 1), NoteByNote.trilledAt(items.trilled, stop + 1));
				if (next.isEmpty() || next.size() > NoteByNote.CAP) {
					break;
				}
				alive = next;
				stop++;
			}
		}
		Node last = null;
		for (Node nd : best.get(n - 1).values()) {
			if (last == null || nd.cost < last.cost - 1e-12) {
				last = nd;
			}
		}
		List<Node> path = new ArrayList<Node>();
		for (Node nd = last; nd != null; nd = nd.prev) {
			path.add(0, nd);
		}
		Solution sol = new Solution();
		sol.total = last.cost;
		State before = null;
		for (Node nd : path) {
			State chosen = nd.state;
			NoteByNote.Stretch s = nd.window;
			boolean has = chosen.trail >= 0;
			int[] fr = has ? chosen.frame : new int[0];
			double tr = before != null && before.trail >= 0 && has ? transition(before.frame, fr) : 0;
			Choice c = new Choice(s, has ? chosen.ft : null, s.alive.size(), 1, has ? chosen.unary - (chosen.asg == null ? 0 : chosen.asg.soft) : 0, tr, fr);
			if (has) {
				c.spots = chosen.spots;
				c.fingerHeights = chosen.heights;
				if (chosen.asg != null) {
					c.fingers = chosen.asg.fingers;
					c.fingersForced = chosen.asg.feasible == 1;
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
			before = chosen;
		}
		return sol;
	}

	/** The candidate stretch w, reached from every state in preds (null at the start of the piece): record the best way of ending in each of its states. */
	private void relax(List<java.util.Map<String, Node>> best, List<Node> preds, NoteByNote.Stretch w, Instrument ins, TrailRun.Items items) throws Exception {
		List<State> states = new ArrayList<State>();
		if (w.alive.isEmpty()) {
			states.add(new State(-1, null, new int[0], 0, null));
		} else {
			java.util.Map<String, Integer> group = new java.util.LinkedHashMap<String, Integer>();
			java.util.Map<String, Double> groupCost = new java.util.HashMap<String, Double>();
			int[][] frames = new int[w.alive.size()][];
			for (int a = 0; a < w.alive.size(); a++) {
				frames[a] = frameOf(w.alive.get(a));
				String k = Arrays.toString(frames[a]);
				double u = within(w.alive.get(a), frames[a]);
				if (!groupCost.containsKey(k) || u < groupCost.get(k) - 1e-12) {   // the cheapest trail of a frame stands for the frame
					groupCost.put(k, u);
					group.put(k, a);
				}
			}
			for (java.util.Map.Entry<String, Integer> g : group.entrySet()) {
				int a = g.getValue();
				states.addAll(trailStates(ins, items, w, a, w.alive.get(a), frames[a], groupCost.get(g.getKey())));
			}
		}
		java.util.Map<String, Node> into = best.get(w.stop);
		for (State st : states) {
			double bestCost = Double.MAX_VALUE;
			Node from = null;
			if (preds == null) {
				bestCost = 0;
			} else {
				for (Node p : preds) {
					double v = p.cost + stepCut(p.state, st) + costs.cut;
					if (v < bestCost - 1e-12) {
						bestCost = v;
						from = p;
					}
				}
			}
			bestCost += st.unary;
			String key = keyOf(st);
			Node old = into.get(key);
			if (old == null || bestCost < old.cost - 1e-12) {
				into.put(key, new Node(bestCost, st, w, from));
			}
		}
	}
}

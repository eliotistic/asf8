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
	}

	private final Costs costs;
	private final FrameTest comfortable, absolute;

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

	double within(FingerTrail t, int[] frame) {
		double c = costs.lift * t.lifts + costs.bariolation * t.countBariolations();
		if (comfortable != null && !comfortable.feasible(frame)) {
			c += costs.beyondComfort;
		}
		return c;
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

	public Solution solve(Instrument ins, TrailRun.Items items) throws Exception {
		List<NoteByNote.Stretch> st = NoteByNote.run(ins, items.chords);
		int n = st.size();
		List<List<FingerTrail>> opts = new ArrayList<List<FingerTrail>>();
		List<int[][]> frames = new ArrayList<int[][]>();
		List<double[]> unary = new ArrayList<double[]>();
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
			unary.add(u);
		}
		// forward pass: f[i][a] = cheapest cost of stretches 0..i ending with option a at i
		double[][] f = new double[n][];
		int[][] back = new int[n][];
		for (int i = 0; i < n; i++) {
			int m = Math.max(1, opts.get(i).size());
			f[i] = new double[m];
			back[i] = new int[m];
			for (int a = 0; a < m; a++) {
				double ua = a < opts.get(i).size() ? unary.get(i)[a] : 0;
				if (i == 0) {
					f[i][a] = ua;
					continue;
				}
				int pm = Math.max(1, opts.get(i - 1).size());
				double best = Double.MAX_VALUE;
				int arg = 0;
				for (int b = 0; b < pm; b++) {
					double t = (a < opts.get(i).size() && b < opts.get(i - 1).size()) ? transition(frames.get(i - 1)[b], frames.get(i)[a]) : 0;
					double v = f[i - 1][b] + t;
					if (v < best - 1e-12) {
						best = v;
						arg = b;
					}
				}
				f[i][a] = best + ua;
				back[i][a] = arg;
			}
		}
		// backward pass: g[i][a] = cheapest cost of stretches i+1.. given option a at i
		double[][] g = new double[n][];
		for (int i = n - 1; i >= 0; i--) {
			int m = f[i].length;
			g[i] = new double[m];
			if (i == n - 1) {
				continue;
			}
			int nm = f[i + 1].length;
			for (int a = 0; a < m; a++) {
				double best = Double.MAX_VALUE;
				for (int b = 0; b < nm; b++) {
					double t = (a < opts.get(i).size() && b < opts.get(i + 1).size()) ? transition(frames.get(i)[a], frames.get(i + 1)[b]) : 0;
					double ub = b < opts.get(i + 1).size() ? unary.get(i + 1)[b] : 0;
					best = Math.min(best, t + ub + g[i + 1][b]);
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
			boolean has = !opts.get(i).isEmpty();
			int cheapest = 0;
			for (int a = 0; a < f[i].length; a++) {
				if (f[i][a] + g[i][a] <= best + 1e-9) {
					cheapest++;
				}
			}
			FingerTrail t = has ? opts.get(i).get(pick[i]) : null;
			int[] fr = has ? frames.get(i)[pick[i]] : new int[0];
			double tr = (i > 0 && has && !opts.get(i - 1).isEmpty()) ? transition(frames.get(i - 1)[pick[i - 1]], fr) : 0;
			Choice c = new Choice(s, t, opts.get(i).size(), cheapest, has ? unary.get(i)[pick[i]] : 0, tr, fr);
			if (has) {
				c.spots = NoteByNote.spots(ins, items, s, t);
				// a trill is held by two fingers, the note's and the one above it: the height of the finger above (a tone
				// up the same string; JJ does not know whether the trill is a tone or a semitone) is part of the frame
				java.util.TreeSet<Integer> held = new java.util.TreeSet<Integer>();
				for (int h : fr) {
					held.add(h);
				}
				int itemNo = s.start;
				for (List<NoteByNote.Spot> row : c.spots) {
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
				FrameTest.Assignment as = absolute == null ? null : absolute.assign(withTrills);
				c.fingerHeights = withTrills;
				if (as == null && absolute != null) {                  // the hand cannot also hold the trills' fingers: the frame's own
					as = absolute.assign(fr);
					c.fingerHeights = fr;
				}
				if (as != null) {
					c.fingers = as.fingers;
					c.fingersForced = as.forced();
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

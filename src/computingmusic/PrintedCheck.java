package computingmusic;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import computingmusic.jj.JJNote;

/**
 * Printed fingerings against the trails.
 *
 * The engine places every note on a string at a height (semitones above the open string) but never
 * assigns a finger number, so a printed number cannot be compared with a number of the engine's.  What can
 * be checked is whether the printed numbers are consistent with the places a trail gives the notes, under
 * two rules about a hand.  A segment usually has several surviving trails, so each is tried and the one that
 * agrees with the most printed notes is the one reported: the question is whether the printed fingering is
 * among the engine's possibilities, not whether it is the engine's first choice.
 *
 *   OPEN     a printed 0 is an open string (height 0), and a printed 1-4 is a stopped note (height above 0).
 *   ORDER    within one segment (one hand position) a higher finger number is not lower on the fingerboard
 *            than a lower one.  Fingers can reach one semitone out of the frame, so notes at different
 *            instants may break the order by SLACK semitones; notes sounding together may not.
 *
 * These are rules of this check, not of the engine.  They are meant to be argued with.
 *
 * Complexity: O(n^2) over the printed notes of one segment, summed over segments.
 */
public class PrintedCheck {
	/** Semitones by which a finger may stray from its place in the frame between different instants. */
	static final int SLACK = 1;

	public enum Verdict {
		/** the note has no printed fingering */
		NONE,
		/** the printed fingering is two numbers (a member's and the chord's), which this check does not read */
		TWO_PRINTED,
		/** the trail has no point for this note */
		NOT_PLACED,
		/** printed 0 and the trail plays it open */
		OPEN_AGREES,
		/** printed 0 but the trail stops the string */
		OPEN_DISAGREES,
		/** printed finger but the trail plays it open */
		STOPPED_DISAGREES,
		/** printed finger, stopped by the trail, and the order against the other printed fingers holds */
		CONSISTENT,
		/** printed finger, stopped, and the order against another printed finger is broken */
		ORDER_BROKEN
	}

	public static class Result {
		public final JJNote note;
		public final Verdict verdict;
		public final int string, height;     // where the trail has the note; -1 if not placed
		public final String detail;

		Result(JJNote note, Verdict verdict, int string, int height, String detail) {
			this.note = note;
			this.verdict = verdict;
			this.string = string;
			this.height = height;
			this.detail = detail;
		}
	}

	private static class Candidate {
		final TrailRun.Placed placed;
		final int item, finger;

		Candidate(TrailRun.Placed placed, int item, int finger) {
			this.placed = placed;
			this.item = item;
			this.finger = finger;
		}
	}

	/** Check every note of the piece.  The map is keyed by note identity.  Complexity: see the class comment. */
	public static Map<JJNote, Result> check(Instrument ins, TrailRun.Items items, List<TrailRun.Segment> segments) {
		Map<JJNote, Result> out = new IdentityHashMap<JJNote, Result>();
		for (TrailRun.Segment seg : segments) {
			if (seg.best == null) {
				continue;
			}
			Map<JJNote, Result> chosen = null;
			int chosenAgree = -1, chosenLifts = Integer.MAX_VALUE;
			for (FingerTrail trail : seg.alive) {
				Map<JJNote, Result> r = checkTrail(ins, items, seg, trail);
				int agree = 0;
				for (Result x : r.values()) {
					if (x.verdict == Verdict.OPEN_AGREES || x.verdict == Verdict.CONSISTENT) {
						agree++;
					}
				}
				if (agree > chosenAgree || (agree == chosenAgree && trail.lifts < chosenLifts)) {
					chosen = r;
					chosenAgree = agree;
					chosenLifts = trail.lifts;
				}
			}
			out.putAll(chosen);
		}
		return out;
	}

	/** The verdicts for one segment's notes against one trail. */
	static Map<JJNote, Result> checkTrail(Instrument ins, TrailRun.Items items, TrailRun.Segment seg, FingerTrail trail) {
		Map<JJNote, Result> out = new IdentityHashMap<JJNote, Result>();
		List<List<TrailRun.Placed>> rows = TrailRun.placements(ins, items, seg, trail);
		List<Candidate> printed = new ArrayList<Candidate>();
		for (int r = 0; r < rows.size(); r++) {
			for (JJNote n : items.notes.get(seg.start + r)) {
				String f = n.fingering();
				TrailRun.Placed placed = null;
				for (TrailRun.Placed p : rows.get(r)) {
					if (p.note == n) {
						placed = p;
					}
				}
				if (f.isEmpty()) {
					out.put(n, new Result(n, Verdict.NONE, placed == null ? -1 : placed.string,
							placed == null ? -1 : placed.height, ""));
				} else if (f.indexOf(',') >= 0) {
					out.put(n, new Result(n, Verdict.TWO_PRINTED, placed == null ? -1 : placed.string,
							placed == null ? -1 : placed.height, "printed " + f));
				} else if (placed == null) {
					out.put(n, new Result(n, Verdict.NOT_PLACED, -1, -1, "printed " + f));
				} else {
					printed.add(new Candidate(placed, seg.start + r, Integer.parseInt(f)));
				}
			}
		}
		for (Candidate c : printed) {
			out.put(c.placed.note, judge(c, printed));
		}
		return out;
	}

	private static Result judge(Candidate c, List<Candidate> all) {
		JJNote n = c.placed.note;
		int h = c.placed.height, s = c.placed.string;
		if (c.finger == 0) {
			return h == 0 ? new Result(n, Verdict.OPEN_AGREES, s, h, "printed 0, open string")
					: new Result(n, Verdict.OPEN_DISAGREES, s, h, "printed 0, but the trail stops string " + s + " at height " + h);
		}
		if (h == 0) {
			return new Result(n, Verdict.STOPPED_DISAGREES, s, h, "printed finger " + c.finger + ", but the trail plays it open");
		}
		for (Candidate d : all) {
			if (d == c || d.finger == 0 || d.placed.height == 0 || d.finger == c.finger) {
				continue;
			}
			int slack = d.item == c.item ? 0 : SLACK;
			boolean lower = c.finger < d.finger;
			int gap = lower ? d.placed.height - h : h - d.placed.height;   // how far the higher finger is above the lower
			if (gap < -slack) {
				return new Result(n, Verdict.ORDER_BROKEN, s, h, "printed finger " + c.finger + " at height " + h
						+ ", but finger " + d.finger + " is at height " + d.placed.height + " (note " + d.placed.note.abcName()
						+ ", measure " + d.placed.note.measure() + ")");
			}
		}
		return new Result(n, Verdict.CONSISTENT, s, h, "printed finger " + c.finger + " at height " + h + ": order holds");
	}
}

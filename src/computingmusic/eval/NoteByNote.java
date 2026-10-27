package computingmusic.eval;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import computingmusic.ChordFingering;
import computingmusic.FingerTrail;
import computingmusic.Instrument;
import computingmusic.TrailRun;
import computingmusic.jj.JJNote;

/**
 * Stretches found by extending ONE ITEM AT A TIME from the first item of the stretch: no group of items is ever
 * held as a chord.  Same extension step as AnchorSequencer.Anchor.extendbyCF (every fingering of the next item
 * on every live trail; kept if the trail accepts it, at most 4 stopped heights; if some survivors have no
 * bariolations keep only those), except that TopologyAnchor is not attached (it is not read by the step).
 *
 * Needs three non-public things of FingerTrail, reached by reflection and read-only in effect: the constructor
 * FingerTrail(Instrument,int,int), and the fields saite / interval / onOrder of FingerTrail.Trailpoint.
 *
 * Complexity per extension: O(live trails x fingerings of the next item); live trails capped at CAP.
 */
public class NoteByNote {
	public static final int CAP = 500;

	public enum End { BREAK, END, CAP, IMPOSSIBLE }

	public static class Stretch {
		public final int start, stop;       // item numbers, inclusive
		public final End end;
		public final List<FingerTrail> alive;

		Stretch(int start, int stop, End end, List<FingerTrail> alive) {
			this.start = start;
			this.stop = stop;
			this.end = end;
			this.alive = alive;
		}
	}

	/** Where one trail plays a note. */
	public static class Spot {
		public final int string, height;   // string 0 = G ... 3 = E; height = semitones above the open string

		Spot(int string, int height) {
			this.string = string;
			this.height = height;
		}
	}

	/** Complexity: O(items x extension cost). */
	public static List<Stretch> run(Instrument ins, List<ChordFingering> input) throws Exception {
		List<Stretch> out = new ArrayList<Stretch>();
		int i = 0;
		while (i < input.size()) {
			List<FingerTrail> alive = start(ins, input.get(i), i);
			if (alive.isEmpty()) {
				out.add(new Stretch(i, i, End.IMPOSSIBLE, alive));
				i++;
				continue;
			}
			int stop = i;
			End end = End.END;
			while (stop + 1 < input.size()) {
				List<FingerTrail> next = extend(ins, alive, input.get(stop + 1));
				if (next.isEmpty()) {
					end = End.BREAK;
					break;
				}
				if (next.size() > CAP) {
					end = End.CAP;
					break;
				}
				alive = next;
				stop++;
			}
			out.add(new Stretch(i, stop, end, alive));
			i = stop + 1;
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	static List<FingerTrail> start(Instrument ins, ChordFingering cf, int index) throws Exception {
		Constructor<FingerTrail> c = FingerTrail.class.getDeclaredConstructor(Instrument.class, int.class, int.class);
		c.setAccessible(true);
		List<FingerTrail> out = new ArrayList<FingerTrail>();
		for (Object f : cf.getFingerings()) {
			FingerTrail t = c.newInstance(ins, index, index).makeCopyWithNewFingering((ChordFingering.Fingering) f);
			if (t != null) {
				out.add(t);
			}
		}
		return out;
	}

	static List<FingerTrail> extend(Instrument ins, List<FingerTrail> alive, ChordFingering next) {
		List<FingerTrail> kept = new ArrayList<FingerTrail>();
		for (ChordFingering.Fingering f : next.getFingerings()) {
			for (FingerTrail t : alive) {
				FingerTrail x = t.makeCopyWithNewFingering(f);
				if (x != null && x.stoppedHeights() <= 4) {
					kept.add(x);
				}
			}
		}
		List<FingerTrail> smooth = new ArrayList<FingerTrail>();
		for (FingerTrail t : kept) {
			if (t.countBariolations() == 0) {
				smooth.add(t);
			}
		}
		return smooth.isEmpty() ? kept : smooth;
	}

	/**
	 * The spot of each note of each item of the stretch under one trail (null where no point of the step has the
	 * note's pitch).  Complexity: O(items x points).
	 */
	public static List<List<Spot>> spots(Instrument ins, TrailRun.Items items, Stretch s, FingerTrail trail) throws Exception {
		Class<?> tpc = Class.forName("computingmusic.FingerTrail$Trailpoint");
		Field fs = tpc.getDeclaredField("saite"), fi = tpc.getDeclaredField("interval"), fo = tpc.getDeclaredField("onOrder");
		fs.setAccessible(true);
		fi.setAccessible(true);
		fo.setAccessible(true);
		List<List<Spot>> out = new ArrayList<List<Spot>>();
		for (int item = s.start; item <= s.stop; item++) {
			List<Spot> row = new ArrayList<Spot>();
			for (JJNote n : items.notes.get(item)) {
				Spot found = null;
				for (Object tp : trail.getTrailPoints()) {
					int saite = fs.getInt(tp), interval = fi.getInt(tp);
					if (((Set<?>) fo.get(tp)).contains(item - s.start) && ins.openStringPitch(saite) + interval == n.pitch) {
						found = new Spot(saite, interval);
						break;
					}
				}
				row.add(found);
			}
			out.add(row);
		}
		return out;
	}
}

package computingmusic.jj;

import java.util.ArrayList;
import java.util.List;

/** A note of the data paired with the same note as Verovio drew it. */
public class Link {
	public final JJNote note;
	public final NotationData.Drawn drawn;

	Link(JJNote note, NotationData.Drawn drawn) {
		this.note = note;
		this.drawn = drawn;
	}

	/**
	 * Pair the data's notes with the drawn ones.  Both lists are put in the same order (onset, then pitch) and
	 * matched one to one; every pair must agree on pitch and onset or the pairing is refused, naming the first
	 * pair that does not.  Complexity: O(n log n).
	 */
	/**
	 * How far, in quarters, a drawn note's onset may lie from the data's.  Verovio's timemap gives a grace note
	 * played time and delays the main note after it (about 0.06 of a quarter in Paganini's Caprice 3), whereas the
	 * data is exact.  A whole 32nd is 0.125, so a note off by a place in the order is still refused.
	 */
	static final double ONSET_TOLERANCE = 0.1;

	public static List<Link> pair(JJScore score, NotationData notation) {
		List<JJNote> data = score.byOnset();
		List<NotationData.Drawn> drawnNotes = new ArrayList<NotationData.Drawn>();
		for (NotationData.Drawn d : notation.notes) {
			if (!d.grace) {
				drawnNotes.add(d);          // grace notes are drawn but are not in JJ's data
			}
		}
		if (data.size() != drawnNotes.size()) {
			throw new IllegalStateException("JJ has " + data.size() + " notes, Verovio drew " + drawnNotes.size()
					+ " (" + (notation.notes.size() - drawnNotes.size()) + " more are grace notes)");
		}
		List<Link> links = new ArrayList<Link>(data.size());
		for (int i = 0; i < data.size(); i++) {
			JJNote n = data.get(i);
			NotationData.Drawn d = drawnNotes.get(i);
			if (n.pitch != d.pitch || Math.abs(n.quarters() - d.onset) > ONSET_TOLERANCE) {
				throw new IllegalStateException(String.format(
						"note %d: JJ has %s at %.4f quarters, Verovio drew pitch %d at %.4f",
						i, n.abcName(), n.quarters(), d.pitch, d.onset));
			}
			links.add(new Link(n, d));
		}
		return links;
	}
}

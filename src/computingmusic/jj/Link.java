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
	public static List<Link> pair(JJScore score, NotationData notation) {
		List<JJNote> data = score.byOnset();
		if (data.size() != notation.notes.size()) {
			throw new IllegalStateException("JJ has " + data.size() + " notes, Verovio drew " + notation.notes.size());
		}
		List<Link> links = new ArrayList<Link>(data.size());
		for (int i = 0; i < data.size(); i++) {
			JJNote n = data.get(i);
			NotationData.Drawn d = notation.notes.get(i);
			if (n.pitch != d.pitch || Math.abs(n.quarters() - d.onset) > 1e-3) {
				throw new IllegalStateException(String.format(
						"note %d: JJ has %s at %.4f quarters, Verovio drew pitch %d at %.4f",
						i, n.abcName(), n.quarters(), d.pitch, d.onset));
			}
			links.add(new Link(n, d));
		}
		return links;
	}
}

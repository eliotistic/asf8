package computingmusic.jj;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** The notes of a score as JJ dumped them (jot_to_json), with all their properties. */
public class JJScore {
	public final List<JJNote> notes = new ArrayList<JJNote>();

	/** Read a jot_to_json dump.  Complexity: O(size of the file). */
	public static JJScore read(File dump) throws IOException {
		String text = new String(Files.readAllBytes(dump.toPath()), StandardCharsets.UTF_8);
		JJScore score = new JJScore();
		score.collect(Json.parse(text));
		if (score.notes.isEmpty()) {
			throw new IOException("no notes (P objects) in " + dump);
		}
		return score;
	}

	private void collect(Object node) {
		if (node instanceof Map) {
			Map<String, Object> m = Json.object(node);
			if ("P".equals(m.get("_type"))) {
				notes.add(new JJNote(m));
				return;
			}
			for (Object v : m.values()) {
				collect(v);
			}
		} else if (node instanceof List) {
			for (Object v : Json.array(node)) {
				collect(v);
			}
		}
	}

	/** The notes in time order, then pitch, then uid.  Complexity: O(n log n). */
	public List<JJNote> byOnset() {
		List<JJNote> sorted = new ArrayList<JJNote>(notes);
		Collections.sort(sorted, new Comparator<JJNote>() {
			public int compare(JJNote a, JJNote b) {
				int c = a.compareOnset(b);
				if (c == 0) {
					c = Integer.compare(a.pitch, b.pitch);
				}
				return c != 0 ? c : Integer.compare(a.uid, b.uid);
			}
		});
		return sorted;
	}
}

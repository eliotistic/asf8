package computingmusic.jj;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** What tools/verovio/render_pages.py wrote: the page images and a box for every drawn note. */
public class NotationData {
	public static class Page {
		public final BufferedImage image;
		public Page(BufferedImage image) {
			this.image = image;
		}
	}

	/** A drawn note: its box in the page image, and Verovio's own onset (quarters) and pitch for it. */
	public static class Drawn {
		public final String id;
		public final int page;
		public final double x, y, w, h;
		public final int pitch;
		public final double onset;

		Drawn(Map<String, Object> m) {
			id = String.valueOf(m.get("id"));
			page = ((Number) m.get("page")).intValue() - 1;
			x = ((Number) m.get("x")).doubleValue();
			y = ((Number) m.get("y")).doubleValue();
			w = ((Number) m.get("w")).doubleValue();
			h = ((Number) m.get("h")).doubleValue();
			pitch = ((Number) m.get("pitch")).intValue();
			onset = ((Number) m.get("on")).doubleValue();
		}
	}

	public final List<Page> pages = new ArrayList<Page>();
	public final List<Drawn> notes = new ArrayList<Drawn>();   // sorted by onset, then pitch, as written

	public static NotationData read(File dir) throws IOException {
		String text = new String(Files.readAllBytes(new File(dir, "notation.json").toPath()), StandardCharsets.UTF_8);
		Map<String, Object> root = Json.object(Json.parse(text));
		NotationData d = new NotationData();
		for (Object p : Json.array(root.get("pages"))) {
			String png = String.valueOf(Json.object(p).get("png"));
			d.pages.add(new Page(ImageIO.read(new File(dir, png))));
		}
		for (Object n : Json.array(root.get("notes"))) {
			d.notes.add(new Drawn(Json.object(n)));
		}
		return d;
	}
}

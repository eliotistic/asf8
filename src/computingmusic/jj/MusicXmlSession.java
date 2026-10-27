package computingmusic.jj;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A score loaded for the app: JJ reads the file (MusicXML, MEI, kern ...) and dumps every note with all its
 * properties; Verovio, through tools/verovio/render_pages.py, draws it.  The two are paired note by note.
 *
 * Both steps run at once; each is a separate process.  Complexity: dominated by JJ's start-up (about 15 s)
 * and Verovio's rendering.
 */
public class MusicXmlSession {
	public final File file;
	public final JJScore score;
	public final NotationData notation;
	public final List<Link> links;
	public final String jjReport;

	private MusicXmlSession(File file, JJScore score, NotationData notation, List<Link> links, String jjReport) {
		this.file = file;
		this.score = score;
		this.notation = notation;
		this.links = links;
		this.jjReport = jjReport;
	}

	static File home(String property, String fallbackUnderHome) {
		String v = System.getProperty(property);
		return v != null ? new File(v) : new File(System.getProperty("user.home"), fallbackUnderHome);
	}

	/** Just the notes: JJ reads the file and dumps every note with its properties; nothing is drawn. */
	public static JJScore loadNotes(File file) throws Exception {
		File cache = cacheFor(file);
		return JJScore.read(dumpThroughJJ(file, cache, new StringBuilder()));
	}

	static File cacheFor(File file) throws IOException {
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		File cache = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base);
		if (!cache.isDirectory() && !cache.mkdirs()) {
			throw new IOException("cannot create " + cache);
		}
		return cache;
	}

	/** Run JJ on the file and return the dump it wrote.  `report` collects what JJ said. */
	static File dumpThroughJJ(File file, File cache, StringBuilder report) throws Exception {
		File jjHome = home("jj.home", "shape");
		File appHome = home("fingering.home", "shape/fingering");
		File dump = new File(cache, "dump.json");
		dump.delete();
		try (JJClient jj = new JJClient(new File(jjHome, "bin/jj_cmd"), new File(appHome, "tools/jj/nojump"), 300)) {
			report.append(jj.eval("load_path(\"" + new File(jjHome, "jjviz/jjviz_init.jj").getPath() + "\")"));
			report.append(jj.eval("_SQ = mei_score(\"" + file.getAbsolutePath() + "\")"));
			report.append(jj.eval("SQX = Sq(_SQ)"));
			report.append(jj.eval("jot_to_json(:path=\"" + cache.getPath() + "\", :name=\"dump\", jot(SQX))"));
		}
		if (!dump.isFile()) {
			throw new IOException("JJ did not write " + dump + "; it said:\n" + report);
		}
		return dump;
	}

	public static MusicXmlSession open(final File file) throws Exception {
		final File jjHome = home("jj.home", "shape");
		final File appHome = home("fingering.home", "shape/fingering");
		String base = file.getName().replaceAll("\\.[^.]*$", "");
		final File cache = new File(System.getProperty("user.home"), ".string_fingering/cache/" + base);
		if (!cache.isDirectory() && !cache.mkdirs()) {
			throw new IOException("cannot create " + cache);
		}
		final File notationDir = new File(cache, "notation");

		ExecutorService pool = Executors.newSingleThreadExecutor();
		try {
			Future<String> drawing = pool.submit(new Callable<String>() {
				public String call() throws Exception {
					return run(new ProcessBuilder("python3", "-I",
							new File(appHome, "tools/verovio/render_pages.py").getPath(),
							file.getAbsolutePath(), notationDir.getPath()));
				}
			});
			File dump = new File(cache, "dump.json");
			dump.delete();
			StringBuilder report = new StringBuilder();
			try (JJClient jj = new JJClient(new File(jjHome, "bin/jj_cmd"),
					new File(appHome, "tools/jj/nojump"), 300)) {
				report.append(jj.eval("load_path(\"" + new File(jjHome, "jjviz/jjviz_init.jj").getPath() + "\")"));
				report.append(jj.eval("_SQ = mei_score(\"" + file.getAbsolutePath() + "\")"));
				report.append(jj.eval("SQX = Sq(_SQ)"));
				report.append(jj.eval("jot_to_json(:path=\"" + cache.getPath() + "\", :name=\"dump\", jot(SQX))"));
			}
			if (!dump.isFile()) {
				throw new IOException("JJ did not write " + dump + "; it said:\n" + report);
			}
			drawing.get();
			JJScore score = JJScore.read(dump);
			NotationData notation = NotationData.read(notationDir);
			return new MusicXmlSession(file, score, notation, Link.pair(score, notation), report.toString());
		} finally {
			pool.shutdown();
		}
	}

	/** Load a score and print what arrived: a check that needs no window.  Usage: MusicXmlSession FILE */
	public static void main(String[] args) throws Exception {
		MusicXmlSession s = open(new File(args[0]));
		int chords = 0, fingered = 0, withSlur = 0;
		java.util.Set<Integer> serials = new java.util.HashSet<Integer>();
		for (JJNote n : s.score.notes) {
			if (n.chordSerial() >= 0 && serials.add(n.chordSerial())) {
				chords++;
			}
			if (!n.fingering().isEmpty()) {
				fingered++;
			}
			if (n.props.containsKey("slur_serials")) {
				withSlur++;
			}
		}
		System.out.println("notes " + s.score.notes.size() + ", chords " + chords + ", notes with a printed fingering "
				+ fingered + ", under a slur " + withSlur);
		System.out.println("pages " + s.notation.pages.size() + ", drawn notes " + s.notation.notes.size()
				+ ", paired " + s.links.size());
	}

	private static String run(ProcessBuilder pb) throws IOException, InterruptedException {
		pb.redirectErrorStream(true);
		Process p = pb.start();
		StringBuilder out = new StringBuilder();
		byte[] buf = new byte[4096];
		for (int n; (n = p.getInputStream().read(buf)) > 0; ) {
			out.append(new String(buf, 0, n, "UTF-8"));
		}
		if (p.waitFor() != 0) {
			throw new IOException(pb.command() + " failed:\n" + out);
		}
		return out.toString();
	}
}

package computingmusic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The last scores opened, newest first, kept in ~/.string_fingering/recent.txt (one absolute path per line).
 * Never throws: an unreadable or unwritable file just means an empty or unsaved list.
 * Complexity: O(MAX) per call plus the file I/O.
 */
public class RecentFiles {
	public static final int MAX = 10;
	private final File store;
	private final List<File> files = new ArrayList<File>();

	public RecentFiles() {
		this(System.getProperty("sf.recent.file") != null ? new File(System.getProperty("sf.recent.file"))
				: new File(System.getProperty("user.home"), ".string_fingering/recent.txt"));
	}

	public RecentFiles(File store) {
		this.store = store;
		load();
	}

	private void load() {
		files.clear();
		try {
			for (String line : Files.readAllLines(store.toPath(), StandardCharsets.UTF_8)) {
				line = line.trim();
				if (!line.isEmpty() && files.size() < MAX) {
					File f = new File(line);
					if (!files.contains(f)) {
						files.add(f);
					}
				}
			}
		} catch (Exception e) {
			// missing or unreadable: start empty
		}
	}

	/** The list now (reread from the file, so several windows share it), newest first. */
	public synchronized List<File> list() {
		load();
		return new ArrayList<File>(files);
	}

	/** `file` was opened: put it first, drop an older entry for it, keep MAX. */
	public synchronized void add(File file) {
		load();
		File f = file.getAbsoluteFile();
		files.remove(f);
		files.add(0, f);
		while (files.size() > MAX) {
			files.remove(files.size() - 1);
		}
		save();
	}

	public synchronized void clear() {
		files.clear();
		save();
	}

	private void save() {
		try {
			File dir = store.getAbsoluteFile().getParentFile();
			if (dir != null) {
				dir.mkdirs();
			}
			StringBuilder sb = new StringBuilder();
			for (File f : files) {
				sb.append(f.getPath()).append('\n');
			}
			Files.write(store.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException | RuntimeException e) {
			System.err.println("could not save the recent files: " + e.getMessage());
		}
	}
}

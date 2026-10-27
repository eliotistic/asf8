package computingmusic.jj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A running JJ process (bin/jj_cmd --stdio), spoken to over its pipe.
 *
 * Protocol: one line of JJ is sent; JJ answers with its output and a line __JJ_DONE__ (it also prints one
 * when it starts).  Lines starting __JJVIZ_ are the viewer's housekeeping and are dropped.
 *
 * JJ calls emacsclient when a script fails to parse, which would move the user's editor, so the
 * process is started with a PATH whose first entry holds a do-nothing emacsclient (tools/jj/nojump).
 *
 * Complexity: O(output lines) per call.
 */
public class JJClient implements AutoCloseable {
	static final String DONE = "__JJ_DONE__";
	private static final String END = "\u0000end of output";

	private final Process process;
	private final BufferedWriter toJJ;
	private final BlockingQueue<String> lines = new LinkedBlockingQueue<String>();
	private final long timeoutSeconds;

	public JJClient(File jjCmd, File shimDir, long timeoutSeconds) throws IOException {
		this.timeoutSeconds = timeoutSeconds;
		ProcessBuilder pb = new ProcessBuilder(jjCmd.getPath(), "--stdio");
		pb.redirectErrorStream(true);
		pb.environment().put("PATH", shimDir.getAbsolutePath() + File.pathSeparator + System.getenv("PATH"));
		process = pb.start();
		toJJ = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));
		final BufferedReader fromJJ = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
		Thread reader = new Thread("jj-output") {
			public void run() {
				try {
					String line;
					while ((line = fromJJ.readLine()) != null) {
						lines.add(line);
					}
				} catch (IOException e) {
					// the process went away; the END marker below tells the waiting caller
				}
				lines.add(END);
			}
		};
		reader.setDaemon(true);
		reader.start();
		collect(); // JJ's start-up banner ends with its own __JJ_DONE__
	}

	/** Send one line of JJ and return what JJ printed for it.  Complexity: O(output lines). */
	public String eval(String jj) throws IOException {
		if (jj.indexOf('\n') >= 0) {
			throw new IllegalArgumentException("JJ is sent one line at a time");
		}
		toJJ.write(jj);
		toJJ.write("\n");
		toJJ.flush();
		return collect();
	}

	private String collect() throws IOException {
		StringBuilder out = new StringBuilder();
		while (true) {
			String line;
			try {
				line = lines.poll(timeoutSeconds, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new IOException("interrupted while waiting for JJ");
			}
			if (line == null) {
				throw new IOException("JJ did not answer within " + timeoutSeconds + " s");
			}
			if (line.equals(END)) {
				throw new IOException("JJ ended; output so far:\n" + out);
			}
			if (line.equals(DONE)) {
				return out.toString();
			}
			if (!line.startsWith("__JJVIZ_")) {
				out.append(line).append('\n');
			}
		}
	}

	@Override
	public void close() {
		process.destroy();
	}
}

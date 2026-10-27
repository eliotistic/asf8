package computingmusic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import computingmusic.jj.Json;

/**
 * String Fingering with no window, driven by lines of JSON on standard input; one reply line for each request.
 *
 *   {"id": 1, "cmd": "load", "path": "/path/score.musicxml"}   ->  {"id": 1, "ok": true, ...}
 *
 * Commands: ping, load, constraints, set_constraints, chord, run, segments, item, check, quit.
 * Pitches are in ABC notation.  An error is {"ok": false, "error": "..."}; the server keeps running.
 * Start it from the project folder (the constraints file is read from the working directory):
 *
 *   java -cp build/classes:$ABCMAINE computingmusic.SFServer
 *
 * Complexity: that of the command.
 */
public class SFServer {
	private final SFModel model = new SFModel();

	/** Handle one request.  Complexity: that of the command. */
	public Map<String, Object> handle(Map<String, Object> request) {
		Map<String, Object> reply = new LinkedHashMap<String, Object>();
		if (request.containsKey("id")) {
			reply.put("id", request.get("id"));
		}
		try {
			Object cmd = request.get("cmd");
			if (cmd == null) {
				throw new IllegalArgumentException("a request needs a \"cmd\"");
			}
			Map<String, Object> result = dispatch(String.valueOf(cmd), request);
			reply.put("ok", true);
			reply.putAll(result);
		} catch (Exception e) {
			reply.put("ok", false);
			reply.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		return reply;
	}

	private static int integer(Map<String, Object> r, String key, int fallback) {
		Object v = r.get(key);
		return v == null ? fallback : ((Number) v).intValue();
	}

	private Map<String, Object> dispatch(String cmd, Map<String, Object> r) throws Exception {
		if (cmd.equals("ping")) {
			return new LinkedHashMap<String, Object>();
		} else if (cmd.equals("load")) {
			return model.load(String.valueOf(r.get("path")));
		} else if (cmd.equals("constraints")) {
			return model.constraints();
		} else if (cmd.equals("set_constraints")) {
			return model.setConstraints(r);
		} else if (cmd.equals("chord")) {
			List<Object> pitches = Json.array(r.get("pitches"));
			return model.chord(pitches, integer(r, "limit", 20));
		} else if (cmd.equals("run")) {
			return model.run();
		} else if (cmd.equals("segments")) {
			return model.segments(integer(r, "from", 0), integer(r, "to", 20));
		} else if (cmd.equals("item")) {
			return model.item(integer(r, "index", 0));
		} else if (cmd.equals("check")) {
			return model.check(r.get("verdict") == null ? null : String.valueOf(r.get("verdict")), integer(r, "limit", 50));
		}
		throw new IllegalArgumentException("unknown command '" + cmd + "'");
	}

	/** Read requests from `in` until it ends or a quit arrives; reply on `out`. */
	public void serve(BufferedReader in, PrintStream out) throws IOException {
		String line;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}
			Map<String, Object> reply;
			boolean quit = false;
			try {
				Map<String, Object> request = Json.object(Json.parse(line));
				quit = "quit".equals(request.get("cmd"));
				reply = quit ? okReply(request) : handle(request);
			} catch (Exception e) {
				reply = new LinkedHashMap<String, Object>();
				reply.put("ok", false);
				reply.put("error", "not a JSON request: " + e.getMessage());
			}
			out.println(Json.write(reply));
			out.flush();
			if (quit) {
				return;
			}
		}
	}

	private static Map<String, Object> okReply(Map<String, Object> request) {
		Map<String, Object> reply = new LinkedHashMap<String, Object>();
		if (request.containsKey("id")) {
			reply.put("id", request.get("id"));
		}
		reply.put("ok", true);
		return reply;
	}

	public static void main(String[] args) throws IOException {
		// Anything else the engine prints must not reach the protocol stream: send it to standard error.
		PrintStream protocol = System.out;
		System.setOut(System.err);
		new SFServer().serve(new BufferedReader(new InputStreamReader(System.in, "UTF-8")), protocol);
	}
}

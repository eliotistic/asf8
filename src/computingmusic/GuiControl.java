package computingmusic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import computingmusic.jj.Json;

/**
 * Lets another program drive the running String Fingering window.
 *
 * A server on the loopback interface (never reachable from other machines) takes the same JSON lines as
 * SFServer.  The model commands (chord, run, set_constraints ...) go straight to an SFServer; the window
 * commands, below, run on the Swing thread:
 *
 *   gui_state                      title, size, the piece, the master index, how many items
 *   gui_load {path}                what dropping a score file on the window does (ABC, MusicXML, MEI, kern);
 *                                  gui_load_abc is the same
 *   gui_drop {path, as}            drop a file on the window's real drop handler, as a "files" list (default) or as
 *                                  "uri-list" text; it is read off the Swing thread: poll gui_state "loading"
 *   gui_recent                     File > Open recent: the paths, newest first
 *   gui_start | gui_next | gui_prev | gui_jump_next        the arrow buttons
 *   gui_jump {index}               go to item `index`
 *   gui_extend | gui_shrink        the Extend and Shrink buttons
 *   gui_click {item}               what a click on that note does: show the fingerboards for its marked stretch
 *   gui_unique {on: true|false}    mark (or unmark) the stretches with only one fingering; replies with them
 *   gui_zoom {factor, x, y}        Verovio view: zoom to `factor` times the fit-to-width scale (clamped 0.25..4), the
 *                                  point x,y of the view staying put; {reset: true} refits (as Ctrl+0); replies
 *                                  with zoom, zoomRelative, userZoomed, prefWidth/prefHeight, viewX/viewY
 *   gui_wheel {rotation, x, y, shift, ctrl}   a wheel event on the view (rotation < 0 = up; shift default true)
 *   gui_note_point {item}          where the item's first note is on the view, and what a click there hits
 *   gui_item_at {x, y}             the item a click at that view point would choose, or -1
 *   gui_trail                      the trail under the red box: its items, and for each note the string and
 *                                  height the head trail puts it on, with the heights used and the hand span
 *   gui_menus                      the menus, with their items
 *   gui_menu {path: "File/Open"}   click a menu item (reported at once; a dialog it opens is not waited for)
 *   gui_screenshot {path}          the window's contents as a PNG, painted from the components, not copied
 *                                  from the screen
 *
 * The port is written to ~/.string_fingering/control.port, or to the file named by -Dsf.control.file=...
 * (so that two windows do not share one).  Start the window with --control.
 *
 * Complexity: that of the command.
 */
public class GuiControl {
	private final BigGui gui;
	private final SFServer model = new SFServer();

	private GuiControl(BigGui gui) {
		this.gui = gui;
	}

	/** Start listening; returns the port. */
	public static int start(BigGui gui) throws IOException {
		final GuiControl control = new GuiControl(gui);
		final ServerSocket server = new ServerSocket(0, 4, InetAddress.getLoopbackAddress());
		File portFile = new File(System.getProperty("sf.control.file",
				System.getProperty("user.home") + "/.string_fingering/control.port"));
		portFile.getParentFile().mkdirs();
		Files.write(portFile.toPath(), String.valueOf(server.getLocalPort()).getBytes(StandardCharsets.UTF_8));
		portFile.deleteOnExit();
		Thread t = new Thread("sf-control") {
			public void run() {
				while (true) {
					try {
						Socket s = server.accept();
						control.serve(s);
					} catch (IOException e) {
						return;
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
		return server.getLocalPort();
	}

	private void serve(Socket socket) throws IOException {
		try (Socket s = socket;
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
				PrintStream out = new PrintStream(s.getOutputStream(), true, "UTF-8")) {
			String line;
			while ((line = in.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}
				Map<String, Object> reply;
				boolean quit = false;
				try {
					Map<String, Object> request = Json.object(Json.parse(line));
					quit = "quit".equals(request.get("cmd"));
					reply = handle(request);
				} catch (Exception e) {
					reply = new LinkedHashMap<String, Object>();
					reply.put("ok", false);
					reply.put("error", "not a JSON request: " + e.getMessage());
				}
				out.println(Json.write(reply));
				if (quit) {
					return;
				}
			}
		}
	}

	private Map<String, Object> handle(final Map<String, Object> request) {
		String cmd = String.valueOf(request.get("cmd"));
		if (!cmd.startsWith("gui_")) {
			return model.handle(request);
		}
		Map<String, Object> reply = new LinkedHashMap<String, Object>();
		if (request.containsKey("id")) {
			reply.put("id", request.get("id"));
		}
		try {
			reply.put("ok", true);
			reply.putAll(onSwingThread(cmd, request));
		} catch (Exception e) {
			reply.put("ok", false);
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			StackTraceElement[] where = cause.getStackTrace();
			reply.put("error", cause.getClass().getSimpleName() + ": " + cause.getMessage()
					+ (where.length > 0 ? " at " + where[0] : ""));
		}
		return reply;
	}

	/** Run a window command on the Swing thread and wait for its answer. */
	private Map<String, Object> onSwingThread(final String cmd, final Map<String, Object> request) throws Exception {
		final AtomicReference<Object> result = new AtomicReference<Object>();
		final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		Callable<Map<String, Object>> job = new Callable<Map<String, Object>>() {
			public Map<String, Object> call() throws Exception {
				return window(cmd, request);
			}
		};
		if (cmd.equals("gui_menu")) {
			// a menu item may open a modal dialog, which would hold the Swing thread: do not wait for it
			final Map<String, Object> started = new LinkedHashMap<String, Object>();
			final String path = String.valueOf(request.get("path"));
			final JMenuItem item = findMenuItem(path);
			if (item == null) {
				throw new IllegalArgumentException("no menu item '" + path + "'");
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					item.doClick();
				}
			});
			started.put("clicked", path);
			return started;
		}
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				try {
					result.set(job.call());
				} catch (Throwable t) {
					failure.set(t);
				}
			}
		});
		if (failure.get() != null) {
			throw new Exception(failure.get());
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) result.get();
		return m;
	}

	private Map<String, Object> window(String cmd, Map<String, Object> r) throws Exception {
		BigScore score = BigGui.bigScore;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		if (cmd.equals("gui_state")) {
			m.put("title", gui.getTitle());
			m.put("width", gui.getWidth());
			m.put("height", gui.getHeight());
			m.put("visible", gui.isVisible());
			m.put("loading", gui.loading);
			m.put("masterIndex", score.masterIndex);
			m.put("items", score.fingResultList == null ? 0 : score.fingResultList.size());
			if (score.scoreUI instanceof VerovioScoreComponent) {
				VerovioScoreComponent v = (VerovioScoreComponent) score.scoreUI;
				m.put("zoom", v.zoom());
				m.put("zoomRelative", v.relativeZoom());
				m.put("userZoomed", v.userHasZoomed());
			}
			return m;
		}
		if (cmd.equals("gui_load_abc") || cmd.equals("gui_load")) {
			gui.lastError = null;
			m.put("loaded", gui.loadScoreFile(new File(String.valueOf(r.get("path")))));
			if (gui.lastError != null) {
				m.put("error", gui.lastError);
			}
			m.put("items", score.fingResultList == null ? 0 : score.fingResultList.size());
			return m;
		}
		if (cmd.equals("gui_recent")) {
			List<Object> paths = new ArrayList<Object>();
			for (File f : gui.recent.list()) {
				paths.add(f.getPath());
			}
			m.put("recent", paths);
			return m;
		}
		if (cmd.equals("gui_drop")) {
			final File f = new File(String.valueOf(r.get("path")));
			final boolean asText = "uri-list".equals(r.get("as"));
			gui.lastError = null;
			java.awt.datatransfer.Transferable t = new java.awt.datatransfer.Transferable() {
				public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
					return new java.awt.datatransfer.DataFlavor[] {
							asText ? java.awt.datatransfer.DataFlavor.stringFlavor : java.awt.datatransfer.DataFlavor.javaFileListFlavor };
				}
				public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor fl) {
					return fl.equals(getTransferDataFlavors()[0]);
				}
				public Object getTransferData(java.awt.datatransfer.DataFlavor fl) {
					return asText ? "# dropped\r\n" + f.toURI() + "\r\n" : java.util.Collections.singletonList(f);
				}
			};
			javax.swing.TransferHandler h = gui.getRootPane().getTransferHandler();
			m.put("accepted", h.importData(new javax.swing.TransferHandler.TransferSupport(gui.getRootPane(), t)));
			if (gui.lastError != null) {
				m.put("error", gui.lastError);
			}
			return m;
		}
		boolean empty = score.fingResultList == null || score.fingResultList.isEmpty();
		if (empty && !cmd.equals("gui_menus") && !cmd.equals("gui_screenshot")) {
			throw new IllegalStateException("no piece is loaded in the window: send gui_load_abc first");
		}
		if (cmd.equals("gui_start")) {
			score.doStart();
			score.setIndexArrows();
		} else if (cmd.equals("gui_next")) {
			score.nextArrowActionPerformed();
		} else if (cmd.equals("gui_prev")) {
			score.prevArrowActionPerformed();
		} else if (cmd.equals("gui_jump_next")) {
			score.jumpNextArrowActionPerformed();
		} else if (cmd.equals("gui_jump")) {
			score.jumpToMasterIndex(((Number) r.get("index")).intValue());
			score.setIndexArrows();
		} else if (cmd.equals("gui_extend")) {
			score.extend();
		} else if (cmd.equals("gui_shrink")) {
			score.shrink();
		} else if (cmd.equals("gui_trail")) {
			return trail(score);
		} else if (cmd.equals("gui_click")) {
			java.awt.Point shown = score.showStretchAt(((Number) r.get("item")).intValue());
			m.put("firstItem", shown.x);
			m.put("lastItem", shown.y);
		} else if (cmd.equals("gui_zoom") || cmd.equals("gui_wheel") || cmd.equals("gui_note_point") || cmd.equals("gui_item_at")) {
			if (!(score.scoreUI instanceof VerovioScoreComponent)) {
				throw new IllegalStateException("the score is not shown by Verovio: no zoom");
			}
			return zoom((VerovioScoreComponent) score.scoreUI, cmd, r);
		} else if (cmd.equals("gui_bow_style")) {
			score.setBowStyle(((Number) r.get("style")).intValue());
		} else if (cmd.equals("gui_unique")) {
			boolean on = !Boolean.FALSE.equals(r.get("on"));
			gui.lastError = null;
			List<Object> out = new ArrayList<Object>();
			for (ScoreView.Stretch st : score.solveAll(on)) {
				if (!st.unique) {
					continue;
				}
				Map<String, Object> e = new LinkedHashMap<String, Object>();
				e.put("firstItem", st.start);
				e.put("lastItem", st.stop);
				e.put("firstMeasure", st.firstMeasure);
				e.put("lastMeasure", st.lastMeasure);
				e.put("label", st.label);
				List<Object> sq = new ArrayList<Object>();     // string of each note in order: 0 = G ... 3 = E
				for (int[] x : st.strings) {
					sq.add(x[2]);
				}
				e.put("strings", sq);
				out.add(e);
			}
			m.put("stretches", out);
			if (gui.lastError != null) {
				m.put("error", gui.lastError);
			}
			return m;
		} else if (cmd.equals("gui_menus")) {
			m.put("menus", menuTree(gui.getJMenuBar()));
			return m;
		} else if (cmd.equals("gui_screenshot")) {
			m.put("written", screenshot(String.valueOf(r.get("path"))));
			return m;
		} else {
			throw new IllegalArgumentException("unknown window command '" + cmd + "'");
		}
		m.put("masterIndex", score.masterIndex);
		return m;
	}

	/** The zoom commands, on the Verovio view.  Complexity: O(pages). */
	private Map<String, Object> zoom(VerovioScoreComponent v, String cmd, Map<String, Object> r) {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		java.awt.Point at = r.containsKey("x") ? new java.awt.Point(((Number) r.get("x")).intValue(), ((Number) r.get("y")).intValue()) : null;
		if (cmd.equals("gui_zoom")) {
			if (Boolean.TRUE.equals(r.get("reset"))) {
				v.resetZoom();
			} else {
				v.setRelativeZoom(((Number) r.get("factor")).doubleValue(), at);
			}
		} else if (cmd.equals("gui_wheel")) {
			// a real wheel event to the view: rotation < 0 is up; shift defaults to true
			int mods = Boolean.FALSE.equals(r.get("shift")) ? 0 : java.awt.event.InputEvent.SHIFT_DOWN_MASK;
			if (Boolean.TRUE.equals(r.get("ctrl"))) {
				mods |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
			}
			double rot = ((Number) r.get("rotation")).doubleValue();
			java.awt.Point p = at != null ? at : new java.awt.Point(10, 10);
			v.dispatchEvent(new java.awt.event.MouseWheelEvent(v, java.awt.event.MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), mods,
					p.x, p.y, p.x, p.y, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, (int) Math.signum(rot), rot));
		} else if (cmd.equals("gui_note_point")) {
			java.awt.Point p = v.notePoint(((Number) r.get("item")).intValue());
			if (p == null) {
				throw new IllegalArgumentException("that item has no drawn note");
			}
			m.put("x", p.x);
			m.put("y", p.y);
			m.put("hit", v.itemAtPoint(p));
		} else {
			if (at == null) {
				throw new IllegalArgumentException("gui_item_at needs x and y");
			}
			m.put("item", v.itemAtPoint(at));
		}
		m.put("zoom", v.zoom());
		m.put("zoomRelative", v.relativeZoom());
		m.put("userZoomed", v.userHasZoomed());
		java.awt.Dimension d = v.getPreferredSize();
		m.put("prefWidth", d.width);
		m.put("prefHeight", d.height);
		javax.swing.JViewport vp = (javax.swing.JViewport) v.getParent();
		m.put("viewX", vp.getViewPosition().x);
		m.put("viewY", vp.getViewPosition().y);
		return m;
	}

	/** The anchor the window is showing: its items, and where its first trail puts each note.  Complexity: O(items x points). */
	private Map<String, Object> trail(BigScore score) {
		AnchorSequencer.Anchor anchor = score.anchSeq.currAnchor;
		if (anchor == null || anchor.activeHead() == null) {
			throw new IllegalStateException("no trail is showing");
		}
		FingerTrail trail = anchor.activeHead();
		Instrument ins = score.instrument;
		String[] stringName = { "G", "D", "A", "E" };
		int groupStop = anchor.fs2.stop();               // the first step of a trail is the whole first group
		List<Object> rows = new ArrayList<Object>();
		java.util.TreeSet<Integer> heights = new java.util.TreeSet<Integer>();
		for (int item = anchor.start; item <= anchor.stop; item++) {
			int order = item <= groupStop ? 0 : item - groupStop;
			List<Object> notes = new ArrayList<Object>();
			for (int midi : score.fingResultList.get(item).kNums()) {
				for (FingerTrail.Trailpoint tp : trail.getTrailPoints()) {
					if (tp.onOrder.contains(order) && ins.openStringPitch(tp.saite) + tp.interval == midi) {
						Map<String, Object> n = new LinkedHashMap<String, Object>();
						n.put("abc", AbcPitch.name(midi, 0));
						n.put("string", stringName[tp.saite]);
						n.put("height", tp.interval);
						notes.add(n);
						if (tp.interval > 0) {
							heights.add(tp.interval);
						}
						break;
					}
				}
			}
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put("item", item);
			row.put("step", order);
			row.put("notes", notes);
			rows.add(row);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("firstItem", anchor.start);
		m.put("lastItem", anchor.stop);
		m.put("firstGroupLastItem", groupStop);
		m.put("trailsAlive", anchor.countActiveTrails());
		m.put("stoppedHeightsUsed", heights);
		m.put("lowestHeight", trail.fbHeightRange().x);
		m.put("highestHeight", trail.fbHeightRange().y);
		m.put("lifts", trail.lifts);
		m.put("bariolations", trail.countBariolations());
		m.put("items", rows);
		return m;
	}

	private String screenshot(String path) throws IOException {
		gui.validate();
		Container root = gui.getRootPane();
		BufferedImage img = new BufferedImage(Math.max(1, root.getWidth()), Math.max(1, root.getHeight()), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		root.paint(g);
		g.dispose();
		ImageIO.write(img, "png", new File(path));
		return path + " (" + img.getWidth() + "x" + img.getHeight() + ")";
	}

	private List<Object> menuTree(JMenuBar bar) {
		List<Object> out = new ArrayList<Object>();
		for (int i = 0; bar != null && i < bar.getMenuCount(); i++) {
			out.add(menuEntry(bar.getMenu(i)));
		}
		return out;
	}

	private Map<String, Object> menuEntry(JMenuItem item) {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("name", item.getText());
		m.put("enabled", item.isEnabled());
		if (item instanceof JMenu) {
			List<Object> kids = new ArrayList<Object>();
			for (Component c : ((JMenu) item).getMenuComponents()) {
				if (c instanceof JMenuItem) {
					kids.add(menuEntry((JMenuItem) c));
				}
			}
			m.put("items", kids);
		}
		return m;
	}

	private JMenuItem findMenuItem(String path) {
		String[] parts = path.split("/");
		JMenuBar bar = gui.getJMenuBar();
		JMenuItem current = null;
		for (int i = 0; bar != null && i < bar.getMenuCount() && current == null; i++) {
			if (bar.getMenu(i).getText().equals(parts[0])) {
				current = bar.getMenu(i);
			}
		}
		for (int p = 1; current != null && p < parts.length; p++) {
			JMenuItem next = null;
			if (current instanceof JMenu) {
				for (Component c : ((JMenu) current).getMenuComponents()) {
					if (c instanceof JMenuItem && ((JMenuItem) c).getText().equals(parts[p])) {
						next = (JMenuItem) c;
					}
				}
			}
			current = next;
		}
		return current;
	}
}

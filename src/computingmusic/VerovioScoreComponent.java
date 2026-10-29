package computingmusic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import computingmusic.gui.AnnotatedBoxes;
import computingmusic.jj.NotationData;
import computingmusic.utils.MySeq;

/**
 * The score as Verovio engraved it, in place of the abc4j drawing, with the boxes the fingering program draws
 * over it.  Pages come from tools/verovio/render_pages.py as images; each drawn note has a box in its page.
 * Everything here is in the component's own coordinates, scaled so that a page fills the width of the view.
 *
 * Items are the engine's notes and chords.  An item is a list of drawn notes, one for each pitch of it.
 *
 * Complexity: painting is O(pages + items in the boxed range); the range is boxed line by line.
 */
public class VerovioScoreComponent extends JComponent implements ScoreView {
	private static final long serialVersionUID = 1L;
	private static final int GAP = 12;                  // between pages, in component pixels
	private static final int BOXED_NOTE_MARGIN = 4;
	private static final int POSITIONAL_MARGIN = BOXED_NOTE_MARGIN + 2;
	private static final int RED_MARGIN = POSITIONAL_MARGIN + 2;
	private static final Color BOX_COLOR = new Color(220, 30, 30);
	private static final Color POSITION_COLOR = new Color(0x005200);

	private NotationData notation;
	private List<List<NotationData.Drawn>> itemBoxes = new ArrayList<List<NotationData.Drawn>>();
	private final ArrayList<AnnotatedBoxes> annotated = new ArrayList<AnnotatedBoxes>();
	private final ArrayList<Integer> red = new ArrayList<Integer>();
	private int boxStart = -1, boxStop = -1;
	private MySeq positional;
	private List<ScoreView.Stretch> stretches = new ArrayList<ScoreView.Stretch>();
	private JScrollPane pane;
	private boolean notScrolling = true;
	private double zoom = 0.75;
	/** The scale at which a page fills the view's width; zoom is limited to MIN_RELATIVE..MAX_RELATIVE times this. */
	private double fitScale = 0.75;
	/** Once the user has zoomed, resizing the window and loading a score keep the zoom; Ctrl+0 / Shift+0 give it back to fit-to-width. */
	private boolean userZoomed = false;
	static final double MIN_RELATIVE = 0.25, MAX_RELATIVE = 4.0, WHEEL_STEP = 1.1;

	public VerovioScoreComponent() {
		setBackground(Color.white);
		setOpaque(true);
		javax.swing.ToolTipManager.sharedInstance().registerComponent(this);
		setFocusable(true);
		addMouseWheelListener(new java.awt.event.MouseWheelListener() {
			public void mouseWheelMoved(java.awt.event.MouseWheelEvent ev) {
				wheel(ev);
			}
		});
		javax.swing.Action refit = new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(java.awt.event.ActionEvent e) {
				resetZoom();
			}
		};
		getInputMap(WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.event.InputEvent.CTRL_DOWN_MASK), "refit");
		getInputMap(WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.event.InputEvent.SHIFT_DOWN_MASK), "refit");
		getActionMap().put("refit", refit);
		addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent ev) {
				requestFocusInWindow();
			}
			public void mouseClicked(java.awt.event.MouseEvent ev) {
				int item = itemAt(ev.getPoint());
				if (item >= 0 && itemClick != null) {
					itemClick.accept(item);
				}
			}
		});
	}

	public JComponent component() {
		return this;
	}

	/** Show a score: its pages, and for each item the drawn notes it stands for. */
	public void setNotation(NotationData notation, List<List<NotationData.Drawn>> itemBoxes) {
		this.notation = notation;
		this.itemBoxes = itemBoxes;
		systems = null;
		clearBoxedNotes();
		turnoffPositions();
		fitToWidth();
		revalidate();
		repaint();
	}

	// ---- geometry ----------------------------------------------------------------------------------

	private int pageWidth() {
		int w = 1;
		for (NotationData.Page p : notation.pages) {
			w = Math.max(w, p.image.getWidth());
		}
		return w;
	}

	/** Scale pages to the width of the scroll pane's view.  Complexity: O(pages). */
	private void fitToWidth() {
		if (notation == null || notation.pages.isEmpty()) {
			return;
		}
		int available = pane != null ? pane.getViewport().getWidth() : 0;
		double z = available > 100 ? (available - 8.0) / pageWidth() : 0.75;
		z = Math.max(0.3, Math.min(z, 2.0));
		fitScale = z;
		if (userZoomed) {
			double kept = Math.max(z * MIN_RELATIVE, Math.min(zoom, z * MAX_RELATIVE));   // the limits move with the fit
			if (Math.abs(kept - zoom) > 1e-3) {
				zoom = kept;
				refreshDerived();
			}
		} else if (Math.abs(z - zoom) > 1e-3) {
			zoom = z;
			refreshDerived();
		}
	}

	// ---- zoom --------------------------------------------------------------------------------------

	/** The scale of the page images on screen. */
	public double zoom() {
		return zoom;
	}

	/** The zoom as a multiple of the fit-to-width scale: 1 = a page fills the width. */
	public double relativeZoom() {
		return zoom / fitScale;
	}

	public boolean userHasZoomed() {
		return userZoomed;
	}

	/** Set the zoom to `factor` times the fit-to-width scale (clamped to 0.25..4), keeping the score point at `anchor` under it. */
	public void setRelativeZoom(double factor, Point anchor) {
		zoomTo(fitScale * factor, anchor);
	}

	/** Go back to fit-to-width, about the middle of the view, and let resizes refit again. */
	public void resetZoom() {
		Point mid = null;
		if (pane != null) {
			Rectangle v = pane.getViewport().getViewRect();
			mid = new Point(v.x + v.width / 2, v.y + v.height / 2);
		}
		zoomTo(fitScale, mid);
		userZoomed = false;
		fitToWidth();
	}

	/**
	 * Multiply the zoom by `factor` about the point (in component coordinates); the score under that point stays
	 * under the cursor.  Complexity: O(pages + overlays).
	 */
	void zoomBy(double factor, Point anchor) {
		zoomTo(zoom * factor, anchor);
	}

	private void zoomTo(double wanted, Point anchor) {
		if (notation == null || notation.pages.isEmpty()) {
			return;
		}
		double z = Math.max(fitScale * MIN_RELATIVE, Math.min(wanted, fitScale * MAX_RELATIVE));
		userZoomed = true;
		if (Math.abs(z - zoom) < 1e-9) {
			return;
		}
		// the anchor, as a point of a page image (the gap between pages counts as the edge of the page above or below)
		int page = 0;
		if (anchor != null) {
			for (int i = 0; i < notation.pages.size(); i++) {
				if (anchor.y >= pageTop(i) - GAP / 2) {
					page = i;
				}
			}
		}
		double ix = 0, iy = 0;
		if (anchor != null) {
			ix = anchor.x / zoom;
			iy = (anchor.y - pageTop(page)) / zoom;
		}
		Point offset = null;
		if (anchor != null && pane != null) {
			Point vp = pane.getViewport().getViewPosition();
			offset = new Point(anchor.x - vp.x, anchor.y - vp.y);
		}
		zoom = z;
		cellHits.clear();
		bracketTips.clear();
		refreshDerived();
		if (offset != null) {
			setSize(getPreferredSize());
			pane.validate();
			int nx = (int) Math.round(ix * zoom) - offset.x, ny = pageTop(page) + (int) Math.round(iy * zoom) - offset.y;
			Dimension ext = pane.getViewport().getExtentSize(), all = getPreferredSize();
			nx = Math.max(0, Math.min(nx, Math.max(0, all.width - ext.width)));
			ny = Math.max(0, Math.min(ny, Math.max(0, all.height - ext.height)));
			pane.getViewport().setViewPosition(new Point(nx, ny));
		}
		repaint();
	}

	/** Shift+wheel zooms by 10% a notch (fractions of a notch count); anything else scrolls as it always did. */
	private void wheel(java.awt.event.MouseWheelEvent ev) {
		if (ev.isShiftDown() && !ev.isControlDown() && !ev.isAltDown() && !ev.isMetaDown()) {
			double turn = ev.getPreciseWheelRotation();
			if (turn != 0) {
				zoomBy(Math.pow(WHEEL_STEP, -turn), ev.getPoint());     // up (negative) = in
			}
			ev.consume();
		} else if (pane != null) {
			pane.dispatchEvent(javax.swing.SwingUtilities.convertMouseEvent(this, ev, pane));
		}
	}

	/** The component point at the middle of the first drawn note of the item, or null. */
	public Point notePoint(int item) {
		Rectangle r = item >= 0 && item < itemBoxes.size() && !itemBoxes.get(item).isEmpty() ? noteRect(itemBoxes.get(item).get(0), 0) : null;
		return r == null ? null : new Point(r.x + r.width / 2, r.y + r.height / 2);
	}

	/** What a click at the point would choose: the item, or -1. */
	public int itemAtPoint(Point p) {
		return itemAt(p);
	}

	/** Rectangles worked out at one scale must be worked out again at another. */
	private void refreshDerived() {
		if (positional != null) {
			positional.lineRects = lineRectangles(positional.start, positional.stop, POSITIONAL_MARGIN);
		}
		for (AnnotatedBoxes a : annotated) {
			a.invalidate();
		}
		revalidate();
		repaint();
	}

	private int pageTop(int page) {
		int y = GAP;
		for (int i = 0; i < page; i++) {
			y += (int) Math.round(notation.pages.get(i).image.getHeight() * zoom) + GAP;
		}
		return y;
	}

	@Override
	public Dimension getPreferredSize() {
		if (notation == null) {
			return new Dimension(400, 300);
		}
		return new Dimension((int) Math.round(pageWidth() * zoom) + 8, pageTop(notation.pages.size()));
	}

	private Rectangle noteRect(NotationData.Drawn d, int margin) {
		int top = pageTop(d.page);
		return new Rectangle((int) Math.round(d.x * zoom) - margin, top + (int) Math.round(d.y * zoom) - margin,
				(int) Math.round(d.w * zoom) + 2 * margin, (int) Math.round(d.h * zoom) + 2 * margin);
	}

	/** The rectangle around all the notes of an item, or null if it has none drawn. */
	private Rectangle itemRect(int item, int margin) {
		if (item < 0 || item >= itemBoxes.size()) {
			return null;
		}
		Rectangle r = null;
		for (NotationData.Drawn d : itemBoxes.get(item)) {
			Rectangle n = noteRect(d, margin);
			r = r == null ? n : r.union(n);
		}
		return r;
	}

	private int itemPage(int item) {
		return itemBoxes.get(item).isEmpty() ? -1 : itemBoxes.get(item).get(0).page;
	}

	/**
	 * One rectangle for each line the items first..last occupy.  A new line begins when the page changes or an
	 * item lies to the left of the one before it.  Complexity: O(last - first).
	 */
	private ArrayList<Rectangle> lineRectangles(int first, int last, int margin) {
		ArrayList<Rectangle> lines = new ArrayList<Rectangle>();
		Rectangle current = null;
		int lastX = -1, lastPage = -1;
		for (int i = Math.max(first, 0); i <= last && i < itemBoxes.size(); i++) {
			Rectangle r = itemRect(i, margin);
			if (r == null) {
				continue;
			}
			int page = itemPage(i);
			if (current == null || page != lastPage || r.x < lastX) {
				if (current != null) {
					lines.add(current);
				}
				current = new Rectangle(r);
			} else {
				current = current.union(r);
			}
			lastX = r.x;
			lastPage = page;
		}
		if (current != null) {
			lines.add(current);
		}
		return lines;
	}

	// ---- ScoreView ---------------------------------------------------------------------------------

	public void setBoxedNotes(int index) {
		setBoxedNotes(index, index);
	}

	public void setBoxedNotes(Point io) {
		setBoxedNotes(io.x, io.y);
	}

	public void setBoxedNotes(int start, int stop) {
		boxStart = start;
		boxStop = stop;
		ArrayList<Rectangle> lines = lineRectangles(start, stop, BOXED_NOTE_MARGIN);
		if (!lines.isEmpty()) {
			showRect(lines.get(0));
		}
		repaint();
	}

	public void setStretches(List<ScoreView.Stretch> stretches) {
		this.stretches = new ArrayList<ScoreView.Stretch>(stretches);
		repaint();
	}

	public void clearBoxedNotes() {
		boxStart = -1;
		boxStop = -1;
		red.clear();
		annotated.clear();
	}

	public void addRedNote(int index) {
		red.add(index);
		repaint();
	}

	public void addAnnotatedBoxes(AnnotatedBoxes boxes) {
		annotated.add(boxes);
		repaint();
	}

	public void clearAnnotationBoxes() {
		annotated.clear();
		repaint();
	}

	public void setPositionalSeq2(MySeq seq) {
		if (seq == null) {
			return;
		}
		positional = seq;
		seq.lineRects = lineRectangles(seq.start, seq.stop, POSITIONAL_MARGIN);
		repaint();
	}

	public void turnoffPositions() {
		positional = null;
		repaint();
	}

	public ArrayList<Rectangle> lineRectanglesofPoint(Point p, int margin) {
		return lineRectangles(p.x, p.y, margin);
	}

	public void myDrawRect(Rectangle r, Graphics g) {
		g.drawRect(r.x, r.y, r.width, r.height);
		showRect(r);
	}

	public void giveScrollPaneAccess(JScrollPane pane) {
		this.pane = pane;
		pane.getViewport().addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				fitToWidth();
			}
		});
	}

	public void setNotScrolling(boolean notScrolling) {
		this.notScrolling = notScrolling;
	}

	/** Scroll so that r is in view, unless the user is dragging the scroll bar. */
	private void showRect(Rectangle r) {
		if (pane == null || !notScrolling) {
			return;
		}
		Rectangle view = pane.getViewport().getViewRect();
		if (!view.contains(r)) {
			scrollRectToVisible(new Rectangle(r.x - 40, r.y - 120, r.width + 80, r.height + 240));
		}
	}

	// ---- painting ----------------------------------------------------------------------------------

	private static final Color STRETCH_LINE = new Color(20, 120, 60);

	private static final Color STRING_TEXT = new Color(20, 70, 190);
	private static final String[] ROMAN = {"IV", "III", "II", "I"};   // string 0 = G ... 3 = E
	private static final int NUMERAL_ROW = 17;                         // screen pixels between rows of numerals

	/** One system of a page: its horizontal extent and the lowest row of the page image that anything of it reaches. */
	private static class SystemBand {
		int page, xmin, xmax;
		int topY = Integer.MAX_VALUE, bottomY = Integer.MIN_VALUE;   // noteheads, in image pixels
		int inkBottom;                                                // lowest engraved ink: beams, slurs, stems
	}

	private List<SystemBand> systems;
	private int[] itemSystem;

	/**
	 * Group the notes into systems (a new system begins when the page changes or a note lies to the left of the one
	 * before it), then find, for each, the lowest ink in its columns by scanning the page image down from its lowest
	 * notehead until a clear gap of one staff space.  Anything written below that row cannot touch the engraving.
	 * Complexity: O(pixels of the page images).
	 */
	private void buildSystems() {
		systems = new ArrayList<SystemBand>();
		itemSystem = new int[itemBoxes.size()];
		SystemBand cur = null;
		double lastX = -1;
		int lastPage = -1;
		for (int i = 0; i < itemBoxes.size(); i++) {
			itemSystem[i] = -1;
			for (NotationData.Drawn d : itemBoxes.get(i)) {
				if (cur == null || d.page != lastPage || d.x < lastX - 1) {
					cur = new SystemBand();
					cur.page = d.page;
					cur.xmin = Integer.MAX_VALUE;
					cur.xmax = Integer.MIN_VALUE;
					systems.add(cur);
				}
				cur.xmin = Math.min(cur.xmin, (int) d.x);
				cur.xmax = Math.max(cur.xmax, (int) (d.x + d.w));
				cur.topY = Math.min(cur.topY, (int) d.y);
				cur.bottomY = Math.max(cur.bottomY, (int) (d.y + d.h));
				itemSystem[i] = systems.size() - 1;
				lastX = d.x;
				lastPage = d.page;
			}
		}
		for (int k = 0; k < systems.size(); k++) {
			SystemBand sb = systems.get(k);
			BufferedImage im = notation.pages.get(sb.page).image;
			int limit = im.getHeight() - 1;
			if (k + 1 < systems.size() && systems.get(k + 1).page == sb.page) {
				limit = Math.min(limit, systems.get(k + 1).topY - 1);
			}
			int gap = 6;                                                   // slurs and beams hang from the noteheads: a gap this wide is clear
			int x0 = Math.max(0, sb.xmin - 4), x1 = Math.min(im.getWidth() - 1, sb.xmax + 4);
			int lastInk = sb.bottomY;
			for (int y = sb.bottomY; y <= limit && y - lastInk <= gap; y++) {
				for (int x = x0; x <= x1; x++) {
					int rgb = im.getRGB(x, y);
					int lum = ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255)) / 3;
					if ((rgb >>> 24) > 100 && lum < 200) {                    // transparent pixels are the blank page
						lastInk = y;
						break;
					}
				}
			}
			sb.inkBottom = lastInk;
		}
	}

	private java.util.function.IntConsumer itemClick;
	private final ArrayList<Object[]> cellHits = new ArrayList<Object[]>();          // {Rectangle, Integer item}, from the last paint

	public void setItemClickListener(java.util.function.IntConsumer listener) {
		itemClick = listener;
	}

	/** The item whose notehead, or whose column of the string roll, holds the point; -1 if none.  Complexity: O(notes). */
	private int itemAt(Point p) {
		for (Object[] h : cellHits) {
			if (((Rectangle) h[0]).contains(p)) {
				return (Integer) h[1];
			}
		}
		for (int i = 0; i < itemBoxes.size(); i++) {
			for (NotationData.Drawn d : itemBoxes.get(i)) {
				if (noteRect(d, 3).contains(p)) {
					return i;
				}
			}
		}
		return -1;
	}

	private static final Color FINGER_FORCED = new Color(20, 40, 110), FINGER_CHOSEN = new Color(80, 95, 135), DISAGREE = new Color(200, 60, 50);
	// the static hand positions that are not forced: two pale tints in turn, so where one ends and the next begins can be seen
	private static final Color POSITION_BAND_A = new Color(255, 236, 179, 120), POSITION_BAND_B = new Color(204, 214, 255, 120);
	private static final Color POSITION_EDGE_A = new Color(200, 170, 80), POSITION_EDGE_B = new Color(120, 135, 200);
	private static final Color FORCED_BAND = new Color(168, 228, 160, 110);     // Crayola granny smith apple, see-through
	private static final Color FORCED_EDGE = new Color(60, 150, 70);

	/**
	 * The x range {left, right} of the cells of stretch `s` in this system's row of notes (sorted by x): from the left
	 * edge of its first cell to the position of the next note after its last, so the band covers the time the notes
	 * last, not only the points where they begin.  Null if the stretch has no note here.  Complexity: O(row).
	 */
	private double[] cellSpan(ArrayList<double[]> row, ScoreView.Stretch s) {
		double lo = Double.MAX_VALUE, last = -1;
		for (double[] n : row) {
			if (n[4] >= s.start && n[4] <= s.stop) {
				lo = Math.min(lo, n[0] - n[9] / 2);
				last = Math.max(last, n[0]);
			}
		}
		if (last < 0) {
			return null;
		}
		double next = last + 12;
		for (double[] n : row) {
			if (Math.round(n[0]) > Math.round(last)) {
				next = n[0] - n[9] / 2;
				break;
			}
		}
		return new double[] {lo, next - 1};
	}

	/** The x range {lo, hi} of the notes of stretch `s` in system `sys`, or null if it has none there. */
	private double[] spanInSystem(ScoreView.Stretch s, int sys) {
		double lo = Double.MAX_VALUE, hi = -1;
		for (int[] e : s.strings) {
			if (e[0] >= 0 && e[0] < itemSystem.length && itemSystem[e[0]] == sys) {
				for (NotationData.Drawn d : itemBoxes.get(e[0])) {
					Rectangle r = noteRect(d, 0);
					lo = Math.min(lo, r.x + r.width / 2.0);
					hi = Math.max(hi, r.x + r.width / 2.0);
				}
			}
		}
		return hi < 0 ? null : new double[] {lo, hi};
	}

	private final ArrayList<Object[]> bracketTips = new ArrayList<Object[]>();   // {Rectangle, label}

	/** Show the strings as Roman numerals instead of the bowing icon. */
	private boolean showNumerals = false;
	/** How the strings are drawn when not as numerals: 0 = sweep line, 1 = a dot on its string's line, 2 = a filled cell on its string's row. */
	private int bowStyle = 2;

	public void setBowStyle(int style) {
		bowStyle = style;
		repaint();
	}
	private static final int LEVEL = 14;                               // screen pixels between strings in the icon

	private static final Color SWEEP_UNIQUE = new Color(147, 204, 234);          // Crayola cornflower
	private static final Color SWEEP_SEVERAL = new Color(205, 197, 194);         // Crayola silver

	/**
	 * The points of the bow's path across the strings, from notes given as {x, string, unique}: equal neighbours
	 * are one point at their mean x; both ends and every reversal of direction are kept.  Complexity: O(n).
	 */
	static ArrayList<double[]> bowPath(List<double[]> notes) {
		ArrayList<double[]> pts = new ArrayList<double[]>();
		for (int i = 0; i < notes.size();) {
			int j = i;
			while (j + 1 < notes.size() && notes.get(j + 1)[1] == notes.get(i)[1]) {
				j++;
			}
			pts.add(new double[] {(notes.get(i)[0] + notes.get(j)[0]) / 2, notes.get(i)[1], notes.get(i)[2]});
			i = j + 1;
		}
		ArrayList<double[]> stroke = new ArrayList<double[]>();
		for (int k = 0; k < pts.size(); k++) {
			double[] p = pts.get(k);
			if (k == 0 || k == pts.size() - 1 || (p[1] - pts.get(k - 1)[1]) * (pts.get(k + 1)[1] - p[1]) < 0) {
				stroke.add(p);
			}
		}
		return stroke;
	}

	/**
	 * Under each system, below the lowest engraved ink, the bow's path across the strings for every note the engine
	 * has a string for, on four faint lines (the four strings, G at the bottom): a stroke between turning points,
	 * blue where the stretch has exactly one fingering and grey where it has several (the best trail's strings),
	 * Nothing is drawn over the staff.  With showNumerals the strings are written as Roman numerals instead.  A thin green
	 * bracket under a one-fingering stretch carries its label as a tooltip.  Complexity: O(notes of the stretches).
	 */
	private void paintStretches(Graphics2D g) {
		bracketTips.clear();
		cellHits.clear();
		if (stretches.isEmpty() || notation == null) {
			return;
		}
		if (systems == null) {
			buildSystems();
		}
		g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 14f));
		java.awt.FontMetrics fm = g.getFontMetrics();
		// per system: every note {x centre, string, unique, text width}, left to right
		java.util.TreeMap<Integer, ArrayList<double[]>> bySystem = new java.util.TreeMap<Integer, ArrayList<double[]>>();
		for (ScoreView.Stretch s : stretches) {
			for (int[] e : s.strings) {
				if (e[0] < 0 || e[0] >= itemBoxes.size() || itemSystem[e[0]] < 0) {
					continue;
				}
				for (NotationData.Drawn d : itemBoxes.get(e[0])) {
					if (d.pitch == e[1]) {
						Rectangle r = noteRect(d, 0);
						int str = Math.max(0, Math.min(3, e[2]));
						bySystem.computeIfAbsent(itemSystem[e[0]], k -> new ArrayList<double[]>())
								.add(new double[] {r.x + r.width / 2.0, str, s.unique ? 1 : 0, fm.stringWidth(ROMAN[str]), e[0],
										e.length > 3 ? e[3] : -1, e.length > 4 ? e[4] : 0, e.length > 5 ? e[5] : -1, e[1], r.width});
						break;
					}
				}
			}
		}
		for (java.util.Map.Entry<Integer, ArrayList<double[]>> en : bySystem.entrySet()) {
			SystemBand sb = systems.get(en.getKey());
			int base = pageTop(sb.page) + (int) Math.round(sb.inkBottom * zoom) + 5;       // top of the first row
			ArrayList<double[]> row = en.getValue();
			java.util.Collections.sort(row, (a, b) -> Double.compare(a[0], b[0]));
			int left = (int) row.get(0)[0] - 6, right = (int) row.get(row.size() - 1)[0] + 6, bottom;
			if (showNumerals) {
				ArrayList<Integer> rowEnd = new ArrayList<Integer>();                       // right edge used on each row
				for (double[] n : row) {
					g.setColor(n[2] > 0 ? STRING_TEXT : SWEEP_SEVERAL);
					int x = (int) (n[0] - n[3] / 2), k = 0;
					while (k < rowEnd.size() && rowEnd.get(k) + 4 > x) {
						k++;
					}
					if (k == rowEnd.size()) {
						rowEnd.add(0);
					}
					rowEnd.set(k, (int) (x + n[3]));
					g.drawString(ROMAN[(int) n[1]], x, base + k * NUMERAL_ROW + fm.getAscent());
				}
				bottom = base + rowEnd.size() * NUMERAL_ROW + 4;
			} else {
				int top = base + 8;                                                         // y of the I string
				g.setStroke(new BasicStroke(0.8f));
				g.setColor(new Color(190, 190, 205));
				for (int k = 0; k < 4; k++) {
					if (bowStyle == 2) {
						g.setColor(k % 2 == 0 ? new Color(243, 243, 247) : new Color(250, 250, 252));
						g.fillRect(left, top + k * LEVEL - LEVEL / 2, right - left, LEVEL);
					} else {
						g.drawLine(left, top + k * LEVEL, right, top + k * LEVEL);
					}
				}
				int ordinal = 0;
				for (ScoreView.Stretch s : stretches) {      // every static hand position: a band behind its cells; green where there is no choice
					double[] span = cellSpan(row, s);
					ordinal++;
					if (span != null) {
						int l = (int) span[0] - 3, w = (int) (span[1] - span[0]) + 6;
						g.setColor(s.unique ? FORCED_BAND : ordinal % 2 == 0 ? POSITION_BAND_A : POSITION_BAND_B);
						g.fillRect(l, top - LEVEL / 2 - 3, w, 4 * LEVEL + 6);
						g.setColor(s.unique ? FORCED_EDGE : ordinal % 2 == 0 ? POSITION_EDGE_A : POSITION_EDGE_B);
						g.setStroke(new BasicStroke(s.unique ? 1.3f : 1.0f));
						g.drawRect(l, top - LEVEL / 2 - 3, w, 4 * LEVEL + 6);
						bracketTips.add(new Object[] {new Rectangle(l, top - LEVEL / 2 - 3, w, 4 * LEVEL + 6), s.label});
					}
				}
				if (bowStyle == 0) {
					ArrayList<double[]> stroke = bowPath(row);
					g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
					for (int k = 0; k + 1 < stroke.size(); k++) {
						g.setColor(stroke.get(k + 1)[2] > 0 ? SWEEP_UNIQUE : SWEEP_SEVERAL);
						g.drawLine((int) Math.round(stroke.get(k)[0]), top + (int) (3 - stroke.get(k)[1]) * LEVEL,
								(int) Math.round(stroke.get(k + 1)[0]), top + (int) (3 - stroke.get(k + 1)[1]) * LEVEL);
					}
				} else {
					for (int k = 0; k < row.size(); k++) {
						double[] n = row.get(k);
						int x = (int) Math.round(n[0]), y = top + (int) (3 - n[1]) * LEVEL;
						g.setColor(n[2] > 0 ? SWEEP_UNIQUE : SWEEP_SEVERAL);
						if (bowStyle == 1) {
							g.fillOval(x - 4, y - 4, 8, 8);
						} else {
							// a cell starts at the left edge of its notehead and runs to the left edge of the next note's head;
							// the digit is centred under the notehead itself, so it lines up with its note
							int half = (int) Math.round(n[9] / 2);
							int j = k + 1;                                  // the next note at a LATER position: the notes of a chord share one
							while (j < row.size() && Math.round(row.get(j)[0]) <= x) {
								j++;
							}
							int cl = x - half;
							int next = j < row.size() ? (int) Math.round(row.get(j)[0]) - (int) Math.round(row.get(j)[9] / 2) : cl + 2 * half + 12;
							int cw = Math.max(3, next - cl - 2);
							g.fillRect(cl, y - LEVEL / 2 + 1, cw, LEVEL - 2);
							int ours = (int) n[5], printed = (int) n[7];
							if (ours >= 0 && printed >= 0 && ours != printed) {                 // our finger and the printed one disagree
								g.setColor(DISAGREE);
								g.setStroke(new BasicStroke(1.6f));
								g.drawRect(cl, y - LEVEL / 2 + 1, cw, LEVEL - 2);
							}
							if (ours >= 0 && cw >= 7) {                                          // our finger, centred under its note
								g.setFont(g.getFont().deriveFont(n[6] > 0 ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 10f));
								g.setColor(n[6] > 0 ? FINGER_FORCED : FINGER_CHOSEN);
								g.drawString(String.valueOf(ours), x - g.getFontMetrics().stringWidth(String.valueOf(ours)) / 2, y + 4);
							}
							cellHits.add(new Object[] {new Rectangle(cl, top - LEVEL / 2, Math.max(3, next - cl), 4 * LEVEL), (int) n[4]});
						}
					}
				}
				bottom = top + 3 * LEVEL + 12;
			}
		}
	}

	private int top0(int bottom) {
		return bottom - 4 * LEVEL - 12;
	}

	@Override
	public String getToolTipText(java.awt.event.MouseEvent ev) {
		for (Object[] t : bracketTips) {
			if (((Rectangle) t[0]).contains(ev.getPoint())) {
				return (String) t[1];
			}
		}
		return null;
	}

	@Override
	protected void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D) g0.create();
		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (notation == null) {
			g.dispose();
			return;
		}
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		Rectangle clip = g.getClipBounds() != null ? g.getClipBounds() : new Rectangle(getSize());
		for (int p = 0; p < notation.pages.size(); p++) {
			BufferedImage im = notation.pages.get(p).image;
			int top = pageTop(p), h = (int) Math.round(im.getHeight() * zoom);
			if (top + h >= clip.y && top <= clip.y + clip.height) {
				g.drawImage(im, 0, top, (int) Math.round(im.getWidth() * zoom), h, null);
			}
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paintStretches(g);
		g.setStroke(new BasicStroke(2));
		g.setColor(Color.RED);
		for (int index : red) {
			Rectangle r = itemRect(index, RED_MARGIN);
			if (r != null) {
				g.drawRect(r.x, r.y, r.width, r.height);
			}
		}
		g.setColor(BOX_COLOR);
		if (boxStart >= 0) {
			for (Rectangle r : lineRectangles(boxStart, boxStop, BOXED_NOTE_MARGIN)) {
				g.drawRect(r.x, r.y, r.width, r.height);
			}
		}
		if (positional != null && positional.lineRects != null && !positional.lineRects.isEmpty()) {
			g.setColor(POSITION_COLOR);
			for (Rectangle r : positional.lineRects) {
				g.drawRect(r.x, r.y, r.width, r.height);
			}
			Rectangle first = positional.lineRects.get(0);
			g.drawString(positional.positionString(), first.x, first.y - 4);
		}
		for (AnnotatedBoxes a : new ArrayList<AnnotatedBoxes>(annotated)) {
			a.draw(g);
		}
		g.dispose();
	}
}

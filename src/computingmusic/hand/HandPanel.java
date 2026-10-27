package computingmusic.hand;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

/**
 * The board drawn to scale (millimetres), nut at the top, strings converging towards the nut as on the instrument.
 * The four fingers are placed on it by the mouse: press or drag to put the active finger on the nearest string at
 * the nearest semitone; right-click lifts it.  The distances between the fingertips are drawn and coloured by
 * the model's verdict, and optionally the zone around each placed finger that the active finger can reach.
 */
public class HandPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Color BOARD = new Color(238, 226, 205), STRING = new Color(120, 104, 84);
	private static final Color OK = new Color(60, 150, 90), MAYBE = new Color(215, 140, 30), NO = new Color(200, 60, 50);
	private static final Color FINGER = new Color(62, 134, 181);

	public Geometry geometry = Geometry.VIOLIN;
	public HandParams params = new HandParams();
	public int maxHeight = 18;
	public int activeFinger = 1;
	public boolean showReach = true;
	/** placement of fingers 1..4: {string, height}, or null if lifted. */
	public final int[][] placed = new int[5][];
	private Runnable onChange = () -> {};

	private static final int TOP = 70, MARGIN_BOTTOM = 24;

	public HandPanel() {
		setPreferredSize(new Dimension(560, 760));
		setBackground(Color.white);
		MouseAdapter m = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				place(e);
			}

			public void mouseDragged(MouseEvent e) {
				place(e);
			}
		};
		addMouseListener(m);
		addMouseMotionListener(m);
	}

	public void setOnChange(Runnable r) {
		onChange = r;
	}

	private double scale() {
		return (getHeight() - TOP - MARGIN_BOTTOM) / geometry.distance(maxHeight);
	}

	private double cx() {
		return getWidth() * 0.55;
	}

	private double yOf(double mm) {
		return TOP + mm * scale();
	}

	private void place(MouseEvent e) {
		if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
			placed[activeFinger] = null;
			fire();
			return;
		}
		double mm = Math.max(0, (e.getY() - TOP) / scale());
		int best = 0;
		double bestD = Double.MAX_VALUE;
		for (int s = 0; s < geometry.strings(); s++) {
			double d = Math.abs(cx() + geometry.lateral(s, mm) * scale() - e.getX());
			if (d < bestD) {
				bestD = d;
				best = s;
			}
		}
		int h = (int) Math.max(1, Math.min(maxHeight, Math.round(geometry.height(mm))));
		placed[activeFinger] = new int[] {best, h};
		fire();
	}

	private void fire() {
		repaint();
		onChange.run();
	}

	public List<HandModel.Placement> placements() {
		List<HandModel.Placement> out = new ArrayList<HandModel.Placement>();
		for (int f = 1; f <= 4; f++) {
			if (placed[f] != null) {
				out.add(new HandModel.Placement(f, placed[f][0], placed[f][1]));
			}
		}
		return out;
	}

	public HandModel.Report report() {
		return HandModel.check(geometry, params, placements());
	}

	static Color color(HandModel.Level l) {
		return l == HandModel.Level.COMFORTABLE ? OK : l == HandModel.Level.POSSIBLE ? MAYBE : NO;
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double s = scale(), cx = cx(), endMm = geometry.distance(maxHeight);
		int n = geometry.strings();
		// the fingerboard
		double edge0 = geometry.gap(0) * 0.9, edge1 = geometry.gap(endMm) * 0.9;
		int w = (n - 1) / 2 + 1;
		double half0 = (n - 1) / 2.0 * geometry.gap(0) + edge0 * 0.5, half1 = (n - 1) / 2.0 * geometry.gap(endMm) + edge1 * 0.5;
		int[] xs = {(int) (cx - half0 * s), (int) (cx + half0 * s), (int) (cx + half1 * s), (int) (cx - half1 * s)};
		int[] ys = {(int) yOf(0), (int) yOf(0), (int) yOf(endMm), (int) yOf(endMm)};
		g.setColor(BOARD);
		g.fillPolygon(xs, ys, 4);
		g.setColor(new Color(60, 60, 60));
		g.setStroke(new BasicStroke(5f));
		g.drawLine(xs[0], ys[0], xs[1], ys[1]);                         // the nut
		// semitone marks, to scale: they close up towards the bridge
		g.setFont(getFont().deriveFont(10f));
		for (int h = 1; h <= maxHeight; h++) {
			double y = yOf(geometry.distance(h));
			g.setColor(new Color(170, 160, 140));
			g.setStroke(new BasicStroke(0.7f));
			g.drawLine((int) (cx - half1 * s), (int) y, (int) (cx - half1 * s - 6), (int) y);
			g.setColor(new Color(110, 105, 95));
			g.drawString(String.valueOf(h), (int) (cx - half1 * s - 26), (int) y + 4);
		}
		// the strings, drawn from the nut with their spacing at each end
		g.setStroke(new BasicStroke(1.4f));
		g.setColor(STRING);
		g.setFont(getFont().deriveFont(Font.BOLD, 13f));
		for (int k = 0; k < n; k++) {
			g.drawLine((int) (cx + geometry.lateral(k, 0) * s), (int) yOf(0), (int) (cx + geometry.lateral(k, endMm) * s), (int) yOf(endMm));
			g.setColor(Color.black);
			g.drawString(geometry.stringNames[k], (int) (cx + geometry.lateral(k, 0) * s) - 4, (int) yOf(0) - 12);
			g.setColor(STRING);
		}
		// where the active finger can reach, from each other finger down
		if (showReach) {
			for (int f = 1; f <= 4; f++) {
				if (f != activeFinger && placed[f] != null) {
					int k = HandParams.pairIndex(activeFinger, f);
					double mm = geometry.distance(placed[f][1]);
					double px = cx + geometry.lateral(placed[f][0], mm) * s, py = yOf(mm);
					ring(g, px, py, params.absolute[k] * params.size * s, MAYBE);
					ring(g, px, py, params.comfortable[k] * params.size * s, OK);
				}
			}
		}
		// distances between the fingertips, coloured by the verdict
		HandModel.Report rep = report();
		g.setFont(getFont().deriveFont(11f));
		FontMetrics fm = g.getFontMetrics();
		for (HandModel.PairReport pr : rep.pairs) {
			double[] a = tip(placed[pr.a], s, cx), b = tip(placed[pr.b], s, cx);
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 3f}, 0f));
			g.setColor(color(pr.level));
			g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
			String t = String.format("%.0f mm", pr.distance);
			int tx = (int) ((a[0] + b[0]) / 2) + 6, ty = (int) ((a[1] + b[1]) / 2);
			g.setColor(new Color(255, 255, 255, 215));
			g.fillRect(tx - 2, ty - fm.getAscent() + 1, fm.stringWidth(t) + 4, fm.getHeight());
			g.setColor(color(pr.level));
			g.drawString(t, tx, ty + 3);
		}
		// the fingers: a pad about 10 mm across
		g.setFont(getFont().deriveFont(Font.BOLD, 12f));
		for (int f = 1; f <= 4; f++) {
			if (placed[f] != null) {
				double[] t = tip(placed[f], s, cx);
				double r = 5 * s;
				g.setColor(FINGER);
				g.fill(new Ellipse2D.Double(t[0] - r, t[1] - r, 2 * r, 2 * r));
				g.setColor(f == activeFinger ? new Color(204, 68, 0) : Color.white);
				g.setStroke(new BasicStroke(f == activeFinger ? 2.5f : 1.4f));
				g.draw(new Ellipse2D.Double(t[0] - r, t[1] - r, 2 * r, 2 * r));
				g.setColor(Color.white);
				g.drawString(String.valueOf(f), (int) t[0] - 4, (int) t[1] + 4);
			}
		}
		g.dispose();
	}

	private double[] tip(int[] p, double s, double cx) {
		double mm = geometry.distance(p[1]);
		return new double[] {cx + geometry.lateral(p[0], mm) * s, yOf(mm)};
	}

	private static void ring(Graphics2D g, double x, double y, double r, Color c) {
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 18));
		g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
		g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 4f}, 0f));
		g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
	}
}

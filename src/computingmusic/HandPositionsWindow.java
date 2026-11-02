package computingmusic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

import computingmusic.hand.FingerFrames;
import computingmusic.hand.Geometry;

/**
 * Step through the hand positions of a passage.  A hand position here is a FINGER FRAME (hand/FingerFrames): an arrangement
 * of the strings, each open or stopped at a height, that the hand holds immobile for a run of notes, and that the fingering
 * Topology allows.  The window computes the MAXIMAL RUNS of the chosen bars (a run no earlier start reaches as far) and
 * shows one frame of one run on a fingerboard at a time.
 *
 *   Left / Right   the previous / next run
 *   Up / Down      the previous / next frame of the run (a run may have several)
 *
 * Frames are shown as heights (semitones above the open string), not as fingers.  Where neighbouring strings are stopped at
 * one height the discs are joined (a flap).  Violin, single-voice or chords written as voices; held notes are not counted.
 * Names the Topology gives that are not yet ruled possible (TWINFLAP, TRIANGLEHI, UNKNOWN) are left out and counted.
 */
public class HandPositionsWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	private final File score;
	private final JSpinner from, to;
	private final JCheckBox absolute = new JCheckBox("absolute hand limits");
	private final JLabel status = new JLabel(" ");
	private final JTextArea info = new JTextArea();
	private final Board board = new Board();
	private FingerFrames result;
	private List<FingerFrames.Run> runs;
	private int run, frame;

	public HandPositionsWindow(File score) {
		super("Hand positions: " + score.getName());
		this.score = score;
		int lo = 0, hi = 0;
		try {
			int[] r = FingerFrames.measureRange(score.getPath());
			lo = r[0];
			hi = r[1];
		} catch (Exception e) {
			status.setText("cannot read " + score.getName() + " as MusicXML: " + e.getMessage());
		}
		from = new JSpinner(new SpinnerNumberModel(lo, lo, hi, 1));
		to = new JSpinner(new SpinnerNumberModel(Math.min(hi, lo + 2), lo, hi, 1));
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("bars"));
		top.add(from);
		top.add(new JLabel("to"));
		top.add(to);
		top.add(absolute);
		JButton compute = new JButton("Find hand positions");
		compute.addActionListener(e -> compute());
		top.add(compute);
		top.add(status);

		info.setEditable(false);
		info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		info.setLineWrap(true);
		info.setWrapStyleWord(true);
		JScrollPane side = new JScrollPane(info);
		side.setPreferredSize(new Dimension(380, 400));

		JPanel arrows = new JPanel(new FlowLayout(FlowLayout.CENTER));
		arrows.add(button("◀ run", e -> stepRun(-1), "The previous run of notes one hand position plays (Left)."));
		arrows.add(button("run ▶", e -> stepRun(1), "The next run (Right)."));
		arrows.add(button("▲ frame", e -> stepFrame(-1), "The previous frame of this run (Up)."));
		arrows.add(button("frame ▼", e -> stepFrame(1), "The next frame of this run (Down)."));

		setLayout(new BorderLayout());
		add(top, BorderLayout.NORTH);
		add(board, BorderLayout.CENTER);
		add(side, BorderLayout.EAST);
		add(arrows, BorderLayout.SOUTH);
		bindKey("LEFT", () -> stepRun(-1));
		bindKey("RIGHT", () -> stepRun(1));
		bindKey("UP", () -> stepFrame(-1));
		bindKey("DOWN", () -> stepFrame(1));
		setSize(1000, 700);
		setLocationByPlatform(true);
		info.setText("Choose the bars and press Find hand positions.");
	}

	private JButton button(String text, java.awt.event.ActionListener l, String tip) {
		JButton b = new JButton(text);
		b.setToolTipText(tip);
		b.addActionListener(l);
		b.setFocusable(false);
		return b;
	}

	private void bindKey(String key, final Runnable r) {
		JComponent root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
		root.getActionMap().put(key, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				r.run();
			}
		});
	}

	/** Choose the bars to find hand positions in (then compute()). */
	public void setBars(int first, int last) {
		from.setValue(first);
		to.setValue(last);
	}

	/** Compute the frames of the chosen bars (off the Swing thread) and show the first run. */
	public void compute() {
		final int f = (Integer) from.getValue(), t = (Integer) to.getValue();
		final boolean abs = absolute.isSelected();
		status.setText("computing...");
		new SwingWorker<FingerFrames, Void>() {
			protected FingerFrames doInBackground() throws Exception {
				return FingerFrames.analyse(score.getPath(), f, t, abs);
			}

			protected void done() {
				try {
					result = get();
					runs = result.maximalRuns();
					run = 0;
					frame = 0;
					status.setText(runs.size() + " runs");
				} catch (Exception e) {
					result = null;
					runs = null;
					status.setText("failed: " + (e.getCause() == null ? e : e.getCause()));
				}
				refresh();
			}
		}.execute();
	}

	public void stepRun(int d) {
		if (runs == null || runs.isEmpty()) {
			return;
		}
		run = Math.max(0, Math.min(runs.size() - 1, run + d));
		frame = 0;
		refresh();
	}

	public void stepFrame(int d) {
		if (runs == null || runs.isEmpty()) {
			return;
		}
		int n = runs.get(run).frames.size();
		frame = Math.max(0, Math.min(n - 1, frame + d));
		refresh();
	}

	/** What the side panel says, for the current run and frame (also for tests). */
	public String describe() {
		return info.getText();
	}

	private void refresh() {
		if (runs == null || runs.isEmpty()) {
			board.show(null, null);
			info.setText(result == null ? info.getText() : "No hand position plays even one moment of these bars.");
			return;
		}
		FingerFrames.Run r = runs.get(run);
		FingerFrames.Frame fr = r.frames.get(frame);
		board.show(result.geometry(), fr);
		Geometry g = result.geometry();
		StringBuilder b = new StringBuilder();
		b.append("run ").append(run + 1).append(" of ").append(runs.size()).append(":  ")
				.append(r.end - r.start + 1).append(r.end == r.start ? " moment" : " moments").append("\n");
		b.append("frame ").append(frame + 1).append(" of ").append(r.frames.size()).append(":  ").append(fr.topology).append("\n");
		StringBuilder h = new StringBuilder();
		for (int s = 0; s < fr.height.length; s++) {
			h.append(g.stringNames[s]).append(' ').append(fr.height[s] == 0 ? "open" : "at " + fr.height[s]).append(s + 1 < fr.height.length ? ",  " : "");
		}
		b.append(h).append("\n\n");
		for (int i = r.start; i <= r.end; i++) {
			b.append(result.labels()[i]).append("   on");
			for (int p : result.events()[i]) {
				for (int s = 0; s < fr.height.length; s++) {
					if (g.open[s] + fr.height[s] == p) {
						b.append(' ').append(g.stringNames[s]);
						break;
					}
				}
			}
			b.append("\n");
		}
		if (r.frames.size() > 1) {
			b.append("\nOther frames of this run (Up / Down):\n");
			for (int k = 0; k < r.frames.size(); k++) {
				b.append(k == frame ? " > " : "   ").append(r.frames.get(k).topology).append("  ").append(java.util.Arrays.toString(r.frames.get(k).height)).append("\n");
			}
		}
		if (result.leftOutPatterns() > 0) {
			b.append("\n").append(result.leftOutPatterns()).append(" arrangement patterns were left out (TWINFLAP, TRIANGLEHI or unnamed): not yet ruled possible.\n");
		}
		info.setText(b.toString());
		info.setCaretPosition(0);
	}

	/** The board as an image, for tests. */
	public BufferedImage snapshot() {
		BufferedImage im = new BufferedImage(Math.max(1, board.getWidth()), Math.max(1, board.getHeight()), BufferedImage.TYPE_INT_RGB);
		board.paint(im.getGraphics());
		return im;
	}

	/** One frame on a fingerboard drawn to scale: a semitone is its true distance from the nut, so they close up towards the bridge. */
	private static final class Board extends JComponent {
		private static final long serialVersionUID = 1L;
		private Geometry g;
		private FingerFrames.Frame frame;

		void show(Geometry g, FingerFrames.Frame f) {
			this.g = g;
			this.frame = f;
			repaint();
		}

		public Dimension getPreferredSize() {
			return new Dimension(520, 600);
		}

		protected void paintComponent(Graphics gr) {
			Graphics2D g2 = (Graphics2D) gr;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, getWidth(), getHeight());
			if (g == null || frame == null) {
				g2.setColor(Color.GRAY);
				g2.drawString("no frame", 20, 30);
				return;
			}
			int n = g.open.length;
			int shown = 14;
			for (int h : frame.height) {
				shown = Math.max(shown, h + 2);
			}
			int left = 70, right = 40, top = 70, bottom = 30;
			int w = getWidth() - left - right, hgt = getHeight() - top - bottom;
			double scale = hgt / g.distance(shown);
			g2.setColor(new Color(236, 224, 200));
			g2.fillRect(left, top, w, hgt);
			g2.setColor(Color.BLACK);
			g2.drawRect(left, top, w, hgt);
			g2.setFont(g2.getFont().deriveFont(11f));
			for (int h = 1; h <= shown; h++) {
				int y = top + (int) Math.round(g.distance(h) * scale);
				g2.setColor(h == 12 ? Color.DARK_GRAY : new Color(200, 188, 164));
				g2.drawLine(left, y, left + w, y);
				g2.setColor(Color.GRAY);
				g2.drawString(String.valueOf(h), left - 24, y + 4);
			}
			int[] x = new int[n];
			for (int s = 0; s < n; s++) {
				x[s] = left + (int) Math.round(w * (s + 0.5) / n);
				g2.setColor(Color.BLACK);
				g2.drawLine(x[s], top, x[s], top + hgt);
				g2.drawString(g.stringNames[s], x[s] - 3, top - 36);
			}
			g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
			g2.setColor(Color.DARK_GRAY);
			g2.drawString(frame.topology, left, 20);
			int r = 10;
			// a flat finger across neighbouring strings at one height: a bar behind the discs
			for (int s = 0; s + 1 < n; s++) {
				if (frame.height[s] > 0 && frame.height[s] == frame.height[s + 1]) {
					int y = top + (int) Math.round(g.distance(frame.height[s]) * scale);
					g2.setColor(new Color(214, 60, 60, 150));
					g2.fillRoundRect(x[s] - r, y - r + 2, x[s + 1] - x[s] + 2 * r, 2 * r - 4, 2 * r, 2 * r);
				}
			}
			g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
			for (int s = 0; s < n; s++) {
				int h = frame.height[s];
				if (h == 0) {
					g2.setColor(Color.BLACK);
					g2.setStroke(new BasicStroke(2f));
					g2.drawOval(x[s] - 7, top - 22, 14, 14);
					g2.setStroke(new BasicStroke(1f));
				} else {
					int y = top + (int) Math.round(g.distance(h) * scale);
					g2.setColor(new Color(200, 30, 30));
					g2.fillOval(x[s] - r, y - r, 2 * r, 2 * r);
					g2.setColor(Color.WHITE);
					String t = String.valueOf(h);
					g2.drawString(t, x[s] - g2.getFontMetrics().stringWidth(t) / 2, y + 4);
				}
				g2.setColor(Color.DARK_GRAY);
				String pn = FingerFrames.name(g.open[s] + h);
				g2.drawString(pn, x[s] - g2.getFontMetrics().stringWidth(pn) / 2, top + hgt + 18);
			}
		}
	}
}

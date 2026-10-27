package computingmusic.jj;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import computingmusic.TrailRun;

/**
 * The score as Verovio draws it, with the data of each note beside it.  Click a note, or step with the
 * arrow keys, to see everything JJ holds for it: its pitch, onset, printed fingering, chord and slurs.
 * The notes of the chord a note belongs to are outlined in orange.
 *
 * Usage:
 *   NotationFrame [SCORE]                         open a window (SCORE: MusicXML, MEI, kern ...)
 *   NotationFrame SCORE --png OUT.png [--note N] [--trails]
 *                                                 write the page holding note N, with its data (and the trails), and exit
 */
public class NotationFrame extends JFrame {
	private static final int GAP = 14;

	private static final long serialVersionUID = 1L;
	private final PagesPanel pages = new PagesPanel(new Picker() {
		public void pick(int index, boolean scroll) {
			if (frame != null) {
				frame.select(index, scroll);
			}
		}
	});
	private static NotationFrame frame;     // the one open window, the target of the panel's picks
	private final JTextArea info = new JTextArea(9, 80);
	private final JLabel status = new JLabel(" ");
	private MusicXmlSession session;
	private TrailResult trails;          // the last run of the fingering trails, or null

	public NotationFrame() {
		super("String Fingering: notation");
		frame = this;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		info.setEditable(false);
		info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		add(status, BorderLayout.NORTH);
		add(new JScrollPane(pages), BorderLayout.CENTER);
		add(new JScrollPane(info), BorderLayout.SOUTH);
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem(new AbstractAction("Open score...") {
			public void actionPerformed(ActionEvent e) {
				chooseAndOpen();
			}
		});
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		file.add(open);
		bar.add(file);
		JMenu run = new JMenu("Trails");
		run.add(new JMenuItem(new AbstractAction("Run the fingering trails over the piece") {
			public void actionPerformed(ActionEvent e) {
				runTrails();
			}
		}));
		bar.add(run);
		setJMenuBar(bar);
		setSize(1100, 900);
	}

	private void chooseAndOpen() {
		JFileChooser fc = new JFileChooser(new File(System.getProperty("user.home"), "libmusic"));
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			open(fc.getSelectedFile());
		}
	}

	/** Load a score in the background (JJ takes a few seconds to start) and show it when ready. */
	public void open(final File file) {
		status.setText("Loading " + file.getName() + " through JJ and Verovio ...");
		new SwingWorker<MusicXmlSession, Void>() {
			protected MusicXmlSession doInBackground() throws Exception {
				return MusicXmlSession.open(file);
			}

			protected void done() {
				try {
					show(get());
				} catch (Exception e) {
					status.setText("Could not load " + file.getName());
					JOptionPane.showMessageDialog(NotationFrame.this, String.valueOf(e.getCause() != null ? e.getCause() : e),
							"Could not load " + file.getName(), JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	/** Run the trails over the open piece in the background, then shade the notation by segment. */
	void runTrails() {
		if (session == null) {
			return;
		}
		final MusicXmlSession s = session;
		status.setText("Running the fingering trails ...");
		new SwingWorker<TrailResult, Void>() {
			protected TrailResult doInBackground() {
				return TrailResult.of(s);
			}

			protected void done() {
				try {
					trails = get();
					pages.trails = trails;
					status.setText(s.file.getName() + ": " + trails.summary());
					select(Math.max(pages.selected, 0), false);
				} catch (Exception e) {
					status.setText("The trails could not be run");
					JOptionPane.showMessageDialog(NotationFrame.this, String.valueOf(e.getCause() != null ? e.getCause() : e),
							"Trails", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	void show(MusicXmlSession s) {
		session = s;
		pages.setSession(s);
		status.setText(s.file.getName() + ": " + s.links.size() + " notes. Click a note or use the arrow keys.");
		select(0, false);
		pages.requestFocusInWindow();
	}

	void select(int index, boolean scroll) {
		if (session == null || index < 0 || index >= session.links.size()) {
			return;
		}
		pages.selected = index;
		info.setText(describe(session, index) + (trails == null ? "" : trails.describe(index)));
		info.setCaretPosition(0);
		if (scroll) {
			pages.scrollToSelected();
		}
		pages.repaint();
	}

	/** The text shown for one note.  Complexity: O(notes) to find the chord mates. */
	static String describe(MusicXmlSession s, int index) {
		Link link = s.links.get(index);
		JJNote n = link.note;
		StringBuilder b = new StringBuilder();
		b.append(String.format("note %d of %d     %s     measure %d     onset %.4f quarters     length %d/%d of a whole note%n",
				index + 1, s.links.size(), n.abcName(), n.measure(), n.quarters(), n.rdurNum, n.rdurDen));
		b.append("printed fingering: ").append(n.fingering().isEmpty() ? "none" : n.fingering()).append('\n');
		if (n.chordSerial() >= 0) {
			List<String> mates = new ArrayList<String>();
			for (Link other : s.links) {
				if (other.note.chordSerial() == n.chordSerial()) {
					mates.add(other.note.abcName() + (other.note.fingering().isEmpty() ? "" : " (" + other.note.fingering() + ")"));
				}
			}
			b.append("chord ").append(n.chordSerial()).append(": ").append(mates).append('\n');
		} else {
			b.append("not in a chord\n");
		}
		b.append("properties:\n");
		for (Map.Entry<String, Object> e : new TreeMap<String, Object>(n.props).entrySet()) {
			b.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
		}
		return b.toString();
	}

	/** The pages stacked in one scrolling column, with the selected note and its chord outlined. */
	/** What the panel tells its owner: a note was clicked, or an arrow key asked for the next or previous one. */
	interface Picker {
		void pick(int index, boolean scroll);
	}

	private static class PagesPanel extends JComponent {
		private static final long serialVersionUID = 1L;
		MusicXmlSession session;
		TrailResult trails;
		int selected = -1;
		private int[] top = new int[0];
		private final Picker picker;

		PagesPanel(Picker picker) {
			this.picker = picker;
			setFocusable(true);
			setBackground(Color.WHITE);
			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					PagesPanel.this.picker.pick(hit(e.getX(), e.getY()), false);
				}
			});
			bindKey(KeyEvent.VK_RIGHT, 1);
			bindKey(KeyEvent.VK_LEFT, -1);
		}

		private void bindKey(int code, final int step) {
			String name = "step" + step;
			getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(code, 0), name);
			getActionMap().put(name, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					picker.pick(selected + step, true);
				}
			});
		}

		void setSession(MusicXmlSession s) {
			session = s;
			selected = -1;
			int y = GAP, w = 0;
			top = new int[s.notation.pages.size()];
			for (int i = 0; i < top.length; i++) {
				top[i] = y;
				BufferedImage im = s.notation.pages.get(i).image;
				y += im.getHeight() + GAP;
				w = Math.max(w, im.getWidth());
			}
			setPreferredSize(new Dimension(w, y));
			revalidate();
			repaint();
		}

		/** Index of the note whose box is nearest the point, or -1.  Complexity: O(notes). */
		int hit(int px, int py) {
			if (session == null) {
				return -1;
			}
			int best = -1;
			double bestD = 12 * 12;
			for (int i = 0; i < session.links.size(); i++) {
				NotationData.Drawn d = session.links.get(i).drawn;
				double cx = d.x + d.w / 2, cy = top[d.page] + d.y + d.h / 2;
				double dist = (cx - px) * (cx - px) + (cy - py) * (cy - py);
				if (dist < bestD) {
					bestD = dist;
					best = i;
				}
			}
			return best;
		}

		void scrollToSelected() {
			if (selected < 0) {
				return;
			}
			NotationData.Drawn d = session.links.get(selected).drawn;
			scrollRectToVisible(new Rectangle((int) d.x - 200, top[d.page] + (int) d.y - 250, 500, 500));
		}

		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0;
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (session == null) {
				return;
			}
			Rectangle clip = g.getClipBounds();
			for (int i = 0; i < top.length; i++) {
				BufferedImage im = session.notation.pages.get(i).image;
				if (top[i] + im.getHeight() >= clip.y && top[i] <= clip.y + clip.height) {
					paintPage(g, i, top[i]);
				}
			}
		}

		void paintPage(Graphics2D g, int page, int y0) {
			g.drawImage(session.notation.pages.get(page).image, 0, y0, null);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (trails != null) {
				for (int i = 0; i < session.links.size(); i++) {
					NotationData.Drawn d = session.links.get(i).drawn;
					if (d.page == page) {
						g.setColor(trails.colour(i));
						g.fillRoundRect((int) d.x - 3, y0 + (int) d.y - 3, (int) d.w + 6, (int) d.h + 6, 6, 6);
						int mark = trails.mark(i);
						if (mark != 0) {
							g.setColor(mark == 1 ? new Color(30, 150, 60) : new Color(200, 0, 160));
							g.fillOval((int) (d.x + d.w / 2) - 4, y0 + (int) d.y - 16, 8, 8);
						}
					}
				}
			}
			if (selected < 0) {
				return;
			}
			int serial = session.links.get(selected).note.chordSerial();
			for (int i = 0; i < session.links.size(); i++) {
				Link l = session.links.get(i);
				if (l.drawn.page != page) {
					continue;
				}
				boolean mate = serial >= 0 && l.note.chordSerial() == serial;
				if (i == selected || mate) {
					g.setColor(i == selected ? new Color(220, 30, 30) : new Color(240, 140, 20));
					g.setStroke(new BasicStroke(i == selected ? 3f : 2f));
					g.drawRoundRect((int) l.drawn.x - 4, y0 + (int) l.drawn.y - 4, (int) l.drawn.w + 8, (int) l.drawn.h + 8, 6, 6);
				}
			}
		}
	}

	/** The page that holds note `index`, with its outline and the note's data under it.  For checks without a window. */
	static BufferedImage snapshot(MusicXmlSession s, int index, TrailResult trails) {
		PagesPanel panel = new PagesPanel(null);
		panel.session = s;
		panel.trails = trails;
		panel.selected = index;
		int page = s.links.get(index).drawn.page;
		BufferedImage im = s.notation.pages.get(page).image;
		String[] text = (describe(s, index) + (trails == null ? "" : trails.describe(index))).split("\n");
		int lineH = 17, textH = Math.min(text.length, 14) * lineH + 12;
		BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight() + textH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		panel.paintPage(g, page, 0);
		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		for (int i = 0; i < Math.min(text.length, 14); i++) {
			g.drawString(text[i], 10, im.getHeight() + 18 + i * lineH);
		}
		g.dispose();
		return out;
	}

	public static void main(final String[] args) throws Exception {
		int png = java.util.Arrays.asList(args).indexOf("--png");
		if (png >= 0) {
			System.setProperty("java.awt.headless", "true");
			int noteArg = java.util.Arrays.asList(args).indexOf("--note");
			MusicXmlSession s = MusicXmlSession.open(new File(args[0]));
			int index = noteArg >= 0 ? Integer.parseInt(args[noteArg + 1]) : 0;
			TrailResult trails = java.util.Arrays.asList(args).contains("--trails") ? TrailResult.of(s) : null;
			ImageIO.write(snapshot(s, index, trails), "png", new File(args[png + 1]));
			System.out.println("wrote " + args[png + 1] + " (note " + index + ")");
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				NotationFrame f = new NotationFrame();
				f.setVisible(true);
				if (args.length > 0) {
					f.open(new File(args[0]));
				}
			}
		});
	}
}

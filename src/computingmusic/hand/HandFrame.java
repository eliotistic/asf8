package computingmusic.hand;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 * Inspect and set the hand.  Left: the board to scale, on which the fingers are placed with the mouse.  Right: the
 * instrument, the size of the hand, how high up the board to draw, which fingers may cross, and every reach limit
 * (comfortable and absolute, in mm) for each pair of fingers.  The verdict and the distance for each pair are shown.
 * Every limit is a chosen placeholder until measured (doc/HAND_MODEL.md).
 *
 *   make hand                          the window
 *   java ... computingmusic.hand.HandFrame --png FILE   draw the example to a file, no window
 */
public class HandFrame {
	private final HandPanel board = new HandPanel();
	private final JLabel verdict = new JLabel(" ");
	private final JTextArea detail = new JTextArea(7, 30);
	private final JLabel source = new JLabel(" ");
	private final JSpinner[] comfy = new JSpinner[6], abs = new JSpinner[6];
	private boolean updating;

	JPanel build() {
		JPanel content = new JPanel(new BorderLayout());
		content.add(board, BorderLayout.CENTER);
		JPanel side = new JPanel(new GridBagLayout());
		side.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(3, 0, 3, 0);
		c.anchor = GridBagConstraints.WEST;

		JComboBox<String> inst = new JComboBox<String>();
		for (Geometry g : Geometry.ALL) {
			inst.addItem(g.name);
		}
		inst.addActionListener(e -> {
			board.geometry = Geometry.ALL[inst.getSelectedIndex()];
			source.setText("<html><div style='width:330px'><i>" + board.geometry.provenance + "</i></div></html>");
			refresh();
		});
		side.add(row("Instrument", inst), c);
		c.gridy++;
		side.add(source, c);
		c.gridy++;

		JSlider size = new JSlider(50, 150, 100);
		JLabel sizeLabel = new JLabel("hand size x1.00");
		size.addChangeListener(e -> {
			board.params.size = size.getValue() / 100.0;
			sizeLabel.setText(String.format("hand size x%.2f", board.params.size));
			refresh();
		});
		side.add(sizeLabel, c);
		c.gridy++;
		side.add(size, c);
		c.gridy++;

		JSlider high = new JSlider(8, 30, board.maxHeight);
		JLabel highLabel = new JLabel("board drawn to height " + board.maxHeight);
		high.addChangeListener(e -> {
			board.maxHeight = high.getValue();
			highLabel.setText("board drawn to height " + board.maxHeight);
			refresh();
		});
		side.add(highLabel, c);
		c.gridy++;
		side.add(high, c);
		c.gridy++;

		JPanel fingers = new JPanel();
		ButtonGroup grp = new ButtonGroup();
		fingers.add(new JLabel("active finger"));
		for (int f = 1; f <= 4; f++) {
			final int ff = f;
			JRadioButton b = new JRadioButton(String.valueOf(f), f == 1);
			b.addActionListener(e -> {
				board.activeFinger = ff;
				refresh();
			});
			grp.add(b);
			fingers.add(b);
		}
		JButton lift = new JButton("lift");
		lift.addActionListener(e -> {
			board.placed[board.activeFinger] = null;
			refresh();
		});
		JButton clear = new JButton("clear");
		clear.addActionListener(e -> {
			for (int f = 1; f <= 4; f++) {
				board.placed[f] = null;
			}
			refresh();
		});
		fingers.add(lift);
		fingers.add(clear);
		side.add(fingers, c);
		c.gridy++;

		JCheckBox reach = new JCheckBox("show where the active finger can reach", true);
		reach.addActionListener(e -> {
			board.showReach = reach.isSelected();
			refresh();
		});
		JCheckBox cross = new JCheckBox("fingers may cross (a higher finger nearer the nut)");
		cross.addActionListener(e -> {
			board.params.fingersMayCross = cross.isSelected();
			refresh();
		});
		side.add(reach, c);
		c.gridy++;
		side.add(cross, c);
		c.gridy++;

		// the limits
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		side.add(new JLabel("reach limit, mm"), c);
		c.gridx = 1;
		side.add(new JLabel("comfortable"), c);
		c.gridx = 2;
		side.add(new JLabel("absolute"), c);
		c.gridy++;
		for (int k = 0; k < 6; k++) {
			final int kk = k;
			comfy[k] = spinner(board.params.comfortable[k], v -> board.params.comfortable[kk] = v);
			abs[k] = spinner(board.params.absolute[k], v -> board.params.absolute[kk] = v);
			c.gridx = 0;
			side.add(new JLabel("fingers " + HandParams.PAIRS[k][0] + "-" + HandParams.PAIRS[k][1]), c);
			c.gridx = 1;
			side.add(comfy[k], c);
			c.gridx = 2;
			side.add(abs[k], c);
			c.gridy++;
		}
		JSpinner minC = spinner(board.params.minSameStringComfortable, v -> board.params.minSameStringComfortable = v);
		JSpinner minA = spinner(board.params.minSameStringAbsolute, v -> board.params.minSameStringAbsolute = v);
		c.gridx = 0;
		side.add(new JLabel("closest on one string"), c);
		c.gridx = 1;
		side.add(minC, c);
		c.gridx = 2;
		side.add(minA, c);
		c.gridy++;

		c.gridx = 0;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		JLabel note = new JLabel("<html><i>All limits are chosen placeholders, not measurements.</i></html>");
		side.add(note, c);
		c.gridy++;
		verdict.setFont(verdict.getFont().deriveFont(Font.BOLD, 16f));
		side.add(verdict, c);
		c.gridy++;
		detail.setEditable(false);
		detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		side.add(detail, c);
		c.gridy++;
		JButton example = new JButton("example: the opening arpeggio (4 3 1)");
		example.addActionListener(e -> {
			board.placed[4] = new int[] {0, 9};
			board.placed[3] = new int[] {1, 6};
			board.placed[1] = new int[] {2, 2};
			board.placed[2] = null;
			refresh();
		});
		side.add(example, c);

		side.setPreferredSize(new Dimension(390, 760));
		content.add(side, BorderLayout.EAST);
		board.setOnChange(this::refresh);
		source.setText("<html><div style='width:330px'><i>" + board.geometry.provenance + "</i></div></html>");
		refresh();
		return content;
	}

	private interface Setter {
		void set(double v);
	}

	private JSpinner spinner(double value, Setter s) {
		JSpinner sp = new JSpinner(new SpinnerNumberModel(value, 1.0, 400.0, 1.0));
		sp.addChangeListener(e -> {
			if (!updating) {
				s.set(((Number) sp.getValue()).doubleValue());
				refresh();
			}
		});
		return sp;
	}

	private static JPanel row(String label, Component c) {
		JPanel p = new JPanel(new BorderLayout(6, 0));
		p.add(new JLabel(label), BorderLayout.WEST);
		p.add(c, BorderLayout.CENTER);
		return p;
	}

	private void refresh() {
		HandModel.Report r = board.report();
		if (board.placements().isEmpty()) {
			verdict.setText("place a finger on the board");
			verdict.setForeground(Color.gray);
		} else {
			String word = r.level == HandModel.Level.COMFORTABLE ? "comfortable" : r.level == HandModel.Level.POSSIBLE ? "possible, beyond comfort" : "impossible";
			verdict.setText(word);
			verdict.setForeground(HandPanel.color(r.level));
		}
		StringBuilder sb = new StringBuilder();
		for (HandModel.PairReport p : r.pairs) {
			sb.append(String.format("%d-%d  %5.1f mm  (comfortable %.0f, absolute %.0f)  %s%s%n", p.a, p.b, p.distance, p.comfortableMax,
					p.absoluteMax, p.level.toString().toLowerCase(), p.note.isEmpty() ? "" : "  - " + p.note));
		}
		detail.setText(sb.toString());
		board.repaint();
	}

	/** Lay a component tree out and paint it to an image, without a window. */
	static void layoutAll(Component c) {
		c.doLayout();
		if (c instanceof Container) {
			for (Component k : ((Container) c).getComponents()) {
				layoutAll(k);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length >= 2 && args[0].equals("--png")) {
			System.setProperty("java.awt.headless", "true");
			HandFrame f = new HandFrame();
			final JPanel[] content = new JPanel[1];
			SwingUtilities.invokeAndWait(() -> {
				content[0] = f.build();
				f.board.placed[4] = new int[] {0, 9};
				f.board.placed[3] = new int[] {1, 6};
				f.board.placed[1] = new int[] {2, 2};
				f.board.activeFinger = 2;
				f.refresh();
				content[0].setSize(new Dimension(1100, 780));
				layoutAll(content[0]);
				BufferedImage im = new BufferedImage(1100, 780, BufferedImage.TYPE_INT_RGB);
				java.awt.Graphics2D g = im.createGraphics();
				g.setColor(Color.white);
				g.fillRect(0, 0, 1100, 780);
				content[0].paint(g);
				g.dispose();
				try {
					ImageIO.write(im, "png", new File(args[1]));
				} catch (java.io.IOException e) {
					throw new RuntimeException(e);
				}
			});
			return;
		}
		SwingUtilities.invokeLater(() -> {
			HandFrame f = new HandFrame();
			JFrame w = new JFrame("Hand model");
			w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			w.setContentPane(f.build());
			w.pack();
			w.setLocationByPlatform(true);
			w.setVisible(true);
		});
	}
}

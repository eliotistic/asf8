package computingmusic;

import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public final class ControlsHelpDialog {
	private ControlsHelpDialog() {
	}

	public static void doDialog(JFrame frame) {
		JEditorPane help = new JEditorPane("text/html", helpText());
		help.setEditable(false);
		help.setCaretPosition(0);
		JScrollPane scrollPane = new JScrollPane(help);
		scrollPane.setPreferredSize(new Dimension(650, 500));
		JOptionPane.showMessageDialog(frame, scrollPane, "String Fingering Controls",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private static String helpText() {
		return "<html><body style='font-family:sans-serif'>"
				+ "<h1>String Fingering controls</h1>"
				+ "<h2>Open a score</h2>"
				+ "<p>Drop one <code>.abc</code> file anywhere on the window, or use "
				+ "<b>File &rarr; Analyze ABC file</b>.</p>"
				+ "<h2>Manual chord</h2>"
				+ "<p>Enter up to four pitches separated by spaces (for example, "
				+ "<code>g5 bf5 d6</code>) and click <b>finger</b>. The small arrows "
				+ "browse alternative fingerings.</p>"
				+ "<h2>Score navigation</h2>"
				+ "<p>The four arrows under the score go to the beginning, previous event, "
				+ "next event, and end. Right-click the next arrow to jump past the current "
				+ "extended passage.</p>"
				+ "<h2>Extend and Shrink</h2>"
				+ "<p><b>Extend</b> adds the next score event while retaining only physically "
				+ "compatible fingering trails. <b>Shrink</b> removes the last event from that "
				+ "trail. <b>Solve</b> extends all compatible trails as far as possible.</p>"
				+ "<h2>Fingerboards</h2>"
				+ "<p>Double-click a fingerboard to remove it and every board to its right. "
				+ "Glue locks the displayed fingering; Unglue restores alternatives. Use "
				+ "<b>Boards &rarr; Reset Score and Panels</b> to return to the start and "
				+ "remove Solve boxes and other temporary highlighting.</p>"
				+ "<p>Hover over any control for a more specific description. The complete "
				+ "guide is in <code>docs/controls.md</code>.</p>"
				+ "</body></html>";
	}
}

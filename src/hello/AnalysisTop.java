package hello;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class AnalysisTop extends JPanel {
	
	private static final long serialVersionUID = 1L;
	Cfinger2 f;
	AnalysisPanel a;
	JFrame frame;
	public AnalysisTop() {
		setLayout(new BorderLayout());
		// setLayout(this, new BoxLayout.X_AXIS);
		// Add content to the window.
		f = new Cfinger2();
		a = new AnalysisPanel(f);
		add("West", f);
		add("East", a);

	}

	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				JFrame frame = new JFrame("Analysis");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				// BoxLayout g = new BoxLayout(frame, BoxLayout.X_AXIS);
				// Add content to the window.
				AnalysisTop analysisTop = new AnalysisTop();
				frame.add(analysisTop); // frame.add(new AnalysisPanel());
				// Display the window.
				AMenuBar menu = new AMenuBar(frame, analysisTop);
				frame.setJMenuBar(menu);
				
				frame.pack();
				frame.setVisible(true);

			}
		});
	}

	
	
}

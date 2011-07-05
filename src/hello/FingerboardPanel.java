package hello;

import javax.swing.*;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class FingerboardPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	JCheckBox fingerTrails;
	
	
	public FingerboardPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder());
		fingerTrails = new JCheckBox("Fingering trails");
		add(fingerTrails);
		add(Box.createVerticalGlue());
		fingerTrails.setSelected(false);
	}
}

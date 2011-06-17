package hello;

import java.awt.*;
import javax.swing.*;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class FingerboardPanel extends JPanel {
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

package hello;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GluePanel extends JPanel {
	JButton unglue;
	JButton glue;
	
	GluePanel(){
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		glue = new JButton("Glue");
		unglue = new JButton("Unglue");
		
		
		
		
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(unglue);
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(glue);
		add(Box.createRigidArea(new Dimension(10, 0)));
		//botbut.add(Box.createHorizontalGlue());
		
		setAlignmentX(Component.CENTER_ALIGNMENT);
		
	}
	public void setButtons(boolean on, boolean off) {
		glue.setEnabled(on);
		unglue.setEnabled(off);
		
	}
	
	
	public void setGlue (boolean v){
		glue.setEnabled(v);
		glue.setEnabled(!v);
	}
	
	
}

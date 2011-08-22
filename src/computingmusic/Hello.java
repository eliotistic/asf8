package computingmusic;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;

public class Hello extends JPanel{

	private static final long serialVersionUID = 1L;
	public JLabel message;
	String base = "";
	Hello () {
		
		message = new JLabel();
		
		add(message);
		this.setMaximumSize(new Dimension(70, 60));
		// this.setPreferredSize(new Dimension(100, 32));
	}
	Hello (String s) {	//used.
		base = s;
		message = new JLabel(s);
		
		setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		c1.gridx = 0;
		c1.gridy = 0;
		c1.insets = new Insets(10, 10, 10, 10);
		add(message, c1);
		//this.setMaximumSize(new Dimension(70, 60));
		// this.setPreferredSize(new Dimension(100, 32));
	}
	
	public void setInit(String s){
		base = s;
	}
	public void clear (){
		say(base);
	}
	public void say (String s) {
		message.setText(s);
		
	}
}


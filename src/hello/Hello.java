package hello;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JLabel;

public class Hello extends JPanel{
	public JLabel message;
	String base = "";
	Hello () {
		
		message = new JLabel();
		
		add(message);
		// this.setPreferredSize(new Dimension(100, 32));
	}
	Hello (String s) {	
		base = s;
		message = new JLabel(s);
		
		add(message);
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


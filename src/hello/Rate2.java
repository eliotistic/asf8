package hello;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Rate2 extends JPanel {
	Instrument instrument;
	Cfinger2 cfinger2;
	JPanel top;
	Hello yak;
	JLabel curr;
	GridBagConstraints c;
	
	String[] buttnames = {"easy", "playable", "awkward", "impossible"};
	String[] buttinfo = {"easy", "playable", "awkward", "impossible"};
	JButton[] butts = makeButts();
	
	
	private void setState(Boolean b) {
		for (JButton butt : butts){
			butt.setEnabled(b);
		}
	}
	
	public void maybeEnable(int stops){
		if (stops > 1) {
			enable();
		} else {disable();
		}
			
	}
	public void enable() {
		setState(true);
		yak.say("This fingering is: ");
	}

	public void disable() {
		setState(false);
		yak.say("<html><font color=gray>Rate playability</font></html>");
	}

	public void clear() {
		disable();

	}
	
	public JButton[] makeButts () {
		int len = buttnames.length;
		JButton[] butts = new JButton[len];
		for (int i=0; i<len; i++ ){
		
			butts[i] = new JButton(buttnames[i]);
			
			
			}
		
		butts[0].addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				logInfo(0); }});
		butts[1].addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				logInfo(1); }});
		butts[2].addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				logInfo(2); }});
		butts[3].addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				logInfo(3); }});
		return butts;
	};
			
			
	public Rate2(Cfinger2 c) {
		setBorder(BorderFactory.createEtchedBorder());
		setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		//setLayout(new GridBagLayout());
		//c = new GridBagConstraints();
		top = new JPanel();
		
		yak = new Hello();
		//JButton [] butts = makeButts();
		
		
		JPanel buttpanel = new JPanel (new GridLayout(2,2,5,5));
		for (JButton j : butts) {
			buttpanel.add(j);
		}
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		this.add(yak);
		this.add(buttpanel);
		
		
		disable();

	}

	private void logInfo(int code) {
		// switch off buttons
		// veryhardbutton.setEnabled(false);
		// impossiblebutton.setEnabled(false);
		// res says whether we managed to talk to the CGI.

		String thanks = "<html><font color=green>" + "Thank You!"
				+ "</font></html>";
		try {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			String x = "";
			
			boolean res = Logger.log("logger", buttinfo[code] + ","
					+ ""// Fing.fingeringResult.logString()
					);
			if (res) {
				yak.say(thanks);
			} else {
				yak.say("<html><font color=red>Connection error!</font></html>");

			}

		} finally {
			this.setCursor(Cursor.getDefaultCursor());
		}
		setState(false);

	}

}

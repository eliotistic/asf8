package computingmusic;

//import com.sun.
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CtlBox extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	//Graphical components, in order of vertical display.
	InstPanel instPanel;
	AbcChord2 abcChord;
	Hello topo;
	Hello msg;
	EntryPanel3 entryPanel;
	GluePanel gluePanel;
	JButton showGlue;
	JButton freeze;
	JButton save;
	JButton load;
	JLabel currentTextDisplay; // jb
	
	RangeImage rangeImage;
	Rate2 rate;
	JFrame sf;
	JPanel sP;
	JButton survey;
	Boolean tookSurvey;
	boolean surveyWinOpen;
	
	
	
	//FingerboardPanel fbp;
	//JMenuBar menuBar;
	//InstMenu instMenu;
	//JMenu aboutMenu;
	
	
	

	public CtlBox(Cfinger2 c) {
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		// GridLayout g = new GridLayout(0,1,0,30);

		//BoxLayout g = new BoxLayout(this, BoxLayout.Y_AXIS);
		//this.setLayout(g);
		setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();
		GridBagConstraints c4 = new GridBagConstraints();
		GridBagConstraints c5 = new GridBagConstraints();
		GridBagConstraints c6 = new GridBagConstraints();
		GridBagConstraints c7 = new GridBagConstraints();
		GridBagConstraints c8 = new GridBagConstraints();
		GridBagConstraints c9 = new GridBagConstraints();
		GridBagConstraints c10 = new GridBagConstraints();
		GridBagConstraints c11 = new GridBagConstraints();
		
		
		//this.setM
		abcChord = new AbcChord2(c);
		entryPanel = new EntryPanel3();
		instPanel = new InstPanel();
		gluePanel = new GluePanel();
		showGlue = new JButton("Show glue");
		currentTextDisplay = new JLabel();//jb
		freeze = new JButton("Freeze");
		save = new JButton("Save");
		load = new JButton("Load");
		
		
		
		//currentTextDisplay.setText("Nothing to say.");
		
		rate = new Rate2(c);
		msg = new Hello("Enter up to 4 notes:"); // Hello();
		topo = new Hello("Topology:");
		survey = new JButton("Very brief survey");
		
		instPanel.setMinimumSize(new Dimension(200, 20));

		c1.gridx = 0;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.BOTH;
		c1.weightx = 0;
		c1.weighty = 0.1;
		//c1.insets = new Insets(0, 20, 0, 20);
		//c1.ipadx = 150;
		c1.anchor = GridBagConstraints.PAGE_END;
		add(instPanel, c1);
		
		c2.gridx = 0;
		c2.gridy = 1;
		c2.fill = GridBagConstraints.BOTH;
		c2.weightx = 0;
		c2.weighty = 0.5;
		//c2.insets = new Insets(0, 10, 0, 0);
		//c2.ipadx = 50;
		//c2.ipady = 50;
		add(abcChord, c2);
		
		c3.gridx = 0;
		c3.gridy = 2;
		c3.fill = GridBagConstraints.BOTH;
		c3.weightx = 0;
		c3.weighty = 0.3;
		c3.anchor = GridBagConstraints.PAGE_START;
		add(topo, c3);
		
		c4.gridx = 0;
		c4.gridy = 3;
		c4.fill = GridBagConstraints.VERTICAL;
		c4.weightx = 0;
		c4.weighty = 0.3;
		c4.anchor = GridBagConstraints.LINE_START;
		add(msg, c4);
		
		c5.gridx = 0;
		c5.gridy = 4;
		c5.fill = GridBagConstraints.VERTICAL;
		c5.weightx = 0.5;
		c5.weighty = 0.25;
		add(entryPanel, c5);
		
		c6.gridx = 0;
		c6.gridy = 5;
		c6.fill = GridBagConstraints.VERTICAL;
		c6.weightx = 0;
		c6.weighty = 0.1;
		add(gluePanel, c6);
		
		c7.gridx = 0;
		c7.gridy = 6;
		c7.fill = GridBagConstraints.VERTICAL;
		c7.weightx = 0;
		c7.weighty = 0.1;
		add(showGlue, c7);
		
		c8.gridx = 0;
		c8.gridy = 7;
		c8.fill = GridBagConstraints.VERTICAL;
		c8.weightx = 0;
		c8.weighty = 0.1;
		add(freeze, c8);
		
		c9.gridx = 0;
		c9.gridy = 8;
		c9.fill = GridBagConstraints.VERTICAL;
		c9.weightx = 0;
		c9.weighty = 0.1;
		add(save, c9);
		
		c10.gridx = 0;
		c10.gridy = 9;
		c10.fill = GridBagConstraints.VERTICAL;
		c10.weightx = 0;
		c10.weighty = 0.1;
		add(load, c10);
		
		c11.gridx = 0;
		c11.gridy = 10;
		c11.fill = GridBagConstraints.VERTICAL;
		c11.weightx = 0;
		c11.weighty = 0.1;
		add(currentTextDisplay, c11);
		
		
		sf = new JFrame("Survey says ...");
		JPanel ss = new surveyForm(sf, this);
		sf.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				sf.setVisible(false);
			}
		});
		
		sf.add(ss);
		sf.pack();
		survey.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// System.out.println("SURVEY SAYS:");

				sf.setVisible(true);

			}
		});

		////add(Box.createRigidArea(new Dimension(0, 300)));
		//add(Box.createVerticalGlue());
		//// msg.say("Enter 2,3 or 4 notes:");
	
	}

	public void clear() {
		// next.disable();
	}

}

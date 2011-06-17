package hello;

import java.awt.Dimension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class StepMode extends JPanel implements ActionListener {
	BigScore big;
	
	ButtonGroup stepBG;
	
	JRadioButtonMenuItem EXTEND_FORWARD;
	JRadioButtonMenuItem STEP1;
	//JRadioButtonMenuItem STEP2;
	//JRadioButtonMenuItem vcCB;
	//ButtonGroup steps;
	
	
	StepMode(BigScore b){
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEtchedBorder());
		
		big = b;
		stepBG = new ButtonGroup();
		STEP1 = new JRadioButtonMenuItem("Step 1");
		//STEP2 = new JRadioButtonMenuItem("Step 2");
		EXTEND_FORWARD =new JRadioButtonMenuItem("Extend forward");
		STEP1.setActionCommand("STEP1");
		//STEP2.setActionCommand("STEP2");
		EXTEND_FORWARD.setActionCommand("EXTEND_FORWARD");
		
		stepBG.add(STEP1);
		//stepBG.add(STEP2);
		stepBG.add(EXTEND_FORWARD);
		add(STEP1);
		//add(STEP2);
		add(EXTEND_FORWARD);
		
		STEP1.setSelected(true);
		
		STEP1.addActionListener(this);
		//STEP2.addActionListener(this);
		EXTEND_FORWARD.addActionListener(this);
		
		//big.setStepValue("STEP1");
		
	}
	
	
	
	
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	public Dimension getPreferredSize() {
		return new Dimension(super.getPreferredSize().width, 36);
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		//if (e.getActionCommand())
		//big.setStepValue(e.getActionCommand());
		
	}

		
}

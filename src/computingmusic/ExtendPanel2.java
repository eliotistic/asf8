package computingmusic;


import javax.swing.*;

import java.awt.*;
import javax.swing.SwingUtilities;

public class ExtendPanel2 extends JPanel{
	
	private static final long serialVersionUID = 1L;
	public GluePanel gluepanel;
	public JPanel exTop;
	public JPanel exBot;
	
	
	public JButton shrink;
	public JButton extend;
	public JButton clear;
	public JCheckBox extendForward;
	public JComboBox extendMode;
	//public JCheckBox extendJump;
	
	public JButton positions;
	public ArrowKeys posArrows;
	
	public JPanel excxPanel; // checkboxes
	public JPanel poscxPanel; // checkboxes
	
	public JCheckBox positionRestrict;
	public JCheckBox fingerTrails;
	
	public JButton solve;
	public JButton clearSolvesBtn;
	
	public void disable (){
		extend.setEnabled(false);
		shrink.setEnabled(false);
		clear.setEnabled(false);
		
	}
	
	public void disablePositions (){
		
		positionRestrict.setEnabled(false);
		positionRestrict.setSelected(false);
		
		positions.setEnabled(false);
		posArrows.next.setEnabled(false);
		posArrows.prev.setEnabled(false);
	}
	
	
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

	
	
	
	ExtendPanel2(){
		//setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBorder(BorderFactory.createEtchedBorder());
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		exTop = new JPanel();
		exBot = new JPanel();
		poscxPanel = new JPanel();
		excxPanel = new JPanel();

		
		exTop.setLayout(new BoxLayout(exTop, BoxLayout.X_AXIS));
		exBot.setLayout(new BoxLayout(exBot, BoxLayout.X_AXIS));
		poscxPanel.setLayout(new BoxLayout(poscxPanel, BoxLayout.Y_AXIS));
		excxPanel.setLayout(new BoxLayout(excxPanel, BoxLayout.Y_AXIS));
		//excxPanel.setAlignmentY(RIGHT_ALIGNMENT);
		exTop.setBorder(BorderFactory.createEtchedBorder());
		exBot.setBorder(BorderFactory.createEtchedBorder());
		poscxPanel.setBorder(BorderFactory.createEtchedBorder());
		
		//setBackground(Color.white);
		//ImageIcon prevIcon = createImageIcon("/resource/left.gif");
		//ImageIcon nextIcon = createImageIcon("/resource/right.gif");
		clear = new JButton("Clear");
		clear.setHorizontalTextPosition(AbstractButton.LEADING);
		shrink = new JButton("Shrink");
		extend = new JButton("Extend");
		//gluepanel = new GluePanel();
		// System.out.println("Made glue panel");
		
		
		
		extendForward = new JCheckBox("Extend forward");
		//extendForward.setHorizontalTextPosition(AbstractButton.LEFT_ALIGNMENT);
		
		
		String[] modes = { "Max chord","Least common" };;
		extendMode = new JComboBox(modes);
		//extendMode.setAlignmentX(LEFT_ALIGNMENT);
		//extendJump = new JCheckBox("Jump");
		
		
		excxPanel.add(extendForward);
		excxPanel.add(extendMode);
		
		positions = new JButton("Positions");
		posArrows = new ArrowKeys(false);
		
		solve = new JButton("Solve");
		clearSolvesBtn = new JButton("Clear solutions");
		
		positionRestrict = new JCheckBox("restrict to position");
		fingerTrails = new JCheckBox("Fingering trails");
		//JPanel botbut = new JPanel();
		//positionRestrict.addActionListener();
		
		
		
		//setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		//setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		exTop.add(Box.createRigidArea(new Dimension(10, 0)));
		exTop.add(clear);
		exTop.add(Box.createRigidArea(new Dimension(10, 0)));
		exTop.add(shrink);
		exTop.add(Box.createRigidArea(new Dimension(10, 0)));
		exTop.add(extend);
		exTop.add(Box.createRigidArea(new Dimension(10, 0)));
		//exTop.add(gluepanel);
		//exTop.add(excxPanel);
		//exTop.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		//add(Box.createRigidArea(new Dimension(10, 0)));
		//botbut.add(Box.createHorizontalGlue());
		exBot.add(Box.createRigidArea(new Dimension(10, 0)));
		exBot.add(positions);
		exBot.add(Box.createRigidArea(new Dimension(10, 0)));
		exBot.add(posArrows);
		//exBot.add(Box.createRigidArea(new Dimension(10, 0)));
		
		
		
		
		
		//poscxPanel.add(positionRestrict);
		//poscxPanel.add(fingerTrails);
		
		poscxPanel.add(solve);
		poscxPanel.add(clearSolvesBtn);
		
		exBot.add(Box.createRigidArea(new Dimension(10, 0)));
		exBot.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		exBot.add(poscxPanel);
		
		add(exTop);
		add(exBot);
		
		disable();
		disablePositions();
	}
	
	
	protected  ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = this.getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, 20);
	}

	
	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				JFrame frame = new JFrame("Extend2 Panel");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				//BoxLayout g = new BoxLayout(frame, BoxLayout.X_AXIS);
				// Add content to the window.
				frame.add(new ExtendPanel2());
				//frame.add(new Cfinger2());

				// Display the window.
				frame.pack();
				frame.setVisible(true);

			}
		});
	}
}


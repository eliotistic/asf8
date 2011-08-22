package computingmusic;


import javax.swing.*;

import java.awt.*;
import javax.swing.SwingUtilities;

public class ExtendPanel extends JPanel{
	
	private static final long serialVersionUID = 1L;
	public JPanel ExTop;
	public JPanel ExBot;
	
	public JButton shrink;
	public JButton extend;
	public JButton clear;
	//private JComboBox extendMode;
	
	public JButton positions;
	public JCheckBox positionRestrict;
	
	public void disable (){
		extend.setEnabled(false);
		shrink.setEnabled(false);
		clear.setEnabled(false);
	}
	ExtendPanel(){
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBorder(BorderFactory.createEtchedBorder());
		
		//setBackground(Color.white);
		//ImageIcon prevIcon = createImageIcon("/resource/left.gif");
		//ImageIcon nextIcon = createImageIcon("/resource/right.gif");
		clear = new JButton("Clear");
		clear.setHorizontalTextPosition(AbstractButton.LEADING);
		shrink = new JButton("Shrink");
		extend = new JButton("Extend");
		positions = new JButton("Positions");
		positionRestrict = new JCheckBox("restrict to position");
		//JPanel botbut = new JPanel();
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
		add(clear);
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(shrink);
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(extend);
	
		
		
		add(Box.createRigidArea(new Dimension(10, 0)));
		//botbut.add(Box.createHorizontalGlue());
		add(positions);
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(positionRestrict);
		setAlignmentX(Component.CENTER_ALIGNMENT);
		
		disable();
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
	
	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				JFrame frame = new JFrame("Extend Panel");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				//BoxLayout g = new BoxLayout(frame, BoxLayout.X_AXIS);
				// Add content to the window.
				frame.add(new ExtendPanel());
				//frame.add(new Cfinger2());

				// Display the window.
				frame.pack();
				frame.setVisible(true);

			}
		});
	}
}

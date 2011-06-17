


package hello;

//import java.awt.Dimension;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;
//import java.awt.event.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/*
 * Main class. appFrame is the main Frame for the project. 
 */
public class BigGui extends JFrame {
	
	private static final long serialVersionUID = 1L;
	static String version = Version.version;
	public Cfinger2 cfinger; // TODO I (jb) removed the static; any remarquable difference?
	public static BigScore bigScore;
	public static boolean doTransitions = false;
	
	public static FMenuBar menu;
	
	//jb
	private boolean arrowsOfRightPanelToKeyboard = true; // if false, then the keyboard maps to the arrows of left panel.
	
	BigGui (){
		/*this = new JFrame("String fingering v" 
				+ version);*/
		this.setTitle("String fingering v" + version);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		//this.setLayout(new BorderLayout());
		//jb
		this.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		//GridBagConstraints c3 = new GridBagConstraints();
		// end jb
		
		cfinger = new Cfinger2();
		bigScore = new BigScore(this, cfinger);
		
		//Create the effect of focus on borders TODO
		bigScore.arrows.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                bigScore.requestFocusInWindow();
                arrowsOfRightPanelToKeyboard = true;
                bigScore.arrows.setBorder(BorderFactory.createLineBorder(Color.black));
				cfinger.ctl.entryPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
            }
        });
		cfinger.ctl.entryPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                cfinger.requestFocusInWindow();
                arrowsOfRightPanelToKeyboard = false;
                bigScore.arrows.setBorder(BorderFactory.createLineBorder(Color.gray));
				cfinger.ctl.entryPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            }
        });
		
		
		
		// implement the arrows
		bigScore.addKeyListener(new KeyListener()
		{
			@Override
			public void keyPressed(KeyEvent evt) {
				//System.out.println("key pressed");
				//System.out.println("Line 1");
				//System.out.println("Key code: " + evt.getKeyCode());
				//System.out.println("VKRight: " + KeyEvent.VK_RIGHT);
				if(evt.getKeyCode() == KeyEvent.VK_RIGHT)
				{
					if(bigScore.arrows.next.isEnabled())
					{
						bigScore.nextArrowActionPerformed();
					}
				}
				else if(evt.getKeyCode() == KeyEvent.VK_LEFT)
				{
					if(bigScore.arrows.prev.isEnabled())
					{
						bigScore.prevArrowActionPerformed();
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				//System.out.println("key released");
			}
			@Override
			public void keyTyped(KeyEvent arg0) {
				//char typed = arg0.getKeyChar();
				//System.out.println("The read character in keyTyped is: " + typed);
			}
			
		});
		cfinger.addKeyListener(new KeyListener()
		{
			@Override
			public void keyPressed(KeyEvent evt) {
				//System.out.println("key pressed");
				//System.out.println("Line 1");
				if(evt.getKeyCode() == KeyEvent.VK_RIGHT)
				{
					if(cfinger.ctl.entryPanel.nextBtn.isEnabled())
					{
						cfinger.nextFingering();
					}
				}
				else if(evt.getKeyCode() == KeyEvent.VK_LEFT)
				{
					if(cfinger.ctl.entryPanel.prevBtn.isEnabled())
					{
						cfinger.prevFingering();
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent arg0) {
				//System.out.println("key released");
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
				//char typed = arg0.getKeyChar();
				//System.out.println("The read character in keyTyped is: " + typed);
			}
			
		});
		
		
		cfinger.setScore(bigScore);
		//Instrument ins = new Instrument("VN");
		
		//cfinger.setInstrument(ins);
		
		//Fing.localInstrument = ins;
		//cfinger.ctl.inst.rangeLabel.setText(ins.rangeString());
		
		
		JMenuBar menu = new FMenuBar(this, cfinger, bigScore);
		
		/*singlePanel.add("West",cfinger);
		
		//frame.add("East",bigScore);
		singlePanel.add("Center",bigScore);
		
		this.setJMenuBar(menu);
		this.add(singlePanel);
		this.pack();
		this.setVisible(true);
		singlePanel.requestFocusInWindow();*/
		
		
		//jb
		c1.gridx=0;
		c1.gridy=0;
		c1.weightx = 0;
		c1.weighty = 0.5;
		c1.fill = GridBagConstraints.BOTH;
		this.add(cfinger, c1);
		
		c2.gridx = 1;
		c2.gridy = 0;
		c2.weightx = 0.7;
		c2.weighty = 0.5; // TODO made cfinger stretch vertically??
		c2.fill = GridBagConstraints.BOTH;
		this.add(bigScore, c2);
		
		
		
		
		
		
		this.setJMenuBar(menu);
		
		this.pack();
		this.setVisible(true);
		//this.bigScore.requestFocusInWindow(); // victory!! 
		//Version.check();
	}
		 
		
	public void  showAboutDialog()
	{
		//AboutDialog.doDialog(this) ;
	}

	


	public void setArrowsOfRightPanelToKeyboard(boolean arrowsOfRightPanelToKeyboard) {
		this.arrowsOfRightPanelToKeyboard = arrowsOfRightPanelToKeyboard;
	}


	public boolean isArrowsOfRightPanelToKeyboard() {
		return arrowsOfRightPanelToKeyboard;
	}
	
	public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new BigGui();
			}
		});

	}
}






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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/*
 * Main class. appFrame is the main Frame for the project. 
 */
public class BigGui extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private static final String USER_HOME = "user.home";
	private static final String PROPERTY_FILE_NAME = ".stringfingering";
	private static final String PROP1_PATH = "userLikesTurtles";
	private static final String ANCHORS_PATH = "anchorsOn";
	private static final String SETTINGS_SF_TOOL = "Settings for the StringFingering tool";
	
	public boolean prop_like_turtles;
	public boolean prop_anchors_on;
	
	static String version = Version.version;
	public Cfinger2 cfinger; // I(jb) removed the static; any remarkable difference?
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
		
		loadProperties();
		
		cfinger = new Cfinger2();
		bigScore = new BigScore(this, cfinger);
		
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
		c1.weightx = 0.1;
		c1.weighty = 0.5;
		c1.fill = GridBagConstraints.BOTH;
		this.add(cfinger, c1);
		
		c2.gridx = 1;
		c2.gridy = 0;
		c2.weightx = 0.7;
		c2.weighty = 0.5; // made cfinger stretch vertically??
		c2.fill = GridBagConstraints.BOTH;
		this.add(bigScore, c2);
		
		
		
		
		
		
		this.setJMenuBar(menu);
		
		this.pack();
		this.setVisible(true);
		//Version.check();
	}
	
	private void loadProperties()
	{
		String homeDir = System.getProperty(USER_HOME);
        File propertiesFile = new File(homeDir + File.separator + PROPERTY_FILE_NAME);
        Properties sfProperties = new Properties();
        if(propertiesFile.exists()) // TODO : If the file is there, but properties are null
        {
        	String boolTurtles = "false";
        	String boolAnchors = "false";
        	try
	        {
	        	sfProperties.load(new FileReader(propertiesFile));
	        	
	        	boolTurtles = (String) sfProperties.get(PROP1_PATH);
	        	boolAnchors = (String) sfProperties.get(ANCHORS_PATH);
	        	sfProperties.store(new FileWriter(propertiesFile), SETTINGS_SF_TOOL);
	        }
	        catch(IOException e)
	        {
	        	System.out.println("The boolean userLikesTurtles could not be stored as true, but File was detected");
	            JOptionPane.showMessageDialog(this, "The boolean userLikesTurtles could not be stored, and " +
	            		"the File should already exist",
	            		"Error", 0);
	            e.printStackTrace();
	        }
	        prop_like_turtles = Boolean.parseBoolean(boolTurtles);
	        System.out.println("Here, boolAnchors (String) is: " + boolAnchors);
	        prop_anchors_on = Boolean.parseBoolean(boolAnchors);
        }
        else 
        {
        	try
	        {
	        	sfProperties.setProperty(PROP1_PATH, "false");
	        	sfProperties.setProperty(ANCHORS_PATH, "false");
	        	sfProperties.store(new FileWriter(propertiesFile), SETTINGS_SF_TOOL);
	        }
	        catch(IOException e)
	        {
	            System.out.println("The boolean userLikesTurtles could not be stored");
	            JOptionPane.showMessageDialog(this, "The boolean userLikesTurtles could not be stored, and " +
	            		"the File should be created",
	            		"Error", 0);
	            e.printStackTrace();
	        }
	        prop_like_turtles = false;
	        prop_anchors_on = false;
        }
	}
	
	public void setPropAnchors(boolean anchorsOn)
	{
		String homeDir = System.getProperty(USER_HOME);
        File propertiesFile = new File(homeDir + File.separator + PROPERTY_FILE_NAME);
        Properties swevizProperties = new Properties();
        if(propertiesFile.exists())
        {
        	try
	        {
	        	swevizProperties.load(new FileReader(propertiesFile));
	        	swevizProperties.setProperty(ANCHORS_PATH, anchorsOn ? "true" : "false");
	        	swevizProperties.store(new FileWriter(propertiesFile), SETTINGS_SF_TOOL);
	        }
	        catch(IOException e)
	        {
	            System.out.println("The boolean ANCHORS could not be stored");
	            JOptionPane.showMessageDialog(this, "The boolean ANCHORS could not be stored, and " +
	            		"the File was detected and loaded",
	            		"error", 0);
	            e.printStackTrace();
	        }
        }
        else 
        {
        	System.err.println("The properties file was not found");
        } 
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



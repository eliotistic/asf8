

//doCurrent, next and prev in cfinger (ONLY LEFT BOARD)
//viewTrail in cfinger and addFingerTrail in FBP. Note that addFingerTrail is included in viewNextTrail
// those are all the moments where we give notes to a fingerboard.

//in FingerTrail: addFingering

//In AnchorSequencer, at the constructor of Anchor with Seq2, the activeTrails are generated. They should be selected 
// with the user preferences.
package hello;

//import java.awt.Dimension;
import hello.utils.XMLPropertiesReader;

import java.awt.Color;
import java.awt.Dimension;
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
	private static final String PATH_PATH = "filePath";
	private static final String EXTENSION_PATH = "extensionString";
	private static final String SETTINGS_SF_TOOL = "Settings for the StringFingering tool";
	
	private boolean arrowsOfRightPanelToKeyboard = true; // if false, then the keyboard maps to the arrows of left panel.
	
	public static BigScore bigScore;
	public static boolean doTransitions = false;
	public static FMenuBar menu;
	public static String version = Version.version;
	
	public boolean prop_like_turtles;
	public boolean prop_anchors_on;//jb
	public String bool_file_path;
	public String prop_extension;
	public Cfinger2 cfinger; // I(jb) removed the static; any remarkable difference?
	
	BigGui (){
		
		this.setTitle("String fingering v" + version);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		//jb
		this.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		
		//loading of properties/constraints
		loadProperties(); // to delete
		XMLPropertiesReader.main(null);
		//Constraints.MAX_HEIGHT = 25;
		//Constraints.MIN_HEIGHT = 1;
		//Constraints.NUM_CHORDS = 4;
		
		System.out.println("The found max height is: "+ Constraints.MAX_HEIGHT);
		
		
		cfinger = new Cfinger2(this);
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
		
		
		//jb
		JSplitPane principalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		principalSplit.setTopComponent(cfinger);
		principalSplit.setBottomComponent(bigScore);
		
		c1.gridx=0;
		c1.gridy=0;
		c1.weightx = 0.1;
		c1.weighty = 0.5;
		c1.fill = GridBagConstraints.BOTH;
		this.add(principalSplit, c1);
		
		/*else
		{
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
		}*/
		
		
		
		
		
		
		
		this.setJMenuBar(menu);
		
		this.pack();
		this.setVisible(true);
		this.setSize(new Dimension(1200, 800)); // TODO adjust until the finale
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
        	String strFilePath = "default";
        	String strExtension = "default 666 d 666";
        	try
	        {
	        	sfProperties.load(new FileReader(propertiesFile));
	        	strFilePath = (String) sfProperties.getProperty(PATH_PATH);
	        	boolTurtles = (String) sfProperties.get(PROP1_PATH);
	        	boolAnchors = (String) sfProperties.get(ANCHORS_PATH);
	        	strExtension = (String) sfProperties.get(EXTENSION_PATH);
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
	        //System.out.println("Here, boolAnchors (String) is: " + boolAnchors);
	        prop_anchors_on = Boolean.parseBoolean(boolAnchors);
	        prop_extension = strExtension;
	        bool_file_path = strFilePath;
        }
        else 
        {
        	try
	        {
	        	sfProperties.setProperty(PROP1_PATH, "false");
	        	sfProperties.setProperty(ANCHORS_PATH, "false");
	        	sfProperties.setProperty(PATH_PATH, "default");
	        	sfProperties.setProperty(EXTENSION_PATH, "default");
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
	
	public void recordPath() {
		String homeDir = System.getProperty(USER_HOME);
        File propertiesFile = new File(homeDir + File.separator + PROPERTY_FILE_NAME);
        Properties swevizProperties = new Properties();
        if(propertiesFile.exists())
        {
        	try
	        {
	        	swevizProperties.load(new FileReader(propertiesFile));
	        	swevizProperties.setProperty(PATH_PATH, bool_file_path);
	        	swevizProperties.store(new FileWriter(propertiesFile), SETTINGS_SF_TOOL);
	        }
	        catch(IOException e)
	        {
	            System.out.println("The boolean FILE PATH could not be stored");
	            JOptionPane.showMessageDialog(this, "The boolean FILE PATH could not be stored, and " +
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
	
	public static void saveExtension(String extensionString) {
		String homeDir = System.getProperty(USER_HOME);
        File propertiesFile = new File(homeDir + File.separator + PROPERTY_FILE_NAME);
        Properties swevizProperties = new Properties();
        if(propertiesFile.exists())
        {
        	try
	        {
	        	swevizProperties.load(new FileReader(propertiesFile));
	        	swevizProperties.setProperty(EXTENSION_PATH, extensionString);
	        	swevizProperties.store(new FileWriter(propertiesFile), SETTINGS_SF_TOOL);
	        }
	        catch(IOException e)
	        {
	            System.out.println("The setting Extension could not be stored");
	            /*JOptionPane.showMessageDialog(this, "The boolean ANCHORS could not be stored, and " +
	            		"the File was detected and loaded",
	            		"error", 0);*/
	            e.printStackTrace();
	        }
        }
        else 
        {
        	System.err.println("The properties file was not found");
        } 
		
	}
	
	public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new BigGui();
			}
		});

	}

	

	
}



package hello;

//import java.awt.Dimension;
import java.awt.EventQueue;
import javax.swing.*;
import java.awt.*;
//import java.awt.event.*;


public class Gui extends JFrame {
	public static JFrame frame;
	//public static BigScoreComponent bigScore;
	public static boolean doTransitions;
		
		  static void createGui (){
			//setLayout(new BorderLayout());	
			String version = Version.version;
			doTransitions = false;
			frame = new JFrame("String fingering v" 
					+ version);
			
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			Cfinger2 c = new Cfinger2();
			//bigScore = new BigScoreComponent();
			BigScore big = new BigScore(); // create, but don't show.
			FMenuBar menu = new FMenuBar(frame, c, big);
			frame.setJMenuBar(menu);
			
			c.setInstrument(new Instrument(Instrument.Instr.VN));
			c.ctl.instPanel.rangeLabel.setText("Violin: G4/55 - G8/103");
			
			
			frame.add(c);
			//frame.add(bigScore);
			
			frame.pack();
			frame.setVisible(true);
			Version.check();
		}
		public void initGui(){
			
		}
		public void  showAboutDialog()
		{
			AboutDialog.doDialog(this) ;
		}

		public static void main(String args[]) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					createGui();
					//new Gui().setVisible(true);
					
				}
			});

		}
	}



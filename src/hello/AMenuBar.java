package hello;


import  java.awt.* ;
import  java.awt.event.* ;
import  javax.swing.* ;

public class AMenuBar extends JMenuBar implements ActionListener {
	private static final long serialVersionUID = 1L;
	JFrame appFrame;
	CtlBox ctl;
	Cfinger2 appPanel;
	AnalysisTop analysisTop;
	
	JMenu fileMenu;
	JMenuItem readFile;
	//JMenu helpMenu;
	//JMenu instMenu;
	
	//JMenu instMenu;
	
	
	
	
	
	AMenuBar(JFrame topFrame, AnalysisTop a) {
		//menuBar = new JMenuBar();
		appFrame = topFrame;
		analysisTop  = a;
		//appPanel = c;
		//ctl = c.ctl;
		//helpMenu = new JMenu("Help");
		//help = new JMenuItem("About");
		//System.out.println("HERE!!!");
			
		fileMenu = new JMenu("File"); 
		readFile = new JMenuItem("Read file");
		//add(instMenu);
		//menuBar.add(aboutMenu);
		add(fileMenu);
		fileMenu.add(readFile);
		//fileMenu.add(file);
		readFile.addActionListener(this);
	
	}
	
	public void  actionPerformed( ActionEvent e )
	{

		if ( e.getSource() == readFile){
		//AboutDialog.doDialog(appFrame);
			System.out.println("read the file");
			//analTop.a.t.setText("HALLO DAR!");
			analysisTop.a.getFeedbackFile();
		} else {
			System.out.println("action??");
		}
			
		
	}	

}

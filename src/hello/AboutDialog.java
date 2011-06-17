package hello;

import  javax.swing.* ;

public class AboutDialog {
	
		private JFrame  mFrame ;

		
	public  AboutDialog( JFrame Frame )
	{	mFrame = Frame ;   }


	/**
	 * A static doDialog method to save instantiation.
	 */
	public static void  doDialog( JFrame Frame )
	{
		AboutDialog  Dlg = new AboutDialog(Frame) ;
		Dlg.doDialog() ;
	}

	/**
	 * Show the dialog and get results.
	 */
	public void  doDialog()
	{
		
		String  Message = "Automated String Fingering Version: " + Version.version + "\n\n"
						+ "Copyright: Eliot Handelman 2010" + "\n"
						+ "License: LGPL\n"
						+ "Bug reports and feature requests: eliot@colba.net"
						;
		
		JOptionPane.showMessageDialog( mFrame, Message, "About Automatic String Fingering",
									   JOptionPane.INFORMATION_MESSAGE ) ;
	}

	}


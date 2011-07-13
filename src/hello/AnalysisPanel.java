package hello;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;

public class AnalysisPanel extends JPanel {
	
	
	private static final long serialVersionUID = 1L;
	//JButton reader;
	JLabel infoLabel;
	ArrowKeys arrows;
	//JButton prev;
	//JButton next;
	JFileChooser fc;
	JTextArea t;
	ArrayList<Feedback.Result> rows;
	int currRow; 
	Cfinger2 c; 
	
	public Dimension getMaximumSize() {
		return new Dimension(300, super.getPreferredSize().height);
	}

	public Dimension getPreferredSize() {
		return new Dimension(300, 400);
	}
	//ArrayList<Feedback.Result> r_list;
	public String nl ="\n";
	
	
	public void showCurrRow(){
		Feedback.Result r = rows.get(currRow);
		//c.ctl.inst.setInstr(r.inst);
		//Fing.localInstrument = r.instrument;
		c.setInstrument(r.instrument);
		c.ctl.instPanel.rangeLabel.setText(r.inst + ": " + r.rating);
		c.ctl.abcChord.drawChord(r.abcString, r.range);
		t.setText("row: " + currRow + nl
				+ r.ip + nl 
				+ r.date + nl 
				+ r.inst + nl
				+ r.rating + nl
				 + r.noteString + nl // string.int
				 + r.keyString + nl // C6/72 etc
				 + r.abcString + nl
				 
		);
		c.fingPane.getCurrentBoard().setFingers(r.notes);
		//c.ctl.abc.drawFingering(f);
		System.out.println(r.noteString);
	}
	
	public void getFile (File f) {
		//rows = new ArrayList<Feedback.Result>();
		rows.clear();
		currRow = -1;
		try {
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(f);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				System.out.println(strLine);
				Feedback.Result rr = Feedback.parse(strLine);
				if (rr.goodParse) rows.add(rr);
			}
			// Close the input stream
			in.close();
			
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		// return r;
		
	}

	public AnalysisPanel(Cfinger2 f) {
		c = f;
		// super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		arrows = new ArrowKeys(false);
		arrows.disable();
		
		BoxLayout g = new BoxLayout(this, BoxLayout.Y_AXIS);
		this.setLayout(g);
		//add(Box.createRigidArea(new Dimension(0, 100)));
		//reader = new JButton("Read file:");
		//add(reader);
		//reader.setAlignmentX(Component.CENTER_ALIGNMENT);
		// add(Box.createRigidArea(new Dimension(0, 10)));
		infoLabel = new JLabel("0 rows read");
		infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(infoLabel);
		add(Box.createRigidArea(new Dimension(0, 10)));
		
		
	
		add(arrows);
		
		rows = new ArrayList<Feedback.Result>();
		
		// TODO what do you mean, give it another action listener?
		arrows.next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nextArrowActionPerformed();
			}
		});
		
		/*protected void nextArrowActionPerformed()
		{
			int sz = rows.size();
			if (currRow - 1 < sz){
				currRow++;
				showCurrRow();
				if(currRow > 0) arrows.prev.setEnabled(true);
				if(currRow + 1 == sz) arrows.next.setEnabled(false);
			}
			return;
		}*/
		
		// TODO what do you mean, give it another action listener?
		arrows.prev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prevArrowActionPerformed();
			}
		});
		
		t = new JTextArea(5, 10);
		add(t);
		
		fc = new JFileChooser("/home/eliot/fingering/fingering_data");
		add(Box.createRigidArea(new Dimension(0, 400)));
		/*
		reader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(AnalysisPanel.this);
				File file = fc.getSelectedFile();

				//System.out.println(file);
				getFile(file);
				infoLabel.setText(rows.size() + " solutions");
				if(rows.size()>0) arrows.next.setEnabled(true);
			}
		});
		*/
	}
	
	/**
	 * Made public to enable the Keyboard action
	 */
	public void prevArrowActionPerformed() {
		if ( currRow > 0){
			currRow--;
			showCurrRow();
			arrows.next.setEnabled(true);
			if (currRow == 0) arrows.prev.setEnabled(false);
		}
	}
	
	/**
	 * Made public to enable the Keyboard action
	 */
	public void nextArrowActionPerformed() {
		System.out.println("nextArrowActionPerformed!!!");
		int sz = rows.size();
		if (currRow - 1 < sz){
			currRow++;
			showCurrRow();
			if(currRow > 0) 
			{
				arrows.prev.setEnabled(true);
			}
			if(currRow + 1 == sz)
			{
				arrows.next.setEnabled(false);
			}
		}
		return;
	}

	void getFeedbackFile () {
		//int returnVal = fc.showOpenDialog(AnalysisPanel.this);
		File file = fc.getSelectedFile();
		
		//System.out.println(file);
		getFile(file);
		infoLabel.setText(file.getName() + ": " + rows.size() + " rows");
		if(rows.size()>0)
		{
			arrows.next.setEnabled(true);
		}
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
	
	
/*
	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				JFrame frame = new JFrame("Analysis");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				//BoxLayout g = new BoxLayout(frame, BoxLayout.X_AXIS);
				// Add content to the window.
				frame.add(new AnalysisPanel());
				//frame.add(new Cfinger2());

				// Display the window.
				frame.pack();
				frame.setVisible(true);

			}
		});
	}
*/
}

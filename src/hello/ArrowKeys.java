package hello;


import javax.swing.*;
import java.awt.*;
import javax.swing.SwingUtilities;

/**
 * 
 * @author eliot
 * On the right Panel, the top arrows. Those arrows are by default assiociated to the keyboard's arrows.
 */
public class ArrowKeys extends JPanel{
	
	private static final long serialVersionUID = 1L;
	// In AnalysisPanel, the events for those buttons are defined. 
	public JButton next;
	public JButton prev;
	public JButton start;
	public JButton stop;
	
	
	public void setPrevNext (boolean previous, boolean nxt){
		prev.setEnabled(previous);
		next.setEnabled(nxt);
	}
	
	public void disable (){
		next.setEnabled(false);
		prev.setEnabled(false);
		start.setEnabled(false);
		stop.setEnabled(false);
	}
	
	public ArrowKeys(boolean fastKeys){
		//setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		ImageIcon prevIcon = createImageIcon("/resource/left.gif");
		ImageIcon nextIcon = createImageIcon("/resource/right.gif");
		ImageIcon startIcon = createImageIcon("/resource/StepBack16.gif");
		ImageIcon stopIcon = createImageIcon("/resource/StepForward16.gif");
		
		start = new JButton(startIcon);
		stop = new JButton(stopIcon);
		
		
		prev = new JButton("",prevIcon);
		prev.setHorizontalTextPosition(AbstractButton.LEADING);
		next = new JButton("", nextIcon);
		
		//JPanel botbut = new JPanel();
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		if (fastKeys) {
			add(Box.createRigidArea(new Dimension(10, 0)));
			add(start);
			add(Box.createRigidArea(new Dimension(10, 0)));
		}
		add(prev);
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(next);
		add(Box.createRigidArea(new Dimension(10, 0)));
		//botbut.add(Box.createHorizontalGlue());
		if (fastKeys) {
			add(stop);
			add(Box.createRigidArea(new Dimension(10, 0)));
		}
		setAlignmentX(Component.CENTER_ALIGNMENT);
		//setBorder(BorderFactory.createLineBorder(Color.black));
		
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
				
				JFrame frame = new JFrame("ArrowKeys");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				//BoxLayout g = new BoxLayout(frame, BoxLayout.X_AXIS);
				// Add content to the window.
				frame.add(new ArrowKeys(true));
				//frame.add(new Cfinger2());

				// Display the window.
				frame.pack();
				frame.setVisible(true);

			}
		});
	}
}

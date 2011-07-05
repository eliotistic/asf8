package hello;

import javax.swing.*;
import java.awt.*;

public class SurveyPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	JRadioButton largo;
	JRadioButton moderato;
	JRadioButton allegro;
	JRadioButton prestissimo;
	Font font = new Font("Verdana", Font.PLAIN, 12);
	ButtonGroup bg;
	JPanel xbutPanel;
	String h = ("Maximum interval span on one string: ");
			
	JLabel span;
	
	JLabel expertise = new JLabel("Expertise: ");
	
	public SurveyPanel() {
		setLayout (new BoxLayout(this,BoxLayout.Y_AXIS));
		
		JLabel inst = new JLabel("Instrument: ");
		inst.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		span = new JLabel (h);
		span.setFont(font);
		span.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		expertise.setFont(font);
		expertise.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//BoxLayout blo = new BoxLayout(this,BoxLayout.Y_AXIS);
		//setLayout(blo);
		
		
		xbutPanel = new JPanel();
		xbutPanel.setFont(font);
		xbutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		bg = new ButtonGroup();
		largo = new JRadioButton("largo");
		moderato = new JRadioButton("moderato");
		allegro = new JRadioButton("allegro con brio");
		prestissimo = new JRadioButton("prestissimo");
		
		BoxLayout g = new BoxLayout(xbutPanel, BoxLayout.X_AXIS);
		//BorderLayout g = new BorderLayout(xbutPanel, BoxLayout.X_AXIS);

		xbutPanel.setLayout(g);
		xbutPanel.add(expertise);
		bg.add(largo);
		bg.add(moderato);
		bg.add(allegro);
		bg.add(prestissimo);
		
		xbutPanel.add(largo);
		xbutPanel.add(moderato);
		xbutPanel.add(allegro);
		xbutPanel.add(prestissimo);
		
		add(inst);
		add(xbutPanel);
		add(Box.createRigidArea(new Dimension(0,10)));
		add(span);
		add(Box.createHorizontalGlue());
		// add(xbutPanel, blo);
		//add(span, blo);
		//float left = Component.LEFT_ALIGNMENT ;
		
	}

	public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Entry3!");
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

				SurveyPanel c = new SurveyPanel();

				frame.add(c);

				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}

package computingmusic;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// import java.util.regex.*;

/*
 import javax.mail.internet.AddressException;
 import javax.mail.internet.InternetAddress;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 */

/*
 * surveyForm.java
 *
 * Created on Apr 17, 2010, 12:02:14 AM
 */

/**
 * 
 * @author eliot
 */
public class surveyForm extends JPanel {
	
	private static final long serialVersionUID = 1L;
	Res r;
	JFrame top;
	CtlBox ctl;
	
	public surveyForm(JFrame sf, CtlBox c) {
		top = sf;
		ctl = c;
		initComponents();
	}

	public class Res {
		int expert;
		String inst;
		int span;
		//String address;
		String email;

		Res() {
			expert = 0;
			inst = "NOINST";
			span = 0;
			//address = "NONE";
			email = "NOEMAIL";
		}

		public String toString(){
			return (email + "," + inst + "," + expert + "," + span);
		

	}
	};

	private void initComponents() {
		r = new Res();

		GridBagConstraints g;
		// int expert = -1;

		bg = new ButtonGroup();
		mainInstrumentLabel = new JLabel();
		instCombo = new JComboBox();
		expertiseLabel = new JLabel();

		largoBut = new JRadioButton();
		modBut = new JRadioButton();
		allegroBut = new JRadioButton();
		prestoBut = new JRadioButton();

		span2Label = new JLabel();
		spanComboBox = new JComboBox();

		span1Label = new JLabel();
		emailLabel = new JLabel();
		emailField = new JTextField();
		submit = new JButton();
		topTitle = new JLabel();

		bg.add(largoBut);
		bg.add(modBut);
		bg.add(allegroBut);
		bg.add(prestoBut);

		setLayout(new GridBagLayout());

		mainInstrumentLabel.setText("Main Instrument:");
		g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = 1;
		g.gridwidth = 2;
		g.ipadx = 2;
		// g.anchor = null;
		g.insets = new Insets(25, 19, 0, 0);
		add(mainInstrumentLabel, g);

		// instCombo

		instCombo.setModel(new DefaultComboBoxModel(new String[] { "Select",
				"Violin", "Viola", "Cello" }));

		instCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				instComboActionPerformed(evt);
			}
		});

		g = new GridBagConstraints();
		g.gridx = 4;
		g.gridy = 1;
		g.gridwidth = 4;
		g.gridheight = 1;
		g.ipadx = 34;
		// g.anchor = null;
		g.insets = new Insets(25, 4, 0, 0);
		add(instCombo, g);

		// instCombo

		expertiseLabel.setText("Expertise:");
		g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = 3;
		g.gridwidth = 2;
		g.ipadx = 9;
		// g.anchor = null;
		g.insets = new Insets(22, 62, 0, 0);
		add(expertiseLabel, g);

		// largo
		largoBut.setText("adagio");
		largoBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				r.expert = 1;
			}
		});
		g = new GridBagConstraints();
		g.gridx = 4;
		g.gridy = 3;
		g.gridheight = 2;
		// g.anchor = null;
		g.insets = new Insets(18, 4, 0, 0);
		add(largoBut, g);

		// moderato
		modBut.setText("moderato");
		modBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				r.expert = 2;
			}
		});

		g = new GridBagConstraints();
		g.gridx = 4;
		g.gridy = 5;
		g.gridwidth = 3;
		// g.anchor = null;
		g.insets = new Insets(0, 4, 0, 0);
		add(modBut, g);

		// allegro

		allegroBut.setText("allegro con brio");
		allegroBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				r.expert = 3;
			}
		});

		g = new GridBagConstraints();
		g.gridx = 4;
		g.gridy = 6;
		g.gridwidth = 6;
		// g.anchor = null;
		g.insets = new Insets(0, 4, 0, 0);
		add(allegroBut, g);

		prestoBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				r.expert = 4;
			}
		});
		prestoBut.setText("prestissimo");

		g = new GridBagConstraints();
		g.gridx = 4;
		g.gridy = 7;
		g.gridwidth = 5;
		// g.anchor = null;
		g.insets = new Insets(0, 4, 0, 0);
		add(prestoBut, g);

		span1Label.setText("From the 1st semitone on the lowest string, your");
		g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = 8;
		g.gridwidth = 12;
		g.ipadx = 1;
		// g.anchor = null;
		g.insets = new Insets(20, 39, 0, 58);
		add(span1Label, g);

		span2Label.setText("widest interval:");
		g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = 9;
		g.gridwidth = 4;
		// g.anchor = null;
		g.insets = new Insets(10, 39, 0, 0);
		add(span2Label, g);

		spanComboBox.setModel(new DefaultComboBoxModel(new String[] { "Select",
				"4th", "tritone", "5th", "minor 6th", "major 6th", "minor 7th",
				"major 7th", "octave"

		}));
		spanComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int ind = spanComboBox.getSelectedIndex();
				// String ss = (String)instCombo.getSelectedItem();
				switch (ind) {
				case 0:
					break;
				case 1:
					r.span = 5;
					break;
				case 2:
					r.span = 6;
					break;
				case 3:
					r.span = 7;
					break;
				case 4:
					r.span = 8;
					break;
				case 5:
					r.span = 9;
					break;
				case 6:
					r.span = 10;
					break;
				case 7:
					r.span = 11;
					break;
				case 8:
					r.span = 12;
					break;
				}
			}
		});

		g = new GridBagConstraints();
		g.gridx = 4;
		g.gridy = 9;
		g.gridwidth = 3;
		g.gridheight = 2;
		// g.anchor = null;
		g.insets = new Insets(6, 4, 0, 0);
		add(spanComboBox, g);

		emailLabel.setText("email:");
		g = new GridBagConstraints();
		g.gridx = 1;
		g.gridy = 11;
		g.gridwidth = 3;
		// g.anchor = null;
		g.insets = new Insets(35, 6, 0, 0);
		add(emailLabel, g);

		emailField.setText(null);
		g = new GridBagConstraints();
		g.gridx = 4;
		g.gridy = 11;
		g.gridwidth = 7;
		g.gridheight = 2;
		g.ipadx = 200;
		g.ipady = 3;
		// g.anchor = null;
		g.insets = new Insets(33, 4, 0, 0);
		add(emailField, g);

		submit.setText("submit");
		submit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				submitActionPerformed(evt);
			}
		});
		g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = 13;
		// g.anchor = null;
		g.insets = new Insets(27, 19, 31, 0);
		add(submit, g);

		topTitle.setText("String fingering survey");
		g = new GridBagConstraints();
		g.gridx = 1;
		g.gridy = 0;
		g.gridwidth = 11;
		g.ipadx = 110;
		// g.anchor = null;
		g.insets = new Insets(12, 15, 0, 58);
		add(topTitle, g);
	}

	private void instComboActionPerformed(ActionEvent evt) {
		int ind = instCombo.getSelectedIndex();
		String ss = (String) instCombo.getSelectedItem();
		// System.out.println("instr: " + ss);
		if (ind > 0)
			r.inst = ss;
	}

	private void submitActionPerformed(ActionEvent evt) {
		/*
		 * expert = 0; inst = ""; span = 0; address = "";
		 */
		String em = emailField.getText();
		// if (em.equals("")) System.out.println("Yep: null email");
		if (!em.equals("")) r.email = em;
		
		//System.out.println("Email: " + r.email );
		//System.out.println("Instrument: " + r.inst);
		//System.out.println("Expertise: " + r.expert);

		//System.out.println("Span: " + r.span);
		//System.out.println("Address:" + r.address);

		Logger.log("survey", r.toString());
		// close the window
		//Container c = this,container;
		ctl.survey.setEnabled(false);
		//top.dispose();
		//this.setVisible(false);

	}

	private ButtonGroup bg;
	private JButton submit;
	private JComboBox instCombo;
	private JComboBox spanComboBox;
	private JLabel mainInstrumentLabel;
	private JLabel expertiseLabel;
	private JLabel span2Label;
	private JLabel span1Label;
	private JLabel emailLabel;
	private JLabel topTitle;
	private JRadioButton largoBut;
	private JRadioButton modBut;
	private JRadioButton allegroBut;
	private JRadioButton prestoBut;
	private JTextField emailField;

	/*
	public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Entry3!");
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

				surveyForm c = new surveyForm(new JFrame(), new CtlBox());

				frame.add(c);

				frame.pack();
				frame.setVisible(true);
			}
		});
	}
	*/
}

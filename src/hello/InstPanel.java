package hello;


import javax.swing.*;
import java.awt.*;

public class InstPanel extends JPanel {
    
	private static final long serialVersionUID = 1L;
	ButtonGroup buttonGroup1;
    JTextField rangeLabel;
    JRadioButton vn;
    JRadioButton va;
    JRadioButton vc;
    JButton fake;
    RangeImage rangeImage;

    public InstPanel() {
        setBorder(BorderFactory.createEtchedBorder());
        initComponents();
    }
    /*
    public  void setVn() {
		//appPanel.clearAll();
		Instrument it = new Instrument (Instrument.Instr.VN);
		Fing.localInstrument = it;
		//big.setInstrument(it);
		//rangeLabel.setText("Violin: G4/55 - G8/103");
		rangeLabel.setText(it.rangeString());
	}

	public void setVa() {
		//appPanel.clearAll();
		Instrument it = new Instrument (Instrument.Instr.VA);
		Fing.localInstrument = it;
		//big.setInstrument(it);
		//rangeLabel.setText("Viola: C4/48 - E7/88");
		rangeLabel.setText(it.rangeString());
	}

	public void setVc() {
		//appPanel.clearAll();
		Instrument it = new Instrument (Instrument.Instr.VC);
		Fing.localInstrument = it;
		//big.setInstrument(it);
		//rangeLabel.setText("Cello: C3/36 - A6/81");
		rangeLabel.setText(it.rangeString());
	}


    public void setVn() {
            Fing.instrument = Fing.Instr.VN;
            rangeLabel.setText("Range: G4/55 - G8/103");
    }

    public void setVa() {
            Fing.instrument = Fing.Instr.VA;
            rangeLabel.setText("Range: C4/48 - E7/88");
    }

    public void setVc() {
            Fing.instrument = Fing.Instr.VC;
            rangeLabel.setText("Range: C3/36 - A6/81");
    }

    public void setInstr(String s){
            if (s.equals("VN"))
            {   vn.setSelected(true); setVn(); }
            else if (s.equals("VA"))
            {   va.setSelected(true); setVa(); }
            else if (s.equals("VC"))
            {       vc.setSelected(true); setVc(); }
            }

    */
    private void initComponents() {

        rangeImage = new RangeImage();

        rangeLabel = new JTextField(16); // HERE, MR. ELIOT!
        rangeLabel.setEditable(false);
        rangeLabel.setBorder(null);
        Font font = new Font("Verdana", Font.BOLD, 12);
        rangeLabel.setFont(font);
        // txt.setForeground(Color.BLUE);

        fake = new JButton("fake");

     
		setLayout(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        
        c1.gridx = 0;
        c1.gridy = 0;
        
        
        add(rangeLabel, c1);

        //vn.setSelected(true);
        //setVn();

    }

    public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 36);
    }

    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
            	InstPanel c = new InstPanel();
                JFrame frame = new JFrame("INST!");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.add(c);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}

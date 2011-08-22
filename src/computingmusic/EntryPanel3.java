package computingmusic;


/*
 *  
 * Created on Apr 11, 2010, 12:59:28 AM
 */

//package gb;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JFrame;



/**
 * 
 * @author eliot
 * This is the panel at the left that enables the user to input a note he wants to see. 
 */

public class EntryPanel3 extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2680747828869772889L;
	JButton clearBtn;
	JButton randBtn;
	JButton fingerBtn;
	
	JTextField noteField;
	
	String helloText = "Enter 2,3 or 4 pitches:";
	
	JButton prevBtn;
	JButton nextBtn;
	// GluePanel gluePanel;
	
	
	public EntryPanel3() {
		// initComponents();
		// setSize(300,300);
		// ImageIcon prevIcon = new ImageIcon(getClass().getResource("/img/StepForward24.gif"));
		ImageIcon prevIcon = createImageIcon("/resource/left.gif");
		ImageIcon nextIcon = createImageIcon("/resource/right.gif");
		
		// ImageIcon prevIcon = new ImageIcon("StepBack24.gif");
	    // ImageIcon nextIcon = new ImageIcon("StepForward24.gif");

		//setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		setBorder(BorderFactory.createLineBorder(Color.gray));
		// setAlignmentX(LEFT_ALIGNMENT);
	
		clearBtn = new JButton("Clear");
		//clearBtn.setPreferredSize(new Dimension(100, 30));
		randBtn = new JButton("Random");
		//randBtn.setPreferredSize(new Dimension(100, 30));
		noteField = new JTextField(8);
		//noteField2 = new JTextField(4);
		noteField.setPreferredSize(new Dimension(200, 30));
		//noteField.setSize(new Dimension(2000, 20));
		
		fingerBtn = new JButton("finger");
		//fingerBtn.setPreferredSize(new Dimension(60, 20));
		//fingerBtn.setPreferredSize(new Dimension(25, 20));
		nextBtn = new JButton("", nextIcon);
		prevBtn = new JButton("", prevIcon );
		prevBtn.setHorizontalTextPosition(AbstractButton.LEADING);
		//prevBtn.setPreferredSize(new Dimension(90, 30));
		//nextBtn.setPreferredSize(new Dimension(90, 30));
		
		
		
	
		/*JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridBagLayout());
		
		topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		topPanel.add(clearBtn);
		topPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		topPanel.add(randBtn);
		topPanel.add(Box.createHorizontalGlue());

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		centerPanel.add(noteField);
		//entry.add(noteField2);
		centerPanel.add(fingerBtn);
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(prevBtn);
		bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		bottomPanel.add(nextBtn);*/
		
	
		
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();
		GridBagConstraints c4 = new GridBagConstraints();
		GridBagConstraints c5 = new GridBagConstraints();
		GridBagConstraints c6 = new GridBagConstraints();
		GridBagConstraints c7 = new GridBagConstraints();
		GridBagConstraints c8 = new GridBagConstraints();
		GridBagConstraints c9 = new GridBagConstraints();
		
		boolean newGridBag = true;
		if(newGridBag)
		{
				
			
			JPanel topPanel = new JPanel();
			c1.gridx = 0;
			c1.gridy = 0;
			//c1.weightx = 0.4;
			c1.fill = GridBagConstraints.BOTH;
			
			c2.gridx = 1;
			c2.gridy = 0;
			//c2.weightx = 0.6;
			c2.fill = GridBagConstraints.BOTH;
			//c2.weightx=0.5;
			topPanel.add(clearBtn, c1);
			topPanel.add(randBtn, c2);
			
			JPanel midPanel = new JPanel();
			midPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
			c3.gridx = 0;
			c3.gridy = 0;
			//c3.gridwidth = GridBagConstraints.RELATIVE;
			//c3.weightx = 0.7;
			//c3.fill = GridBagConstraints.BOTH;
			c3.weightx = 1.0;
			c3.weighty = 1.0;
			c3.ipadx = 800;
			c3.ipady = 30;
			c4.gridx = 1;
			c4.gridy = 0;
			c4.weightx = 0.2;
			c4.weighty = 0.5;
			c4.fill = GridBagConstraints.BOTH;
			c4.gridwidth = GridBagConstraints.REMAINDER;
			midPanel.add(noteField, c3);
			midPanel.add(fingerBtn, c4);
			System.out.println("noteField's size:" + noteField.getSize());
			
			
			JPanel bottomPanel = new JPanel();
			c5.gridx = 0;
			c5.gridy = 0;
			c5.fill = GridBagConstraints.BOTH;
			c6.gridx = 1;
			c6.gridy = 0;
			c6.fill = GridBagConstraints.BOTH;
			bottomPanel.add(prevBtn, c5);
			bottomPanel.add(nextBtn, c6);
			
			c7.gridx = 0;
			c7.gridy = 0;
			c7.weightx = 0.5;
			c7.weighty = 0.5;
			c7.fill = GridBagConstraints.BOTH;
			//c7.anchor = GridBagConstraints.EAST;
			c8.gridx = 0;
			c8.gridy = 1;
			c8.fill = GridBagConstraints.BOTH;
			c8.weightx = 0.5;
			c8.weighty = 0.5;
			
			c9.gridx = 0;
			c9.gridy = 2;
			c9.fill = GridBagConstraints.BOTH;
			c9.weighty = 0.5;
			c9.weightx = 0.5;
			this.add(topPanel, c7);
			this.add(midPanel, c8);
			this.add(bottomPanel, c9);
		}
		
		else 
		{
			
		
			JButton btn1 = new JButton("1");
			JButton btn2 = new JButton("2");
			JButton btn3 = new JButton("3");
			
			
			c1.gridx = 0;
			c1.gridy = 0;
			c1.fill = GridBagConstraints.BOTH;
			//c1.weightx = 0.3;
			//c1.weighty = 0.3;
			add(clearBtn, c1);
			
			c2.gridx = 1;
			c2.gridy = 0;
			c2.gridwidth = 2;
			c2.fill = GridBagConstraints.BOTH;
			//c2.weightx = 0.3;
			//c2.weighty = 0.3;
			add(randBtn, c2);
			
			c7.gridx = 2;
			c7.gridy = 0;
			c7.fill = GridBagConstraints.BOTH;
			//c7.weightx = 0.3;
			//c7.weighty = 0.3;
			add(btn1, c7);
			
			c3.gridx = 0;
			c3.gridy = 1;
			c3.gridwidth = 2;
			c3.fill = GridBagConstraints.BOTH;
			//c3.weightx = 0.3;
			//c3.weighty = 0.3;
			add(noteField, c3);
			
			c4.gridx = 2;
			c4.gridy = 1;
			c4.fill = GridBagConstraints.BOTH;
			//c4.weightx = 0.3;
			//c4.weighty = 0.3;
			add(fingerBtn, c4);
			
			c8.gridx = 2;
			c8.gridy = 1;
			c8.fill = GridBagConstraints.BOTH;
			//c8.weightx = 0.3;
			//c8.weighty = 0.3;
			add(btn2, c8);
			
			c5.gridx = 0;
			c5.gridy = 2;
			c5.fill = GridBagConstraints.VERTICAL;
			c5.anchor = GridBagConstraints.LINE_END;
			//c5.insets = new Insets(0, prevBtn.getWidth() - nextBtn.getWidth(), 0, 0);
			//c5.weightx = 0.3;
			//c5.weighty = 0.3;
			add(prevBtn, c5);
			
			c6.gridx = 1;
			c6.gridy = 2;
			//c6.gridwidth = 2;
			c6.fill = GridBagConstraints.BOTH;
			//c6.weightx = 0.3;
			//c6.weighty = 0.3;
			add(nextBtn, c6);
			
			c9.gridx = 2;
			c9.gridy = 2;
			//c6.gridwidth = 2;
			c9.fill = GridBagConstraints.BOTH;
			//c9.weightx = 0.3;
			//c9.weighty = 0.3;
			add(btn3, c9);
		
		}
		
		
		
		this.setFocusable(true);
		
		/*addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                System.out.println("Focus gained");
            }
            public void focusLost(java.awt.event.FocusEvent evt)
            {
                System.out.println("Focus lost");
            }
        });*/
		/*addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
            	//cfinger.setBorder(new BasicBorders.MarginBorder());
            	//System.out.println("MouseClicked event in Panel3");
            	
            }
            public void mouseExited(MouseEvent evt)
            {
            	//System.out.println("Mouse exited from Panel3");
            }
        });*/
		this.addKeyListener(new KeyListener()
		{
			@Override
			public void keyPressed(KeyEvent arg0) {
				System.out.println("key pressed");
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				System.out.println("key released");
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
				char typed = arg0.getKeyChar();
				System.out.println("The read character in keyTyped is: " + typed);
			}
			
		});
		
		
		
		/*add(topPanel, BorderLayout.PAGE_START);
		add(entryPanel, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.PAGE_END);*/
		prevBtn.setEnabled(false);
		nextBtn.setEnabled(false);
	};
	
	public void setArrows (int currInd, int maxInd){
		prevBtn.setEnabled(currInd > 0);
		nextBtn.setEnabled(currInd < maxInd);
	}
	
	public void setArrows(ChordFingering r){
		prevBtn.setEnabled(r.hasPrevFingering());
		nextBtn.setEnabled(r.hasNextFingering());
	}
	
	public void setArrows(Transitions t){
		prevBtn.setEnabled(t.hasPrevFingering());
		nextBtn.setEnabled(t.hasNextFingering());
	}
	public void clearArrows() {
		prevBtn.setEnabled(false);
		nextBtn.setEnabled(false);
	}
	
	public void disableFinger (){
		fingerBtn.setEnabled(false);
	}
	public void enableFinger (){
		fingerBtn.setEnabled(true);
	}
	
	
	public void clear (){
		
		noteField.setText(null);
		//noteField2.setText(null);
		clearArrows();
		// computingmusic.setText(helloText);
		
		
		//howmany.setText(computingmusic);
		//currBox.setText(null);
	}
	
	
	/*
	public void sayCurr (Fing.Result r){
		String x = "" + (r.curr + 1); // 0-based index
		// currBox.setText(x);
	}
	*/
	
	protected  ImageIcon createImageIcon(String path) {
	        java.net.URL imgURL = this.getClass().getResource(path);
	        if (imgURL != null) {
	            return new ImageIcon(imgURL);
	        } else {
	            System.err.println("Couldn't find file: " + path);
	            return null;
	        }
	    }

	public Dimension getMaximumSize() {
        return new Dimension(300, 100);
    }
	   
	
	public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Entry3!");
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				
				EntryPanel3 c = new EntryPanel3();
				
				frame.add(c);
				frame.pack();
				frame.setVisible(true);
			}
		});
	}

}

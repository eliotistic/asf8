package computingmusic;

import  java.awt.event.* ;
import  javax.swing.* ;

public class FMenuBar extends JMenuBar implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	BigGui appFrame;
	CtlBox ctl;
	Cfinger2 appPanel;
	
	BigScore big;
	JMenu fileMenu;
	JMenuItem readABC;
	JMenuItem justABC;
	
	JMenu instMenu;
	
	
	//JMenu instMenu;
	
	JRadioButtonMenuItem vnCB;
	JRadioButtonMenuItem vaCB;
	JRadioButtonMenuItem vcCB;
	ButtonGroup instBG;
	
	JMenu helpMenu;
	JMenuItem help;
	
	JMenu randMenu;
	
	ButtonGroup randBG;
	JRadioButtonMenuItem rand1;
	JRadioButtonMenuItem rand1_4;
	JRadioButtonMenuItem rand2;
	
	JMenu topologyMenu;
	ButtonGroup topologyBG;
	JRadioButtonMenuItem filterBad;
	
	JMenu modeMenu;
	ButtonGroup modeBG;
	JRadioButtonMenuItem modeChordal;
	JRadioButtonMenuItem modePositional;
	
	// POSITIONS
	JMenu positions;
	ButtonGroup positionsBG;
	JRadioButtonMenuItem positionsNone;
	JRadioButtonMenuItem positionsCursor;
	JRadioButtonMenuItem cursorBindMaxPosition;
	
	JMenu arp;
	ButtonGroup arpBG;
	JRadioButtonMenuItem arpNone;
	JRadioButtonMenuItem arpLongest;
	JRadioButtonMenuItem arpJump; // jump to an arp with a new final note
	
	JMenu anchor;
	ButtonGroup anchorBG;
	JRadioButtonMenuItem anchorNone;
	JRadioButtonMenuItem anchorOn;
	
	JMenu solve;
	ButtonGroup solveBG;
	JRadioButtonMenuItem solveNone;
	JRadioButtonMenuItem solveOn;
	
	JMenu boards;
	JMenuItem clearBtn;
	
	
	public String arpMenuGetState() {
		
		if (arpLongest.isSelected()) { 
			return "LONG";
		}
		if (arpJump.isSelected()) {
			return "JUMP";
		} else {
			return "NORMAL";
		}
	}
	FMenuBar(BigGui topFrame, Cfinger2 c, BigScore b) {
		//menuBar = new JMenuBar();
		appFrame = topFrame;
		appPanel = c;
		//BigScore b = topFrame;
		ctl = c.ctl;
		big = b;
		
		fileMenu = new JMenu("File");
		readABC = new JMenuItem("Analyze ABC file");
		justABC = new JMenuItem("Draw ABC, no analysis");
		fileMenu.add(readABC);
		fileMenu.add(justABC);
		readABC.addActionListener(this);
		justABC.addActionListener(this);
		add(fileMenu);
		
		fileMenu.setMnemonic(KeyEvent.VK_F);
		readABC.setMnemonic(KeyEvent.VK_R);

		
		
		instMenu = new JMenu("Instrument"); 
		//add(instMenu);
		//menuBar.add(aboutMenu);
	
		instBG = new ButtonGroup();
		vnCB = new JRadioButtonMenuItem("Violin");
		vaCB = new JRadioButtonMenuItem("Viola");
		vcCB = new JRadioButtonMenuItem("Cello");
		
		//cbMenuItem.setMnemonic(KeyEvent.VK_C);
		instBG.add(vnCB);
		instBG.add(vaCB);
		instBG.add(vcCB);
		
		vnCB.addActionListener(this);
		vaCB.addActionListener(this);
		vcCB.addActionListener(this);
		
		
		instMenu.add(vnCB);
		instMenu.add(vaCB);
		instMenu.add(vcCB);
		
		add(instMenu);
		instMenu.setMnemonic(KeyEvent.VK_I);
		
		
		// RAND 
		
		randMenu = new JMenu("Random");
		randBG = new ButtonGroup();
		rand1 = new JRadioButtonMenuItem("1 chord");
		rand1_4 = new JRadioButtonMenuItem("1 chord (4 stops)");
		//rand2 = new JRadioButtonMenuItem("2 chords");
		
		rand1.addActionListener(this);
		rand1_4.addActionListener(this);
		//rand2.addActionListener(this);
		
		randMenu.add(rand1);
		randMenu.add(rand1_4);
		//randMenu.add(rand2);
		randBG.add(rand1);
		randBG.add(rand1_4);
		//randBG.add(rand2);
		add(randMenu);
		randMenu.setMnemonic(KeyEvent.VK_R);
			
		// TOPOLOGY
			
		topologyMenu = new JMenu("Topology");;
		topologyBG = new ButtonGroup();
		filterBad = new JRadioButtonMenuItem("Filter bad topologies") ;
		filterBad.addActionListener(this);
		topologyMenu.add(filterBad);
		filterBad.setSelected(big.filterBadToplogies);
		add(topologyMenu);
		topologyMenu.setMnemonic(KeyEvent.VK_T);
		
		// MODE
		
		modeMenu = new JMenu ("Mode");
		
		modeBG = new ButtonGroup ();
		modeChordal = new JRadioButtonMenuItem ("Chordal");
		modePositional = new JRadioButtonMenuItem("Positional") ;
		modeChordal.addActionListener(this);
		modePositional.addActionListener(this);
		
		modeMenu.add(modeChordal);
		modeMenu.add(modePositional);
		
		modeBG.add(modeChordal);
		modeBG.add(modePositional);
		
		modeMenu.setMnemonic(KeyEvent.VK_M);
		//add(modeMenu);
		
		// POSITIONS
		positions = new JMenu("Positions");
	
		positionsBG = new ButtonGroup();
		positionsNone = new JRadioButtonMenuItem("Off");
		positionsCursor = new JRadioButtonMenuItem("Manual");
		cursorBindMaxPosition = new JRadioButtonMenuItem("Auto Max cursor");
		
		positionsBG.add(positionsNone);
		positionsBG.add(positionsCursor);
		positionsBG.add(cursorBindMaxPosition);
		
		positions.add(positionsNone);
		positions.add(positionsCursor);
		positions.add(cursorBindMaxPosition);
		
		positionsNone.addActionListener(this);
		positionsCursor.addActionListener(this);
		cursorBindMaxPosition.addActionListener(this);
		
		
		positionsNone.setSelected(true);
		
		add(positions);
		positions.setMnemonic(KeyEvent.VK_P);
		
		// ARPEGGIATION
		arp = new JMenu("Arpeggiation");
		arpBG = new ButtonGroup();
		arpNone = new JRadioButtonMenuItem ("None");
		arpLongest = new JRadioButtonMenuItem ("Longest here");
		arpJump = new JRadioButtonMenuItem ("Jump to new longest");
		
		arpBG.add(arpNone);
		arpBG.add(arpLongest);
		arpBG.add(arpJump);
		
		arp.add(arpNone);
		arp.add(arpLongest);
		arp.add(arpJump);
		
		arpNone.addActionListener(this);
		arpLongest.addActionListener(this);
		arpJump.addActionListener(this);
		
		arpNone.setSelected(true);
		add(arp);
		
		// anchor
		
		anchor = new JMenu ("Anchors");
		anchorBG = new ButtonGroup ();
		anchorNone = new JRadioButtonMenuItem ("None");
		anchorOn = new JRadioButtonMenuItem ("On");
		
		anchorBG.add(anchorNone);
		anchorBG.add(anchorOn);
		
		anchor.add(anchorNone);
		anchor.add(anchorOn);
		
		anchorNone.addActionListener(this);
		anchorOn.addActionListener(this);
		
		//System.out.println("Anchors on: " + appFrame.prop_anchors_on);
		anchorNone.setSelected(false);
		anchorOn.setSelected(true);
		
		add(anchor);
		
		// solve
		
		solve = new JMenu("Solve");
		solveBG = new ButtonGroup ();
		solveNone = new JRadioButtonMenuItem ("Off");
		solveOn = new JRadioButtonMenuItem ("On");
		
		solveBG.add(solveNone);
		solveBG.add(solveOn);
		
		solve.add(solveNone);
		solve.add(solveOn);
		
		add(solve);
		// HELP
		
		boards = new JMenu("Boards");
		clearBtn = new JMenuItem("Clear Panels");
		boards.add(clearBtn);
		
		add(boards);
		
		helpMenu = new JMenu("Help");
		help = new JMenuItem("About");
		add(helpMenu);
		helpMenu.add(help);
		help.addActionListener(this);
		clearBtn.addActionListener(this);
		helpMenu.setMnemonic(KeyEvent.VK_H);
		// start with violin
		vnCB.setSelected(true);
		// start with 2 random chords
		rand1.setSelected(true);
		
		
		
	}
	
	public void setInstrument (String s) {
		//Instrument it = Instrument.ofString(s);
		Instrument it = new Instrument(big, s);
		appPanel.clearAll();
		//Fing.localInstrument = it;
		big.setInstrument(it);
		//ctl.inst.rangeLabel.setText(it.rangeString());
	}
	
	public void  actionPerformed( ActionEvent e )
	
	{
		if (e.getSource() == readABC){
			// reads & draws a score
			//AbcReader abc = new AbcReader(big);
			//big.abcReader.readAndDraw();
			
			big.readandDrawABC(true); // boolean says whether to do fingering
			//System.out.println(abc.abcString);
			//ctl.abc.setH(5f);
			
		}
		if (e.getSource() == justABC){
			big.readandDrawABC(false); // boolean says whether to do fingering
		}
		
		if (e.getSource() == rand1){
			appPanel.clearAll();

			appPanel.random4stop = false;
			appPanel.randomChords = 1;
		}
		if (e.getSource() == rand1_4){
			appPanel.clearAll();

			appPanel.random4stop = true;
		}
		if (e.getSource() == rand2){
			appPanel.clearAll();

			appPanel.random4stop = false;
			appPanel.randomChords = 2;
		}

		if ( e.getSource() == vnCB ){
			setInstrument("VN");
		};

		if ( e.getSource() == vaCB ){
			setInstrument("VA");
		};

		if ( e.getSource() == vcCB ){
			setInstrument("VC");
		};
		
		if ( e.getSource() == help){
		AboutDialog.doDialog(appFrame);
		}
		if ( e.getSource() == filterBad) {
		big.toggleBadTopologies();
		}
		if (e.getSource() == modeChordal) {
			big.controlMode = "CHORD";
		}
		if (e.getSource() == modePositional) {
			big.controlMode = "POSITION";
		}
		
		if (e.getSource() == positionsNone) {
			System.out.println("menu: positions off");
			big.turnoffPositions();
		}
		if (e.getSource() == positionsCursor) {
			//System.out.println("menu: positions at cursor");
			//big.pseq.positionsFromCursor(); // 
			big.extendPanel.positions.setEnabled(true);
			big.extendPanel.positionRestrict.setEnabled(true);
			//big.positionFollowsCursor = false;
			big.stopPositionFollowing();
		}
		if (e.getSource() == cursorBindMaxPosition) {
			big.extendPanel.positionRestrict.setEnabled(true);
			big.togglePositionFollowing();
			//big.positionFollowsCursor = !big.positionFollowsCursor;
			
		}
		if (e.getSource() == arpNone) {
			//big.arpSeq.setState()
		
			big.arpSeq.setStateNormal();
			System.out.println("set state: NORMAL");
		}
		if (e.getSource() == arpLongest) {
			big.arpSeq.setStateLongest();
			System.out.println("set state: LONGEST");
			
		}
		if (e.getSource() == arpJump) {
			/*
			if (big.arpSeq == null) {
				System.out.println("JUMP -- No arp Seq");
				return;
			}
			*/
			big.arpSeq.setStateJump();
			System.out.println("set state: JUMP");
		}
		
		
		
		if (e.getSource() == anchorNone) {
			big.anchSeq.setStateOff();
			//appFrame.setPropAnchors(false);
			System.out.println("Anchor: None");
		}
		if (e.getSource() == anchorOn) {
			big.anchSeq.setStateOn();
			//appFrame.setPropAnchors(true);
			System.out.println("Anchor: On");
		}
		
		if(e.getSource() == clearBtn)
		{
			appPanel.fingPane.clearAll();
			System.out.println("event here in FMenubR");
		}
	}	

}

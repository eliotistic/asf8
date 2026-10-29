package computingmusic;

import computingmusic.instrument.InstrumentSpec;
import  java.awt.event.* ;
import  javax.swing.* ;

import computingmusic.utils.XMLPropertiesReader;
import computingmusic.utils.XMLPropertiesWriter;

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
	JMenuItem readScore;
	JMenuItem restartItem;
	JMenu openRecent;
	javax.swing.JCheckBoxMenuItem markUnique;
	
	JMenu instMenu;
	
	
	//JMenu instMenu;
	
	/** One radio item for each instrument of the registry (InstrumentSpec.ALL), in its order; the violin is first. */
	JRadioButtonMenuItem[] instrumentItems;
	ButtonGroup instBG;
	
	JMenu helpMenu;
	JMenuItem help;
	JMenuItem controlsHelp;
	
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
	
	JMenu properties;
	JMenuItem reloadBtn;
	JMenuItem resetBtn;
	
	
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
		readScore = new JMenuItem("Open score (MusicXML, MEI, kern)...");
		readScore.setToolTipText("Open a MusicXML, MEI or kern score: JJ reads the notes, Verovio draws them, and fingerings are calculated.");
		readScore.addActionListener(this);
		fileMenu.add(readScore);
		fileMenu.add(readABC);
		fileMenu.add(justABC);
		openRecent = new JMenu("Open recent");
		openRecent.setToolTipText("The last ten scores opened, newest first.");
		fileMenu.add(openRecent);
		appFrame.recentChanged = new Runnable() {
			public void run() {
				fillRecent();
			}
		};
		fillRecent();
		fileMenu.addSeparator();
		restartItem = new JMenuItem("Restart");
		restartItem.setToolTipText("Start a fresh copy of the program (it loads the classes as built now) and reopen the last score.");
		restartItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
		restartItem.addActionListener(this);
		fileMenu.add(restartItem);
		readABC.addActionListener(this);
		justABC.addActionListener(this);
		readABC.setToolTipText("Open an ABC file, draw the score, and calculate fingerings.");
		justABC.setToolTipText("Open and draw an ABC score without calculating fingerings.");
		add(fileMenu);
		
		fileMenu.setMnemonic(KeyEvent.VK_F);
		readABC.setMnemonic(KeyEvent.VK_R);

		
		
		instMenu = new JMenu("Instrument"); 
		//add(instMenu);
		//menuBar.add(aboutMenu);
	
		instBG = new ButtonGroup();
		instrumentItems = new JRadioButtonMenuItem[InstrumentSpec.ALL.size()];
		for (int k = 0; k < instrumentItems.length; k++) {
			InstrumentSpec spec = InstrumentSpec.ALL.get(k);
			instrumentItems[k] = new JRadioButtonMenuItem(spec.name);
			instBG.add(instrumentItems[k]);
			instrumentItems[k].addActionListener(this);
			instrumentItems[k].setToolTipText("Analyze fingerings using " + spec.name.toLowerCase() + " tuning and range.");
			instMenu.add(instrumentItems[k]);
		}
		
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
		rand1.setToolTipText("Make Random generate an ordinary random chord.");
		rand1_4.setToolTipText("Make Random generate a four-note stop.");
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
		filterBad.setToolTipText("Reject playable fingerings whose fingerboard shape violates the topology rules.");
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
		positionsNone.setToolTipText("Do not restrict the score by hand position.");
		positionsCursor.setToolTipText("Choose hand-position sequences with the Positions controls below the score.");
		cursorBindMaxPosition.setToolTipText("Automatically follow the maximum available position as the score cursor moves.");
		
		
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
		arpNone.setToolTipText("Show the fingering at the current score event only.");
		arpLongest.setToolTipText("Show the longest compatible arpeggiated passage beginning here.");
		arpJump.setToolTipText("Make score navigation jump between changes in the longest arpeggiated passage.");
		
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
		anchorNone.setToolTipText("Navigate without preserving fingering trails between score events.");
		anchorOn.setToolTipText("Track physically compatible fingering trails while navigating and extending.");
		
		add(anchor);
		
		// solve
		
		solve = new JMenu("Solve");
		solveBG = new ButtonGroup ();
		solveNone = new JRadioButtonMenuItem ("Off");
		solveOn = new JRadioButtonMenuItem ("On");
		solveNone.setToolTipText("Legacy solve-mode switch; currently not connected to an action.");
		solveOn.setToolTipText("Legacy solve-mode switch; use the Solve button below the score instead.");
		
		solveBG.add(solveNone);
		solveBG.add(solveOn);
		
		solve.add(solveNone);
		solve.add(solveOn);
		
		add(solve);
		// HELP
		
		boards = new JMenu("Boards");
		clearBtn = new JMenuItem("Reset Score and Panels");
		boards.add(clearBtn);
		
		add(boards);
		
		properties = new JMenu("Properties");
		reloadBtn = new JMenuItem("Reload Properties");
		resetBtn = new JMenuItem("Reset Properties");
		clearBtn.setToolTipText("Return to the start and clear fingerboards, Solve boxes, and position highlighting.");
		reloadBtn.setToolTipText("Reload fingering constraints from SFProperties.xml.");
		resetBtn.setToolTipText("Replace SFProperties.xml with the built-in defaults, then reload it.");
		properties.add(reloadBtn);
		properties.add(resetBtn);
		
		add(properties);
		
		JMenu trailsMenu = new JMenu("Trails");
		markUnique = new javax.swing.JCheckBoxMenuItem("Solve all");
		markUnique.setToolTipText("Finger the whole piece. Stretches with only one possible trail are marked; the rest are chosen by cost. Needs a score opened with File > Open score.");
		markUnique.addActionListener(this);
		trailsMenu.add(markUnique);
		add(trailsMenu);

		helpMenu = new JMenu("Help");
		controlsHelp = new JMenuItem("Controls");
		help = new JMenuItem("About");
		add(helpMenu);
		helpMenu.add(controlsHelp);
		helpMenu.add(help);
		controlsHelp.setToolTipText("Open a guide to the main controls and interaction shortcuts.");
		help.setToolTipText("Show version, copyright, and license information.");
		controlsHelp.addActionListener(this);
		help.addActionListener(this);
		clearBtn.addActionListener(this);
		reloadBtn.addActionListener(this);
		resetBtn.addActionListener(this);
		helpMenu.setMnemonic(KeyEvent.VK_H);
		// start with violin
		instrumentItems[0].setSelected(true);
		// start with 2 random chords
		rand1.setSelected(true);
		
		
		
	}
	
	public void setInstrument (String s) {
		//Instrument it = Instrument.ofString(s);
		Instrument it = new Instrument(big, s);
		appPanel.clearAll();
		//Fing.localInstrument = it;
		big.changeInstrument(it);
		//ctl.inst.rangeLabel.setText(it.rangeString());
	}
	
	/** Rebuild File > Open recent from the list on disk: a file that is gone is shown disabled. */
	void fillRecent() {
		openRecent.removeAll();
		java.util.List<java.io.File> files = appFrame.recent.list();
		for (final java.io.File f : files) {
			JMenuItem item = new JMenuItem(f.getName());
			item.setToolTipText(f.getPath());
			item.setEnabled(f.isFile());
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					appFrame.openScoreFile(f);
				}
			});
			openRecent.add(item);
		}
		if (files.isEmpty()) {
			JMenuItem none = new JMenuItem("(none yet)");
			none.setEnabled(false);
			openRecent.add(none);
		}
		openRecent.addSeparator();
		JMenuItem clear = new JMenuItem("Clear recent");
		clear.setEnabled(!files.isEmpty());
		clear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				appFrame.recent.clear();
				fillRecent();
			}
		});
		openRecent.add(clear);
	}

	public void  actionPerformed( ActionEvent e )
	
	{
		if (e.getSource() == markUnique){
			big.solveAll(markUnique.isSelected());
			if (markUnique.isSelected() && big.marked.isEmpty()) {
				markUnique.setSelected(false);
			}
		}
		if (e.getSource() == restartItem){
			appFrame.restart();
			return;
		}
		if (e.getSource() == readScore){
			javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(new java.io.File(System.getProperty("user.home"), "libmusic"));
			chooser.setDialogTitle("Open a score");
			if (chooser.showOpenDialog(appFrame) == javax.swing.JFileChooser.APPROVE_OPTION) {
				appFrame.openScoreFile(chooser.getSelectedFile());
			}
		}
		if (e.getSource() == readABC){
			// reads & draws a score
			//AbcReader abc = new AbcReader(big);
			//big.abcReader.readAndDraw();
			
			if (big.readandDrawABC(true)) { // boolean says whether to do fingering
				appFrame.noteOpened(big.abcReader.fc.getSelectedFile());
			}
			//System.out.println(abc.abcString);
			//ctl.abc.setH(5f);
			
		}
		if (e.getSource() == justABC){
			if (big.readandDrawABC(false)) { // boolean says whether to do fingering
				appFrame.noteOpened(big.abcReader.fc.getSelectedFile());
			}
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

		for (int k = 0; k < instrumentItems.length; k++) {
			if ( e.getSource() == instrumentItems[k] ){
				setInstrument(InstrumentSpec.ALL.get(k).shortName);
			}
		}
		
		if ( e.getSource() == help){
		AboutDialog.doDialog(appFrame);
		}
		if (e.getSource() == controlsHelp) {
			ControlsHelpDialog.doDialog(appFrame);
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
			big.resetScoreView();
		}
		if (e.getSource() == reloadBtn)
		{
			XMLPropertiesReader.main(null);
		}
		if(e.getSource() == resetBtn)
		{
			XMLPropertiesWriter.main(null);
			XMLPropertiesReader.main(null);
		}
	}	

}

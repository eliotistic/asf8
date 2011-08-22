package computingmusic;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import computingmusic.AbcReader.KNote;
import computingmusic.AnchorSequencer.State;

import abc.notation.Tune;
import abc.notation.Tune.Music;

/*
 * Right Panel: display of the Music File currently read
 */
public class BigScore extends JPanel{
	
	public BigGui appFrame;
	//public JOptionPane errorPane; 
	public Instrument instrument;
	private static final long serialVersionUID = 1L;
	protected static final int FAST_FORWARD_SPEED = 10;
	AbcReader abcReader;
	Cfinger2 cfinger;
	
	//public FingerBoard fingerBoard;
	
	public Glue gluer;
	
	public BigScoreComponent scoreUI;
	public static Music m;
	public Tune tune;
	
	//public ArpeggioSequencer.State doo;
	public XMode xmode;
	public int masterIndex = 0; // event # in score, this number is the location of the current fingering
	public ArrowKeys arrows;
	public ExtendPanel2 extendPanel;
	//public StepMode stepMode;// panel
	// public String stepValue;
	
	public String controlMode;
	//REMOVING RESULTABC
	//public ArrayList<ResultABC> fingResultList; // list of chords or notes with ABC info.
	public ArrayList<KNote> kNoteList;
	//public ArrayList<Fing.Result> fingResultList;
	public ArrayList<ChordFingering> fingResultList;
	
	
	
	
	//private FingerSeq2 currentFingerSeq2;
	// XXX
	// public ArrayList<FingerSeq2> fingerseq2List;
	
	// booleans
	
	public boolean filterBadToplogies;
	//public boolean extendingForward = false;
	
	public PositionalSequencer2 pseq2;
	public ArpeggioSequencer arpSeq;
	public AnchorSequencer anchSeq;
	public SolSequencer solSeq;
	
	@SuppressWarnings("unused")
	private ArpeggioSequencer.State arpSeqState; 
	public AnchorSequencer.State anchorSeqState;
	public SolSequencer.State solSeqState;
	
	//private ArrayList<PositionalSequencer2.Seq> currentSeq2Positions;
	//public PositionalSequencer2.Seq currentPositionalSeq2; 
	private boolean positionFollowsCursor = false;
	// old positional sequencer
	public String positionalMode;
	//public PositionalSequencer pseq;
	//public PositionalSequencer.Seq currPositionalSeq;
	//private ArrayList<PositionalSequencer.Seq> currentPositions;
	//private int currentPositionalPos; 
	//private int currentPositionalSize;
	JScrollPane sPane;
	
	public int getFingSize () {
		return fingResultList.size();
	}
	
	public void setArpSeqState(ArpeggioSequencer.State state) {
		arpSeqState = state;
	}
	
	public void setAnchorSeqState(AnchorSequencer.State state) {
		anchorSeqState = state;
		if (anchorSeqState == State.Off) {
			anchSeq.resetFingerboard(); // take away gray notes, trail
			xmode = XMode.Arp;
		} else {
			xmode = XMode.Anch;
		};
		doCurr();
		
	}
	
	
	public enum XMode{ // execution mode 
		Arp, Anch
	}
	
	public void rebuild(){
		
		arpSeq.rebuild();
		pseq2.rebuild();
	}
	public void setIndexArrows(){
		int sz = getFingSize();
		arrows.prev.setEnabled(masterIndex > 0);
		arrows.next.setEnabled(masterIndex + 1 < sz);	
	}
	
	public boolean hasNextFing(){
		int sz = getFingSize();
		return masterIndex - 1 < sz;
		
	}
	
	public void incrCurrentFing (){
		
		int sz = getFingSize();
		if (masterIndex - 1 < sz){
			masterIndex++;
		};
		// setIndexArrows();
			
	}
	
	public void decrCurrentFing(){
		if (masterIndex > 0){
			masterIndex--;
		};
	}
	public boolean hasPrevFind(){
		return masterIndex > 0;
	}
	
	public int getCurrentFing(){
		return masterIndex;
	}
	
	public void setCurrentFing(int i) {
		int sz = getFingSize();
		if (i >= 0 && i < sz) {
			masterIndex = i;
		}
	}
		
	public boolean isPositionFollowing (){
		return positionFollowsCursor;
	}
	
	public void doPositionFollowing(){
		if (positionFollowsCursor) {
			pseq2.setCurrentPositions(masterIndex);
		}
		
	}
	
	public void togglePositionFollowing () {
		if (pseq2 == null) return;
		positionFollowsCursor = !positionFollowsCursor;
		if (positionFollowsCursor) {
			pseq2.setCurrentPositions(masterIndex);
			
		}
	}
	public void stopPositionFollowing () {
		positionFollowsCursor = false;
	}
	
	public void turnoffPositions () {
		// currentPositions = null;
		//currPositionalSeq = null;
		scoreUI.turnoffPositions();
		// turn off panel
		extendPanel.disablePositions();
		// turn off restriction box
		
	
		positionFollowsCursor = false;
	}
	
	
	
	
	
	
	public void setInstrument(Instrument in) {
		instrument = in;
		cfinger.setInstrument(in);
	}
	
	public void arpMenuState (){}
	
	public void writeChordText(ChordFingering f){
		String text = f.noteStringforChordInput();
		cfinger.ctl.entryPanel.noteField.setText(text);
	}
	
	public BigScore() {
		// doesn't do anything -- for smaller Gui app.
	}
	
	public BigScore(BigGui app, Cfinger2 c) {
		appFrame = app;
		//errorPane = new JOptionPane();
		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();
		
		setLayout(new GridBagLayout());
		
		
		
		filterBadToplogies = false;
		
		cfinger = c;
		
	
			
		instrument = new Instrument(this, "VN"); // TODO add new Constraints for the Instrument here, like maxspan
		cfinger.setInstrument(instrument);
		
		gluer = new Glue(this);
		//setBackground(Color.white);
		
		abcReader = new AbcReader(this);
		scoreUI = new BigScoreComponent();
		
		
		//arpSeq = new ArpeggioSequencer(this);
		
		sPane = new JScrollPane(scoreUI);
		sPane.getViewport().setBackground(Color.white);
		sPane.getVerticalScrollBar().setUnitIncrement(15); // 15 seems legit
		scoreUI.giveScrollPaneAccess(sPane);
		arrows = new ArrowKeys(true);
		//arrows.setBackground(Color.white);
		arrows.setBorder(BorderFactory.createEtchedBorder());
		
		
		c1.gridx = 0;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.BOTH;
		c1.weighty = 0.65;
		c1.weightx = 0.5;
		add(sPane, c1);
		
		c2.gridx = 0;
		c2.gridy = 1;
		c2.fill = GridBagConstraints.VERTICAL;
		c2.weighty = 0;
		c2.weightx = 0;
		add(arrows, c2);
		
		
		
		
		
		arrows.disable();
		
		add(Box.createRigidArea(new Dimension(0, 5)));
		
		// NEXT 	
		// note that for this one, a MouseAdapter is used, since we want to isolate right click from left click
		arrows.next.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == 1 && e.getClickCount() == 1)//if left click
				{
					nextArrowActionPerformed();
				}
				else if (e.getButton() == 3)// if right click
				{
					jumpNextArrowActionPerformed();
				}
				else if(e.getClickCount() == 2)
            	{
            		for(int i = 0; i<FAST_FORWARD_SPEED; i++)
            		{
            			nextArrowActionPerformed();
            		}
            	}
				requestFocusInWindow();
				arrows.setBorder(BorderFactory.createLineBorder(Color.black));
				appFrame.cfinger.ctl.entryPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
			}
		});
		// PREV
		arrows.prev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prevArrowActionPerformed();
				requestFocusInWindow();
				arrows.setBorder(BorderFactory.createLineBorder(Color.black));
				appFrame.cfinger.ctl.entryPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
			}
		});
		
		// START 
		arrows.start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doStart();
				setIndexArrows();
				//System.out.println("Location of Scroll: " + sPane.getVerticalScrollBar().getValue());
				if(0 != sPane.getVerticalScrollBar().getValue())
				{
					sPane.getVerticalScrollBar().setValue(0);
				}
				sPane.getHorizontalScrollBar().setValue(0);
			}
		});
		
		// STOP
		arrows.stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				arpSeq.doEnd();
				setIndexArrows();
				sPane.getVerticalScrollBar().setValue(sPane.getVerticalScrollBar().getMaximum());
				sPane.getHorizontalScrollBar().setValue(sPane.getHorizontalScrollBar().getMaximum());
			}
		});
		arrows.setBorder(BorderFactory.createLineBorder(Color.black));
		extendPanel = new ExtendPanel2();
		
		extendPanel.disable();
		 
		 
		// SHRINK
		 
		extendPanel.shrink.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				shrink();
				
			}
		});
		extendPanel.extend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				extend();
			}
		});
		
		
		 
		// POSITIONS
		extendPanel.positions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				pseq2.setCurrentPositions(masterIndex);
				//big.cfinger.fing.clearTrails();
				
				
			}
		});
		extendPanel.posArrows.next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cfinger.fingPane.getCurrentBoard().clearTrails();
				pseq2.nextPosition();
			}
		});
		extendPanel.posArrows.prev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cfinger.fingPane.getCurrentBoard().clearTrails();
				pseq2.prevPosition();
			}
		});
		extendPanel.solve.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Hit position restrict");
				solSeq.solve();				}
		});
		extendPanel.clearSolvesBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO implement. 
				
				// TODO set back the XMode?
				// TODO AnchorSequencer.autoUpdateAchor() was called. Do we have to cancel?
				boolean success = anchSeq.currAnchor.extendBack(); // TODO does that cancel the previous extend()?
				System.out.println("Extend back success: " + success);
				scoreUI.clearAnnotationBoxes(); // gg
				anchSeq.doCurr(); // actually works
			}
		});
		extendPanel.positionRestrict.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Hit position restrict");
				cfinger.fingPane.getCurrentBoard().clearTrails();
				//showCurrFing();
				arpSeq.doCurr();
			}
		});
		extendPanel.fingerTrails.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean state = extendPanel.fingerTrails.isSelected();
				// System.out.println("Fingertrails enabled = " + state);
				cfinger.fingPane.getCurrentBoard().setFingerTrails(state);
				//showCurrFing();
			}
		});
		 
	    //stepMode = new StepMode(this);
	    //add(stepMode);
		
		
	
		c3.gridx=0;
		c3.gridy=2;
		c3.fill = GridBagConstraints.BOTH;
		
		add(extendPanel, c3);
		
		
		initFingering();
		scoreUI.setBackground(Color.white);
	    //controlMode = "CHORD";
		
		
	}
	
	
	
	public void prevArrowActionPerformed() {
		doPrev();
		setIndexArrows();
	}

	public void nextArrowActionPerformed() {
		doNext();
		setIndexArrows();
	}
	
	public void jumpNextArrowActionPerformed()
	{
		doJumpNext();
		setIndexArrows();
	}
	
	public void jumpToMasterIndex(int masterInd)
	{
		masterIndex = masterInd;
		anchSeq.doCurr();
	}
	
	public void jumpToIndexAndExtend(int masterInd, int extensionSize)
	{
		masterIndex = masterInd;
		doCurr();
		/*for(int i = 0; i<extensionSize-1; i++)
		{
			extend();
		}*/
		/*scoreUI.setBoxedNotes(masterIndex, stop)*/
	}

	public void toggleBadTopologies (){
		filterBadToplogies = !filterBadToplogies;
		//toggleBadTopologies();
		
		System.out.println("filterBadTopologies:" + instrument.filterBadTopologies());
	}
	
	
	/*
	public class MyAction implements AdjustmentListener{
		public void adjustmentValueChanged(AdjustmentEvent ae){
			int value = ae.getValue();
			String st = Integer.toString(value);
			//field.setText(st);
		}
	}
	*/
	
	public void makePositionalSeq2(PositionalSequencer2 oldPosSeq) {
		// for now, we build it here
		if (arpSeq.fingerseq2List == null) {
			return;
		} else {
			pseq2 = new PositionalSequencer2(this);
			
			
			
		}
	}
	
	public void makeArpeggioSequencer(ArpeggioSequencer oldArpSeq) {
		if (fingResultList == null) {
			return;
		} else {
			
			arpSeq = new ArpeggioSequencer(this, fingResultList, instrument);
			arpSeq.inheritState(oldArpSeq);
			
		}
	}
	
	public void makeAnchorSequencer(AnchorSequencer oldAncSeq) {
		if (fingResultList == null) {
			return;
		} else {
			anchSeq = new AnchorSequencer(this, fingResultList, instrument);
			anchSeq.inheritState(oldAncSeq);
		
			anchSeq.rebuild();
			
		}
	}
	
	public void makeSolSequencer(SolSequencer oldSolSeq) {
		if (fingResultList == null) {
			return;
		} else {
			solSeq = new SolSequencer(this, fingResultList, instrument);
			solSeq.inheritState(oldSolSeq);
			
			//anchSeq.rebuild();
			
		}
	}
	

	public void readandDrawABC (boolean fingering){
		abcReader.read();
		if (!abcReader.readError) {
			//cfinger.clearAll(); // TODO added by jb
			cfinger.fingPane.clearAll();
			tune = abcReader.tune;
			
			//scoreUI.noteList = abcReader.noteList;
			//scoreUI.setTune(tune);
			scoreUI.setABCTune(abcReader);
			
			String inst = tune.getInformation();
			if (inst == null) {
				inst = "violin";
			}
			;
			//setInstrument( Instrument.ofString(inst));
			setInstrument(new Instrument(this,inst));
			//cfinger.setInstrument(instrument);
			System.out.println("Instrument: " + instrument.getInstName());
			if (fingering) {
				initFingering();
			}
			if(appFrame.prop_anchors_on)
			{
				anchSeq.setStateOn();
			}
			cfinger.fingPane.giveAccess(this);

			// scoreUI.repaint();
		}
		
	}
	
	
	// top level, called from ABCReader.
	public void setXmode () {
		if (anchSeq.isOn()){
			xmode = XMode.Anch;
		} else {
			xmode = XMode.Arp;
		
		}
	}
	
	public void setXmode (XMode x) {
		xmode = x;
	}
	public void doCurr(){
		if (xmode == XMode.Anch){
			anchSeq.doCurr();
		} else {
			arpSeq.doCurr();
		}
	}
	public void doNext(){
		if (xmode == XMode.Anch){
			anchSeq.doNext();
		} else {
			arpSeq.doNext();
		}
	}
	public void doJumpNext(){
		if (xmode == XMode.Anch){
			anchSeq.doJumpNext();
		} else {
			//arpSeq.doNext();TODO
		}
	}
	public void doPrev(){
		if (xmode == XMode.Anch){
			anchSeq.doPrev();
		} else {
			arpSeq.doPrev();
		}
	}
	public void doStart(){
		if (xmode == XMode.Anch){
			anchSeq.doStart();
		} else {
			arpSeq.doStart();
		}
	}
	
	
	public void extend(){
		if (xmode == XMode.Anch){
			anchSeq.extend();
		} else {
			arpSeq.extend();
		}
	}
	
	public void shrink(){
		if (xmode == XMode.Anch){
			anchSeq.shrink();
		} else {
			arpSeq.shrink();
		}
	}
	
	
	public void initFingering() {
		masterIndex = 0;
		kNoteList = abcReader.kNotes;
		
		genFingeringResultList();
		
		
		// 
		makeArpeggioSequencer (arpSeq);
		 //makeFingerSeqList();
		
		makePositionalSeq2(pseq2);
		
		makeAnchorSequencer(anchSeq);
		makeSolSequencer(solSeq);
		
		if (positionFollowsCursor)pseq2.setCurrentPositions(masterIndex);
		
		//showCurrFing();
		
		setXmode();
		
		doCurr();
		
		
		
		arrows.next.setEnabled(true);
		arrows.start.setEnabled(true);
		arrows.stop.setEnabled(true);
		//extendPanel.positions.setEnabled(true);
	}
		
	
	
	
	
	/*
	
	public void genFingeringResultList () {
		
		ArrayList<MusicElement> noteList = scoreUI.noteList;
		
		//fingResultList = new ArrayList<ResultABC>();
		fingResultList = new ArrayList<Fing.Result>();
		
		
	
		//int eltN = -1;
		for (int ind = 0; ind<noteList.size(); ind++) {
			MusicElement elt = (MusicElement) noteList.get(ind);
	
			if (elt instanceof MultiNote) {
				Note[] notes = ((MultiNote) elt).toArray();
				//int stringN = 0;
				Keynum.K[] karr = new Keynum.K[notes.length];
				for (int i = 0; i<notes.length; i++){
					karr[i] = getNote(notes[i]);
				
				};
				Keynum.Result rr = new Keynum.Result(karr);
				Fing.Result fr = Fing.runKResult(ind, instrument, rr);
				//Fing.Result fr = new Fing.Result(rr);
				//ResultABC abc = new ResultABC(eltN,fr, elt);
			
				fingResultList.add(fr);
				
			} else if (elt instanceof Note){
				if (((Note) elt).isRest()) {
					// eltN++;
					System.out.println("[warning:] Should not have encountered REST at " + ind);
				} else {
				
				Keynum.K k = getNote((Note) elt);
				Keynum.Result rr = new Keynum.Result(k);
				//Fing.Result fr = new Fing.Result(rr);
				Fing.Result fr = Fing.runKResult(ind, instrument, rr);
				//ResultABC abc = new ResultABC(eltN, fr, elt);
				fingResultList.add(fr);
			}
		}; // while
	}
	}
	*/
	

	public void genFingeringResultList() {

		fingResultList = new ArrayList<ChordFingering>();
		
		for (int ind = 0; ind < kNoteList.size(); ind++) {
			KNote kn = kNoteList.get(ind);
			// we can probably get rid of this intermediate Result
			//Keynum.Result rr = new Keynum.Result(kn.ks);
			//Fing.Result fr = Fing.runKResult(ind, instrument, rr);
			ChordFingering fr = new ChordFingering(ind, instrument, kn.ks);
			if (fr.impossible()) {
				System.out.println("Setting RED: " + ind);
				scoreUI.addRedNote(ind);
			}
			fingResultList.add(fr);
		}

	}
	
	
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	public Dimension getPreferredSize() {
		return new Dimension(550,600);
	}
	/*
	public static void main (String[] arg) {
		JFrame j = new JFrame();
		j.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		BigScore u = new BigScore();
		j.add(u);
		j.pack();
		j.setVisible(true);
		String xx = "X:1\nT:Chords\nK:C\nL:1/4\n"
				//+ "[_b_B]  [=B_B]  [_B=B]  a ";
		+ "g_bgdgd_Bd_BG_BGDGD";
		// JScoreComponent u = ofString(xx);
		u.drawString(xx);
		// u.testgfx();
		// u.drawString(xx);
		//u.markNote();
		
		
	}
	*/


		
}

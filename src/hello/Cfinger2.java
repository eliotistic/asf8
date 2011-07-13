package hello;



import hello.BigScore.XMode;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/*
 * Left Panel: contains both the  fingering selection and the instrument representation.
 */
public class Cfinger2 extends JPanel {
	
	private static final long serialVersionUID = -1155655565308041186L;
	
	private Instrument instrument;
	//FingerBoard originalFingBoard;
	
	public CtlBox ctl;
	public BigGui appFrame;
	public BigScore big;
	public int randomChords = 1;
	public RandomChord randc;
	public boolean random4stop = false;
	public FingerBoardsPanel fingPane; // jb
	public int problemType; // 1 if just one, 2 if transitions, else err
	public int nSolutions = 0;
	public int currSolution = 0;
	public int badToplogies = 0;
	public double counter = 0;
	public ChordFingering CurrentCF1;
	public ArrayList<FingerTrail> activeTrails;
	public int indexViewActiveTrail = 0;
	// ChordFingering CurrentCF2;
	//Fing.Result fingResult1;
	//Fing.Result fingResult2;
	
	public Cfinger2(BigGui frame) {
		// super (new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Make a default instrument, but this has to be set.
		//instrument = new Instrument("VN");
		randc = new RandomChord(this);
		ctl = new CtlBox(this);
		appFrame = frame;
		
		//setLayout(new BorderLayout());
		//add("West", ctl);
		setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		

		
		//originalFingBoard = new FingerBoard();
		/*originalFingBoard.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent evt) {
                int x_position = evt.getX();
                int y_position = evt.getY();
                //System.out.println("X:" + x_position);
                //System.out.println("Y:" + y_position);
                //int noteHeight = getHeightAtLocation(x_position, y_position); // if coords do not enter a note, then noteHeight = -1
                int noteHeight = 0; // TODO default set here.
                if(noteHeight != -1)
                {
                	int abcNoteHeight = noteHeight + 55;
	                //System.out.println("abcNoteHeight is: " + abcNoteHeight);
	                ctl.currentTextDisplay.setText("Note: " + abcNoteHeight); 
                }
                
            }
            
        });
		
		originalFingBoard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                System.out.println("Ps1: ");
                printEachPoint(fingBoard.ps1);
                System.out.println("Ps2: ");
                printEachPoint(fingBoard.ps2);
            	System.out.println("Pscom: ");
                printEachPoint(fingBoard.psCom);
            	System.out.println("gray: ");
                printEachPoint(fingBoard.grayNotes);
            	int x_position = evt.getX();
                int y_position = evt.getY();
                //System.out.println("X:" + x_position);
                //System.out.println("Y:" + y_position);
                Point noteCoords = getNoteCoordsAtLocation(x_position, y_position);
                System.out.println("Note coords at this location: " + noteCoords);
                if(noteCoords.x != -1) // could have tested noteCoords.y
                {
                	if(originalFingBoard.hasActiveNoteInColumn(noteCoords.x))
                	{
                		// if the note clicked is active, then delete it from the active notes
                		if(originalFingBoard.hasActiveNote(noteCoords))
                		{
                			// TODO implement
                			System.out.println("You just clicked on a note.");
                		}
                		// if the clicked note is not active, then change the active note in that column to that one
                		else 
                		{
                			// TODO Implement.
                			System.out.println("You just clicked in a column with a note.");
                		}
                		
                	}
                	else 
                	{
                		System.out.println("You clicked in a column without a note.");
                	}
                }
            }
        });*/
		
		
		
		fingPane = new FingerBoardsPanel();
		JScrollPane fingScrollPane = new JScrollPane(fingPane);
		JSplitPane secondSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		secondSplit.setTopComponent(ctl);
		secondSplit.setBottomComponent(fingScrollPane);
		c1.gridx = 0;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.BOTH;
		c1.weightx = 0.5;
		c1.weighty = 0.5;
		add(secondSplit, c1);
		
		/*else 
		{
			c2.gridx = 1;
			c2.gridy = 0;
			c2.fill = GridBagConstraints.VERTICAL;
			c2.weightx = 0.5;
			c2.weighty = 0.5;
			c2.anchor = GridBagConstraints.CENTER;
			add(fingBoard, c2);
		}*/
		
		

		
		
		
		ctl.entryPanel.clearBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				clearAll();
				ctl.entryPanel.clear();
				// ctl.abc.drawString(notes);
			}
		});
		
		
		ctl.entryPanel.randBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (random4stop) {
					genRandomStops(4);
				} else
					genRandomChords();
			}
		});
			
		
		ctl.entryPanel.fingerBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				clearAll();
		    	doInputFingerings();
				//FingeringNextActionPerformed(evt);
			}
		});
		
		// PREV
		ctl.entryPanel.prevBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				prevFingering();
				requestFocusInWindow();
				big.arrows.setBorder(BorderFactory.createLineBorder(Color.gray));
				ctl.entryPanel.setBorder(BorderFactory.createLineBorder(Color.black));
			}
		});
		
		// NEXT
		ctl.entryPanel.nextBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				nextFingering();
				requestFocusInWindow();
				big.arrows.setBorder(BorderFactory.createLineBorder(Color.gray));
				ctl.entryPanel.setBorder(BorderFactory.createLineBorder(Color.black));
			}
		});

		ctl.entryPanel.noteField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				doInputFingerings();
				// noteFieldActionPerformed(evt);
			}
		});
		 // GLUE
		
		 ctl.gluePanel.glue.addActionListener(new ActionListener() {
		 	public void actionPerformed(ActionEvent e) {
		 		big.gluer.setGlue(CurrentCF1);
		 	}
		 });
	
		 ctl.gluePanel.unglue.addActionListener(new ActionListener() {
			 public void actionPerformed(ActionEvent e) {
		 		big.gluer.unGlue(CurrentCF1);
		 		/*
		 		CurrentCF1.unGlue();
		 		big.rebuild();
		 		setGlueButtons();
		 		ctl.entry.setArrows(CurrentCF1);
		 		big.doPositionFollowing();
		 	*/
		 	}
		 });
			 
		 ctl.showGlue.addActionListener(new ActionListener() {
		 	public void actionPerformed(ActionEvent e) {
		 		big.arpSeq.showGlue();		
		 	}
		 });
		 
		 
		
		/*
		ctl.fbp.fingerTrails.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			
				boolean state = ctl.fbp.fingerTrails.isSelected();
				// System.out.println("Fingertrails enabled = " + state);
				fing.setFingerTrails(state);
				//showCurrFing();
			}
		});
	 	*/

		// addBindings();
	};
	
	protected void printEachPoint(Point[] ps1) {
		for(Point p : ps1)
		{
			System.out.println("Point: " + p);
		}
	}
	


	public void setScore (BigScore b) {
		big = b;
	}
	
	public void genRandomStops (int n) {
		clearAll();
		String r1 = randc.gen(n);
		ctl.entryPanel.noteField.setText(r1);
		doInputFingerings();
		if (nSolutions < 1){
			// no solutions -- try again
			//System.out.println("random: recurse");
			genRandomStops(n);
		}
	}
	public void setInstrument (Instrument it){
		instrument = it;
		
		fingPane.setInstrument(it);// TODO
		
		/*else 
		{
			fingBoard.instrument = it;
		}*/
		
		ctl.instPanel.rangeLabel.setText(it.rangeString());
	}
	
	public Instrument getInstrument (){
		return instrument;
	}
	public void genRandomChords() {
		clearAll();
		String r1 = randc.gen();
		
		ctl.entryPanel.noteField.setText(r1);
		
		if (randomChords == 1) {
			//ctl.entry.noteField2.setText(null);
		}
		
		if (randomChords > 1){
		//String r2 = randc.gen();
		
		//ctl.entry.noteField2.setText(r2);
		}
		// ctl.entry.finger.setSelected(true);

		doInputFingerings();
		
		if (nSolutions < 1){
			// no solutions -- try again: recurse");
			genRandomChords();
			//System.out.println("random
		}
		
		
	}
	
	/*
	private void setGlueButtons(){
		// buttons ordered glue, unglue
		if (CurrentCF1==null){
			ctl.gluePanel.setButtons(false, false);
		} else if (CurrentCF1.isGlued()){
			ctl.gluePanel.setButtons(false, true);
		} else if (CurrentCF1.nFingerings()>1){
			ctl.gluePanel.setButtons(true, false);
		} else if (CurrentCF1.isArp()){
			ctl.gluePanel.setButtons(true, false);
		} else {
			ctl.gluePanel.setButtons(false, false);
		}
	}
	*/
	
	// private final static String newline = "\n";
	
	
	
	/**
	 * @author JeanBenoit
	 * This method takes as parameters the location of the cursor in the FingerBoard, and
	 * returns the height of the note.
	 * 
	 * @param x_position The location in X
	 * @param y_position The location in Y
	 * @return The height of the note, or -1 if the mouse is not inside a note. 
	 */
	/*protected int getHeightAtLocation(int x_position, int y_position) {
		// fing.DOT = 12  (radius = 6) 
		int found = -1;
		// OMG APPLIED MATHS 
		int radiusSquared = FingerBoard.DOT*FingerBoard.DOT/4; // diameter squared divided by 4 = radius squared
		for(int i = 0; i< fingBoard.noteCoordinates.length; i++)
		{
			for(int j = 0; j<fingBoard.noteCoordinates[i].length; j++)
			{
				Point x_noteP = fingBoard.getNoteCoordinates()[i][j];
				//System.out.println("at i = " + i);
				//System.out.println("and j = "+j);
				//System.out.println(x_noteP);
				int x_note = (int)x_noteP.getX();
				int y_note = fingBoard.getNoteCoordinates()[i][j].y;
				if((x_position-x_note)*(x_position-x_note)+(y_position-y_note)*(y_position-y_note) <= radiusSquared)
				{
					found = i + 7*j; // i + 7j: note height. j is the number of the chord: on the 3rd chord (j=2), the note
					// is 14 places higher than on the first. 
				}
			}
		}
		//System.out.println("Found : height ="+found);
		return found;
	}*/ // TODO How do we now use this method?
	
	/**
	 * @author JeanBenoit
	 * This method takes as parameters the location of the cursor in the FingerBoard, and
	 * returns the coordinates of the note.
	 * 
	 * @param x_position The location in X
	 * @param y_position The location in Y
	 * @return The coordinates of the note, or (-1, -1) if the mouse is not inside a note. dd
	 */
	/*protected Point getNoteCoordsAtLocation(int x_position, int y_position) {
		// fing.DOT = 12  (radius = 6) 
		Point found = new Point(-1, -1);
		// OMG APPLIED MATHS 
		int radiusSquared = FingerBoard.DOT*FingerBoard.DOT/4; // diameter squared divided by 4 = radius squared
		for(int i = 0; i< fingBoard.noteCoordinates.length; i++) // for each row
		{
			for(int j = 0; j<fingBoard.noteCoordinates[i].length; j++) // for each column
			{
				Point x_noteP = fingBoard.getNoteCoordinates()[i][j];
				//System.out.println("at i = " + i);
				//System.out.println("and j = "+j);
				//System.out.println(x_noteP);
				int x_note = (int)x_noteP.getX();
				int y_note = fingBoard.getNoteCoordinates()[i][j].y;
				if((x_position-x_note)*(x_position-x_note)+(y_position-y_note)*(y_position-y_note) <= radiusSquared)
				{
					found = new Point(j, i); 
				}
			}
		}
		return found; TODO same as above
	}*/

	/*
	 * 
	 * private Action myYak = new AbstractAction() { public void
	 * actionPerformed(ActionEvent e) { ctl.msg.say("YAH, YOU HIT DAT BUTTON.");
	 * } };
	 * 
	 * KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT); KeyStroke down
	 * = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN);
	 * 
	 * 
	 * protected void addBindings() { InputMap inputMap = this.getInputMap();
	 * KeyStroke key = KeyStroke.getKeyStroke("F9"); inputMap.put(key,
	 * "myohmy"); this.getActionMap().put("myohmy", myYak);
	 * 
	 * };
	 */
	// addB
	// this.addB

	// public void clear() {
	// ctl.next.disable();};
	// hitting return triggers fingering button.
	

	//String notes = "X:1\nT:Chords\nM:C\nK:C\nL:1/1\n"+ " [A,_Ece] ";
	
	
	
	public void clearAll() {
		// leave text as is
		nSolutions = 0;
		
		//fingResult1 = null;
		//fingResult2 = null;
		
		ctl.entryPanel.clearArrows();
		ctl.rate.clear();
		ctl.msg.clear(); // sets msg to default message
		ctl.topo.clear();
		//ctl.positions.clear();
		fingPane.getCurrentBoard().clear(); // TODO maybe clear all the boards
		// ctl.abc.drawChord("");
		ctl.abcChord.clear();
	}
	/*
	private void setGlue (){
		ChordFingering r = CurrentCF1;
		if (r == null){
			return;
		} else{
			r.setGlue();
		}
	}
	*/
	
	
	public void setActiveTrails(ArrayList<FingerTrail> a) {
		activeTrails = a;
		
		indexViewActiveTrail = 0; // index of the active trail to look at
		// note that the difference between a Fingering and a FingerTrail is that the Trail is made from many notes (anchor)
		viewTrail();
		//ctl.entry.setArrows(0, activeTrails.size()-1);
	}
	
	public void setActiveTrailsFromPrevious(ArrayList<FingerTrail> newActiveTrails) {
		boolean extendingExisted = false;
		FingerTrail prevFing = activeTrails.get(indexViewActiveTrail);
		indexViewActiveTrail = 0;
		for(int i = 0; i<newActiveTrails.size(); i++)
		{
			if(newActiveTrails.get(i).getStoppedPoints().IsExtensionOf(prevFing.getStoppedPoints())) // TODO AND we keep open string
			{
				System.out.println("previous saiten: " + prevFing.saiten);
				System.out.println("next saiten: " + newActiveTrails.get(i).saiten);
				indexViewActiveTrail = i;
				System.out.println("index is: " + i);
				extendingExisted = true;
			}
		}
		if(!extendingExisted)
		{
			System.out.println("There was no way to keep previous FingerTrail.");
			ctl.currentTextDisplay.setText("There was no way to keep previous FingerTrail.");
		}
		
		activeTrails = newActiveTrails;
		
		
		// note that the difference between a Fingering and a FingerTrail is that the Trail is made from many notes (anchor)
		viewNextTrail();
		
	}
	
	private void viewTrail() {
		if (activeTrails == null) {
			//System.out.println("No active trails in CFINGER");
			return;
		} else {
			FingerTrail ft = activeTrails.get(indexViewActiveTrail); // TODO out of bounds exception 1
			int max = activeTrails.size() - 1;
			ctl.entryPanel.setArrows(indexViewActiveTrail, activeTrails.size() - 1);
			
			//System.out.println("Showng trail #" + indexViewActiveTrail + "/" + max);
			
			String s = "Showing: " + (indexViewActiveTrail+1) + "/" + activeTrails.size();
			ctl.msg.say("<html>" + s + "<br>" + (ft.describe()) + "</html>");
			
			//System.out.println(ft);
			
			//fing.setBvMap(ft.heightMap);
			
			
			fingPane.getCurrentBoard().setFingerTrail(ft);
			
			/*else
			{
				fingBoard.setFingerTrail(ft);
			}*/
			
		}
	}
	
	private void viewNextTrail() {
		if (activeTrails == null) {
			//System.out.println("No active trails in CFINGER");
			return;
		} else {
			FingerTrail ft = activeTrails.get(indexViewActiveTrail); // TODO out of bounds exception 2
			int max = activeTrails.size() - 1;
			ctl.entryPanel.setArrows(indexViewActiveTrail, activeTrails.size() - 1);
			
			//System.out.println("Showng trail #" + indexViewActiveTrail + "/" + max);
			
			String s = "Showing: " + (indexViewActiveTrail+1) + "/" + activeTrails.size();
			ctl.msg.say("<html>" + s + "<br>" + (ft.describe()) + "</html>");
			
			//System.out.println(ft);
			
			//fing.setBvMap(ft.heightMap);
			
			fingPane.addFingerTrail(ft);
			
			/*else
			{
				fingBoard.setFingerTrail(ft);
			}*/
			
		}
	}
	
	private void anchorNext(){
		indexViewActiveTrail++;
		viewTrail();
		
			
	}
	private void anchorPrev(){
		indexViewActiveTrail--;
		//ctl.entry.setArrows(indexViewActiveTrail, activeTrails.size()-1);
		viewTrail();
		
			
	}
	
	public void prevFingering () {
		int stops = 0;
		if (big.xmode == XMode.Anch){
			anchorPrev();
		}
		else {
			//Fing.Result r = Fing.fingeringResult;
			ChordFingering r = CurrentCF1;
			if (r!=null && r.hasPrevFingering()) {
				ChordFingering.Fingering f0 = r.getPrevFingering();
				//f0.sayContour();
				Point[] coords = f0.getCoords();
				// int[] note = DrawCoords.getNoteStringCoords(Fing.Instr.VN, 0,
				// 2);
				fingPane.getCurrentBoard().setFingers(coords);
				// ctl.abc.drawChord(f0.getABCString());
				ctl.abcChord.drawFingering(f0);
				sayTopology(f0);
				//sayPositions(f0);
			
				//ctl.topo.say("Topology: " + f0.getTopology().toString());
				//f0.printToplogy();
				stops = f0.countStops();
			}
			;
			// turn off rating panel if just one stop.
			ctl.rate.maybeEnable(stops);
			ctl.entryPanel.setArrows(r);
		}
	}
	
	public void nextFingering() {
		if (big.xmode == XMode.Anch){
			anchorNext();
		}  else {
			//int stops = 0;
			//Fing.Result r = Fing.fingeringResult;
			ChordFingering r = CurrentCF1;
			if (r!=null && r.hasNextFingering()) {
				ChordFingering.Fingering f0 = r.getNextFingering();// increments counter
				//f0.sayContour();
				Point[] coords = f0.getCoords();
				// int[] note = DrawCoords.getNoteStringCoords(Fing.Instr.VN, 0,
				// 2);
				fingPane.getCurrentBoard().setFingers(coords);
				// ctl.abc.drawChord(f0.getABCString());
				ctl.abcChord.drawFingering(f0);
				sayTopology(f0);
				//sayPositions(f0);
				//ctl.topo.say("Topology: " + f0.getTopology().toString());
				//f0.printToplogy();
				//stops = f0.countStops();
			}

			//ctl.rate.maybeEnable(stops);
			ctl.entryPanel.setArrows(r);
		}
	}

	public String howMany(ChordFingering r) {
		int nSol = r.nFingerings();
		// System.out.println( "How many? " + nSol);
		int badTopologies = r.countBadToplogies();
		int hidden = r.countHidden();
		if (nSol > 1) {
			return nSol + " solutions (" + badTopologies 
				+ " bad, " + hidden + " hidden)";

		} else if (nSol == 0) {
			return "Impossible";
		} else {
			return "1 solution (" + badTopologies 
			+ " bad, " + hidden + " hidden)";
		}
	}

	
	static final String xx = "X:1\nT:Chords\nM:C\nK:C\nL:1/1\n"
	// + " [C_EGc] [C2G2] [CE][DF]|[D2F2][EG][FA] [A4d4]|]"
			+ " [G,_EGc] ";
	
	public void sayTopology(ChordFingering.Fingering f) {
		String s = f.getTopology().toString();
		if (f.badToplogy()){
			ctl.topo.say("<html>Toplogy: <font color=red>" + s + "</font></html>");
		} else
		ctl.topo.say("Topology: " + s );
	}
	/*
	public void sayPositions(ChordFingering.Fingering f) {
		String s = f.getPositions().say();
		ctl.positions.say(s);
	}
	*/
	
	@SuppressWarnings("unused")
	private void sayBad(Keynum.Result r) {
		ctl.msg.say("<html>Bad input: " + "<font color = red>"
				+ r.badParseString() + "</font></html>");
	}

	@SuppressWarnings("unused")
	private void sayWrong(Keynum.Result r) {
		ctl.msg.say("<html><font color=red>" + "Can only take up to 4 notes"
				+ "</font></html>");
	}

	@SuppressWarnings("unused")
	private void sayImpossible(Keynum.Result r) {
		clearAll();
		ctl.msg.say("<html><font color=red>impossible</font></html>");
		
		
	}
	private void sayImpossible(ChordFingering r) {
		clearAll();
		ctl.msg.say("<html><font color=red>impossible</font></html>");
		
		
	}

	public void doInputFingerings() {
		nSolutions = 0;
		problemType = 1; // obsolete
		// System.out.println("===doFingerings is called");
		String input1 = ctl.entryPanel.noteField.getText();
		//System.out.println("input 1 is: " + input1);
		Keynum.Result r1 = Keynum.parse(input1);
		if (r1.noInput()) return;
		doKeynumFingering1(r1);
		return;
	}
		
	
	public void doArpeggiation(ChordFingering r){
		CurrentCF1 = r;
		//System.out.println("doArpeggiation");
		doCurrentFingering();
	}
	
	// anchors -- show fingerTRails
	public void doTrails (ArrayList<FingerTrail> activeTrails){
		
	}
	
	
	
	
	// handling case where just one chord.
	
	public void doFingering1(ChordFingering r) {
		CurrentCF1 = r;
		//System.out.println("doFingering1 with msg: " + msg);
		doCurrentFingering();
		//System.out.println("===> FINISHED doFingering1 with msg: " + msg);
		
	}
	public void doFingering1(ChordFingering r, String msg) {
		
		//System.out.println("doFingering1 with msg: " + msg);
		doFingering1(r);
		//System.out.println("===> FINISHED doFingering1 with msg: " + msg);
		
	}
	
	
	// ok if no input
	public boolean keynumsOK (Keynum.Result r){
		if (r.noInput()) {
			return true;
		} else if (r.badInput()) {
			ctl.msg.say("<html>Bad input: " + "<font color = red>"
					+ r.badParseString() + "</font></html>");
			return false;
		} else if (r.wrongNumberofNotes()) {
			ctl.msg.say("<html><font color=red>"
					+ "Can only take up to 4 notes" + "</font></html>");
			return false;
		} else return true;
		
	}
	
	public void doKeynumFingering1(Keynum.Result r) {
		if (r.noInput()) {
			return;
		};
		// Fing.fingeringResult = r;
		if (!keynumsOK(r)) {
			return;
		}
		
		CurrentCF1 = new ChordFingering(0, instrument, r.ks);
		//CurrentCF2 = null;
		doCurrentFingering();
	}
	
	/**
	 * Using only the field CurrentCF1, do (display) the fingering. 
	 */
	public void doCurrentFingering() {
		int nSol = CurrentCF1.nFingerings();
		if (nSol == 0) {
			sayImpossible(CurrentCF1);
			//ctl.msg.say("<html><font color=red>impossible</font></html>");
			return;
		}
		if (CurrentCF1.isGlued()) {
			// System.out.println("CURRENT FINGERING IS GLUED with index = " + CurrentCF1.index);
		}
		ctl.msg.say(howMany(CurrentCF1));
		if (CurrentCF1.isGlued()){
			System.out.println("Glued");
		}
		// System.out.println("-->" + (howMany(r)));
		
		//int n = CurrentCF1.fingerings.size();
		// System.out.println("===CFINGER: doFingering1: size = " + n);
		nSolutions = nSol;
		badToplogies = CurrentCF1.countBadToplogies();
		big.gluer.setGlueButtons(CurrentCF1);
		if (nSol > 0) {
			
			ctl.entryPanel.prevBtn.setEnabled(false);

			ChordFingering.Fingering f0 = CurrentCF1.getFingerings().get(0);
			// String abcS = f0.getABCString();
			// ctl.abc.drawChord(f0.getABCString());
			//f0.sayContour();
				
			ctl.abcChord.drawFingering(f0);
			// System.out.println(abcS);
			Point[] coords = f0.getCoords();
			
			for (Point p : coords) {
				System.out.println(p.x + " " + p.y);
			}
			
			
			// int[] note = DrawCoords.getNoteStringCoords(Fing.Instr.VN, 0, 2);
			fingPane.getCurrentBoard().setFingers(coords);
			//stops = f0.countStops();
			//ctl.topo.say("Topolgy:" + f0.getTopology().toString());
			sayTopology(f0);
			//sayPositions(f0);
			//f0.printToplogy();
			//ctl.rate.maybeEnable(stops);
			/*if (nSol > 1) {
				ctl.entryPanel.nextBtn.setEnabled(true);
			} else {
				ctl.entryPanel.nextBtn.setEnabled(false);
			}*/
			ctl.entryPanel.nextBtn.setEnabled(nSol > 1);
		}
	}
}

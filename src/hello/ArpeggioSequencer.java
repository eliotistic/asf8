package hello;


//import java.awt.event.ActionEvent;
import hello.utils.MySeq;

import java.util.ArrayList;

//import com.sun.java.util.jar.pack.ConstantPool.Index;

//import abc.notation.MusicElement;

public class ArpeggioSequencer {
	BigScore big;
	
	
	public Instrument instrument;

	ArrayList<FingerSeq2> fingerseq2List;
	
	ArrayList<ArrayList<MySeq>> arpeggioArray;
	//public ArrayList<ResultABC> fingResultList;
	public ArrayList<ChordFingering> inputChordFingerings;
	int len;
	
	private FingerSeq2 currentArps;
	private int currentArpIndex;
	private ChordFingering currCF;
	//private Position prevJumpPos;
	
	
	
	public State state = State.Normal;
	public boolean positionFollowing;
	
	public enum State {
		Normal, Longest, Jump
	}
	
	private boolean initialized (){
		return fingerseq2List.size() > 0;
	}
	
	
	public void inheritState(ArpeggioSequencer old ){
		if (old == null) {
			return;
		} else{

		this.state = old.state;
		}
	}
	public void setState(State state){
		/*
		this.state = state;
		big.setArpSeqState(state);
		if (fingerseq2List == null || fingerseq2List.size() == 0) {
			System.out.println("setState: empty or null fingerseq2list");
			return;
		}
		*/
		if (state == State.Normal) {
			setStateNormal();
			showNormalArp();
		}
		if (state == State.Longest){
			setStateLongest();
		} 
		if (state == State.Jump) {
			// jumping apoplies to next, prev.
			setStateJump();
		}
		
	}
	
	public boolean restrictedPosition () {
		return  big.extendPanel.positionRestrict.isSelected();
	}
	
	public void setStateNormal() {
			state = State.Normal;
			big.setArpSeqState(state);
			if (initialized()) showNormalArp();
		} 
	public void setStateLongest () {
			state = State.Longest;
			big.setArpSeqState(state);
			if (initialized()) showLongArp();
		} 
	
	public void setStateJump() {
			state = State.Jump;
			big.setArpSeqState(state);
			if (initialized()) showLongArp();
		}
				
	public void showState(){
		System.out.println("Arpegiator state: Current arp index: " + currentArpIndex);
		System.out.println("                  fingerseq2list size: " + fingerseq2List.size());
	}
		
	ArpeggioSequencer(BigScore b, ArrayList<ChordFingering> fingresults, Instrument it) {
		big = b;
		instrument = it;
		inputChordFingerings = fingresults;
		len = inputChordFingerings.size();
		makeFingerSeqList();
		//System.out.println("New arpeggio: state = " + state);
	
		
	}

		
		private void makeFingerSeqList (){
			//System.out.println("MAKE FINGER SEQ");
			//boolean filterBadTopologies = big.filterBadToplogies;
			fingerseq2List = new ArrayList<FingerSeq2>();
			for (int index = 0; index<len; index++){
				FingerSeq2 fs = new FingerSeq2(big.filterBadToplogies, instrument, inputChordFingerings, index);
				fingerseq2List.add(fs);
				// add longest
			}
			
		}
		
		private int countGlued (ArrayList<ChordFingering> cf){
			int c = 0;
			for (ChordFingering f : cf) {
				if (f.isGlued()){
					c++;
				};
				
			}
			return c;
		}
		
		public void rebuild() {
			System.out.println("  ARP REBUILD: " + (countGlued(inputChordFingerings)) + " IS GLUED." );
			makeFingerSeqList();
		}
		
		public void propagateGlue(FingerSeq2 gf) {
			
		}
		
		/*
		private void maybePosRestrict () {
			boolean restrictPos = big.extendPanel.positionRestrict.isSelected();
			
		}
		*/
		public FingerSeq2 getCurrentArps(){
			return currentArps;
		}
		
		
		public FingerSeq2 getArp (int index){
			return fingerseq2List.get(index);
		}
		// when glued, it would appear this is getting called 2x forward & 3x back 
		public void setArp (int index){
			//System.out.println("SET ARP IS CALLED");
			if (currentArps != null) {
				// shrink or extend have to be cancelled
				// unless we're glued
				//System.out.println("resetting old arp ...");
				currentArps.reset();
			}
			//System.out.println("SetArp -- ind = " + index + " fs2List sz = " +fingerseq2List.size());
			if (fingerseq2List.size() == 0) return;
			boolean restrictPos = restrictedPosition();
			//System.out.println("INDEX: " + index + " restrictPos is " + restrictPos);	
			boolean hasInd = big.pseq2.currPositionalSeqhasIndex(index);
			//System.out.println(" --> Pseq has ind? " + hasInd);
			
			FingerSeq2 fs = getArp(index);
			
			if (fs.isGlued()){
				
			
			System.out.println("Set Arp arp is GLUED: " + fs.tostring());
			// maybe restrict this positionally
			}
			if (restrictPos && hasInd)
			{   
				//System.out.println("SetArp: ===== Restricted fingering at ind " + index);
				Position pos = big.pseq2.masterPos();
				//System.out.println("restrict: ==========MASTER POS:" + pos.tostring());
				fs = fs.restricttoPosition(pos);
			}
			
			currentArpIndex = index;
			currentArps = fs;	
			
		}
		
		private void showNormalArp() {
			setArp(big.masterIndex);
			//currentArpIndex = 0;
			
			// for exporting this fs2, we need to know that
			// we're just looking at 1 thing.
			currentArps.setViewIndex0();
			
			currCF = currentArps.nth(0);
			
			
			big.extendPanel.shrink.setEnabled(false);
			//String text = r.noteStringforChordInput();
			//System.out.println("TEXT chord: " + text);
			//big.cfinger.ctl.entry.noteField.setText(text);
			big.writeChordText(currCF);

			big.cfinger.doFingering1(currCF);
			
			//big.scoreUI.setBoxedNotes(currentArps.jScoreElts);
			big.scoreUI.setBoxedNotes(currentArps.index);
			
			//System.out.println("Showing index = " + currentArps.index );
		}
		
		private void showLongArp() {
			setArp(big.masterIndex);
			currCF = currentArps.lastResult();
			// not getting copy
			//System.out.println("Showlongarps: chordfingering: " + currCF.msg);
			big.writeChordText(currCF);
			big.cfinger.doArpeggiation(currCF);
			
			// System.out.println("ARP: ChordFingering -- longest = "+ r.nKs() );
			big.extendPanel.shrink.setEnabled(currentArps.shrinkable());
			big.extendPanel.extend.setEnabled(currentArps.extendable());
		
			big.gluer.setGlueButtons(currentArps);
			/*	
			big.cfinger.ctl.entry.noteField.setText(r.noteStringforChordInput());
			big.cfinger.doFingering1(r);
			*/
		
			
			//int in = currentArps.index;
			//int start = currentArps.start();
			//int out = currentArps.stop();
			//System.out.println("SHOW LONG ARG: (START = " + start + ") INDEX = " + in + " OUT = " + out);
			
			
			// REMOVE REFERENCE TO JSCOREELTS
			//big.scoreUI.setBoxedNotes(currentArps.jScoreElts);
			big.scoreUI.setBoxedNotes(currentArps.start(), currentArps.stop());
			
			//System.out.println("Arps index = " + currentArps.index + ", " + currentArps.longestIndexOut() );
		}
		
		/*
		public void showNormal() {
			setArp(big.currentfing);
			// System.out.println("=== (showCurrFing1 " + restrictPos + "} current=" + currentfing);
			showNormalArp();
		}
		*/
		
		/*
		public void showLong() {
			
			setArp(big.currentfing);
			showLongArp();
			// System.out.println("Current arp index: " + currentArpIndex);
			}
		*/
		
		public void showNextJump () {
			
			if (currentArps  == null) {
				showLongArp();
			} else{
				// find next set of current arps whose index in is > this longest ind out.
				int old_ind_out = currentArps.longestIndexOut();
				
				while (currentArpIndex < fingerseq2List.size() - 1) {
					currentArpIndex++;
					setArp(currentArpIndex);
					
					int new_ind_out = currentArps.longestIndexOut();
					if (new_ind_out > old_ind_out) {
						big.setCurrentFing(currentArpIndex);
						big.doPositionFollowing();
						showLongArp();
						
						return;
					} else {
						
					}
					
					
				}
				
			}
		}
		
		
		public void showPrevJump () {
			//System.out.println("=======PREV JUMP:");
			boolean found = false;
			if (currentArps  == null) {
				//System.out.println("currentArps are NULL");
				showLongArp();
				
			} else{
				// find next set of current arps whose index in is > this longest ind out.
				int old_ind_out = currentArps.longestIndexOut();
				// System.out.println(" old_int_out: " + old_ind_out);
				while (!found && currentArpIndex > 0) {
					currentArpIndex--;
					setArp(currentArpIndex);
					
					int new_ind_out = currentArps.longestIndexOut();
					if (new_ind_out < old_ind_out) {
						//System.out.println(" found new ind_out:: " + new_ind_out);
						// we now have to back up the longest arp with this end.				
						while (!found && currentArpIndex > 0) {
							currentArpIndex--;
							setArp(currentArpIndex);
							int back_ind = currentArps.longestIndexOut();
							if (back_ind != new_ind_out) {
								
								// gone too far ... use earlier.
								currentArpIndex++;
								found = true;;
								
								
							}
						};
						
						// System.out.println(" SETTING: --> " + currentArpIndex);
						
						big.setCurrentFing(currentArpIndex);
						big.doPositionFollowing();
						setArp(currentArpIndex);
						showLongArp();
						
					} else {
						
					}
					
					
				}
				
			}
		}
		
		
		public void shrink () {
			currCF = currentArps.shrink();
			
			// turn off button if no more
			adjustExpandButtons();
			
			//big.cfinger.ctl.entry.noteField.setText(r.noteStringforChordInput());
			big.writeChordText(currCF);
			big.cfinger.doFingering1(currCF);
			// REMOVE REFERENCE TO JSCORE
			//big.scoreUI.setBoxedNotes(currentArps.currentJScoreElts());
			big.scoreUI.setBoxedNotes(currentArps.io());
			//System.out.println("Shrink");	

		}
		
		public void extend() {
			currCF = currentArps.extend();
			//currCF = r;
			// turn off button if no more
			adjustExpandButtons();
			
			//big.cfinger.ctl.entry.noteField.setText(r.noteStringforChordInput());
			big.writeChordText(currCF);
			big.cfinger.doFingering1(currCF);
			// REMOVE REFERENCE TO JSCORE
			//big.scoreUI.setBoxedNotes(currentArps.currentJScoreElts());
			big.scoreUI.setBoxedNotes(currentArps.io());
			//System.out.println("Shrink");	

		}
		
		public void reset(){
			currentArps.reset();
		}
		
		public void adjustExpandButtons (){
			boolean shrink = true;
			boolean extend = true;
			if (!currentArps.shrinkable()) {
				shrink = false;
					};
				if (!currentArps.extendable()) {
					extend = false;
					}
				big.extendPanel.shrink.setEnabled(shrink);
				big.extendPanel.extend.setEnabled(extend);
			}
		
		public void doStart(){
			big.setCurrentFing(0);
			big.doPositionFollowing();
			doCurr();
		}
		public void doEnd(){
			// 
			big.setCurrentFing(len-1);
			big.doPositionFollowing();
			doCurr();
		}
		
		public void doCurr() {
			if (fingerseq2List == null) return;
			if (fingerseq2List.size() == 0) {
				//System.out.println("Arp seq: in docCurr, fingerseq2List is size 0");
				return;
			}
			//System.out.println("State: " + state);
			int ind = big.masterIndex;
			if (ind >= len) return;
			
			if (state == State.Normal) {
				showNormalArp();
			} 
			if (state == State.Longest) {
				showLongArp();
			}
			if (state == State.Jump) {
			
				showLongArp();
			}
		}
		

		private void nextUnHidden(){
			int index = currentArpIndex+1; 
			int sz = fingerseq2List.size() - 1;
			boolean found = false;
			while (!found && index < sz) {
				FingerSeq2 fs = fingerseq2List.get(index);
				if (fs.isHidden()) {
					index++;
				} else {
					found = true;
				}
			};
			currentArpIndex = index;
			big.setCurrentFing(index);
		}
		
		private void prevUnHidden(){
			int index = currentArpIndex-1; 
		
			boolean found = false;
			while (!found && index > 0) {
				FingerSeq2 fs = fingerseq2List.get(index);
				if (fs.isHidden()) {
					index--;
				} else {
					found = true;
				}
			};
			currentArpIndex = index;
			big.setCurrentFing(index);
		}
		
		
		public void doNext() {

			if (state == State.Normal  || state == State.Longest) {
				//big.incrCurrentFing();
				nextUnHidden();
				big.doPositionFollowing();
				doCurr();//TODO
			} 
			if (state == State.Jump) {
				showNextJump();
			}
		}
		public void doPrev() {
			
			if (state == State.Normal  || state == State.Longest) {
				//big.decrCurrentFing();
				prevUnHidden();
				big.doPositionFollowing();
				doCurr();
			} 
			if (state == State.Jump) {
				showPrevJump();
			}
		}
		
		public void showGlue() {
			int c = 0;
			for (ChordFingering cf : inputChordFingerings) {
				if (cf.isGlued()) {
					c++;
					int ind = cf.index;
					System.out.println("glue: " + ind);
				}	
			}
			if (c==0){
				System.out.println("No glue");
			}
		}
		public ChordFingering currentCF(){
			return currCF;
		}
}

package computingmusic;

import java.util.ArrayList;

import computingmusic.ChordFingering.Fingering;
import computingmusic.topology.Bars;


public class AnchorSequencer {
	State state;
	BigScore big;
	public Instrument instrument;
	ArrayList<ChordFingering> inputChordFingerings;
	ArrayList<FingerSeq2> fs2List;
	int inputSize;
	
	//FingerTrail currFT;
	Anchor currAnchor;
	
	
	Bars bars; // bars topology
	//JButton extendButton = big.extendPanel.extend;
	//JButton shrinkButton = big.extendPanel.shrink;
	public AnchorSequencer(BigScore b, ArrayList<ChordFingering> fingresults, Instrument it) {
		big = b;
		
		instrument = it;
		inputChordFingerings = fingresults;
		inputSize = inputChordFingerings.size();
		
		currAnchor = null;
		bars = new Bars();
	}
	
	public enum State {
		Off, On
	}
	
	public boolean isOn(){
		return state == State.On;
	}
	
	
	public void setState(State s){
		state = s;
	}
	public void setStateOn(){
		big.setAnchorSeqState(State.On);
		state = State.On;
	}
	public void setStateOff(){
		big.setAnchorSeqState(State.Off);
		state = State.Off;
	}
	public void inheritState(AnchorSequencer old ){
		if (old == null) {
			return;
		} else{

		this.state = old.state;
		}
	}
	
	public void rebuild(){
		fs2List = big.arpSeq.fingerseq2List;
		if (fs2List == null) return;
		/*
		for (FingerSeq2 f2 : fs2List) {
			new Anchor(f2);
		}
		*/
	}
	
	// Anchor is an arp in Fingerseq2 extended with trails.
	public class Anchor  {
		FingerSeq2 fs2;
		ChordFingering cf;
		ArrayList<FingerTrail> activeTrails; // ongoing
		ArrayList<FingerTrail> finalTrails; // breaks
		FingerTrail trailOut;
		public int start;
		public int stop; 
		// testing idea.
		Anchor(FingerSeq2 f){
			fs2 = f;
			activeTrails = new ArrayList<FingerTrail>();
			
			//cf = fs2.lastResult(); // get longest arp here
			cf = fs2.currentResult();
			start = fs2.start();
			stop = fs2.stop();
			//stop = cf.index;
			//System.out.println("Anchor: in= " + start + " out: " + stop);
			
			/*
			// we'll only do what's visible in the initial fingering.
			Fingering fCurr = cf.getCurrFingering();
			trailOut = new FingerTrail (instrument, start, stop);
			
			// addFingering returns copy, since we need to
			// test & possibly reject it.
			
			FingerTrail t = trailOut.addFingering(fCurr);
			activeTrails.add(t);
			*/
			
			// 28 aug 2010 -- reverted to making initial trails of ALL fingers.
			
			for (Fingering fing : cf.getFingerings()) {
				
				trailOut = new FingerTrail (instrument, start, stop);
				
				// addFingering returns copy, since we need to
				// test & possibly reject it.
				
				FingerTrail t = trailOut.makeCopyWithNewFingering(fing);
				if (t != null){
					activeTrails.add(t);
				}else {
					//System.out.println("Rejected: " + t );
				}
				
				
				
			}
			
		}
		
		public void finalizeTrails (){
			for (FingerTrail ft : activeTrails){
				ft.finalize();
			}
		}
		
		public FingerTrail activeHead(){
			if (activeTrails.size() == 0) {
				return null;
			} else {
				return activeTrails.get(0);
			}
		}
			
		/*
		// return ft with least hand span.
		public FingerTrail activeTrailLeastSpan() {
			if (activeTrails.size() == 0) {
				return null;
			} else {
				FingerTrail best = activeTrails.get(0);
				int bestSpan = best.getHandSpan();
				if (activeTrails.size() == 1) {
					return best;
				} else {
					for (int i = 1; i < activeTrails.size(); i++) {
						FingerTrail cand = activeTrails.get(i);
						int h = cand.getHandSpan();
						if (h < bestSpan) {
							bestSpan = h;
							best = cand;
						}
					}
					return best;
				}
			}
		}
		*/
		@Override public String toString (){
			String s = "";
			for (FingerTrail ft : activeTrails) {
				s = s + "\n" + ft.heightMap;
			}
			return s;
		}
		
		public int countActiveTrails (){
			return activeTrails.size();
		}
		
		// return success of extension.
		
		public boolean extendForward(){
			//System.out.println("stop = " + stop);
			//System.out.println("inputSize = " + inputSize);
			if (stop+1>= inputSize) return false; // if we are at the end of the piece
			// next item in score.
			ChordFingering cfnext = inputChordFingerings.get(stop + 1);
			//System.out.println("size of inputChordF: " + inputChordFingerings.size());

			ArrayList<FingerTrail> ft = extendbyCF(cfnext);
			if (ft.size() > 500) {
				System.err.println("Warning! we seem to be exploding!");
				return false;
			}
			if (ft.size() > 0) {
				// check
				for (FingerTrail f : ft){
					if (!f.inSpan){
						System.out.println("YEE----OW! extendForward is accepting bad fts!");
					}
				}
				activeTrails = ft;
				stop++;
				return true;
			} else {
				finalizeTrails();
				return false;
			}
		}
		
		/**
		 * Author: Eliot
		 * This method extends the Anchor by the fingering that's at its left. It does not shrink the anchor. 
		 * @return the extension was successful.
		 */
		public boolean extendBack(){
			if (start == 0) return false;
			// next item in score.
			ChordFingering cfnext = inputChordFingerings.get(start - 1);
			ArrayList<FingerTrail> ft = extendbyCF(cfnext);
			if (ft.size() > 0) {
				activeTrails = ft;
				start--;
				return true;
			} else {
				return false;
			}
		}
		
		
		public ArrayList<FingerTrail> extendbyCF(ChordFingering cfnext) {
			// get next cf
			ArrayList<FingerTrail> stillProgressing = new ArrayList<FingerTrail>();

			// try extebnding each trail with all input fingerings
			for (Fingering f : cfnext.getFingerings()) {
				for (FingerTrail t : activeTrails) {
					FingerTrail x = t.makeCopyWithNewFingering(f);
					if (x == null) {
						// System.out.println("extend by CF: NULL");
					} else{
						
						// NOT USING 
						TopologyAnchor tt = new TopologyAnchor(instrument,bars, x);
						x.topology = tt;
						
						// topology tests here
						if (x.stoppedHeights() > 4) { // more than 4 = not acceptable FingerTrail
							// do nothing
						} else{
							stillProgressing.add(x);
						}
						/*	
						if (!tt.isBad() && !x.bad()) {	
							stillProgressing.add(x);
						} else {
							// check if we ever extend this.
						}
						*/
					}		
						
				}
			}
			
			/// We enforce bowing here. If we have a trail where there are
			/// no bowing skips, then remove those that do skip.
		
	
			//ArrayList<FingerTrail> smooth = new ArrayList<FingerTrail>();
			ArrayList<FingerTrail> noBari = new ArrayList<FingerTrail>();
			
			for (FingerTrail ft : stillProgressing) {
				if (ft.countBariolations() == 0) { 
					noBari.add(ft);
				}
			}
			if (noBari.size() == 0) {
				// ignore
			} else {
				stillProgressing = noBari;
			}
			
			/*
			for (FingerTrail ft : stillProgressing) {
					if (ft.bowing.skips == 0) { 
						smooth.add(ft);
				}
			}
			
			if (smooth.size() == 0) {
				// only remembering longest
				return stillProgressing;

			} else {
				return smooth;
				
			}
			*/
			return stillProgressing;
			
		}
	} // end of Class Anchor


	
	private void setSEButtons (boolean shrink, boolean expand) {
		big.extendPanel.shrink.setEnabled(shrink);
		big.extendPanel.extend.setEnabled(expand);
	}
	
	public void autoUpdateAnchor () {
		currAnchor = new Anchor(big.arpSeq.getCurrentArps());
	}
	
	private void updateAnchor(){
		currAnchor = new Anchor(big.arpSeq.getCurrentArps());
		System.out.println("updateAnchor -- activeTrails = " + currAnchor.countActiveTrails());
		big.cfinger.setActiveTrails(currAnchor.activeTrails);
	}
	
	private void updateNextAnchor()
	{
		currAnchor = new Anchor(big.arpSeq.getCurrentArps());
		System.out.println("updateAnchor -- activeTrails = " + currAnchor.countActiveTrails());
		big.cfinger.setNextActiveTrails(currAnchor.activeTrails);
	}
	
	public void doCurr(){ // TODO verify if still necessary
		//resetFingerboard();
		System.out.println("HELLO -- Anchor CURR running Arp.");
		big.arpSeq.doCurr();
		updateAnchor();
		setSEButtons(false, true); // TODO verify if valid
	}
	public void doPrev(){
		//resetFingerboard();
		System.out.println("HELLO -- Anchor PREV running Arp.");
		big.arpSeq.doPrev();
		updateAnchor();
		setSEButtons(true, true);
	}
	public void doNext(){
		//resetFingerboard();
		System.out.println("HELLO -- Anchor NEXT running Arp.");
		big.arpSeq.doNext();
		updateAnchor();
		setSEButtons(true, true);
	}
	
	public void doJumpNext()
	{
		for(int i = 0; i< big.appFrame.cfinger.fingPane.getCurrentLength()  ; i++)
		{
			big.arpSeq.doNext();
		}
		updateNextAnchor();
	}
	
	
	public void doStart() {
		resetFingerboard();
		System.out.println("HELLO -- Anchor START running Arp.");
		big.arpSeq.doStart();
		updateAnchor();
		setSEButtons(false, true);

	}
	public void resetFingerboard(){
		//big.fingerBoard.grayNotes = null;
		big.cfinger.fingPane.getCurrentBoard().clear();
		currAnchor = null;
	}
	
	public void extend(){
		// TODO change so that the returned Fingering is not the default one, but the one corresponding
  	    // to the previous one      I understand myself.
		if (currAnchor == null){
			System.out.println("uh oh -- current anchor was null, setting");
			currAnchor = new Anchor(big.arpSeq.getCurrentArps());
		}
		
		boolean extended = currAnchor.extendForward();
		System.out.println("BOOLEAN EXTENDED:" + extended);
		if (extended) {
			setSEButtons(true, true);
			//System.out.println(currAnchor);
			//System.out.println("Active trails: " + currAnchor.countActiveTrails());
			
			//FingerTrail best = currAnchor.activeTrailLeastSpan();
			
			// just prints some info
			//TopologyTrail tt = new TopologyTrail(best);
			
			
			//TopologyTrail tt2 = new TopologyTrail(bars, best);
			//PointList gray = a.activeTrails.get(0).grayPoints;
			
			/*
			// display the trail with the least handspan
			PointList gray = best.grayPoints;
			PointList red = best.redPoints;
			//PointList gray = currAnchor.activeTrails.get(0).grayPoints;
			if (gray == null ){
				System.out.println("Oops! null Gray!!");
			};
			System.out.println("best trail start/stop: " + best.start() + " "+  best.stop() 
					+ " span: " + best.getHandSpan() + " fingers: " + best.heightSetSize() );
			// base fingering could change as we move on
			*/
			((Cfinger2) big.cfinger).giveTheTrailIndex();
			big.cfinger.setNextExtendedTrails(currAnchor.activeTrails);
			// check
			for (FingerTrail f : currAnchor.activeTrails){
				if (!f.inSpan){
					System.out.println("YEE----OW! extend is accepting bad fts!");
				}
			}
			//big.fingerBoard.setFingers(red.toarray());
			//big.fingerBoard.setGray(gray.toarray());
			FingerTrail f0 = currAnchor.activeHead();
			big.scoreUI.setBoxedNotes(f0.start(), f0.stop());
		} else {
			
			setSEButtons(big.extendPanel.shrink.isEnabled(), false);
			System.out.println("FINALIZED");
			System.out.println(currAnchor.activeTrails.get(0));
			System.out.println("No forward extensions");
		}
		
	}
	
	
	public ArrayList<FingerTrail> autoExtend (){
		boolean extended = true;
		while (extended) {
			 extended = currAnchor.extendForward();
		}
		return currAnchor.activeTrails;
		
	}
	// this is a dummy.
	
	public void shrink(){
		// TODO change so that the returned Fingering is not the default one, but the one corresponding
		
		
		
		// jb start
		if (currAnchor == null){
			System.out.println("uh oh -- current anchor was null, setting");
			currAnchor = new Anchor(big.arpSeq.getCurrentArps());
		}
		
		int sizeOfCurrentExtension = currAnchor.stop - currAnchor.start;
		//System.out.println("size of ext is: " + sizeOfCurrentExtension);
		
		doCurr(); // has its place in the board to inactivate the current board
		for(int i = 0; i< sizeOfCurrentExtension-1 ; i++)
		{
			extend();
		}
		big.cfinger.fingPane.applyShrink();
		big.cfinger.indexViewActiveTrail = big.cfinger.fingPane.getCurrentBoard().getTrailIndex();
		big.cfinger.viewTrail();
		
		//TODO put message if shrink does nothing. 
		
		// jb end
			/*
		Anchor a = new Anchor(big.arpSeq.getCurrentArps());
		boolean extended = a.extendBack();
		if (extended) {
			System.out.println("BackActive trails: " + a.countActiveTrails());
			System.out.println(a);
			FingerTrail best = a.activeTrailLeastSpan();
			//PointList gray = a.activeTrails.get(0).grayPoints;
			PointList gray = best.grayPoints;
			//big.fingerBoard.setGray(gray.toarray());
		} else {
			System.out.println("No backward extensions");
		}
		*/
	}
	
}


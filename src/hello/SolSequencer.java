package hello;

import java.awt.Color;
import java.util.ArrayList;

import hello.AnchorSequencer.Anchor;
import hello.BigScore.XMode;
import hello.ChordFingering.Fingering;
import hello.gui.AnnotatedBoxes;
import hello.topology.Bars;

public class SolSequencer {
	private State state;
	@SuppressWarnings("unused")
	private Instrument instrument;
	private BigScore big;
	ArrayList<ChordFingering>inputChordFingerings;
	ArrayList<FingerSeq2> fs2List;
	/**
	 * Chords/single notes that can be fingered in just one way.
	 */
	ArrayList<Fingering> fixedChords;
	
	int inputSize;
	
	//FingerTrail currFT;
	Anchor currAnchor;
	
	
	Bars bars; // bars topology
	//JButton extendButton = big.extendPanel.extend;
	//JButton shrinkButton = big.extendPanel.shrink;
	public SolSequencer(BigScore b, ArrayList<ChordFingering> fingresults, Instrument it) {
		big = b;
		
		instrument = it;
		inputChordFingerings = fingresults;
		inputSize = inputChordFingerings.size();
		
		fixedChords = new ArrayList<Fingering>();
		
		
		currAnchor = null;
		bars = new Bars();
		
		
		
	}
	
	public enum State {
		Off, On
	}
	
	
	public void inheritState(SolSequencer s) {
		if (s == null ) {
			return;
		} else {
			state = s.state;
		}
	}
	
	private void findFixedChords(){
		fixedChords.clear();
		for (ChordFingering cf : inputChordFingerings) {
			ArrayList<Fingering> ff = cf.getFingerings();
			if (ff.size() == 1) {
				fixedChords.add(ff.get(0));
			}
		}
	}
	
	
	public void genAnchors(int start) {
		Color colorBlue = new Color(0x26BEFF);
		AnnotatedBoxes boxes = new AnnotatedBoxes(big, colorBlue, 3); // big, color,
																// margin
		
		big.setCurrentFing(start);
		big.anchSeq.autoUpdateAnchor();
		big.setXmode(XMode.Anch);
		ArrayList<FingerTrail> fts = big.anchSeq.autoExtend();
		/*
		 * System.out.println("extended anchors: " + fts.size()); for
		 * (FingerTrail ft : fts) { System.out.println(ft.describe()); }
		 */
		for (FingerTrail ft : fts) {
			boxes.addPoint(ft.io());

		}
		
		big.scoreUI.addAnnotatedBoxes(boxes);
		big.cfinger.setActiveTrails(fts); //TODO are you sure its the right cfinger?
	}
	
	
	
	
	public void solve () {
		findFixedChords();
		System.err.println("Fixed chords: " + fixedChords.size());
		//for (Fingering f : fixedChords) {
		//	boxes.addInd(f.index);
		//}
		System.out.println("XMode: " + big.xmode);
		genAnchors(big.getCurrentFing());
		
	}
}

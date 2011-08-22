package computingmusic;


import java.awt.Point;
import java.util.ArrayList;

import computingmusic.ChordFingering.Fingering;

public class Glue {
	BigScore big;
	GluePanel controls;
	private ChordFingering CurrentCF1;
	
	Glue(BigScore big){
		this.big = big;
		controls = big.cfinger.ctl.gluePanel;
	}
	
	public void unGlue(ChordFingering cf1){
		cf1.unGlue();
		big.rebuild();
		setGlueButtons(cf1);
		big.cfinger.ctl.entryPanel.setArrows(CurrentCF1);
		big.doPositionFollowing();
	};
		
	public void setGlue(ChordFingering cf1) {
		CurrentCF1 = cf1;
		if (cf1.isArp()) {
			// System.out.println("glue: is arp");
			setArpGlue(cf1);

			// CurrentCF1.setArpGlue();
		} else {
			System.out.println("glue: is NOT arp.");
			cf1.setGlue();

			// rebuild entire pos sequence, though we probably want to
			// optimize LATER.
			big.rebuild();
			setGlueButtons(cf1);
			big.cfinger.ctl.entryPanel.setArrows(cf1);
			big.doPositionFollowing();
		}
	}
	
	/*
	 * How to set arp glue
	 * 
	 * 
	 * 1. hide everything before + after this view index
	 * 2. For near arps in arpseq's fs2 array, hide everything
	 *    with incompatible fingering.
	 * 
	 * 3. we have to remember here what we did for unGlue.
	 *  
	 */

	
	public void setArpGlue(ChordFingering cf1) {
		// this is a construction -- get origs.
		FingerSeq2 f2 = big.arpSeq.getCurrentArps();
		f2.setGlue(); 
		System.out.println("This CF1 has fingeringds: " + cf1.nFingerings());
		Fingering currFing = cf1.getCurrFingering();
		Point io = f2.io();
		Point[] gluepoints = currFing.getCoords();		
		System.out.println("how many gluepoints you ask? " + gluepoints.length);
		ArrayList<ChordFingering> input = big.arpSeq.inputChordFingerings;
		for (int k = io.x; k <= io.y; k++) {
			ChordFingering f = input.get(k);
			f.reducetoGlue2(gluepoints);
		}
			
		
		//big.arpSeq.propagateGlue (f2);
		System.out.println("Arp glued in/out : " + io );
		big.rebuild();
		big.cfinger.ctl.entryPanel.setArrows(cf1);
		big.doPositionFollowing();
	}


	public void setGlueButtons(ChordFingering cf1) {

		// buttons ordered glue, unglue
		if (cf1 == null) {
			controls.setButtons(false, false);
		} else if (cf1.isGlued()) {
			controls.setButtons(false, true);
		} else if (cf1.nFingerings() > 1) {
			controls.setButtons(true, false);
		} else if (cf1.isArp()) {
			controls.setButtons(true, false);
		} else {
			controls.setButtons(false, false);
		}
	}
	public void setGlueButtons(FingerSeq2 cf1) {
		//System.out.println("BUTTONS: Gluing fingerseq ...");
		// buttons ordered glue, unglue
		if (cf1 == null) {
			controls.setButtons(false, false);
		} else if (cf1.isGlued()) {
			//System.out.println("Yes, I believe glue is ON.");
			controls.setButtons(false, true);
		
		} else {
			//System.out.println("glue is OFF.");
			controls.setButtons(true, false);
		}
	}
}
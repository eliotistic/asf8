package computingmusic;

import computingmusic.FingerTrail.TPArray;
import computingmusic.topology.Bars;
import computingmusic.utils.*;


public class TopologyAnchor {
	Instrument instrument;
	int nHeights;
	int nStops; // no 0
	int nAxes = 0;
	int nBars = 0;
	int nOnes = 0;
	int nGaps = 0;
	private boolean bad = false;
	FingerTrail trail;
	int nSaiten; // n strings in this instrument -- forget it we demand 4 for this
	int nFingers;
	PointSet stoppedPoints;
	
	PointGroup heightSets; // ( ( <s0, H1>, <s1. H1> ) (<s0, H2> <s0, H2> ...) )
	TPArray timePointArr;
	// PointList axes;
	
	Topos tops;
	Bars bars;
	
	public enum Topos {
		BAD,
		OPEN, // every string open
		
	}
	
	public TopologyAnchor() {
	}
	
	
	public TopologyAnchor(Instrument ins, Bars bars, FingerTrail ft ){
		instrument = ins;
		trail = ft;
		this.bars = bars;
		nHeights = ft.heightMap.size();
		timePointArr = ft.getTrailPoints();
		
		doBarTopology();
		if (!bad && nHeights == 4) {
			doFourFingerTopology();
		}
		
		
		/*
		// just say this
		
		for (Trailpoint tp : timePointArr){
		
			System.out.println("order:" 
					+ tp.order + " "+ tp.saite + " " + tp.interval
					+ " fingerDown:" + tp.fingerDown);
				
		}
		
		// we think we can hold down all fingers at once.
		// but we may have to lift some fingers. 
		
		PointList lastPoints = ft.lastPoints();
		System.out.println("Lastpoints: " + lastPoints.size() );
		*/
	}
	
	private void doBarTopology() {
		//nHeights = heightMap.size();
		if (trail.heightMap.hasKey(0)) { // ignore open string
			nHeights--;
		}
		if (nHeights > 4) {
			bad = true;
		} else {
			
			
			// evaluate all bars. 
			for (BvKey bk : trail.heightMap) {
				if (bk.key == 0) {
					// ignore open strings
				} else {
					Bars.barType t = bars.analyzeType(bk.bv);
					if (bars.isOne(t)) {
						nOnes++;
					} else if (bars.isAxis(t)) {
						nAxes++;
					} else if (bars.isBar(t)) {
						nBars++;
					} else {
						bad = true; // +..+ should be ok
					}
				}
			} 
			
			// if we're still alive, check case where 4 fingers are placed.  
			/*
			if (!bad && (nHeights == 4)) {
				
				System.out.println("-->Theoretical 4 finger anchor check");
				
			}
			*/
			
			
		}

	}
	
	private void doFourFingerTopology () {
		@SuppressWarnings("unused")
		IntList ii = fingerHeightYPositions();
		//System.out.println("Four finger diffs:" + ii);
	}
	
	
	private IntList fingerHeightYPositions () {
		IntList keys =  trail.heightMap.getKeys();
		IntList yPos = instrument.intervalYPositions(keys, 0, 100);
		yPos.sort();
		IntList diffs = yPos.diffs();
		
		return diffs;
	}
	public boolean isBad(){
		return bad
		|| nBars > 1
		|| (nBars == 1 && nHeights > 2);
	}
	
	public TopologyAnchor(FingerTrail ft) {
		trail = ft;
		nSaiten = ft.saitenArray.length;
		//trail = new FingerTrail();
		//trail.addFingering(f);
		//stoppedPoints = ft.getStoppedPoints().copy();
		//IntSet[] saitenArray = ft.saitenArray;
		
		System.out.println("Topology:\n" + ft.heightMap);
		
	}

	
}

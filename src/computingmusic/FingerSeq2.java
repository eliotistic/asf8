package computingmusic;




import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import computingmusic.utils.IntList;

//import abc.notation.MusicElement;

public class FingerSeq2 implements Cloneable {
	public Instrument instrument;
	
	private boolean hidden; 
	@SuppressWarnings("unused")
	private boolean bogusGlue;
	int seqSize; 
	int inputListStart;
	int inputListStop;
	int index;
	// int stop;
	
	private int viewIndex; // index in fingerSeqResult of what we are presently viewing
	
	public boolean atEnd;
	public boolean canPlay;
	
	//public ArrayList<Keynum.Result> keynumMergeResults; // 0 is the first keynum set
	// at current ind, so guaranteed to be there
	// following the results of consecutive merges.
	
	private ChordFingering chordFingering0; // the first, always same item as input.
	
	private ArrayList<ChordFingering> progressiveArpChords; // all fingerings into which we can
	private ArrayList<ChordFingering> uchordFingerings;
	// collapse N consecutive items from fingerseqstart to stop.
	
	public ArrayList<ChordFingering> inputList; // just notes/chords in musical input.
	
	
	private int glueindex; // -1 by default, otherwise chordFingerings.get(glueindex)
	
	private ArrayList<Point> gluePoints; // for recalculating 
	private boolean filterBadTopologies;
	
	
	
	
	// REMOVE
	//public ArrayList<MusicElement> jScoreElts; // abcj elts for all notes in this seq.
	
	// for each index in score, we have a list of Seqs, showing start & stop of extensions. 
	//public ArrayList<ArrayList<Seq>> extendedSeqs;
	
	/*
	 * we get in here with fingResultList, a list of ResultABC's which
	 * have Fing.Results + musicelements for printing.
	 * 
	 * we build the longest possible sequence of fing.results that
	 * can be fingered as one chord (returned in fingerSeqResult).
	 * 
	 */
	/*
	FingerSeq2 (ArrayList<BigScore.ResultABC> frl, int i) { // i = currentFing
		
	}
	*/

	FingerSeq2 () {
		
	}

	FingerSeq2 (boolean filterBadTopologies, Instrument it, ArrayList<ChordFingering> frl, int i) { // i = currentFing
		hidden = false;
		glueindex = -1;
		
		this.filterBadTopologies = filterBadTopologies;
		// max = (at most) # expand
		// build preliminary seq -- whatever is at current position
		instrument = it;
		inputList = frl; // possibly NON CONSECUTIVE inds in ChordFingerings
		seqSize = inputList.size();
		inputListStart = i;
		progressiveArpChords = new ArrayList<ChordFingering>();
		gluePoints = new ArrayList<Point>();
		
		// inputList is just notes + chords as they appear in the score.
		
		chordFingering0 = inputList.get(i); // required, even if bad topology
		
		chordFingering0.addGluePoints(gluePoints);
		
		
		index = chordFingering0.index; //
		//fingerSeqResults.add(r0.fingResult);
		
		// initialize with first, getting rid of this!
		progressiveArpChords.add(chordFingering0);
		
	
		expandResult (i+1, chordFingering0);
		
		// array we work with
		buildUnhidden();
		// last + longest expansion
		viewIndex = maxIndex();
		inputListStop = viewIndex;
		/// stop = currentResult + index;
	};
	
	public FingerSeq2 copy () {
		
		FingerSeq2 f = new FingerSeq2();
		f.hidden = hidden;
		f.glueindex = glueindex;
		f.filterBadTopologies = filterBadTopologies;
		f.chordFingering0 = chordFingering0;
		f.uchordFingerings = uchordFingerings;
		f.index = index;
		f.seqSize = seqSize;
		f.instrument = instrument;
		f.inputList = inputList;
		f.inputListStart = inputListStart;
		f.inputListStop = inputListStop;
		f.progressiveArpChords = progressiveArpChords;
		
		f.viewIndex = viewIndex;
		return f;
	}
	
	/*
	public int seqSize () {
		return fingerSeqResults.size();
	}
	*/
	
	// 
	
	public void setViewIndex0 () {
		viewIndex = 0;
	}
	private int maxIndex (){
		if (isGlued()) {
			return 0;
		} else{
			return uchordFingerings.size() - 1;
		}
	}
	public int stop () {
		// 
		if (isGlued()) {
			return getGluedFingering().index;
		} else
		{
		return uchordFingerings.get(viewIndex).index;
		}
	}
	
	
	public int start () {
		// 
		return chordFingering0.index;
	}
	
	
	
	public Point io () {
		return new Point (start(), stop());
	}
	
	public void getArpChords () {
		
	}
	
	public void buildUnhidden(){
		uchordFingerings = new ArrayList<ChordFingering>();
		for (ChordFingering cf : progressiveArpChords ){
			if (cf.isHidden()){
				
			} else {
				uchordFingerings.add(cf);
			}
		}
	}
	
	public ArrayList<ChordFingering>getUnhidden(){
		return uchordFingerings;
	}
	
	
	public ChordFingering firstUnhidden(){
		return uchordFingerings.get(0);
	}
	
	public ChordFingering lastUnhidden(){
		int sz = uchordFingerings.size();
		System.out.println("Last unhidden -- sz = " + sz);
		return uchordFingerings.get(sz-1);
	}
	
	public ArrayList<Point> getGluePoints(){
		return gluePoints;
	}
	public ChordFingering getGluedFingering(){
		if (isGlued()) {
			return progressiveArpChords.get(glueindex);
		} else
		{
			return null;
		
		}
	}
	//headFingering is still in possequencer.
	
	public ChordFingering headFingering(){
		//ChordFingering f = getGluedFingering();
		
		return progressiveArpChords.get(0);
	}
	
	
	// hide this fingerseq2. 
	
    public boolean isHidden (){
    	return hidden;
    }
    
    public void hide(){
    	hidden = true;
    	
    }
    
    public void unhide(){
    	hidden = false;
    	
    }
	
	public int getViewIndex () {
		return viewIndex;
	}
 	public void setGlue(){
		bogusGlue = true;
 		glueindex = viewIndex;
	}
	
	public void unGlue(){
		bogusGlue = false;
		glueindex = -1;
		
	}
	public boolean isGlued(){
		return glueindex != -1;
	}
	
	
	/*
	private int[] inputListMonoPitches (int in, int out ){
		// must deal with chords eventually. 
		// if we have chords, we just say 0 for now
		int[] p = new int[1+out-in];
		int ind = -1;
		for (int i = in; i<=out; i++){
			ind++;
			ChordFingering f = inputList.get(i);
			Keynum.K[] ks = f.ks;
			if (ks.length > 1) {
				p[ind] = 0;
			} else {
				p[ind] = ks[0].pitch;
			}
		}
		return p;
	}
	
	*/
	
	private IntList[] inputListPolyPitches (int in, int out ){
		// must deal with chords eventually. 
		// if we have chords, we just say 0 for now
		int len = 1+out-in;
		IntList[] p = new IntList[len];  
		
		//int[] p = new int[1+out-in];
		int ind = 0;
		for (int i = in; i<=out; i++){
			ChordFingering f = inputList.get(i);
			p[ind] = Keynum.toPitchList(f.ks);
			ind++;
		}
		return p;
	}
	
	
	
	
	public String tostring() {
		Point p = io();
		
		String s = "FingerSeq2: io = "   + p;
			
		
		return s;
	}
	
	public FingerSeq2 restricttoPosition (Position pos) {
		FingerSeq2 f = copy();
		f.progressiveArpChords = new ArrayList<ChordFingering>();
		
		// we restrict fingerings unil one is impossible
		//String xx = "    RESTRICTING ===> ";
		//System.out.println(xx + "curr fs2 length:" + fingerSeqResults.size());
		
		for (int i = 0; i< progressiveArpChords.size(); i++) {
			ChordFingering cf = progressiveArpChords.get(i);
			ChordFingering restrictedFingering = cf.restricttoPosition(pos);
			if (restrictedFingering.nFingerings() != 0) {
				f.progressiveArpChords.add(restrictedFingering);
				
			} else {
				
				//System.out.println("Can't RESTRICT: " + restrictedResult);
				// finished
				// or are we? each result is a chord
				// sometimes we can play 3 but not 2.
				 //break;
			}
		};
		f.buildUnhidden();
		// what happens if we get nothing? 
		
		
		//f.seqSize = f.fingerSeqResults.size();
		//System.out.println(xx + " AFTER RESTRICTING: " + f.fingerSeqResults.size());
		// this is index of currently viewing seq -- set to longest
		//f.viewIndex = f.progressiveArpChords.size() - 1;
		f.viewIndex = f.maxIndex();
		return f;
	}
	
	public int longestIndexOut () {
		return  index + progressiveArpChords.size() - 1;
	}
	public int currentIndexOut () {
		System.out.println("Current index out IS NOT SET");
		return -1;
	}
	
	
	public ArrayList<ChordFingering.Fingering> headFingeringsofInd (int ind){
		if (ind > progressiveArpChords.size()) { 
			return null;
			} else {
				return progressiveArpChords.get(ind).getFingerings();
			}
			
	};
	/*
	public MusicElement scoreofInd (int ind){
		if (ind > fingerSeqResults.size()) { 
			return null;
			} else {
				return jScoreElts.get(ind);
			}
			
	};
	*/
	
	@SuppressWarnings("unused")
	private int nExpansions () {
		return progressiveArpChords.size();
	}
	
	public boolean shrinkable () {
		return !isGlued() && viewIndex > 0;
	}
	public boolean extendable () {
		return !isGlued() && viewIndex < maxIndex();
	}
		
	
	// if glued, return that
	// else longest unHidden.
	
	
	public ChordFingering lastResult(){
		//System.out.println("lastResult: fingerSeqResults.size() is " + fingerSeqResults.size());
		
		if (isGlued()) {
			//System.out.println("Last result is GLUED");
			return  progressiveArpChords.get(glueindex);
		} else {
			return lastUnhidden();
		}
		
	}
	
	
	
	
	
	public ChordFingering nth(int n){
		return progressiveArpChords.get(n);
	}
	public ChordFingering currentResult(){
		return progressiveArpChords.get(viewIndex);
	}
	
	public ChordFingering shrink () {
		if (shrinkable()) {
			viewIndex--;
		};
		return currentResult();
	}
	public ChordFingering extend () {
		if (extendable()) {
			viewIndex++;
		};
		return currentResult();
	}
	public void reset() {
		if (isGlued()) {
			
		} else {
			viewIndex = maxIndex();		}
		
	}
	
	/*
	public ArrayList<MusicElement> currentJScoreElts (){
		int sz = jScoreElts.size();
		if (currentResult + 1 == sz) {
			return jScoreElts;
		} else {
			ArrayList<MusicElement> ans = new ArrayList<MusicElement>();
			for (int i = 0; i<= currentResult; i++){
				ans.add(jScoreElts.get(i));
			};
			return ans;
		}
	}
	*/
		
		
	public void expandResult (int currIndex, ChordFingering prevResult){ // curr guranteed > 0, true index
		if (currIndex >= seqSize) {
			return;
		} else {}
			//ResultABC r1 = inputList.get(curr);
			ChordFingering f1 = inputList.get(currIndex);
			f1.addGluePoints(gluePoints);
			//ChordFingering prevResult = lastResult();
			//System.out.println("expanding lastresult: " + prevResult.noteString());
			//System.out.println("          currresult: " + f1.noteString());
			//Keynum.Result k0 = keynumMergeResults.get(keynumMergeResults.size()-1);
			// Keynum.Result k0 = prevResult.keynumResult;
			// first, see if we can merge keynums from last + this.
			int[] aNums = prevResult.kNums();
			//int[] bNums = r1.kNums();
			int[] bNums = f1.kNums();
			ArrayList<Point> bag = Points.combineMultisets(aNums, bNums);

			// can have max 4 items, corresponding to 4 strings.
			int sum = Points.multisetSum(bag);
			if (sum > 4) {
				// too many, bow out here
				// System.out.println("expand: too many notes");
				return;
			} else {
				int[] flatNums = Points.flattenMultiset(bag);
				
				// need to generate ks so that we get the correct spelling.
				// we first use names from k1, then k2 for any we missed.
				Keynum.K[] k1 = prevResult.ks;
				//Keynum.K[] k2 = r1.getKs();
				Keynum.K[] k2 = f1.ks;
				Keynum.K[] ks = mergeKs(flatNums, k1, k2); 
				//Keynum.Result r = new Keynum.Result(ks);
				//keynumMergeResult = r;
				
				/*
				 *  we now generate all playable fingerings for this.
				 */
				// 
				// we use currIndex as index for this, which is where this
				// expansion ENDS. 
				// 
				ChordFingering compositeFingering = new ChordFingering(f1.index, instrument, ks);
				
				
				
				
				// we have to remember our start index
				compositeFingering.setArp();
				// 
				// remove fingerings of this composite
				// chord that would make partial chords unplayable
				// because of non-adjacent strings.
				
				// must test not only this chord, but all previous chords
				// for this position.
				for (int k = inputListStart; k<=currIndex; k++) {
					int pitches[] = inputList.get(k).kNums();
					consecutiveStringsCheck(currIndex, compositeFingering, pitches);
					compositeFingering.filterMarked();
				}
				if (filterBadTopologies){
					compositeFingering.hideBadTopologies();
				}
				
				// filter wrong contour
				IntList[] contour = inputListPolyPitches(inputListStart, currIndex);
				
				
				
				
				compositeFingering.addInputContour(contour);
				compositeFingering.hideBadContours();
				
				// fingerings must be constrained to glue requirements
				compositeFingering.reducetoGlue(gluePoints);
				
				// if no solutions leave, since all next fingerings would include this.
				if (compositeFingering.nFingerings() == 0) {
					//System.out.println("expand: no composite fingering");
					return;
				} else {
					//System.out.println("expand: accept");
					
					
					
					// add music elts, this keynum result, recurse,

					progressiveArpChords.add(compositeFingering);
					// REMOVED
					//jScoreElts.add(r1.jscoreElt);
					expandResult(currIndex+1, compositeFingering);
				// fingerSeqResult = fr;
				}
				
			}
	}
	
	
			
	
	
	public  Keynum.K[] mergeKs (int[] mergedPitch, Keynum.K[] k1, Keynum.K[] k2){
		
		// we have to build an array of Ks usint the mergedPitch array.
		// the ks are either copies from k1 (which comes first) or k2. 
		
		// we make a list of ks in order
		// then, for each pitch we need, we find the next k with that pitch,
		// copy to new list, and remove from this list.
		
		// this is a rather stupid algorithm but we need to get reasonable
		// pitchnames.
	
		ArrayList<Keynum.K> fromKs = new ArrayList<Keynum.K>();
		for (Keynum.K k : k1) fromKs.add(k);
		for (Keynum.K k : k2) fromKs.add(k);
		
		Keynum.K[] toKs = new Keynum.K[mergedPitch.length];
		
		// cycle through pitch array.
		for (int i = 0; i<mergedPitch.length; i++){
			int pitch = mergedPitch[i];
			Keynum.K k = Keynum.takePitch(fromKs, pitch);
			toKs[i]= k.copy(); // my copy routine in Keynum
		}
		// there shouldn't be nulls anywhere.
		return toKs;
	}
	
	public int[] mergeKNums (int[] aNums, int[] bNums) {
					//	(Keynum.K[] k1, Keynum.K[] k2)
		// arrays of midi nums.
		//int[] aNums = a.getKNums();
		//int[] bNums = b.getKNums();

		ArrayList<Point> bag = Points.combineMultisets(aNums, bNums);

		// can have max 4 items, corresponding to 4 strings.
		int sum = Points.multisetSum(bag);
		if (sum>4){ // more than 4 chord tones
			return null;
		} else {
			int[] flatNums = Points.flattenMultiset(bag);
			return flatNums;
		}
	}
	 
	/*
	 * F is a composite fingering
	 * C is a real chord that is included in F
	 * in fingering F(i), are C's notes on consecutive strings?
	 */
	
	
	
	public boolean myarrequals (int[] a, int[]b) {
		// these are same len.
		for (int i = 0; i<a.length; i++){
			if (a[i] != b[i]) {
				return false;
			}
		}; return true;
		
	}
	
	public void consecutiveStringsCheck (int curr, ChordFingering composite, int[]chordPitches) {
		//System.out.println( "[ " + curr + "] --------------- ");
		//System.out.println("testing len " + chordPitches.length + ": " + IntArr.tostring(chordPitches));
		int chordlen = chordPitches.length;
		// can quit of chordlen < 2
		int[] sortedChord = Arrays.copyOf(chordPitches,chordlen);
		
		for (ChordFingering.Fingering f : composite.getFingerings()) {
			
			// array of (string, pitch)
			Point[] cmpPoint =   f.stringPitchPoints();
			// must be in string order -- this goes from 0 to 3, rather than the usual 3->0 
			// but it doesn't matter
			 Points.sortbyX(cmpPoint);
			// we just need the y = pitch of this.
			int[] cmpPitches = Points.PointYs(cmpPoint);
			//System.out.println(" -Testing " + Points.tostring(cmpPoint));
			// all chordPitches must lie on consecutive strings.
			// we scan all len consec subsets of cmpPoint.
			int nSubseqs = cmpPoint.length - chordlen + 1;
			//System.out.println();
			boolean valid = false;
			int i = 0;
			while (i < nSubseqs && !valid) {	
				
				int[] sub = Arrays.copyOfRange(cmpPitches, i, i+chordlen);
				Arrays.sort(sub);
				//System.out.println("    subseq: " + IntArr.tostring(sub));
				if (myarrequals(sub, sortedChord)){
					valid = true;
				} else {
					i++;
				}
				
			}; // end while
			// if not valid, remove this fingering.
			
			if (!valid) {
				//System.out.println("Invalid :: remove 1 fingering from " + composite.nFingerings());			
				// composite.removeFingering(f);
				f.markForRemoval();
				} else {
					//System.out.println("Valid!");
				}
					
				
			};
			
		
		}
	
	public void filterBadTopologies(){
		
	}
	
	// we want to put inds of the longest seqs into an array.
	public void emitSeqs (){
		
		///extendedSeqs = new ArrayList<ArrayList<Seq>> ();
		
	}
		
	
	

}

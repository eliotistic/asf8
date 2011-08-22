package computingmusic;

import computingmusic.utils.IntList;



/*
 * we are comparing a bowing pattern up or down of an arp fingering to an
 * input list. we say hopw many times we went all the way up or down. 
 * This is our "strongest" criteria -- the productivity should be our goal
 * but that means choosing a magic number.
 */



public class ArpContour {
	int index;
	IntList[] inputContour;
	int[] LRpitchSequence;
	int[] RLpitchSequence;
	int countUp;
	int countDn;
	
	/*
	ArpContour (int[] inputList, int[]pseq) {
		inputContour = remBounce(inputList);
		countUp = countSubs(pseq);
		//System.out.println("Count: " + count);
		
	}
	
	ArpContour (ChordFingering.Fingering f, int[] inputList) {
		
		inputContour = remBounce(inputList);
		LRpitchSequence = f.LRpitchSequence;
		RLpitchSequence = rev(f.LRpitchSequence);
		countUp = countSubs(LRpitchSequence);
		countDn = countSubs(RLpitchSequence);
	}
	*/
	ArpContour (ChordFingering.Fingering f, IntList[] inputList) {
		
		inputContour = IntList.remBounce(inputList);
		LRpitchSequence = f.LRpitchSequence;
		RLpitchSequence = rev(f.LRpitchSequence);
		countUp = countSubs(LRpitchSequence);
		countDn = countSubs(RLpitchSequence);
	}
	// for testing
	public ArpContour(IntList[] input, int[] pitchSeq) {
		inputContour = input;
		System.out.println("pitch seq: " + Lis.tostring(pitchSeq));
		LRpitchSequence = pitchSeq;
		RLpitchSequence = rev(pitchSeq);
		countUp = countSubs(LRpitchSequence);
		countDn = countSubs(RLpitchSequence);
	}
	
	
	public void describe (){
		
		System.out.println("----input: ");
		System.out.println(inputContour);
		System.out.println("Pitch Seq: --");
		System.out.println(Lis.tostring(LRpitchSequence));
	}
	
	@SuppressWarnings("unused")
	private void describeBad(boolean bad){
		if (bad){
			System.out.println("===================BAD: " );
			describe();
		} else {
			System.out.println("=========ACCEPT");
			describe();
		}
	}
	public boolean wrong (){
		boolean bad = (countUp == 0) && (countDn == 0);	
		return bad;
	}
	
	private int[] rev(int[] a){
		int len = a.length;
		int[] o = new int[len];
		for (int i = 0; i<len; i++){
			o[(len-1)-i] = a[i];
		}
		return o;
	}
	/*
	private void remZeros(int[] a) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i : a ) {
			if (i != 0){
				list.add(i);
			}
		}
		Integer[] r = (Integer[]) list.toArray();
		//inputList = r;
	}
	
	private int[] remBounce(int[] a) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		int last = Integer.MAX_VALUE;
		for (int i : a ) {
			if (i != last){
				last = i;
				list.add(i);
			}
		}
		return Lis.ArrayofList(list);
		
	}
	*/
	
	
	private int countSubs(int[] bowingSequence){
		// count # times we exactly match this bowing sequence in input.
		// complexity because input might have chords, whereas bowing seq
		// is a series of pitches.
		
		int len = bowingSequence.length;
		int count = 0;
		//int foo =  1+inputContour.length-len;
		//System.out.println("end: " + foo);
		for (int i = 0; i<inputContour.length; i++){
			boolean found = true;
			int seqInd = 0;
			int inputInd = i;
			while (found && seqInd<len && inputInd<inputContour.length ){
				
				//System.out.println(seq[us] + " -> " + inputContour[them]);
				IntList input = inputContour[inputInd];
				
				// match as many of us as there are items in them, in
				// no particular order.
				
				int stopInd = input.matchAllSorted(bowingSequence, seqInd);
				//System.out.println("testing: " + (input.toString()));
				//System.out.println("inputInd: " + inputInd + " seqInd: " + seqInd + " StopInd:" + stopInd);
				if (stopInd == -1){
					found = false;
				} else {
					seqInd = stopInd + 1;
					inputInd++;
				}
				
			};
			// seqInd should have run to the end
			if (found && seqInd >= len) {
				//System.out.println("succeeded! at seqInd=" + seqInd);
				count++;
			};
			
		
		}
		return count;
		
		
	}
	
	public static void main(String[] str){
		
		int[] i1 = {74}; // must be sorted
		int[] i2 = {76};
		int[] i3 = {84};
		int[] i4 = {83};
		IntList[] xx = new IntList[4];
		xx[0] = new IntList(i1);
		xx[1] = new IntList(i2);
		xx[2] = new IntList(i3);
		xx[3] = new IntList(i4);
		int[] s  = {74,83,84,76};
		ArpContour a = new ArpContour(xx, s);
		System.out.println("up: " + a.countUp + " dn: " + a.countDn);
		a.wrong();
	}
	
	
}

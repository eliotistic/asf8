package hello;

import hello.utils.IntList;
import hello.utils.OpenSeq;
import hello.utils.MySeq;

import java.util.ArrayList;


public class PositionalSequencer2 {
	BigScore big;
	ArrayList<FingerSeq2> fsList;
	ArrayList<MySeq> q; // working mem for extensible seqs
	ArrayList<MySeq> ans;
	IntList allPositions;
	
	ArrayList<PSChain> allChains;
	
	private ArrayList<MySeq> chain; 
	
	ArrayList<ArrayList<MySeq>> positionsArray;
	int len;
	
	private ArrayList<MySeq> currentPositions;
	private int currentPositionalPos; 
	private int currentPositionalSize;
	public MySeq currentPositionalSeq;
	
	PositionalSequencer2(BigScore b) {
		big = b;
	
		//fsList = new ArrayList<FingerSeq2>();
		//fsList = b.fingerseq2List;
		fsList = big.arpSeq.fingerseq2List;
		positionsArray = new ArrayList<ArrayList<MySeq>>();
		len = fsList.size();
		allPositions = new IntList();
		allChains = new ArrayList<PSChain>();
		q = new ArrayList<MySeq>();
		chain = new ArrayList<MySeq>();
	
		//System.out.println("Making pos arr");
	
		rebuild();
		//showUnmarked();
		//sayChains();
	
	
	}
	
	public void rebuild(){
		positionsArray.clear();
		allPositions.clear();
		allChains.clear();
		q.clear();
		chain.clear();
		collectPositions();
		makePosArr();
		// each Seq in this is marked with start & stop	
		//System.out.println("Making chains");
		makeChains();
		sortPosArr();
	}
	
	public Position masterPos(){
		int pos = currentPositionalSeq.position;
		return new Position (pos, pos);
	}
	
	public boolean noCurrentPositionalSeq (){
		return currentPositionalSeq == null  ;
	}
	
	public boolean currPositionalSeqhasIndex (int index){
		if (noCurrentPositionalSeq()) { 
			return false;
		} else {
			return currentPositionalSeq.hasIndex(index);
		}
	}
	
	private MySeq getCurrentPositionalSeq(){
		return currentPositions.get(currentPositionalPos);
		
	}
	public void setCurrentPositions(int index){
		if (positionsArray.size() == 0) return;
		currentPositionalPos = 0;
		currentPositions = positionsArray.get(index);
		System.out.println("========setCurrentPosition:  ---- index = " + index);
		
		currentPositionalSize = currentPositions.size();
		currentPositionalSeq = getCurrentPositionalSeq();
		big.extendPanel.posArrows.setPrevNext(false, false);
		if (currentPositionalSize>1) {
			big.extendPanel.posArrows.next.setEnabled(true);
		};
		big.scoreUI.setPositionalSeq2(currentPositionalSeq);
	};
	
	public void prevPosition () {
		//if (currentPositions==null) return;
		//System.out.println("currentPositionSize: " + currentPositionalSize
		//		+ " POS: " + currentPositionalPos);
		if (currentPositionalPos > 0) {
			currentPositionalPos--;
			
			if (currentPositionalPos==0) { 
				big.extendPanel.posArrows.setPrevNext(false, true);
				
			} else {
				big.extendPanel.posArrows.setPrevNext(true, true);
				
			}
			currentPositionalSeq = getCurrentPositionalSeq();
			big.scoreUI.setPositionalSeq2(currentPositionalSeq);
			//big.showCurrFing();
			big.arpSeq.doCurr();
		}
		
	}
	
	public void nextPosition () {
		int max = currentPositionalSize-1;
		//System.out.println("Next -- currentPositionSize: " + currentPositionalSize
		// + " MAX: " + max + " POS: " + currentPositionalPos);
		//if (currentPositions==null) return;
		if (currentPositionalPos < max) {
			currentPositionalPos++;
			big.extendPanel.posArrows.setPrevNext(true, currentPositionalPos != max);
			/*
			if (currentPositionalPos==max) { 
				big.extendPanel.posArrows.setPrevNext(true, false);
			
			} else {
				big.extendPanel.posArrows.setPrevNext(true, true);
				
			}
			*/
			currentPositionalSeq = getCurrentPositionalSeq();
			big.scoreUI.setPositionalSeq2(currentPositionalSeq);
			//big.showCurrFing();
			big.arpSeq.doCurr();
		}
		
	}
	
	

	
	private boolean isMember (int p) {
		for (int x : allPositions) {
			if (x == p) return true;
		};
		return false;
	}
	private void addNewPosition (int p){
		if (!isMember(p)) {
			allPositions.add(p);
		}
	}
	
	
	
	private void addNewSeq(MySeq s, ArrayList<MySeq> seqs) {
		boolean found = false;
		int pos = s.position;
		for (MySeq s0 : seqs) {
			if (s0.position == pos) {
				found = true;
				break;
			}
		}
		if (!found) {
			seqs.add(s);
		}
	}
	
	private void addNewSeq(OpenSeq s, ArrayList<MySeq> seqs) {
		boolean found = false;
		int pos = s.position;
		for (MySeq s0 : seqs) {
			if ((s0 instanceof OpenSeq) && s0.position == pos) {
				found = true;
				break;
			}
		}
		if (!found) {
			seqs.add(s);
		}
	}
	
	
	// prelimiary pass to get a list of unique  positions
	private void collectPositions() {
		//allPositions.clear();
		for (int i = 0; i < len; i++) {
			FingerSeq2 currentFs = fsList.get(i);
			ChordFingering h = currentFs.headFingering();
			if (h.isGlued()) {
				// System.out.println("COLLECT POSITIONS: GLUE AT INDEX " + i);
			}
			for (ChordFingering.Fingering f : h.getFingerings()) {
				
				Position pos = f.getPositions();
				
				
					for (int p = pos.lo; p < pos.hi + 1; p++) {
						addNewPosition(p);
						
					}
				
			}
			
		}
	}
	
	private void makePosArr() {
		for (int i = 0; i < len; i++) {
			FingerSeq2 currentFs = fsList.get(i);

			ChordFingering h = currentFs.headFingering();
			
			ArrayList<MySeq> a = new ArrayList<MySeq>();
			for (ChordFingering.Fingering f : h.getFingerings()) {

				// f.texts();
				// /System.out.println("pseq: initializing fingering");

				Position pos = f.getPositions();
				String abc = f.getABCString();
				if (pos.isOpen()) {
					for (int p : allPositions) {
						OpenSeq s = new OpenSeq(i,p,abc);
						addNewSeq(s, a);
					}
				} else {
					for (int p = pos.lo; p < pos.hi + 1; p++) {
						//addNewPosition(p);
						MySeq s = new MySeq(i, p, abc);
						addNewSeq(s, a);
						// a.add(s);
					}
				}
				
			}
			;
			positionsArray.add(a);
		}
	}
	
	
	
	
	
	@SuppressWarnings("unused")
	private void oldmakePositionChain(int position) {
		q.clear();
		for (ArrayList<MySeq> seqs : positionsArray) {
			
			for (MySeq s : seqs) {	
				// if 0, add new "open seq" with this position
				if (s.position == 0) {
					// add this positions
					OpenSeq o = new OpenSeq(s, position);
					q.add(o);
				} else
			// put this pos and 0 in q.
				if (s.position == position) {
					q.add(s);
				}
			}
		};
		System.out.println("************NEW Q: ********** ");
		for (MySeq s : q) { s.say();
			
		}
	}
	
	private void makePositionChain(int position) {
		q.clear();
		for (ArrayList<MySeq> seqs : positionsArray) {

			for (MySeq s : seqs) {
				// if 0, add new "open seq" with this position

				// put this pos and 0 in q.
				if (s.position == position) {
					q.add(s);

				}
			}
			;
			/*
			System.out.println("************NEW Q: ********** ");
			for (Seq s : q) {
				s.say();
			}
			;
			*/
		}
	}
	
	@SuppressWarnings("unused")
	private void markChain(int start, int stop) {
		// System.out.println("--> marking chain (" + start + " " + stop + ") len = " + (chain.size()));
		for (MySeq c : chain) {
			c.start = start;
			c.stop = stop;
		};
		//chain.clear();
	}
	private void markChain(PSChain storage) {
		int len = chain.size();
		// System.out.println(" Marking LEN = " + len );
		if (len == 0) return;
		int start = chain.get(0).index;
		int stop = chain.get(len-1).index;
		// System.out.println("--> marking chain (" + start + " " + stop + ") len = " + len); 
		
		storage.addChain(start, stop);
		// we are mutating seq's in the positionArray.
		for (MySeq c : chain) {
			c.start = start;
			c.stop = stop;
		};
		//chain.clear();
	}
	
	private void markChains (int position){
		// q has list of seqs in same pos or 0. 
		// System.out.println("MARKING CHAINS, Q SIZE = " + (q.size()));
		
		PSChain c = new PSChain(position);
		
		if (q.size() == 0) {
			//System.out.println("--> Q SIZE: 0");
			return;
		}
		
		MySeq prevSeq = q.get(0);
		int prevStart = prevSeq.index;
		
		if (q.size() == 1) {
			//System.out.println("UNIT CHAIN:" + (prevSeq.tostring()));
		};

		//ArrayList<Seq> chain = new ArrayList<Seq>();
		chain.clear();
	
		for (MySeq s : q) {
			// s.touched = true;
			// int curr = prevSeq.index;
			//System.out.println("prev index = " + prevStart + " ind here = " + (s.index) );
			if (s.index == prevStart || s.index == prevStart + 1) {
				// either equal or 1 greater
				//System.out.println(prevStart + " chains with " + (s.index));
				chain.add(s);
				prevSeq = s;
				prevStart = s.index;
				
			} else {
				//System.out.println("jump index  ... finishing chain");
				// we're at start of new chain
				//int stop = prevSeq.index;
				// add start, stop
				// markChain(prevStart, stop);
				markChain(c);
				chain.clear();
				prevSeq = s;
				prevStart = s.index;
				chain.add(s);
				
			}
		};
		// finish last chain
		markChain(c);
		// markChain(prevStart, prevSeq.index);
		c.finalize(); // adds to allChains;
		chain.clear();
		
	}
	
	
	private void makeChains() {
		for (int position : allPositions) {
			makePositionChain(position);
			// q is now list of just this & 0.
			markChains(position);
		}
	}
	
	
	
	public void sayPositions(int ind) {
		System.out.println("*********** positions at " + ind + " ***");
		ArrayList<MySeq> seqs = positionsArray.get(ind);
		for (MySeq s : seqs) {
			s.say();
		}
	
	}
	
	
	public class PSChain {
		int position;
		int nChains;
		int len;
	    ArrayList<MySeq> chains;
		
		PSChain (int pos){
			len = 0;
			position = pos;
			nChains = 0;
			chains = new ArrayList<MySeq>();
		}
		
		public void addChain (int start, int stop) {
			MySeq s = new MySeq(start,stop, position);
			chains.add(s);
			nChains++;
			len = len + s.length();
		}
		public void finalize(){
			allChains.add(this);
		}
		public void say () {
			System.out.println("<Chain pos:" + position + " nChains: " + nChains + " len:" 
					+ len +">");
		}
	}
	
	public void sayChains () {
		System.out.println("*************CHAINS");
		for (PSChain c : allChains) {
			c.say();
		}
	}
	
	
	/*
	 * FingerSeq2's show how pasages can be contracted in
	 * one chord. The elts of these are Fing.Results. 
	 * The 1st elt (the head) is a note or chord as it appears im
	 * the score. Fing.Results show different ways of fingering
	 * it.
	 * 
	 *  Starting from the first elt, we want to construct all possible
	 *  paths through successive elts that have common positions. 
	 */
	/*
	public class Seq {
		//boolean touched;
		public int index;
		public int start;
		public int stop;
		public String abc;
		
		//Position masterPos; // doing this immutably
		int position;
		//ArrayList<Fing.Fingering> seq;
		
		//ArrayList<MusicElement> musicElements;
		//ArrayList<Rectangle> lineRects; // bounding boxes added by BigJScoreComponent
		public ArrayList<Rectangle> lineRects; // for drawing
		
		
		Seq (int ind, int pos, String a){
			//touched = false;
			abc = a;
			start = -1;
			stop = -1;
			index = ind;
			position = pos;
			
			
		}
		Seq (int start, int stop, int pos){
			index = start;
			this.start = start;
			this.stop = stop;
			position = pos; 
			
		}
		public boolean hasIndex(int i) {
			if (this == null) {
				return false;
			} else
			return (start<=i && stop >= i);
		}
		
		public String positionString() {
	    		return "Position: " + position;
	    }

		public int length (){
			return stop - start + 1;
		}
		
		public boolean posEqual (Seq s0) {
			return s0.position == position; 
		}
		
		public String tostring() {
			String len = "" + (stop-start+1);
			if (stop==-1) len = "???";
			return ("ind" + index + " " + abc + " Start: " + start + " Stop: " + stop + " " +
					"pos:" + position  + " len: " + len);
				
		}
		public void say (){
			System.out.println((tostring()));
		}
		
	

			
			

			
		}
*/
/*		
	public class OpenSeq extends Seq {
		public OpenSeq(Seq s, int pos) {	
			super(s.index, pos, s.abc);
			
		}
		public OpenSeq(int index, int pos, String abc) {	
			super(index, pos, abc);
			
		}
		
		
	}
		
*/	
	public void showUnmarked(){
		int c = 0;
		for (ArrayList<MySeq> seqs : positionsArray) {
			for (MySeq s : seqs) {
				if (s.start == -1 || s.stop == -1) 
					//(!s.touched)
				{
					c++;
					s.say();
				}
			}
		}
		System.out.println("Unmarked: " + c);
	}
		
/*	
static Comparator<Seq> byLenDn = new SeqLenComparator();
	
	static class SeqLenComparator implements Comparator<Seq>{

	    public int compare(Seq s1, Seq s2) {
	    	int len1 = s1.length();
	    	int len2 = s2.length();
	    	
	       if (len1 < len2) {
	    	   return 1;
	       } else if (len1 > len2) {
	    	   return -1;
	       } else { //equal -- sort by LEAST pos.
	    	   int p1 = s1.position;
	    	   int p2 = s2.position;
	    	   if (p1 < p2) {
	    		   return -1;
	    	   } else if (p1 > p2) {
	    		   return 1;
	    	   }
	    	   else return 0;
	       }
	    }
	    
	}
	
	public static  void SeqArrSortbyLen (ArrayList<Seq> s){
	
		Collections.sort(s, byLenDn);
	}	
	*/
	private void sortPosArr() {
		for (ArrayList<MySeq> s : positionsArray) {
			MySeq.ArrSortbyLen(s);
		}
	}
	
	

}



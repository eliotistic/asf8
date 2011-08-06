package hello.utils;

	// utilities for int[]


import java.util.ArrayList;
import java.util.Collections;


public class IntList extends ArrayList<Integer> {
		
		
		/**
	 * 
	 */
	private static final long serialVersionUID = 4406674038487004692L;

	public IntList() {
	}

	public IntList(int[] a) {
		for (int i : a) {
			add(i);
		}
	}
	public IntList(int[] a, int start, int stop){
		for (int i = start; i<=stop; i++ ) {
			add(a[i]);
		}
	}
	
	public void addNew (int i){
		if (!contains(i)){
			add(i);
		}
	}
	public void sort () {
		Collections.sort(this);
	}
	
	public IntList copy () {
		IntList i = new IntList();
		for (int x : this){
			i.add(x);
		}
		return i;
	}

	
	/**
	 * 
	 * @return for intlist a1,a2,a3, ... return (a2-a1, a3-a2, ...) 
	 * 
	 */
	public IntList diffs () {
		IntList d = new IntList();
		d.sort();
		for (int i = 0; i<size()-1;i++){
			d.add(get(i+1) - get(i));
		}
		return d;
	
		
	}
	// step is 1,2,3 ... 
	public Chain stepChains(){
		int sz = size();
		if (sz < 2) return null;
		
		Chain c = new Chain();
		int last = get(0);
		int lastStart = 0;
		
		for (int i=1; i<sz; i++){
			int here = get(i);
			if (here != last + 1) {
				Seq s = new Seq (lastStart,i-1);
				c.add(s);
				last = here;
				lastStart = i;
			} else {
				last = here;
			}
		};
	// last
		c.add(new Seq (lastStart, sz-1));
	return c;
	}
	
	
	public static int count (IntList[] a) {
		int sz = 0;
		for (IntList x : a ) {
			sz = sz + x.size();
		}
		return sz;
	} 
		
		
	public static IntList[] remBounce(IntList[] a) {
		IntList last = new IntList();
			
		ArrayList<IntList> acc = new ArrayList<IntList>();
		for (IntList x : a) {
				if (!x.equals(last)) {
					acc.add(x);
					last = x;
				}
		};
		int len = acc.size();
		IntList[] out = new IntList[len];
		for (int i = 0; i<len;i++){
				out[i]=acc.get(i);
			};
		return out;
		
		
	}
	/*
	public class Chain {
		int position;
		int nChains;
		int len;
	    ArrayList<Seq> chains;
		
		Chain (int pos){
			len = 0;
			position = pos;
			nChains = 0;
			chains = new ArrayList<Seq>();
		}
		
		public void addChain (int start, int stop) {
			Seq s = new Seq(start,stop, position);
			chains.add(s);
			nChains++;
			len = len + s.length();
		}
		public void finalize(){
			// allChains.add(this);
		}
		public void say () {
			System.out.println("<Chain pos:" + position + " nChains: " + nChains + " len:" 
					+ len +">");
		}
	}
	*/
	
	public int matchAllSorted (int[] seq, int start){
		// we are sorted!
		
		// take as many consecutive elts from pseq as are needed to match 
		// ALL elts in intlist, in no order,
		// we lose if we run out.
		// if bad, return -1, else the out ind of seq..
		
		
		int len = size();
		if (len == 0) return -1;
		
		if (len == 1) {
			if (contains(seq[start])){
				return start;
			} else{
				return -1;
			}
		}
		
		int seqLen = seq.length; 
		
		int stop = start+(len-1);
		if (stop >= seqLen) return -1;
		
		IntList out = new IntList(seq, start,stop);
		Collections.sort(out);
		if (this.equals(out)){
			return stop;
		} else {
			return -1;
		}
		
		
		
	}
	/*
	public static String tostring(IntList[] a) {
		String s = "";
		for (IntList x : a) {
			s = s + " " + x.toString();
		}
		return s;
	}
	*/
			
	
		
		// 2 int arrs, say total number of unique items.
		// we assume, however, that each arr is unique
		// but this is not so: chords can hav duplicates.
		
		// eg: (d5 d5 a5) -> (a5 a5)
		// in chord 1, d5 must be fingered in 2 different ways.
		// in chord 2, a5 2 ways also.
		
		// let's say:
		// 1: d5 a5 a5
		// 2: a5 a5
		
		//this can all be absorbed in one position, which is what we want to know. 
		// to test this, we say: 1st a5 in 2nd chord is taken by 1st in 1st chord
		// 2nd also there. 
		// there are thus a total of 3 notes to be played (2 of which are same).
		
		
	public static int mergeUnique(int[] a, int[] b) {

		return 0;
	}
		
		/*
		// old version, does not understand doubles.
		public static int countUnique (int[] a, int[] b){
			ArrayList<Integer> sum = toList(a);
			for (int bb : b) {
				if (!sum.contains(bb)) {
					sum.add(bb);
				};
				
			};
			return sum.size();
		}
		
		*/
		
	public static String tostring(int[] a) {
		        String s = ("(");
		        for (int i : a) {
		            s = s + i + " ";
		        }
		        return s + ")";
		 }
		 
	public static void print(int[] a) {
		System.out.print("(");
		for (int i : a) {
			System.out.print(i + " ");
		}
		System.out.println(")");
	}
	
	public static void main(String[] s) {
		int[] a = { 1, 2, 3, 5, 6, 7, 10, 11, };
		//int[] b = { 0, 1, 3, 2 };
		IntList alist = new IntList(a);

		// System.out.println(alist);
		// System.out.println(alist.matchAllSorted(b, 0));
		// System.out.println(alist.matchAllSorted(b, 1));
		System.out.println(alist.stepChains());
	}

	public boolean allUnder(int numChords) { // TODO remove if unused
		for(int i : this)
		{
			System.out.println("numChords: " + numChords);
			System.out.println("int here: " + i);
			if(numChords <= i)
			{
				return false;
			}
		}
		System.out.println("HERE");
		return true;
	}
	
		 
		 
	}



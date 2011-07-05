package hello.utils;

import java.util.BitSet;

//import com.sun.org.apache.xalan.internal.xsltc.dom.BitArray;

public class Bv extends BitSet {
	
	private static final long serialVersionUID = 1L;
	int printSize = 0;
	
	public Bv(int s){
		super(s);
		printSize = s;
	}
	
	
	public Bv (String binary){
		// string is dot or plus
		this(binary.length());
		Character plus = '+';
		for (int i = 0; i<binary.length(); i++){
			if (binary.charAt(i) == plus){
				set(i);
			}
		}
		
	}
	
	public Bv copy (){
		Bv c = new Bv(size());
			for (int i = 0; i< size(); i++){
				c.set(i, get(i));
			}
		return c;
		
	}
	
	public IntList toList () {
		IntList a = new IntList();
		int ind = 0;
		boolean stop = false;
		while (!stop){
			int next = nextSetBit(ind);
			//System.out.println("Next: " + next);
			if (next >= 0) {
				a.add(next);
				ind = next+1;
			}  else {
				stop = true;
			}
		}
		return a;
	}
	/**
	 *
	 * @return  Total number of 0's between 1's
	 */
	public int countGaps () {
		int gaps = 0;
		int pendingGaps = 0;
		boolean foundTrue = false;
		for (int i = 0; i< size(); i++){
			if (get(i) && foundTrue){
					gaps = gaps + pendingGaps;
					pendingGaps = 0;
				} else if (get(i)) {
					foundTrue = true;
					
				} else if (foundTrue) {
					pendingGaps++;
				} else {
					
				}
		}
		return gaps;
	}
	
	@Override
	public String toString (){
		String s = "";
		for (int i = 0; i<size(); i++){
			if (get(i)){
				s = s+"+";
			} else {
				s = s+".";
			}
		}
		return s;
	}
	public static void main(String[] sss){
		int[] saiten = {0, 1,3};
		Bv b = new Bv(4);
		for (int x : saiten) {
			b.set(x);
		}
		Bv c = new Bv(".+..+....+++.+....");
		System.out.println("Gaps: " + c.countGaps());
		System.out.println("Card: " + c.cardinality());
		System.out.println(b.size() + " " + b);
		System.out.println(new Bv("+.+.+.++"));
		System.out.println(b.copy());
		System.out.println("toList: " + c + " "  + c.toList());
	}
	
}

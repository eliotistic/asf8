package hello;

import hello.utils.Bv;
import hello.utils.BvList;
import hello.utils.IntList;

/**
 * 
 * @author eliot
 *
 */
public class Bowing {
	private IntList sequence; // jb: seems not implemented
	int skips; // how many strings we skip over, which in general we want to avoid.
	int lastSaite;
	Bv lastSaitenBv;
	BvList bvSequence;
	
	Bowing(){
		sequence = new IntList();
		bvSequence = new BvList();
		skips = 0;
		lastSaite = -1;
		lastSaitenBv = new Bv(4);
		
	}
	 /**
	  * 
	  */
	
	public IntList getSequence()
	{
		return sequence;
	}
	
	public Bv bvOfSequence (IntList saiten) {
		Bv b = new Bv (4);
		for (int i : saiten) {
			b.set(i);
		}
		return b;
	}
	
	public static int keyOfSequence (IntList saiten) {
		int[] primes = { 2, 3, 5, 7 };
		int k = 1;
		for (int s : saiten) {
			k = k * primes[s];
		}
		switch (k) {
			case 2: return 0;
			case 3: return 1;
			case 5: return 2;
			case 7: return 3;
			case 6: return 4; // 0, 1
			case 15: return 5; // 1, 2
			case 35: return 6; // 2, 3
			case 30: return 7;  // 0 1 2 
			case 105: return 8; // 1 2 3
			case 210: return 9; //1 2 3 4
			default: return k;
		
		}
	
	}
	
	private int bvSkips (Bv x, Bv y) {
		Bv xx = x.copy();
		xx.or(y);
		return xx.countGaps();
	}
	
	@SuppressWarnings("unused")
	private int getSkip (int x, int y){
		// add 1 for every open string we cross without playing.
		// ignore chords for now
		if (x<4 && y < 4){
			int d = Math.abs(x-y);
			if (d == 0) {
				return 0;
			} else {
				return d-1;
			} 
		} else {
			return 0;
		}
		
		
	}
	public void addKey(IntList saiten){
		//int k = keyOfSequence(saiten);
		Bv bv = bvOfSequence(saiten);
		//sequence.addAll(saiten);
		if (lastSaitenBv != null) {
			skips = skips + bvSkips(lastSaitenBv, bv);
		}
		lastSaitenBv = bv;
		bvSequence.add(bv);
	}
	
	public Bowing copy() {
		Bowing b = new Bowing();
		b.sequence = sequence.copy();
		b.bvSequence = bvSequence.copy();
		b.skips = skips;
		b.lastSaite = lastSaite;
		b.lastSaitenBv = lastSaitenBv.copy();
		return b;
	}
	private String bvtoString (Bv b) {
		IntList ii = b.toList();
		if (ii.size() == 1) {
			return ii.get(0).toString();
		} else {
			return ii.toString();
		}
	}
	
	private String bvArrtoString(){
		String s = "[";
		for (Bv b : bvSequence) {
			s = s + (bvtoString(b)) + " "; 
		}
		return s + "]";
	}
	
	@Override
	public String toString(){
		String s = bvArrtoString();
		return "Skips: " + skips + ": " + (strsplit(s, 18));
	}
	
	private String strsplit(String s, int sz) {
		int len = s.length();
		String ans = "";
		int start = 0;

		// int out = sz;
		while (start < len) {
			int out = start + sz;
			if (out > len) {
				out = len;

			}
			ans = ans + "<br>" + s.subSequence(start, out).toString();
			start = start + sz;
		}

		return ans;
	}
	
	
	public String describe(){
		return "Skips: " + skips + "<br>" + (strsplit(bvArrtoString(), 27));
	}
	
	public static void main (String[] sss) {
		int[] a = {0};
		int[] b = {2};
		IntList aa = new IntList(a);
		IntList bb = new IntList(b);
		Bowing w = new Bowing();
		w.addKey(aa);
		w.addKey(bb);
		System.out.println(w);
		String xx = w.strsplit("ABCABCABC", 3);
		String yy = w.strsplit("ABCDABCDABC", 2);
		System.out.println(xx + "\n" + yy);
		
	}

}

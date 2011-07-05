package hello.utils;

//import java.util.ArrayList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


@SuppressWarnings("rawtypes")
public class IntSet extends HashSet<Integer> implements Comparable {
	
	private static final long serialVersionUID = 1L;

	public IntSet(){
		
	}
	public IntSet(int[] a) {
		for (int x : a) add(x);
		
	}
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Object o) {
		
		if (this.equals((HashSet<Integer>) o)){
			return 0;
		} else {
			
			return 1;
		}
		
	}
	
	public IntSet copy () {
		IntSet c = new IntSet();
		for (int a : this) c.add(a);
		return c;
	}
	
	public int max () {
		int max = Integer.MIN_VALUE;
		for (Integer i : this){
			max = Math.max(max, i);
		}
		return max;
	}
	public int min () {
		int min = Integer.MAX_VALUE;
		for (Integer i : this){
			min = Math.min(min, i);
		}
		return min;
	}
	
	public IntSet inter (IntSet s){
		
		  IntSet t = copy();
		  t.retainAll(s);
		  return t;
	}
	
	public boolean empty(){
		return size() == 0;
	}
	
	public int[] toarray() {
		Object[] a = toArray();
		int len = size();
		int[] o = new int[len];
		for (int i = 0; i<len;i++){
			o[i] =  ((Number)a[i]).intValue();
		}
		return o;
	}
	
	
	
	
	
	public static IntSet[] remDupes(IntSet[] a){
		IntSet last = new IntSet();
		
		ArrayList<IntSet> acc = new ArrayList<IntSet>();
		for (IntSet x : a) {
			if (!x.equals(last)) {
				acc.add(x);
				last = x;
			}
		};
		int len = acc.size();
		IntSet[] out = new IntSet[len];
		for (int i = 0; i<len;i++){
			out[i]=acc.get(i);
		};
		return out;
	}
	
	
	@Override
	 public  String toString(){
      	String s = "s{";
    	String space = ""; 
    	Iterator it = iterator();
    	while (it.hasNext()) {
    		int p = (Integer)it.next();
    		s = s + space + p;
    		space = ", ";
    	}
    	
    	s = s + "}";
    	return s;
    	
    }
	
	/*
	public String tostring(){
		return Lis.tostring(toarray());
			
	}
	*/
	public static void main (String[] args){
		int[] a = {2,1,2,1,3,3,3,3,3,24};
		int[] b = {10, 20, 20, 2,3};
		IntSet a1 = new IntSet(a);
		IntSet b1 = new IntSet(b);
		IntSet x = a1.inter  (b1);
		//Object[] b2 = b1.toArray();
		System.out.println(a1 + " --- " + b1 + "INTER: " + x );
		//System.out.println(a1.toString());
		//System.out.println(b1.toString());
	}
	
	
	
}

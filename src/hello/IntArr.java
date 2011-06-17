package hello;

import java.util.ArrayList;
import java.util.Arrays;



// utilities for int[]
public class IntArr {
	
	
	
	/*
	public boolean equalContent (int[] a, int[] b){
		//int[] aa = Arrays.copyOf(a);
		Arrays.sort(a);
		return true;
		
	}
    */
	
	public static ArrayList<Integer> toList(int[] a) {
        ArrayList<Integer> b = new ArrayList<Integer>();

        for (int i : a) {
            b.add(i);
        }
        return b;
	}
	
	
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
	
	
	public static int mergeUnique (int[] a, int[] b){
		
		
		
		return 0;
	}
	
	
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
	 
	 public static void main (String[] s){
		 int[] a = {1,2,3};
		 int[] b = {1,4};
		 int unique = countUnique(a, b);
		 System.out.println("Unique " + tostring(a) + " " + tostring(b) + ": " + unique);
	 }
	 
	 
}

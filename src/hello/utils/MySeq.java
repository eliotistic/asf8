package hello.utils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MySeq {
	//boolean touched;
	public int index;
	public int start;
	public int stop;
	public String abc;
	
	//Position masterPos; // doing this immutably
	public int position;
	//ArrayList<Fing.Fingering> seq;
	
	//ArrayList<MusicElement> musicElements;
	//ArrayList<Rectangle> lineRects; // bounding boxes added by BigJScoreComponent
	public ArrayList<Rectangle> lineRects; // for drawing
	
	
	public MySeq (int ind, int pos, String a){
		//touched = false;
		abc = a;
		start = -1;
		stop = -1;
		index = ind;
		position = pos;
		
		
	}
	public MySeq (int start, int stop, int pos){
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
	
	public boolean posEqual (MySeq s0) {
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
	
	static Comparator<MySeq> byLenDn = new SeqLenComparator();

	static class SeqLenComparator implements Comparator<MySeq>{

    public int compare(MySeq s1, MySeq s2) {
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

	public static  void ArrSortbyLen (ArrayList<MySeq> s){
		Collections.sort(s, byLenDn);
	}		
}
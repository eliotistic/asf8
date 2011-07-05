package hello.utils;


import hello.Instrument;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;



public class PointList extends ArrayList<Point>{
	
	private static final long serialVersionUID = 1L;

	public PointList copy() {
		PointList ans = new PointList();
		for (int i = 0; i < size(); i++) {
			ans.add(get(i));
		};
		return ans;
	}
	
	
	
	
	
	public void addNew (Point p){
		if (!contains(p)) {
			add(p);
		}
	}
	

		// points -> array of (int) y's
	public IntList PointYs (){
			IntList ys = new IntList(); 
			for (int i = 0; i<size(); i++){
				ys.add(get(i).y);
			};
			return ys;
		}
	
	public IntList PointXs (){
		IntList xs = new IntList(); 
		for (int i = 0; i<size(); i++){
			xs.add(get(i).x);
		};
		return xs;
		}
	
	
	private Point copyPoint (Point z) {
		return new Point (z.x, z.y);
	}
	
	// ** 
	
	// points are in order
	// chain:
	// (_, x) (y, _) 
	// where: y<=x+1
	
	public PointList chainPoints(){
		//int sz = size();
		PointList c = new PointList();
		
		//Point last = get(0);
		
		Point curr = copyPoint(get(0));
		
		
		for (Point p : this) {
			//int here = p.x;
			if (p.x > curr.y + 1) { // end current chain
				c.add(curr);
				curr = copyPoint(p);
			} else {
				// extend curr
				curr.y = Math.max(curr.y,p.y);
			
			}
		};
	// last
		c.add(curr);
	return c;
	}
		
	public PointList chainAdd(Point p){
		add(p);
		return chainPoints();
	}
		
		
	public Comparator<Point> byY  = new PointYComparator();
		
	public class PointYComparator implements Comparator<Point>{

		    public int compare(Point p1, Point p2) {
		       if (p1.y < p2.y) {
		    	   return -1;
		       } else if (p1.y > p2.y) {
		    	   return 1;
		       } else return 0;
		    }
		    
		}

	public Comparator<Point> byYUp  = new PointYUpComparator();
		
	public class PointYUpComparator implements Comparator<Point>{

		    public int compare(Point p1, Point p2) {
		       if (p1.y < p2.y) {
		    	   return 1;
		       } else if (p1.y > p2.y) {
		    	   return -1;
		       } else return 0;
		    }
		    
		}
		
	public void sortbyY (){
			//int[] ans = new int[ps.length];
			Collections.sort(this, byY);
		}	
	public void sortbyYUp (){
			//int[] ans = new int[ps.length];
			Collections.sort(this, byYUp);
		}	
		
	public PointList pointSortYOrder (){
			PointList p1 = copy();
			p1.sortbyY();
			return p1;
		}
		
	public Comparator<Point> byX = new PointXComparator();
		
	public class PointXComparator implements Comparator<Point>{

		    public int compare(Point p1, Point p2) {
		       if (p1.x < p2.x) {
		    	   return -1;
		       } else if (p1.x > p2.x) {
		    	   return 1;
		       } else return 0;
		    }
		    
		}
	public Comparator<Point> byXUP  = new PointXUpComparator();
		
	public class PointXUpComparator implements Comparator<Point>{

		    public int compare(Point p1, Point p2) {
		       if (p1.x < p2.x) {
		    	   return 1;
		       } else if (p1.x > p2.x) {
		    	   return -1;
		       } else return 0;
		    }
		    
		}
		
	public void sortbyX (){
			//int[] ans = new int[ps.length];
			Collections.sort(this, byX);
		}	
	public void sortbyXUp (){
			//int[] ans = new int[ps.length];
			Collections.sort(this, byXUP);
		}	
		
		
	public PointList pointSortXOrder (){
			PointList p1 = copy();
			p1.sortbyX();
			return p1;
		}
		
		
	public Point[] toarray (){
		Point[] ans = new Point[size()];
			for (int i = 0; i<size(); i++){
				ans[i] = get(i);
			}
		return ans;
		}
			
		/* ***************************************************************
		 * * Multisets of ints: Point[] with x = our int, y = count
		 * ***************************************************************
		*/
		
		
	public Point multisetFind (int item){
		for (Point p : this) {
				if (p.x == item) return p;
			};
			return null;
		
		}
		
		
	public void multisetAdd (int item){
		boolean found = false;
			int ind = 0;
			int size = size();
			while (!found && ind < size){
				Point p = get(ind);
				if (p.x == item){
					p.y++;
					found = true;
					
				} else {
					ind++;
				}
			};
			if (!found){
				Point newP = new Point (item, 1);
				add(newP);
			}
		}
		
	public void multisetMerge(int[] items){
		for (int item : items){
				multisetAdd(item);
			}
		}
		
		
		
	public PointList multisetMaxMerge(PointList s2){
		PointList bag = new PointList();
			
			// get unique things in p1, + 
		for (Point p1 : this){
				Point p2 = s2.multisetFind(p1.x) ;
				if (p2 == null){
					bag.add(p1);
				} else if (p1.y > p2.y){
					bag.add(p1);
				} else {
					bag.add(p2);
				}
					
			};
			
			// now get things in p2 that are not in p1.
			for (Point p2 : s2){
				Point p1 = multisetFind(p2.x) ;
				if (p1 == null){
					bag.add(p2);
				}
			};
			return bag;
		};
		
		public int multisetSum () {
			int sum = 0;
			for (Point p : this){
				sum = sum + p.y;
			};
			return sum;
		}
		
		
		public int[] flattenMultiset(){
			int[] flat = new int[multisetSum()];
			int ind = 0;
			for (Point p : this) {
				for (int i = 0; i<p.y;i++){
					flat[ind] = p.x;
					ind++;
				}
			}
			return flat;
		}
		
		
		public PointList toMultiset(int[] a){
			 //HashMultiset h = new HashMultiset();
		
			PointList bag = new PointList();
			for (int item : a) {
				bag.multisetAdd(item);
			};
			return bag;
		}
		
		
		public PointList combineMultisets(int[] a, int[] b){
			PointList abag = toMultiset(a);
			PointList bbag = toMultiset(b);
			
			return abag.multisetMaxMerge(bbag);
		}
		
		
		
		// representing point lists for (saite, interval) 
		public boolean hasOpenStrings (){
			for (Point p : this) {
				if (p.y == 0) return true; 
			};
			return false;
		}
		
		
		
		public PointList filterOpenStrings (){
			PointList list = new PointList();
			for (Point p : this) {
				if (p.y != 0) list.add(p); 
			};
			return list;
		}
		
		static int pointPitch(Instrument v, Point p){
	    	return (v.openStringPitch(p.x) + p.y);
	    }
		
		
		public Point range (Instrument v){
			
	    	int lo = 127;
	    	int hi = 0;
	    	for (Point p : this ){
	    		int pitch = v.pointPitch(p);
	    		if (pitch < lo) lo = pitch;
	    		if (pitch > hi) hi = pitch;
	    	}
	    	return new Point (lo, hi);
	    }
		
		
	   
		public String tostring(){
			String s = "";
			for (Point p : this){
				s = s + "(" + p.x + ", " + p.y + ") "; 
			}
			return s;
		}
		private  String tostring(Point p){
			return  "<" + p.x + ", " + p.y + ">"; 	
		}
		@Override
		 public  String toString(){
	      	String s = "{";
	    	String space = ""; 
	    	@SuppressWarnings("rawtypes")
			Iterator it = iterator();
	    	while (it.hasNext()) {
	    		Point p = (Point) it.next();
	    		s = s + space + (tostring(p));
	    	}
	    	
	    	s = s + "}";
	    	return s;
	    	
	    }
		
		
		
	
		/*
		public static void main (String[] s){
			int[] x = {1,1, 2, 3, 5532};
			int[] y = {1,2,2,3, 212};
			ArrayList<Point> x1 = toMultiset(x);
			ArrayList<Point> x2 = toMultiset(y);
			ArrayList<Point> bag = multisetMaxMerge(x1,x2);
			int[] flatSet = flattenMultiset(bag);
			
			System.out.println(tostring(bag) + " Sum: " + (multisetSum( bag )));
			System.out.println("Flat: " + IntArr.tostring(flatSet));
		}
			
		*/
		public static void main (String[] s){
			//int[] x = {1,1, 2, 3, 5532};
			//int[] y = {1,2,2,3, 212};
			PointList pp = new PointList();
			pp.add(new Point (1, 2));
			//pp.add(new Point (11, 20));
			PointList c = pp.chainPoints();
			System.out.println("orig  :" + pp);
			System.out.println("chain :" + c);
			
		}
		}




package computingmusic;

//import computingmusic.Fing.XCase;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Points {
	
	
	
	
	static Point[] copy(Point[] ps){
		Point [] ans = new Point [ps.length];
		for (int i = 0; i<ps.length; i++){
			ans[i] = ps[i];
		};
		return ans;
	}

	// points -> array of (int) y's
	static int[] PointYs (Point[] ps){
		int[] ys = new int[ps.length];
		for (int i = 0; i<ps.length; i++){
			ys[i] = ps[i].y;
		};
		return ys;
	}
	static int[] PointXs (Point[] ps){
		int[] xs = new int[ps.length];
		for (int i = 0; i<ps.length; i++){
			xs[i] = ps[i].x;
		};
		return xs;
	}
	
	
	
	static Comparator<Point> byY  = new PointYComparator();
	
	static class PointYComparator implements Comparator<Point>{

	    public int compare(Point p1, Point p2) {
	       if (p1.y < p2.y) {
	    	   return -1;
	       } else if (p1.y > p2.y) {
	    	   return 1;
	       } else return 0;
	    }
	    
	}

	static Comparator<Point> byYUp  = new PointYUpComparator();
	
	static class PointYUpComparator implements Comparator<Point>{

	    public int compare(Point p1, Point p2) {
	       if (p1.y < p2.y) {
	    	   return 1;
	       } else if (p1.y > p2.y) {
	    	   return -1;
	       } else return 0;
	    }
	    
	}
	
	public static  void sortbyY (Point[] ps){
		//int[] ans = new int[ps.length];
		Arrays.sort(ps, byY);
	}	
	public static  void sortbyYUp (Point[] ps){
		//int[] ans = new int[ps.length];
		Arrays.sort(ps, byYUp);
	}	
	
	public static Point[] pointSortYOrder (Point[] ps){
		Point[] p1 = copy(ps);
		sortbyY(p1);
		return p1;
	}
	
	static Comparator<Point> byX = new PointXComparator();
	
	public static class PointXComparator implements Comparator<Point>{

	    public int compare(Point p1, Point p2) {
	       if (p1.x < p2.x) {
	    	   return -1;
	       } else if (p1.x > p2.x) {
	    	   return 1;
	       } else return 0;
	    }
	    
	}
	static Comparator<Point> byXUP  = new PointXUpComparator();
	
	static class PointXUpComparator implements Comparator<Point>{

	    public int compare(Point p1, Point p2) {
	       if (p1.x < p2.x) {
	    	   return 1;
	       } else if (p1.x > p2.x) {
	    	   return -1;
	       } else return 0;
	    }
	    
	}
	
	public static  void sortbyX (Point[] ps){
		//int[] ans = new int[ps.length];
		Arrays.sort(ps, byX);
	}	
	public static  void sortbyXUp (Point[] ps){
		//int[] ans = new int[ps.length];
		Arrays.sort(ps, byXUP
				);
	}	
	
	
	public static Point[] pointSortXOrder (Point[] ps){
		Point[] p1 = copy(ps);
		sortbyX(p1);
		return p1;
	}
	
	
	static Point[] pointListArr (ArrayList<Point> ps){
		Point[] ans = new Point[ps.size()];
		for (int i = 0; i<ps.size(); i++){
			ans[i] = ps.get(i);
		}
		return ans;
	}
		
	/* ***************************************************************
	 * * Multisets of ints: Point[] with x = our int, y = count
	 * ***************************************************************
	*/
	
	
	static Point multisetFind (ArrayList<Point> bag, int item){
		for (Point p : bag) {
			if (p.x == item) return p;
		};
		return null;
	
	}
	
	
	static void multisetAdd (ArrayList<Point> bag, int item){
		boolean found = false;
		int ind = 0;
		int size = bag.size();
		while (!found && ind < size){
			Point p = bag.get(ind);
			if (p.x == item){
				p.y++;
				found = true;
				
			} else {
				ind++;
			}
		};
		if (!found){
			Point newP = new Point (item, 1);
			bag.add(newP);
		}
	}
	
	static void multisetMerge(ArrayList<Point> bag, int[] items){
		for (int item : items){
			multisetAdd(bag, item);
		}
	}
	
	
	
	static ArrayList<Point> multisetMaxMerge(ArrayList<Point> s1, ArrayList<Point> s2){
		ArrayList<Point> bag = new ArrayList<Point>();
		
		// get unique things in p1, + 
		for (Point p1 : s1){
			Point p2 = multisetFind(s2, p1.x) ;
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
			Point p1 = multisetFind(s1, p2.x) ;
			if (p1 == null){
				bag.add(p2);
			}
		};
		return bag;
	};
	
	static int multisetSum (ArrayList<Point> bag) {
		int sum = 0;
		for (Point p : bag){
			sum = sum + p.y;
		};
		return sum;
	}
	
	
	static int[] flattenMultiset(ArrayList<Point> bag){
		int[] flat = new int[multisetSum(bag)];
		int ind = 0;
		for (Point p : bag) {
			for (int i = 0; i<p.y;i++){
				flat[ind] = p.x;
				ind++;
			}
		}
		return flat;
	}
	
	
	static ArrayList<Point> toMultiset(int[] a){
		 //HashMultiset h = new HashMultiset();
	
		ArrayList<Point> bag = new ArrayList<Point>();
		for (int item : a) {
			multisetAdd(bag, item);
		};
		return bag;
	}
	
	
	static ArrayList<Point> combineMultisets(int[] a, int[] b){
		ArrayList<Point> abag = toMultiset(a);
		ArrayList<Point> bbag = toMultiset(b);
		
		return multisetMaxMerge(abag, bbag);
	}
	
	
	static boolean hasOpenStrings (Point[] ps){
		for (Point p : ps) {
			if (p.y == 0) return true; 
		};
		return false;
	}
	
	static int pointPitch(Instrument v, Point p){
	    	return (v.openStringPitch(p.x) + p.y);
	    }
	
	static Point[] filterOpenStrings (Point[] ps){
		ArrayList<Point> list = new ArrayList<Point>();
		for (Point p : ps) {
			if (p.y != 0) list.add(p); 
		};
		return pointListArr(list);
	}
	
	
	static Point range (Instrument v, Point[] ps){
		
	        	int lo = 127;
	        	int hi = 0;
	        	for (Point p : ps){
	        		int pitch = v.pointPitch(p);
	        		if (pitch < lo) lo = pitch;
	        		if (pitch > hi) hi = pitch;
	        	}
	        	return new Point (lo, hi);
	        }
	static Point range (Instrument v, ArrayList<Point> ps){
		
    	int lo = 127;
    	int hi = 0;
    	for (Point p : ps){
    		int pitch = v.pointPitch(p);
    		if (pitch < lo) lo = pitch;
    		if (pitch > hi) hi = pitch;
    	}
    	return new Point (lo, hi);
    }
	
	
   
	public static String tostring(Point[] ps){
		String s = "";
		for (Point p : ps){
			s = s + "(" + p.x + ", " + p.y + ") "; 
		}
		return s;
	}
	public static String tostring(ArrayList<Point> ps){
		String s = "";
		for (Point p : ps){
			s = s + "(" + p.x + ", " + p.y + ") "; 
		}
		return s;
	}
	
	
	
	
	public static void main2 (String[] s){
		Point[] p = { new Point (3, 0) };
		Point[] q = filterOpenStrings(p);
		System.out.println("Non open:" + q.length);
		
	}
	public static void main3 (String[] s){
		/*Point[] ps = { new Point (3, 6),
					   new Point (2, 10),
					   new Point (1, 4),
					   new Point (0, 6)
				};*/
		Point[] ps1 = { new Point (3, 1),
				        new Point (2, 2),
				        new Point (1, 1),
				        new Point (0, 2)
			};
		//Comparator<Point> byY  = new PointYComparator();
		//Arrays.sort(ps, byY);
		Point[] ans = pointSortYOrder(ps1);
		
		System.out.println(tostring(ans));
		
	}
	public static void main4 (String[] s){
		int[] x = {1,1,1,1};
		int[] y = {1,2,2,2,2};
		ArrayList<Point> bag = toMultiset(x);
		multisetMerge(bag, y);
		System.out.println(tostring(bag));
		
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
	
	}


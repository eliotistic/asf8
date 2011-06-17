package hello;

import java.awt.Point;

public class Topology {
	Instrument instrument;
	Topos topos;
	Point[] stoppedPointOrder; // (string, pitchheight) arr, sorted up by pitchHeight
	int[] stoppedStringOrder; // x components (ie strings) of above
	
	// classify some fingering toplogies: this gets us some idea about 
	// difficulty and helps figure out transitions. 
	public enum Topos {
		OPEN, // every string open
		EASY, // 1 or 2 stops
		BAR, // going straight across
		LINEUP, // 3 or 4 up from low string (f1 is left)
		LINEDN, // 3 or 4 dn from lo, ie up from high string(f4 is left)
		 
		TRIANGLELO, // between strings, vertex is lowest position
		TRIANGLEBAR, // outer equal height, inner with a 2nd of these.
		BAD_TRIANGLELO,
		BAD_TRIANGLEBAR,
		BAD_DIAMOND,
		BAD_TRAPEZOID, // 4 stops, lower are on outer strings.
		UNKNOWN
	}
	public class ToposOption {
		Topos t;
		
	}
	
	
	
	Topology(){
		topos = Topos.UNKNOWN;
	}
	
	
	Topology(Instrument it, ChordFingering.Fingering f){
		instrument = it;
		topos = Topos.UNKNOWN;
		Point[] ps = f.getCoords(); // 
		//System.out.println("New fingering" + ps.toString());
		Point[] stops = Points.filterOpenStrings(ps);
		stoppedPointOrder = Points.pointSortYOrder(stops); 
		stoppedStringOrder = Points.PointXs(stoppedPointOrder);
		
		//System.out.println("Number of stops:" + stops.length);
		boolean easy = stops.length < 3;
		if (stops.length == 0) {
			//System.out.println("All open");
				topos = Topos.OPEN;
		} else if (stops.length == 1){
			    topos = Topos.EASY;
	
		} else if (checkBar(stops)){
				topos = Topos.BAR;
		} else if (checkLineUp(stops)){
				topos = Topos.LINEUP;
		} else if (checkLineDn(stops)){
				topos = Topos.LINEDN;
				
		} 
		/*
		else if (badTriangleLo(stops)){ // killer triangle
				topos = Topos.BAD_TRIANGLELO;
				
		} else if (isLoTriangle(stops)){
				topos = Topos.TRIANGLELO;
		}
		*/
		else {
			diamondCheck(stops);
			if (unknown()) {
				triangleLoCheck(stops);
				if (unknown()) {
					triangleBarCheck(stops);
				}
			}
		};
			
			//topos = Topos.UNKNOWN;		
		}	
	Topology(Point[] ps){
		topos = Topos.UNKNOWN;
		
		Point[] stops = Points.filterOpenStrings(ps);
		stoppedPointOrder = Points.pointSortYOrder(stops); 
		stoppedStringOrder = Points.PointXs(stoppedPointOrder);
		System.out.println("Number of stops:" + stops.length);
		boolean easy = stops.length < 3;
		if (stops.length == 0) {
			//System.out.println("All open");
				topos = Topos.OPEN;
		} else if (stops.length == 1){
			    topos = Topos.EASY;
	
		} else if (checkBar(stops)){
				topos = Topos.BAR;
		} else if (checkLineUp(stops)){
				topos = Topos.LINEUP;
		} else if (checkLineDn(stops)){
				topos = Topos.LINEDN;
				
		} 
		/*
		else if (badTriangleLo(stops)){ // killer triangle
				topos = Topos.BAD_TRIANGLELO;
				
		} else if (isLoTriangle(stops)){
				topos = Topos.TRIANGLELO;
		}
		*/
		else {
			diamondCheck(stops);
			if (unknown()) {
				triangleLoCheck(stops);
				if (unknown()) {
					triangleBarCheck(stops);
				}
			}
		} 
		}	
	
	
	public void print(){
		System.out.println("Toplogy: " + topos);
	}
	 public boolean playable() {
		//return topos != Topos.TRIANGLELO;
		 if (instrument.filterBadTopologies() && bad()) {
			 return false;
		 }
		 else return true;
	 }
	 public boolean unknown() {
		 return topos == Topos.UNKNOWN;
	 }
	 public boolean bad() {
			return (topos == Topos.BAD_TRIANGLELO 
					|| topos == Topos.BAD_TRIANGLEBAR
					|| topos == Topos.BAD_DIAMOND);
			 
		 }
	
	
	/*
	 * We are looking at fingerings that have a common hand position.

	 * Certain positions of the fingers -- their topology -- have to be
	 * eliminated. Clear case: triangle with low vertex, eg 5 1 5, where 5, 1 is
	 * interval from open string. However, triangle 2 1 2 is possible.
	 * 
	 * coords are (string, interval), where low string = 4.
	 * These are always ordered by string from lo to hi.
	 * 
	 */

	 /*
	static boolean check(Fing.Fingering f) {
		//String abc = f.getABCString();
		return checkTriangle(f.getCoords());
	}
	*/
	
	
	
	static boolean checkBar(Point[] ps){
		// all non-zero ints are equal,
		int last = ps[0].y;
		//System.out.println("checkBar: " + last);
		for (int i = 1; i<ps.length; i++){
			if (ps[i].y != last){
				return false;
			} 
		};
		return true;
	}
	static boolean checkLineDn(Point[] ps){
		// interval from left to right strictly descending.
		// no open strings; we have at least one stop.
		int last = ps[0].y;
		for (int i = 1; i<ps.length; i++){
			if (ps[i].y < last){
				last = ps[i].y;
			} else return false;
		}
		return true;
	}
	static boolean checkLineUp(Point[] ps){
		// interval from left to right strictly ascending.
		// no open strings; we have at least one stop.
		int last = ps[0].y;
		for (int i = 1; i<ps.length; i++){
			if (ps[i].y > last){
				last = ps[i].y;
			} else return false;
		}
		return true;
	}
	
	
	/*
	static boolean badTriangleLo(Point[] ps){
		// NB: we only have stopped strings in ps.
		//int len = ps.length;
		// String prin = Points.toString(ps);
		//System.out.println("Topology check: " + prin);
		switch (ps.length) {
		case 0: 
		case 1:	
		case 2:
			return false;
		case 3:
			return tricheck3(ps);
		case 4: 
			return tricheck4(ps);
		default:
			return false;
		}

	}
	
	Bb5 E C E 
	static boolean isLoTriangle (Point [] ps){
		if (ps.length == 3) {
			Point a = ps[0];
			Point b = ps[1];
			Point c = ps[2];
			if (b.y < a.y && b.y < c.y) {
				int d1 = a.y - b.y;
				int d2 = c.y - b.y;
				return (d1<3 && d2<3);
			} else return false;
			
		}
		else return false;
	}
	
	*/
	
	private String intArrString(int[] x ) {
		String s = "";
		for (int a: x) {
			s = s + a;
		};
		return "(" + s + ")";
	}
	
	
	private boolean intarrequals (int[] x, int[] y){
		// only using for diamondcheck -- don't care about null
		/// I completely fucking don't understand why equals does not work 
		if (x.length == y.length){
			for (int i = 0; i<x.length; i++){
				if (x[i] != y[i]) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static int[][] diamonds = 
	{ { 1, 3, 0, 2 }, 
	  { 1, 0, 3, 2 },
	  { 2, 3, 0, 1 }, 
	  { 2, 0, 3, 1 } };
	
	
	
	private void diamondCheck(Point[] ps) {
		//System.out.println("Diamond check: has " + ps.length);
		if (ps.length == 4) {
			for (int[] dia : diamonds) {
				if (intarrequals (dia, stoppedStringOrder)) {
					//System.out.println("IS FUCKING TRUE");
					topos = Topos.BAD_DIAMOND;
					return;
				}
			}
		}
	}
	
	private void triangleLoCheck (Point a, Point b, Point c){
		
		// b is lower than a + c; a + c are bigger than ma. 2.
		// returns true if killer triangle..
		//System.out.println("Triangle");
		//System.out.println("Triangle: check "  + a.y + " " + b.y + " " + c.y );
		if (b.y < a.y && b.y < c.y) {
			int d1 = Math.abs(a.y - b.y);
			int d2 = Math.abs(c.y - b.y); // 
			if (d1<3 || d2<3) {
				topos = Topos.TRIANGLELO;
			} else
			    topos = Topos.BAD_TRIANGLELO;
		}
	}
	
	// hi tri: a + c at same height, then b only within at most a major 2.
	 private void triangleBarCheck (Point a, Point b, Point c){
		
		if (a.y == c.y) {
			int d1 = Math.abs(a.y - b.y);
			
			if (d1 > 2) { 
				topos = Topos.BAD_TRIANGLEBAR;
			}
			else {
				topos =  Topos.TRIANGLEBAR;
			}
		}
	 }
	 
	 
	private void triangleBarCheck (Point[] ps){
		int len = ps.length;
		if (len == 3){
			triangleBarCheck(ps[0], ps[1], ps[2]);
		} else if (len == 4) {
			triangleBarCheck(ps[0], ps[1], ps[2]);
			if (topos == Topos.BAD_TRIANGLEBAR) return;
			triangleBarCheck(ps[0], ps[1], ps[3]);
			if (topos == Topos.BAD_TRIANGLEBAR) return;
			triangleBarCheck(ps[0], ps[2], ps[3]);
			if (topos == Topos.BAD_TRIANGLEBAR) return;
			triangleBarCheck(ps[1], ps[2], ps[3]);
			
		}
	}
	private void triangleLoCheck (Point[] ps){
		int len = ps.length;
		if (len == 3){
			triangleLoCheck(ps[0], ps[1], ps[2]);
		} else if (len == 4) {
			triangleLoCheck(ps[0], ps[1], ps[2]);
			if (topos == Topos.BAD_TRIANGLELO) return;
			triangleLoCheck(ps[0], ps[1], ps[3]);
			if (topos == Topos.BAD_TRIANGLELO) return;
			triangleLoCheck(ps[0], ps[2], ps[3]);
			if (topos == Topos.BAD_TRIANGLELO) return;
			triangleLoCheck(ps[1], ps[2], ps[3]);
			
		}
	}
	
	/*
	static boolean tricheck4(Point[] ps){
		Point a = ps[0];
		Point b = ps[1];
		Point c = ps[2];
		Point d = ps[3];
		
		return (  triangleLoCheck(a,b,c) 
				|| triangleLoCheck(a,b,d)
				|| triangleLoCheck(a,c,d)
				|| triangleLoCheck(b,c,d));
				
	}
		
	static boolean tricheck3(Point[] ps) {
		return triangleLoCheck(ps[0], ps[1], ps[2]);		
		}
	
	*/
	
	public static void main (String[] s){
		
		Point[] ps = { 
					new Point(3, 6), 
					new Point(2, 10), 
					new Point(1, 4),
					new Point(0, 7)
						};
		Topology t = new Topology(ps);
 		
		System.out.println(t.topos);
	}
	
	
	}



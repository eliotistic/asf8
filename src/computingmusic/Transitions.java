package computingmusic;

import java.awt.Point;
import java.util.ArrayList;

//import computingmusic.Fing.ChordFingering;

// given 2 fingerings, 
// figure out some distance metric that
// rates how hard the transition is. 

public  class Transitions {
	ChordFingering r1;
	ChordFingering r2;
	int sz1;
	int sz2;
	int combos; 

	public static Transitions transitions = new Transitions();

	public class FingeringPair {
		ChordFingering f0;
		ChordFingering f1;
		Point[] f0Coords; 
		Point[] f1Coords; 
		Point[] comCoords; // has notes common to f0 & f1
		/*
		FingeringPair(ChordFingering g0, ChordFingering g1) {
			Point[] f0c = g0.getCoords();
			Point[] f1c = g1.getCoords();
			
			f0 = g0;
			f1 = g1;
			f0Coords = f0c;
			f1Coords = f1c;
			comCoords = pointXsects(f0Coords, f1Coords);
		}
		*/
	}

	public Point[] pointXsects (Point[] xs, Point[] ys){
		ArrayList<Point> inters = new ArrayList<Point>();
		for (Point x : xs){
			for (Point y : ys){
				if (x.equals(y))
					inters.add(x);
			}
		};
		 Point[] r = new Point[inters.size()];
	        for (int i = 0; i< r.length; i++){
	            r[i] = inters.get(i);
	        };
	        return r;
	        
	}
	public boolean hasNextFingering() {
		return (r2.hasNextFingering() | r1.hasNextFingering());
	}

	public boolean hasPrevFingering() {
		return (r2.hasPrevFingering() | r1.hasPrevFingering());
	}
	/*

	public FingeringPair getNextFingering() {
		if (r2.hasNextFingering()) {
			return new FingeringPair(r1.getCurrFingering(), r2
					.getNextFingering());
		} else {
			r2.resetFingering();
			return new FingeringPair(r1.getNextFingering(), r2
					.getCurrFingering());
		}

	}

	public FingeringPair getPrevFingering() {
		if (r2.hasPrevFingering()) {
			return new FingeringPair(r1.getCurrFingering(), r2
					.getPrevFingering());
		} else {
			r2.resetFingering();
			return new FingeringPair(r1.getPrevFingering(), r2
					.getCurrFingering());
		}

	}
	public FingeringPair getCurrFingering(){
		return new FingeringPair(r1.getCurrFingering(), r2.getCurrFingering());
	}
	
	*/
	
	/*

	public static void makeTransitions(Fing.Result q1, Fing.Result q2) {
		//Transitions t = new Transitions();
		transitions.r1 = q1;
		transitions.r2 = q2;
		int s1 = q1.fingerings.size();
		int s2 = q2.fingerings.size();
		transitions.sz1 = s1;
		transitions.sz2 = s2;
		transitions.combos = s1 * s2;
		
	}
	*/
	
	
	
	
}

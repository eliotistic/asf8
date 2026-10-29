package computingmusic;

import java.util.ArrayList;

import computingmusic.ChordFingering.Fingering;
import computingmusic.ChordFingering.StringCases;
import computingmusic.ChordFingering.StringMatrix;
import computingmusic.ChordFingering.XCase;





public class ChordSolver {
	
	Instrument instrument;
	ChordFingering cf;
	ArrayList<Fingering> fingerings;
	
	public ChordSolver(Instrument inst, ChordFingering cf) {
		instrument = inst;
		this.cf = cf;
		fingerings = new ArrayList<Fingering>();
	}
	 
    /*
   
    private boolean pointArrEqual (Point[] x, Point[] y){
        if (x.length != y.length) return false;

        for (int i = 0 ; i< x.length; i++){
            if (!x[i].equals(y[i])){
                
                return false;
            }
        }
        return true;
    }
    private boolean coordsExist(Point[] c) {
      
        for (Fingering f :fingerings) {
            Point[] cc = f.getCoords();
          
            if (pointArrEqual(c,cc)) {
                return true;
            }
        }
        return false;
    }
   
    public void csaddNewFingering(Fingering f) {
    	 Point[] c = f.getCoords();
        if (!coordsExist(c)) // how can this happen?    
        	fingerings.add(f);
    }
    */
    
    /**
     * The XCases of one finished assignment (note k of the chord went to the k-th used string, strings ascending), or null when it is
     * refused: a note that cannot be stopped on its string (height out of range), or, for an instrument whose chord is a bow's
     * window, a stopped span the fixed hand cannot hold.  For an instrument whose hand is the model's (guitar) the span is the
     * trail's business (FingerTrail.frameFits), so only the strings' ranges are checked here.
     * Complexity: O(n).
     */
    private XCase[] acceptable(XCase[] x) {
    	if (!instrument.spec.chordOnNeighbouringStrings) {
    		return x;
    	}
    	int lo = Integer.MAX_VALUE;
    	int hi = -1;
    	for (XCase c : x) {
    		int ht = c.height;
    		if (ht != 0) {
    			lo = Math.min(lo, ht);
    			hi = Math.max(hi, ht);
    		}
    	}
    	// must be within correct span exclusive of open strings
    	if (lo == Integer.MAX_VALUE && hi == -1) { // single open string
    		return x;
    	}
    	return instrument.inSpan(lo, hi) ? x : null;
    }

    /**
     * Give each of the n notes a distinct string, strings taken in ascending order from `from`, a note for a string in ascending
     * order of the note's number: depth-first, a branch is cut as soon as a note cannot be stopped on its string (htOk).  With
     * `contiguous` every string from `from` on takes a note (a window of n neighbouring strings: this produces the permutations of
     * the old tables in the same lexicographic order); without, a string may be skipped (guitar), and of two notes of one pitch the
     * lower-numbered takes the lower string, so identical assignments are not repeated.
     * Complexity: O(number of partial assignments reached x n); at most S!/(S-n)! for S strings, but each note is playable on
     * only a few strings (at most 6 notes on 6 strings: well under a thousand).
     */
    private void search(StringMatrix stm, int from, boolean[] used, XCase[] chosen, int placed, boolean contiguous) {
    	int n = chosen.length;
    	if (placed == n) {
    		XCase[] ok = acceptable(chosen.clone());
    		if (ok != null) {
    			fingerings.add(cf.new Fingering(ok));
    		}
    		return;
    	}
    	int nStrings = instrument.nStrings;
    	if (nStrings - from < n - placed) {
    		return;
    	}
    	for (int s = from; s < nStrings; s++) {
    		StringCases sc = stm.arr[s];
    		for (int k = 0; k < n; k++) {
    			if (used[k] || !sc.xcasearr[k].htOk) {
    				continue;
    			}
    			if (!contiguous && k > 0 && !used[k - 1] && sc.xcasearr[k].pitch == sc.xcasearr[k - 1].pitch) {
    				continue;                      // the same pitch, lower number first
    			}
    			used[k] = true;
    			chosen[placed] = sc.xcasearr[k];
    			search(stm, s + 1, used, chosen, placed + 1, contiguous);
    			used[k] = false;
    		}
    		if (contiguous) {
    			return;                            // a window has no gaps: the next string is the only one to take
    		}
    	}
    }

    /**
     * Every way to put the chord's notes on distinct strings, each note on a string it can be stopped on.
     * Bowed instruments (chordOnNeighbouringStrings): the window of nNotes neighbouring strings slides over the strings and each
     * window is tried with every assignment of notes to its strings, as before.  Guitar: any strings, no window, no bow limit.
     * Complexity: bowed O(strings x nNotes! x nNotes); guitar O(S!/(S-n)! x n) at worst, see search.
     */
    public void solve(StringMatrix stm, int nNotes) {
    	boolean[] used = new boolean[nNotes];
    	XCase[] chosen = new XCase[nNotes];
    	if (instrument.spec.chordOnNeighbouringStrings) {
    		for (int i = 0; i <= (instrument.nStrings - nNotes); i++) {
    			search(stm, i, used, chosen, 0, true);
    		}
    	} else {
    		search(stm, 0, used, chosen, 0, false);
    	}
    }
}

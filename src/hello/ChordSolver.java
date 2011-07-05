package hello;

import java.util.ArrayList;

import hello.ChordFingering.Fingering;
import hello.ChordFingering.StringCases;
import hello.ChordFingering.XCase;
import hello.ChordFingering.StringMatrix;




public class ChordSolver {
	
	Instrument instrument;
	ChordFingering cf;
	ArrayList<Fingering> fingerings;
	
	public ChordSolver(Instrument inst, ChordFingering cf) {
		instrument = inst;
		this.cf = cf;
		fingerings = new ArrayList<Fingering>();
	}
	 
	public static final int[][] perm1 = {{0}};
	
    public static final int[][] perm2 = {{0, 1},
        {1, 0}
    };
    public static final int[][] perm3 = {{0, 1, 2},
        {0, 2, 1},
        {1, 0, 2},
        {1, 2, 0},
        {2, 0, 1},
        {2, 1, 0}
    };
    public static final int[][] perm4 = {{0, 1, 2, 3},
        {0, 1, 3, 2},
        {0, 2, 1, 3},
        {0, 2, 3, 1},
        {0, 3, 1, 2},
        {0, 3, 2, 1},
        {1, 0, 2, 3},
        {1, 0, 3, 2},
        {1, 2, 0, 3},
        {1, 2, 3, 0},
        {1, 3, 0, 2},
        {1, 3, 2, 0},
        {2, 0, 1, 3},
        {2, 0, 3, 1},
        {2, 1, 0, 3},
        {2, 1, 3, 0},
        {2, 3, 0, 1},
        {2, 3, 1, 0},
        {3, 0, 1, 2},
        {3, 0, 2, 1},
        {3, 1, 0, 2},
        {3, 1, 2, 0},
        {3, 2, 0, 1},
        {3, 2, 1, 0}
    };
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
    
    private int[][] permsofN(int n){
    	if (n==1) return perm1;
    	if (n==2) return perm2;
    	if (n==3) return perm3;
    	if (n==4) return perm4;
    	System.out.println("Error in perms of N -- N too big:" + n);
    	return null;
    }

    private XCase[] xcasesOfPerm(int[] perm, StringCases[] stc){
    	int len = perm.length;
    	int lo = Integer.MAX_VALUE;
    	int hi = -1;
    	XCase [] x = new XCase[len];
    	for (int i = 0; i<len;i++){
    		x[i] = stc[i].xcasearr[perm[i]];
    		// if this is bad reject whole.
    		if (!x[i].htOk) {
    			return null;
    		};
    		int ht = x[i].height;
    		
    		//System.out.println("xcasesOfPerm -- height:" + ht);
    		if (ht != 0) {
    			lo = Math.min(lo, ht);
    			hi = Math.max(hi, ht);
    		};
    	}
    	// must be within correct span exclkusive of open strings
    	if (lo==Integer.MAX_VALUE && hi ==-1){ // single open string
    		return x;
    	} else if (instrument.inSpan(lo, hi)){
    		//System.out.println("      --> in span:" + lo + " " + hi);
    		return x;
    	} else{
    		//System.out.println("Not in span: " + lo + " " + hi);
    		return null;
    	}
    
    }
    	
  
    private void solver(StringCases[] stc) {
    	int nStrings = stc.length;
    	int perms[][] = permsofN(nStrings);
    	
        for (int[] perm : perms) {
        	XCase [] xcasearr = xcasesOfPerm(perm, stc);
        	if (xcasearr != null) {
        		//System.out.println(sayXcases(xcasearr));
        		//Position p = new Position(0, 0);
                Fingering f = cf.new Fingering(xcasearr);
                //String a = f.answerString();
                //System.out.println("CS -- adding fingering:\n" + f);
                fingerings.add (f);
                    	//csaddNewFingering(f);
        	}
        }
    }
     
    public void solve(StringMatrix stm, int nNotes) {
       
    	for (int i = 0; i <= (4-nNotes); i++){
    		StringCases [] stc = new StringCases[nNotes];
    		
    		// make subarr
    		
    		for (int k = 0; k<nNotes; k++) {
    			stc[k] = stm.arr[i+k];
    		}
    		solver (stc);
    		// it seems our fingerings are orderd from hi-> lo, reverse that.
    		// 
    	
    		//Arrays.sort(fingerings);
    		
    	
    	}
    }
    	
    	
    	
 };


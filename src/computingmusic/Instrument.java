package computingmusic;


import java.awt.Point;

import computingmusic.utils.IntList;
import computingmusic.utils.StringFingeringConstants;

import abc.notation.Clef;


//import computingmusic.Fing.Instr;

public class Instrument {
	BigScore big;
	Instr instr;
	private int[] openStrings;
	public int[][][] caseArr;
	double[] spanVect;
	double[] r12Vect;
	public double ezSpan;
	public double maxSpan;
	
	
	
	//private int FBhandspan; // we apply to log distance on fingerboard in our own
							 // arbitrary measure
	@SuppressWarnings("unused")
	private boolean topologyFilter;
	public int nStrings = 4;
	
	public int stringRange = StringFingeringConstants.STRING_RANGE; // semitones on string (25 on May 9 2011)
	
	public double spanVectTop () {
		return spanVect[stringRange];
	}
	public double r12Top () {
		return spanVect[stringRange];
	}
	
	public double getR12 (int i){
		return spanVect[i];
	}
	
	Instrument(Instr ins) {
		makeSpanVect();
		instr = ins;
		//setCaseArr();
		topologyFilter = true;
		openStrings = makeOpenStrings(instr);
		//FBhandspan = initHandSpan();

	}
	Instrument (BigScore b, String instrumentName) { // s is name of instr
		makeSpanVect();
		big = b;
		instr = getInstr(instrumentName);
		//setCaseArr();
		openStrings = makeOpenStrings(instr);
		//FBhandspan = initHandSpan();
		
	}
	
	// used ony in Feedback
	Instrument (String s) {
		System.out.println("Warning, deprecated call to Instrument(String)");
		makeSpanVect();
		instr = getInstr(s);
		//setCaseArr();
		openStrings = makeOpenStrings(instr);
		//FBhandspan = violinHandSpan();
	}
	
	public int openPitch (int saite){
		return openStrings[saite];
	}
	
	// return -1 if outside our range?
	public int pitchHeight(int saite, int pitch) { 
		int i =  pitch - openPitch(saite);
        if (i >= 0 && i<=stringRange) {
        	return i;
        }
        else{
        	return -1;
        }
    }
	
	
	
	/*
	public static int[] aFreqs = {440, 466, 493, 523, 554, 587,622,
		  659, 698, 740, 784, 830,
		  880, 932, 987, 1046, 1108, 1174, 1244, 1318, 1396, 1480,
		  1568, 1661, 
		  1760};
	*/
	private static double root12thOf2 = 1.05946309;

	public static double exp12r2toThe (int openInterval){
		return Math.pow(root12thOf2, openInterval);
	}
	public double getSpan(int a){
		return spanVect[a];
	}
	public double getSpan(int a, int b){
		return Math.abs(spanVect[a] - spanVect[b]);
	}
	
	public double getSpan (Point p){
		return getSpan(p.x, p.y);
	}
	 
	private void makeSpanVect(){
		spanVect = new double[StringFingeringConstants.SPANNING_VECTOR_SIZE_IN_INSTRUMENT]; // which is 36 as of may 2011
		r12Vect = new double[StringFingeringConstants.SPANNING_VECTOR_SIZE_IN_INSTRUMENT];
		for (int i=0; i<StringFingeringConstants.SPANNING_VECTOR_SIZE_IN_INSTRUMENT; i++){
			r12Vect[i] = exp12r2toThe(i);
			spanVect[i] = 1/(exp12r2toThe(i)); // spanVect now has an inverse exponential scale
		}
		
		//maxSpan = getSpan(5,13); // finger 10th in Ysaye
		maxSpan = getSpan(1, 8);
		ezSpan = getSpan(1,7);
	}
	 // scaling this at 100, the Ysaye fingered 10th comes out
	 // to 27
	 // whereas a 4th in 1st pos is 26.
	 // cache this.
	 /*
	 public static int notePos(int openInterval) {
		 
		int FBlength = 100; // finhgerboard len, arbirary
		 int freq = aFreqs[openInterval];
		 double xx = FBlength - ((FBlength * 880) / (2 * freq));
		 return (int)(Math.round(xx));
	 }
	
	 public int getHandSpan (int p1, int p2) { // points are saite, interval
		 // we should do a thing with diagonals/across strings, but 
		 // this is fairly rough anyway.
		 //System.out.println("getHandSpan: " + p1 + " " + p2);
		 int y1 = notePos(p1);
		 int y2 = notePos(p2);
		 return  Math.abs(y1-y2);
	 }
	 */
	public double getHandSpan (Point p1, Point p2) { // points are saite, interval
		// we should do a thing with diagonals/across strings, but 
		// this is fairly rough anyway.
		return getSpan(p1.y, p2.y);	 
	}
	 
	 
	public boolean inSpan(int p1, int p2){	 
		return getSpan(p1, p2) <= maxSpan;
	}
	public boolean inEZSpan(int a, int b){
		 return getSpan(a, b) <= ezSpan;
	}
	 /*
	 public int violinHandSpan() { 
		 // this is finger 10th, with d on the A string, in Ysaye's "amitie",
		 // representing a rather big hand. 
		 int p1 = notePos(5)	;
		 int p2 = notePos(13);
		 return (p2-p1);
	 }	
	
	 
	 public int initHandSpan(){
		 return violinHandSpan();
	 }
	 
	  */
	 
	public boolean filterBadTopologies(){
		return big.filterBadToplogies;
	}
	
	/*
	public void toggleBadTopologies(){
		topologyFilter = !topologyFilter;
	}
	*/
	
	
	/*
	private void setCaseArr() {
		switch (instr) {
		case VN:
			caseArr = vn;
			break;
		case VA:
			caseArr = va;
			break;
		case VC:
			caseArr = vc;
			break;
		}
		;
	}
	*/
		
	public enum Instr {

        VN, VA, VC
    };
    public Clef getClef() {

		Clef c = null;
		switch (instr) {
		case VN:
			c = Clef.G;
			break;
		case VA:
			c = Clef.C;
			break;
		case VC:
			c = Clef.F;
			break;
		}
		;
		return c;
	}
	public Clef getClef(Point range) {

		Clef c = null;
		switch (instr) {
		case VN:
			c = Clef.G;
			break;
		case VA:
			// arbitrarily, lo >= 64 & hi 
			if (range.x > 59 & range.y > 78) c = Clef.G; 
			else c = Clef.C;
			break;
		case VC:
			if (range.x > 60 & range.y > 71) c = Clef.G; 
			else c = Clef.F;
			break;
		}
		;
		return c;
	}
	
   /*
	
    public int XXgetTop(){
    	int [][] hiString = caseArr[0];
    	int[] hiCase = hiString[hiString.length - 1];
    	return VpitchHi(hiCase);
    }
    
    public int XXgetBot(){
    	int [][] loString = caseArr[caseArr.length - 1];
    	int[] loCase = loString[0];
    	return VpitchLo(loCase);
    }
    
    */
    
    public int getTop() {
    	int hiString = openStrings()[nStrings-1];
    	return hiString + stringRange;
    }
    
    public int getBot(){
    	return openStrings()[0];
    }
    
    
    
    
    public Point getRange(){
    	return new Point (getBot(), getTop());
    }
    
    public String rangeString(){
    	String lo = Keynum.ofIntMidi(getBot());
    	String hi = Keynum.ofIntMidi(getTop());
    	String v = getInstName();
    	return "*" + v + ": " + lo + " - " + hi;
    }
    
    public  String getInstName() {
        switch (instr) {
            case VN:
                return "Violin";
            case VA:
                return "Viola";
            case VC:
                return "Cello";
        }
        throw new AssertionError("Unknown instrument: " + instr);
    }

    ;
    
    public Instr getInstr(String a) {
		if (a == null) {
			return Instr.VN;
		} else {
			a = a.toUpperCase();
			if (a.equals("VN") || a.equals("VIOLIN")) {
				return Instr.VN;
			} else if (a.equals("VA") || a.equals("VIOLA")) {
				return Instr.VA;
			} else if (a.equals("VC") || a.equals("CELLO")
					|| a.equals("VIOLINCELLO")) {
				return Instr.VC;
			} else {
				// default to violin
				return Instr.VN;
			}

		}
	}
    
    
    
    
    //public  final String[] vnStringNames = {"E", "A", "D", "G"};
    //public  final String[] vcStringNames = {"A", "D", "G", "C"};
    
    public final String[] vnStringNames = {"G", "D", "A", "E"};
    public final String[] vcStringNames = {"C", "G", "D", "A"};
    
    public final String string_name(int n) {
        if (instr == Instr.VN) {
            return vnStringNames[n];
        } else {
            return vcStringNames[n];
        }
    }
    public  String getInstShortName() {
        switch (instr) {
            case VN:
                return "VN";
            case VA:
                return "VA";
            case VC:
                return "VC";
        }
        throw new AssertionError("Unknown instrument: " + instr);
    }
    ;
    // revised so that strings are lo -> hi
    public  int[] makeOpenStrings(Instr instr){
    	// 0 = high string.
    	switch (instr) {
        case VN:
            int[] a = Constraints.VIOLIN_STRINGS; // TODO
            return a;
        case VA:
            int[] b = {48, 55, 62, 69};
            return b;
        case VC:
            int[] c = {36, 43, 50, 57};
            return c;
    }
    throw new AssertionError("Unknown instrument: " + instr);

}
  
    public int[] openStrings(){
    	return openStrings;
    }
    
    public  int openStringPitch(int str){
    	return openStrings()[str];
    }
    
    public  int pointPitch(Point p){
    	return (openStringPitch(p.x) + p.y);
    }
    
    public  String pointKey(Point p){
    	return Keynum.ofInt((openStringPitch(p.x) + p.y));
    }
    
    /*
    
    
    //accessors for case array
    public  final int VcaseNum(int[] a) {
        return a[0];
    }

    ;

    public  final int VpitchLo(int[] a) {
        return a[1];
    }

    ;

    public  final int VpitchHi(int[] a) {
        return a[2];
    }

    ;

    public  final int VposLo(int[] a) {
        return a[3];
    }

    ;

    public  final int VposHi(int[] a) {
        return a[4];
    }
    //Case[] case_arr (str) =
    public  final int[][][] vn = {
        // case_num, lo-note, hi-note, lo-pos hi-pos
        // vn e-string
        {{0, 76, 76, 0, 0},
            {1, 77, 77, 1, 1},
            {2, 77, 79, 1, 1},
            {3, 78, 80, 1, 2},
            {4, 80, 82, 1, 3},
            {5, 82, 84, 1, 4},
            {6, 83, 85, 2, 5},
            {7, 85, 87, 3, 6},
            {8, 87, 89, 4, 7},
            {9, 89, 91, 5, 8},
            {10, 90, 92, 6, 9},
            {11, 92, 94, 7, 10},
            {12, 94, 96, 8, 10},
            {13, 95, 97, 9, 10},
            {14, 97, 98, 10, 10},}, // went up to 98 -- extended to 101.
        //vn a-string
        {{0, 69, 69, 0, 0},
            {1, 70, 70, 1, 1},
            {2, 70, 72, 1, 1},
            {3, 71, 73, 1, 2},
            {4, 73, 75, 1, 3},
            {5, 75, 77, 1, 4},
            {6, 76, 78, 2, 5},
            {7, 78, 80, 3, 6},
            {8, 80, 82, 4, 7},
            {9, 82, 84, 5, 8},
            {10, 83, 85, 6, 9},
            {11, 85, 87, 7, 10},
            {12, 87, 89, 8, 10},
            {13, 88, 90, 9, 10},
            {14, 90, 91, 10, 10},},
        //vn d-string
        {{0, 62, 62, 0, 0},
            {1, 63, 63, 1, 1},
            {2, 63, 65, 1, 1},
            {3, 64, 66, 1, 2},
            {4, 66, 68, 1, 3},
            {5, 68, 70, 1, 4},
            {6, 69, 71, 2, 5},
            {7, 71, 73, 3, 6},
            {8, 73, 75, 4, 7},
            {9, 75, 77, 5, 8},
            {10, 76, 78, 6, 9},
            {11, 78, 80, 7, 10},
            {12, 80, 82, 8, 10},
            {13, 81, 83, 9, 10},
            {14, 83, 84, 10, 10}
        },
        // vn g-string
        {{0, 55, 55, 0, 0},
            {1, 56, 56, 1, 1},
            {2, 56, 58, 1, 1},
            {3, 57, 59, 1, 2},
            {4, 59, 61, 1, 3},
            {5, 61, 63, 1, 4},
            {6, 62, 64, 2, 5},
            {7, 64, 67, 3, 6}, // lo pos was 66
            {8, 66, 68, 4, 7},
            {9, 68, 70, 5, 8},
            {10, 69, 71, 6, 9},
            {11, 71, 73, 7, 10},
            {12, 73, 75, 8, 10},
            {13, 74, 76, 9, 10},
            {14, 76, 77, 10, 10}
        }
    };
    // viola 
    public  final int[][][] va = {
        // case_num, lo-note, hi-note, lo-pos hi-pos
        // va a-string
        {
            {0, 69, 69, 0, 0},
            {1, 70, 70, 1, 1},
            {2, 70, 72, 1, 1},
            {3, 71, 73, 1, 2},
            {4, 73, 75, 1, 3},
            {5, 75, 77, 1, 4},
            {6, 76, 78, 2, 5},
            {7, 78, 80, 3, 6},
            {8, 80, 82, 4, 7},
            {9, 82, 84, 5, 8},
            {10, 83, 85, 6, 9},
            {11, 85, 87, 7, 10},
            {12, 87, 89, 8, 10},
            {13, 88, 89, 9, 10}
        },
        // va d-string
        {{0, 62, 62, 0, 0},
            {1, 63, 63, 1, 1},
            {2, 63, 65, 1, 1},
            {3, 64, 66, 1, 2},
            {4, 66, 68, 1, 3},
            {5, 68, 70, 1, 4},
            {6, 69, 71, 2, 5},
            {7, 71, 73, 3, 6},
            {8, 73, 75, 4, 7},
            {9, 75, 77, 5, 8},
            {10, 76, 78, 6, 9},
            {11, 78, 80, 7, 10},
            {12, 80, 82, 8, 10},
            {13, 81, 82, 9, 10}
        },
        //va g-string
        {{0, 55, 55, 0, 0},
            {1, 56, 56, 1, 1},
            {2, 56, 58, 1, 1},
            {3, 57, 59, 1, 2},
            {4, 59, 61, 1, 3},
            {5, 61, 63, 1, 4},
            {6, 62, 64, 2, 5},
            {7, 64, 66, 3, 6},
            {8, 66, 68, 4, 7},
            {9, 68, 70, 5, 8},
            {10, 69, 71, 6, 9},
            {11, 71, 73, 7, 10},
            {12, 73, 75, 8, 10},
            {13, 74, 75, 9, 10},},
        //va c string
        {{0, 48, 48, 0, 0},
            {1, 49, 49, 1, 1},
            {2, 49, 51, 1, 1},
            {3, 50, 52, 1, 2},
            {4, 52, 54, 1, 3},
            {5, 54, 56, 1, 4},
            {6, 55, 57, 2, 5},
            {7, 57, 59, 3, 6},
            {8, 59, 61, 4, 7},
            {9, 61, 63, 5, 8},
            {10, 62, 64, 6, 9},
            {11, 64, 66, 7, 10},
            {12, 66, 68, 8, 10},
            {13, 67, 68, 9, 10}
        }
    };
    // cello 
    public  final int[][][] vc = {
        // case_num, lo-note, hi-note, lo-pos hi-pos
        // vc a-string
    	{{0, 57, 57, 0, 0},
    		{1, 58, 59, 1, 1},	
            {2, 59, 60, 1, 2},
            {3, 60, 61, 1, 3},
            {4, 61, 62, 1, 4},
            {5, 62, 63, 1, 5},
            {6, 63, 64, 2, 6},
            {7, 64, 65, 3, 7},
            {8, 65, 66, 4, 8},
            {9, 66, 67, 5, 9},
            {10, 67, 68, 6, 10},
            {11, 68, 69, 7, 11},
            {12, 69, 70, 8, 12},
            {13, 70, 71, 9, 13},
            {14, 71, 72, 10, 14},
            {15, 72, 73, 11, 15},
            {16, 73, 74, 12, 16},
            {17, 74, 75, 13, 17},
            {18, 75, 76, 14, 18},
            {19, 76, 77, 15, 19},
            {20, 77, 78, 16, 20},
            {21, 78, 79, 17, 21},
            {22, 79, 80, 18, 22},
            {23, 80, 81, 19, 23},
          //  {24, 81, 82, 20, 24} // ... we only have 2 octave fingerboard.
        },
        {{0, 50, 50, 0, 0},
            {1, 51, 52, 1, 1},
            {2, 52, 53, 1, 2},
            {3, 53, 54, 1, 3},
            {4, 54, 55, 1, 4},
            {5, 55, 56, 1, 5},
            {6, 56, 57, 2, 6},
            {7, 57, 58, 3, 7},
            {8, 58, 59, 4, 8},
            {9, 59, 60, 5, 9},
            {10, 60, 61, 6, 10},
            {11, 61, 62, 7, 11},
            {12, 62, 63, 8, 12},
            {13, 63, 64, 9, 13},
            {14, 64, 65, 10, 14},
            {15, 65, 66, 11, 15},
            {16, 66, 67, 12, 16},
            {17, 67, 68, 13, 17},
            {18, 68, 69, 14, 18},
            {19, 69, 70, 15, 19},
            {20, 70, 71, 16, 20},
            {21, 71, 72, 17, 21},
            {22, 72, 73, 18, 22},
            {23, 73, 74, 19, 23},
        //    {24, 74, 75, 20, 24}
        },
        // vc g-string
        {{0, 43, 43, 0, 0},
            {1, 44, 45, 1, 1},
            {2, 45, 46, 1, 2},
            {3, 46, 47, 1, 3},
            {4, 47, 48, 1, 4},
            {5, 48, 49, 1, 5},
            {6, 49, 50, 2, 6},
            {7, 50, 51, 3, 7},
            {8, 51, 52, 4, 8},
            {9, 52, 53, 5, 9},
            {10, 53, 54, 6, 10},
            {11, 54, 55, 7, 11},
            {12, 55, 56, 8, 12},
            {13, 56, 57, 9, 13},
            {14, 57, 58, 10, 14},
            {15, 58, 59, 11, 15},
            {16, 59, 60, 12, 16},
            {17, 60, 61, 13, 17},
            {18, 61, 62, 14, 18},
            {19, 62, 63, 15, 19},
            {20, 63, 64, 16, 20},
            {21, 64, 65, 17, 21},
            {22, 65, 66, 18, 22},
            {23, 66, 67, 19, 23},
           //  {24, 67, 68, 20, 24}
        },
        // vc c-string
        {{0, 36, 36, 0, 0},
            {1, 37, 38, 1, 1},
            {2, 38, 39, 1, 2},
            {3, 39, 40, 1, 3},
            {4, 40, 41, 1, 4},
            {5, 41, 42, 1, 5},
            {6, 42, 43, 2, 6},
            {7, 43, 44, 3, 7},
            {8, 44, 45, 4, 8},
            {9, 45, 46, 5, 9},
            {10, 46, 47, 6, 10},
            {11, 47, 48, 7, 11},
            {12, 48, 49, 8, 12},
            {13, 49, 50, 9, 13},
            {14, 50, 51, 10, 14},
            {15, 51, 52, 11, 15},
            {16, 52, 53, 12, 16},
            {17, 53, 54, 13, 17},
            {18, 54, 55, 14, 18},
            {19, 55, 56, 15, 19},
            {20, 56, 57, 16, 20},
            {21, 57, 58, 17, 21},
            {22, 58, 59, 18, 22},
            {23, 59, 60, 19, 23},
          //  {24, 60, 61, 20, 24}
        }
    };
    */
    
    /**
     * For the specific instrument, vertically from y0 to y1, find the locations at which to draw the discs. 
     * For example, for the violin, interval goes from 1 to 25 (25 was the case for the violin on may 4th 2011, may have changed)
     */
    public int noteYPosition (int interval, int y0, int y1) {
    	//System.out.println("
    	
    	double fractionOfInterval = (getR12(interval) - 1) / (getR12(stringRange) -1); // division of negative numbers
    	//System.out.println("r12(0)" + (getR12(24)));
    	//System.out.println(fractionOfInterval);
    	int a =  y0 + (int)(Math.round(fractionOfInterval*(y1-y0)));
    	//System.out.println("fint: " + finterval+ " int: " + interval  + " pos: " + a);
    	return a;	
    }
    /**
     * 
     * @param intervals
     * @param y0
     * @param y1
     * @return IntList of noteYpositions between y- + y1 of all ints in intervals.
     */
    public IntList intervalYPositions (IntList intervals, int y0, int y1) {
    	IntList positions = new IntList();
    	for (int i : intervals) {
    		positions.add(noteYPosition(i, y0, y1));
    	}
    	return positions;
    }
     
    public static void main (String[] sss) {
    	Instrument ins = new Instrument("VN");
    	for (int i = 0 ; i<=24; i++) {
    		int n = ins.noteYPosition(i, 0, 24); // TODO maybe stringrange(25)-1?
    	
    		System.out.println("i: " + i + " n: " + n);
    	}
    }
    
    public static void main1 (String[] sss) {
    	Instrument ins = new Instrument("VN");
    	double s1 =  ins.getSpan(0,7);
    	double s2 =  ins.getSpan(1,8);
    	double s3 =  ins.getSpan(2,9);
    	double s4 =  ins.getSpan(3,10);
    	System.out.println(ins.maxSpan +  "  " + s1 
    			+ " " + s2
    			+ " " + s3
    			+ " " + s4
    	);
    }
    
}

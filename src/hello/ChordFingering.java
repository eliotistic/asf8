package hello;

import hello.Instrument.Instr;
import hello.Keynum.K;
import hello.utils.IntList;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;



public class ChordFingering {
	public  Instrument instrument;
	public String msg;
	
	private boolean hidden;
	
	private boolean isArp; // this is a composite,  assembled in fingerseq2.  
	private int[] monoInputContour; // turned out to be mistake
	private IntList[] polyInputContour; // series of pitches happeneding now (particularly if we're an arp).
	
	private int subindex; // for viewing, which Fingering we're looking at
	public int index;
	
	private int glueindex;
	
	
	public K[] ks;
	public int[] inputPitchSequence;
	//public  ArrayList<Fingering> ans = new ArrayList<Fingering>();
	private  ArrayList<Fingering> fingerings;
	
	
	//public  Result fingeringResult = new Result();
	//public int[][][] caseArr;	
	public boolean filterBadTopologies;
	private ChordSolver csolver;
	
	
	
	 public boolean isHidden (){
     	return hidden;
     }
     
     public void hide(){
     	hidden = true;
     }
     
     public void unhide(){
     	hidden = false;
     }
	
	public ChordFingering(){}
	
	@SuppressWarnings("unchecked")
	public ChordFingering(int ind, Instrument ins, K[] ks) {
		msg = "orginal from factory";
		this.ks = ks;
		isArp = false;
		index = ind;
		subindex = 0;
		glueindex = -1; // fixing just one of the possibilities.
		instrument = ins;
		//System.out.println("Ins = " + instrument);
		
		//caseArr = ins.caseArr;
		
		//System.out.println("Case arr null? = " + (caseArr == null ));
		//Result r = new Result(p);
		//r.index = ind;
		fingerings = new ArrayList<Fingering>();
		
		StringMatrix a = new StringMatrix(ks);
		
		// old solver, which used cases
		// a.solve();
		
		csolver = new ChordSolver(instrument, this);
		csolver.solve(a, ks.length);
		
		fingerings = csolver.fingerings;
		Collections.sort(fingerings);

		for (Fingering f : fingerings) {
			f.index = index;
		}
		
		//System.out.println("size of array is: " + ks.length);
		//addPitchHeight();
		//addAns(ans);
		//System.out.println("Arraylist size is: " + fingerings.size());
	}
	
	public ChordFingering(int ind, Instrument it, String keynumString){
		 this(ind, it, Keynum.parse(keynumString).ks);
		
	}
	
	public ChordFingering copy () {
		ChordFingering c = new ChordFingering();
		c.msg = "this is a copy!" ;
			
		c.isArp = isArp;
		c.index = index;
		c.subindex = index;
		c.glueindex = glueindex;
		c.ks = ks;
		c.instrument = instrument;
		//c.caseArr = caseArr;
		c.fingerings = fingerings;
		return c;
	}
	
	@Override
	public String toString(){
		String s = "Chord Fingerings: " + fingerings.size() + "\n";
		for (Fingering f : fingerings) {
			s = s + f;
		}
		return s;
	}
	public String abcString(){
		String s = " ";
		for (Keynum.K k : ks) {
			String aaa = k.toAbc();
			// System.out.println("getABCString: " + aaa);
			s = s + aaa + " ";
		
		}

		return "[" + s + "]";

	}
	
	public void addInputContour(int[] contour){
		monoInputContour = contour;
	}
	public void addInputContour(IntList[] contour){
		polyInputContour = contour;
	}
	
	public boolean isArp(){
		return isArp;
	}
	public void setArp(){
		isArp = true;
	}
	
	public boolean isGlued(){
		return glueindex != -1;
	}
	public void setGlue(){
		// System.out.println("    GLUE: setting index " + index + " to " + subindex );
		glueindex = subindex;
	}
	
	public void setArpGlue(){
		// this is an arpeggiated CF -- we need to set the glue of all paremt
		// constituents. 
	}
	public void unGlue (){
		subindex = glueindex;
		glueindex = -1;
		
	}
	
	// need for fingerseq2
	public ArrayList<Point> emitGluePoints(){
		ArrayList<Point> ans = new ArrayList<Point>();
		addGluePoints(ans);
		return ans;
	}
	
	public void addGluePoints(ArrayList<Point> ps) {
		if (isGlued()){
			Fingering f = fingerings.get(glueindex);
			Point[] pp = f.getCoords();
			for (Point p : pp) ps.add(p);
		}
	}
	
	public void glueFingering(Fingering f) {
		// eq to something here.
		for (int i = 0; i<fingerings.size(); i++) {
			if (fingerings.get(i) == f) {
				glueindex = i;
				System.out.println("Index = "+ index + " gluedfingering!");
				System.out.println("        --> " + f.pointsString());
				
				return;
			}
		}
	}
	
	private boolean pointMember (Point p, Point[] a){
		boolean ans = false;
		for (Point p0 : a) {
			if (p0.equals(p)) {
				ans = true;
				break;
			}
		} 
		return ans;
	}
	/*
	private boolean pointMember (Point p, ArrayList<Point> a){
		boolean ans = false;
		for (Point p0 : a) {
			if (p0.equals(p)) {
				ans = true;
				break;
			}
		} 
		return ans;
	}
	*/
	
	// only hides stuff
	public void reducetoGlue(ArrayList<Point> gluedPoints) {
		// THIS is a chord that we made using f.
	// every fingering has to have all glue points.
	
		
			for (Fingering f0 : getFingerings()) {
				Point[] f0Points = f0.getCoords();
				// hide if our glue is not present.
				for (Point g : gluedPoints) {
					if (!pointMember(g, f0Points)) {
						f0.hide();
						break;
					}
				}

			}

		}

	
	public void reducetoGlue2(Point[] gluedPoints) {
		String p = Points.tostring(gluedPoints);
		System.out.println("Reduce to glue: " + p);
		// THIS is one of te input chordfingerings
		// 
		// every fingering must be exclusively made of some glue points.
		// this should only succeed once
			boolean found = false;
			for (Fingering f0 : getFingerings()) {
				Point[] f0Points = f0.getCoords();
				// hide if our glue is not present.
				found = true;
				for (Point f : f0Points) {
					if (!pointMember(f, gluedPoints)) {
						found = false;
						break;
					}
				}
				if (found) {
					// glue at this point.
					glueFingering(f0);
					return;
				}
			}

	}

	
	
	
	public ArrayList<Fingering> getFingerings (){
		// We might be a restricted pos thing with
		// just one item ... if glued, our glue index is now irrelevant,
		// but on the other hand we don't need glue for just one element.
		
		// some fingerings could be hidden -- by convention, we will 
		// never hide all.
		
		// we kludge here, because I'm too reliant on state to benefit from copy
		// and this will have to be redone.
		
		int sz = fingerings.size();
		
		if (sz == 1) {
			return fingerings; // forced
		} else if (isGlued()) { // 
			ArrayList<Fingering> f = new ArrayList<Fingering>();
			f.add(fingerings.get(glueindex));
			return f;
		} else {
			return getUnhidden();
		}
	}
	
	public int countHidden (){
		int c = 0;
		for (Fingering f : fingerings) {
			if (f.isHidden()) c++;
		}
		return c;
	}
	public int countunHidden (){
		int c = 0;
		for (Fingering f : fingerings) {
			if (!f.isHidden()) c++;
		}
		return c;
	}
	
	public void hideBadTopologies() {
		for (Fingering f : fingerings){
			if (f.badToplogy()){
				f.hide();
			}
		}
	}
	
	public void hideBadContours(){
		// inputContour -- list of pitches in musical input with 0 when chord.
		// Fingering.pitchsequence -- pitches we get if we play this from low to hi.
		// input: remove dupes
		// pitchseq: accept if some subsequence of input = the whole of pitchseq.
		for (Fingering f : fingerings) {
			ArpContour cont = new ArpContour(f, polyInputContour);
			if (cont.wrong()){
				f.hide();
			}
		}
	}


	private ArrayList<Fingering> getUnhidden (){
		ArrayList<Fingering> unhidden = new ArrayList<Fingering>();
		
		for (Fingering f : fingerings) {
			if (!f.isHidden()) {
			unhidden.add(f);	
			}
		}
		return unhidden;
	}
	
	public int nFingerings() {
		if (isGlued()) {
			return 1;
		} else {
        //return fingerings.size();
			return countunHidden();
		}
    }
    	
	public String noteStringforChordInput () {
    	String s = "";
    	for (Keynum.K k : ks) {
    		s = s + k.name + " ";
    } ;
    	return s;
    }
	 
	
	 
	 
	 public boolean impossible() {
      	return (nFingerings() == 0 );
      }
	 
	 public int[] kNums () {
      	int n = nKs();
      	if (n == 0){
      		return null;
      	} else {
      		int[] m = new int[n];
      		//Keynum.K[] ks = getKs();
      		for (int i=0; i<n;i++){
      			m[i] = ks[i].pitch;
      		};
      		return m;
      		
      	}
      }
      
      public int nKs (){
      	 return ks.length;
      }
      
      public void filterMarked () {
      	ArrayList<Fingering> ff = new ArrayList<Fingering>();
      	for (Fingering f : getFingerings()) {
      		if (!f.isMarkedforRemoval()){
      			ff.add(f);
      		} 
      	};
      	fingerings = ff;
      }
      
      public int countBadToplogies(){
      	int i = 0;
      	// System.out.println("in countBadTopologies: nFingerings = " + nFingerings());
      	for (Fingering f : getFingerings()){
      		if (f.badToplogy()) {
      			i++;
      		}
      	}
      	return i;
      }
      public boolean noFingering (){
      	//return fingerings.size() == 0;
    	  return (nFingerings() == 0);
      }
      public boolean hasNextFingering() {
          return !isGlued() && nFingerings() > subindex + 1;

      }

      public boolean hasPrevFingering() {
    	  
          return !isGlued() && subindex > 0;
      }

      public Fingering getNextFingering() {
          subindex++;
          return getFingerings().get(subindex);
      }

      public Fingering getPrevFingering() {
          subindex--;
          return getFingerings().get(subindex);
      }
      public Fingering getCurrFingering(){
          return getFingerings().get(subindex);
      }

      /*
	 public  String answerString() {
	        String s = "";
	        String inst = instrument.getInstName();
	        s = s + "-------------------\nInstrument: " + inst + "\nNotes: ";

	        for (Keynum.K k : ks) {
	            s = s + k.name + " ";
	        }
	        ;
	        
	        int nSol = nFingerings();
	        //statusLabel.setText("Solutions: " + nSol);
	        if (nSol == 0) {
	            s = s + "\nNo solutions\n";
	        } else {
	            for (Fingering f : getFingerings()) {
	                //int stops = f.countStops();
	                // if (verboseOutput) s = s + "\nStops: " + stops;

	                //FText a = f.ftexts();
	                //String[] a = f.texts();
	                // texts[0] has the positions string.
	                if (verboseOutput) {
	                    s = s + "\nPitch Height: " + a.pitchHeight;
	                }
	                s = s + "\nPositions: " + a.pos;
	                for (String ss : a.fingerings) {
	                    //String ss = a[i];
	                    s = s + "\n" + ss;
	                }
	                ;
	                s = s + "\n";
	            }
	            ;
	        }
	        return s;
	    }
	*/
	 
	/*
	public void setVn(){
		localInstrument = new Instrument("VN");
		//caseArr = localInstrument.caseArr;
	}
	*/
	/*
    public static String intArrString(int[] a) {
        String s = "(";
        for (int i : a) {
            s += i + " ";
        }
        ;
        return (s + ")");
    }
    */
    /*
    public enum Instr {

        VN, VA, VC
    };
    */
    
   
    
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

    public static final int VcaseNum(int[] a) {
        return a[0];
    };

    public static final int VpitchLo(int[] a) {
        return a[1];
    };

    public static final int VpitchHi(int[] a) {
        return a[2];
    }

    ;

    public static final int VposLo(int[] a) {
        return a[3];
    }

    ;

    public static final int VposHi(int[] a) {
        return a[4];
    }

    ;
    
    
    // container for answer
   
   
    /*
    public static void addPitchHeight() {
        for (Fingering f : ans) {
            f.addPitchHeight();
        }

    }

    ;
*/
    class StringNotes {
        // each obj has n notes
    };
    
    

    @SuppressWarnings("rawtypes")
	public class XCase implements Comparable {
        // make case that combines all positions.
        //  -1 in pos. means no pos.
        //Instr instrument;
        //int case_num;

        int caseLo;
        int caseHi;
        int saite;
        // int num;
        int pitch;
        int height; // interval from open
        String noteName;
        String abc;
        // int note_lo;
        // int note_hi;
        int pos_lo;
        int pos_hi;
        boolean ok; // is playable
        boolean htOk; // replaces ok
        boolean isOpen;
        Keynum.K k;
        
        
       
       
        boolean has_note(int[] c, int note) {
            return (VpitchLo(c) <= note & VpitchHi(c) >= note);
        };

        
        // interval between open string and this pitch.

        // deprecated
        /*
        public int pitchHeight() {
            int openPitch = VpitchLo(caseArr[saite][0]);
            
            return pitch - openPitch;
        }

         */	

       
        XCase(int stringNum, Keynum.K kk) {

            int lo = -1;
            int hi = -1;
            //int n = kk.pitch;
            k = kk;
            noteName = kk.name;
            saite = stringNum;
            pitch = kk.pitch;
            
            // interval from open string
            height = instrument.pitchHeight(saite, pitch);
            
            abc = kk.toAbc();
            /*
            int[][] str = caseArr[stringNum];
            for (int[] c : str) {
                int poslo = VposLo(c);
                int poshi = VposHi(c);
                int cN = VcaseNum(c);
                
                if (has_note(c, pitch)) {
                    if (caseLo == 0) { // unintialized?
                        caseLo = caseHi = cN;
                    } else {
                        caseLo = Math.min(caseLo, cN);
                        caseHi = Math.max(caseHi, cN);
                    }

                    if (lo == -1) {
                        lo = poslo;
                    } else {
                        lo = Math.min(poslo, lo);
                    }
                    hi = Math.max(poshi, hi);

                }
                ;
            }
            ;
            */
            // pitch = n;
            pos_lo = lo;
            pos_hi = hi;
            ok = (pos_lo != -1 && height != -1);
            htOk = height != -1;
            //isOpen = (pos_lo == 0);
            isOpen = height == 0;
        };
       
        
        public Point stringHeightPoint() {
            //int height = this.pitchHeight();
            return new Point(saite, height);

        }
        /**
         * 
         * @return point of saite, pitch
         */
        public Point stringPitchPoint() {   
           return new Point(saite, pitch);
        }


        public String pointString(){
            Point p = stringHeightPoint();
            return (p.x + "," + p.y);
        }

        void commonPos(Position p) {
            // pos is a range lo, hi, expressed as duple. It is bad
            // when lo > hi.  if we have a bad position, do nothing.
            if (p.isBad() || this.isOpen) {
            } // use whatever is in position
            else {
                int xlo = this.pos_lo;
                int xhi = this.pos_hi;
                int plo = p.lo;
                int phi = p.hi;

                // if p is open, then use xlo, xhi.

                if (p.isOpen()) {
                    p.lo = xlo;
                    p.hi = xhi;
                } else {
                    int maxlo = Math.max(xlo, plo);
                    int minhi = Math.min(xhi, phi);
                    p.lo = maxlo;
                    p.hi = minhi;
                }
                ;
            }
            ;
        }

        ;
        @Override
        public String toString() {
            String nam = instrument.string_name(saite);
            return ("hidden? " + hidden + "  note: " + pitch + " string: " + nam
            		+ " stringNum: " + saite
            		
            	
            		+ " height: " + height
                    + " pos_lo: " + pos_lo + " pos_hi: " + pos_hi
                    + " ok: " + ok + " isOpen: " + isOpen);
        }

      
        String toshortstring() {
            String nam = instrument.string_name(saite);

            String o = "";
            if (isOpen) {
                o = "Open";
            }
            String cs = "";
            // if (verboseOutput)  cs = " cases: (" + caseLo + ", " + caseHi +")";

            return ("String: " + nam + " note: " + noteName + " " + o
                    + cs);
        }

        /*
        void print() {
            System.out.println(this);
        }
        */
        
        /*
        // method to sort by pitch
        @Override
		public int compareTo(Object x2) {
			
        	int n2 = ((XCase)x2).pitch;
        	if (pitch > n2) 
        		return 1; 
        	else if (pitch < n2)
        		return -1;
        	else
        	return 0;
		}

        ;
        */
        // method to sort by string
        @Override
		public int compareTo(Object x2) {
        	int n2 = ((XCase)x2).saite;
        	if (saite > n2) 
        		return -1; 
        	else if (saite < n2)
        		return 1;
        	else
        	return 0;
		}

        ;
    }
    // one string, with all notes represented as array of XCases.

    public class StringCases {

        int saite;
        XCase[] xcasearr;
        //PointList chordColumn;

        /*
        StringCases(int str, ArrayList<Keynum.K> klist) {
            saite = str;
            int len = klist.size();
            xcasearr = new XCase[len];
            chordColumn = new PointList();
            for (int n = 0; n < len; n++) // n is note ind.
            {
                xcasearr[n] = new XCase(str, klist.get(n));
            }
        }

        ;
         */
        
        StringCases(int str, Keynum.K[] ks) {
            saite = str;
            int len = ks.length;
            xcasearr = new XCase[len];
            //chordColumn = new PointList();
            for (int n = 0; n < len; n++) // n is note ind.
            {
                xcasearr[n] = new XCase(str, ks[n]);
                //int p = ks[n].pitch;
                //int ht = instrument.pitchHeight(saite, p);
                // returns -1 if out of range.
                
                //chordColumn.add(new Point(saite,ht));
                
            }
            
        }
        
        /*
        void print() {
            System.out.println("String: " + saite);
            for (XCase c : xcasearr) {
                c.print();
            }
        }
        public String logString (){
            return "BAZ";
        }

        ;
        */
        // 1 note solver
        //BABY
        /*
        public void solver() {
          
                XCase x1 = this.xcasearr[0];
                //System.out.println("ONE NOTE Case arr[0] is " + x1.tostring());
                if (!x1.ok) {
                } else {
                	XCase[] xxx = {x1};
                	
                	// single note positions
                	//Position d = new Position(0,0);
                         // FIX 
                	Position d = new Position (x1.pos_lo, x1.pos_hi);
                	Fingering f = new Fingering(xxx, d);
                	f.addNewFingering();
                	
                    }
                
        }
           

        public void solver(StringCases that) {
            //

            //List< Fingering > c = new ArrayList< Fingering >();

            // x & y are two adjacent strings.
            // each string has two cases, representing each of the two notes
           
        	for (int[] perm : perm2) {
                int a = perm[0];
                int b = perm[1];
                // StringCases c1 = x.arr[a];
                XCase x1 = this.xcasearr[a];
                XCase x2 = that.xcasearr[b];

                // x1, x2 must both be ok, ie, the note can be played on their strings.

                if (!x1.ok || !x2.ok) {
                } else {

                    Position d = new Position(0, 0);
                    x1.commonPos(d);
                    x2.commonPos(d);

                    // they must have a common position.

                    if (d.isBad()) {
                    } else // (x1.ok && x2.ok)
                    {
                        XCase[] xcases = {x1, x2};

                        //ans.add(new Fingering(xxx, d));
                        
                         Fingering f = new Fingering(xcases, d);
                         Topology t = new Topology (instrument, f); // of f
                         if (true) // (t.playable())
                         	{
                         	f.topos = t;
                         	f.addNewFingering();
                         	//f.addnew(ans);
                       
                    }
                    }
                }
            }
        }
                    
                   
                
      

        public void solver(StringCases x, StringCases y) {

            // this, x & y are three adjacent strings.
            // each string has three cases, representing each of the three notes
            for (int[] perm : perm3) {
                int a = perm[0];
                int b = perm[1];
                int c = perm[2];
                // StringCases c1 = x.arr[a];
                XCase x1 = this.xcasearr[a];
                XCase x2 = x.xcasearr[b];
                XCase x3 = y.xcasearr[c];

                // all 3 notes must be available
                if (!x1.ok || !x2.ok || !x3.ok) {
                } else {

                    Position p = new Position(0, 0);
                    x1.commonPos(p);
                    x2.commonPos(p);
                    x3.commonPos(p);

                    // must have common position
                    if (!p.isBad()) {
                        XCase[] xcases = {x1, x2, x3};
                        Fingering f = new Fingering(xcases, p);
                        //Topology t = new Topology (instrument, f); // of f
                        if (true) // (t.playable())
                        	{
                        	//f.topos = t;
                        	//f.addnew(ans);
                        	f.addNewFingering();
                        } 
                         //(new Fingering(xxx, p)).addnew(ans);
                        // ans.add(new Fingering(xxx, p));
                    }
                    ;
                }
                ;
            }
            ;

            // return c;
        }

        ;

        public void solver(StringCases x, StringCases y, StringCases z) {

            // this, x, y, z are all 4 strings.
            // each string has four  cases, representing each of the four notes
            for (int[] perm : perm4) {
                int a = perm[0];
                int b = perm[1];
                int c = perm[2];
                int d = perm[3];
                // StringCases c1 = x.arr[a];
                XCase x1 = this.xcasearr[a];
                XCase x2 = x.xcasearr[b];
                XCase x3 = y.xcasearr[c];
                XCase x4 = z.xcasearr[d];

                // each string can play its note
                if (!x1.ok || !x2.ok || !x3.ok || !x4.ok) {
                } else {
                    Position p = new Position(0, 0);
                    x1.commonPos(p);
                    x2.commonPos(p);
                    x3.commonPos(p);
                    x4.commonPos(p);
                    // must have common position
                    if (!p.isBad()) {
                        XCase[] xcases = {x1, x2, x3, x4};
                        // ans.add(new Fingering(xxx, p));
                        Fingering f = new Fingering(xcases, p);
                        //Topology t = new Topology (instrument, f); // of f
                        if (true) // (t.playable())
                        	{
                        	//f.topos = t;
                        	//f.addnew(ans);
                        	f.addNewFingering();
                        } 
                        
                        
                    }
                    ;
                }
                ; // end else
            }
            ;

            // return c;
        } ;
        
      */  
    };

    public static class FText {

        public String pos;
        public String[] fingerings;
        public String pitchHeight;

        FText(String xpos, String ph, String[] xfingerings) {
            pitchHeight = ph;
            pos = xpos;
            fingerings = xfingerings;
        }
    }

    
    
    @SuppressWarnings("rawtypes")
	public class Fingering implements Comparable {
        
    	// assumption here: everything is fingered together.
    	
    	// a fingering is an array of xcases.
    	int index;
    	private boolean markedForRemoval;
    	
    	private boolean hidden; // if true, we ignore this when getting our fingerings.
    	Position commonPos;
        XCase[] fingering;
        int[] pitchHeight;
        Topology topos;
        
        int[] stringSequence; // strings ordered from low pitch to hi
        int[] LRpitchSequence; // pitches as played from bottom string up. 
        //String[] abcSequence;
        
        public int bowing; // 0-3 if single , otherwise single int representing chord 
        
        Fingering () {
        	
        }
        
        @SuppressWarnings("unused")
		private String sayXcases (XCase[] xx){
        	String s = "";
        	for (XCase x : xx) {
        		s = s + " \npitch: " + x.pitch + " saite: " + x.saite + " height: " + x.height;
        				
        	}
        	return s;
        }
        // using this in chordSolver
        Fingering(XCase[] xcases) {
            //pitchHeight = x.pitchHeightArr();
        	markedForRemoval = false;
        	hidden = false;
        	int len = xcases.length;
        	// we come in in the correct order
        	//Arrays.sort(xcases);
        	// xcases now sorted from low STRING to hi.
        	// sort by pitch to get correct sequence of strings reading
        	// up
        	
        	//System.out.println("Fingering: " + sayXcases(xcases) );
        	
        	Point[] pS = new Point[len];
        	
        	for (int i=0; i<len;i++){
        		pS[i]=new Point (xcases[i].pitch, xcases[i].saite);
        	}
        	
        	//System.out.println("Points: " + Points.tostring(pS));
        	
        	Points.sortbyX(pS);
        	//System.out.println("Points sorted by X: " + Points.tostring(pS));
        	stringSequence = new int[len];
        	;
        	for (int i=0; i<len;i++){
        		
        		stringSequence[i] = pS[i].y;
        	}
        	Points.sortbyYUp(pS);
        	//System.out.println("Points sorted by Y up: " + Points.tostring(pS));
        	LRpitchSequence = new int[len];
        	for (int i=0; i<len;i++){
        		//System.out.println("            ... index =" + i + ", pitch = " + (pS[i].x));
        		LRpitchSequence[i] = pS[i].x;
        	
        		
        	}
        	
        	/*
        	pitchSequence = new int [len];
        	abcSequence = new  String [len];
        	for (int i = 0; i<len;i++){
        		pitchSequence[i] = xcases[i].pitch;
        		abcSequence[i] = xcases[i].abc;
        	}
        	*/
        	fingering = xcases; 	
            commonPos = new Position (0, 0);
           
            //this.addPitchHeight();
            topos = new Topology(instrument, this);
        }
        
        
        
        Fingering(XCase[] xcases, Position d) {
            //pitchHeight = x.pitchHeightArr();
        	markedForRemoval = false;
        	hidden = false;
        	
        	int len = xcases.length;
        	Arrays.sort(xcases);
        	// xcases now sorted from low STRING to hi.
        	// sort by pitch to get correct sequence of strings reading
        	// up
        	Point[] pS = new Point[len];
        	
        	for (int i=0; i<len;i++){
        		pS[i]=new Point (xcases[i].pitch, xcases[i].saite);
        	}
        	
        	//System.out.println("Points: " + Points.tostring(pS));
        	
        	Points.sortbyX(pS);
        	//System.out.println("Points sorted by X: " + Points.tostring(pS));
        	stringSequence = new int[len];
        	;
        	for (int i=0; i<len;i++){
        		
        		stringSequence[i] = pS[i].y;
        	}
        	Points.sortbyYUp(pS);
        	//System.out.println("Points sorted by Y up: " + Points.tostring(pS));
        	LRpitchSequence = new int[len];
        	for (int i=0; i<len;i++){
        		//System.out.println("            ... index =" + i + ", pitch = " + (pS[i].x));
        		LRpitchSequence[i] = pS[i].x;
        	
        		
        	}
        	
        	/*
        	pitchSequence = new int [len];
        	abcSequence = new  String [len];
        	for (int i = 0; i<len;i++){
        		pitchSequence[i] = xcases[i].pitch;
        		abcSequence[i] = xcases[i].abc;
        	}
        	*/
        	fingering = xcases; 	
            commonPos = d;
           
            //this.addPitchHeight();
            topos = new Topology(instrument, this);
        }
        
        // shallow -- we don't copy xcases.
        public Fingering copy(){
        	Fingering f = new Fingering();
        	f.index = index;
        	f.markedForRemoval = markedForRemoval;
        	f.hidden = hidden;
        	
        	f.fingering = fingering;
        	f.commonPos = commonPos;
        	f.topos = topos;
        	f.stringSequence = stringSequence;
        	//f.abcSequence = abcSequence;
        	//f.pitchSequence = pitchSequence;
        	return f;
        }
        
        /*
        public String answerString (){
        	String s = "";
            //int stops = f.countStops();
            // if (verboseOutput) s = s + "\nStops: " + stops;

            FText a = ftexts();
            //String[] a = f.texts();
            // texts[0] has the positions string.
           
               // s = s + "\nPitch Height: " + a.pitchHeight;
            
           // s = s + "\nPositions: " + a.pos;
            for (String ss : a.fingerings) {
                //String ss = a[i];
                s = s + "\n" + ss;
            };
            return s;
       
        
        }
        */
        // FINGERING toString
        @Override
        public String toString(){
        	String s = "";
        	for (XCase x : fingering) {
        		s = s + x + "\n";
        	}
        	return s;
        }
        
        public void compatible(Fingering f2) {
        	
        }
        
        public boolean isHidden (){
        	return hidden;
        }
        
        public void hide(){
        	hidden = true;
        }
        
        public void unhide(){
        	hidden = false;
        }
        
       
        public void sayContour(){
        	int[] stringSeq = stringSequence;
        	int[] pitchSeq = LRpitchSequence;
			System.out.println("------ L->r Pitch sequence: ---");
			Lis.print(pitchSeq);
			System.out.println("... input contour");
			Lis.print(monoInputContour);
			System.out.println("... string sequence:");
			Lis.print(stringSeq);
        }
        
        
        public Point[] getCoords() {
            // point is string, pitch height
            Point[] res = new Point[fingering.length];
            for (int i = 0; i < fingering.length; i++) {
                res[i] = fingering[i].stringHeightPoint();
            }
            return res;
        }
        
        
        
        
        
        public boolean exists (){
            Point[] c = this.getCoords();
            //String s = sPoints (c);

            return coordsExist(c, fingerings);
        }
        public void addNewFingering() {
            if (!this.exists())
                fingerings.add(this);
        }
        
        public void markForRemoval () {
        	markedForRemoval = true;
        }
        public boolean isMarkedforRemoval () {
        	return markedForRemoval;
        }
        

        public Topology.Topos getTopology() {
        	return topos.topos;
        	
        }
        public Position getPositions(){
        	return commonPos;
        }
       public boolean badToplogy() {
    	   return topos.bad();
       }
        
        public void printToplogy(){
        	topos.print();
        }
       
        
        public int lowPitch () {
        	int lo = 127;
        	for (XCase x : fingering){
        		if (x.pitch < lo) lo = x.pitch;
        	}
        	return lo;
        }
        
        public Point pitchRange (){
        	int lo = 127;
        	int hi = 0;
        	for (XCase x : fingering){
        		if (x.pitch < lo) lo = x.pitch;
        		if (x.pitch > hi) hi = x.pitch;
        	}
        	return new Point (lo, hi);
        }
        	
        //private  String [] strnames = { "I", "II", "III", "IV"}; 
        private  String [] strnames = { "IV", "III", "II", "I"}; 
        
        // make list of ascending strings (ie, instrument strings)
        public String[] XXXgetIString(){
        	
        	String[] ans = new String[fingering.length];
        	for (int i = 0; i< fingering.length; i++){
        		// open?
        		if (fingering[i].isOpen) 
        			ans[i] = "0";
        		else 
        			ans[i] = strnames[fingering[i].saite];
        	};
        	
        	return ans;
        }	
        
        public String[] XXX2getIString(){
        	
        	String[] ans = new String[fingering.length];
        	for (int i = 0; i< fingering.length; i++){
        		// open?
        		String open = "";
        		if (fingering[i].isOpen) {
        			open = "";
        		}
        		;
        	
        		ans[i] = strnames[fingering[i].saite] + open;
        	};
        	
        	return ans;
        }
        
        
        public String[] getIString(){
        	int len = fingering.length;
        	String[] ans = new String[len];
        	for (int i = 0; i< len; i++){
        		ans[i] = strnames[stringSequence[i]];
        	};
        	
        	return ans;
        }
        
       
      	
        public String getABCString() {
        	// xcase ordered by STRING lo->hi
        	
        	String s = "";
        	String arp = "";
        	  for (XCase xcase : fingering) {
        		  String aaa = xcase.k.toAbc();
        		 // System.out.println("getABCString: " + aaa);
        		  s = s  + aaa + "4";
        		  arp = arp + aaa;
                  }
        	if (fingering.length > 1) {
        		return "[" + s + "] yy " + arp;
        	} else{
        		return "[" + s + "]";
        				
        	}
        		
        }
/*
        public void addPitchHeight() {
            XCase[] xx = this.stopppedCases();
            int[] ph = new int[xx.length];
          
            for (int i = 0; i < xx.length; i++) {
                ph[i] = xx[i].pitchHeight();
            }
            ;
            pitchHeight = ph;
          
        }

        ;
*/
        public int countStops() {
            int c = 0;
            for (XCase xcase : fingering) {
                if (!xcase.isOpen) {
                    c++;
                }
            }
            ;
            return c;
        }

        ;
        // make array of stopped cases.

        public XCase[] stopppedCases() {
            int n = this.countStops();
            XCase[] a = new XCase[n];
            int next = 0;
            for (XCase xcase : fingering) {
                if (!xcase.isOpen) {
                    a[next] = xcase;
                    next++;
                }
            }
            ;
            return a;
        }
       
        /*
        public FText ftexts() {
            int len = fingering.length;
            String[] text = new String[len];
            String p = commonPos.tostring();
            for (int i = 0; i < len; i++) {
                text[i] = fingering[i].tostring();
            }
            //String pH = intArrString(pitchHeight);
            return new FText(p, pH, text);
        }
        */

        public String[] texts() {
            int len = fingering.length;
            String[] text = new String[len + 1];
            text[0] = commonPos.tostring();
            for (int i = 1; i < len + 1; i++) {
                text[i] = fingering[i - 1].toshortstring();
            }
            return text;
        }
        
        public void print() {
            System.out.println("-----------------------");
            //commonPos.print();
            String com = commonPos.tostring();
            System.out.println("Positions: " + com);
            for (XCase c : fingering) {
                System.out.println(c);
               
            }
            ;
        }

        public String pointsString(){
            String s = "";
             for (XCase c : fingering) {
                s = s + c.pointString() + ":";
            }
            ;
            return s;
        }

        public Point[] stringHeightPoints(){
        	int len = fingering.length;
        	Point[] p = new Point[len];
        	for (int i = 0; i < len; i++){
        		p[i] = fingering[i].stringHeightPoint();
        	}
        	return p;
        }
        /**
         * 
         * @return array of points of (saite, pitch)
         */
        public Point[] stringPitchPoints(){
        	int len = fingering.length;
        	Point[] p = new Point[len];
        	for (int i = 0; i < len; i++){
        		p[i] = fingering[i].stringPitchPoint();
        	}
        	return p;
        }
        public int maxHeight(){
        	int h = 0;
        	for (XCase x : fingering){
        		h = Math.max (h, x.height);
        	}
        	return h;
        }
       
        private boolean pointArrEqual (Point[] x, Point[] y){
            if (x.length != y.length) return false;

            for (int i = 0 ; i< x.length; i++){
                if (!x[i].equals(y[i])){
                    
                    return false;
                }
            }
            return true;
        }
        private boolean coordsExist(Point[] c, ArrayList<Fingering> ans) {
            //boolean r = false;
            //String newS = sPoints(c);
            // System.out.println("Testing " + sPoints(c) + " with:");
            for (Fingering f : ans) {
                Point[] cc = f.getCoords();
               // System.out.println("   compare: " + sPoints(cc) + " " );
                if (pointArrEqual(c,cc)) {
                    // String oldS = sPoints(cc);

                    //System.out.println("EQUALS new: " + newS + " old " + oldS);
                 //       + " AND  " + sPoint(cc) );
                   
                    return true;
                }
            }
            return false;
        }

        public String sPoint (Point p){
             return ("(" + p.x + ", " + p.y + ")");
        }
        public String sPoints (Point[] ps){
            String s = "";
            for (Point p : ps){
                s = s + " " + (sPoint(p));
            }
            return s;
           
        }

		@Override
		public int compareTo(Object f2) {
			int h1 = maxHeight();
        	int h2 = ((Fingering)f2).maxHeight();
        	if (h1 > h2) 
        		return 1; 
        	else if (h1 < h2)
        		return -1;
        	else
        	return 0;
        	
		}
        
    }

    // generate all strings.
    public  class StringMatrix {

        public StringCases[] arr;
        int nNotes;
        
        /*
        StringMatrix(ArrayList<Keynum.K> klist) {
            nNotes = klist.size();
            arr = new StringCases[4];
            for (int str = 0; str < 4; str++) {
                arr[str] = new StringCases(str, klist);
            }
        }

        ;
        */

        StringMatrix(Keynum.K[] ks) {
            nNotes = ks.length;
            arr = new StringCases[4];
            for (int str = 0; str < 4; str++) {
                arr[str] = new StringCases(str, ks);
            }
        }

        ;
        /*
        void print() {
            for (StringCases c : arr) {
                c.print();
            }
        }

        ;
        
        */
        /*
        public void solve() {
           // System.out.println("There are " + nNotes + " notes");
            StringCases s1 = this.arr[0];
            StringCases s2 = this.arr[1];
            StringCases s3 = this.arr[2];
            StringCases s4 = this.arr[3];

            switch (nNotes) {
            	case 1:
            		s1.solver();
            		s2.solver();
            		s3.solver();
            		s4.solver();
            		break;
                case 2:
                    s1.solver(s2);
                    s2.solver(s3);
                    s3.solver(s4);
                    break;
                case 3:
                    s1.solver(s2, s3);
                    s2.solver(s3, s4);
                    break;
                case 4:
                    s1.solver(s2, s3, s4);
                    break;
                default:
                    // System.out.println("Can only take 2,3 or 4 args");
                    break;
            }
            ;


        }

        ;
        */
    };
    
    
    /*
    public enum ProblemType {

        EXPAND_PCS,
        FINGERING
    };
    */
	public  int minDuration(K[] ks){
		
		// take min duration
		int d = Integer.MAX_VALUE;
		for (Keynum.K k : ks) {
			d = Math.min(d, k.duration);
		}
		return d;
	
}


 	public  int maxDuration(K[] ks) {
		int d = 0;
		for (Keynum.K k : ks) {
			d = Math.max(d, k.duration);
		}
		return d;
	}
 	
 	 public ChordFingering restricttoPosition(Position pos){
 		 
 		ChordFingering r = copy();    
     	r.subindex = 0;
 		//r.keynumResult = keynumResult;
     	//r.pcExpansions = pcExpansions;
     	r.fingerings = new ArrayList<Fingering>();
     	//System.out.println("================= FING restricting to pos: " + (pos.tostring()));
     	// only take fingerings with pos interesction.
     	for (Fingering f : getFingerings()) {
     		Position posHere = f.commonPos;
     		//System.out.println("---> fingering " + f.getABCString());
     		//System.out.println("================= THIS HAS: : " + (posHere.tostring()));
     		Position common = pos.commonPos(posHere);
     		
     		
     		if (!common.isBad()) {
     			Fingering ff = f.copy();
     			ff.commonPos = pos;
     			r.fingerings.add(ff);
     		}
     	}
     	
     	// possibly none of these work. 
     	/*
     	if (r.fingerings.size() == 0) {
     		String abc = r.abcString();
     		System.out.println("Uh oh -- ind:  "+ index +" "+abc +" restricting to pos got NOTHING! " + pos.tostring());
     	}
     	*/
     	
     	return r;
     }
 	
 	
    /*
    public static void makePCExpansion(Result r) {
        //setCaseArr();
        ans.clear();
        ArrayList<Keynum.K> ks = new ArrayList<Keynum.K>();
        for (Keynum.K k : r.getKs()) {
            ks.add(k);
        }
        ArrayList<ArrayList<Keynum.K>> chinesechords =
                AllNotes.chineseKs(localInstrument, ks);

        // allchords is now a list of every possible combination
        // of all pitches of pc  in range.
        for (ArrayList<Keynum.K> chord : chinesechords) {
            ans.clear();
            AllStrings a = new AllStrings(chord);
            a.solve();
            addPitchHeight();

            r.addAns(ans);
        }
    }
    */

  
    
    // display variables
    public static boolean verboseOutput = false;
    /*
    public class Arr extends ArrayList<ChordFingering> {
    	
    }
    
    */
   
    
    
    
    public static void main (String[] ss){
    	//K ks [] = Keynum.parseKeyString("g4 d5 b5 g6");
    	K ks [] = Keynum.parseKeyString("g5 bf5 d6");
        System.out.println(ks.length + " notes");
        for (K k : ks) 
        	System.out.println(k.pitch + " " + k.name + " " + k.accidentals + " " + k.toAbc());
        Instrument v = new Instrument (Instr.VN);
        ChordFingering f = new ChordFingering(0, v, ks);
        System.out.println(f);
    	
    }
}
     
    
	    




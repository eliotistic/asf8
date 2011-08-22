/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package computingmusic;

import java.util.Collections;
import java.util.ArrayList;
import java.util.regex.*;

import computingmusic.utils.IntList;

/**
 *
 * @author eliot
 */
public class Keynum {

    private static final String[] noteVec = {"C", "C#", "D", "Ef", "E", "F", "F#", "G", "G#", "A", "Bb", "B"};

    // private static HashMap<String, Integer> m = new HashMap<String, Integer>();
    private static final int noteNum(Character c) {
        c = Character.toUpperCase(c);
        switch (c) {
            case 'C':
                return 0;
            case 'D':
                return 2;
            case 'E':
                return 4;
            case 'F':
                return 5;
            case 'G':
                return 7;
            case 'A':
                return 9;
            case 'B':
                return 11;
            default:
                return -1;
        }
    }
    /*
    private static boolean numeric(String s){
        return Pattern.matches("^[\\d]*\\d$", s);

     }
     */
    // pitch name + accidental is a pitch class.
  


    // return name + midinum
    // not used??
    public static String ofIntMidi(int s) {
        int q = s / 12;
        int p = s % 12;
        return (noteVec[p] + q + "/" + s);

    }
    // return just name, no midi
    public static String ofInt(int s) {
        int q = s / 12;
        int p = s % 12;
        return (noteVec[p] + q);

    }
    
    public static int[] toPitchArray (K[] ks){
        int len = ks.length;
        	int[] a = new int[len];
        	for (int i = 0;i<len;i++){
        		a[i] = ks[i].pitch;
        	}
        	return a;
        }
    
    public static IntList toPitchList (K[] ks){
        IntList a = new IntList();
        for (K k : ks){
        	a.add(k.pitch);
        	}
        Collections.sort(a);
        return a;
    }
    
    /*
    public static IntSet[] toPitchSets (K[] ks){
        int len = ks.length;
        	IntSet[] a = new IntSet[len];
        	for (int i = 0;i<len;i++){
        		a[i] = ks[i].pitch;
        	}
        	return a;
        }
    */
    
    
    
    
    
    public static class K { // TODO Start of class K
        public int pitch;
        //public String baseName; // name - any octaves
        public String name;
        public String chromaName;
        public boolean isPc;
        public boolean isBad;
        public int pc; // == note = pitch modulo 12
        public int accidentals; // +1 if sharp, -1 if flat, 0 if none
        public int octaves; // 
        
        public short duration;
        public int om; // jb: seems unused  time of start of the note
        
        public K(){
        	om = 0;
            pitch = 0;
            name = "";
            chromaName = "";
            isBad = false;
            isPc = false;
            pc = 0;
            accidentals = 0;
            octaves = 0;
            duration = 0;
        }
        
        public K copy(){
        	K k = new K ();
        	k.pitch = this.pitch;
        	k.name = this.name;
        	k.chromaName = this.chromaName;
        	k.isBad = this.isBad;
        	k.pc = this.pc;
        	k.accidentals = this.accidentals;
        	k.octaves = this.octaves;
        	k.duration = duration;
        	k.om = om;
			return k;
        	
        }
        
        public void setDuration (short d){
        	duration = d;
        }
        public void setOm(int o){
        	om = o;
        }
        // just pitch name.
        public String toAbc (){
        	
    		String chroma = chromaName;
    		String abcAcc = acciString(accidentals);
    		
    		String ans = "";
    		if (octaves >= 6){
    			String oct = mult("'", octaves-6);
    			ans = (abcAcc + chroma.toLowerCase() + oct);
    		}  else if (octaves == 5){
    				ans = (abcAcc + chroma.toUpperCase());
    		} else {
    			String oct = mult(",", Math.abs (5-octaves));
    			ans = (abcAcc + chroma.toUpperCase() + oct);
    		};
    		/*
    		System.out.println("toAbc:chromaName=" 
    				+ chroma 
    				+ " accidentals: " + accidentals
    				+ "accistring: " + abcAcc 
    				+ " ans: " + ans);
    		*/
    		return ans;
    	}
       

        // constructed when parse fails.
        public K(String s, boolean bad){
           name = s;
           isBad = bad;
        }
        // basic constructor
        public K(String chroma, String s, int note, int acc, int oct, boolean hasoct ){
            //System.out.println("New K, chroma = " + chroma);
        	int notenum = note + acc + (12 * oct);
            pitch = notenum;
            name = s;
            chromaName = chroma;
            isPc = !hasoct;
            isBad = false;
            pc = note;
            accidentals = acc;
            octaves = oct;

        }
        // constructed when pc (no octaves) was specified.

        // old basic constructor.
      
        public String tostring(){
            if (isBad) return ("Bad parse:" + this.name);
            if (isPc)  return ("Pc: " + name + " (" + pitch +")" + accidentals);
            return ("K " + name + " " + pitch);

        }
        public void print() {
            System.out.println(this.tostring());
        }

    }
/*
    public static int parseOctave (String s){
        if (s.length() == 0){
            return 0;
        } else {
        Integer i = Integer.parseInt(s);
        return i;
    }
    }
*/
    public static int parseAccidental (String s){
        int a = 0;
        for (int i = 0; i< s.length(); i++) {
        switch (s.charAt(i)){
            case 'S':
            case 's':
            case '#':
                a= a + 1;
                break;
            case 'F':
            case 'f':
            case 'B':
            case 'b':
                a= a - 1;
                break;
            default: break;
            }
        }

        return a;
    }
    
     public static boolean isValid (String s) {
        s = s.toUpperCase();
        if (Pattern.matches("^[A-G][SF#B]*$", s)) return true;
        if (Pattern.matches("^[A-G][#S|BF]*\\d*$", s)) return true;
        return false;
    }

     public static K parseKey (String s) {
       // Pattern valid = Pattern.compile("^[ABCDEFG][#S|BF]*[\\d]*\\d$");
    	 // TODO find a key thourgh text. text given is: gb5, for example. 
        if (!isValid(s)) return new K (s, true );

        Pattern p = Pattern.compile("^([A-G])([S#FB]*)(\\d*)",Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher (s);
        if (m.find()) {
            String note = m.group(1);
            String acc  = m.group(2);
            String oct  = m.group(3);

           // System.out.println("parseKey note: " + note + " acc: " + acc + " oct: " + oct );
            String chroma = Character.toString(note.charAt(0));
            int nn = noteNum(note.charAt(0));
            int a = parseAccidental(acc);

            boolean hasOct = oct.length() != 0;
            int o = 0;
            if (hasOct) o = Integer.parseInt(oct);
            K k = new  K(chroma, s, nn, a, o, hasOct );
            // nt notenum = nn + a + (12 * o);
            return k;
            /*
            System.out.println(s + " Note: " + nn
                    + " Accidental: " + a
                    + " oct:" + o 
                    + " pitchNum:" + notenum);
             */
        } else {
            return new K (s, true);
        }
    }

     public static K[] parseKeyString (String input){
        input = input.trim();
        String delimiters = "[ ]+"; // all chars the delimit a separation
        String[] tokens = input.split(delimiters);
        //for(String s: tokens) System.out.println("token is: " + s);
        K[] ks = new K[tokens.length];
        for(int i = 0; i<tokens.length; i++)
            ks[i] = parseKey(tokens[i]);
        return ks;
     }

     public static ArrayList<K> parseKeyStringList (String input){
         ArrayList<K> ks = new ArrayList<K> ();
         K[] k1 = parseKeyString (input);
         for (K k : k1) ks.add(k);
         return ks;
     }

     public static final boolean somePcs (K[] ks){
        for (K k : ks)
            if (k.isPc) return true;
        return false;
    }

     public static final ArrayList<String>badParses (K[] ks){
         ArrayList<String> s = new ArrayList<String>();
         for (K k : ks)
             if (k.isBad)
                 s.add(k.name);
         return s;
     }

     public static class Result {
         public boolean noInput;
         public boolean bad;
         public ArrayList<String> badparses ;
         public boolean hasPcs;
         public K[] ks;

         Result() {
        	 
         }
         Result (K[] k_s){
        	 noInput = false;
        	 bad = false;
        	 hasPcs = false;
        	 ks = k_s;
         }
         Result (K k){
        	 noInput = false;
        	 bad = false;
        	 hasPcs = false;
        	 ks = new K[1];
        	 ks[0] = k;
         }
         
         public int nKs(){
        	 return ks.length;
         }
         public K[] getKs(){
        	 return ks;
         }
         public String badParseString (){
             String r = "";
             for (K k : ks)
             if (k.isBad)
                 r = r + (k.name) + " ";
             return r;

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
         
         
         
         public boolean noInput() {
             return noInput;
         }

         public boolean wrongNumberofNotes() {
             int len = ks.length;
            // return (len < 2 || len > 4);
             return (len > 4);
         }

        
         public boolean badInput() {
             return bad;
         }

        

     }
        
     
     

     public static final Result parse (String s){
         Result r = new Result();
         if (s.isEmpty()) {
             r.noInput = true;
             return r;
         }
         //System.out.println("String s is : " + s);
         K[] ks = parseKeyString(s);
       
         ArrayList<String> badparses = badParses(ks);
         r.noInput = false;
         r.ks = ks;
         r.bad = !badparses.isEmpty();
         r.badparses = badparses;
         r.hasPcs = somePcs(ks);
         return r;

     }
     
     
 	public static String mult (String s, int times){
		String r = "";
		for (int i = 0; i<times; i++){
			r = r + s;
		}
		return r;
	}
	
 	public static String acciString(int accidentals){
		
		if (accidentals == 0) {
			return "";
		} else if (accidentals > 0){
			return mult("^", accidentals);
		} else {
			return mult("_", Math.abs (accidentals));
		}
	}
 	
 	
 	/// some operations on K[]
 	
 	// find a k with this pitch, remove it from list, return k.
 	static K takePitch (ArrayList<K> ks, int pitch){
 		for (K k : ks){
 			if (k.pitch == pitch){
 				ks.remove(k);
 				return k;
 			}
 		};
 		return null;
 		
 	}
	
 	public int minDuration(K[] ks){
		
		// take min duration
		int d = Integer.MAX_VALUE;
		for (Keynum.K k : ks) {
			d = Math.min(d, k.duration);
		}
		return d;
	
}


 	public int maxDuration(K[] ks) {
		int d = 0;
		for (Keynum.K k : ks) {
			d = Math.max(d, k.duration);
		}
		return d;
	}
 	
 	public int getOm (K[] ks){
 		return ks[0].om;
 	}

     public static void main(String[] args){
        /*
         parseKey("ff");
         parseKey("cbb");
         parseKey("g6");
         parseKey("gsf6")   ;
         parseKeyString("this f####17 is c6 d hellodar!");
         *
         */
         /*
         String s = "carumba!!!!             eight g";
            String delimiter = "[ ]+";
            String[] tokens = s.split(delimiter);

            for(int i = 0; i<tokens.length; i++)
               System.out.println("  tok: " + tokens[i]);    ;

          *
          */
         //K[] ks = parseKeyString("girl         gffd  gfs gffs gffss f####17 is c6 d hellodar!");
         K ks [] = parseKeyString("a c ef5 gss6");
         System.out.println(ks.length + " notes");
         for (K k : ks) 
        	 System.out.println(k.pitch + " " + k.name + " " + k.accidentals + " " + k.toAbc());
     }
}


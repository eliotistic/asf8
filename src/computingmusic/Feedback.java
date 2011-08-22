package computingmusic;
//import computingmusic.Fing.Instr;

import java.awt.Point;
import java.util.ArrayList;
import java.util.regex.*;

/*
 * looks like this:
 * 216.252.89.180 (19:21:10 17 3 110) impossible,VN,0,8:1,3:2,1:3,3:
	
 * 
 * 
 * 
 * 
 */



public class Feedback {
	
	
	public static class Result {
		Boolean goodParse;
		String parseString;
		String ip;
		String date;
		String inst;
		Instrument instrument;
		String rating;
		String noteString;
		String keyString; // C6/72 etc
		String abcString;
		ArrayList<Point> notes;
		Point [] pnotes;
		Point range;
		Result(String s){
			goodParse = false;
			parseString = s;
		}
			
	}
	@SuppressWarnings("rawtypes")
	public static class Trip implements Comparable {
		int string;
		int height;
		int pitch;
		
		@Override
		public int compareTo(Object t2) {
			int p2 = ((Trip)t2).pitch;
		
			if (pitch == p2) return 0;
			else if (pitch > p2) return 1;
			else return -1;
			
		}
	}
	public static ArrayList<Point> parseNotes(String s) {
		ArrayList<Point> notes = new ArrayList<Point>();
		Pattern colon = Pattern.compile(":");
		Pattern sp = Pattern.compile("(\\d+),(\\d+)");
		// Split input with the pattern
		String[] result = colon.split(s);
		for (int i = 0; i < result.length; i++) {
			String nn = result[i];
			Matcher xx = sp.matcher(nn);
			if (xx.find()) {
				int string = Integer.parseInt(xx.group(1));
				int height = Integer.parseInt(xx.group(2));
				//System.out.println("String: " + string + "  Height: " + height);
				notes.add(new Point(string, height));
			}
		}
		return notes;

	}
		
	
	static String ip = 
		"^\\d+\\.\\d+\\.\\d+\\.\\d+";
	static String datex = "(\\([0-9: ]*\\))"; 
	
	static String word = "(\\w+)";
		
	static String notesx = 
		"([0-9:,]+)";
	
	static String rx = "(" + ip + ")" + "\\s+" + datex + "\\s+" 
		+ word + ",+" + word + "," + notesx;
	
	public String pitchString (String inst, ArrayList<Point> pts){
		String s = "";
		
		return s;
	}
	
	public static String keyString(Instrument v, ArrayList<Point> pts){
		String s = "";
		for (Point p : pts){
			s = s + v.pointKey(p) + " ";
		}
		return s;
	}
	public static Result parse(String s) {
		
		Pattern p = Pattern.compile(rx, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(s);
		 Result r = new Result(s);
		if (m.find()) {
			ArrayList<Point> pts = parseNotes(m.group(5));
			String ins = m.group(4);
			//Instr insI = Fing.getInstr(ins);
			//Instrument insI = Instrument.ofString(ins);
			Instrument insI = new Instrument(ins);
			String keys = keyString(insI,pts );
			Keynum.K[] kKeys= Keynum.parseKeyString(keys);
			r.abcString = AbcParse.kArrToAbc(kKeys);
			r.range = Points.range(insI, pts);
			r.ip = m.group(1);	
			r.date = m.group(2);
			r.rating = m.group(3);
			r.inst = ins;
			r.instrument = insI;
			r.keyString = keys;
			r.noteString = m.group(5);
			r.notes = pts;
			r.goodParse = true;
			return r; 
				} else {
					System.out.println("ERROR IN PARSE: " + s);
					return r;
				}	
	}
	
	    
	 public static void main (String[] s){
		Result r  = parse 
		("216.252.89.180 (19:21:10 17 3 110) impossible,VN,0,2:1,3:2,1:3,3:");
		for (Point p : r.notes ){
			System.out.println("String: " + p.x + " Height: " + p.y);
		}
	 }
}

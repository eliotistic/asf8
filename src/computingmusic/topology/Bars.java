package computingmusic.topology;

import java.util.ArrayList;

import computingmusic.utils.Bv;



public class Bars {
	public ArrayList<Bv> bvs2;
	public ArrayList<Bv> bvs3;

	public enum barType {
		Zero,
		One,
		Bar4,
		Bar3,
		Gap1, // 1011 -- recall E string is 0.
		Gap2, // 1101
		Gap101, 
		Gap1001,
		Axis0,
		Axis1,
		Axis2,
		Unknown // should never be used
		
	}
	
	public Bars(){
		bvs2 = new ArrayList<Bv>();
		bvs3 = new ArrayList<Bv>();
		gen();
	}
	
	public class Type {
		Bars.barType type;
		int ind;
	}
	
	private String[] pats2 = {
			"++..",
			".++.",
			"..++",
			"+.+.",
			".+.+",
			"+..+",
	};
	
	private Bars.barType[] pats2Types = {
			barType.Axis0,
			barType.Axis1,
			barType.Axis2,
			barType.Gap101,
			barType.Gap101,
			barType.Gap1001
			
	};
	
	private String[] pats3 = {
			"+++.",
			".+++",
			"++.+",
			"+.++",
	};
	
	private Bars.barType[] pats3Types = {
		barType.Bar3,
		barType.Bar3,
		barType.Gap2,
		barType.Gap1,
			
	};
	
	public boolean isAxis(Bars.barType t){
		return t.equals(barType.Axis0) || t.equals(barType.Axis1) || t.equals(barType.Axis2);
	}
	public boolean isBar(Bars.barType t){
		return t.equals(barType.Bar3) || t.equals(barType.Bar4);
	}
	public boolean isOne(Bars.barType t){
		return t.equals(barType.One);
	}
	
	
	// analyzing one row of fingerboard.
	// we are just matching bvs and returning the associating type.
	public Bars.barType analyzeType(Bv b){
		int sz = b.cardinality();
		if (sz == 0){ 
			return barType.Zero;

		} else if (sz == 1) {
			return barType.One;
		} else if (sz == 2){
			for (int i = 0; i<pats2.length; i++) {
				if (bvs2.get(i).equals(b)) {
					return pats2Types[i];
				}
			}
			return barType.Unknown;
		} else if (sz == 3) {
			for (int i = 0; i<pats3.length; i++) {
				if (bvs3.get(i).equals(b)) {
					return pats3Types[i];
				}
			}
			return barType.Unknown;
		} else if (sz == 4) {
			return barType.Bar4;
		} else {
			return barType.Unknown;
		}
	}
	
	private void gen (){
		for (int i=0; i<pats2.length; i++){
			bvs2.add(new Bv(pats2[i]));			
		}
		for (int i=0; i<pats3.length; i++){
			bvs3.add(new Bv(pats3[i]));			
		}
	}
	
	public static void main (String[]args){
		Bars bars = new Bars();
		Bv b = new Bv("++,.");
		System.out.println(bars.analyzeType(b));
	}
	
	
}

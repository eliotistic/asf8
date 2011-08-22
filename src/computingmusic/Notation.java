package computingmusic;

public class Notation {

	private String mult (String s, int times){
		String r = "";
		for (int i = 0; i<times; i++){
			r = r + s;
		}
		return s;
	}
	
	public String acciString(int accidentals){
		
		if (accidentals == 0) {
			return "";
		} else if (accidentals > 0){
			return mult("^", accidentals);
		} else {
			return mult("_", accidentals);
		}
	}
	
	public String kToAbc (Keynum.K k){
		String chroma = k.chromaName;
		String accidentals = acciString(k.accidentals);
		int octs = k.octaves;
		
		if (octs >= 6){
			return (accidentals + chroma.toLowerCase());
		
		} else
			return (accidentals + chroma.toUpperCase());
	
	}
	
}

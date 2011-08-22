package computingmusic;

public class AbcParse {

		 static String kToAbc(Keynum.K k){
			return k.toAbc();
		 }
		 
		 static String kArrToAbc (Keynum.K[] ks){
			 String s = "";
			 for (Keynum.K k : ks){
				 s = s + kToAbc(k);
			 };
			 return "[" + s + "]";
		 }
		
}

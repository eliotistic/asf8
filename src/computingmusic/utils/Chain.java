package computingmusic.utils;

import java.util.ArrayList;

public class Chain extends ArrayList<Seq>{
	
	private static final long serialVersionUID = 1L;

	@Override
	public String toString(){
		String space = "";
		String s = "(";
		for (Seq q : this){
			s = s + space + q;
			space = " ";
		};
		return s + ")";
	}

}

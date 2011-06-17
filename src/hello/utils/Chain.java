package hello.utils;

import java.util.ArrayList;

public class Chain extends ArrayList<Seq>{
	
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

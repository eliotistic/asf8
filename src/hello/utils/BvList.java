package hello.utils;

import java.util.ArrayList;

public class BvList extends ArrayList<Bv>{
	
	private static final long serialVersionUID = 1L;

	public BvList copy () {
		BvList b = new BvList();
		for (Bv v : this) {
			b.add(v.copy());
		}
		return b;
	}

}

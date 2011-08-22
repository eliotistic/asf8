package computingmusic.utils;


public class OpenSeq extends MySeq {
	public OpenSeq(MySeq s, int pos) {	
		super(s.index, pos, s.abc);
		
	}
	public OpenSeq(int index, int pos, String abc) {	
		super(index, pos, abc);
		
	}
	
	
}
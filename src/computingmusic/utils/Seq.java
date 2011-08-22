package computingmusic.utils;

public class Seq {
	int start;
	int stop;
	public int length;
	
	public Seq (int start, int stop){
		this.start = start;
		this.stop  = stop;
		length = 1 + stop - start; 
	}

	@Override
	public String toString(){
		if (start == stop){
			return "Seq<" + start + ">";
		} else{
			return "Seq<" + start + "-" + stop + ">";
		}
	}
}

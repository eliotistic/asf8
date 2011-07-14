package hello;

/**
 * 
 * @author JeanBenoit
 * Stored in memory by the user
 */
public class Extension {
	
	private int index; // where in the piece is the first (or second) Fingering of this extension
	public int getIndex() {
		return index;
	}

	public int getLength() {
		return length;
	}

	private int length; // if size of 4, then length is 3
	
	public Extension(int masterIndex, int length) {
		index = masterIndex;
		this.length = length;
		
	}

	@Override
	public String toString()
	{
		return "This Extension begins at " + index + " in the piece and has length of " + length;
	}
}

package computingmusic;

import java.util.ArrayList;

/**
 * 
 * @author JeanBenoit
 * Stored in memory by the user
 */
public class Extension {
	
	private int index; // where in the piece is the first (or second) Fingering of this extension
	private int length; // if size of 4, then length is 4
	private ArrayList<Integer> indexList = new ArrayList<Integer>();
	//private boolean isSingleBoard;
	public int getIndex() {
		return index;
	}

	public int getLength() {
		return length;
	}
	
	public ArrayList<Integer> getIndexes()
	{
		return indexList;
	}
	
	public Extension(int masterIndex, int length, ArrayList<Integer> intList) {
		if(intList.size() != length)
		{
			System.err.println("Error: constructor of Extension, the number of indexes do not correspond with length");
		}
		else 
		{
			index = masterIndex;
			this.length = length;
			indexList = intList;
		}
	}

	@Override
	public String toString()
	{
		return "This Extension begins at " + index + " in the piece and has length of " + length;
	}
}

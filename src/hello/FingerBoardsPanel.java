package hello;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.JPanel;

/**
 * 
 * @author JeanBenoit
 *
 */
public class FingerBoardsPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private Instrument instr;
	private ArrayList<FingerBoard> boards = new ArrayList<FingerBoard>();
	//private ArrayList<Integer> freezesList = new ArrayList<Integer>();
	//private FingerBoard board1;
	//private FingerBoard board2;
	//private FingerBoard board3;
	public int numBoards = 0;
	
	public int currentIndex = 0; // Index where we will modify the next FingerBoard
	
	
	//private int indexStartExtension = -1;
	//private int masterIndex = -1;
	private Extension frozenExtension;
	//private int currentExtensionLength = 1;
	
	public FingerBoardsPanel()
	{
		setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();
		
		FingerBoard board1 = new FingerBoard();
		FingerBoard board2 = new FingerBoard();
		FingerBoard board3 = new FingerBoard();
		
		c1.gridx = 0;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.BOTH;
		
		c2.gridx = 1;
		c2.gridy = 0;
		c2.fill = GridBagConstraints.BOTH;
		
		c3.gridx = 2;
		c3.gridy = 0;
		c3.fill = GridBagConstraints.BOTH;
		
		this.add(board1, c1);
		this.add(board2, c2);
		this.add(board3, c3);
		
		boards.add(board1);
		boards.add(board2);
		boards.add(board3);
		
		numBoards = 3;
		giveInstrument();
		boards.get(0).setIsCurrentFingerBoard(true);
	}
	
	/**
	 * 
	 * @return
	 */
	public FingerBoard getCurrentBoard()
	{
		return boards.get(currentIndex);
	}
	
	
	
	/**
	 * Try to freeze; the freeze will be done only if there is currently an extension being made
	 * @return
	 */
	public void freeze()
	{
		if(getCurrentBoard().representsAnExtension())
		{
			frozenExtension = makeCurrentExtension();
			System.out.println("EXTENSION: " + frozenExtension);
			
		}
		else
		{
			
		}
	}
	
	public void save()
	{
		if(frozenExtension != null)
		{
			/*String extensionString = "ind " + extension.getIndex() + " len " + extension.getLength();
			BigGui.saveExtension(extensionString);*/
			//TODO
		}
	}
	public Extension makeCurrentExtension()
	{
		Extension ext;
		
		int length = getCurrentBoard().getExtensionSize();
		int masterInd = getCurrentBoard().getMasterIndex();
		ArrayList<Integer> intList = new ArrayList<Integer>();
		for(int i = currentIndex - length + 1; i<= currentIndex; i++)
		{
			intList.add(boards.get(i).getTrailIndex());
		}
		ext = new Extension(masterInd, length, intList);
		return ext;
	}
	
	public void clearAll()
	{
		for(FingerBoard fb : boards)
		{
			fb.clear();
			fb.setIsCurrentFingerBoard(false);
		}
		while(boards.size() > 3)
		{
			boards.remove(3);
		}
		while (this.getComponentCount() > 3)
		{
			this.remove(3);
		}
		currentIndex = 0;
		numBoards = 3;
		boards.get(0).setIsCurrentFingerBoard(true);
		// TODO trace back the first board
	}
	
	/*public void addFingers(Point[] pts)
	{
		addABoard();
		boards.get(currentIndex).setIsCurrentFingerBoard(false);
		boards.get(++currentIndex).setFingers(pts);
		
		//getCurrentBoard().incrementExtensionSize();
		getCurrentBoard().setIsCurrentFingerBoard(true);
		
		
	}*/
	
	public void addFingerTrail(FingerTrail ft)
	{
		addABoard();
		boards.get(currentIndex).setIsCurrentFingerBoard(false);
		boards.get(++currentIndex).setFingerTrail(ft);
		
		//getCurrentBoard().incrementExtensionSize();
		getCurrentBoard().setIsCurrentFingerBoard(true);
		
		giveListeners(); // TODO bad code to put it here
	}
	
	private void addABoard()
	{
		GridBagConstraints c1 = new GridBagConstraints();
		FingerBoard newBoard = new FingerBoard();
		boards.add(newBoard);
		giveInstrument();
		//System.out.println("Adding a board");
		
		c1.gridx = numBoards++;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.BOTH;
		this.add(newBoard, c1);
		
	}
	
	public void applyShrink()
	{
		deleteBoardsFrom(currentIndex - getCurrentLength()); // note that getCurrentLength returns the new shortened length
		
	}
	
	public void deleteBoardsFrom(int index)
	{
		while(boards.size() > index)
		{
			boards.remove(index);
		}
		while (this.getComponentCount() > index)
		{
			this.remove(index);
		}
		numBoards = index;
		addABoard();
		addABoard();
		addABoard();
		boards.get(index-1).setIsCurrentFingerBoard(true);
		currentIndex = index-1;
		this.repaint();
	}
	
	public int getCurrentLength()
	{
		return getCurrentBoard().getExtensionSize();
	}

	public void setInstrument(Instrument it) {
		instr = it;
		giveInstrument();
	}
	
	public void giveInstrument()
	{
		for(FingerBoard f : boards)
		{
			f.instrument = instr;
		}
	}
	
	public void giveListeners()
	{
		for(int i = 0; i<boards.size(); i++)
		{
			final int a = i;
			boards.get(i).addMouseListener(new java.awt.event.MouseAdapter() {
	            public void mouseClicked(java.awt.event.MouseEvent evt) {
	            	if(evt.getClickCount() == 2)
	            	{
	            		System.out.println("two clicks detected");
	            		deleteBoardsFrom(a);
	            	}
	            	if(evt.getButton() == 3)
	            	{
	            		System.out.println("=3");
	            	}
	            }
	        });
		}
	}
	
}

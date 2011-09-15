package computingmusic;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * 
 * @author JeanBenoit
 *
 */
public class FingerBoardsPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private Instrument instr;
	private ArrayList<FingerBoard> boards = new ArrayList<FingerBoard>();
	private ArrayList<AboveFingerBoard> aboveBoards = new ArrayList<AboveFingerBoard>();
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
	
	private BigScore scoreAccess;
	
	public FingerBoardsPanel(BigScore access)
	{
		
		setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();
		GridBagConstraints c4 = new GridBagConstraints();
		GridBagConstraints c5 = new GridBagConstraints();
		GridBagConstraints c6 = new GridBagConstraints();
		
		FingerBoard board1 = new FingerBoard();
		FingerBoard board2 = new FingerBoard();
		FingerBoard board3 = new FingerBoard();
		
		AboveFingerBoard above1 = new AboveFingerBoard();
		AboveFingerBoard above2 = new AboveFingerBoard();
		AboveFingerBoard above3 = new AboveFingerBoard();
		
		c1.gridx = 0;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.VERTICAL;
		c1.anchor = GridBagConstraints.CENTER;
		c1.weightx = 0;
		
		c2.gridx = 1;
		c2.gridy = 0;
		c2.fill = GridBagConstraints.VERTICAL;
		c2.anchor = GridBagConstraints.CENTER;
		c2.weightx = 0;
		
		c3.gridx = 2;
		c3.gridy = 0;
		c3.fill = GridBagConstraints.VERTICAL;
		c3.anchor = GridBagConstraints.CENTER;
		c3.weightx = 0;
		
		c4.gridx = 0;
		c4.gridy = 1;
		c4.fill = GridBagConstraints.BOTH;
		
		c5.gridx = 1;
		c5.gridy = 1;
		c5.fill = GridBagConstraints.BOTH;
		
		c6.gridx = 2;
		c6.gridy = 1;
		c6.fill = GridBagConstraints.BOTH;
		
		
		this.add(above1, c1);
		this.add(above2, c2);
		this.add(above3, c3);
		this.add(board1, c4);
		this.add(board2, c5);
		this.add(board3, c6);
		
		boards.add(board1);
		boards.add(board2);
		boards.add(board3);
		aboveBoards.add(above1);
		aboveBoards.add(above2);
		aboveBoards.add(above3);
		
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
	 * 
	 * @return
	 */
	public AboveFingerBoard getCurrentAboveBoard()
	{
		return aboveBoards.get(currentIndex);
	}
	
	
	
	public void giveAccess(BigScore access)
	{
		scoreAccess = access;
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
		for(AboveFingerBoard ab : aboveBoards)
		{
			ab.clear();
		}
		while(boards.size() > 3)
		{
			boards.remove(3);
		}
		while(aboveBoards.size() > 3)
		{
			aboveBoards.remove(3);
		}
		while (this.getComponentCount() > 6)
		{
			this.remove(6);
		}
		currentIndex = 0;
		numBoards = 3;
		boards.get(0).setIsCurrentFingerBoard(true);
		this.repaint();
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
		GridBagConstraints c2 = new GridBagConstraints();
		FingerBoard newBoard = new FingerBoard();
		AboveFingerBoard newAbove = new AboveFingerBoard();
		
		boards.add(newBoard);
		aboveBoards.add(newAbove);
		giveInstrument();
		//System.out.println("Adding a board");
		
		c1.gridx = numBoards;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.VERTICAL;
		c1.anchor = GridBagConstraints.CENTER;
		c1.weightx = 0;
		this.add(newAbove, c1);
		c2.gridx = numBoards++;
		c2.gridy = 1;
		c2.fill = GridBagConstraints.BOTH;
		this.add(newBoard, c2);
		
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
			//System.out.println("boARDS size is now:" + boards.size());
		}
		while(aboveBoards.size() > index)
		{
			aboveBoards.remove(index);
			//System.out.println("above size is now: " + aboveBoards.size());
		}
		while (this.getComponentCount() > index*2) // TODO not sure about that code
		{
			//System.out.println("count = " + this.getComponentCount() + " and index*2 = " + index*2);
			this.remove(index*2);
		}
		numBoards = index;
		addABoard();
		addABoard();
		addABoard();
		currentIndex = index;
		FingerBoard currBoard = boards.get(index - 1); // could replace TODO by getCurrentBoard()
		
		scoreAccess.jumpToMasterIndex(currBoard.getMasterIndex());
		
		
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
	            		JPopupMenu menu = new JPopupMenu();
	            		JMenuItem removeRight = new JMenuItem("Remove On Right (double-click)");
	            		removeRight.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								deleteBoardsFrom(a);
							}
						});
	            		menu.add(removeRight);
	            		menu.show(evt.getComponent(), evt.getX(), evt.getY());
	            	}
	            }
	        });
		}
	}

	/**
	 * When this method is called, the Current Board represents an extension that does not link with the ones before it.
	 * This method takes the current board as a basis, and goes left to make all the previous boards link with
	 * that last one. That way, the boards do not show a cut. This is the only method that makes use of 
	 * the fact that FingerBoards keep their whole ArrayList of FingerTrails in memory. 
	 * FingerTrail ft: FingerTrail which we want all the boards to connect to. 
	 */
	public void migratePreviousBoardsToAValidExtension(FingerTrail ft) {
		int index = currentIndex-1;
		FingerTrail ftModel = ft.copy();
		while(boards.get(index).representsAnExtension())
		{
			ArrayList<FingerTrail> tList = boards.get(index).listOfTrailsFromThisExtension;
			int validIndexInList = -1;
			for(int i = 0; i<tList.size(); i++)
			{
				if(ftModel.isExtensionOf(tList.get(i)))
				{
					validIndexInList = i;
				}
			}
			ftModel = tList.get(validIndexInList);
			boards.get(index).setFingerTrail(ftModel);
			
			
			
			index -= 1;
		}
		
	}
	
}

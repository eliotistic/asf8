package hello;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
	private ArrayList<Integer> freezesList = new ArrayList<Integer>();
	public int numBoards = 0;
	
	public int currentIndex = 0; // Index where we will modify the next FingerBoard
	
	private boolean isAnExtension = false;
	private int indexStartExtension = -1;
	private int masterIndex = -1;
	private Extension extension;
	
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
	
	public void save()
	{
		if(extension != null)
		{
			String extensionString = "ind " + extension.getIndex() + " len " + extension.getLength();
			BigGui.saveExtension(extensionString);
		}
	}
	
	/**
	 * Try to freeze; the freeze will be done only if there is currently an extension being made
	 * @return
	 */
	public boolean freeze()
	{
		if(isAnExtension)
		{
			extension = makeAnExtension(indexStartExtension, currentIndex);
			System.out.println("EXTENSION: " + extension);
		}
		return isAnExtension;
	}
	
	public Extension makeAnExtension(int start, int end)
	{
		Extension ext;
		System.out.println("Making an extension from " + start + " to " + end);
		int length = end-start;
		int masterInd = masterIndex;
		ext = new Extension(masterInd, length);
		return ext;
	}
	
	public void invalidateExtension()
	{
		isAnExtension = false;
		indexStartExtension = -1;
		masterIndex = -1;
		System.out.println("Extension invalidated");
	}
	
	public void addFingerTrail(FingerTrail ft, int masterInd)
	{
		masterIndex = masterInd;
		System.out.println("The master index is: " + masterIndex);
		if(indexStartExtension == -1) // if we are starting a new extension, we change the index of start
		{
			indexStartExtension = currentIndex;
		}
		addABoard();
		boards.get(++currentIndex).setFingerTrail(ft);
		boards.get(currentIndex).setIsCurrentFingerBoard(true);
		boards.get(currentIndex > 0 ? currentIndex-1 : currentIndex).setIsCurrentFingerBoard(false);
		isAnExtension = true;
	}
	
	private void addABoard()
	{
		GridBagConstraints c1 = new GridBagConstraints();
		FingerBoard newBoard = new FingerBoard();
		boards.add(newBoard);
		giveInstrument();
		System.out.println("Adding a board");
		
		c1.gridx = numBoards++;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.BOTH;
		this.add(newBoard, c1);
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
	
	
	//code for precedent fingerboardPanel:
	/*public class FingerboardPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		JCheckBox fingerTrails;
		
		
		public FingerboardPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEtchedBorder());
			fingerTrails = new JCheckBox("Fingering trails");
			add(fingerTrails);
			add(Box.createVerticalGlue());
			fingerTrails.setSelected(false);
		}
	}*/
	
}

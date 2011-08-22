package computingmusic;

import java.awt.Dimension;

import javax.swing.JLabel;

public class AboveFingerBoard extends JLabel
{

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_TEXT = "def";
	
	public AboveFingerBoard()
	{
		this.setSize(new Dimension(12, 10));
		this.setText(DEFAULT_TEXT);
	}
	
	public void setFraction(int up, int down)
	{
		this.setText(up + "/" + down);
	}
	
	public void clear()
	{
		this.setText(DEFAULT_TEXT);
	}
	
}

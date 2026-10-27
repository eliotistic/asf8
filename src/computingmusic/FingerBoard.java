
package computingmusic;

/**
 *
 * @author eliot
 */

import java.awt.*;
//import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import computingmusic.utils.BvMap;
import computingmusic.utils.StringFingeringConstants;

/*
 * The FingerBoard is the label that displays a representation of the instrument (Central in the main frame). 
 */
public class FingerBoard extends JLabel {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4218158232921438281L;
	private boolean isCurrentFingerBoard = false;
	
	public boolean forward = true; // we hit the "next" button rather than prev
	                          // fingertrails wants to know.
	boolean fingerTrailsOn; 
	int problemType;
    Instrument instrument;
    Dimension size = new Dimension();
    ArrayList<Point> ps = new ArrayList<Point>(); // apparently not used anymore
    Point[] ps1; // regular points, and open strings
    Point[] ps2; // green points, lifted fingers
    Point[] psCom; // no idea.
    Point[] grayNotes; // no idea either.
    
    //jb
    ArrayList<FingerTrail> listOfTrailsFromThisExtension = new ArrayList<FingerTrail>();
    
    // trademark of JB Corp.
    public Point[][] noteCoordinates; 
    private int masterIndex;
    private boolean representsAnExtension = false;
    private int extensionSize = 1;
    private int givenTrailIndex; // given only when we leave this board for the next one.
    
	
	private ArrayList<ColoredNote> trails;
    public static final int DOT = 12; // size, diameter of the notes (circles)
    private static final int X_BORDER = 10;
    private static final int Y_BORDER = 10;

    // Color boardcolor = Color.decode("F3DEAC");
    Color boardColor = new Color (0xF3DEAC);
    Color black = Color.BLACK;
    Color red = Color.RED;
    Color green = new Color(0x099913);
    Color blue = Color.BLUE;
    Color bgreen = new Color(0x11C7D1);
    Color gray = new Color(0x262626);
    
    public void setRepresentsAnExtension(boolean rep)
    {
    	representsAnExtension = rep;
    }
    
    public boolean representsAnExtension() {
		return representsAnExtension;
	}
    
    public int getExtensionSize()
    {
    	return extensionSize;
    }
    
    public void setExtensionSize(int s)
    {
    	extensionSize = s;
    }
    
    public int getMasterIndex() {
		return masterIndex;
	}
    
    public void setMasterIndex(int ind)
    {
    	masterIndex = ind;
    }
    
    public Point[][] getNoteCoordinates() {
		return noteCoordinates;
	}
    public boolean isCurrentFingerBoard() {
		return isCurrentFingerBoard;
	}
	public void setIsCurrentFingerBoard(boolean bool)
	{
		isCurrentFingerBoard = bool;
		repaint();
	}

	public void giveTrailIndex(int index)
	{
		givenTrailIndex = index;
	}
	
	public int getTrailIndex()
	{
		return givenTrailIndex;
	}
   
    public FingerBoard() {
    	javax.swing.ToolTipManager.sharedInstance().registerComponent(this);
    	trails = new ArrayList<ColoredNote>();
    	fingerTrailsOn = false;
    	
    }
    
    /** The items of the piece (as the engine holds them, with the score's own spelling of each note); set by the panel. */
    public java.util.function.Supplier<java.util.List<ChordFingering>> chords;

    /**
     * The spellings, in ABC notation, that the notes of this board's items give to `pitch`: the score's own
     * (a note the red circles stand for).  Empty if no note of those items has that pitch.  The items are the
     * ones from the board's first item for its extension size.  Complexity: O(items x notes).
     */
    private java.util.List<String> scoreSpellings(int pitch) {
    	java.util.List<String> out = new java.util.ArrayList<String>();
    	java.util.List<ChordFingering> list = chords == null ? null : chords.get();
    	if (list == null) {
    		return out;
    	}
    	for (int i = Math.max(0, masterIndex); i < Math.min(list.size(), masterIndex + Math.max(1, extensionSize)); i++) {
    		Keynum.K[] ks = list.get(i).ks;
    		if (ks == null) {
    			continue;
    		}
    		for (Keynum.K k : ks) {
    			if (k.pitch == pitch) {
    				int alter = k.name.indexOf('#') > 0 ? 1 : k.name.indexOf('b') > 0 ? -1 : 0;
    				String n = AbcPitch.name(pitch, alter);
    				if (!out.contains(n)) {
    					out.add(n);
    				}
    			}
    		}
    	}
    	return out;
    }

    /** The hand model used to say which finger a stopped point of this board would be: violin, default limits. */
    private static computingmusic.hand.FrameTest fingerModel;

    /**
     * What is under the mouse: the disc nearest to it, as its pitch in ABC notation, its string and height (semitones
     * above the open string, with its distance from the nut in mm), and, if it is one of the points shown on this
     * board, the finger the hand model gives it: the stopped heights shown are held by fingers in order from the
     * nut.  Empty away from a disc.  Complexity: O(strings x heights).
     */
    @Override
    public String getToolTipText(MouseEvent evt)
    {
    	if (noteCoordinates == null || instrument == null) {
    		return null;
    	}
    	int bestH = -1, bestS = -1;
    	double best = (DOT / 2 + 3) * (DOT / 2 + 3);
    	for (int h = 0; h < noteCoordinates.length; h++) {
    		for (int st = 0; st < noteCoordinates[h].length; st++) {
    			Point c = noteCoordinates[h][st];
    			if (c == null) {
    				continue;
    			}
    			double d = (c.x - evt.getX()) * (double) (c.x - evt.getX()) + (c.y - evt.getY()) * (double) (c.y - evt.getY());
    			if (d < best) {
    				best = d;
    				bestH = h + 1;
    				bestS = st;
    			}
    		}
    	}
    	if (bestS < 0) {
    		return null;
    	}
    	int pitch = instrument.openStringPitch(bestS) + bestH;
    	String[] names = { "G", "D", "A", "E" };
    	String stringName = (instrument.nStrings == 4 && instrument.instr == Instrument.Instr.VN ? names[bestS] : "string " + bestS) + " string";
    	computingmusic.hand.Geometry g = computingmusic.hand.Geometry.VIOLIN;
    	// a red circle is a note of the score: its pitch is spelled as the score spells it.  A disc with no note on it
    	// has no spelling of its own, so a black key is given both.
    	java.util.List<String> spelled = scoreSpellings(pitch);
    	String pitchText;
    	if (!spelled.isEmpty()) {
    		pitchText = String.join(" and ", spelled);
    	} else if (TrailRun.whiteKey(pitch)) {
    		pitchText = AbcPitch.name(pitch, 0);
    	} else {
    		pitchText = AbcPitch.name(pitch, 1) + " or " + AbcPitch.name(pitch, -1);
    	}
    	StringBuilder s = new StringBuilder("<html>");
    	s.append("<b>").append(pitchText).append("</b><br>").append(stringName).append(instrument.instr == Instrument.Instr.VN ? "" : "").append("<br>height ").append(bestH);
    	if (instrument.instr == Instrument.Instr.VN) {
    		s.append(String.format("<br>%.0fmm", g.distance(bestH)));
    	}
    	boolean shown = false;
    	java.util.TreeSet<Integer> held = new java.util.TreeSet<Integer>();
    	if (ps1 != null) {
    		for (Point p : ps1) {
    			if (p.y > 0) {
    				held.add(p.y);
    			}
    			if (p.x == bestS && p.y == bestH) {
    				shown = true;
    			}
    		}
    	}
    	if (shown && instrument.instr == Instrument.Instr.VN) {
    		if (fingerModel == null) {
    			fingerModel = new computingmusic.hand.FrameTest(g, new computingmusic.hand.HandParams(), true);
    		}
    		int[] hs = new int[held.size()];
    		int i = 0;
    		for (int h : held) {
    			hs[i++] = h;
    		}
    		computingmusic.hand.FrameTest.Assignment a = fingerModel.assign(hs);
    		if (a != null) {
    			int k = 0;
    			for (int h : held) {
    				if (h == bestH) {
    					s.append("<br>finger ").append(a.fingers[k]).append(a.forced() ? "" : "?");
    				}
    				k++;
    			}
    		}
    	} else if (shown) {
    		s.append("<br>down");
    	}
    	return s.append("</html>").toString();
    }
   
    
    
    public Dimension getMaximumSize() {
    	return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    
    public Dimension getPreferredSize() {
    	return new Dimension(70, 600); // This is rigidly setting the dimensions of the FingerBoard
    }
    
    public Dimension getMinimumSize()
    {
    	return new Dimension(70, 500);
    }
    
    
    public void setFingerTrails (boolean state){
    	trails.clear();
    	repaint();
    	fingerTrailsOn = state;
    }
    
    public void setNext() {
    	forward = true;
    }
    public void setPrev() {
    	forward = false;
    }
    
    
    
/*
    public static Color xxxlighter (Color color, double fraction)
    {
      int red   = (int) Math.round (color.getRed()   * (1.0 + fraction));
      int green = (int) Math.round (color.getGreen() * (1.0 + fraction));
      int blue  = (int) Math.round (color.getBlue()  * (1.0 + fraction));

      if (red   < 0) red   = 0; else if (red   > 255) red   = 255;
      if (green < 0) green = 0; else if (green > 255) green = 255;
      if (blue  < 0) blue  = 0; else if (blue  > 255) blue  = 255;    

      int alpha = color.getAlpha();

      return new Color (red, green, blue);
    }
*/    
    
    public static Color lighter (Color color, double fraction)
    {
      int red   = color.getRed();
      int green = color.getGreen();
      int blue  = color.getBlue();
      int alpha = (int) Math.round(color.getAlpha() * fraction);
     
      return new Color (red, green, blue, alpha);
    }

    public class ColoredNote extends Point {
    	/**
		 * 
		 */
		private static final long serialVersionUID = -3423657721481511641L;
		Color color;
    	int date;
    	public ColoredNote(Point p) {
    		date = 0;
    		x = p.x;
    		y = p.y;
			color = Color.BLACK; // gray();
    	}
    	public void redate(){
    		date++;
    	}
    	public void lighten () {
    		color = lighter(color, 0.5);
    	}
    }
    
   
    public void colorNotes(Graphics2D g2, int h, int[] xPositions,
			Point[] noteCoords, Color openColor, Color stopColor) {
		// g2.setColor(Color.RED);
		if (noteCoords == null)
			return;
		for (Point p : noteCoords) {

			//int notex = xPositions[3 - p.x];
			int notex = xPositions[p.x];
			//int notey = notePos(p.y, yborder, h - yborder * 2);
			int notey = instrument.noteYPosition(p.y, Y_BORDER, h - Y_BORDER * 2);
			if (p.y == 0) {
				g2.setColor(stopColor);
			} else {
				g2.setColor(openColor);

			}
			g2.fillOval(notex - DOT / 2, notey - DOT / 2, DOT, DOT);
		}
	}
    
    public void drawTrails(Graphics2D g2, int h, int[] xPositions) {
		if (trails == null)
			return;
		if (!fingerTrailsOn)
			return;
		for (ColoredNote cn : trails) {
			//int notex = xPositions[3 - cn.x];
			int notex = xPositions[cn.x];
			int notey = instrument.noteYPosition(cn.y, Y_BORDER, h - Y_BORDER * 2);
			g2.setColor(cn.color);
			g2.fillOval(notex - DOT / 2, notey - DOT / 2, DOT, DOT);
		}
	}
    
    
    
    //=========================================================== paintComponent
    
    private void drawFingerboard(Graphics g, int[] xPositions) {
    	//System.out.println("Drawing...");
    	int maxInterval = instrument.stringRange;
    	noteCoordinates = new Point[StringFingeringConstants.STRING_RANGE][instrument.nStrings];
    	//System.out.println("Draw FB for " + instrument.instr + " " + maxInterval );
    	
    	int panelWidth = getWidth();
		int panelHeight = getHeight();
		int internalWidth = panelWidth - X_BORDER * 2;
		int internalHeight = panelHeight - Y_BORDER * 2;
		
		Graphics2D g2 = (Graphics2D) g; // Downcast.
		

		// draw background
		g2.setColor(boardColor);
		g2.fillRect(X_BORDER, Y_BORDER, internalWidth, internalHeight);

		// draw frame around background
		g2.setColor(Color.BLACK);
		g2.drawRect(X_BORDER, Y_BORDER, internalWidth, internalHeight);

		// int hi = 10;
		//Stroke s2 = g2.getStroke();
		
		// add bars for octaves, increase readability
		g2.setStroke(new BasicStroke(1));
		for (int oct = 12 ; oct <= instrument.stringRange; oct = oct + 12){
			int y12 = instrument.noteYPosition(oct, Y_BORDER, internalHeight);
			g2.drawLine(X_BORDER, y12, X_BORDER + internalWidth, y12);
		//g2.setStroke(new BasicStroke(1));
		}
		
		// draw strings + discs
		int index = 0;
		for (int stringx : xPositions) {
			// draw the vertical strings (the "real" strings)
			g2.setColor(Color.BLACK);
			g2.drawLine(stringx, Y_BORDER, stringx, panelHeight - Y_BORDER);
			
			//Draw the discs
			for (int interval = 1; interval <= maxInterval; interval++) {
				//interval: linearly increasing int, up to 25
				//yborder: vertical starting position
				//internalHeight: height until vertical ending position.
				// so int y describes the locations at which the discs will be drawn
				int y = instrument.noteYPosition(interval, Y_BORDER, internalHeight);
				
				g2.setColor(Color.WHITE);
				g2.fillOval(stringx - DOT / 2, y - DOT / 2, DOT, DOT);
				g2.setColor(Color.BLACK);
				g2.drawOval(stringx - DOT / 2, y - DOT / 2, DOT, DOT);
				
				noteCoordinates[interval-1][index] = new Point(stringx, y);
				//System.out.println("adding: " + (interval-1) + " " + index);
			}
			
			index++;
		}
		for(int i = 0; i<noteCoordinates.length; i++)
		{
			for(int j = 0; j<noteCoordinates[0].length; j++)
			{
				//System.out.println("NoteCoordinates at " + i + ", " + j + " = " + noteCoordinates[i][j]);
			}
		}
		
		//TODO
		if(isCurrentFingerBoard)
		{
			g2.setColor(Color.yellow);
			g2.fillOval(panelWidth/2 - DOT / 2, panelHeight/2 - DOT / 2, DOT, DOT);
			g2.setColor(Color.BLACK);
			g2.drawOval(panelWidth/2 - DOT / 2, panelHeight/2 - DOT / 2, DOT, DOT);
		}
		
    }
    
    @Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g; // Downcast.

		//Set the four positions of the Fingerboard, horizontally
		int width = getWidth(); // Get height and width
		int height = getHeight();
		int internalWidth = width - X_BORDER * 2;
		int I = X_BORDER;
		int II = X_BORDER + (internalWidth / 3);
		int III = X_BORDER + ((2 * internalWidth) / 3);
		int IV = width - X_BORDER;
		int[] xPositions = { I, II, III, IV };

		drawFingerboard(g, xPositions);
		colorNotes(g2, height, xPositions, ps1, red, blue);
		colorNotes(g2, height, xPositions, ps2, green, bgreen);
		colorNotes(g2, height, xPositions, psCom, black, black);
		colorNotes(g2, height, xPositions, grayNotes, gray, blue);

	}

	
   
    /*
    public void drawString (Graphics2D g2){
        
    }
    */

    /*
      public void addDot(int x, int y) {
      Point p = new Point(x, y);
      if (!ps.contains(p)) {
      ps.add(p);
      }
      repaint();
      }
    */

    
    
    public void oldsetFingers(Point[] stops) {
        this.ps.clear();
        for (Point p : stops) {
            ps.add(p);
        }
        repaint();

    }
    
    public void shortenTrail () {
    	int MAXTRAIL = 3;
    	int sz = trails.size();
    		while (sz>MAXTRAIL){
    			trails.remove(0);
    			sz--;
    			
    		
    	}
    }
    
    public void lightenTrail() {
    	for (ColoredNote cn : trails) {
    		cn.lighten();
    	}
    }
    
    public void sayTrail(){
    	for (ColoredNote c : trails) {
    		System.out.println(c.color.toString());
    	}
    }
    
    
    /*
    public void updateTrail() {
		if (ps1 == null)
			return;		
		boolean added = false;
		// add new trail
		for (Point p : ps1) {
			if (p.y != 0) { // don't put open strings in trail
				added = true;
				ColoredNote cn = new ColoredNote(p);
				trails.add(cn);
			}
			if (added) {
				shortenTrail();
				lightenTrail();
			}
		}

		// sayTrail();
	}
    */
    
    public void clearTrails(){
    	trails.clear();
    	repaint();
    }
    
    
    
    public void setFingers(Point[] stops) {
    	//updateTrail();
    	ps1 = stops;
    	ps2 = null;
    	isCurrentFingerBoard = true;
    	repaint();

    }
    /*
    public void setGray(Point[] ps){
    	grayNotes = ps;
    	repaint();
    }
    */
    	
    
    // deprecated
    public void setFingers(Transitions.FingeringPair f) {
        ps1 = f.f0Coords;
        ps2 = f.f1Coords;
        psCom = f.comCoords;
        repaint();

    }
      
    
    // using this in analysis panel
    // deprecated
    public void setFingers(ArrayList<Point> stops) {
        Point[] arr = new Point[stops.size()];
        for (int i=0; i<stops.size(); i++) {
        	arr[i] = stops.get(i);
        }
        setFingers(arr);
    }
    
    public void setBvMap(BvMap heightMap){
    	Point[] stops = heightMap.toPointArray();;
    	setFingers(stops);
    }
    
    // as of aug 26 2010 this is our current interface. 
    public void setFingerTrail (FingerTrail ft){
    	ps1 = ft.redPoints.toarray();
    	ps2 = ft.grayPoints.toarray();
    	isCurrentFingerBoard = true;
    	repaint();
    }
    
    /*
     * Made by JB. No proof that it works, though.
     */
    public boolean hasActiveNoteInColumn(int x) {
    	if(ps1 != null)
    	{
    		for(Point pt : ps1)
			{
    			System.out.println("Point: " + pt);
				if(pt.x == x)
				{
					return true;
				}
			}
    	}
		
		return false;
	}
    
    public boolean hasActiveNote(Point noteCoords) {
		for(Point pt : ps1)
		{
			if(pt.x == noteCoords.x && pt.y == noteCoords.y)
			{
				return true;
			}
		}
		return false;
	}
    
    /*
    // frequencies of pitches from a440 to a1760
   

    public static int[] aFreqs = {440, 466, 493, 523, 554, 587,622,
				  659, 698, 740, 784, 830,
				  880, 932, 987, 1046, 1108, 1174, 1244, 1318, 1396, 1480,
				  1568, 1661, 
				  1760, 1865, 1976, 2093,2217};

    //private double root12of2 = 1.05946309;
    
    public static int notePos(int interval, int y0, int y1) {
        double xlen = (y1-y0) * 1.33333;
        int freq = aFreqs[interval];
        double xx = xlen - ((xlen * 880) / (2 * freq));
        return y0 + (int)(Math.round(xx));
    }
*/
  
    public void clear() {
        this.ps.clear();
        ps1 = null;
        ps2 = null;
        psCom = null;
        grayNotes = null;
        trails.clear();
        representsAnExtension = false;
        isCurrentFingerBoard = false;
        extensionSize = 1;
        givenTrailIndex = -1;
        repaint();

    }

	public void giveArrayListOfTrails(ArrayList<FingerTrail> newActiveTrails) {
		listOfTrailsFromThisExtension = newActiveTrails;
	}
    


	
}



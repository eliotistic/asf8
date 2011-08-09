package hello;

//import hello.BigScore.ResultABC;

import hello.gui.AnnotatedBoxes;
import hello.utils.MySeq;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JScrollPane;
//import java.util.Iterator;
//import java.util.Vector;

//import javax.crypto.spec.PSource;
//import javax.swing.Scrollable;

//import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import abc.notation.*;
//import abc.notation.Tune.Music;
import abc.ui.swing.*;
import abc.ui.scoretemplates.*;

public class BigScoreComponent extends JScoreComponent 
			//implements Scrollable, MouseMotionListener 
			{
	
	private static final long serialVersionUID = 1L;
	private boolean analyze;
	Tune tune;
	ArrayList<MusicElement> noteList;
	ArrayList<Integer> indexVector; // iv(N) is the index of the musicElet one of our things
		                            // corresponds to
	
	Rectangle xtraRect = new Rectangle(0, 0, 1, 1);
	Rectangle xRect = new Rectangle(0, 0, 1, 1);
	BufferedImage buf;
	String[] iStrings;
	String[][] romanNames;
	Clef myClef;
	float m1Top = 3f;
	float mTop = 40f; // had 15f
	float mBot = 0f; // had 15f
	float mRight = 0f; // 40f
	float mLeft = 15f; // 40f
	
	//Color posColor = new Color(0x52C5FF);
	Color posColor = new Color(0x005200); // greenish color.
	public ArrayList<AnnotatedBoxes> annotatedBoxesList;
	
	private JScrollPane accessToPane; // jb
	
	// boxed notes are either chords or chordable sequences.
	
	@SuppressWarnings("unused")
	private MusicElement boxedNote;
	private ArrayList<MusicElement> boxedNotes;
	
	private ArrayList<ArrayList<MusicElement>> boxedNotesList;
	private ArrayList<MusicElement> redBoxedNotes;
	
	//private PointList blueBoxedInds; 
	
	int boxedNoteMargin = 4;
	int positionalMargin = boxedNoteMargin + 2;
	int redBoxMargin = positionalMargin + 2;
	// lines = tmp working array that splits a list of music
	// elements into a list of lists, representing line breaks.
	private ArrayList<ArrayList<MusicElement>> lines;
	  // temporarily copy to here; move to lines when break encountered. 
	private ArrayList<MusicElement> line;
	
	//private Seq positionalSeq;
	private MySeq positionalSeq2;

	
	public BigScoreComponent() {
		setsize();
		boxedNotes = new ArrayList<MusicElement>();
		boxedNotesList = new ArrayList<ArrayList<MusicElement>> ();
		redBoxedNotes = new ArrayList<MusicElement>();
		//blueBoxedInds = new PointList();
		annotatedBoxesList = new ArrayList<AnnotatedBoxes>();
		noteList = new ArrayList<MusicElement>();
		indexVector = new ArrayList<Integer>();
		lines = new ArrayList<ArrayList<MusicElement>>();
		line =  new ArrayList<MusicElement>();
		analyze = true;

	}
/*
	public BigScoreComponent(Tune t, ArrayList<MusicElement> notes) {
		super();
		setsize();

		tune = t;
		noteList = notes;
		analyze = true;
		// xtraRect = new Rectangle(0,0,1,1);
		;

	}
	*/
	private MusicElement getMusicElement(int index){
		return noteList.get(index);
	}
	private ArrayList<MusicElement> getMusicElements(int start, int stop){
		//System.out.println("getMusicElements NoteList size: " + noteList.size());
		ArrayList<MusicElement> m = new ArrayList<MusicElement>();
		for (int i = start; i<stop+1; i++) {
			m.add(noteList.get(i));
		}
		return m;
	}
	// 
	public void myDrawRect(Rectangle r, Graphics g) {
		// g.setPenColor(new Color(255, 0, 255));	
		g.drawRect(r.x, r.y, r.width, r.height);
		//accessToPane.getViewport().scrollRectToVisible(r); // TODO trying.
		if(!accessToPane.getViewport().getViewRect().contains(r))
		{
			accessToPane.getVerticalScrollBar().setValue(r.y > 100 ? r.y - 100 : r.y /2);
			//TODO modify this code, perfect it; also, can't scroll down and leave the square.
		}
	}
	/*
	public void myDrawRect(Rectangle2D r, Graphics g) {
		// g.setPenColor(new Color(255, 0, 255));
		
		// g.drawRect(r.x, r.y, r.width, r.height);
	}
	*/
	/*
	public void setPositionalSeq (Seq s) {
		if (s==null) return;
		int margin = boxedNoteMargin + 2; 
		positionalSeq = s;
		// add rectangles
		positionalSeq.lineRects = lineRectangles(s.musicElements, margin);
		repaint();
		
	}
	*/
	
	public void setPositionalSeq2 (MySeq s) {
		if (s==null) return;
		int margin = boxedNoteMargin + 2; 
		
		// add rectangles
		ArrayList<MusicElement> sub = getMusicElements(s.start, s.stop);
		// System.out.println("Collected subset of " + sub.size());
		positionalSeq2 = s;
		positionalSeq2.lineRects = lineRectangles(sub, margin);
		
		
		repaint();
		
	}
	
	public void turnoffPositions () {
		
		//positionalSeq = null;
		positionalSeq2 = null;
		
		repaint();
		
	}
	
	
	
	public void setABCTune (AbcReader abc){
		super.setTune(abc.tune);
		tune = abc.tune;
		noteList = abc.noteList;
		clearBoxedNotes();
		clearPSeq();
	}
	/*
	public void setTune(Tune t) {
		super.setTune(t);
		tune = t;
		//makeNoteList();
		
		clearPSeq();
	}
	*/
	public void setBoxedNote(MusicElement e) {
		boxedNote = e;
		repaint();
	}
	public void setBoxedNote(int index){
		boxedNote = noteList.get(index);
		repaint();
		
	}
	public void clearBoxedNotes () {	
		boxedNotes.clear();
		boxedNotesList.clear();
		redBoxedNotes.clear();
		annotatedBoxesList.clear();
	}
	public void clearBoxedNote () {
		
		boxedNote = null;
	}
	
	public void clearPSeq (){
		//positionalSeq = null;
		positionalSeq2 = null;
	}
	
	public void addAnnotatedBoxes (AnnotatedBoxes aa) {
		annotatedBoxesList.add(aa);
		System.out.println("addAnnotatedBoxes Total rects: " + annotatedBoxesList.size());
		repaint();
	}
	
	public void clearAnnotationBoxes()
	{
		annotatedBoxesList.clear();
		repaint();
	}
	
	// return rectangle that surrounds all elements. 
	// There are no line breaks here. 
	private Rectangle elementBoundingBox (MusicElement note, int b) {
		JScoreElement e = getRenditionElementFor(note);
		Rectangle2D r = e.getBoundingBox();
		int x = (int) r.getX();
		int h = (int) r.getHeight();
		int w = (int) r.getWidth();
		int y = (int) r.getY();
		
		
		return  new Rectangle(x-b, y-b, w+2*b, h+2*b);
		
	}
	private Rectangle elementsBoundingBox (ArrayList<MusicElement> es, int b) {
		int sz = es.size();
		int topy = Integer.MAX_VALUE;
		int boty = 0;
		
		// int y0 = 0;
		// need 1st & last x;
		MusicElement in = es.get(0);
		if (in == null) return null;
		MusicElement ou = es.get(sz - 1);
		Rectangle2D inRect = getRenditionElementFor(in)
				.getBoundingBox();
		Rectangle2D ouRect = getRenditionElementFor(ou)
				.getBoundingBox();

		int xin = (int) inRect.getX();
		int xou = (int) ouRect.getX();
		int wou = (int) ouRect.getWidth();

		// now get min topy, max bot y.
		for (MusicElement elt : es) {
			JScoreElement e = getRenditionElementFor(elt);
			Rectangle2D r = e.getBoundingBox();

			int h = (int) r.getHeight();
			int y = (int) r.getY();
			int bot = y+h;
			//System.out.println("topx: " + topx + " X: " + x);
			if (y < topy) topy = y;
			if (bot > boty) boty = bot;
		}
		;
		int w = xou - xin + wou;
		int h = boty - topy;
		
		Rectangle r = new Rectangle(xin-b, topy-b, w+2*b, h+2*b);
		//g.drawRect(xin-b, topy-b, w+2*b, h+2*b);
		return r;
	}
	
	
	
	
	
	// get X drawing value for this element.
	private int musicElementX (MusicElement e) {
		Rectangle2D r = getRenditionElementFor(e).getBoundingBox();
		return (int) r.getX();
	}
	public int musicElementX (int i){
		return musicElementX(getMusicElement(i));
	}
	
	
	
	
	
	
	private void bnMovetoList(){
		// move notes from boxedNotes to a new list in BoxedNotesList,
		if (boxedNotes.size() == 0){			
		} else {
		ArrayList<MusicElement> notes = new ArrayList<MusicElement>();
		for (MusicElement e : boxedNotes) {
			notes.add(e);
		};
		boxedNotesList.add(notes);
		boxedNotes.clear();
		}
	}
	
	
	
	private void lineMovetoLines(){
		// move notes from boxedNotes to a new list in BoxedNotesList,
		if (line.size() == 0){			
		} else {
			// make tmp copy
		ArrayList<MusicElement> notes = new ArrayList<MusicElement>();
		for (MusicElement e : line) {
			notes.add(e);
		};
		lines.add(notes);
		line.clear();
		}
	}
	
	// takes list of notes, breaks into 2d arr with line breaks.
	private void lineBreaks(ArrayList<MusicElement> es) {
		lines.clear();
		line.clear();

		int currX = musicElementX(es.get(0));

		for (MusicElement e : es) {
			int x = musicElementX(e);
			if (x < currX) {
				// finish current list
				lineMovetoLines();
			}
			currX = x;
			line.add(e);
		}
		;
		// do last
		lineMovetoLines();

		// return lines;
	}
	
	public void setBoxedNotes(Point io){
		setBoxedNotes(getMusicElements(io.x, io.y));
	}
	public void setBoxedNotes(int index){
		setBoxedNotes(getMusicElements(index, index));
	}
	public void setBoxedNotes(int start, int stop){
		setBoxedNotes(getMusicElements(start,stop));
	}
	public void setBoxedNotes(ArrayList<MusicElement> es) {
		//boxedNote = null;
		
		
		boxedNotes.clear();
		boxedNotesList.clear();
		
		// first X position
		int currX = musicElementX(es.get(0));
		
		for (MusicElement e : es){
			int x = musicElementX(e);
			if (x < currX){
				// finish current list
				bnMovetoList();
			}
			currX = x;
			boxedNotes.add(e);
		};
		bnMovetoList();
		repaint();
	}
	
	public void addRedNote (int index){
		redBoxedNotes.add(getMusicElement(index));
	}
	@SuppressWarnings("unused")
	private void addRedNote (MusicElement note){
		redBoxedNotes.add(note);
		
	}
	
	
	public void setBoxedNotesHead(ArrayList<MusicElement> es) {
		boxedNote = null;
		boxedNotes.clear();
		boxedNotesList.clear();
		boxedNotes.add(es.get(0));
		bnMovetoList();
		repaint();
	}
	
	// given list of elts, fingure out line breeaks and return
	// bounding rectangles. 
	private ArrayList<Rectangle> lineRectangles(ArrayList<MusicElement> es, int b){
		ArrayList<Rectangle> rs = new ArrayList<Rectangle>();
		lineBreaks(es); // ans in lines
		for  ( ArrayList<MusicElement> boxedNotes : lines ) {
			Rectangle r = elementsBoundingBox(boxedNotes, b);
			rs.add(r);
		}
		
		return rs;
	}
	
	public ArrayList<Rectangle> lineRectanglesofPoint (Point p, int margin){
		return lineRectangles(getMusicElements(p.x, p.y), margin);
	}
	
	private void evenNewerDrawBoxedNotes(Graphics g){
		int b = boxedNoteMargin;
		if (boxedNotesList == null) {
			return;
		}
		for  ( ArrayList<MusicElement> boxedNotes : boxedNotesList ) {
			Rectangle r = elementsBoundingBox(boxedNotes, b);
			myDrawRect(r, g);
		}
		
	}
	private void drawRedNotes(Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		int b = redBoxMargin;
		
		Color old = g2.getColor();
		g2.setColor(Color.RED);
		Stroke s2 = g2.getStroke();
		g2.setStroke(new BasicStroke(2));
		
		for  ( MusicElement redNote : redBoxedNotes ) {
			//Rectangle2D r2 = getRenditionElementFor(redNote).getBoundingBox();
			Rectangle r = elementBoundingBox(redNote, b); 
		
			myDrawRect(r, g);
		};
		g2.setColor(old);
		g2.setStroke(s2);
		
	}
	
	private void drawAnnotatedBoxes (Graphics g) {
		for (AnnotatedBoxes a : annotatedBoxesList) {
			a.draw(g);
		}
	}
	
	private void drawPositionalSeq2(Graphics g) {
		// rectangles are stuffed in lineRects
		if (positionalSeq2 == null) return;
		Color old = g.getColor();
		g.setColor(posColor);
		for (Rectangle r : positionalSeq2.lineRects){
			myDrawRect(r, g);
		};
		
		// draw label
		Rectangle r0 = positionalSeq2.lineRects.get(0);
		
		String pos = positionalSeq2.positionString();
		g.drawString(pos, r0.x, r0.y-4);
		
		g.setColor(old);
	}
	
	
	
	
	/*
	private void setHorizontal(float f) {

		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_LEFT, f);
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_LEFT, f);
	}
	*/
	
	private void setsize() {
		// extra space between title & first staff
		getTemplate().setAttributeSize(ScoreAttribute.FIRST_STAFF_TOP_MARGIN,
				m1Top);
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_TOP, mTop);
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_RIGHT, mRight);
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_LEFT, mLeft);
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_BOTTOM, mBot);
		// space between staves
		getTemplate().setAttributeSize(ScoreAttribute.STAFF_LINES_SPACING, 3f);

	}

	
	/*
	public void addTune(Tune t) {

		tune = t;
	}
	*/

	// array of printable names from lowest note to highest.
	@SuppressWarnings("unused")
	private void setIStrings(String[] s) {
		iStrings = s;
	}

	@SuppressWarnings("unused")
	private void setromanNames(String[] s) {
		romanNames = new String[1][];
		romanNames[0] = s;
	}

	public void setromanNames(String[][] s) {
		romanNames = s;
	}

	public String getIString(int n) {
		if (iStrings == null)
			return "";
		else if (n - 1 > iStrings.length)
			return "";
		else
			return iStrings[n];
	}

	public String getRomanName(int scoreElt, int n) {
		if (romanNames == null)
			return "";
		else if (scoreElt - 1 > romanNames.length)
			return "";
		String[] s = romanNames[scoreElt];
		if (n - 1 > s.length)
			return "";
		else
			return s[n];
	}

	/*
	 * if minY is fixed in all chord notes, use maxY else use minY and don't ask
	 * me why. -- ok, I get it. The bounding box even for wholes has a stem --
	 * down if above midline, else up. so, if pitch is above midline, we use
	 * max, else min.
	 */
/*
	private double noteMaxY(Note note) {
		JScoreElement e = getRenditionElementFor(note);
		Rectangle2D r = e.getBoundingBox();
		return r.getMaxY();
	}
*/
	/*
	private boolean FixedMaxY(Note[] notes) {
		// boolean yes = false;
		double y = noteMaxY(notes[0]);
		for (int i = 1; i < notes.length; i++) {
			if (noteMaxY(notes[i]) != y) {
				return false;
			}
		}

		return true;
	}
	*/

	public void clear() {
		// Rectangle x = xRect;

	}

	static int width_extra = 100;
	
	public void sayTemplate() {
		String s = getTemplate().toString();
		System.out.println(s);
	}


	@Override
	public void paint(Graphics g) {
		// clearMyStuff(g);
		// super.refresh();

		super.paint(g);

		// Graphics2D g2 = (Graphics2D) g; // Downcast.

		if (tune == null) {
			// System.out.println("BigScoreComponent: No tune");
			return;
		};
		if (analyze) {
			// newdrawBoxedNotes(g);
			drawRedNotes(g);
			evenNewerDrawBoxedNotes(g);
			// drawPositionalSeq(g);
			drawPositionalSeq2(g);
			drawAnnotatedBoxes(g);
			
		}
		
		// repaint();
	}
	
	public void giveScrollPaneAccess(JScrollPane sPane) {
		accessToPane = sPane;	
	}

}

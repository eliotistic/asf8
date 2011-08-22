package computingmusic;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import abc.notation.*;
import abc.notation.Tune.Music;
import abc.ui.swing.*;
import abc.ui.scoretemplates.*;

public class MyJScoreComponent extends JScoreComponent {

	private static final long serialVersionUID = 1L;
	Tune tune;
	Rectangle xtraRect =  new Rectangle(0,0,1,1);
	Rectangle xRect =  new Rectangle(0,0,1,1);
	BufferedImage buf;
	//String[] iStrings;
	String[][] romanNames;
	Clef myClef;
	float mTop = 2f; // had 15f, put to 2f in order to enable resizing
	float mRight = 40f; //40f
	float mLeft = 0f; //40f
	
	boolean drawRomans = true;
	
	public void setTune (Tune t){
		super.setTune(t);
		tune = t;
		
	}
	
	public void setHorizontal(float f){
		
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_LEFT,
				f); 
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_LEFT,
				f); 
	}
	
	public void setsize (){
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_TOP,
				mTop); 
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_RIGHT,
				mRight); 
		getTemplate().setAttributeSize(ScoreAttribute.MARGIN_LEFT,
				mLeft); 
	}
	
	public MyJScoreComponent() {
		setsize();
		this.setMaximumSize(new Dimension(80, 60));
	}
	public MyJScoreComponent(Tune t) {
		super();
		setsize(); 

		tune = t;
		// xtraRect = new Rectangle(0,0,1,1);
		;

	}
	
	public void addTune (Tune t){
	
		tune = t;
	}
	
	
	/*
	// array of printable names from lowest note to highest.
	public void setIStrings (String[] s){
		iStrings = s;
	}
	*/
	
	public void setRomanNames (String[] s){
		romanNames = new String[1][];
		romanNames[0] = s;
	}
	
	public void setRomanNames (String[][] s){
		romanNames = s;
		
	}
	public void clearRomanNames(){
		romanNames = null;
	}

	
	/*
	public String getIString (int n){
		if (iStrings == null) return "";
		else if (n-1 > iStrings.length) return "";
		else return iStrings[n];
	}
	*/
	public String stringroman() {
		// 2 d arr, we just look in 1d
		if (romanNames == null) return "NULL" ;
		else {
		String s = "";
		for (String x : romanNames[0]){
			s = s + " " + x;
		}
		return s;
		}
	}
	public String getRomanName (int scoreElt, int n){
		/*
		String rstr = stringroman();
		System.out.println("getRomanName: scoreElt " + scoreElt 
				+ " n: " + n + "romans[1]: " + rstr);
		*/
		if (romanNames == null) 
			{ return "";
			} else if (scoreElt-1 > romanNames.length) { 
				return "";
			} else {
				String[] s = romanNames[scoreElt];
				if (n-1 > s.length) return "";
				else return s[n];
			}
	}
	
	/*
	 * if minY is fixed in all chord notes, use maxY else use minY and don't ask
	 * me why. -- ok, I get it. The bounding box even for wholes has a stem --
	 * down if above midline, else up. so, if pitch is above midline, we use max, else min.
	 */
	
	private double noteMaxY(Note note) {
		JScoreElement e = getRenditionElementFor(note);
		Rectangle2D r = e.getBoundingBox();
		return r.getMaxY();
	}

	private boolean FixedMaxY(Note[] notes) {
		//boolean yes = false;
		double y = noteMaxY(notes[0]);
		for (int i = 1; i < notes.length; i++) {
			if (noteMaxY(notes[i]) != y) {
				return false;
			}
		}

		return true;
	}
	public void clear() {
		//Rectangle x = xRect;
		
	}
	
	static int width_extra = 100;
	
	
	
	public void sayTemplate () {
		String s = getTemplate().toString();
		System.out.println(s);
	}
	
	
	
	@Override
	public void paint (Graphics g) {
		//clearMyStuff(g);
		//super.refresh();
		
		super.paint(g);
		//Graphics gc = extendGfx(g);
		//super.paint(g);
		Graphics2D g2 = (Graphics2D) g; // Downcast.
		
		// g. drawRect(clip.x, clip.y,clip.height,clip.width);
		if (tune == null) {
			//System.out.println("No tune");
		} else if (!drawRomans){
			
		}
		else {
			//System.out.println("Drawing tune...");
			Music m = tune.getMusic();
			@SuppressWarnings("rawtypes")
			Iterator  it = m.iterator();
			//xRect = g2.getClipBounds();
			
			int eltN = -1;
			
			while (it.hasNext()) {
				
				MusicElement elt = (MusicElement) it.next();
				//String[] romans = romanNames[eltN];
				if (elt instanceof MultiNote) {
					eltN++;
					
					
					Note[] notes = ((MultiNote) elt).toArray();
					FontMetrics fm = g2.getFontMetrics();
					
					TextCollider txc = new TextCollider ();
					int sh = fm.getHeight();
					
					// MultiNote notes = (MultiNote) elt).getNotesAsVector();
					boolean fixedMaxY = FixedMaxY(notes);
					// System.out.println("Fixed max Y: " + (fixedMaxY));
					int stringN = 0;
					for (Note note : notes) {
						// int pitch = note.getMidiLikeHeight();
						// String nam = note.getName();
						// System.out.println("drawing " + nam);
						// we are going from low to hi.
						//note.
						//String iString = getIString(eltN);
						//String roman = getRomanName(eltN, stringN);
						String roman = getRomanName(eltN, stringN);
						int sw = fm.stringWidth(roman);
						
						
						stringN++;
						
						JScoreElement e = getRenditionElementFor(note);
						
						Rectangle2D r = e.getBoundingBox();
						//int h = (int) r.getHeight();
						int w = (int) r.getWidth();
						int x = (int) r.getX();
						//int y = (int) r.getY();
						//int centerY = (int) r.getCenterY();
						int maxY = (int) r.getMaxY();
						int minY = (int) r.getMinY();
						//g2.drawRect(x, y, w, h);
						// g2.drawOval(x, y, 30, 20);
						// g2.setStroke(new BasicStroke(2));
						int ourY = 0;
						if (fixedMaxY) {
							ourY = minY + 6;
						} else {
							ourY = maxY;
						}
						//Rectangle sr = new Rectangle (x+w+12, ourY, sw, sh);
						
						//System.out.println("RECT: " + sr);
						//g2.drawString(iString, x + w + 12, ourY);
						
						txc.add(roman, x + w + 12, ourY, sw, sh);
						

					};
					txc.draw(g2);

				}
				;
			}
		};
		
		//repaint();
	}
}

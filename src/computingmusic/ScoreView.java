package computingmusic;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import computingmusic.gui.AnnotatedBoxes;
import computingmusic.utils.MySeq;

/**
 * What the rest of the program needs from the thing that draws the score: show which notes are the current
 * group, which cannot be played, which positions and anchors are boxed.  Notes are named by item number, the
 * index in the list of notes and chords the fingering engine works on, so the drawing does not have to be
 * known to the sequencers.
 *
 * Two implementations: BigScoreComponent (abc4j, for ABC files) and VerovioScoreComponent (Verovio's engraving,
 * for MusicXML, MEI and kern).
 */
public interface ScoreView {
	/** A stretch of items that can be played in only one way: marked off on the score with a short label. */
	class Stretch {
		public final int start, stop;                 // item numbers, inclusive
		public final int firstMeasure, lastMeasure;
		public final String label;
		/** The string each note is played on in the engine's trail: {item, pitch, string}, string 0 = the lowest. */
		public final java.util.List<int[]> strings = new java.util.ArrayList<int[]>();
		/** False when the engine has several fingerings for the stretch (its strings are those of the best trail). */
		public boolean unique = true;
		/** The solver's chosen trail for the stretch (a FingerTrail), or null; the window shows it first on a click. */
		public Object chosenTrail;

		public Stretch(int start, int stop, int firstMeasure, int lastMeasure, String label) {
			this.start = start;
			this.stop = stop;
			this.firstMeasure = firstMeasure;
			this.lastMeasure = lastMeasure;
			this.label = label;
		}
	}

	JComponent component();

	/** Mark these stretches on the score (replacing any marked before); an empty list clears them. */
	void setStretches(java.util.List<Stretch> stretches);

	/** Call this with the item number when the user clicks a note, or its string cell, on the drawing. */
	void setItemClickListener(java.util.function.IntConsumer listener);

	/** The group of notes the engine is looking at: one item, or items start..stop inclusive. */
	void setBoxedNotes(int index);

	void setBoxedNotes(int start, int stop);

	/** x = first item, y = last item (the engine's io() of a group). */
	void setBoxedNotes(Point io);

	void clearBoxedNotes();

	/** Mark an item that cannot be fingered. */
	void addRedNote(int index);

	void addAnnotatedBoxes(AnnotatedBoxes boxes);

	void clearAnnotationBoxes();

	void setPositionalSeq2(MySeq seq);

	void turnoffPositions();

	/** One rectangle for each line of the score that items first..last (p.x..p.y) occupy, in this view's coordinates. */
	ArrayList<Rectangle> lineRectanglesofPoint(Point p, int margin);

	/** Draw a rectangle, scrolling it into view when the user is not dragging the scroll bar. */
	void myDrawRect(Rectangle r, Graphics g);

	void giveScrollPaneAccess(JScrollPane pane);

	/** False while the scroll bar is being dragged: then the view must not scroll by itself. */
	void setNotScrolling(boolean notScrolling);
}

package computingmusic;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import javax.swing.*;

import abc.notation.*;
import abc.notation.Tune.Music;
import abc.parser.AbcVersion;
import abc.parser.TuneParser;
import abc.ui.swing.*;

public class AbcChord2 extends JPanel {

	private static final long serialVersionUID = 1L;
	public Cfinger2 cfinger2;
	//public static Instrument instrument;
	public static MyJScoreComponent scoreUI;
	public static Music m;
	public Tune tune;
	
	public void setH (float f){
		//scoreUI.setHorizontal(f);
	}
	public Instrument getInstrument(){
		return cfinger2.getInstrument();
	}
	
	public void markNote(){
		if (m != null){
			MusicElement note = (MusicElement) m.get(1);
			JScoreElement e = scoreUI.getRenditionElementFor(note);
			Rectangle2D r = e.getBoundingBox();
			int h = (int) r.getHeight();
			int w = (int) r.getWidth();
			int x = (int) r.getX();
			int y = (int) r.getY();
			//Graphics2D g = scoreUI.getGraphics2D();
			//System.out.println("h:" + h + " w:" + w + " x:" + x + " y:" + y );
			//g.fillRect(x,y,w,h);
			//g.fillRect(2, 2, 10, 10);
			//scoreUI.repaint();
		}
	}
	
	public AbcChord2(Cfinger2 c) {
		this.setMinimumSize(new Dimension(60, 80));
		cfinger2 = c;
		setBackground(Color.white);
		setBorder(BorderFactory.createEtchedBorder());
		scoreUI = new MyJScoreComponent();
		// ScoreTemplate st = scoreUI.getTemplate();
		setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		c1.gridx = 0;
		c1.gridy = 0;
		//c1.insets = new Insets(10, 10, 10, 10);
		c1.anchor = GridBagConstraints.CENTER; // was useless.
		//c1.fill = GridBagConstraints.NONE;
		
		add(scoreUI, c1);
		
		
	}
	/*
	// get rid of this
	AbcChord2(String s, int dummy) {
		setBackground(Color.white);
		setBorder(BorderFactory.createEtchedBorder());
		// Tune tune = new TuneParser().parse(s);
		tune = new TuneParser(AbcVersion.v2_0).parse(s);
		scoreUI = new MyJScoreComponent(tune);
		// ScoreTemplate st = scoreUI.getTemplate();
		add(scoreUI);
		//m = tune.getMusic();
		KeySignature key = tune.getKey();
		//key.setClef(ourClef());
		key.setClef(cfinger2.getInstrument().getClef());
		
		// tune.getRenditionObjectsMapping();
		// tune.setComposer(composer)
		// JScoreComponent scoreUI = new JScoreComponent();
		scoreUI.setTune(tune);
	
		// tune.
		//scoreUI.repaint();
	}
	
	*/
	
	// public midPitch 
	
	
	public void setKey (Tune t, Clef c){
		KeySignature key = t.getKey();
		key.setClef(c);
	}
	
	public void drawString(String s) {
		//System.out.println("drawString: " + s);
		// Tune tune = new TuneParser().parse(s);
		tune = new TuneParser(AbcVersion.v2_0).parse(s);
		//tune.
		//m = tune.getMusic();
		KeySignature key = tune.getKey();
		if (key != null) {
			key.setClef(cfinger2.getInstrument().getClef());
		}
		
		
		//scoreUI.addTune(tune);
		//scoreUI = new MyJScoreComponent(tune);
		scoreUI.setTune(tune);
		//scoreUI.sayTemplate();
		
	}
	public void drawString(String s, Point range) {
		// System.out.println("drawString: " + s);
		//Tune tune = new TuneParser().parse(s);
		Tune tune = new TuneParser(AbcVersion.v2_0).parse(s);
		//tune.
		//m = tune.getMusic();
		KeySignature key = tune.getKey();
		key.setClef(cfinger2.getInstrument().getClef(range));
		
		scoreUI.setTune(tune);
		//scoreUI.sayTemplate();
		
	}
	// T:| -- invisible title, parse screws up without title.
	// removed
	public static String header = "X:1\nT: \nK:C\nL:1/4\n";
	
	public void drawChord(String s) {
		drawString(header + s);
	}
	public void drawChord(String s, Point range) {
		drawString(header + s, range);
	}
	
	// draw one chord. 
	public void drawFingering(ChordFingering.Fingering f){
		String noteString = f.getABCString();
		String[] iStrings = f.getIString(); 
		//String[] abcStrings = f.abcSequence;
		
		//System.out.println("ABC---------------");
		//Lis.print(iStrings);
		//System.out.println(noteString);
		//scoreUI.setIStrings(iStrings);
	
		scoreUI.setRomanNames(iStrings);
		
		drawString(header + noteString, f.pitchRange());
	}
	
	private Point maxRange(Point p1, Point p2){
		return new Point ( Math.max(p1.x, p2.x), 
						    Math.max(p1.y, p2.y)
						    );
		
	}
	
	// draw TWO chords.
	public void drawFingering(ChordFingering.Fingering f1, ChordFingering.Fingering f2){
		String[][] romanNames = new String[2][];
		
		String noteString1 = f1.getABCString();
		//String[] iStrings1 = f1.getIString(); // roman string names
		String noteString2 = f2.getABCString();
		//String[] iStrings2 = f2.getIString();
		romanNames[0] = f1.getIString();
		romanNames[1] = f2.getIString();
		
		scoreUI.setRomanNames(romanNames);
		// String header = "X:1\nT:|\nK:C\nL:1/1\n";
		Point range = maxRange(f1.pitchRange(), f2.pitchRange());
		drawString(header + noteString1 + " yy" + noteString2 + "yy", range);
	}
	
	/*
	public void drawFingering(Transitions.FingeringPair p){
		drawFingering(p.f0, p.f1);
	}
	*/

	public void clear (){
		scoreUI.clearRomanNames();
		drawChord("");
	}
	
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	public Dimension getPreferredSize() {
		return new Dimension(super.getPreferredSize().width,
		// 120
				160);
	}

	
	/*
	public static void main (String[] arg) {
		JFrame j = new JFrame();
		j.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		String xx = "X:1\nT:Chords\nK:C\nL:1/4\n"
		
				//+ "[_b_B]  [=B_B]  [_B=B]  a ";
		+ "g_bgdgd_Bd_BG_BGDGD";
		
		// JScoreComponent u = ofString(xx);
		AbcChord2 u = new AbcChord2(xx);
		
		// u.testgfx();
		// u.drawString(xx);
		//u.markNote();
		j.add(u);
		j.pack();
		j.setVisible(true);
		
	}
	*/

}

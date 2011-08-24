package computingmusic;

//import computingmusic.Fing.Instr;


import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import computingmusic.utils.IntSet;

import abc.notation.*;
import abc.notation.Tune.Music;
import abc.parser.AbcVersion;
import abc.parser.TuneParser;


public class AbcReader {
	BigScore big;
	CtlBox ctl;
	JFileChooser fc;
	String abcString;
	Tune tune;
	String AbcHome = "/home/eliot/abc_scores/";
	//File currDir = new File (".");
	File currDir = new File (AbcHome);
	Boolean readError;
	
	ArrayList<KNote> kNotes;
	ArrayList<MusicElement> noteList;
	int om;
	
	
	
	
	
	AbcReader(CtlBox c){
		ctl = c;
		fc = new JFileChooser(currDir);
		
		//int result = fc.showOpenDialog(fc);
		
			
		File f = fc.getSelectedFile();
		if (f != null){
	
		//System.out.println(f);
		abcString = "";
		getFile(f);
		ctl.abcChord.clear(); // MUST kill romans, annotations
		
		ctl.abcChord.drawString(abcString);
		tune = ctl.abcChord.tune;
		noteList = new ArrayList<MusicElement> ();
		// getNotes();
		
		}
	}
	public AbcReader() {
		fc = new JFileChooser();
		
	}
	
	AbcReader(BigScore score){
		//ctl = c;
		big = score;
		fc = new JFileChooser();
		fc.setDialogTitle("Select ABC file:");
		noteList = new ArrayList<MusicElement> ();
		kNotes = new ArrayList<KNote>();
		readError = false;
	}
	
	public void read(){
		readError = false;
		fc.setCurrentDirectory(currDir);
		int result = fc.showOpenDialog(fc);
		
		if (result == JFileChooser.CANCEL_OPTION){
			System.out.println("Cancel");
			readError = true;
			return; // Cancel or the close-dialog icon was clicked 
		}
		 
		
		File f = fc.getSelectedFile();
		
		fc.setSelectedFile(f);
		currDir = fc.getCurrentDirectory();
		System.out.println("CurrDir: " + currDir);
		//AbcHome = f.getPath();
		
		kNotes = new ArrayList<KNote>();
		om = 0;
		
		if (f != null){
			System.out.println("File: " + f.getPath());
			// TODO jb here: confirm (save) the file path, it is valid
			((BigGui) big.appFrame).recordPath();
			//abcString = "";
			getFile(f);
			// System.out.println(abcString);
			try {
				tune = new TuneParser(AbcVersion.v2_0).parse(abcString);
				if (tune == null) {
				System.out.println("Parse Problem: tune is null");
				};
				if (tune.getMusic() == null) {
				System.out.println("NULL TUNE");
				} else {
				
					makeNoteList();
					//getkNotes();	
					//score.drawScore(abcString);
					//this is just setting the score.
					// big.drawScore(tune);
					// big.doFingering();
			
			
			// big.arrows.next.setEnabled(true);
			}
			} catch  (java.lang.IllegalArgumentException e) {
				readError = true;
				JOptionPane.showMessageDialog(null, 
						e.getMessage(),
						("Error reading "+ f.getPath()),
						JOptionPane.ERROR_MESSAGE
								);
				//System.err.println("Error reading " + f.getPath());
				//System.err.println(e.getMessage());
			}
		
		}
		
		
	}
	
	public class KNote {
		boolean isMulti;
		Keynum.K[]  ks;
		// MusicElement e;
		IntSet pitchSet;

		KNote(Note note) {
			isMulti = false;
			//e = note;
			// / midilikeheight -- middle c is 0.
			// add 60 to get real midi value.
			ks = new Keynum.K[1];
			ks[0] = kofNote(note, om);
			// System.out.println(">>>Note: " + pitch + " " + knam + " " +
			// k.tostring());
			//ks = new Keynum.Result(k);

		}
		KNote(MultiNote notes) {
			isMulti = true;
			//e = notes;
			// / midilikeheight -- middle c is 0.
			// add 60 to get real midi value.
			
			ks = ksofNotes(notes,om);
			
			// System.out.println(">>>Note: " + pitch + " " + knam + " " +
			// k.tostring());
			//r = new Keynum.Result(k);
			
		}
		
		public int minDur(){
		
				// take min duration
				int d = Integer.MAX_VALUE;
				for (Keynum.K k : ks) {
					d = Math.min(d, k.duration);
				}
				return d;
			
		}
		
		// 
		public int maxDur(){
			int d = 0;
			for (Keynum.K k : ks) {
				d = Math.max(d, k.duration);
			}
			return d;
		}
	}
	
	
	public  void getFile (File f) {
		abcString = "";
		try {
			
			FileInputStream fstream = new FileInputStream(f);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				abcString = abcString + "\n" + strLine;
			}
			in.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		
	}
	
	public int noteAccidentals (Note note){
		switch(note.getAccidental()) {
		case AccidentalType.SHARP:
			return 1;
		case AccidentalType.FLAT:
			return -1;
		case AccidentalType.DOUBLE_FLAT:
			return -2;
		case AccidentalType.DOUBLE_SHARP:
			return 2;	
		default: return 0;
		}
	}
	
	/// guegantan left out double sharps, flats.
	
	public String getNoteName(Note note) {
		try {
			String acc = "";
			switch(note.getAccidental()) {
			case AccidentalType.SHARP:
				acc = "#";
				break;
			case AccidentalType.FLAT:
				acc = "b";
				break;
			case AccidentalType.DOUBLE_FLAT:
				acc = "bb";
				break;
			case AccidentalType.DOUBLE_SHARP:
				acc = "##";
				break;				
			}
			switch(note.getStrictHeight()) {
			case Note.REST:
				return "";
			case Note.C:
				return "C"+acc;
			case  Note.D:
				return "D"+acc;
			case  Note.E:
				return "E"+acc;
			case  Note.F:
				return "F"+acc;
			case  Note.G:
				return "G"+acc;
			case  Note.A:
				return "A"+acc;
			case  Note.B:
				return "B"+acc;
			}
			return "";//should not happen
		} catch (IllegalArgumentException shouldNeverHappen) {
			return "";
		}
	}
	
	public String addOct(String notename, int midipitch){
		return notename + (midipitch/12);
	}
	
	public Keynum.K  kofNote (Note note, int om){
		/// midilikeheight -- middle c is 0. 
		// add 60 to get real midi value. 
		int pitch = 60 + note.getMidiLikeHeight();
		String nam = getNoteName(note);
		int accis = noteAccidentals(note);
		int chromapitch = pitch - accis;
		int octave = chromapitch / 12; // because b#, etc otherwise comes out wrong
		String knam = nam+octave; 
		
		Keynum.K k = Keynum.parseKey(knam);
		short dur = note.getDuration();
		k.setDuration(dur);
		k.setOm(om);
		//System.out.println(">>>Note: " + knam + " om " + om + " dur: " + dur);
		return k;
		
	}
	public Keynum.K[] ksofNotes (MultiNote multi, int om) {
		Note[] notes = ((MultiNote) multi).toArray();
		//int stringN = 0;
		Keynum.K[] ks = new Keynum.K[notes.length];
		for (int i = 0; i<notes.length; i++) {
			ks[i] = kofNote(notes[i], om);
		}
		return ks;
	}

	public int noteDur (Note note) {
		return note.getDuration();
	}

/*
	public void getkNotes () {
		Music m = tune.getMusic();
		KeySignature keysig = tune.getKey();
	
		Iterator  it = m.iterator();
	
	
		int eltN = -1;
	
		while (it.hasNext()) {
			MusicElement elt = (MusicElement) it.next();
			
			//String[] romans = romanNames[eltN];
			if (elt instanceof MultiNote) {
				//System.out.println("------ chord ------");
				KNote ks = new KNote ((MultiNote) elt);
				//System.out.println("-------END CHORD----");
				kNotes.add(ks);
				om = om + ks.minDur();
			
			} else if (elt instanceof Note){
				KNote ks = new KNote((Note) elt);
				kNotes.add(ks);
				om = om + ks.minDur();
				
			} else if (elt instanceof EndOfStaffLine){
				//System.out.println("LINE END");
			} else {
				//System.out.println("Other element: " + elt);
			}
		}; // while
	};
	
*/
	
	private void makeNoteList() {
		noteList.clear();
		//indexVector.clear();
		//int xv = -1;
		
		Music m = tune.getMusic();
		@SuppressWarnings("rawtypes")
		Iterator it = m.iterator();	
		while (it.hasNext()) {
			MusicElement elt = (MusicElement) it.next();
			//xv++;
			
			
			if (elt instanceof MultiNote) {
				noteList.add(elt);
				KNote ks = new KNote ((MultiNote) elt);
				//System.out.println("-------END CHORD----");
				kNotes.add(ks);
				om = om + ks.minDur();
				
			} 
			else if (elt instanceof Note) 
			{
				if (((Note) elt).isRest()) 
				{
					// noteList.add(elt);
					
					om = om + noteDur((Note) elt);
				} 
				else
				{
					noteList.add(elt);
					KNote ks = new KNote((Note) elt);
					kNotes.add(ks);
					om = om + ks.minDur();
				}
				
			} 
			else {
				//System.out.println("MakeNoteList OTHER: " + elt );
			}
			
		};
		//System.out.println("NoteList: " + noteList.size());
			
	}
	public void genMoveHolds (){
		/*for (KNote k : kNotes){
			
		}*/
	}
	
	
} // top level

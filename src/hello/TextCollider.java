package hello;

//import hello.MyJScoreComponent.GenText;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

//
// on collisions we move newwst text to right of last,
public class TextCollider {
	ArrayList<GenText> all;
	Rectangle lastRect;
	String lastText;

	TextCollider() {
		all = new ArrayList<GenText>();
		
	}
	
	public void draw(Graphics g) {
		for (GenText ge : all) ge.draw(g);
	}

	public class GenText {
		String txt;
		Rectangle r;
		
		GenText(String s1, Rectangle r1){
			txt = s1;
			r = r1;
		}
		// rects are hanging downward from baseline of text
		// doesn't matter for our purposes
		public void draw (Graphics g){
			g.drawString(txt, r.x, r.y);
			// g.drawRect(r.x, r.y-r.height, r.width, r.height);
		}
	}
	

	private static int pad = 5;
	
	
	public static String ss (Rectangle r){
		return "[x:" + r.x + " y:" + r.y + " w:" + r.width + " h:" + r.height + "]";  
	}
	
	public void add(String s, Rectangle newr) {
		GenText gt = new GenText(s, newr);
	
		if (lastRect != null) {
			// we start from the bottom up
			//Rectangle rx = lastRect.intersection(newr);
			// shift right whichever original x is greater, so
			// that the shift mirrors the layout of notes.
			//System.out.println("         X rect: " + ss(rx));
			if (lastRect.intersects(newr)){
				/*
				System.out.println("Collision: " + s + " " 
						+ ss(lastRect) 
						+ " " + ss(newr));
				System.out.println("lastText: " + lastText + " NEWR text = " + s 
						+ " newr.x: " + newr.x + " lastRect.x: " + lastRect.x);
				 */
				if (newr.x >= lastRect.x) newr.x = newr.x + pad + lastRect.width;
				else {
					lastRect.x = lastRect.x + pad + newr.width;
				}
			};	
		};	
		lastRect = newr;
		lastText = s;
		all.add(gt);
		}
	public void add(String s, int x, int y, int w, int h){
		add(s, new Rectangle(x,y,w,h));
	}
		
	public void show (){
		for (GenText g : all){
			System.out.println(g.txt + " " + g.r);
		}
	}

	
	// RECT: java.awt.Rectangle[x=55,y=52,width=11,height=14]
	// RECT: java.awt.Rectangle[x=61,y=52,width=9,height=14]

	public static void main1 (String [] sss){
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		Rectangle r1 = new Rectangle (0,0,3,10);
		Rectangle r2 = new Rectangle (0,0,3,10);
		Rectangle r3 = new Rectangle (0,11,3,10);
		Rectangle r4 = new Rectangle (0,11,3,10);
		TextCollider t = new TextCollider ();
		t.add("R1", r1);
		t.add("R2", r2);
		t.add("R3", r3);
		t.add("R4", r4);
		t.show();
				
		
	}
	public static void main (String [] sss){
		Rectangle r1 = new Rectangle (0,100,3,10);
		Rectangle r2 = new Rectangle (0, 0, 3,10);
		
		Rectangle x = r1.intersection(r2);
		boolean b = r1.intersects(r2);
		System.out.println("Does intersect? " + b);
		System.out.println("X: " + ss(x));
	}
}

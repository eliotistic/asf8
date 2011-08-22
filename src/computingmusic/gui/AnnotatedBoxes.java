package computingmusic.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;

import computingmusic.BigScore;
import computingmusic.utils.PointList;


public class AnnotatedBoxes {
	BigScore big;
	PointList boxPoints;
	Color color;
	String text;
	int stroke;
	int topMargin;
	private ArrayList<Rectangle> rectangles; // 
	
	@SuppressWarnings("unused")
	private boolean requireChainUpdate; 
	private boolean requireRectUpdate;
	
	private ArrayList<Rectangle> getRectangles(){
		if (requireRectUpdate) {
			requireRectUpdate = false;
			rectangles.clear();
			for (Point p : boxPoints) {
				ArrayList<Rectangle> r = big.scoreUI.lineRectanglesofPoint(p, topMargin);
				for (Rectangle rr : r) {
					rectangles.add(rr);
				}
				
			}
		}
		return rectangles;
	}
	public AnnotatedBoxes(BigScore big, Color color, int margin) {
		
		this.big = big;
		boxPoints = new PointList ();
		rectangles = new ArrayList<Rectangle>();
		this.color = color;
		stroke = 2;
		topMargin = margin;
	}
	
	public void addPoint (Point p){
		requireRectUpdate = true;
		boxPoints = boxPoints.chainAdd(p);
	}

	
	public void addInd (int p){
		requireRectUpdate = true;
		boxPoints = boxPoints.chainAdd(new Point(p,p));
	}
	
	public void draw  (Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		//int b = topMargin;
		
		Color old = g2.getColor();
		g2.setColor(color);
		Stroke s2 = g2.getStroke();
		g2.setStroke(new BasicStroke(stroke));
		
		for  (Rectangle r : getRectangles() ) {
			//Rectangle2D r2 = getRenditionElementFor(redNote).getBoundingBox();	
			big.scoreUI.myDrawRect(r, g);
		};
		g2.setColor(old);
		g2.setStroke(s2);
		
	}
	
	
	
}

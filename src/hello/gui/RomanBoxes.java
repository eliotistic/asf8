package hello.gui;

import hello.BigScore;
import hello.utils.IntList;

import java.awt.Color;
import java.awt.Graphics;

import abc.parser.def.GracingBeginDefinition;

public class RomanBoxes extends AnnotatedBoxes{
	IntList romans;
	public RomanBoxes(BigScore big, Color color, int margin) {
		super(big, color, margin);
		romans = new IntList();
	}
	
	public void addRoman (int ro){
		romans.add(ro);
	}
	
	public void draw (Graphics g) {
		super.draw(g);
		
	}
	

}

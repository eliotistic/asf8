package hello.utils;

import java.awt.Point;
import java.util.ArrayList;


// I can't figure out how to do this using comparable interface.
// only adding if new.

public class PointGroup extends ArrayList<PointList>{
	
	public int findGroupInd (Point p0, boolean byX) {
		int item = p0.y;
		if (byX) item = p0.x;
		for (int i = 0; i < size(); i++ ){
			Point p1 = get(i).get(0);
			int thisItem = p1.y; 
			if (byX){
				item = p0.x;
			} 
			if (item == thisItem) {
				return i;
			}
			
		}
		return -1;
	}
	public PointGroup copy () {
		PointGroup c = new PointGroup ();
		for (PointList p : this) {
			c.add(p.copy());
		}
		
		return c;
		
	}
	
	private void maybeAdd(Point p, boolean byX){
		int ind = findGroupInd(p, byX);
		if (ind == -1){
			PointList newGroup = new PointList();
				newGroup.addNew(p);
				add(newGroup);
			} else {
				ArrayList<Point> group = get(ind);
				group.add(p);
				
			}
		}
	
	
	public void addByY (Point p){
		maybeAdd(p, false);
		
	}
	public void addByX (Point p){
		maybeAdd(p, true);
	}


}

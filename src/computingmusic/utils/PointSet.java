package computingmusic.utils;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;

public class PointSet extends HashSet<Point> {
	
	private static final long serialVersionUID = 1L;

	public PointSet(){
		
	}
	
	/**
	 * author: JB
	 * @param previousPs a bigger PointSet 
	 * @return whether or not this PointSet is contained in the other PointSet.
	 */
	public boolean IsExtensionOf(PointSet previousPs)
	{
		boolean unknownPoint = false;
		for(Point pt : previousPs)
		{
			//System.out.println("NEW POINTS:" + this);
			//System.out.println("THIS POINT: " + pt);
			if(!this.contains(pt))
			{
				//System.out.println("UNKNOWN POINT WAS FOUND!!!!");
				unknownPoint = true;
			}
		}
		return !unknownPoint;
	}
	
	public PointSet copy () {
		PointSet c = new PointSet();
		for (Point a : this) c.add(a);
		return c;
	}
	private  String tostring(Point p){
		return  "<" + p.x + ", " + p.y + ">"; 	
	}
	
	@Override
	 public  String toString(){
      	String s = "{";
    	String space = ""; 
    	@SuppressWarnings("rawtypes")
		Iterator it = iterator();
    	while (it.hasNext()) {
    		Point p = (Point) it.next();
    		s = s + space + (tostring(p));
    	}
    	
    	s = s + "}";
    	return s;
    	
    }
	/*
	public int[] toarray() {
		Object[] a = toArray();
		int len = size();
		int[] o = new int[len];
		for (int i = 0; i<len;i++){
			o[i] =  ((Number)a[i]).intValue();
		}
		return o;
	}
	*/
	
	public static void main (String[] args){
		Point p1 = new Point(1,1);
		Point p2 = new Point(1,1);
		Point p3 = new Point(2,2);
		PointSet s = new PointSet();
		s.add(p1);
		s.add(p2);
		s.add(p3);
		
		System.out.println(s);
		
	}
}

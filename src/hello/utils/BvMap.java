package hello.utils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;

/**
 * BvMap
 * @author eliot
 *
 *
 */
public class BvMap extends ArrayList<BvKey>  {
	//int bvSize;
	//ArrayList<BvKey> bvKeys;

	private static final long serialVersionUID = 1L;


	@SuppressWarnings("unchecked")
	public void sortMap(){
		Collections.sort(this);
	}
	
	public Bv findKey(int val){
		for (BvKey k : this){
			if (k.key == val) {
				return k.bv; 
			}
		};
		BvKey bvk = new BvKey(val);
		add(bvk);
		sortMap();
		return bvk.bv;
	}
	public boolean hasKey (int k){
		for (BvKey bk : this){
			if (bk.key == k) {
				return true; 
			}
		};
		return false;
	}
	
	public BvMap XXXfilterKey(){
		BvMap c = copy();
		return c;
	}
	
	public int cardinality (){
		int s = 0;
		for (BvKey bk : this){
			s = s + bk.bv.cardinality();

			}
		return s;
		}
	
	/**
	 * 
	 * @return Point[] of saite, interval height in no particular order.
	 */
	public Point[] toPointArray(){
		int card = cardinality();
		Point[] points = new Point[card];
    		int i = 0;
    		for (BvKey bk : this){
    			IntList saiten = bk.bv.toList();
    			for (int saite : saiten){
    				points[i] = new Point (saite, bk.key);
    				i++;
    			}
    		}
    		return points;
	}
	
	
	public IntList getKeys () {
		IntList keys = new IntList();
		for (BvKey bk : this){
			keys.add(bk.key);
			}
		return keys;
	}
	
	public void addKey(int key, int bvInd){
		Bv b = findKey(key);
		b.set(bvInd);
		
	}
	public void addSaiteInterval (Point p){
		// x = saite
		// y = interval
		addKey(p.y, p.x);
	}
	
	public BvMap copy () {
		BvMap bMap = new BvMap();
		for (BvKey b : this) {
			bMap.add(b.copy());
		}
		return bMap;
	}
	
	@Override
	public String toString(){
		String s = "";
		for (BvKey k : this){
			s = s + k + "\n";
		}
		return s;
	}
	
	
	public static void main (String[] args){
		BvMap b = new BvMap();
		b.addKey(100, 2);
		b.addKey(0, 1);
		b.addKey(0, 2);
		b.addKey(30, 3);
		//b.sortMap();
		System.out.println(b);
		
	}

	
}

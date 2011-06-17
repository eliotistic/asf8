package hello.utils;

import java.util.ArrayList;
import java.util.Comparator;

public class Group<E> extends ArrayList<ArrayList<E>> implements Comparable<E>{
	
	
	public int findGroupInd (E e) {
		for (int i = 0; i < size(); i++ ){
			int c = ((Comparable<E>) e).compareTo(get(i).get(0));
				
			if (c==0) {
				return i;
			}
		}
		return -1;
	}



	
	public int findGroupInd (E e, Comparator<E> comp) {
		for (int i = 0; i < size(); i++ ){
			int c = ((Comparable<E>) e).compareTo(get(i).get(0));
				
			if (c==0) {
				return i;
			}
		}
		return -1;
	}
	

	public void groupAdd (E e){
		int groupInd = findGroupInd(e);
		//System.out.println("Adding " + e + " at " + groupInd);
		if (groupInd == -1){
			ArrayList<E> newGroup = new ArrayList<E>();
			newGroup.add(e);
			add(newGroup);
		} else {
			ArrayList<E> group = get(groupInd);
			group.add(e);
			
		}
	}
	public void add (E e, Comparator<E> comp){
		int groupInd = findGroupInd(e);
		//System.out.println("Adding " + e + " at " + groupInd);
		if (groupInd == -1){
			ArrayList<E> newGroup = new ArrayList<E>();
			newGroup.add(e);
			add(newGroup);
		} else {
			ArrayList<E> group = get(groupInd);
			group.add(e);
			
		}
	}
	@Override
	public String toString(){
		String s = "";
		for (ArrayList<E> group : this) {
			
			s = s + group.get(0).toString() + ": " + group.size() + "\n";
		}
		return s;
	}
	
	
		

	
	public static void main (String [] ss) {
		Group<Integer> g = new Group<Integer>();
		g.groupAdd(22);
		g.groupAdd(22);
		g.groupAdd(5532);
		g.groupAdd(22);
		g.groupAdd(5532);
		System.out.println(g);
		//g.groupAdd(1);
		Group<IntSet> g1 = new Group<IntSet>();
		int[] i1 = {1,2,3,4};
		int[] i2 = {2,1,4,3};
		int[] i3 = {100,101};
		g1.groupAdd(new IntSet(i1));
		g1.groupAdd(new IntSet(i2));
		g1.groupAdd(new IntSet(i3));
		g1.groupAdd(new IntSet(i3));
		System.out.println("\n" + g1);
	}




	@Override
	public int compareTo(E o) {
		// TODO Auto-generated method stub
		return 0;
	}










}

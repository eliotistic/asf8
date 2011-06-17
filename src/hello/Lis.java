package hello;

//import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
//import java.util.regex.*;
import java.util.Set;

/**
 *
 * @author eliot
 */
public class Lis {

    public static class Pair<A,B> {
    public  A fst;
    public  B snd;

    public Pair(A fst, B snd) {
        this.fst = fst;
        this.snd = snd;
      }
    }
    
   
   
    
    public static Pair<Integer, Integer> intpair (int a, int b){
      // Pair<Integer, String> p1 = new Pair(1,"one");
       return new Pair<Integer, Integer>(1, 2);
    }

    public static ArrayList<Integer> copy (ArrayList<Integer> a){
       ArrayList<Integer> c = new ArrayList<Integer>();
       for (int x : a){
           c.add(x);
       }
       return(c);
    }

    public static <E>ArrayList<E> xcopy (ArrayList<E> a){
       ArrayList<E> c = new ArrayList<E>();
       for (E x : a){
           c.add(x);
       }
       return(c);
    }

    public static ArrayList<String> strcopy(ArrayList<String> s){
        return xcopy(s);
    }
    public static ArrayList<Integer> intcopy(ArrayList<Integer> s){
        return xcopy(s);
    }


    public static <E>ArrayList<ArrayList<E>> chineseMenu(ArrayList<ArrayList<E>> a) {
        ArrayList<ArrayList<E>> r = new ArrayList<ArrayList<E>>();
        ArrayList<E> empty = new ArrayList<E>();
        r.add(empty);
        for (ArrayList<E> x : a) {

            ArrayList<ArrayList<E>> t = new ArrayList<ArrayList<E>>();

            for (E y : x) {
                for (ArrayList<E> i : r) {
                    ArrayList<E> xxx =  xcopy(i);
                    xxx.add(y);
                    t.add(xxx);
                }
            }
            ;

            r = t;
        }
        ;
        return r;
    }

    ;

    public static int[][] chineseMenu (int[][] a){
        ArrayList<ArrayList<Integer>> l2 = Arr2toList2(a);
        l2 = chineseMenu(l2);
        return Arr2ofList2(l2);
    }

    public static void print (ArrayList<Integer> a) {
        System.out.print("(");
        for (int i : a) {
            System.out.print(i + " ");
        }
        System.out.println(")");
    }

    ;
    public static String tostring(int[] a){
    	if (a==null){
    		return "NULL";
    	} else{
    		String s = "{";
    		String space = ""; 
    		int len = a.length;
    		for (int i = 0; i<len;i++) { 			 
    	            s =  s + space + a[i];
    	            space = " ";
    	        }
    		s = s + ")";
    		return s;
    	}
    }
    public static void print(int[] a) {
    	if (a==null ){
    		System.out.println("NULL");
    		return;
    	}
        System.out.print("(");
        for (int i : a) {
            System.out.print(i + " ");
        }
        System.out.println(")");
    }
    public static void print(String[] a) {
        System.out.print("(");
        for (String i : a) {
            System.out.print(i + " ");
        }
        System.out.println(")");
    }
    
    public static void print(int[][] a) {
        System.out.print("(");
        for (int[] i : a) {
            print(i);
        }
        System.out.println(")");
    };

    
    public static void print(String s, ArrayList<ArrayList<Integer>> a) {
        System.out.print(s + " \n(");
        for (ArrayList<Integer> b : a) {
           print(b);
        };
        System.out.println(")");
    }


    public static ArrayList<Integer> ListofArray(int[] a) {
        ArrayList<Integer> b = new ArrayList<Integer>();

        for (int i : a) {
            b.add(i);
        }
        return b;
    }

    public static ArrayList<Object> xListofArray(Object[] a) {
        ArrayList<Object> b = new ArrayList<Object>();

        for (Object i : a) {
            b.add(i);
        }
        return b;
    }
    //convert 1
    public static int[] ArrayofList(ArrayList<Integer> a){
        int[] r = new int[a.size()];
        for (int i = 0; i< r.length; i++){
            r[i] = a.get(i);
        }
        return r;
    }
    // convert 2d int list to 2d int arr
    public static int[][] Arr2ofList2 (ArrayList<ArrayList<Integer>> a2){
        int [][] r = new int[a2.size()][];
        for (int i = 0; i<a2.size(); i++){
            r[i] = ArrayofList(a2.get(i));
        }
        return r;
    }
    
    
    public static ArrayList<ArrayList<Integer>> Arr2toList2(int[][] a) {
        ArrayList<ArrayList<Integer>> r = new ArrayList<ArrayList<Integer>>();
        for (int[] x : a) {
            r.add(ListofArray(x));
        }
        return r;
    };

}


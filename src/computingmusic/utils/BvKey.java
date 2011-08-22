package computingmusic.utils;



@SuppressWarnings("rawtypes")
public class BvKey implements Comparable {
	public Bv bv;
	public int key;
	
	BvKey(int val){
		bv = new Bv(4);
		key = val;
	}
	public BvKey copy (){
		BvKey k = new BvKey(key);
		k.bv = bv.copy();
		return k;
	}
	public String toString(){
		String kk = String.format("key: %3d", key);
		return kk + " bv: " + bv;
	}
	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		BvKey b1 = (BvKey)o;
		int k1 = b1.key;
		 if (key < k1){
			return -1;
		} else if (key>k1){
			return 1;
		} else {
			return 0;
		}
		
	}
}
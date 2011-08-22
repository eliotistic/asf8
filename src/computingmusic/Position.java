package computingmusic;



public  class Position {
	int start = -1;
	int stop = -1;
    int lo;
    int hi;
    
    Position(int x, int y) {
        lo = x;
        hi = y;
    }

    ;

    // void setBad() {	    lo = -1;	};
    boolean isBad() {
        return lo > hi;
    }

    ;

    boolean isOpen() {
        return lo == 0 && hi == 0;
    }

    ;
    
    boolean eq(Position p) {
    	return p.lo == lo && p.hi == hi;
    }
    
    public Position commonPos(Position p) {
		// pos is a range lo, hi, expressed as duple.
		// 
		if (isOpen()) {
			return p;
		} // use whatever is in position
		else if (p.isOpen()) {
			return this;

		} else {
			int maxlo = Math.max(lo, p.lo);
			int minhi = Math.min(hi, p.hi);
			return new Position(maxlo, minhi);
		}

	}
   
    

    String tostring() {
        return ("(" + lo + ", " + hi + ")");
    }

    ;
    // this is for computingmusic box.
    String say() {
    	if (lo == hi){
    		return "Position: " + lo;
    	} else
    		return ("Positions: " + lo + "--" + hi);
    }

    void print() {
        System.out.print(this.tostring());
    }

    ;
    public static void main(String[] s){
    	Position p0 = new Position (1,4);
    	Position p1 = new Position (3,8);
    	Position x = p0.commonPos(p1);
    	System.out.println(x.tostring());
    }
};


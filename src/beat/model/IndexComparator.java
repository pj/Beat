package beat.model;

import java.util.Comparator;


public class IndexComparator implements Comparator<RawEvent> {

	@Override
	public int compare(RawEvent o1, RawEvent o2) {
		
		if(o1.index > o2.index){
			return 1;
		}else if(o1.index < o2.index){
			return -1;
		}else{
			return 0;
		}
	}


}

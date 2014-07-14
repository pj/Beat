package beat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import beat.model.IndexComparator;
import beat.model.RawEvent;

public class CreateZonesHelpers {
	static List<RawEvent> nextSet(List<ListIterator<RawEvent>> iterators) {
		List<RawEvent> next = new ArrayList<RawEvent>();
		
		for(ListIterator<RawEvent> iterator : iterators){
			if(iterator.hasNext()){
				next.add(iterator.next());
				iterator.previous();
			}
		}
		
		// sort next
		Collections.sort(next, new IndexComparator());
		
		// check if elements form a sequence
		int lastIndex = CreateZonesHelpers.checkSequence(next);			
							
		return next.subList(0, lastIndex);
	}

	static boolean endOfIterators(List<ListIterator<RawEvent>> iterators) {
		boolean dobreak = true;
		
		for(ListIterator<RawEvent> iterator : iterators){
			if(iterator.hasNext()){
				dobreak = false;
				break;
			}
		}
		return dobreak;
	}
	
	static int checkSequence(List<RawEvent> events) {
		int lastIndex = events.size();
			
		RawEvent previous = events.get(0);
		
		for(int n = 1; n < events.size(); n++){
			RawEvent object = events.get(n);
			
			lastIndex = n;
			
			if((object.index - previous.index) != 1)
				break;
						
			previous = object;
		}
		
		return lastIndex;
	}
	

}

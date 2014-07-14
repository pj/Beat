package beat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import beat.model.ObjectData;
import beat.model.RawEvent;
import beat.model.ThreadData;

/**
 * Various methods for fixing problems with data
 * 
 * @author pauljohnson
 */
public class DataFixes {
	static void fixThreadStartObject(Map<String, ThreadData> tis) {	
		for(ThreadData td : tis.values()){	
			for(int n = 0; n < td.events.size()-1; n++){
				RawEvent event = td.events.get(n);
								
				if(event.type.equals("threadStartEntered")){
					RawEvent eventNext = td.events.get(n+1);
					
					event.object.events.remove(event);
					event.startObject = event.object;
					event.object = eventNext.object;
					
					event.object.events.add(event);
					
					Collections.sort(event.object.events);
					
					break;
				}
			}
		}
	}

	static void setContextSwitchObjects(Map<Long, ThreadData> tis) {
		for(ThreadData td : tis.values()){
			ObjectData current = td.events.get(0).object;
			
			for(RawEvent event : td.events){
				if(event.type.equals("contextSwitchIn") || event.type.equals("contextSwitchOut")){
					event.object = current;
					
					current.events.add(event);
				}else{
					current = event.object;
				}
			}
		}
	}
	
	static void setIndexes(List<RawEvent> events) {
		for(int n = 0; n < events.size(); n++){
			events.get(n).index = n;
		}
	}
}

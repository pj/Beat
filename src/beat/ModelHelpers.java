package beat;

import java.util.Map;
import java.util.Set;

import beat.model.ObjectData;
import beat.model.RawEvent;
import beat.model.ThreadData;
import beat.model.ThreadObjectData;

public class ModelHelpers {
	static RawEvent createRawEvent(String[] lsplit) {
		RawEvent event = new RawEvent();
		
		event.type = lsplit[0].trim();
		event.methodName = lsplit[4].trim();
		event.lineNo = Integer.decode(lsplit[5].trim())-1; // line up with array indexing not line number indexing
		event.time = Long.decode(lsplit[7].trim());
		
		return event;
	}
	
	static ObjectData createObject(String[] lsplit, Map<Integer, ObjectData> objectIds, Map<String, Integer> classToOid) {
		int staticIds = -1;
		
		int oid = Integer.decode(lsplit[1].trim());
		
		if(oid == -1){
			if(classToOid.containsKey(lsplit[3].trim())){
				oid = classToOid.get(lsplit[3].trim());
			}else{
				classToOid.put(lsplit[3].trim(), staticIds);
				oid = staticIds--;
			}
		}
			
		ObjectData object = objectIds.get(oid);
		
		if(object == null){
			object = new ObjectData();
			object.oid = oid;
			object.clazz = lsplit[3].trim();
			objectIds.put(oid, object);
		}

		return object;
	}
	
	static ThreadData createThreadByName(String[] lsplit, RawEvent event, Map<String, ThreadData> threadNames) {
		/*
		 * Create thread
		 */
		String name = lsplit[2].trim();
		
		if(threadNames.containsKey(name)){
			ThreadData thread = threadNames.get(name);
			
			return thread;
		}else{
			ThreadData thread = new ThreadData();
			
			thread.name = name;
			
			threadNames.put(name, thread);
			
			return thread;
		}
	}
	
	static ThreadObjectData getTOD(ObjectData object, ThreadData td, Set<ThreadObjectData> tods) {
		ThreadObjectData tod = null;
		
		for(ThreadObjectData t : tods){
			if(t.thread == td && t.object == object){
				tod = t;
			}
		}
		
		if(tod == null){
			tod = new ThreadObjectData(td, object);
			tods.add(tod);
		}
		return tod;
	}
}

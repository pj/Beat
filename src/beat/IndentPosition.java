package beat;

import beat.model.RawEvent;
import beat.model.ThreadObjectData;

public class IndentPosition {
	public IndentPosition(ThreadObjectData threadObjectData) {
		this.tod = threadObjectData;
	}

	int index = 0;
	
	ThreadObjectData tod;
	
	public boolean hasNext(){
		if(index < tod.events.size()){
			return true;
		}else{
			return false;
		}
	}
	
	public RawEvent get(){
		return tod.events.get(index);
	}
	
	int currentStart = -1;
	int currentEnd = -1;
	
	int currentIndent = -1;
}

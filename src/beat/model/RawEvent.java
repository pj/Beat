package beat.model;

public class RawEvent implements Comparable<RawEvent>{
	public String type;
	public ObjectData object;
	public ThreadData thread;
	public String methodName;
	public int lineNo;
	public long time;
	
	public ObjectData startObject;
	public ThreadObjectData tod;
	
	public int indent = -1;
	
	public int max_indent = 0;
	
	@Override
	public int compareTo(RawEvent event) {
		
		if(event.time == this.time){
			return 0;
		}else if(event.time > this.time){
			return -1;
		}else{
			return 1;
		}

	}
	
	// from objectZone
	
	public int lines;
		
	public int index;
	
	public long position;
	
	//public long delta;
	
	//public long minDelta;
	
	//public long correctedDelta;
	
	//public long cumulativeDelta;
}

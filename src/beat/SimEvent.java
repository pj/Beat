package beat;

import java.util.List;

import beat.model.RawEvent;

public class SimEvent {
	public SimEvent(int currentStart, int currentEnd, List<RawEvent> events) {
		this.currentStart = currentStart;
		this.currentEnd = currentEnd;
		this.events = events;
	}

	int currentStart = -1;
	int currentEnd = -1;
	
	List<RawEvent> events;
}

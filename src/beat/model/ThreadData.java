package beat.model;

import java.util.ArrayList;
import java.util.List;

public class ThreadData implements Comparable<ThreadData>{
	public long tid;
	public String name;
	public List<RawEvent> events = new ArrayList<RawEvent>();

	@Override
	public int compareTo(ThreadData o) {
		return this.name.compareTo(o.name);
	}
}

package beat.model;

import java.util.ArrayList;
import java.util.List;

import org.jruby.compiler.ir.operands.Array;


public class ThreadObjectData {
	public ThreadData thread;
	public ObjectData object;
	
	public List<RawEvent> events = new ArrayList<RawEvent>();
	
	public ThreadObjectData(ThreadData thread, ObjectData object) {
		super();
		this.thread = thread;
		this.object = object;
	}

	@Override
	public boolean equals(Object obj) {
		ThreadObjectData tod = (ThreadObjectData)obj;
				
		return this.thread == tod.thread && this.object == tod.object;
	}
	
	// for drawing
	public int currentIndent = -1;
	
	public int currentMaxIndent = -1;
}

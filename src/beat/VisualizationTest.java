package beat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beat.model.ObjectData;
import beat.model.RawEvent;
import beat.model.ThreadData;
import beat.model.ThreadObjectData;
import junit.framework.TestCase;

public class VisualizationTest extends TestCase {
	
	public IndentPosition[] generateData(){
		ThreadObjectData tod1 = makeTOD(new int[]{0, 10, 40, 60, 110, 130, 160}, new String[]{"methodEntered", "methodCall", "methodEntered", "methodCall", "methodEntered","lockNotify", "methodCall"}, new int[]{0,1,4,6,11,13, 16});
		
		ThreadObjectData tod2 = makeTOD(new int[]{20, 30, 50, 80, 90, 120, 140, 150}, new String[]{"methodEntered", "methodCall", "methodEntered", "methodCall", "methodEntered", "methodCall", "methodEntered", "methodCall"}, new int[]{2,3,5,8,9,12,14,15});
		
		ThreadObjectData tod3 = makeTOD(new int[]{70, 100, 170}, new String[]{"methodEntered", "methodCall", "methodEntered"}, new int[]{7, 10, 17});
		
		IndentPosition ip1 = new IndentPosition(tod1);
		IndentPosition ip2 = new IndentPosition(tod2);
		IndentPosition ip3 = new IndentPosition(tod3);
		
		return new IndentPosition[]{ip1, ip2, ip3};
	}
	
	public void testSetEventIndent() throws Exception {
		IndentPosition[] indents = generateData();
		
		Visualization visualization = new Visualization();
		
		visualization.setObjectIndent(indents);
		
		IndentPosition ip = indents[0];
		
		List<RawEvent> ip_events = ip.tod.events;
		
		assertTrue(ip_events.get(0).indent == -1);
		assertTrue(ip_events.get(1).indent == -1);
		
		assertTrue(ip_events.get(2).indent == 0);
		assertTrue(ip_events.get(3).indent == 0);
		
		assertTrue(ip_events.get(4).indent == 0);
		assertTrue(ip_events.get(5).indent == 0);
		assertTrue(ip_events.get(6).indent == 0);
		
		ip = indents[1];
		
		ip_events = ip.tod.events;
		
		assertTrue(ip_events.get(0).indent == -1);
		assertTrue(ip_events.get(1).indent == -1);
		
		assertTrue(ip_events.get(2).indent == 1);
		assertTrue(ip_events.get(3).indent == 1);
		
		assertTrue(ip_events.get(4).indent == 1);
		assertTrue(ip_events.get(5).indent == 1);
		
		assertTrue(ip_events.get(6).indent == 1);
		assertTrue(ip_events.get(7).indent == 1);
		
		ip = indents[2];
		
		ip_events = ip.tod.events;
		
		assertTrue(ip_events.get(0).indent == 2);
		assertTrue(ip_events.get(1).indent == 2);
	}

	public void testGetNextRunComplex() throws Exception {
		Visualization visualization = new Visualization();
		
		IndentPosition[] indents = generateData();
		
		IndentPosition next = visualization.getNextRun(indents);
		assertSame(next, indents[0]);
		
		assertTrue(next.currentStart == 0);
		assertTrue(next.currentEnd == 1);
		
		next = visualization.getNextRun(indents);
		assertSame(next, indents[1]);
		
		assertTrue(next.currentStart == 0);
		assertTrue(next.currentEnd == 1);
		
		next = visualization.getNextRun(indents);
		assertSame(next, indents[0]);
		
		assertTrue(next.currentStart == 2);
		assertTrue(next.currentEnd == 3);
		
		next = visualization.getNextRun(indents);
		assertSame(next, indents[1]);
		
		assertTrue(next.currentStart == 2);
		assertTrue(next.currentEnd == 3);
		
		next = visualization.getNextRun(indents);
		assertSame(next, indents[2]);
		
		assertTrue(next.currentStart == 0);
		assertTrue(next.currentEnd == 1);
		
		next = visualization.getNextRun(indents);
		assertSame(next, indents[1]);
		
		assertTrue(next.currentStart == 4);
		assertTrue(next.currentEnd == 5);
		
		next = visualization.getNextRun(indents);
		assertSame(next, indents[0]);
		
		assertTrue(next.currentStart == 4);
		assertTrue(next.currentEnd == 6);
		
		next = visualization.getNextRun(indents);
		assertSame(next, indents[1]);
		
		assertTrue(next.currentStart == 6);
		assertTrue(next.currentEnd == 7);
		
		next = visualization.getNextRun(indents);
		assertNull(next);
	}
	
	public void testGetNextRun() throws Exception {
		Visualization visualization = new Visualization();
		
		ThreadObjectData tod1 = makeTOD(new int[]{5, 15, 40}, new String[]{"methodEntered", "methodCall", "loopIn"}, new int[]{0,2,3});
		
		ThreadObjectData tod2 = makeTOD(new int[]{10, 20}, new String[]{"methodEntered", "methodCall"}, new int[]{1,4});
		
		IndentPosition ip1 = new IndentPosition(tod1);
		IndentPosition ip2 = new IndentPosition(tod2);
		
		IndentPosition next = visualization.getNextRun(new IndentPosition[]{ip1, ip2});
		
		assertNotNull(next);
		assertSame(ip1, next);
		
		assertTrue(next.currentStart == 0);
		assertTrue(next.currentEnd == 1);
		
		next = visualization.getNextRun(new IndentPosition[]{ip1, ip2});
		
		assertNotNull(next);
		assertSame(ip2, next);
		
		assertTrue(next.currentStart == 0);
		assertTrue(next.currentEnd == 1);
		
		next = visualization.getNextRun(new IndentPosition[]{ip1, ip2});
		
		assertNull(next);
		
	}
	
	public void testGetNextEventsComplex() throws Exception {
		Visualization visualization = new Visualization();
		
		IndentPosition[] ips = generateData();
		
		IndentPosition next = visualization.getNextEvents(ips);
		
		
	}
	
	public void testGetNextEvents() throws Exception {
		Visualization visualization = new Visualization();
		
		ThreadObjectData tod1 = makeTOD(new int[]{5, 15, 40}, new String[]{"methodEntered", "methodCall", "loopIn"}, new int[]{0,2,3});
		
		ThreadObjectData tod2 = makeTOD(new int[]{10, 20}, new String[]{"methodEntered", "methodCall"}, new int[]{1,4});
		
		IndentPosition ip1 = new IndentPosition(tod1);
		IndentPosition ip2 = new IndentPosition(tod2);
		
		// prepare data
		
		IndentPosition next = visualization.getNextEvents(new IndentPosition[]{ip1, ip2});

		assertSame(ip1, next);
		ip1.index++;
		
		next = visualization.getNextEvents(new IndentPosition[]{ip1, ip2});
		
		assertSame(ip2, next);
		ip2.index++;
		
		next = visualization.getNextEvents(new IndentPosition[]{ip1, ip2});
		

		assertSame(ip1, next);
		ip1.index++;
		
		next = visualization.getNextEvents(new IndentPosition[]{ip1, ip2});
		
		
		assertSame(ip1, next);
		ip1.index++;
		
		next = visualization.getNextEvents(new IndentPosition[]{ip1, ip2});
		
		
		assertSame(ip2, next);
		ip2.index++;
		
		next = visualization.getNextEvents(new IndentPosition[]{ip1, ip2});
		
		assertNull(next);
	}
	
	public void testSimultaneous() throws Exception {
		Visualization visualization = new Visualization();
			
		ThreadObjectData tod1 = makeTOD(new int[]{5,15}, null, null);
		
		ThreadObjectData tod2 = makeTOD(new int[]{10, 20}, null, null);
		
		IndentPosition previous = new IndentPosition(tod1);
		IndentPosition next = new IndentPosition(tod2);
		
		previous.currentStart = 0;
		previous.currentEnd = 1;
		next.currentStart = 0;
		next.currentEnd = 1;
		
		
		assertTrue(visualization.simultaneous(previous, next));
	}
	
	public void testSimultaneousEnclosing() throws Exception {
		Visualization visualization = new Visualization();
		
		ThreadObjectData tod1 = makeTOD(new int[]{10,30}, null, null);
		
		ThreadObjectData tod2 = makeTOD(new int[]{15, 20}, null, null);
		
		IndentPosition previous = new IndentPosition(tod1);
		IndentPosition next = new IndentPosition(tod2);
		
		previous.currentStart = 0;
		previous.currentEnd = 1;
		next.currentStart = 0;
		next.currentEnd = 1;
		
		
		assertTrue(visualization.simultaneous(previous, next));
	}
	
	public void testNotSimultaneous() throws Exception {
		Visualization visualization = new Visualization();
		
		ThreadObjectData tod1 = makeTOD(new int[]{10,20}, null, null);
		
		ThreadObjectData tod2 = makeTOD(new int[]{30, 40}, null, null);
		
		IndentPosition previous = new IndentPosition(tod1);
		IndentPosition next = new IndentPosition(tod2);
		
		previous.currentStart = 0;
		previous.currentEnd = 1;
		next.currentStart = 0;
		next.currentEnd = 1;
		
		
		assertFalse(visualization.simultaneous(previous, next));
	}
	
	private ThreadObjectData makeTOD(int[] positions, String[] types, int[] indexes){
		ThreadData thread = new ThreadData();
		ObjectData object = new ObjectData();
		ThreadObjectData tod = new ThreadObjectData(thread, object);
		
		for(int i = 0; i < positions.length; i++){
			
			RawEvent event = new RawEvent();
			
			event.position = positions[i];
			
			if(types != null)
				event.type = types[i];
			
			if(indexes != null)
				event.index = indexes[i];
			
			thread.events.add(event);
			object.events.add(event);
			tod.events.add(event);
		}
				
		return tod;
	}
}

package beat.functions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import beat.Visualization;
import beat.model.ObjectData;
import beat.model.RawEvent;
import beat.model.ThreadData;

public class GetDrawPoints extends BrowserFunction {
	Visualization visualization;

	public GetDrawPoints(Browser browser, String name, Visualization visualization) {
		super(browser, name);
		this.visualization = visualization;
	}
	
	@Override
	public Object function(Object[] arguments) {
		int threadIndex = ((Double)arguments[0]).intValue();
		
		visualization.drawThread = visualization.threadOrder.get(threadIndex);
		
		List<Long[][]> points = getPoints(visualization.drawThread, threadIndex);
		Long[][][] pt = points.toArray(new Long[][][]{});
		
		if(pt.length == 0){
			pt = null;
		}
		
		return pt;
	}
	
	private List<Long[][]> getPoints(ThreadData thread, int threadIndex){
		// find start of events for thread
		long startPosition = (long)visualization.screenY;
		long endPosition = (long) startPosition + visualization.screenHeight;
		
		List<Long[][]> points = new ArrayList<Long[][]>();
		
		RawEvent previous = thread.events.get(0);
		
		visualization.startEvent = -1;
		
		for(int n = 1; n < thread.events.size(); n++){
			RawEvent current = thread.events.get(n);
			
			if(current.type.equals("loopOut") && previous.equals("loopIn")){
				previous = current;
				continue;
			}
			
			//System.out.println(startPosition + " " + endPosition + " " + current.position);
			
			int inEvents = eventIn(startPosition, endPosition, previous, current);
			
			if(inEvents == 0){
				if(visualization.startEvent == -1)
					visualization.startEvent = n;
				
				if(previous.type.equals("threadStartEntered")){
					Long[][] line = new Long[3][2];
					
					line[0] = createPoint(threadIndex, previous, previous.startObject, startPosition, endPosition);
					line[1] = createPoint(threadIndex, previous, previous.object, startPosition, endPosition);
					line[2] = createPoint(threadIndex, current, current.object, startPosition, endPosition);
								
					points.add(line);					
				}else{
					Long[][] line = new Long[2][2];
				
					line[0] = createPoint(threadIndex, previous, previous.object, startPosition, endPosition);
					line[1] = createPoint(threadIndex, current, current.object, startPosition, endPosition);
								
					points.add(line);
				}
				
			}else if(inEvents == 1){
				visualization.endEvent = n;
				break;
			}
			
			previous = current;
		}
					
		return points;
	}

	private int eventIn(long startPosition, long endPosition, RawEvent previous, RawEvent current) {
		
		if(current.position < startPosition){
			return -1;
		
		}else if(previous.position > endPosition){
			return 1;		
		
		}else if(previous.position > startPosition && current.position < endPosition||
				(previous.position > startPosition && previous.position < endPosition) ||
				(current.position > startPosition && current.position < endPosition) ||
				(previous.position < startPosition && current.position > endPosition)){
			return 0;
		}else{
			return -1;
		}
	}

	private Long[] createPoint(int threadIndex, RawEvent event, ObjectData object,
			long startPosition, long endPosition) {
		Long[] point = new Long[2];
		
		if(event.indent != -1){
			point[0] = (long)(object.x + 5 + (5 * threadIndex)) + (long)(((float)object.width) * (((float)event.indent) / ((float)event.max_indent)));
			//System.out.println(event.indent + " " + event.max_indent + " " + (long)(((float)object.width) * (((float)event.indent) / ((float)event.max_indent))));
		}else{
			point[0] = (long)(object.x + 5 + (5 * threadIndex));
		}
		
//		if(event.position < startPosition){
//			point[1] = 0L;
//		}else if(event.position > endPosition){
//			point[1] = visualization.screenHeight;
//		}else {
			point[1] = event.position - startPosition;
//		}
		
		return point;
	}
}

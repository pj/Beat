 package beat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.jruby.embed.ScriptingContainer;

import beat.model.ObjectData;
//import beat.model.ObjectTextBlock;
import beat.model.RawEvent;
import beat.model.SourceData;
import beat.model.ThreadData;
import beat.model.ThreadObjectData;

public class Visualization {
	public static List<ObjectData> objectOrder;
	
	List<RawEvent> events = new ArrayList<RawEvent>();

	public Map<String, ThreadData> threadNames = new HashMap<String, ThreadData>();
	public Map<Long, ThreadData> threadIds = new HashMap<Long, ThreadData>();
	public Map<String, SourceData> objectSources = new HashMap<String, SourceData>();
	public Map<Integer, ObjectData> objectIds = new HashMap<Integer, ObjectData>();
	public Map<String, Integer> classToOid = new HashMap<String, Integer>();
	
	public Set<ThreadObjectData> tods = new HashSet<ThreadObjectData>();
	public Map<ObjectData, List<ThreadObjectData>> objectToTod = new HashMap<ObjectData, List<ThreadObjectData>>();
	public List<ThreadData> threadOrder = new ArrayList<ThreadData>();
	
	// for creating indent css
	public int cssIndent = 0;
	
	/*
	 * Data and state for drawing
	 */
	public long screenX;
	public long screenY;
	public long screenWidth;
	public long screenHeight;

	public long columnHeight = 0;
	
	public ThreadData drawThread;
	
	public int startEvent;
	public int endEvent;
	
	// Drawing Constants
	public static final long minZoneHeight = 30;
	public static final long minZoneSpace = 20;
	public static final long lineHeight = 20;
	
	public static final long column_offset = 30L;
	public static final long headerSpace = 40L;
	public static final long threadColumnOffset = 5L;
	
	public static final String[] inTypes = {"forLoopIn","whileLoopIn","doLoopIn"};
	public static final String[] outTypes = {"forLoopOut","whileLoopOut","doLoopOut"};
	public static final String[] methodInTypes = {"methodStart","synchronizedMethodEntered"};
	
	public String renderVisualizationJRuby(BeatLauncher launcher, ILaunchConfiguration configuration) throws CoreException {
		try {
			ScriptingContainer container = new ScriptingContainer();
						
			InputStream script = FileLocator.openStream(Activator.getDefault().getBundle(), new Path(
			"resources/process_visualization.rb"), false);
						
			StringWriter sw = setContainerConstants(container);
			
			// inputs for data files
			IJavaProject javaProject = launcher.getJavaProject(configuration);
			
			IProject project = javaProject.getProject();
			
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			
			List<BufferedReader> eventData = DataLoader.loadThreadData(project);
			
			//BufferedReader switchData = DataLoader.loadSwitchData(project);
			
			processRawEvents(eventData, null, launcher, configuration);

			container.put("objectIds", objectIds);
			container.put("sources", objectSources);
			container.put("headerSpace", headerSpace);
			container.put("viewHelper", new ViewHelpers(this));
			container.put("columnHeight", columnHeight);
			
			container.runScriptlet(script, "process_visualization.rb");
			
			return sw.toString();
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally{}
		return "hello";
	}

	private StringWriter setContainerConstants(ScriptingContainer container)
			throws IOException {
		// output
		StringWriter sw = new StringWriter();
		container.put("out", sw);
		
		// get resource path for css files etc
		URL resourcesDir = FileLocator.find(Activator.getDefault().getBundle(), 
				new Path("resources"), null);
		
		container.put("filesUrl", FileLocator.toFileURL(resourcesDir).toString().substring(5));
		
		// template input
		InputStream template = FileLocator.openStream(Activator.getDefault().getBundle(), new Path(
			"resources/visualization.rhtml"), false);
					
		container.put("template", new BufferedReader(new InputStreamReader(template)));
		return sw;
	}
	
	
	private void processRawEvents(List<BufferedReader> eventData, BufferedReader switchData, BeatLauncher launcher, ILaunchConfiguration configuration) throws IOException, JavaModelException, CoreException{
		for(BufferedReader br : eventData){
			
			String line;
			while((line = br.readLine()) != null){
				/*
				 * Create raw event and object model
				 * 
				 */
				String[] lsplit = line.split("\t");
				
				RawEvent event = ModelHelpers.createRawEvent(lsplit);
				
				events.add(event);
				
				ObjectData object = ModelHelpers.createObject(lsplit, objectIds, classToOid);
				
				event.object = object;
				object.events.add(event);
				
				DataLoader.addSource(launcher, configuration, object, objectSources);
			
				ThreadData td = ModelHelpers.createThreadByName(lsplit, event, threadNames);
				
				event.thread = td;
				td.events.add(event);		
				
				ThreadObjectData tod = ModelHelpers.getTOD(object, td, tods);
				
				event.tod = tod;
				tod.events.add(event);
				
				if(objectToTod.containsKey(object)){
					if(!objectToTod.get(object).contains(tod)){
						objectToTod.get(object).add(tod);
					}
				}else{
					List<ThreadObjectData> ltod = new ArrayList<ThreadObjectData>();
					ltod.add(tod);
					objectToTod.put(object, ltod);
				}
			}
		}
			
		sortData();
		
		// fix thread start events that have the wrong object id
		DataFixes.fixThreadStartObject(threadNames);
					
		for(ThreadData l : threadNames.values()){
			threadOrder.add(l);
		}
		
		Collections.sort(threadOrder);
		
		// set index of raw events
		DataFixes.setIndexes(events);
				
		//setEventPositions();
		setEventPositions(tods);
	
		// set event indent
		setEventIndent(objectIds, tods, objectToTod);
	}

	void setEventIndent(Map<Integer, ObjectData> objects, Set<ThreadObjectData> tods2, Map<ObjectData, List<ThreadObjectData>> objectToTod2) {
		for(ObjectData od : objects.values()){
			List<ThreadObjectData> objectTODs = objectToTod2.get(od);
			
			if(objectTODs.size() == 1){
				continue;
			}
			
			IndentPosition[] indents = new IndentPosition[objectTODs.size()];
			
			for(int i = 0; i < objectTODs.size(); i++){
				indents[i] = new IndentPosition(objectTODs.get(i));
			}
			
			System.out.println(od.clazz);
			
			setObjectIndent(indents);
		}
	}
		
	void setObjectIndent(IndentPosition[] indents){
		IndentPosition previous = getNextRun(indents);
		
		if(previous == null){
			return;
		}
		
		IndentPosition next = null;
		
		Map<ThreadObjectData, Integer> threadIndents = new HashMap<ThreadObjectData, Integer>();
		
		List<RawEvent> setMaxEvents = new ArrayList<RawEvent>();
		
		int simEventsMax = 0;
		
		while((next = getNextRun(indents)) != null){
//			System.out.print("Previous: " + previous.tod.events.get(previous.currentStart).position + " " + previous.tod.events.get(previous.currentEnd).position);
//			System.out.println(" Next: " + next.tod.events.get(next.currentStart).position + " " + next.tod.events.get(next.currentEnd).position);

			if(simultaneous(previous, next)){
//				System.out.println("simultaneous");
				
				RawEvent prevEventEnd = previous.tod.events.get(previous.currentEnd);
				RawEvent prevEventStart = previous.tod.events.get(previous.currentStart);
				
				RawEvent nextEventEnd = next.tod.events.get(next.currentEnd);
				RawEvent nextEventStart = next.tod.events.get(next.currentStart);
				
				// start of simultaneous run
				if(prevEventEnd.indent == -1){
					for(int i = previous.currentStart; i <= previous.currentEnd; i++){
						previous.tod.events.get(i).indent = 0;
						setMaxEvents.add(previous.tod.events.get(i));
					}
					
					threadIndents.put(previous.tod, 0);

					for(int i = next.currentStart; i <= next.currentEnd; i++){
						next.tod.events.get(i).indent = 1;
						setMaxEvents.add(next.tod.events.get(i));
					}
					
					threadIndents.put(next.tod, 1);
					
					simEventsMax = 2;
				}else{
					if(threadIndents.containsKey(next.tod)){
						int ind = threadIndents.get(next.tod);
						
						for(int i = next.currentStart; i <= next.currentEnd; i++){
							next.tod.events.get(i).indent = ind;
							setMaxEvents.add(next.tod.events.get(i));
						}
					}else{
						for(int i = next.currentStart; i <= next.currentEnd; i++){
							next.tod.events.get(i).indent = simEventsMax;
							setMaxEvents.add(next.tod.events.get(i));
						}
						
						simEventsMax++;
					}
				}
			}else{
				for(RawEvent simEvent : setMaxEvents){
					simEvent.max_indent = simEventsMax;
				}
				
				threadIndents.clear();
				setMaxEvents.clear();
				
				if(simEventsMax > cssIndent){
					cssIndent = simEventsMax;
				}
				
				simEventsMax = 0;
			}
			
			previous = next;
		}
	}
	
	boolean simultaneous(IndentPosition previous, IndentPosition next) {
		RawEvent previousStart = previous.tod.events.get(previous.currentStart);
		RawEvent previousEnd = previous.tod.events.get(previous.currentEnd);
		RawEvent nextStart = next.tod.events.get(next.currentStart);
		
		return nextStart.position > previousStart.position && nextStart.position < previousEnd.position;
	}

	IndentPosition getNextRun(IndentPosition[] indents) {	
		ViewHelpers vh = new ViewHelpers(this);
		IndentPosition indent;
		
		while(true){
			// get the next event
			indent = getNextEvents(indents);

			if(indent == null){
				return null;
			}
					
			if(!indent.hasNext()){
				indent.currentEnd = -1;
				indent.currentStart = -1;
				continue;
			}
			
			RawEvent previous = indent.get();
			indent.currentStart = indent.index;
			indent.index++;
			
			if(!indent.hasNext()){
				indent.currentEnd = -1;
				indent.currentStart = -1;
				continue;
			}
			
			while(true){				
				RawEvent next = indent.get();
				
				if(!vh.getBlockType(previous, next).equals("normal")){
					return indent;
				}else{
					indent.currentEnd = indent.index;
					indent.index++;
					previous = next;
				}
				
				if(!indent.hasNext()){
					return indent;
				}
			}
		}	
	}
	
	IndentPosition getNextEvents(IndentPosition[] indents){
		IndentPosition next = null;
		
		RawEvent nextEvent = null;
		
		for(IndentPosition indent : indents){
			if(indent.hasNext()){
				RawEvent t = indent.get();
			
				if(nextEvent == null || t.index < nextEvent.index){
					nextEvent = t;
					next = indent;
				}
			}
		}
		
		return next;
	}
	
	/**
	 * Very confusing :(
	 * 
	 */
	private void setEventPositions(Set<ThreadObjectData> tods2) {
		// create a list of iterators for each list of zones
		List<ListIterator<RawEvent>> iterators = new ArrayList<ListIterator<RawEvent>>();
		
		Map<ThreadObjectData, ListIterator<RawEvent>> iteratorsMap = new HashMap<ThreadObjectData, ListIterator<RawEvent>>();
		
		for(ThreadObjectData od : tods2){
			List<RawEvent> e = od.events;
			
			ListIterator<RawEvent> iterator = e.listIterator();
			iterators.add(iterator);
			iteratorsMap.put(od, iterator);
		}

		/*
		 * All this is confusing -
		 * previousEvent is the highest previous event - this could be from the previous set of events or from the current
		 * previousEvent will be set to the highest event from the current set after the end of processing making it available for 
		 * the next loop round
		 */
		RawEvent previousSetEvent = null;
		
		while(true){
			// break if we've reached the end of all the iterators
			if(CreateZonesHelpers.endOfIterators(iterators))
				break;
			
			// get all next elements
			List<RawEvent> nextEvents = CreateZonesHelpers.nextSet(iterators);
			
			for(int n = 0; n < nextEvents.size(); n++){
				RawEvent current = nextEvents.get(n);
				
				RawEvent previousObjectEvent = getPreviousObjectEvent(current);
				
				if(previousObjectEvent != null && current.type.equals("loopOut") && previousObjectEvent.equals("loopIn")){
					previousSetEvent = current;
					continue;
				}
				
				if(previousSetEvent == null){
					// current is the first event altogether - we'll position this event using offsets to all positions rather than setting
					// this to the minZoneSpace or whatever
					current.position = 0;
					
				}else if(previousObjectEvent == null){
					// current is the first event for the column
					current.position = previousSetEvent.position + Visualization.minZoneSpace;
					
				}else{
					long newHeight = ViewHelpers.getLineCount(previousObjectEvent, current) * Visualization.lineHeight;
				
					if(newHeight < Visualization.minZoneHeight)
						newHeight = Visualization.minZoneHeight;
					
					newHeight += previousObjectEvent.position;
					
					if(newHeight < (previousSetEvent.position + Visualization.minZoneSpace))
						newHeight = previousSetEvent.position + Visualization.minZoneSpace;
					
					current.position = newHeight;
					
					// this creates the blocks for object drawing
					//createObjectTextBlock(current, previousObjectEvent);
				}
				
				previousSetEvent = current;
			}
			
			// advance processed iterators to next event
			advanceIterators(iteratorsMap, nextEvents);
		}
		
		if(previousSetEvent != null){		
			columnHeight = previousSetEvent.position;
		}
	}
	
	private void advanceIterators(Map<ThreadObjectData, ListIterator<RawEvent>> iteratorsMap,
			List<RawEvent> nextSet) {
		for(RawEvent event : nextSet){
			ListIterator<RawEvent> iterator = iteratorsMap.get(event.tod);
			
			iterator.next();
		}
	}
	
	private RawEvent getPreviousObjectEvent(RawEvent event) {

		int index = event.tod.events.indexOf(event);
		
		if(index == 0) return null;
		
		return event.tod.events.get(index-1);
	}
	
	private void sortData() {
		//Sort raw data
		for(ObjectData object : objectIds.values()){
			Collections.sort(object.events);
		}
		
		for(ThreadData thread : threadNames.values()){
			Collections.sort(thread.events);
		}
		
		for(ThreadObjectData tod : tods){
			Collections.sort(tod.events);
		}
		
		Collections.sort(events);
	}
}

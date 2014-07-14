package beat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import beat.model.ObjectData;
import beat.model.RawEvent;
import beat.model.SourceData;
import beat.model.ThreadObjectData;

public class ViewHelpers {
	Visualization visualization;
	
	public ViewHelpers(Visualization visualization) {
		this.visualization = visualization;
	}

	/**
	 * Constants for get block type
	 * 
	 */
	private static String[][] blockTypeEvents = {
			{ "lockWaitStart", "lockWaitEnd" },
			{ "threadSleepStart", "threadSleepEnd" },
			{ "threadJoinStart", "threadJoinEnd" },
			{ "threadStartEntered", "threadRunEntered" },
			{ "contextSwitchIn", "contextSwitchOut" },
			{ "synchronizedBlockAcquire", "synchronizedBlockEntered" },
			{ "synchronizedMethodCalled", "synchronizedMethodEntered" },
			{ "synchronizedMethodWaitStart", "synchronizedMethodWaitEnd" },
			{ "methodExit", "*"},
			{ "methodCall", "*"},
			{ "synchronizedMethodExit", "*"	},
			{ "lockWaitStart", "*"},
			{ "threadSleepStart", "*"},
			{ "threadJoinStart", "*"},
			{ "returnStatement", "*"}
			
		};

	/**
	 * Gets the type of object block to draw
	 * 
	 * @return the type of block to draw
	 */
	public String getBlockType(RawEvent firstEvent, RawEvent secondEvent){
		String type = "normal";
		
		for(String[] types : blockTypeEvents){
			if(firstEvent.type.equals(types[0]) && (secondEvent.type.equals(types[1]) || types[1].equals("*"))){
				type = "nodraw";
				break;
			}
		}
		
		return type;
	}
	
	public List<ObjectData> getObjectOrder(){
		
		if(Visualization.objectOrder == null){
			List<ObjectData> objects = new ArrayList<ObjectData>();
			for(ObjectData od : visualization.objectIds.values()){
				objects.add(od);
			}
					
			Collections.sort(objects);
			
			return objects;
		}else{
			return Visualization.objectOrder;
		}
	}
	
	public List<ThreadObjectData> getTODS(ObjectData object){
		return visualization.objectToTod.get(object);
	}
	
	public int cssIndent(){
		return visualization.cssIndent;
	}
	
	/**
	 * Get the source for this block of text or null
	 * @param firstEvent 
	 * @param secondEvent 
	 * 
	 * @return the source for this block of text
	 */
	public String[] getSourceLines(RawEvent firstEvent, RawEvent secondEvent){
		SourceData source = firstEvent.object.source;
		
		int[] positions = getLineStartEnd(firstEvent, secondEvent);
		
		return (String[]) ArrayUtils.subarray(source.highlightedSource, positions[0], positions[1]);
	}
	
	public static int getLineCount(RawEvent firstEvent, RawEvent secondEvent) {
		int[] positions = getLineStartEnd(firstEvent, secondEvent);
		
		return (positions[1] - positions[0]);
	}
	
	public static int[] getLineStartEnd(RawEvent firstEvent, RawEvent secondEvent){
		String[] secondEventP1 = {"methodExit", "synchronizedMethodExit", "programExit", 
			"threadRunExit", "returnStatement", "lockWaitStart", "threadSleepStart", "methodCall",
			"synchronizedBlockAcquire", "synchronizedBlockExit", "threadDeathException"};
		
		String[] firstEventP1 = {"methodCallExit"};
		
		String[] firstEventM1 = {"synchronizedBlockEntered"};
		
		String[] firstEventP2 = {"synchronizedBlockExit"};
		
		int[] positions = new int[2];
		
		if(ArrayUtils.contains(firstEventP1, firstEvent.type)){
			positions[0] = firstEvent.lineNo+1;
		}else{
			positions[0] = firstEvent.lineNo;
		}
		
		if(ArrayUtils.contains(secondEventP1, secondEvent.type)){
			positions[1] = secondEvent.lineNo+1;
		}else{
			positions[1] = secondEvent.lineNo;
		}
		
		if(ArrayUtils.contains(firstEventM1, firstEvent.type)){
			positions[0] = firstEvent.lineNo-1;
		}else{
			positions[0] = firstEvent.lineNo;
		}
		
		if(ArrayUtils.contains(firstEventP2, firstEvent.type)){
			positions[0] = firstEvent.lineNo+1;
		}else{
			positions[0] = firstEvent.lineNo;
		}
		
		return positions;
	}
	
	public int threadCount(){
		return visualization.threadOrder.size();
	}
	
	public String threadColor(int index){
		return "rgb(" + Colors.colors[index] + ")";
	}
	
	public String threadColorAlpha(int index, String alpha){
		return "rgba(" + Colors.colors[index] + ","+ alpha +")";
	}
	
	public String threadName(int index){
		return visualization.threadOrder.get(index).name;
	}
	
	public void textLogging(RawEvent firstEvent, RawEvent secondEvent){
//		int[] positions = getLineStartEnd(firstEvent, secondEvent);
//if(firstEvent.object.clazz.equals("producer_consumer.Producer") && getBlockType(firstEvent, secondEvent).equals("normal")){
//			System.out.println("--------------------");
//			System.out.println("Type - " + getBlockType(firstEvent, secondEvent));
//			System.out.println("Object - " + firstEvent.object.clazz);
//			System.out.println("Entry Type: " + firstEvent.type + " Exit Type: " + secondEvent.type);
//			System.out.println("Entry Index: " + firstEvent.index + " Exit Index: " + secondEvent.index);
//			System.out.println("Entry Line: " + positions[0] + " Exit Line: " + positions[1]);
//			System.out.println("Entry Position: " + firstEvent.position + " Exit Position: " + secondEvent.position);
//			String[] lines = getSourceLines(firstEvent, secondEvent);
//
//			for(String line : lines){
//				System.out.println(line);
//			}
//		}
	}
	
	public String getColor(RawEvent event, String alpha){
		int index = visualization.threadOrder.indexOf(event.thread);
		
		return "rgba(" + Colors.colors[index] + "," + alpha + ")";
		
	}
	
	public static String getLineType(RawEvent event){
		if(event.type.equals("threadJoinStart")){
			return "join";
		}else if(event.type.equals("threadSleepStart")){
			return "sleep";
		}else if(event.type.equals("lockWaitStart")){
			return "wait";
		}else if(event.type.equals("threadStartEntered")){
			return "start";
		}else if(event.type.equals("threadRunExit")){
			return "exit";
		}else if(event.type.equals("lockNotify")){
			return "notify";
		}else if(event.type.equals("lockNotifyAll")){
			return "notifyAll";
		}else if(event.type.equals("programEntered")){
			return "programStart";
		}else if(event.type.equals("programExit")){
			return "exit";
		}else if(event.type.equals("contextSwitchIn")){
			return "switch";
		}else if(event.type.equals("forLoopStart") || event.type.equals("doLoopStart") || event.type.equals("whileLoopStart")){
			return "loop";
		}else if(event.type.equals("synchronizedBlockAcquire")){
			return "blockAcquire";
		}else if(event.type.equals("exceptionEntered")){
			return "exception";
		}else if(event.type.equals("thrown")){
			return "thrown";
		}else if(event.type.equals("threadDeathException")){
			return "threadDeathException";
		}else{
			return "normal";
		}
	}
}

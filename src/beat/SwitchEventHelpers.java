package beat;

public class SwitchEventHelpers {
	// load switch data
	//createSwitchEvents(switchData);
	
	// set object for context switch events
	//DataFixes.setContextSwitchObjects(threadIds);
	
//	private void createSwitchEvents(BufferedReader switchData)
//	throws IOException {
//String line;
//
//Set<Long> tids = threadIds.keySet();
//		
//while((line = switchData.readLine()) != null){
//	String[] lsplit = line.split(" ");
//
//	if(lsplit.length != 3){
//		continue;
//	}
//	
//	Long out = Long.parseLong(lsplit[0]);
//	Long in = Long.parseLong(lsplit[1]);
//	Long time = Long.parseLong(lsplit[2]);
//	
//	if(tids.contains(out)){
//		RawEvent outEvent = new RawEvent();
//		outEvent.type = "contextSwitchOut";
//		outEvent.time = time;
//		
//		ThreadData td = threadIds.get(out);
//		
//		outEvent.thread = td;
//		
//		td.events.add(outEvent);
//		
//		events.add(outEvent);
//	}
//	
//	if(tids.contains(in)){
//		RawEvent inEvent = new RawEvent();
//		inEvent.type = "contextSwitchIn";
//		inEvent.time = time;
//		
//		ThreadData td = threadIds.get(in);
//		
//		inEvent.thread = td;
//		
//		td.events.add(inEvent);
//		
//		events.add(inEvent);
//	}
//}
//}
}

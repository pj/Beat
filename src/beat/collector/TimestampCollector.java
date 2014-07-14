package beat.collector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

/**
 * This is a collector that uses timestamps to probe events in the vm
 * 
 * It should be usable on it's own and with dtrace
 * 
 * @author pauljohnson
 *
 */
public class TimestampCollector {	
	ArrayList<String> events = new ArrayList<String>(10000);
	
	StringBuilder sb = new StringBuilder();
	
	long tid;
	
	public static Object lock = new Object();
	
	public static void probe(EventType type, int objectId, String threadName, String objectClass, String methodName, int lineNo){
		TimestampCollector tc = TimestampCollectorTL.get();

		tc.sb.append(StringUtils.rightPad(type.toString(), 30));
		tc.sb.append("\t");
		tc.sb.append(StringUtils.rightPad(Integer.toString(objectId), 15));
		tc.sb.append("\t");
		tc.sb.append(StringUtils.rightPad(threadName, 15));
		tc.sb.append("\t");
		tc.sb.append(StringUtils.rightPad(objectClass, 40));
		tc.sb.append("\t");
		tc.sb.append(StringUtils.rightPad(methodName, 15));
		tc.sb.append("\t");
		tc.sb.append(StringUtils.rightPad(Integer.toString(lineNo), 5));
		tc.sb.append("\t");
		tc.sb.append(StringUtils.rightPad(Long.toString(tc.tid), 15));
		tc.sb.append("\t");
		tc.sb.append(StringUtils.rightPad(Long.toString(System.nanoTime()), 20));
		tc.sb.append("\n");
	}
	
	public static Thread threadStartProbe(Thread newThread, EventType type, int objectId, String threadName, String objectClass, String methodName, int lineNo){
		probe(EventType.threadStartCall, objectId, threadName, objectClass, methodName, lineNo);
		probe(EventType.threadStartEntered, objectId, newThread.getName(), objectClass, methodName, lineNo);
		
		return newThread;
	}
	
	public static void threadRunExit(EventType type, int objectId, String threadName, String objectClass, String methodName, int lineNo) {
		probe(type, objectId, threadName, objectClass, methodName, lineNo);
			
		// output thread data
		TimestampCollector tc = TimestampCollectorTL.get();
		
		try {
			FileWriter fw = new FileWriter("beat_thread_data/" + threadName);
			
			fw.write(tc.sb.toString());
			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void programEntered(EventType type, int objectId, String threadName, String objectClass, String methodName, int lineNo) {
		// create directory
		File dir = new File("beat_thread_data");
		
		dir.mkdir();
		
		for(File x : dir.listFiles()){
			x.delete();
		}
		
		probe(type, objectId, threadName, objectClass, methodName, lineNo);
	}
	
	public static void programExit(EventType type, int objectId, String threadName, String objectClass, String methodName, int lineNo) {
		probe(type, objectId, threadName, objectClass, methodName, lineNo);
		
		// output thread data
		TimestampCollector tc = TimestampCollectorTL.get();
		
		try {
			FileWriter fw = new FileWriter("beat_thread_data/" + threadName);
			
			fw.write(tc.sb.toString());
			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

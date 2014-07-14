package beat.collector;

import java.util.ArrayList;

 /**
 * Thread Local variable for events
 * 
 * @author pauljohnson
 *
 */
public class TimestampCollectorTL {
    private static ThreadLocal collectorEvents = new ThreadLocal() {
        protected synchronized Object initialValue() {
        	TimestampCollector tc = new TimestampCollector();
//    		System.load("/Users/pauljohnson/Java/libGetInternalThreadIDUser.dylib");
//        	tc.tid = TimestampCollectorTL.getThreadId();
            return tc;
        }
    };

    public static TimestampCollector get() {
        return (TimestampCollector) collectorEvents.get();
    }

	public native static long getThreadId();
}



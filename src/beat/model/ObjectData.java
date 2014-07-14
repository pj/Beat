package beat.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

public class ObjectData implements Comparable<ObjectData>{
	// attr_accessor :oid, :clazz, :events, :source
	public int oid;

	public String clazz;
	public List<RawEvent> events = new ArrayList<RawEvent>();
	
	//public List<ObjectTextBlock> textBlockEvents = new ArrayList<ObjectTextBlock>();
	
	public SourceData source;
	
	public int width;
	public int x;
	
	public int getOid() {
		return oid;
	}
	public void setOid(int oid) {
		this.oid = oid;
	}
	public String getClazz() {
		return clazz;
	}
	public void setClazz(String clazz) {
		this.clazz = clazz;
	}
	public List<RawEvent> getEvents() {
		return events;
	}
	public void setEvents(List<RawEvent> events) {
		this.events = events;
	}
	public SourceData getSource() {
		return source;
	}
	public void setSource(SourceData source) {
		this.source = source;
	}
	@Override
	public int compareTo(ObjectData o) {

		int clazzCompare = this.clazz.compareTo(o.clazz);
		
		if(clazzCompare == 0){
			if(this.oid == o.oid){
				return 0;
			}else if(this.oid > o.oid){
				return 1;
			}else{
				return -1;
			}
		}else if(clazzCompare == 1 ){
			return 1;
		}else{
			return -1;
		}
	}
}

package beat.model;

public class SourceData {
	//:clazz, :source, :html
	
	public String clazz;
	public String[] source;
	public String sourceString;
	public String highlightedString;
	public String[] highlightedSource;
	
	public void setHighlighting(){
		highlightedSource = highlightedString.split("\n");
	}
}

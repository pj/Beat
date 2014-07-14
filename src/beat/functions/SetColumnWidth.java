package beat.functions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import beat.Visualization;
import beat.model.ObjectData;

public class SetColumnWidth extends BrowserFunction {

	Visualization visualization;
	
	public SetColumnWidth(Browser browser, String name, Visualization visualization) {
		super(browser, name);
		
		this.visualization = visualization;
	}
	
	@Override
	public Object function(Object[] arguments) {
		String oid = (String)arguments[0];
		Double width = (Double)arguments[1];
		Double x = (Double)arguments[2];
		Double index = (Double)arguments[3];
		
		//System.out.println(x + " " + width);
		
		ObjectData object = visualization.objectIds.get(Integer.decode(oid));
		
		object.width = width.intValue();
		object.x = x.intValue();
		
		// System.out.println(object.width + " " + object.x);
		
		return true;
	}
}

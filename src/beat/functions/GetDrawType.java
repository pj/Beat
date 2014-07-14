package beat.functions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import beat.ViewHelpers;
import beat.Visualization;
import beat.model.RawEvent;

public class GetDrawType extends BrowserFunction {
	Visualization visualization;

	public GetDrawType(Browser browser, String name, Visualization visualization) {
		super(browser, name);
		this.visualization = visualization;
	}
	
	@Override
	public Object function(Object[] arguments) {
		int pointNumber = ((Double)arguments[0]).intValue();
		
		RawEvent event = visualization.drawThread.events.get(visualization.startEvent + pointNumber - 1);
		
		RawEvent next = visualization.drawThread.events.get(visualization.startEvent + pointNumber);
		
		return new String[] {ViewHelpers.getLineType(event), ViewHelpers.getLineType(next)};
	}
}

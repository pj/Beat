package beat.functions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import beat.Visualization;

public class DrawSetupFunction extends BrowserFunction {
	Visualization visualization;

	public DrawSetupFunction(Browser browser, String name, Visualization visualization) {
		super(browser, name);
		this.visualization = visualization;
	}
	
	@Override
	public Object function(Object[] arguments) {
		visualization.screenX = ((Double)arguments[0]).longValue();
		visualization.screenY = ((Double)arguments[1]).longValue();
		visualization.screenWidth = ((Double)arguments[2]).longValue();
		visualization.screenHeight = ((Double)arguments[3]).intValue();
		
		return visualization.threadOrder.size();
	}
}
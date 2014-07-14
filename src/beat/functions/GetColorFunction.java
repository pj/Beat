package beat.functions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import beat.Colors;
import beat.Visualization;

public class GetColorFunction extends BrowserFunction {

	Visualization visualization;
	
	public GetColorFunction(Browser browser, String name, Visualization visualization) {
		super(browser, name);
		this.visualization = visualization;
	}

	@Override
	public Object function(Object[] arguments) {
		Double threadNumber = (Double)arguments[0];

		return "rgb(" + Colors.colors[threadNumber.intValue()] + ")";
	}
}

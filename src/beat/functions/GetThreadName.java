package beat.functions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import beat.Colors;
import beat.Visualization;

public class GetThreadName extends BrowserFunction {

	private Visualization visualization;

	public GetThreadName(Browser browser, String name, Visualization visualization) {
		super(browser, name);
		this.visualization = visualization;
	}

	@Override
	public Object function(Object[] arguments) {
		Double threadNumber = (Double)arguments[0];

		return visualization.threadOrder.get(threadNumber.intValue()).name;
	}
}

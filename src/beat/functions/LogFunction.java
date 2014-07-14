package beat.functions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

public class LogFunction extends BrowserFunction {

	public LogFunction(Browser browser, String name) {
		super(browser, name);
	}

	@Override
	public Object function(Object[] arguments) {
		System.out.println(arguments[0]);
		return null;
	}
	
}

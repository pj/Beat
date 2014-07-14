package beat.views;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import beat.Activator;

public class BeatHTMLView extends ViewPart {

	public Browser browser;
	
	public void update(String url){
		//browser.setText(url);
		
		browser.setUrl(url);

	}
	
	@Override
	public void createPartControl(Composite parent) {
		String os = System.getProperty("os.name");
		if(os.contains("Windows")){
			browser = new Browser(parent, SWT.MOZILLA);
		}else{
			browser = new Browser(parent, 0);
		}
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}

	
}

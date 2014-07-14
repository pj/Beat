package beat;

import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;

import beat.views.BeatHTMLView;

public class BrowserProgressListener implements ProgressListener {

	BeatHTMLView view;
	
	public BrowserProgressListener(BeatHTMLView view) {
		super();
		this.view = view;
	}

	@Override
	public void completed(ProgressEvent event) {
		view.browser.execute("setResize();");
	}
	
	@Override
	public void changed(ProgressEvent event) {}

}

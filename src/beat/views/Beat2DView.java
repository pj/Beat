package beat.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class Beat2DView extends ViewPart {

	Canvas canvas;
	
	@Override
	public void createPartControl(Composite parent) {
		canvas = new Canvas(parent, SWT.NONE);	    
		
	    canvas.setSize(150, 150);
	    canvas.setLocation(20, 20);

	    GC gc = new GC(canvas);
	    gc.drawRectangle(10, 10, 40, 45);
	    gc.drawOval(65, 10, 30, 35);
	    gc.drawLine(130, 10, 90, 80);
	    gc.drawPolygon(new int[] { 20, 70, 45, 90, 70, 70 });
	    gc.drawPolyline(new int[] { 10, 120, 70, 100, 100, 130, 130, 75 });
	    gc.dispose();
	    
	    canvas.redraw();
	}

	@Override
	public void setFocus() {
		canvas.setFocus();
	    canvas.redraw();
	}

}

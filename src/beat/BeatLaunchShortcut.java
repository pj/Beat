package beat;

import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

public class BeatLaunchShortcut implements ILaunchShortcut {

	public void launch(ISelection selection, String mode) {
		System.out.println("Running from selection");
	}

	public void launch(IEditorPart editor, String mode) {
		System.out.println("Running from editor");
	}

}

package beat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import beat.functions.DrawSetupFunction;
import beat.functions.GetColorFunction;
import beat.functions.GetDrawPoints;
import beat.functions.GetDrawType;
import beat.functions.GetThreadName;
import beat.functions.LogFunction;
import beat.functions.SetColumnWidth;
import beat.views.Beat2DView;
import beat.views.BeatHTMLView;

@SuppressWarnings("restriction")
public class BeatLauncher extends JavaLaunchDelegate {
	
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
//		System.out.println("Entry:");
		final long start_time = System.currentTimeMillis();
		
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		monitor.beginTask(MessageFormat.format("{0}...", new String[] { configuration.getName() }), 4);

		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Verifying_launch_attributes____1);

		String mainTypeName = verifyMainTypeName(configuration);
		// IVMRunner runner = getVMRunner(configuration, mode);

		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) {
			workingDirName = workingDir.getAbsolutePath();
		}

		// Environment variables
		String[] envp = getEnvironment(configuration);

		// Program & VM args
		String pgmArgs = getProgramArguments(configuration);
		String vmArgs = getVMArguments(configuration);
		ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);

		// VM-specific attributes
		Map vmAttributesMap = getVMSpecificAttributesMap(configuration);

		// Classpath
		String[] classpath = getClasspath(configuration);

		// Create VM config
		VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
		runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
		runConfig.setEnvironment(envp);
		runConfig.setVMArguments(execArgs.getVMArgumentsArray());
		runConfig.setWorkingDirectory(workingDirName);
		runConfig.setVMSpecificAttributesMap(vmAttributesMap);

		// Bootpath
		runConfig.setBootClassPath(getBootpath(configuration));

		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}

		// stop in main
		prepareStopInMain(configuration);

		// done the verification phase
		monitor.worked(1);

		monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Creating_source_locator____2);
		// set the default source locator if required
		setDefaultSourceLocator(launch, configuration);
		monitor.worked(1);

		// preprocess source and compile
		//ProbeCompiler preprocessor = new ProbeCompiler(this);
		
		//preprocessor.preprocessAndCompile(configuration);
		
		JDTProcessor jdtp = new JDTProcessor(this);
		
		try {
			jdtp.addProbes(configuration);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		String[] instArgs = runConfig.getVMArguments();

		runConfig.setVMArguments(instArgs);

		IVMRunner runner = getVMRunner(configuration, mode);

		// This is for the experimental dtrace runner
		//BeatDtraceMacVMRunner runner = new BeatDtraceMacVMRunner(getVMInstall(configuration), getJavaProject(configuration));

		// Launch the configuration - 1 unit of work
		runner.run(runConfig, launch, monitor);

		monitor.worked(1);

		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}

		monitor.subTask("Displaying Visualization");

		//System.out.println("Pre render: " + (System.currentTimeMillis() - start_time));
		
		// show visualization
		try {
			
			Visualization visualization = new Visualization();
			String html = visualization.renderVisualizationJRuby(this, configuration);
			
//			System.out.println("After render: " + (System.currentTimeMillis() - start_time));
			
			// write string to file
			File directory = getWorkingDirectory(configuration);
			
			File file = new File(directory, "visualization.html");
			
			FileWriter fw = new FileWriter(file);
			
			fw.append(html);
			
			fw.close();
			
//			System.out.println("After Output: " + (System.currentTimeMillis() - start_time));
			
			displayVisualization(configuration, file.getAbsolutePath(), visualization);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}

		monitor.done();
		
//		System.out.println("Exit: " + (System.currentTimeMillis() - start_time));
	}

	private void displayVisualization(ILaunchConfiguration configuration, final String html, final Visualization visualization) throws CoreException, FileNotFoundException, IOException, ClassNotFoundException {
		
		// open visualization
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				String viewId = "beat.views.BeatHTMLView";

				IWorkbench workbench = PlatformUI.getWorkbench();

				IWorkbenchWindow mainWindow = workbench
						.getActiveWorkbenchWindow();

				try {

					BeatHTMLView view = (BeatHTMLView) mainWindow.getActivePage().showView(viewId);
					
					// Beat2DView view = (Beat2DView) mainWindow.getActivePage().showView(viewId);
					
					// custom functions
					new LogFunction(view.browser, "log");
					new DrawSetupFunction(view.browser, "drawSetup", visualization);
					new GetColorFunction(view.browser, "getDrawColor", visualization);
					new GetDrawType(view.browser, "getDrawType", visualization);
					new GetDrawPoints(view.browser, "getDrawPoints", visualization);
					new SetColumnWidth(view.browser, "setColumnWidth", visualization);
					new GetThreadName(view.browser, "getThreadName", visualization);
					
					view.browser.addProgressListener(new BrowserProgressListener(view));
					
					view.update(html);
					
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
}

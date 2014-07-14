package beat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import beat.model.ObjectData;
import beat.model.SourceData;

public class DataLoader {
	static BufferedReader loadSwitchData(IProject project) throws CoreException {
		// switch data file
		IFile switchData = project.getFile("dtracedata/beat_dtrace_out");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(switchData.getContents()));
		
		return br;
	}

	static List<BufferedReader> loadThreadData(IProject project)
			throws CoreException {
		IFolder eventDataFolder = project.getFolder("beat_thread_data");	
		
		ArrayList<BufferedReader> eventData = new ArrayList<BufferedReader>();
		
		IResource[] files = eventDataFolder.members();
					
		for(IResource rfile : files){
			if(rfile.getType() == IResource.FILE){
				IFile file = (IFile)rfile;
				
				eventData.add(new BufferedReader(new InputStreamReader(file.getContents())));
			}
		}
		return eventData;
	}
	
	static void getObjectSource(BeatLauncher launcher,
			String clazz, ILaunchConfiguration configuration,
			SourceData source)
			throws CoreException, JavaModelException {

			IJavaProject project = launcher.getJavaProject(configuration);
			IType type = project.findType(clazz);

			IResource resource = type.getResource();
			InputStream is = ((IFile) resource).getContents();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			ArrayList<String> listLines = new ArrayList<String>();

			String line;
			
			StringBuilder sb = new StringBuilder();
			
			try {
				while ((line = in.readLine()) != null) {
					String l2 = line + "\n";
					sb.append(l2);
					listLines.add(l2);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			source.sourceString = sb.toString();
			source.source = listLines.toArray(new String[] {});
	}
	
	static void addSource(BeatLauncher launcher,
			ILaunchConfiguration configuration, ObjectData object, Map<String, SourceData> sources)
			throws CoreException, JavaModelException {
		/*
		 * Add object source
		 */
		if(sources.containsKey(object.clazz)){
			object.source = sources.get(object.clazz);
		}else{
			SourceData source = new SourceData();
			source.clazz = object.clazz;
			DataLoader.getObjectSource(launcher, object.clazz, configuration, source);
			// source.sourceString
			object.source = source;
			sources.put(source.clazz, source);
		}
	}
}

package beat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.jruby.exceptions.RaiseException;

import beat.collector.*;

// Experimental jdt probe inserter
public class JDTProcessor {
	BeatLauncher beatLauncher;

	public JDTProcessor(BeatLauncher beatLauncher) {
		this.beatLauncher = beatLauncher;
	}

	public void addProbes(ILaunchConfiguration configuration)
			throws JavaModelException, CoreException, IOException,
			MalformedTreeException, BadLocationException {
		IJavaProject jp = beatLauncher.getJavaProject(configuration);

		String projectBase = jp.getProject().getLocation().toOSString();

		// get all source files
		ArrayList<ICompilationUnit> files = findFilesForPreprocessing(beatLauncher
				.getJavaProject(configuration));

		// check annotations
		ArrayList<ICompilationUnit> annotatedFiles = checkAnnotations(files);

		// process files
		List<File> sourceFiles = processFiles(annotatedFiles, projectBase);

		// copyFiles(projectBase, annotatedFiles);

		// compile
		compilerProcessedSource(configuration, projectBase, sourceFiles);
	}

	//ICompilationUnit currentICU;

	TypeDeclaration currentType;

	MethodDeclaration currentMethod;

	ArrayList<IType> annotatedTypes;

	private List<File> processFiles(ArrayList<ICompilationUnit> annotatedFiles,
			String projectBase) throws JavaModelException,
			MalformedTreeException, BadLocationException {

		// get types we need to track
		annotatedTypes = new ArrayList<IType>();
		for (ICompilationUnit icu : annotatedFiles) {
			for (IType it : icu.getTypes()) {
				if (it.isClass()) {
					IAnnotation annotation = it.getAnnotation("BeatTrace");

					if (annotation != null) {
						annotatedTypes.add(it);
					}
				}
			}
		}
		
		// find the names of all methods to use for checking methods		
		List<ASTNode> asts = recordMethodNames(annotatedFiles);
			
		List<File> sourceFiles = processCompilationUnit(projectBase, asts);
		
		return sourceFiles;
	}

	CompilationUnit compilationUnit;
	
	private List<File> processCompilationUnit(String projectBase, List<ASTNode> asts)
			throws JavaModelException, BadLocationException {
		
		List<File> sourceFiles = new ArrayList<File>();
		
		for(ASTNode root : asts) {
			if (root.getNodeType() == ASTNode.COMPILATION_UNIT) {
				CompilationUnit cu = (CompilationUnit) root;
				
				compilationUnit = cu;
				
				ICompilationUnit icu = (ICompilationUnit) cu.getJavaElement();
				
				source = icu.getSource();

				Document document = new Document(source);
				

				
				ast = root.getAST();
				
				for (Object t : cu.types()) {
					TypeDeclaration td = (TypeDeclaration) t;

					currentType = td;

					if (!td.isInterface()) {
						if (!hasBeat(td))
							continue;
						
						processMethods(td, source);

						addOIDInterface(td);
						
						writeProcessedFile(projectBase, sourceFiles, cu, icu, document);
					}
				}
			}
		}
		
		return sourceFiles;
	}

	private void writeProcessedFile(String projectBase,
			List<File> sourceFiles, CompilationUnit cu, ICompilationUnit icu,
			Document document) throws BadLocationException {
		TextEdit te = cu.rewrite(document, null);
		te.apply(document);
		// System.out.println(document.get());

		IResource icuResource = icu.getResource();

		IPath prp = icuResource.getProjectRelativePath();

		File outFile = new File(projectBase + "/preprocessor-"
				+ prp.toOSString());

		FileWriter fw;
		try {
			FileUtils.touch(outFile);
			
			fw = new FileWriter(outFile);

			fw.append(document.get());

			fw.close();

			sourceFiles.add(outFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<ASTNode> recordMethodNames(
			ArrayList<ICompilationUnit> annotatedFiles) {
		List<ASTNode> asts = new ArrayList<ASTNode>();
		
		for (ICompilationUnit icu : annotatedFiles) {
			ASTParser astp = ASTParser.newParser(AST.JLS3);

			astp.setSource(icu);

			ASTNode root = astp.createAST(new NullProgressMonitor());

			AST ast = root.getAST();
		
			asts.add(root);
			
			if (root.getNodeType() == ASTNode.COMPILATION_UNIT) {
				CompilationUnit cu = (CompilationUnit) root;
				
				cu.recordModifications();
				
				for (Object t : cu.types()) {
					TypeDeclaration td = (TypeDeclaration) t;
					if (!td.isInterface()) {
						if (!hasBeat(td))
							continue;
						for (MethodDeclaration md : td.getMethods()) {
							methodNames.add(md.getName().getFullyQualifiedName());
						}
						
						typeNames.add(td.getName().getFullyQualifiedName());
					}
				}
			}
		}
		return asts;
	}

	List<String> methodNames = new ArrayList<String>();
	List<String> typeNames = new ArrayList<String>();

	private AST ast;

	private String source;

	ArrayList<VariableDeclarationStatement> localVariables;

	List<SingleVariableDeclaration> parameters;
	
	// was there a return statement in the method?
	private boolean hasNoReturn;

	private void processMethods(TypeDeclaration td, String source) {
		for (MethodDeclaration md : td.getMethods()) {
			boolean synchronizd = (md.getModifiers() & Modifier.SYNCHRONIZED) > 0;


			Block code = md.getBody();

			currentMethod = md;

			List statements = code.statements();

			parameters = md.parameters();

			addMethodProbes(td, md, statements);

			localVariables = new ArrayList<VariableDeclarationStatement>();
			
			hasNoReturn = true;
			
			processBlock(code, td.getName().getIdentifier(), md.getName()
					.getIdentifier());

			String methodType = checkMethodType(md);
			
			// catch and rethrow exceptions
			if(methodType.equals("program") || methodType.equals("thread")){
				String eventType;
				if(methodType.equals("program")){
					eventType = "programExit";
				}else{
					eventType = "threadRunExit";
				}
				
				processMethodExceptions(md, code,  md.getName().getIdentifier(), td.getName().getIdentifier(), eventType);
			}else{
				if(hasNoReturn){
					addReturnProbe(md, td, methodType, statements);
				}
			}
		}
	}

	private void addReturnProbe(MethodDeclaration md, TypeDeclaration td, String methodType, List statements) {
		String exitEventType = "";
		
		if(methodType.equals("synchronized")){
			exitEventType = "synchronizedMethodExit";
		}else{
			exitEventType = "methodExit";
		}
			
		ASTNode node = (ASTNode) statements.get(statements.size() - 1);

		int exitLineNumber = compilationUnit.getLineNumber(md.getStartPosition() + md.getLength());//node.getStartPosition() + node.getLength());//getLineNumber(node.getStartPosition());

		MethodInvocation exitProbe = makeProbe(exitEventType, td.getName()
				.getIdentifier(), md.getName().getIdentifier(),
				exitLineNumber);

		statements.add(ast.newExpressionStatement(exitProbe));	
	}

	private void processMethodExceptions(MethodDeclaration md, Block code, String methodName, String clazz, String probeType) {
		TryStatement tryStatement = ast.newTryStatement();
		
		List catches = tryStatement.catchClauses();
		
		int exitLineNumber = compilationUnit.getLineNumber(md.getStartPosition() + md.getLength());
		
		// exceptions to catch
		
		// catch errors
		
		addCatchStatement(methodName, clazz, catches, "Error", exitLineNumber);
		
		// catch runtime exceptins
		addCatchStatement(methodName, clazz, catches, "RuntimeException", exitLineNumber);
		
		// create finally statement
		Block finallyBlock = ast.newBlock();
		
		List finallyStatements = finallyBlock.statements();
		
		List statements = code.statements();
		
		ASTNode node = (ASTNode) statements.get(statements.size() - 1);


		
		MethodInvocation finallyMi = makeProbe(probeType, clazz, methodName, exitLineNumber);
		
		ExpressionStatement finallyEs = ast.newExpressionStatement(finallyMi);
		
//		statements.add(finallyEs);
		
		finallyStatements.add(finallyEs);
		
		tryStatement.setFinally(finallyBlock);
		
		code.delete();
		
		tryStatement.setBody(code);
		Block tryBlock = ast.newBlock();
		tryBlock.statements().add(tryStatement);
		
		md.setBody(tryBlock);
	}

	private void addCatchStatement(String methodName, String clazz, List catches, String catchName, int exitLineNumber) {
		CatchClause catchClause = ast.newCatchClause();
		
		SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
		svd.setType(ast.newSimpleType(ast.newName(catchName)));
		svd.setName(ast.newSimpleName("e"));
		
		catchClause.setException(svd);
		
		// code to run on catch
		Block catchBody = ast.newBlock();
		
		List catchStatements = catchBody.statements();
		
		MethodInvocation catchMi = makeProbe("threadDeathException", clazz, methodName, exitLineNumber);
		
		ExpressionStatement catchEs = ast.newExpressionStatement(catchMi);
		
		catchStatements.add(catchEs);
		
		// catch runtime exceptions
		
		// throw statement
		ThrowStatement throwStatement = ast.newThrowStatement();
		Expression throwExpression = ast.newName("e");
		throwStatement.setExpression(throwExpression);
		
		catchStatements.add(throwStatement);
		
		catchClause.setBody(catchBody);
		
		catches.add(catchClause);
	}

	private void processBlock(Block block, String className, String methodName) {
		List statements = block.statements();
		ListIterator x = statements.listIterator();

		Statement statement;
		while (x.hasNext()) {
			statement = (Statement) x.next();
			switch (statement.getNodeType()) {
			case (ASTNode.BLOCK):
				processBlock((Block) statement, className, methodName);
				break;
			case (ASTNode.IF_STATEMENT):
				processIf((IfStatement) statement, className, methodName);
				break;
			case (ASTNode.FOR_STATEMENT):
				ForStatement fs = (ForStatement)statement;
			
				processBlock((Block)fs.getBody(), className, methodName);
				
				processLoop((Block)fs.getBody(), statement, className, methodName);
				break;
			case (ASTNode.ENHANCED_FOR_STATEMENT):
				EnhancedForStatement efs = (EnhancedForStatement) statement;
				
				processBlock((Block)efs.getBody(), className, methodName);
				
				processLoop((Block)efs.getBody(), statement, className, methodName);
				break;
			case (ASTNode.WHILE_STATEMENT):
				WhileStatement ws = (WhileStatement)statement;
							
				processBlock((Block)ws.getBody(), className, methodName);
			
				processLoop((Block)ws.getBody(), statement, className, methodName);
				break;
			case (ASTNode.DO_STATEMENT):
				DoStatement ds = (DoStatement)statement;
				
				processBlock((Block)ds.getBody(), className, methodName);
				
				processLoop((Block)ds.getBody(), statement, className, methodName);
				break;
			case (ASTNode.SWITCH_STATEMENT):
				break;
			case (ASTNode.SYNCHRONIZED_STATEMENT):
				// add call statement before block
				Statement entry = processSynchronizedAcquire(
						(SynchronizedStatement) statement, className,
						methodName);

				x.previous();
				x.add(entry);
				x.next();

				processSynchronizedBlock(((SynchronizedStatement) statement),
						className, methodName);
				
				Statement exit = processSynchronizedExit((SynchronizedStatement) statement, className,methodName);
				
				x.add(exit);
				
				break;
			case (ASTNode.EXPRESSION_STATEMENT):
				ExpressionStatement es = (ExpressionStatement) statement;
			
				Expression expression = es.getExpression();
				
				processExpression(expression, statement, x, className, methodName);
				
				break;
			case (ASTNode.VARIABLE_DECLARATION_STATEMENT):
				
			
				VariableDeclarationStatement vds = (VariableDeclarationStatement) statement;
			
				localVariables.add(vds);
			
				processVariableFragments(vds.fragments(), statement, x, className, methodName);
			
				break;
			case (ASTNode.RETURN_STATEMENT):
				hasNoReturn = false;
				int lineNo = compilationUnit.getLineNumber(statement.getStartPosition());
				MethodInvocation returnProbe = makeProbe("returnStatement",
						className, methodName, lineNo);

				ExpressionStatement returnExpStatement = ast
						.newExpressionStatement(returnProbe);

				x.previous();

				if (returnExpStatement != null)
					x.add(returnExpStatement);

				x.next();
				break;
			case (ASTNode.THROW_STATEMENT):
				ThrowStatement throwS = (ThrowStatement) statement;
			
				int lineNumberThrow = compilationUnit.getLineNumber(throwS.getStartPosition());
			
				MethodInvocation throwMI = makeProbe("thrown", className, methodName, lineNumberThrow);
				
				x.previous();
				x.add(ast.newExpressionStatement(throwMI));
				x.next();
			
				break;
			case (ASTNode.TRY_STATEMENT):
				TryStatement ts = (TryStatement) statement;

				processBlock(ts.getBody(), className, methodName);

				List catches = ts.catchClauses();
				
				for(Object n : catches){
					CatchClause cc = (CatchClause)n;
				
					Block catchBody = cc.getBody();
					
					List catchStatements = catchBody.statements();
					
					int lineNumber = compilationUnit.getLineNumber(catchBody.getStartPosition() + catchBody.getLength());
					
					MethodInvocation mi = makeProbe("exceptionEntered", className, methodName, lineNumber);
					
					catchStatements.add(0, ast.newExpressionStatement(mi));
				}
				
				break;
			}
		}
	}
	
	private Statement processSynchronizedExit(SynchronizedStatement synchronizedBlock,
			String className, String methodName) {
				

		int outLineNumber = compilationUnit.getLineNumber(synchronizedBlock.getStartPosition() + synchronizedBlock.getLength());

		MethodInvocation exitProbe = makeProbe("synchronizedBlockExit", className, methodName, outLineNumber);
		
		return ast.newExpressionStatement(exitProbe);
	}

	private void processVariableFragments(List fragments, Statement statement, ListIterator x,
			String className, String methodName) {
		
		for(Object n : fragments){
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) n;
			
			Expression initializer = fragment.getInitializer();
			
			if(initializer != null)
				processExpression(initializer, statement, x, className, methodName);
		}
	}

	private void processExpression(Expression expression, Statement statement, ListIterator x, String className, String methodName){

		switch (expression.getNodeType()) {
		case (ASTNode.CLASS_INSTANCE_CREATION):
			processMethodCall(x, (ClassInstanceCreation) expression, className, methodName, statement);
			break;
		case (ASTNode.METHOD_INVOCATION):
			processMethodCall(x, (MethodInvocation) expression, className, methodName, statement);
			break;
		case (ASTNode.SUPER_METHOD_INVOCATION):
			processMethodCall(x, (SuperMethodInvocation) expression, className, methodName, statement);
			break;
		case (ASTNode.VARIABLE_DECLARATION_EXPRESSION):
			VariableDeclarationExpression vde = (VariableDeclarationExpression) expression;
			
			processVariableFragments(vde.fragments(), statement, x, className, methodName);
			break;
		case (ASTNode.ASSIGNMENT):
			Assignment assign = (Assignment) expression;

			Expression rhs = assign.getRightHandSide();
			switch (rhs.getNodeType()) {
			case (ASTNode.CLASS_INSTANCE_CREATION):
				processMethodCall(x, (ClassInstanceCreation) rhs, className, methodName, statement);
				break;
			case (ASTNode.METHOD_INVOCATION):
				processMethodCall(x, (MethodInvocation) rhs, className, methodName, statement);
				break;
			case (ASTNode.SUPER_METHOD_INVOCATION):
				processMethodCall(x, (SuperMethodInvocation) rhs, className, methodName, statement);
				break;
			}

			break;
		}
	}
	
	private void processMethodCall(ListIterator x, ClassInstanceCreation callExpression, String className, String methodName, Statement statement){
		String[] names = getCallTypes(callExpression);
		
		processMethodInvocation(x, names, callExpression, className, methodName, statement);
	}
	
	private void processMethodCall(ListIterator x, MethodInvocation callExpression, String className, String methodName, Statement statement){
		String[] names = getCallTypes(callExpression);
		
		processMethodInvocation(x, names, callExpression, className, methodName, statement);
	}
	
	private void processMethodCall(ListIterator x, SuperMethodInvocation callExpression, String className, String methodName, Statement statement){
		String[] names = getCallTypes(callExpression);
		
		processMethodInvocation(x, names, callExpression, className, methodName, statement);
	}
	
	private String[] getCallTypes(ClassInstanceCreation invocation){
		Type constructorType = invocation.getType();
		
		String methodName = null;
		
		if(constructorType instanceof SimpleType){
			methodName = ((SimpleType)constructorType).getName().getFullyQualifiedName();
		}else if(constructorType instanceof ParameterizedType){
			Type paramType = ((ParameterizedType)constructorType).getType();
			
			if(paramType instanceof SimpleType){
				methodName = ((SimpleType)constructorType).getName().getFullyQualifiedName();
			}else if(constructorType instanceof QualifiedType){
				methodName = ((QualifiedType)constructorType).getName().getFullyQualifiedName();
			}else{
				return null;
			}
		}else if(constructorType instanceof QualifiedType){
			methodName = ((QualifiedType)constructorType).getName().getFullyQualifiedName();
		}else{
			return null;
		}
		
		return getCallTypes(methodName);
	}
	
	private String[] getCallTypes(SuperMethodInvocation invocation){
		String methodName = invocation.getName().getFullyQualifiedName();
		
		return getCallTypes(methodName);
	}
	
	private String[] getCallTypes(MethodInvocation invocation){
		String methodName = invocation.getName().getFullyQualifiedName();
		
		return getCallTypes(methodName);
	}
	
	private String[] getCallTypes(String invocationText) {		
		String[] names = new String[2];
		
		// thread wait
		if (invocationText.equals("wait")) {
			names[0] = "lockWaitStart";
			names[1] = "lockWaitEnd";
		// thread notify
		} else if (invocationText.equals("notify")) {
			names[0] = "lockNotify";
		// thread notify all
		} else if (invocationText.equals("notifyAll")) {
			names[0] = "lockNotifyAll";
			// thread start point
		} else if (invocationText.equals("start")) {
			names[0] = "threadStart";
			// thread join point
		} else if (invocationText.equals("join")) {
			names[0] = "threadJoinStart";
			names[1] = "threadJoinEnd";
			// thread sleep point
		} else if (invocationText.equals("sleep")) {
			names[0] = "threadSleepStart";
			names[1] = "threadSleepEnd";
		} else if (methodNames.contains(invocationText)){// || typeNames.contains(invocationText)){
			names[0] = "methodCall";
			names[1] = "methodCallExit";
			// check name
			//checkMethodName(invocationText, mi.getExpression());
		}
		
		return names;
	}


	private void processMethodInvocation(ListIterator x, String[] names, Expression expression, String className, String methodName, Statement statement) {
		if(names == null || names[0] == null)
			return;
		
		if(names[0].equals("threadStart")){
			processThreadStart((MethodInvocation) expression, className, methodName,  statement);
			return;
		}
		
		int lineNumber =  compilationUnit.getLineNumber(expression.getStartPosition()+expression.getLength());
		
		ExpressionStatement beforeExp = ast.newExpressionStatement(makeProbe(
				names[0], className, methodName, lineNumber));
		
		x.previous();
		x.add(beforeExp);
		x.next();
		
		if(names[1] != null){
			ExpressionStatement afterExp = ast.newExpressionStatement(makeProbe(
					names[1], className, methodName, lineNumber));

			x.add(afterExp);
		}
		
	}

	private void processThreadStart(MethodInvocation mi, String clazz, String methodName,  Statement statement) {
		// create a new expression to wrap the existing one
		Expression existingExpression = mi.getExpression();
		
		MethodInvocation threadNameInvocation = ast.newMethodInvocation();
		
		Expression newExpression = ast.newName("beat.collector.TimestampCollector");
		
		threadNameInvocation.setExpression(newExpression);
		
		threadNameInvocation.setName(ast.newSimpleName("threadStartProbe"));
		
		List arguments = threadNameInvocation.arguments();
		
		existingExpression.delete();
		
		arguments.add(existingExpression);
		
		int lineNumber = compilationUnit.getLineNumber(mi.getStartPosition());
		
		addArguments("threadStart", clazz, methodName, lineNumber, arguments);
	
		mi.setExpression(threadNameInvocation);
	}

	private boolean checkMethodName(String invocationText, Expression invocationExpression) {
		if (invocationExpression != null) {
			// complicated case of needing to look up type of variable
			if (invocationExpression instanceof SimpleName) {
				Type invocationType = findVariableType(invocationExpression);

				// is the type one of the annotated types? - if so insert probe
				if (invocationType != null && invocationType.isSimpleType()) {
					SimpleType invocationSimpleType = (SimpleType) invocationType;
					String invocationString = invocationSimpleType.getName()
					.getFullyQualifiedName();
					for (IType aType : annotatedTypes) {
						if (aType.getElementName().equals(invocationString)) {
							//start = "methodCall";
							//callMethod = invocationText;
							return true;
						}
					}
				}
			} else if (invocationExpression instanceof ThisExpression) {
				return false;
			} else {
				// do nothing ?
				return false;
			}
		} else {
			return false;
		}
		return false;
	}

//	private void newProcessLoop(String loopStart, String loopEnd,
//			String className, String methodName, ListIterator x, Statement statement) {
//		
//		int startLineNumber = compilationUnit.getLineNumber(statement.getStartPosition());
//		
//		MethodInvocation startProbe = makeProbe(loopStart, className, methodName,
//			startLineNumber);
//		
//		x.previous();
//		x.add(ast.newExpressionStatement(startProbe));
//		x.next();
//		
//		int endLineNumber = compilationUnit.getLineNumber(statement.getStartPosition() + statement.getLength());
//
//		MethodInvocation endProbe = makeProbe(loopEnd, className, methodName,endLineNumber);
//			
//		x.add(ast.newExpressionStatement(endProbe));
//	}


	private Type findVariableType(Expression invocationExpression) {
		SimpleName expName = (SimpleName) invocationExpression;

		// check locals
		for (VariableDeclarationStatement vds : localVariables) {
			for (Object x : vds.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) x;
				SimpleName variableIdentifier = vdf.getName();

				if (expName.getIdentifier().equals(variableIdentifier.getIdentifier())) {
					return vds.getType();
				}
			}
		}

		// check parameters
		for (SingleVariableDeclaration svd : parameters) {
			SimpleName variableIdentifier = svd.getName();

			if (expName.getIdentifier().equals(variableIdentifier.getIdentifier())) {
				return svd.getType();
			}
		}

		// check fields
		for (FieldDeclaration fd : this.currentType.getFields()) {
			for (Object x : fd.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) x;
				SimpleName variableIdentifier = vdf.getName();

				if (expName.getIdentifier().equals(variableIdentifier.getIdentifier())) {
					return fd.getType();
				}
			}
		}

		return null;
	}

	private Statement processSynchronizedAcquire(SynchronizedStatement statement,
			String className, String methodName) {
		int acquireLineNumber = compilationUnit.getLineNumber(statement.getStartPosition());

		MethodInvocation entryInvocation = makeProbe(
				"synchronizedBlockAcquire", className, methodName,
				acquireLineNumber);

		return ast.newExpressionStatement(entryInvocation);
	}

	private void processSynchronizedBlock(
			SynchronizedStatement synchronizedStatement, String className,
			String methodName) {
		Block synchronizedBlock = synchronizedStatement.getBody();

		processBlock(synchronizedBlock, className, methodName);

		List blockStatements = synchronizedBlock.statements();

		int inLineNumber = compilationUnit.getLineNumber(((ASTNode) blockStatements.get(0))
				.getStartPosition());

		MethodInvocation entryProbe = makeProbe("synchronizedBlockEntered", className,
				methodName, inLineNumber);

		blockStatements.add(0, ast.newExpressionStatement(entryProbe));


	}
	
	private void processLoop(Block block, Statement inStatement,
			String className, String methodName) {

		List blockStatements = block.statements();

		int inLineNumber = compilationUnit.getLineNumber(inStatement.getStartPosition());

		MethodInvocation entryProbe = makeProbe("loopIn", className, methodName,
				inLineNumber);

		blockStatements.add(0, ast.newExpressionStatement(entryProbe));

		ASTNode node = (ASTNode) blockStatements
				.get(blockStatements.size() - 1);

		int outLineNumber = compilationUnit.getLineNumber(inStatement.getStartPosition() + inStatement.getLength());

		MethodInvocation exitProbe = makeProbe("loopOut", className, methodName,
				outLineNumber);

		blockStatements.add(ast.newExpressionStatement(exitProbe));
	}
	
	private String[] getLoopProbeTypes(Statement inStatement){
		String[] probes = new String[2];

		if (inStatement instanceof ForStatement) {
			probes[0] = "forLoopIn";
			probes[1] = "forLoopOut";
		} else if (inStatement instanceof EnhancedForStatement) {
			probes[0] = "forLoopIn";
			probes[1] = "forLoopOut";
		} else if (inStatement instanceof WhileStatement) {
			probes[0] = "whileLoopIn";
			probes[1] = "whileLoopOut";
		} else if (inStatement instanceof DoStatement) {
			probes[0] = "doLoopIn";
			probes[1] = "doLoopOut";
		}
		
		return probes;
	}

	private Block createBlock(Statement statement) {
		if (statement.getNodeType() == ASTNode.BLOCK) {
			return (Block) statement;
		} else {
			// rewrite code here to place statement in a block
			Block blockStatement = ast.newBlock();
			blockStatement.statements().add(statement);

			return blockStatement;
		}
	}

	private void processIf(IfStatement ifStatement, String className,
			String methodName) {
		Block then = getBlockIf(ifStatement);
		
		processBlock(then, className, methodName);

		List thenStatements = then.statements();

		int ifLineNumber = compilationUnit.getLineNumber(ifStatement.getStartPosition());

		MethodInvocation ifInvocation = makeProbe("ifStatement", className,
				methodName, ifLineNumber);

		ExpressionStatement es = ast.newExpressionStatement(ifInvocation);
		
		thenStatements.add(0, es);

		// process else
		Statement elseStatement = ifStatement.getElseStatement();

		if (elseStatement != null) {
			processElse(ifStatement, elseStatement, className, methodName);
		}
	}

	private void processElse(IfStatement ifStatement, Statement elseStatement,
			String className, String methodName) {

		// if the block is an if statement
		if (elseStatement.getNodeType() == ASTNode.IF_STATEMENT) {
			processIf((IfStatement) elseStatement, className, methodName);
			return;
		}

		Block blockStatement = (Block)elseStatement;

		// process the block
		processBlock(blockStatement, className, methodName);

		// insert probes
		int elseLineNumber = compilationUnit.getLineNumber(elseStatement.getStartPosition());

		MethodInvocation elseInvocation = makeProbe("elseStatement", className,
				methodName, elseLineNumber);

		ExpressionStatement es = ast.newExpressionStatement(elseInvocation);
		
		blockStatement.statements().add(0, es);
	}

	private Block getBlockIf(IfStatement ifStatement){
		Statement statement = ifStatement.getThenStatement();
		
		if(statement instanceof Block){
			return (Block)statement;
		}else{
			Block newBlock = ast.newBlock();
			
			ifStatement.setThenStatement(newBlock);
			
			newBlock.statements().add(statement);
			
			return newBlock;
		}
		
	}
	
	private String checkMethodType(MethodDeclaration md){
		boolean synchronizedM = (md.getModifiers() & Modifier.SYNCHRONIZED) > 0;
		boolean publicM = (md.getModifiers() & Modifier.PUBLIC) > 0;
		boolean staticM = (md.getModifiers() & Modifier.STATIC) > 0;
		boolean voidM = isVoidMethod(md);

		String methodName = md.getName().toString();
		if (methodName.equals("main") && publicM && staticM && voidM) {// && mainArgs){
			return "program";
		} else if (methodName.equals("run") && publicM && voidM) {
			return "thread";
		} else if (synchronizedM) {
			return "synchronized";
		} else {
			return "method";
		}
	}

	private boolean isVoidMethod(MethodDeclaration md) {
		Type returnType = md.getReturnType2();

		// null is a constructor

		if (returnType == null || (returnType.isPrimitiveType() && ((PrimitiveType) returnType).getPrimitiveTypeCode() == PrimitiveType.VOID))
			return true;
		
		return false;
	}
	
	private void addMethodProbes(TypeDeclaration td, MethodDeclaration md, List statements) {

		String entryEventType = "";
		String exitEventType = "";

		String methodType = checkMethodType(md);
		
		if(methodType.equals("program")){
			entryEventType = "programEntered";
			exitEventType = "programExit";
		}else if(methodType.equals("thread")){
			entryEventType = "threadRunEntered";
			exitEventType = "threadRunExit";
		}else if(methodType.equals("synchronized")){
			entryEventType = "synchronizedMethodEntered";
			exitEventType = "synchronizedMethodExit";
		}else{
			entryEventType = "methodEntered";
			exitEventType = "methodExit";
		}
				
		int entryLineNumber = compilationUnit.getLineNumber(md.getStartPosition());//getLineNumber(md.getStartPosition());

		MethodInvocation entryProbe = makeProbe(entryEventType, td.getName()
				.getIdentifier(), md.getName().getIdentifier(), entryLineNumber);

		statements.add(0, ast.newExpressionStatement(entryProbe));
		
//		if (isVoidMethod(md) && (methodType.equals("method") || methodType.equals("synchronized"))) {
//			ASTNode node = (ASTNode) statements.get(statements.size() - 1);
//
//			int exitLineNumber = getLineNumber(node.getStartPosition());
//
//			MethodInvocation exitProbe = makeProbe(exitEventType, td.getName()
//					.getIdentifier(), md.getName().getIdentifier(),
//					exitLineNumber);
//
//			statements.add(ast.newExpressionStatement(exitProbe));
//		}
	}

	// count how many new lines before the position
	private int getLineNumber(int position) {
		int count = 0;

		for (int i = 0; i < position; i++) {
			if (source.charAt(i) == '\n') {
				count++;
			}
		}

		return count;
	}

	private MethodInvocation makeProbe(String eventType, String clazz,
			String methodName, int lineNumber) {
		MethodInvocation invocation = ast.newMethodInvocation();

		if (eventType.equals("programEntered")) {
			invocation.setName(ast.newSimpleName("programEntered"));
		} else if (eventType.equals("programExit")) {
			invocation.setName(ast.newSimpleName("programExit"));
		} else if (eventType.equals("threadRunExit")) {
			invocation.setName(ast.newSimpleName("threadRunExit"));
		} else {
			invocation.setName(ast.newSimpleName("probe"));
		}
		invocation.setExpression(ast
				.newName("beat.collector.TimestampCollector"));

		List arguments = invocation.arguments();

		addArguments(eventType, clazz, methodName, lineNumber, arguments);

		return invocation;

	}

	private void addArguments(String eventType, String clazz,
			String methodName, int lineNumber, List arguments) {
		// event type
		addEventTypeArgument(eventType, arguments);

		// object id argument
		addIdArgument(arguments);

		// event thread name
		addThreadNameArgument(arguments);

		// class name
		addClassNameArgument(clazz, arguments);

		// method name
		addMethodNameArgument(methodName, arguments);

		// line number
		addLineNumberArgument(lineNumber, arguments);
	}

	private void addLineNumberArgument(int lineNumber, List arguments) {
		NumberLiteral lineNumberLiteral = ast.newNumberLiteral(Integer
				.toString(lineNumber));

		arguments.add(lineNumberLiteral);
	}

	private void addMethodNameArgument(String methodName, List arguments) {
		StringLiteral methodNameLiteral = ast.newStringLiteral();

		methodNameLiteral.setLiteralValue(methodName);

		arguments.add(methodNameLiteral);
	}

	private void addEventTypeArgument(String eventType, List arguments) {

		FieldAccess access = ast.newFieldAccess();
		access.setName(ast.newSimpleName(eventType));

		access.setExpression(ast.newName("beat.collector.EventType"));

		arguments.add(access);
	}

	private void addClassNameArgument(String clazz, List arguments) {
		TypeLiteral tr = ast.newTypeLiteral();
		tr.setType(ast.newSimpleType(ast.newSimpleName(clazz)));

		MethodInvocation nameInvocation = ast.newMethodInvocation();

		nameInvocation.setExpression(tr);

		nameInvocation.setName(ast.newSimpleName("getName"));

		arguments.add(nameInvocation);
	}

	private static void blah() {

	}

	private void addThreadNameArgument(List arguments) {
		MethodInvocation getNameInvocation = ast.newMethodInvocation();

		getNameInvocation.setName(ast.newSimpleName("getName"));

		MethodInvocation currentThreadInvocation = ast.newMethodInvocation();

		currentThreadInvocation.setName(ast.newSimpleName("currentThread"));

		currentThreadInvocation.setExpression(ast.newSimpleName("Thread"));

		getNameInvocation.setExpression(currentThreadInvocation);

		arguments.add(getNameInvocation);
	}

	private void addOIDInterface(TypeDeclaration td) {
		// add interface
		List interfaces = td.superInterfaceTypes();
		Name name = ast.newName("beat.collector.ObjectId");
		Type type = ast.newSimpleType(name);

		interfaces.add(type);

		// add field
		List declarations = td.bodyDeclarations();

		VariableDeclarationFragment fragment = ast
				.newVariableDeclarationFragment();

		fragment.setName(ast.newSimpleName("objectId"));

		MethodInvocation invocation = ast.newMethodInvocation();

		invocation.setName(ast.newSimpleName("createId"));

		invocation.setExpression(ast.newName("beat.collector.IdCreator"));

		fragment.setInitializer(invocation);

		FieldDeclaration field = ast.newFieldDeclaration(fragment);
		field.setType(ast.newPrimitiveType(PrimitiveType.INT));

		List fieldModifiers = field.modifiers();

		fieldModifiers.addAll(ast.newModifiers(Modifier.PRIVATE));

		declarations.add(field);

		// add method

		MethodDeclaration method = ast.newMethodDeclaration();

		method.setReturnType2(ast.newPrimitiveType(PrimitiveType.INT));
		method.setName(ast.newSimpleName("getObjectId"));

		method.modifiers().addAll(ast.newModifiers(Modifier.PUBLIC));

		// method block

		Block code = ast.newBlock();

		List statements = code.statements();

		ReturnStatement returnStatement = ast.newReturnStatement();

		FieldAccess fieldAccess = ast.newFieldAccess();

		fieldAccess.setName(ast.newSimpleName("objectId"));
		fieldAccess.setExpression(ast.newThisExpression());

		returnStatement.setExpression(fieldAccess);

		statements.add(returnStatement);

		method.setBody(code);

		declarations.add(method);
	}

	private void addIdArgument(List arguments) {
		if (Modifier.isStatic(currentMethod.getModifiers())) {
			arguments.add(ast.newNumberLiteral("-1"));
		} else {
			// objectId
			MethodInvocation idInvocation = ast.newMethodInvocation();
			idInvocation.setName(ast.newSimpleName("getObjectId"));

			ParenthesizedExpression parenExp = ast.newParenthesizedExpression();

			CastExpression idCast = ast.newCastExpression();

			idCast.setExpression(ast.newThisExpression());

			SimpleType qt = ast.newSimpleType(ast
					.newName("beat.collector.ObjectId"));
			idCast.setType(qt);

			parenExp.setExpression(idCast);

			idInvocation.setExpression(parenExp);

			arguments.add(idInvocation);
		}
	}

	private boolean hasBeat(TypeDeclaration td) {
		boolean has_beat = false;

		for (Object m : td.modifiers()) {
			IExtendedModifier iem = (IExtendedModifier) m;

			if (iem.isAnnotation()) {
				Annotation a = (Annotation) iem;

				Name n = a.getTypeName();

				if (n.getFullyQualifiedName().equals("BeatTrace")) {
					has_beat = true;
					break;
				}
			}
		}
		return has_beat;
	}

	private void copyFiles(ArrayList<ICompilationUnit> annotatedFiles,
			ILaunchConfiguration configuration) throws IOException,
			CoreException {

		String projectBase = beatLauncher.getJavaProject(configuration)
				.getProject().getLocation().toOSString();

		// make sure directory exists
		File dir = new File(projectBase + "/preprocessor-src");

		FileUtils.deleteDirectory(dir);

		dir.mkdirs();

		// copy files
		for (ICompilationUnit icu : annotatedFiles) {
			IResource ir = icu.getResource();

			IPath prp = ir.getProjectRelativePath();
			IPath fp = ir.getFullPath();

			System.out.println(projectBase + "/" + prp.toString());
			// System.out.println(fp.segment(0) + "/preprocessor-" +
			// prp.removeLastSegments(1).toString());

			File dest = new File(projectBase + "/preprocessor-"
					+ prp.removeLastSegments(1).toString());
			File src = new File(projectBase + "/" + prp.toString());

			FileUtils.copyFileToDirectory(src, dest);

			// IPath destination = Path.fromPortableString("/" + fp.segment(0) +
			// "/preprocessor-" + prp.toString());
			// ir.copy(destination, true, new NullProgressMonitor());
		}
	}

	// find all source files in project
	private ArrayList<ICompilationUnit> findFilesForPreprocessing(
			IJavaProject jp) throws JavaModelException {
		ArrayList<ICompilationUnit> files = new ArrayList<ICompilationUnit>();

		for (IPackageFragmentRoot pfr : jp.getAllPackageFragmentRoots()) {
			if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
				for (IJavaElement e : pfr.getChildren()) {
					if (((IPackageFragment) e).containsJavaResources()) {
						for (ICompilationUnit icu : ((IPackageFragment) e)
								.getCompilationUnits()) {
							files.add(icu);
						}
					}
				}
			}
		}
		return files;
	}

	// check source files for @BeatTrace annotation
	private ArrayList<ICompilationUnit> checkAnnotations(
			ArrayList<ICompilationUnit> files) throws JavaModelException {
		ArrayList<ICompilationUnit> outFiles = new ArrayList<ICompilationUnit>();
		for (ICompilationUnit icu : files) {
			for (IType it : icu.getTypes()) {
				if (it.isClass()) {
					IAnnotation annotation = it.getAnnotation("BeatTrace");

					if (annotation != null) {
						outFiles.add(icu);
						break;
					}
				}
			}
		}

		return outFiles;
	}

	public void compilerProcessedSource(ILaunchConfiguration configuration,
			String projectBase, List<File> sourceFiles) throws CoreException {
		ArrayList<String> cp = new ArrayList<String>();
		cp.add("-classpath");

		// System.out.println(StringUtils.join(this.beatLauncher.getClasspath(configuration),
		// ':'));

		String separator = File.pathSeparator;
		
		cp.add(StringUtils.join(this.beatLauncher.getClasspath(configuration),
				separator));
		cp.add("-d");
		cp.add(projectBase + "/bin");

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(
				null, null, null);

		Iterable<? extends JavaFileObject> compilationUnits = fileManager
				.getJavaFileObjects(sourceFiles.toArray(new File[] {})); // use
																			// alternative
																			// method
		// reuse the same file manager to allow caching of jar files
		compiler.getTask(null, fileManager, null, cp, null, compilationUnits)
				.call();

		try {
			fileManager.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

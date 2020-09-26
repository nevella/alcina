package cc.alcina.framework.servlet.util.compiler;

import java.util.List;
import java.util.stream.Collectors;

import javax.tools.StandardJavaFileManager;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import javassist.ClassPool;

public class ServerCodeCompiler {
	// private static boolean classPathSet = false;
	private static int index = 0;

	public static void install(StandardJavaFileManager fileManager) {
		// ServerJavaFileManager.fileManager = fileManager;
	}

	public void compileAndRun(AbstractTaskPerformer parent, String value)
			throws Exception {
		/*
		 * This approach doesn't really work either 'cos the java compiler for
		 * byteassist is *real* limited
		 */
		String javaCode = value;
		List<String> imports = SEUtilities
				.matchStream(javaCode, "import (\\S+);", 1)
				.collect(Collectors.toList());
		String className = javaCode.replaceFirst("(?s).+?public class (\\S+).+",
				"$1");
		String packageName = javaCode.replaceFirst("(?s)package (.+?);.+",
				"$1");
		String runnableMethod = javaCode
				.replaceFirst("(?s).+(public void run\\(\\).+)\\}.*", "$1");
		String indexedClassName = Ax.format("%s_%s", className, index++);
		ClassPool ctPool = new ClassPool(true);
		CtClassTemplate classTemplate = new CtClassTemplate(
				Ax.format("%s.%s", packageName, indexedClassName));
		classTemplate.addImplements("java.lang.Runnable");
		for (String importClass : imports) {
			String importPackage = importClass.replaceFirst("(.+)\\..+", "$1");
			ctPool.importPackage(importPackage);
		}
		classTemplate.addMethod(runnableMethod);
		Class<? extends Runnable> clazz = classTemplate.createClass(ctPool);
		clazz.newInstance().run();
	}

	public void compileAndRunTools(AbstractTaskPerformer parent, String value)
			throws Exception {
		// String javaCode = value;
		// Matcher m = Pattern.compile("public class (\\S+)").matcher(javaCode);
		// m.find();
		// String className = m.group(1);
		// String indexedClassName = Ax.format("%s_%s", className, index++);
		// javaCode = javaCode.replace(className, indexedClassName);
		// if (!classPathSet) {
		// CachingClasspathScanner scanner = new CachingClasspathScanner("*",
		// true, false, null, Registry.MARKER_RESOURCE,
		// Arrays.asList(new String[0]));
		// scanner.getClasses();
		// String classPath = scanner.getUrls().stream().map(Object::toString)
		// .collect(Collectors.joining(":"));
		// CompilerUtils.addClassPath(classPath);
		// ServerJavaFileManager.classPath = classPath;
		// }
		// Class clazz = CompilerUtils.CACHED_COMPILER
		// .loadFromJava(indexedClassName, javaCode);
		// AbstractTaskPerformer performer = (AbstractTaskPerformer) clazz
		// .newInstance();
		// performer.runAsSubtask(parent);
	}
}

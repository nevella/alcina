package cc.alcina.framework.servlet.task;

import java.util.Arrays;
import java.util.stream.Collectors;

import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import net.openhft.compiler.CompilerUtils;

public abstract class TaskCompileAndRunBase extends AbstractTaskPerformer {
	private static boolean classPathSet = false;

	protected abstract String getCompiledClassName();

	@Override
	protected void run0() throws Exception {
		String javaCode = value;
		String className = getCompiledClassName();
		if (!classPathSet) {
			CachingClasspathScanner scanner = new CachingClasspathScanner("*",
					true, false, null, Registry.MARKER_RESOURCE,
					Arrays.asList(new String[0]));
			scanner.setUsingRemoteScanner(true);
			scanner.getClasses();
			String classPath = scanner.getUrls().stream().map(Object::toString)
					.collect(Collectors.joining(":"));
			CompilerUtils.addClassPath(classPath);
			classPathSet = true;
		}
		Class clazz = CompilerUtils.CACHED_COMPILER.loadFromJava(className,
				javaCode);
		AbstractTaskPerformer performer = (AbstractTaskPerformer) clazz
				.newInstance();
		performer.runAsSubtask(this);
	}
}

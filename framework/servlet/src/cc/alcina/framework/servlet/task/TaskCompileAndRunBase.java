package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import net.openhft.compiler.CompilerUtils;

public abstract class TaskCompileAndRunBase extends AbstractTaskPerformer {
	protected abstract String getCompiledClassName();

	@Override
	protected void run0() throws Exception {
		String javaCode = value;
		String className = getCompiledClassName();
		Class clazz = CompilerUtils.CACHED_COMPILER.loadFromJava(className,
				javaCode);
		AbstractTaskPerformer performer = (AbstractTaskPerformer) clazz
				.newInstance();
		performer.runAsSubtask(this);
	}
}

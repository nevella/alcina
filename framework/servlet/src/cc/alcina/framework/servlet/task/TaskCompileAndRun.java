package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import net.openhft.compiler.CompilerUtils;

public class TaskCompileAndRun extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		String javaCode = value;
		String className = "cc.alcina.framework.servlet.task.TaskCompileAndRunLocal";
		Class clazz = CompilerUtils.CACHED_COMPILER.loadFromJava(className,
				javaCode);
		AbstractTaskPerformer performer = (AbstractTaskPerformer) clazz
				.newInstance();
		performer.runAsSubtask(this);
	}
}

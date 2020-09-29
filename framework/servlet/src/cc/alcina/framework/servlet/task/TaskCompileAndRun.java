package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.util.compiler.ServerCodeCompiler;

public class TaskCompileAndRun extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		new ServerCodeCompiler().compileAndRun(this, value);
	}
}

package cc.alcina.framework.servlet.process;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.util.AlcinaParallel;

@Registration.Singleton
public class SelectionTraversalExecutorThreadPool
		implements SelectionTraversal.Executor {
	public static final String CONTEXT_SERIAL = SelectionTraversalExecutorThreadPool.class
			.getName() + ".CONTEXT_SERIAL";

	public static SelectionTraversalExecutorThreadPool get() {
		return Registry.impl(SelectionTraversalExecutorThreadPool.class);
	}

	private List<Runnable> runnables = new ArrayList<>();

	private ThreadPoolExecutor executor;

	private boolean serial;

	public SelectionTraversalExecutorThreadPool() {
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
				Configuration.getInt("threadCount"),
				new NamedThreadFactory("SelectionTraversal-ExecutorImpl"));
		resetSerial();
	}

	@Override
	public void awaitCompletion(boolean serialExecution) {
		List<Runnable> runnables = this.runnables;
		this.runnables = new ArrayList<>();
		AlcinaParallel.builder().withExecutor(executor).withTransaction()
				.withSerial(isSerial() || serialExecution
						|| LooseContext.is(CONTEXT_SERIAL))
				.withRunnables(runnables).run();
	}

	public boolean isSerial() {
		return this.serial;
	}

	public void resetSerial() {
		serial = Configuration.is("serial");
	}

	public void setSerial(boolean serial) {
		this.serial = serial;
	}

	@Override
	public void submit(Runnable runnable) {
		runnables.add(new ThreadParentNamingRunnable(runnable));
	}

	class ThreadParentNamingRunnable implements Runnable {
		Runnable runnable;

		String parentThreadName;

		public ThreadParentNamingRunnable(Runnable runnable) {
			this.runnable = runnable;
			parentThreadName = Thread.currentThread().getName();
		}

		@Override
		public void run() {
			Thread currentThread = Thread.currentThread();
			String entryThreadName = currentThread.getName();
			try {
				String relationalThreadName = Ax.format("%s--%s",
						entryThreadName, parentThreadName);
				currentThread.setName(relationalThreadName);
				runnable.run();
			} finally {
				currentThread.setName(entryThreadName);
			}
		}
	}

	public void close() {
		executor.close();
	}
}

package cc.alcina.framework.servlet.process;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.util.AlcinaParallel;

@Registration.Singleton
public class SelectionTraversalExecutorThreadPool
		implements SelectionTraversal.Executor {
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
	public void awaitCompletion() {
		List<Runnable> runnables = this.runnables;
		this.runnables = new ArrayList<>();
		AlcinaParallel.builder().withExecutor(executor).withTransaction()
				.withSerial(isSerial()).withRunnables(runnables).run();
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
		runnables.add(runnable);
	}
}

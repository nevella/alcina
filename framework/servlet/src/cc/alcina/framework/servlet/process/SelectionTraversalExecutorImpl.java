package cc.alcina.framework.servlet.process;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.util.AlcinaParallel;

@Registration.Singleton
public class SelectionTraversalExecutorImpl
		implements SelectionTraversal.Executor {
	public static SelectionTraversalExecutorImpl get() {
		return Registry.impl(SelectionTraversalExecutorImpl.class);
	}

	private List<Runnable> runnables = new ArrayList<>();

	private ThreadPoolExecutor executor;

	private boolean serial;

	public SelectionTraversalExecutorImpl() {
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
				ResourceUtilities.getInteger(
						SelectionTraversalExecutorImpl.class, "threadCount"),
				new NamedThreadFactory("SiteTraversal-ExecutorImpl"));
		serial = ResourceUtilities.is(SelectionTraversalExecutorImpl.class,
				"serial");
	}

	@Override
	public void awaitCompletion() {
		List<Runnable> runnables = this.runnables;
		this.runnables = new ArrayList<>();
		AlcinaParallel.builder().withExecutor(executor).withTransaction()
				.withSerial(serial).withRunnables(runnables).run();
	}

	public boolean isSerial() {
		return this.serial;
	}

	public void setSerial(boolean serial) {
		this.serial = serial;
	}

	@Override
	public void submit(Runnable runnable) {
		runnables.add(runnable);
	}
}
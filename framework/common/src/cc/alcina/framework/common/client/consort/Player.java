package cc.alcina.framework.common.client.consort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.CommonUtils;

/*
 * -- dependencies :: always attempt to resolve -- preconditions :: if non-null,
 * wait til met before player becomes active -- provides :: use to satisfy other
 * dependencies (only run if required, as well)
 *
 */
public abstract class Player<D> {
	public static final transient int LOW = 1;

	public static final transient int PRIORITY_NORMAL = 100;

	public static final transient int PRIORITY_IMMEDIATE = 1000;

	protected Runnable runnable;

	protected Consort<D> consort;

	private boolean asynchronous;

	private List<D> requires = new ArrayList<D>();

	private List<D> provides = new ArrayList<D>();

	private long start;

	protected Player() {
	}

	public Player(Runnable runnable) {
		this.runnable = runnable;
	}

	public void addProvides(D state) {
		provides.add(state);
	}

	public void addRequires(D state) {
		requires.add(state);
	}

	public void cancel() {
	}

	public boolean canRunInParallelWith(Player<D> otherPlayer) {
		return false;
	}

	public Consort<D> getConsort() {
		return this.consort;
	}

	protected Logger getLogger() {
		return consort.logger;
	}

	public Collection<D> getPreconditions() {
		return Collections.emptyList();
	}

	public int getPriority() {
		return PRIORITY_NORMAL;
	}

	/**
	 * Important - unenforced is the idea that, for a non-parallel machine, only
	 * one state will be provided per player - still working on this, but see
	 * Consort.satisfiesSomeSoughtDependenciesOrIsNotASatisfier
	 */
	public Collection<D> getProvides() {
		return provides;
	}

	public Collection<D> getRequires() {
		return requires;
	}

	public long getStart() {
		return this.start;
	}

	public boolean isAllowEqualPriority() {
		return false;
	}

	public boolean isAsynchronous() {
		return this.asynchronous;
	}

	public boolean isCancellable() {
		return true;
	}

	public boolean isPerConsortSingleton() {
		return false;
	}

	public boolean isRemoveAfterPlay() {
		return getProvides().isEmpty();
	}

	protected void logToInfo(String string, Object... args) {
		consort.logger.info(string, args);
	}

	public void onFailure(Throwable caught) {
		consort.onFailure(caught);
	}

	public void play(boolean replaying) {
		if (replaying) {
			((LoopingPlayer) this).loop();
		} else {
			start = System.currentTimeMillis();
			runnable.run();
		}
		if (!isAsynchronous() && getProvides().size() <= 1) {
			wasPlayed();
		}
	}

	public String provideNameForTransitions() {
		return getClass().getSimpleName();
	}

	public void removeRequires(D... requiresStates) {
		requires.removeAll(Arrays.asList(requiresStates));
	}

	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}

	public void setConsort(Consort<D> consort) {
		this.consort = consort;
	}

	public String shortName() {
		return CommonUtils.simpleClassName(getClass());
	}

	protected void wasPlayed() {
		consort.wasPlayed(this);
	}

	protected void wasPlayed(D dep) {
		consort.wasPlayed(this, dep == null ? Collections.EMPTY_LIST
				: Collections.singletonList(dep));
	}

	public abstract static class RepeatingCommandPlayer<D> extends Player<D>
			implements Runnable, RepeatingCommand {
		protected RepeatingCommand delegate;

		public RepeatingCommandPlayer() {
			super(null);
			setAsynchronous(true);
			runnable = this;
		}

		@Override
		public boolean execute() {
			if (!consort.isRunning()) {
				return false;
			}
			try {
				boolean result = executeRepeatingCommand();
				if (result == false) {
					wasPlayed();
				}
				return result;
			} catch (Throwable e) {
				consort.onFailure(e);
				return false;
			}
		}

		protected boolean executeRepeatingCommand() {
			return delegate.execute();
		}
	}

	public abstract static class RunnableAsyncCallbackPlayer<C, D>
			extends Player<D> implements Runnable, AsyncCallback<C> {
		public RunnableAsyncCallbackPlayer() {
			super(null);
			setAsynchronous(true);
			runnable = this;
		}

		@Override
		public void onSuccess(C result) {
			wasPlayed();
		}
	}

	public abstract static class RunnablePlayer<D> extends Player<D>
			implements Runnable {
		public RunnablePlayer() {
			super(null);
			runnable = this;
		}
	}
}

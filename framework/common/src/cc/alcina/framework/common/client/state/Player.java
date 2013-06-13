package cc.alcina.framework.common.client.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/*
 * -- dependencies :: always attempt to resolve
 * -- preconditions :: if non-null, wait til met before player becomes active
 * -- provides :: use to satisfy other dependencies (only run if required, as well)
 *
 */
public abstract class Player<D> {
	public static final transient int LOW = 1;

	public static final transient int PRIORITY_NORMAL = 100;

	public static final transient int PRIORITY_IMMEDIATE = 1000;

	protected Runnable runnable;

	protected Consort<D, ?> consort;

	private boolean asynchronous;

	private List<D> requires = new ArrayList<D>();

	private List<D> provides = new ArrayList<D>();

	public Player(Runnable runnable) {
		this.runnable = runnable;
	}

	public void addProvides(D... providesStates) {
		provides.addAll(Arrays.asList(providesStates));
	}

	public void addRequires(D... requiresStates) {
		requires.addAll(Arrays.asList(requiresStates));
	}

	public Consort<D, ?> getConsort() {
		return this.consort;
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

	public boolean isAllowEqualPriority() {
		return false;
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

	public void play() {
		runnable.run();
		if (!isAsynchronous()) {
			wasPlayed();
		}
	}

	public void setConsort(Consort<D, ?> consort) {
		this.consort = consort;
	}

	protected void wasPlayed() {
		consort.wasPlayed(this);
	}

	protected void wasPlayed(D dep) {
		consort.wasPlayed(this, Collections.singletonList(dep));
	}
	
	public void onFailure(Throwable caught) {
		consort.onFailure(caught);
	}

	public abstract static class RunnableAsyncCallbackPlayer<C, D> extends
			Player<D> implements Runnable, AsyncCallback<C> {
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

	public abstract static class RunnablePlayer<D> extends Player<D> implements
			Runnable {
		public RunnablePlayer() {
			super(null);
			runnable = this;
		}
	}

	public boolean isAsynchronous() {
		return this.asynchronous;
	}

	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}
}

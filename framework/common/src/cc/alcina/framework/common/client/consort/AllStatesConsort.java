package cc.alcina.framework.common.client.consort;

import java.util.Arrays;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Timer;

public abstract class AllStatesConsort<E extends Enum> extends Consort<E> {
	protected Object lastCallbackResult;

	protected int timeout;

	private long startTime;

	protected Class<E> enumClass;

	private AsyncCallback<Void> endpointCallback;

	private boolean asynchronous = true;

	public AllStatesConsort(Class<E> enumClass,
			AsyncCallback<Void> endpointCallback) {
		this.enumClass = enumClass;
		this.endpointCallback = endpointCallback;
	}

	protected E finalState() {
		return Ax.last(Arrays.asList(getStates()));
	}

	protected E[] getStates() {
		return enumClass.getEnumConstants();
	}

	public boolean isAsynchronous() {
		return this.asynchronous;
	}

	public void retry(final AllStatesPlayer allStatesPlayer, final E state,
			int delay) {
		Runnable replayer = new Runnable() {
			@Override
			public void run() {
				if (System.currentTimeMillis() - startTime > timeout) {
					timedOut(allStatesPlayer, state);
					cancel();
				} else {
					runPlayer(allStatesPlayer, state);
				}
			}
		};
		Registry.impl(Timer.Provider.class).getTimer(replayer).schedule(delay);
	}

	public abstract void runPlayer(AllStatesPlayer allStatesPlayer, E next);

	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}

	private void setupPlayers() {
		// may not be in the natural ordering
		E[] states = getStates();
		for (int idx = 0; idx < states.length; idx++) {
			addPlayer(new AllStatesPlayer(idx == 0 ? null : states[idx - 1],
					states[idx]));
		}
		addEndpointPlayer(endpointCallback, true);
	}

	@Override
	public void start() {
		setupPlayers();
		this.startTime = System.currentTimeMillis();
		super.start();
	}

	protected void timedOut(AllStatesPlayer allStatesPlayer, E state) {
	}

	public class AllStatesPlayer extends EnumPlayer<E>
			implements ConsortPlayer, AsyncCallback, Runnable {
		public Consort stateConsort;

		public AllStatesPlayer(E from, E to) {
			super(from, to);
			runnable = this;
			setAsynchronous(AllStatesConsort.this.isAsynchronous());
		}

		@Override
		public Consort getStateConsort() {
			return stateConsort;
		}

		@Override
		public void onFailure(Throwable caught) {
			consort.onFailure(caught);
		}

		@Override
		public void onSuccess(Object result) {
			lastCallbackResult = result;
			if (stateConsort == null) {
				wasPlayed();
			}
		}

		@Override
		public String provideNameForTransitions() {
			return Ax.format("AllStatesPlayer [%s -> %s]", from, to);
		}

		@Override
		public void run() {
			runPlayer(this, getProvides().iterator().next());
		}

		@Override
		public String shortName() {
			return super.shortName() + " - " + getProvides().iterator().next();
		}
	}
}

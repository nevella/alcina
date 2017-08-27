package cc.alcina.framework.common.client.state;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;

public abstract class AllStatesConsort<E extends Enum> extends Consort<E> {
	protected Object lastCallbackResult;

	protected int timeout;

	private long startTime;

	public AllStatesConsort(Class<E> enumClass,
			AsyncCallback<Void> endpointCallback) {
		for (E to : enumClass.getEnumConstants()) {
			addPlayer(new AllStatesPlayer(to));
		}
		addEndpointPlayer(endpointCallback, true);
	}

	public abstract void runPlayer(AllStatesPlayer allStatesPlayer, E next);

	public void retry(final AllStatesPlayer allStatesPlayer, final E state,
			int delay) {
		Runnable replayer = new Runnable() {
			@Override
			public void run() {
				if (System.currentTimeMillis() - startTime > timeout) {
					timedOut(allStatesPlayer,state);
					cancel();
				} else {
					runPlayer(allStatesPlayer, state);
				}
			}
		};
		Registry.impl(TimerWrapperProvider.class).getTimer(replayer)
				.scheduleSingle(delay);
	}

	protected void timedOut(AllStatesPlayer allStatesPlayer, E state) {
		
	}

	@Override
	public void start() {
		this.startTime = System.currentTimeMillis();
		super.start();
	}

	public class AllStatesPlayer extends EnumPlayer<E> implements
			ConsortPlayer, AsyncCallback, Runnable {
		public Consort stateConsort;

		public AllStatesPlayer(E to) {
			super(to);
			runnable = this;
			setAsynchronous(true);
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
		public void run() {
			runPlayer(this, getProvides().iterator().next());
		}

		@Override
		public String shortName() {
			return super.shortName() + " - " + getProvides().iterator().next();
		}
	}
}

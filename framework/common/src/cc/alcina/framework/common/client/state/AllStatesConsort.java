package cc.alcina.framework.common.client.state;

import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class AllStatesConsort<E extends Enum> extends Consort<E> {
	protected Object lastCallbackResult;

	public AllStatesConsort(Class<E> enumClass,
			AsyncCallback<Void> endpointCallback) {
		for (E to : enumClass.getEnumConstants()) {
			addPlayer(new AllStatesPlayer(to));
		}
		addEndpointPlayer(endpointCallback, true);
	}

	public abstract void runPlayer(AllStatesPlayer allStatesPlayer, E next);

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

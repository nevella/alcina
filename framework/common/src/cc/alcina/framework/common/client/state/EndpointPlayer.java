package cc.alcina.framework.common.client.state;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class EndpointPlayer<D> extends RunnablePlayer<D> {
	private AsyncCallback completionCallback;

	private boolean finishes;

	protected EndpointPlayer(D requiresState) {
		super();
		addRequires(requiresState);
		this.finishes = true;
	}

	public EndpointPlayer(D requiresState, AsyncCallback completionCallback,
			boolean finishes) {
		super();
		this.completionCallback = completionCallback;
		this.finishes = finishes;
		addRequires(requiresState);
	}

	@Override
	public void run() {
	}

	@Override
	protected void wasPlayed() {
		consort.wasPlayed(this, getProvides(), !finishes);
		if (completionCallback != null) {
			completionCallback.onSuccess(null);
		}
		if (finishes) {
			consort.finished();
		}
		
	}
}
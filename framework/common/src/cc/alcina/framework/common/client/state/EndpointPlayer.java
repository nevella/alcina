package cc.alcina.framework.common.client.state;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

public class EndpointPlayer<D> extends RunnablePlayer<D> {
	private AsyncCallback completionCallback;

	private boolean finishes;

	private boolean continueWithNoExit;

	public EndpointPlayer(D requiresState, AsyncCallback completionCallback,
			boolean finishes) {
		super();
		this.completionCallback = completionCallback;
		this.finishes = finishes;
		addRequires(requiresState);
	}

	protected EndpointPlayer(D requiresState) {
		super();
		addRequires(requiresState);
		this.finishes = true;
	}

	public void continueWithNoExit() {
		continueWithNoExit = true;
	}

	@Override
	public void run() {
	}

	@Override
	protected void wasPlayed() {
		boolean didFinish = finishes && !continueWithNoExit;
		continueWithNoExit = false;
		consort.wasPlayed(this, getProvides(), !didFinish);
		if (completionCallback != null) {
			completionCallback.onSuccess(null);
		}
		if (didFinish) {
			consort.finished();
		}
	}
}
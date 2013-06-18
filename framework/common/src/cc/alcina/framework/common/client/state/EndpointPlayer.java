package cc.alcina.framework.common.client.state;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class EndpointPlayer<D> extends RunnablePlayer<D> {
	private AsyncCallback completionCallback;

	private boolean finishes;

	public EndpointPlayer(D requiresState, AsyncCallback completionCallback,
			boolean finishes) {
		super();
		this.completionCallback = completionCallback;
		this.finishes = finishes;
		addRequires(requiresState);
	}

	@Override
	public void run() {
		if (finishes) {
			wasPlayed();// make sure wasPlayed before finished, that's why the
						// explicit call
			consort.finished();
		}
		if (completionCallback != null) {
			completionCallback.onSuccess(null);
		}
	}
}
package cc.alcina.framework.common.client.state;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class EndpointPlayer<D> extends RunnablePlayer<D> {
	private AsyncCallback completionCallback;

	public EndpointPlayer(D requiresState, AsyncCallback completionCallback) {
		super();
		this.completionCallback = completionCallback;
		addRequires(requiresState);
	}

	@Override
	public void run() {
		wasPlayed();
		consort.finished();
		if (completionCallback != null) {
			completionCallback.onSuccess(null);
		}
	}
}
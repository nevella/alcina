package cc.alcina.framework.common.client.state;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ConsortSignalHandler<S> {
	public void signal(Consort consort, AsyncCallback signalHandledCallback);

	S handlesSignal();
}

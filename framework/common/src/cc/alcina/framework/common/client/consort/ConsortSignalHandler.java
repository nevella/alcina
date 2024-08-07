package cc.alcina.framework.common.client.consort;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ConsortSignalHandler<S> {
	S handlesSignal();

	public void signal(Consort consort, AsyncCallback signalHandledCallback);
}

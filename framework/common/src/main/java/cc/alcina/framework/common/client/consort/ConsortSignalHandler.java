package cc.alcina.framework.common.client.consort;

import cc.alcina.framework.gwt.client.util.AsyncCallback;

public interface ConsortSignalHandler<S> {
	S handlesSignal();

	public void signal(Consort consort, AsyncCallback signalHandledCallback);
}

package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.common.client.util.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class VoidWithFailureCallback implements AsyncCallback {
	public void onFailure(Throwable caught) {
		throw new WrappedRuntimeException(caught);
	}

	public void onSuccess(Object result) {
	}
}
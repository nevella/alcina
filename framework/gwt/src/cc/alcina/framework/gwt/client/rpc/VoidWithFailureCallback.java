package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class VoidWithFailureCallback implements AsyncCallback {
	public void onFailure(Throwable caught) {
		throw new WrappedRuntimeException(caught);
	}

	public void onSuccess(Object result) {
	}
}
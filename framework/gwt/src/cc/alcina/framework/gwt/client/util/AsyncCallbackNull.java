package cc.alcina.framework.gwt.client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class AsyncCallbackNull implements AsyncCallback {
	@Override
	public void onFailure(Throwable caught) {
		throw new WrappedRuntimeException(caught);
	}

	@Override
	public void onSuccess(Object result) {
	}
}
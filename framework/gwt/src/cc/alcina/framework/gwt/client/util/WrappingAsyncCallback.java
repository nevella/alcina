package cc.alcina.framework.gwt.client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public abstract class WrappingAsyncCallback<T> implements AsyncCallback<T> {
	public AsyncCallback<T> wrapped;

	public WrappingAsyncCallback() {
	}

	public WrappingAsyncCallback(AsyncCallback<T> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void onFailure(Throwable caught) {
		if (wrapped != null) {
			wrapped.onFailure(caught);
		} else {
			throw new WrappedRuntimeException(caught);
		}
	}

	@Override
	public void onSuccess(T result) {
		onSuccess0(result);
		if (wrapped != null) {
			wrapped.onSuccess(result);
		}
	}

	protected abstract void onSuccess0(T result);
}
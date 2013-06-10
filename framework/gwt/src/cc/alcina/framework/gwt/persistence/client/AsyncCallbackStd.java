package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class AsyncCallbackStd<T> implements
		AsyncCallback<T> {
	public static final AsyncCallback VOID_CALLBACK = new AsyncCallback() {
		@Override
		public void onSuccess(Object result) {
		}
	
		@Override
		public void onFailure(Throwable caught) {
			throw new WrappedRuntimeException(caught);
		}
	};

	@Override
	public void onFailure(Throwable caught) {
		throw new WrappedRuntimeException(caught);
	}
}
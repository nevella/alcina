package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class AsyncCallbackStd<T> implements AsyncCallback<T> {
	@Override
	public void onFailure(Throwable caught) {
		throw new WrappedRuntimeException(caught);
	}

	public static class ReloadOnSuccessCallback extends AsyncCallbackStd {
		@Override
		public void onSuccess(Object result) {
			Window.Location.reload();
		}
	}
	public static class AsyncCallbackValue<T> extends AsyncCallbackStd<T> {
		public T value;
		@Override
		public void onSuccess(T result) {
			value=result;
		}
	}
}
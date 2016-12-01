package cc.alcina.framework.gwt.client.util;

import java.util.function.Consumer;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;

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
	public static <T> AsyncCallbackStd<T> consumerForm(Consumer<T> consumer){
		return new AsyncCallbackStd<T>(){

			@Override
			public void onSuccess(T result) {
				consumer.accept(result);
			}
			
		};
	}
	
}
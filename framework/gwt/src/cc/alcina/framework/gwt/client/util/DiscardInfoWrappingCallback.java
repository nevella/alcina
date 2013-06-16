package cc.alcina.framework.gwt.client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class DiscardInfoWrappingCallback<T> implements
		AsyncCallback<T> {
	private AsyncCallback<Void> wrapped;

	public DiscardInfoWrappingCallback(AsyncCallback<Void> wrapped) {
		this.wrapped = wrapped;
	}

	public void onFailure(Throwable caught) {
		wrapped.onFailure(caught);
	}

	@Override
	public void onSuccess(T result) {
		wrapped.onSuccess(null);
	}
}
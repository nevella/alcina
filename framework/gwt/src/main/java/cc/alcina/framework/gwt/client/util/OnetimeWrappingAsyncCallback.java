package cc.alcina.framework.gwt.client.util;

public abstract class OnetimeWrappingAsyncCallback<T>
		extends WrappingAsyncCallback<T> {
	@Override
	public void onFailure(Throwable caught) {
		super.onFailure(caught);
		wrapped = null;
	}

	@Override
	public void onSuccess(T result) {
		super.onSuccess(result);
		wrapped = null;
	}
}
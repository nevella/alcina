package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public interface PersistenceCallback<T> {
	void onFailure(Throwable caught);

	void onSuccess(T result);

	public static final PersistenceCallback VOID_CALLBACK = new PersistenceCallback() {
		@Override
		public void onSuccess(Object result) {
		}

		@Override
		public void onFailure(Throwable caught) {
			throw new WrappedRuntimeException(caught);
		}
	};

	public abstract class PersistenceCallbackStd<T> implements
			PersistenceCallback<T> {
		@Override
		public void onFailure(Throwable caught) {
			throw new WrappedRuntimeException(caught);
		}
	}
}

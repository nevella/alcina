package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.gwt.client.util.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.IsCancellable;

public abstract class CancellableAsyncCallback<T>
		implements AsyncCallback<T>, IsCancellable {
	private boolean cancelled;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void onFailure(Throwable caught) {
		throw WrappedRuntimeException.wrap(caught);
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
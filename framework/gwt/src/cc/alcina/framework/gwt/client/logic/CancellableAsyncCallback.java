package cc.alcina.framework.gwt.client.logic;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.IsCancellable;

public abstract class CancellableAsyncCallback<T>
		implements AsyncCallback<T>, IsCancellable {
	private boolean cancelled;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
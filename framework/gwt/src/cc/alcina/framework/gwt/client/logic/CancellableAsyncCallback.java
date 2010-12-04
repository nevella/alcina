package cc.alcina.framework.gwt.client.logic;

import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class CancellableAsyncCallback<T> implements AsyncCallback<T> {
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	private boolean cancelled;
}
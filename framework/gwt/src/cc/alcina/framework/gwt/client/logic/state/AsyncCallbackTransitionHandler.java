package cc.alcina.framework.gwt.client.logic.state;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.gwt.client.logic.IsCancellable;

import com.google.gwt.user.client.rpc.AsyncCallback;

/*
 * Null checks allow reuse as a vanilla asyncCallback
 *
 */
public abstract class AsyncCallbackTransitionHandler<T, M extends MachineModel>
		implements MachineTransitionHandler<M>, AsyncCallback<T>, IsCancellable {
	private MachineEvent successEvent;

	private M model;

	private boolean cancelled;

	public AsyncCallbackTransitionHandler(MachineEvent successEvent) {
		this.successEvent = successEvent;
	}

	@Override
	public void onFailure(Throwable caught) {
		if (isCancelled()) {
			return;
		}
		if (model != null) {
			model.getMachine().handleAsyncException(caught, this);
		} else {
			throw new WrappedRuntimeException(caught);
		}
	}

	@Override
	public void onSuccess(T result) {
		onSuccess0(result);
		if (model != null) {
			model.getMachine().newEvent(successEvent);
		}
	}

	public abstract void onSuccess0(T result);

	@Override
	public void performTransition(M model) {
		this.model = model;
		model.setRunningCallback(this);
		start();
	}

	public abstract void start();

	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}

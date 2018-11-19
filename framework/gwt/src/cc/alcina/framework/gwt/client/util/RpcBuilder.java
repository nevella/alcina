package cc.alcina.framework.gwt.client.util;

import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.csobjects.ITaskResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

public class RpcBuilder<T> {
	private Consumer<T> inner;

	private String caption;

	private Runnable runnable;

	private NonCancellableRemoteDialog crd;

	boolean notifySuccessAsMessage;

	public void call(Runnable runnable) {
		this.runnable = runnable;
		callWithModal();
	}

	public RpcBuilder caption(String caption) {
		this.caption = caption;
		return this;
	}

	public RpcBuilder notifySuccessAsMessage() {
		notifySuccessAsMessage = true;
		return this;
	}

	public RpcBuilder onSuccess(Consumer<T> inner) {
		this.inner = inner;
		return this;
	}

	public AsyncCallback<T> wrappedSuccess() {
		return new WrappingAsyncCallback<T>() {
			@Override
			public void onFailure(Throwable caught) {
				crd.hide();
				super.onFailure(caught);
			}

			@Override
			protected void onSuccess0(T result) {
				crd.hide();
				if (notifySuccessAsMessage && result != null) {
					if (result instanceof ITaskResult
							&& !((ITaskResult) result).isOk()) {
						MessageManager.get()
								.icyCenterMessage("An error occurred - "
										+ Ax.blankTo(result.toString(),
												"no message"));
					} else {
						MessageManager.get().centerMessage(Ax
								.blankTo(result.toString(), "OK - no message"));
					}
				}
				inner.accept(result);
			}
		};
	}

	private void callWithModal() {
		crd = new NonCancellableRemoteDialog(caption, null);
		crd.show();
		runnable.run();
	}
}

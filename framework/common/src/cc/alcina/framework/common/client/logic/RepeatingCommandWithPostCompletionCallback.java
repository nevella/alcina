package cc.alcina.framework.common.client.logic;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class RepeatingCommandWithPostCompletionCallback implements
		RepeatingCommand {
	private AsyncCallback postCompletionCallback;

	private RepeatingCommand delegate;

	private boolean cancelled;

	public RepeatingCommandWithPostCompletionCallback(
			AsyncCallback postCompletionCallback, RepeatingCommand delegate) {
		this.postCompletionCallback = postCompletionCallback;
		this.delegate = delegate;
	}

	@Override
	public boolean execute() {
		if(cancelled){
			return false;
		}
		boolean shouldContinue = false;
		try {
			shouldContinue = delegate.execute();
		} catch (final Exception e) {
			Registry.impl(TimerWrapperProvider.class).scheduleDeferred(
					new Runnable() {
						@Override
						public void run() {
							postCompletionCallback.onFailure(e);
						}
					});
		}
		if (!shouldContinue) {
			Registry.impl(TimerWrapperProvider.class).scheduleDeferred(
					new Runnable() {
						@Override
						public void run() {
							postCompletionCallback.onSuccess(null);
						}
					});
		}
		return shouldContinue;
	}
	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public static class RepeatingCommandEndpoint implements RepeatingCommand {
		@Override
		public boolean execute() {
			return false;
		}
	}

	public static class RepeatingCommandEndpointWithPostCompletionCallback
			extends RepeatingCommandWithPostCompletionCallback {
		public RepeatingCommandEndpointWithPostCompletionCallback(
				AsyncCallback postCompletionCallback) {
			super(postCompletionCallback, new RepeatingCommandEndpoint());
		}
	}
}

package cc.alcina.framework.gwt.client.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class Async {
	private static Set<Object> inflight = new LinkedHashSet<>();

	public static <T> AsyncCallbackBuilder<T> callbackBuilder() {
		return new AsyncCallbackBuilder<T>();
	}

	public static RunAsyncBuilder runAsyncBuilder() {
		return new RunAsyncBuilder();
	}

	public static AsyncCallback<Void> voidCallback(Runnable runnable) {
		return Async.<Void> callbackBuilder().success(v -> runnable.run())
				.build();
	}

	public static class AsyncCallbackBuilder<T> {
		private Consumer<T> successConsumer = t -> {
		};

		private boolean cancelledBecauseExistingInflight = false;

		private Consumer<Throwable> failureConsumer = this::onFailure;

		private Runnable completionCallback;

		private Object inflightMarker;

		public AsyncCallback<T> build() {
			if (cancelledBecauseExistingInflight) {
				return new CancelledCallback();
			}
			return new BuilderCallback();
		}

		public AsyncCallbackBuilder<T>
				failure(Consumer<Throwable> failureConsumer) {
			this.failureConsumer = failureConsumer;
			return this;
		}

		public AsyncCallbackBuilder<T> success(Consumer<T> successConsumer) {
			this.successConsumer = successConsumer;
			return this;
		}

		public AsyncCallbackBuilder<T>
				withCancelInflight(AsyncCallback<T> runningCallback) {
			if (runningCallback != null) {
				((BuilderCallback) runningCallback).setCancelled(true);
			}
			return this;
		}

		public AsyncCallbackBuilder<T> withCompletion(Runnable runnable) {
			this.completionCallback = runnable;
			return this;
		}

		public AsyncCallbackBuilder<T> withInflight(Object object) {
			this.inflightMarker = object;
			cancelledBecauseExistingInflight = !inflight.add(object);
			return this;
		}

		private void onFailure(Throwable caught) {
			throw new WrappedRuntimeException(caught);
		}

		public final class BuilderCallback implements AsyncCallback<T> {
			private boolean cancelled;

			public boolean isCancelled() {
				return this.cancelled;
			}

			@Override
			public void onFailure(Throwable caught) {
				if (isCancelled()) {
					return;
				}
				onComplete();
				failureConsumer.accept(caught);
			}

			@Override
			public void onSuccess(T result) {
				if (isCancelled()) {
					return;
				}
				onComplete();
				successConsumer.accept(result);
			}

			public void setCancelled(boolean cancelled) {
				this.cancelled = cancelled;
			}

			private void onComplete() {
				if (completionCallback != null) {
					completionCallback.run();
				}
				inflight.remove(inflightMarker);
			}
		}
	}

	public static class CancelledCallback implements AsyncCallback {
		@Override
		public void onFailure(Throwable caught) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void onSuccess(Object result) {
			throw new UnsupportedOperationException();
		}
	}

	public static class RunAsyncBuilder {
		private Consumer<Throwable> failureConsumer = this::onFailure;

		private Runnable onSuccess;

		public RunAsyncCallbackImpl build() {
			return new RunAsyncCallbackImpl();
		}

		public RunAsyncBuilder failure(Consumer<Throwable> failureConsumer) {
			this.failureConsumer = failureConsumer;
			return this;
		}

		public RunAsyncBuilder success(Runnable onSuccess) {
			this.onSuccess = onSuccess;
			return this;
		}

		private void onFailure(Throwable caught) {
			throw new WrappedRuntimeException(caught);
		}

		public class RunAsyncCallbackImpl implements RunAsyncCallback {
			@Override
			public void onFailure(Throwable reason) {
				failureConsumer.accept(reason);
			}

			@Override
			public void onSuccess() {
				onSuccess.run();
			}
		}
	}
}

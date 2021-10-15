package cc.alcina.framework.gwt.client.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class Async {
	private static Set<Object> inflight = new LinkedHashSet<>();

	public static <T> AsyncCallbackBuilder<T> callbackBuilder() {
		return new AsyncCallbackBuilder<T>();
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
			return new AsyncCallback<T>() {
				@Override
				public void onFailure(Throwable caught) {
					onComplete();
					failureConsumer.accept(caught);
				}

				@Override
				public void onSuccess(T result) {
					onComplete();
					successConsumer.accept(result);
				}

				private void onComplete() {
					if (completionCallback != null) {
						completionCallback.run();
					}
					inflight.remove(inflightMarker);
				}
			};
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
}

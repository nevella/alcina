package cc.alcina.framework.gwt.client.util;

import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class Async {
	public static class AsyncCallbackBuilder<T> {
		private Consumer<T> successConsumer = t -> {
		};

		private Consumer<Throwable> failureConsumer = this::onFailure;

		private void onFailure(Throwable caught) {
			throw new WrappedRuntimeException(caught);
		}

		public AsyncCallbackBuilder<T> success(Consumer<T> successConsumer) {
			this.successConsumer = successConsumer;
			return this;
		}

		public AsyncCallbackBuilder<T>
				failure(Consumer<Throwable> failureConsumer) {
			this.failureConsumer = failureConsumer;
			return this;
		}

		public AsyncCallback<T> build() {
			return new AsyncCallback<T>() {
				@Override
				public void onFailure(Throwable caught) {
					failureConsumer.accept(caught);
				}

				@Override
				public void onSuccess(T result) {
					successConsumer.accept(result);
				}
			};
		}
	}

	public static AsyncCallbackBuilder callbackBuilder() {
		return new AsyncCallbackBuilder();
	}
}

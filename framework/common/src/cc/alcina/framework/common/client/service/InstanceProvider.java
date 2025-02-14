package cc.alcina.framework.common.client.service;

import java.util.concurrent.CountDownLatch;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;

/*
 * Notes:
 * 
 * - normally one-off and async would be the same (and they're intended form
 * non-MT GWT apps, basically)
 */
@Registration.NonGenericSubtypes(InstanceProvider.class)
public interface InstanceProvider<T> extends Registration.AllSubtypes {
	T provide(InstanceOracle.Query<T> query);

	default boolean isAsync() {
		return false;
	}

	public static abstract class Awaiter<T> implements InstanceProvider<T> {
		T instance;

		CountDownLatch loadedLatch = new CountDownLatch(1);

		protected abstract void start(Query<T> query);

		protected void loaded(T instance) {
			this.instance = instance;
			loadedLatch.countDown();
		}

		@Override
		public T provide(Query<T> query) {
			start(query);
			try {
				loadedLatch.await();
			} catch (Exception e) {
				WrappedRuntimeException.throwWrapped(e);
			}
			return instance;
		}
	}

	default boolean isOneOff() {
		return false;
	}

	default void provideAsync(Query<T> query) {
		throw new UnsupportedOperationException();
	}
}

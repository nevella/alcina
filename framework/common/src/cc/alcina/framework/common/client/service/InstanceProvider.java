package cc.alcina.framework.common.client.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;

/*
 * Notes:
 * 
 * - normally one-off and async would be the same (and they're intended for
 * non-MT GWT apps, basically)
 * 
 * TODO - server-side async executors (most) should probably have a threadpool
 */
@Registration.NonGenericSubtypes(InstanceProvider.class)
public interface InstanceProvider<T> extends Registration.AllSubtypes {
	T provide(InstanceOracle.Query<T> query);

	/**
	 * Models a parameter used by the provider, arguments passed by the
	 * submitter
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.TYPE)
	@ClientVisible
	@Repeatable(Parameter.Multiple.class)
	public @interface Parameter {
		Class<? extends InstanceQuery.Parameter> value();

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target(ElementType.TYPE)
		@ClientVisible
		public static @interface Multiple {
			Parameter[] value();
		}
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

	/*
	 * The default is for the call to be evaluated sequentially, this (single)
	 * entry point gives simpler upstream code
	 */
	default void provide(Query<T> query, Consumer<T> asyncReturn) {
		asyncReturn.accept(provide(query));
	}

	public interface Async<T> extends InstanceProvider<T> {
		@Override
		default T provide(Query<T> query) {
			throw new UnsupportedOperationException(
					"Override provide(query, asyncReturn)");
		}

		default void provide(Query<T> query, Consumer<T> asyncReturn) {
			throw new UnsupportedOperationException(
					"Override this method in implementations");
		}
	}
}

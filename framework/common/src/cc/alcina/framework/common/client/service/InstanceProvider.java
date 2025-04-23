package cc.alcina.framework.common.client.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
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
 * 
 */
@Registration.NonGenericSubtypes(InstanceProvider.class)
public interface InstanceProvider<T> extends Registration.AllSubtypes {
	T provide(InstanceOracle.Query<T> query) throws Exception;

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

	/**
	 * If true, an individual provider must be constructed for each query, even
	 * if a current provision operation is in-flight
	 */
	default boolean isOneOff() {
		return false;
	}

	/*
	 * The default is for the call to be evaluated sequentially, this (single)
	 * entry point gives simpler upstream code
	 */
	default void provide(Query<T> query,
			BiConsumer<InstanceProvider, T> asyncReturn,
			BiConsumer<InstanceProvider, Exception> asyncException) {
		try {
			asyncReturn.accept(this, provide(query));
		} catch (Exception e) {
			asyncException.accept(this, e);
		}
	}

	public static abstract class Async<T> implements InstanceProvider<T> {
		protected Query<T> query;

		protected BiConsumer<InstanceProvider, T> asyncReturn;

		protected BiConsumer<InstanceProvider, Exception> asyncException;

		@Override
		public T provide(Query<T> query) throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public void provide(Query<T> query,
				BiConsumer<InstanceProvider, T> asyncReturn,
				BiConsumer<InstanceProvider, Exception> asyncException) {
			this.query = query;
			this.asyncReturn = asyncReturn;
			this.asyncException = asyncException;
			start();
		}

		protected abstract void start();
	}
}

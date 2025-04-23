package cc.alcina.framework.common.client.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.util.HasBind;

/**
 * <p>
 * A more general approach than Registry/GWT.impl - this class provides
 * instances based on a contract, with potential eviction management and
 * supporting multiple query/single instance creation
 * <p>
 * It's intended as the backing for general object requestors - such as UIs
 * (Sequence, Traversal) as well as maybe more general contract/abstractions
 * <p>
 * Non-multithreaded GWT apps (non-romcom, or those with non-romcom variants)
 * can only use the async forms
 * <p>
 * Services objects without eviction requirements should continue to use the
 * Registry
 * <p>
 * As a general note, the contract of an InstanceOracle is really another type
 * of name (or address) - as are sets of Registry keys, or static Topic
 * instances, or the class types of ProcessObservable subtypes.
 * <p>
 * They all provide a subscription/indirection/decoupling mechanism -
 * InstanceOracle is just the latest and most complex of the provider types
 * 
 */
/*
 * Implementation sketch:
 * @formatter:off
 
- Create a querytoken for each query
- Look up the store - attach to the existing ProviderQueries entry if it exists
- Add to 
- If the existing entry has a result, fire that result


 * @formatter:on
 * 
 */
/*
 * Essentially, this acts as a broker (social mixer?) between instance
 * requestors and providers. Once a contract (modelled by a ProviderQueries) is
 * established, it shouldn't be removed - rather eviction should cause result
 * instances to be evicted (and unbind of requestors removes the requestor refs)
 * 
 * See ModelBinding for more docs on UI environment dispatch
 * 
 * Thread safety: all api operations are thread-safe - access to mutable
 * collections and potentially concurrent modifications is synchronized
 */
@Registration.Singleton
public class InstanceOracle {
	@Registration.Self
	public static class ProviderInvoker {
		/**
		 * The naive (browser) implementation just executes the runnable
		 * 
		 * @param runnable
		 */
		public void invoke(String name, Runnable runnable) {
			runnable.run();
		}
	}

	public static class Query<T> implements HasBind {
		ProviderQueries<T> token;

		Class<T> clazz;

		List<InstanceQuery.Parameter<?>> parameters = new ArrayList<>();

		Consumer<T> instanceConsumer;

		/*
		 * once unbound, a query cannot be rebound
		 */
		boolean bound = true;

		Consumer<Exception> exceptionConsumer;

		Runnable reemitRunnable;

		boolean async;

		/*
		 * Discard the query after the consumer is fired once
		 */
		boolean oneOff = true;

		/*
		 * Discard any existing query result
		 */
		boolean refresh;

		Query(Class<T> clazz) {
			this.clazz = clazz;
		}

		@Override
		public int hashCode() {
			return Objects.hash(clazz, parameters);
		}

		public Query<T>
				addParameters(InstanceQuery.Parameter<?>... parameters) {
			addParameters(Arrays.asList(parameters));
			return this;
		}

		public Query<T>
				addParameters(List<InstanceQuery.Parameter<?>> parameters) {
			this.parameters.addAll(parameters);
			return this;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Query) {
				Query o = (Query) obj;
				return clazz.equals(o.clazz)
						&& Objects.equals(parameters, o.parameters);
			} else {
				return super.equals(obj);
			}
		}

		public T get() {
			Preconditions.checkState(!async);
			return submit0().await();
		}

		/*
		 * Don't return anything (since ProviderQueries is intentionally
		 * package-private)
		 */
		public void submit() {
			submit0();
		}

		/**
		 * Just semantics - wait until the object is gettable, but don't return
		 * (indicates that the requestor is awaiting a state, not requiring the
		 * object itself)
		 */
		public void await() {
			get();
		}

		public void bind() {
			this.bound = true;
		}

		@Override
		public void unbind() {
			bound = false;
			InstanceOracle.get().checkEviction();
		}

		public Query<T> withInstanceConsumer(Consumer<T> instanceConsumer) {
			this.instanceConsumer = instanceConsumer;
			return this;
		}

		public Query<T> withInstanceSignal(Runnable runnable) {
			return withInstanceConsumer(o -> runnable.run());
		}

		public Query<T>
				withExceptionConsumer(Consumer<Exception> exceptionConsumer) {
			this.exceptionConsumer = exceptionConsumer;
			return this;
		}

		public Query<T> withOneOff(boolean oneOff) {
			this.oneOff = oneOff;
			return this;
		}

		public Query<T> withRefresh(boolean refresh) {
			this.refresh = refresh;
			return this;
		}

		public void reemit() {
			if (reemitRunnable != null) {
				reemitRunnable.run();
			}
		}

		public <PT extends InstanceQuery.Parameter> PT
				typedParameter(Class<PT> clazz) {
			return optionalParameter(clazz).orElse(null);
		}

		public <PT extends InstanceQuery.Parameter> Optional<PT>
				optionalParameter(Class<PT> clazz) {
			return (Optional<PT>) parameters.stream()
					.filter(p -> p.getClass() == clazz).findFirst();
		}

		public Query<T> withAsync(boolean async) {
			this.async = async;
			return this;
		}

		/* Note that this will block if the query is not async */
		ProviderQueries<T> submit0() {
			return InstanceOracle.get().submit(this);
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("clazz", clazz, "parameters",
					parameters);
		}

		/**
		 * Clear the existing value, if any
		 */
		public void invalidate() {
			InstanceOracle.get().invalidate(this);
		}

		public <PT extends InstanceQuery.Parameter, V> V
				parameterValue(Class<PT> clazz) {
			return (V) typedParameter(clazz).getValue();
		}
	}

	/*
	 * handles synchronized access to uniqueTokens
	 */
	class Store {
		Map<ProviderQueries, ProviderQueries> uniqueProviderQueries = new LinkedHashMap<>();

		synchronized <T> ProviderQueries<T> getProviderQueries(Query<T> query) {
			ProviderQueries<T> test = new ProviderQueries<>(query);
			/*
			 * NOTE - if the InstanceProvider call is already in progress, and
			 * InstanceProvider is a one-off, the ProviderQueries equals() test
			 * will fail, so a new token will be provided
			 */
			ProviderQueries<T> active = uniqueProviderQueries
					.computeIfAbsent(test, k -> test);
			active.add(query);
			if (query.refresh) {
				active.discardExistingInstance();
			}
			/*
			 * ordering is critical here, submit after registering receiver
			 */
			active.ensureSubmitted();
			return active;
		}

		synchronized void checkEviction() {
			Iterator<Entry<ProviderQueries, ProviderQueries>> itr = uniqueProviderQueries
					.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<ProviderQueries, ProviderQueries> entry = itr.next();
				if (entry.getKey().checkEviction()) {
					itr.remove();
				}
			}
		}
	}

	/**
	 * <p>
	 * Models the link between a provider and the 1-to-many queries it satisfies
	 * <p>
	 * Instances are unique modulo {@link #definingQuery} -
	 * {@link Query#equals(Object)}
	 */
	static class ProviderQueries<T> {
		boolean firing;

		/**
		 * 
		 * @return true if the instance has no awaiting queries
		 */
		synchronized boolean checkEviction() {
			if (firing) {
				return false;
			}
			awaitingQueries.removeIf(qs -> !qs.query.bound);
			return awaitingQueries.isEmpty();
		}

		/**
		 * Models how the provider has satisfied this particular query. The
		 * check against lastAccepted instance prevents possible
		 * double-consumption due to the provider calling back (with the
		 * instance) during submit()
		 */
		class QueryState {
			Query<T> query;

			T lastAcceptedInstance;

			Exception lastAcceptedException;

			Consumer<Runnable> dispatch;

			QueryState(Query<T> query) {
				this.query = query;
				query.reemitRunnable = this::reemit;
				dispatch = DispatchRefProvider.get().getDispatch();
			}

			void reemit() {
				if (instance != null) {
					lastAcceptedInstance = null;
					acceptInstanceOrException();
				}
			}

			void acceptInstanceOrException() {
				if (instance != null && query.instanceConsumer != null
						&& lastAcceptedInstance != instance) {
					lastAcceptedException = null;
					lastAcceptedInstance = instance;
					// this possibly causes query.instanceConsumer to be called
					// on the originating (UI) thread
					dispatch.accept(() -> {
						query.instanceConsumer.accept(lastAcceptedInstance);
					});
					if (query.oneOff) {
						query.unbind();
					}
				}
				if (exception != null && query.exceptionConsumer != null
						&& lastAcceptedException != exception) {
					lastAcceptedException = exception;
					lastAcceptedInstance = null;
					// this possibly causes query.instanceConsumer to be called
					// on the originating (UI) thread
					dispatch.accept(() -> query.exceptionConsumer
							.accept(lastAcceptedException));
					if (query.oneOff) {
						query.unbind();
					}
				}
			}
		}

		Query<T> definingQuery;

		List<QueryState> awaitingQueries = new ArrayList<>();

		InstanceProvider<T> provider;

		T instance;

		volatile CountDownLatch awaitLatch = null;

		Exception exception;

		ProviderQueries(Query<T> query) {
			this.definingQuery = query;
			query.token = this;
		}

		@Override
		public int hashCode() {
			return definingQuery.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ProviderQueries) {
				ProviderQueries o = (ProviderQueries) obj;
				if (isOneOff() || o.isOneOff()) {
					return obj == this;
				} else {
					return definingQuery.equals(o.definingQuery);
				}
			} else {
				return super.equals(obj);
			}
		}

		synchronized void discardExistingInstance() {
			instance = null;
			exception = null;
		}

		/*
		 * pass the result instance to any consumers
		 */
		synchronized void passToConsumers() {
			try {
				firing = true;
				awaitingQueries.forEach(QueryState::acceptInstanceOrException);
			} finally {
				firing = false;
				checkEviction();
			}
		}

		/*
		 * not synchronized here, effectively synchronized in Store#getToken.
		 * This will be called when the first ProviderQueries for the Query is
		 * created
		 */
		void ensureSubmitted() {
			if (provider == null) {
				provider = Registry.query(InstanceProvider.class)
						.addKeys(definingQuery.clazz).impl();
				ensureLatch();
				/*
				 * In non-browser environments, this will run off-thread
				 */
				String name = Ax.format("instance-oracle::%s",
						NestedName.get(provider));
				Registry.impl(ProviderInvoker.class).invoke(name,
						() -> provider.provide(definingQuery,
								this::acceptInstance, this::acceptException));
			}
		}

		/*
		 * both sync + async instance provision route back to here
		 * 
		 * note this is not synchronized (can't be, since it might easily
		 * conflict with await()) - but it calls into passToConsumers, which is.
		 * That path has been checked
		 */
		void acceptInstance(InstanceProvider provider, T instance) {
			if (provider != this.provider) {
				return;
			}
			this.instance = instance;
			awaitLatch.countDown();
			passToConsumers();
		}

		void acceptException(InstanceProvider provider, Exception exception) {
			if (provider != this.provider) {
				return;
			}
			exception.printStackTrace();
			this.exception = exception;
			awaitLatch.countDown();
			passToConsumers();
		}

		synchronized void add(Query<T> query) {
			awaitingQueries.add(new QueryState(query));
		}

		boolean isOneOff() {
			return provider != null && provider.isOneOff();
		}

		// SE only
		synchronized void ensureLatch() {
			if (awaitLatch == null) {
				awaitLatch = new CountDownLatch(1);
			}
		}

		T await() {
			Preconditions
					.checkState(Al.isMultiThreaded() && !provider.isAsync());
			ensureLatch();
			try {
				awaitLatch.await();
				return instance;
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		synchronized void invalidate() {
			provider = null;
			instance = null;
			exception = null;
		}
	}

	public static <T> Query<T> query(Class<T> clazz) {
		return new Query<>(clazz);
	}

	<T> void invalidate(Query<T> query) {
		store.getProviderQueries(query).invalidate();
	}

	static InstanceOracle get() {
		return Registry.impl(InstanceOracle.class);
	}

	Store store = new Store();

	<T> ProviderQueries<T> submit(Query<T> query) {
		query.bind();
		ProviderQueries<T> providerQueries = store.getProviderQueries(query);
		providerQueries.ensureSubmitted();
		checkEviction();
		if (!query.async && !providerQueries.provider.isAsync()) {
			providerQueries.await();
		}
		providerQueries.passToConsumers();
		return providerQueries;
	}

	void checkEviction() {
		store.checkEviction();
	}
}

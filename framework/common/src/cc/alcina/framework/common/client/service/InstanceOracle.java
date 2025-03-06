package cc.alcina.framework.common.client.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Al;
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
 * 
 */
@Registration.Singleton
public class InstanceOracle {
	static InstanceOracle get() {
		return Registry.impl(InstanceOracle.class);
	}

	public static <T> Query<T> query(Class<T> clazz) {
		return new Query<>(clazz);
	}

	<T> ProviderQueries<T> submit(Query<T> query) {
		ProviderQueries<T> active = store.getToken(query);
		active.ensureSubmitted();
		checkEviction();
		if (!query.async) {
			active.await();
		}
		active.passToConsumers();
		return active;
	}

	void checkEviction() {
		// TODO iterate through the tokens, check the provider evictionpolicy,
		// possibly setting a timer for the next eviction check
	}

	Store store = new Store();

	/*
	 * handles synchronized access to uniqueTokens
	 */
	class Store {
		Map<ProviderQueries, ProviderQueries> uniqueTokens = new LinkedHashMap<>();

		synchronized <T> ProviderQueries<T> getToken(Query<T> query) {
			ProviderQueries<T> test = new ProviderQueries<>(query);
			/*
			 * NOTE - if the InstanceProvider call is already in progress, and
			 * InstanceProvider is a one-off, the ProviderQueries equals() test
			 * will fail, so a new token will be provided
			 */
			ProviderQueries<T> active = uniqueTokens.computeIfAbsent(test,
					k -> test);
			active.add(query);
			/*
			 * ordering is critical here, submit after registering receiver
			 */
			active.ensureSubmitted();
			return active;
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
		/**
		 * Models how the provider has satisfied this particular query
		 */
		class QueryState {
			Query<T> query;

			boolean canAccept = true;

			T lastAcceptedInstance;

			QueryState(Query<T> query) {
				this.query = query;
			}

			void acceptInstance() {
				if (canAccept && instance != null
						&& query.instanceConsumer != null
						&& lastAcceptedInstance != instance) {
					lastAcceptedInstance = instance;
					query.instanceConsumer.accept(lastAcceptedInstance);
				}
			}
		}

		Query<T> definingQuery;

		List<QueryState> awaitingQueries = new ArrayList<>();

		ProviderQueries(Query<T> query) {
			this.definingQuery = query;
			query.token = this;
		}

		/*
		 * pass the result instance to any consumers
		 */
		synchronized void passToConsumers() {
			awaitingQueries.forEach(QueryState::acceptInstance);
		}

		InstanceProvider<T> provider;

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
				provider.provide(definingQuery, this::acceptInstance);
			}
		}

		/*
		 * both sync + async instance provision route back to here
		 */
		void acceptInstance(T instance) {
			this.instance = instance;
			awaitLatch.countDown();
		}

		synchronized void add(Query<T> query) {
			awaitingQueries.add(new QueryState(query));
		}

		@Override
		public int hashCode() {
			return definingQuery.hashCode();
		}

		T instance;

		boolean isOneOff() {
			return provider != null && provider.isOneOff();
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

		CountDownLatch awaitLatch = null;

		// SE only
		synchronized void ensureLatch() {
			if (awaitLatch == null) {
				awaitLatch = new CountDownLatch(1);
			}
		}

		T await() {
			Preconditions.checkState(Al.isMultiThreaded());
			ensureLatch();
			try {
				awaitLatch.await();
				return instance;
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	public static class Query<T> implements HasBind {
		ProviderQueries<T> token;

		Class<T> clazz;

		List<InstanceQuery.Parameter<?>> parameters = new ArrayList<>();

		Consumer<T> instanceConsumer;

		boolean bound;

		Consumer<Exception> exceptionConsumer;

		boolean async;

		@Override
		public int hashCode() {
			return clazz.hashCode();
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
				return clazz.equals(o.clazz);
			} else {
				return super.equals(obj);
			}
		}

		Query(Class<T> clazz) {
			this.clazz = clazz;
		}

		public T get() {
			Preconditions.checkState(!async);
			return submit0().await();
		}

		/* Note that this will block if the query is not async */
		ProviderQueries<T> submit0() {
			return InstanceOracle.get().submit(this);
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

		@Override
		public void bind() {
			bound = true;
			InstanceOracle.get().submit(this);
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

		public Query<T>
				withExceptionConsumer(Consumer<Exception> exceptionConsumer) {
			this.exceptionConsumer = exceptionConsumer;
			return this;
		}

		public void reemit() {
			// FIXME - instanceoracle
			// // TODO Auto-generated method stub
			// throw new UnsupportedOperationException(
			// "Unimplemented method 'reemit'");
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
	}
}

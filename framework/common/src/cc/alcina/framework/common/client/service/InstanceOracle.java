package cc.alcina.framework.common.client.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Al;

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
- Look up the store - attach to the existing QueryToken entry if it exists
- Add to 
- If the existing entry has a result, fire that result

 * @formatter:on
 */
@Registration.Singleton
public class InstanceOracle {
	static InstanceOracle get() {
		return Registry.impl(InstanceOracle.class);
	}

	public static <T> Query<T> query(Class<T> clazz) {
		return new Query<>(clazz);
	}

	<T> QueryToken<T> submit(Query<T> query) {
		QueryToken<T> active = store.getToken(query);
		active.ensureSubmitted();
		checkEviction();
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
		Map<QueryToken, QueryToken> uniqueTokens = new LinkedHashMap<>();

		synchronized <T> QueryToken<T> getToken(Query<T> query) {
			QueryToken<T> test = new QueryToken<>(query);
			/*
			 * NOTE - if the InstanceProvider call is already in progress, and
			 * InstanceProvider is a one-off, the QueryToken equals() test will
			 * fail, so a new token will be provided
			 */
			QueryToken<T> active = uniqueTokens.computeIfAbsent(test,
					k -> test);
			active.add(query);
			/*
			 * ordering is critical here, submit after registering receiver
			 */
			active.ensureSubmitted();
			return active;
		}
	}

	static class QueryToken<T> {
		Query<T> query;

		List<Query<T>> awaitingQueries = new ArrayList<>();

		QueryToken(Query<T> query) {
			this.query = query;
		}

		InstanceProvider<T> provider;

		/*
		 * not synchronized here, effectively synchronized in Store#getToken.
		 * This will be called when the first QueryToken for the Query is
		 * created
		 */
		void ensureSubmitted() {
			if (provider == null) {
				provider = Registry.query(InstanceProvider.class)
						.addKeys(query.clazz).impl();
				if (provider.isAsync()) {
					/*
					 * tmp, no use case for non-one-off at this time
					 * 
					 * to implement for multiple async queries with the same
					 * provider, multicast the response callback
					 */
					provider.provideAsync(query);
				} else {
					instance = provider.provide(query);
					ensureLatch();
					awaitLatch.countDown();
				}
			}
		}

		synchronized void add(Query<T> query) {
			awaitingQueries.add(query);
		}

		@Override
		public int hashCode() {
			return query.hashCode();
		}

		T instance;

		boolean isOneOff() {
			return provider != null && provider.isOneOff();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof QueryToken) {
				QueryToken o = (QueryToken) obj;
				if (isOneOff() || o.isOneOff()) {
					return obj == this;
				} else {
					return query.equals(o.query);
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

	// TODO - add argtypes, argvalues
	public static class Query<T> {
		Class<T> clazz;

		AsyncCallback<T> callback;

		@Override
		public int hashCode() {
			return clazz.hashCode();
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

		public void async(AsyncCallback<T> callback) {
			this.callback = callback;
			InstanceOracle.get().submit(this);
		}

		public T get() {
			return InstanceOracle.get().submit(this).await();
		}

		/**
		 * Just semantics - wait until the object is gettable, but don't return
		 * (indicates that the requestor is awaiting a state, not requiring the
		 * object itself)
		 */
		public void await() {
			get();
		}
	}
}

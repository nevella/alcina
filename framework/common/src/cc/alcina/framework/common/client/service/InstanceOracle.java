package cc.alcina.framework.common.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * <p>
 * A more general approach than Registry/GWT.impl - this class provides
 * instances based on a contract, with potential eviction management
 * <p>
 * It's intended as the backing for general object requestors - such as UIs
 * (Sequence, Traversal) as well as maybe more general contract/abstractions
 */
@Registration.Singleton
public class InstanceOracle {
	static InstanceOracle get() {
		return Registry.impl(InstanceOracle.class);
	}

	public static <T> Query<T> query(Class<T> clazz) {
		return new Query<>(clazz);
	}

	<T> Awaiter<T> submit(Query<T> query) {
		return new Awaiter<>(query);
	}

	class Awaiter<T> {
		Query<T> query;

		Awaiter(Query<T> query) {
			this.query = query;
		}

		T await() {
			return null;
		}
	}

	public static class Query<T> {
		Class<T> clazz;

		AsyncCallback<T> callback;

		Query(Class<T> clazz) {
			this.clazz = clazz;
		}

		public Query<T> withCallback(AsyncCallback<T> callback) {
			this.callback = callback;
			return this;
		}

		public T get() {
			return InstanceOracle.get().submit(this).await();
		}
	}
}

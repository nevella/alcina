package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.domain.BaseProjection;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHash;
import cc.alcina.framework.common.client.util.HasEquivalenceHashMap;

/**
 * at some point in the transaction, the object's hash must have equalled the
 * check hash
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public abstract class BaseProjectionHasEquivalenceHash<T extends Entity>
		extends BaseProjection<T> {
	private ThreadLocal<HasEquivalenceHashMap> perThreadEquivalenceMap = new ThreadLocal<HasEquivalenceHashMap>() {
		@Override
		protected synchronized HasEquivalenceHashMap initialValue() {
			return new HasEquivalenceHashMap();
		}
	};

	public BaseProjectionHasEquivalenceHash(Class initialType,
			Class... secondaryTypes) {
		super(initialType, secondaryTypes);
	}

	public T matchesTransactional(Collection<T> perClassTransactional,
			Object[] path) {
		Collection<T> coll = perClassTransactional;
		if (DomainStore.stores().writableStore().transactions()
				.transactionActiveInCurrentThread()) {
			List list = perThreadEquivalenceMap.get()
					.get(equivalenceHashFromPath(path));
			if (list == null) {
				return null;
			}
			coll = list;
		}
		for (T t : coll) {
			if (matches(t, path)) {
				return (T) t;// will always be transactional object
			}
		}
		return null;
	}

	public void onTransactionEnd() {
		perThreadEquivalenceMap.get().clear();
	}

	public void projectHash(Entity obj) {
		HasEquivalenceHash heh = (HasEquivalenceHash) obj;
		perThreadEquivalenceMap.get().add(heh);
	}

	protected abstract int equivalenceHashFromPath(Object[] path);
}

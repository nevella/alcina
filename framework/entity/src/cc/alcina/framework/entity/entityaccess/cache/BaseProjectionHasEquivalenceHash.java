package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.cache.BaseProjection;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
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
public abstract class BaseProjectionHasEquivalenceHash<T extends HasIdAndLocalId>
		extends BaseProjection<T> {
	private ThreadLocal<HasEquivalenceHashMap> perThreadEquivalenceMap = new ThreadLocal<HasEquivalenceHashMap>() {
		protected synchronized HasEquivalenceHashMap initialValue() {
			return new HasEquivalenceHashMap();
		}
	};

	public T matchesTransactional(Collection<T> perClassTransactional,
			Object[] path) {
		Collection<T> coll = perClassTransactional;
		if (AlcinaMemCache.get().transactional
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

	public void projectHash(HasIdAndLocalId obj) {
		HasEquivalenceHash heh = (HasEquivalenceHash) obj;
		perThreadEquivalenceMap.get().add(heh);
	}

	protected abstract int equivalenceHashFromPath(Object[] path);
}

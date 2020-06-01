package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MostlySingleValuedSet;

/*
 *Does *not* implement transactional collection (the degenerate case's map does, but looks like ~95% time that isn't reached)...
 * 
 * 
 */
public class TransactionalSet<E extends Entity> extends MostlySingleValuedSet<E>
		implements MvccObject<TransactionalSet> {
	private Class<E> entityClass;

	MvccObjectVersions<TransactionalSet> __mvccObjectVersions__;

	public TransactionalSet(Class<E> entityClass) {
		this.entityClass = entityClass;
	}

	@Override
	public MvccObjectVersions<TransactionalSet> __getMvccVersions__() {
		return __mvccObjectVersions__;
	}

	@Override
	public void __setMvccVersions__(
			MvccObjectVersions<TransactionalSet> __mvccVersions__) {
		this.__mvccObjectVersions__ = __mvccVersions__;
	}

	@Override
	public boolean add(E o) {
		if (__mvccObjectVersions__ == null) {
			return super.add(o);
		}
		TransactionalSet<E> __instance__ = Transactions
				.resolveTransactionalSet(this, true);
		if (__instance__ == this) {
			return super.add(o);
		} else {
			return __instance__.add(o);
		}
	}

	@Override
	public Object clone() {
		if (__mvccObjectVersions__ == null) {
			return super.clone();
		}
		TransactionalSet<E> __instance__ = Transactions
				.resolveTransactionalSet(this, false);
		if (__instance__ == this) {
			return super.clone();
		} else {
			return __instance__.clone();
		}
	}

	@Override
	public boolean contains(Object o) {
		if (__mvccObjectVersions__ == null) {
			return super.contains(o);
		}
		TransactionalSet<E> __instance__ = Transactions
				.resolveTransactionalSet(this, true);
		if (__instance__ == this) {
			return super.contains(o);
		} else {
			return __instance__.contains(o);
		}
	}

	@Override
	public Iterator<E> iterator() {
		if (__mvccObjectVersions__ == null) {
			return super.iterator();
		}
		TransactionalSet<E> __instance__ = Transactions
				.resolveTransactionalSet(this, false);
		if (__instance__ == this) {
			return super.iterator();
		} else {
			return __instance__.iterator();
		}
	}

	public Class<E> provideEntityClass() {
		return entityClass;
	}

	@Override
	public boolean remove(Object o) {
		if (__mvccObjectVersions__ == null) {
			return super.remove(o);
		}
		TransactionalSet<E> __instance__ = Transactions
				.resolveTransactionalSet(this, true);
		if (__instance__ == this) {
			return super.remove(o);
		} else {
			return __instance__.remove(o);
		}
	}

	@Override
	public int size() {
		if (__mvccObjectVersions__ == null) {
			return super.size();
		}
		TransactionalSet<E> __instance__ = Transactions
				.resolveTransactionalSet(this, false);
		if (__instance__ == this) {
			return super.size();
		} else {
			return __instance__.size();
		}
	}
}

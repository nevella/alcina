package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MostlySingleElementSet;

/*
 * 
 * 
 */
public class TransactionalSet<E extends Entity> extends MostlySingleElementSet<E>
		implements TransactionalCollection {
	private Class<E> entityClass;

	private Transaction baseTransaction;

	public TransactionalSet(Class<E> entityClass) {
		this.entityClass = entityClass;
	}

	// for copying
	TransactionalSet() {
	}

	@Override
	protected Map<E, Boolean> createDegenerateMap(E soleValue,
			boolean nonEmpty) {
		TransactionalMap<E, Boolean> map = new TransactionalMap<>(entityClass,
				Boolean.class);
		if (nonEmpty) {
			if (baseTransaction != null) {
				map.putInBaseLayer(baseTransaction, soleValue, Boolean.TRUE);
			} else {
				map.put(soleValue, Boolean.TRUE);
			}
		}
		return map;
	}

	@Override
	protected boolean mustDegenerate() {
		Transaction current = Transaction.current();
		if (current.isBaseTransaction()) {
			this.baseTransaction = current;
			return false;
		} else {
			return true;
		}
	}
}

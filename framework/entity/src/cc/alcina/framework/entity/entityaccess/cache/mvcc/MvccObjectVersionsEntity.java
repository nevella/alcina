package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;

public class MvccObjectVersionsEntity<T extends Entity>
		extends MvccObjectVersions<T> {
	public MvccObjectVersionsEntity(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	@Override
	protected boolean accessibleFromOtherTransactions(T t) {
		return t.domain().wasPersisted();
	}

	@Override
	protected void copyObjectFields(T fromObject, T toObject) {
		Transactions.copyObjectFields(fromObject, toObject);
	}

	@Override
	protected Class<T> entityClass() {
		return getBaseObject().entityClass();
	}

	@Override
	protected void register(T object) {
		Domain.register(object);
	}
}

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
	protected void copyObjectFields(T fromObject, T toObject) {
		Transactions.copyObjectFields(fromObject, toObject);
	}

	@Override
	protected Class<T> provideEntityClass() {
		return getBaseObject().provideEntityClass();
	}

	@Override
	protected void register(T object) {
		Domain.register(object);
	}

	@Override
	protected boolean wasPersisted(T t) {
		return t.provideWasPersisted();
	}
}

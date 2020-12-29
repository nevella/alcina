package cc.alcina.framework.entity.persistence.mvcc;

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
	protected void copyObject(T fromObject, T baseObject) {
		Transactions.copyObjectFields(fromObject, baseObject);
	}

	@Override
	protected Class<T> entityClass() {
		return getBaseObject().entityClass();
	}

	@Override
	protected void onVersionCreation(T object) {
		Domain.register(object);
	}

	@Override
	protected boolean thisMayBeVisibleToPriorTransactions() {
		return false;
	}
}

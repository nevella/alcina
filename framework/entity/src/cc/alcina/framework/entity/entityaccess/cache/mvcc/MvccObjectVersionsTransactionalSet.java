package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class MvccObjectVersionsTransactionalSet
		extends MvccObjectVersions<TransactionalSet> {
	MvccObjectVersionsTransactionalSet(TransactionalSet t,
			Transaction initialTransaction, boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	@Override
	protected boolean accessibleFromOtherTransactions(TransactionalSet t) {
		// this is impossible to determine, so return 'true'
		return true;
	}

	@Override
	protected void copyObjectFields(TransactionalSet fromObject,
			TransactionalSet toObject) {
		Transactions.copyObjectFields(fromObject, toObject);
	}

	@Override
	protected Class<? extends Entity> entityClass() {
		return getBaseObject().entityClass();
	}

	@Override
	protected void register(TransactionalSet object) {
		// noop
	}
}

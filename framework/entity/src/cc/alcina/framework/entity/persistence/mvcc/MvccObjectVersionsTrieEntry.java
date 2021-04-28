package cc.alcina.framework.entity.persistence.mvcc;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class MvccObjectVersionsTrieEntry
		extends MvccObjectVersions<TransactionalTrieEntry> {
	MvccObjectVersionsTrieEntry(TransactionalTrieEntry t,
			Transaction initialTransaction, boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	@Override
	protected void copyObject(TransactionalTrieEntry fromObject,
			TransactionalTrieEntry baseObject) {
		Transactions.copyObjectFields(fromObject, baseObject);
	}

	@Override
	protected <E extends Entity> Class<E> entityClass() {
		return getBaseObject().entityClass();
	}

	@Override
	protected boolean thisMayBeVisibleToPriorTransactions() {
		return false;
	}
}

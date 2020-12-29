package cc.alcina.framework.entity.persistence.mvcc;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class MvccObjectVersionsTrieEntry
		extends MvccObjectVersions<TransactionalTrieEntry> {
	MvccObjectVersionsTrieEntry(TransactionalTrieEntry t,
			Transaction initialTransaction, boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	@Override
	protected boolean
			accessibleFromOtherTransactions(TransactionalTrieEntry t) {
		// this is impossible to determine, so return 'true'
		return true;
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
	protected void onVersionCreation(TransactionalTrieEntry object) {
		// noop
	}

	@Override
	protected boolean thisMayBeVisibleToPriorTransactions() {
		return false;
	}
}

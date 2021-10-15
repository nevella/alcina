package cc.alcina.framework.entity.persistence.mvcc;

import cc.alcina.framework.entity.persistence.mvcc.MvccObjectVersions.MvccObjectVersionsMvccObject;

public class MvccObjectVersionsTrieEntry
		extends MvccObjectVersionsMvccObject<TransactionalTrieEntry> {
	MvccObjectVersionsTrieEntry(TransactionalTrieEntry t,
			Transaction initialTransaction, boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	@Override
	protected void copyObject(TransactionalTrieEntry fromObject,
			TransactionalTrieEntry baseObject) {
		Transactions.copyObjectFields(fromObject, baseObject);
	}
}

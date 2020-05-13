package cc.alcina.framework.entity.entityaccess.cache.mvcc;

public class MvccObjectVersionsTrieEntry
		extends MvccObjectVersions<TransactionalTrieEntry> {
	MvccObjectVersionsTrieEntry(TransactionalTrieEntry t,
			Transaction initialTransaction, boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	@Override
	protected void copyObjectFields(TransactionalTrieEntry fromObject,
			TransactionalTrieEntry toObject) {
		Transactions.copyObjectFields(fromObject, toObject);
	}

	@Override
	protected Class<TransactionalTrieEntry> provideEntityClass() {
		return getBaseObject().provideEntityClass();
	}

	@Override
	protected void register(TransactionalTrieEntry object) {
		// noop
	}

	@Override
	protected boolean wasPersisted(TransactionalTrieEntry t) {
		// never hurts to copy existing values (even if they're actually new)
		// TODO - could revisit
		return true;
	}
}

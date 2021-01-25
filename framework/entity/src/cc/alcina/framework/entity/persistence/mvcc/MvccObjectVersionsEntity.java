package cc.alcina.framework.entity.persistence.mvcc;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;

public class MvccObjectVersionsEntity<T extends Entity>
		extends MvccObjectVersions<T> {
	private int hash;

	public MvccObjectVersionsEntity(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	@Override
	/*
	 * FIXME - jdk9 - check performance and maybe remove
	 */
	public int hashCode() {
		if (hash == 0) {
			if (getBaseObject().getId() == 0
					&& getBaseObject().getLocalId() == 0) {
				hash = super.hashCode();
			} else {
				hash = getBaseObject().hashCode();
			}
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
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

package cc.alcina.framework.entity.persistence.mvcc;

import cc.alcina.framework.entity.persistence.cache.DomainStore;

/**
 *
 * committingSequenceId is key for ordering in-flight committed txs
 * 
 * @author nick@alcina.cc
 *
 */
class StoreTransaction {
	DomainStore store;

	public long committingSequenceId;

	public StoreTransaction(DomainStore store) {
		this.store = store;
	}

	public Mvcc getMvcc() {
		return store.getMvcc();
	}
}

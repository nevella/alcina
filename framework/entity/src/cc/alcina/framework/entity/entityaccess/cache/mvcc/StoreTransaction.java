package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

class StoreTransaction {
    private DomainStore store;

    public StoreTransaction(DomainStore store) {
        this.store = store;
    }
}

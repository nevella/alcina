package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

/**
 * 
 * FIXME - not sure why the various db ids are here. Probably incorrect, early
 * thinking about transaction writing...see package-info
 * 
 * @author nick@alcina.cc
 *
 */
class StoreTransaction {
    long committedDbDomainTransformRequestId;

    /*
     * This *must* be in sync with the db transaction when performing initial
     * load
     */
    long committedKafkaDomainTransformRequestIdLogId;

    DomainStore store;

    public StoreTransaction(DomainStore store) {
        this.store = store;
    }

    public Mvcc getMvcc() {
        return store.getMvcc();
    }
}

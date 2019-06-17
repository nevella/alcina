package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class MvccTransaction {
    private static ThreadLocal<MvccTransaction> threadLocalInstance = new ThreadLocal() {
    };

    public static void start() {
        start(DomainStore.stores().writableStore());
    }

    public static void start(DomainStore store) {
        threadLocalInstance.set(new MvccTransaction(store));
    }

    private DomainStore store;

    public MvccTransaction(DomainStore store) {
        this.store = store;
    }
}

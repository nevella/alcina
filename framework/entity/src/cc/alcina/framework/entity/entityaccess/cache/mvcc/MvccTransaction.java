package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class MvccTransaction {
    private static ThreadLocal<MvccTransaction> threadLocalInstance = new ThreadLocal() {
    };

    public static void end() {
        threadLocalInstance.remove();
    }

    public static void start() {
        threadLocalInstance.set(new MvccTransaction());
    }

    private Map<DomainStore, StoreTransaction> storeTransactions = new LinkedHashMap<>();

    public MvccTransaction() {
        DomainStore.stores().stream().forEach(store -> storeTransactions
                .put(store, new StoreTransaction(store)));
    }
}

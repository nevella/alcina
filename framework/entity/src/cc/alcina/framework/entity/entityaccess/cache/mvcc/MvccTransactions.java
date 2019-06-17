package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MvccTransactions {
    public ConcurrentLinkedQueue<MvccTransaction> activeTransactions;

    public volatile long initialTxId;
}

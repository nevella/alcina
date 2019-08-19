package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class Transactions {
    private static Transactions instance;

    private static Map<DomainStore, Transaction> baseTransactions = new LightMap<>();

    public static <T extends HasIdAndLocalId> boolean checkResolved(T t) {
        return resolve(t, false) == t;
    }

    public static <T extends HasIdAndLocalId> T copyObject(T object) {
        // FIXME - write some byteassist classes to do direct copying
        return ResourceUtilities.fieldwiseClone(object, false, true);
    }

    public static synchronized void ensureInitialised() {
        if (instance == null) {
            instance = new Transactions();
        }
    }

    public static synchronized boolean isInitialised() {
        return instance != null;
    }

    public static <T extends HasIdAndLocalId> T resolve(T t, boolean write) {
        if (t instanceof MvccObject) {
            MvccObject mvccObject = (MvccObject) t;
            MvccObjectVersions<T> versions = mvccObject.__getMvccVersions__();
            if (versions == null && !write) {
                // no transactional versions, return base
                return t;
            } else {
                Transaction transaction = Transaction.current();
                if (transaction.isBaseTransaction()) {
                    return t;
                } else {
                    if (versions == null) {
                        versions = MvccObjectVersions.ensure(t, transaction);
                    }
                    return versions.resolve(write);
                }
            }
        } else {
            return t;
        }
    }

    static Transaction baseTransaction(DomainStore store) {
        return baseTransactions.get(store);
    }

    static Transactions get() {
        return instance;
    }

    static synchronized void registerBaseTransaction(DomainStore store,
            Transaction transaction) {
        baseTransactions.put(store, transaction);
    }

    private AtomicLong transactionIdCounter = new AtomicLong();

    // these will be in commit order
    private Map<TransactionId, Transaction> committedTransactions = new LinkedHashMap<>();

    // these will be in start order
    private Map<TransactionId, Transaction> activeTransactions = new LinkedHashMap<>();

    private Object transactionMetadataLock = new Object();

    public void onTransactionCommited(Transaction transaction) {
        synchronized (transactionMetadataLock) {
            committedTransactions.put(transaction.getId(), transaction);
        }
    }

    public void onTransactionEnded(Transaction transaction) {
        synchronized (transactionMetadataLock) {
            activeTransactions.remove(transaction.getId());
        }
    }

    void initialiseTransaction(Transaction transaction) {
        synchronized (transactionMetadataLock) {
            TransactionId transactionId = new TransactionId(
                    this.transactionIdCounter.getAndIncrement());
            transaction.setId(transactionId);
            transaction.phase = TransactionPhase.PREPARING;
            transaction.setCommittedTransactions(
                    new LinkedHashMap<>(committedTransactions));
            activeTransactions.put(transactionId, transaction);
        }
    }
}

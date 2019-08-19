package cc.alcina.framework.entity.entityaccess.cache.mvcc;

enum TransactionPhase {
    PREPARING, COMMITING, COMMITTED, ABORTED;
}

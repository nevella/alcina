package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Objects;

import cc.alcina.framework.common.client.util.CommonUtils;

public class MvccTransactionId {
    public String domainStoreId;

    public long committedDbDomainTransformRequestId;

    /*
     * This *must* be in sync with the db transaction when performing initial
     * load
     */
    public long committedKafkaDomainTransformRequestIdLogId;

    // add - internal txId. If no writes, =
    // committedKafkaDomainTransformRequestIdLogId
    public long domainStoreTxId;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MvccTransactionId) {
            MvccTransactionId o = (MvccTransactionId) obj;
            return CommonUtils.equals(domainStoreId, o.domainStoreId,
                    committedDbDomainTransformRequestId,
                    o.committedDbDomainTransformRequestId,
                    committedKafkaDomainTransformRequestIdLogId,
                    o.committedKafkaDomainTransformRequestIdLogId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainStoreId, committedDbDomainTransformRequestId,
                committedKafkaDomainTransformRequestIdLogId);
    }
}

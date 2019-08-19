package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface MvccObject<T extends HasIdAndLocalId> {
    MvccObjectVersions<T> __getMvccVersions__();

    void __setMvccVersions__(MvccObjectVersions<T> mvccVersions__);
}

package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreDescriptor;

public class Mvcc {
    public static DomainStore getStore(HasIdAndLocalId hili) {
        return DomainStore.stores().storeFor(hili.provideEntityClass());
    }

    private DomainStore domainStore;

    DomainStoreDescriptor domainDescriptor;

    private DetachedEntityCache cache;

    private ClassTransformer classTransformer;

    public Mvcc(DomainStore domainStore, DomainStoreDescriptor domainDescriptor,
            DetachedEntityCache cache) {
        Transactions.ensureInitialised();
        this.domainStore = domainStore;
        this.domainDescriptor = domainDescriptor;
        this.cache = cache;
        this.classTransformer = new ClassTransformer(this);
        this.classTransformer.setAddObjectResolutionChecks(
                domainDescriptor.isAddMvccObjectResolutionChecks());
    }

    public <T extends HasIdAndLocalId> T create(Class<T> clazz) {
        return classTransformer.create(clazz);
    }

    public void init() {
        classTransformer.generateTransformedClasses();
    }
}

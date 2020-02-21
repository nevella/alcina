package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreDescriptor;

public class Mvcc {
	public static <T extends HasIdAndLocalId> T
			getEntity(EntityManager entityManager, T t) {
		if (t instanceof MvccObject) {
			return (T) entityManager.find(t.provideEntityClass(), t.getId());
		} else {
			return t;
		}
	}

	public static DomainStore getStore(HasIdAndLocalId hili) {
		return DomainStore.stores().storeFor(hili.provideEntityClass());
	}

	public static boolean isMvccObject(HasIdAndLocalId hili) {
		return hili instanceof MvccObject;
	}

	public static Class<? extends HasId>
			resolveEntityClass(Class<? extends HasId> clazz) {
		if (MvccObject.class.isAssignableFrom(clazz)) {
			clazz = (Class<? extends HasId>) clazz.getSuperclass();
		}
		return clazz;
	}

	@SuppressWarnings("unused")
	private DomainStore domainStore;

	DomainStoreDescriptor domainDescriptor;

	@SuppressWarnings("unused")
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

package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreDescriptor;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccCorrectnessIssue.MvccCorrectnessIssueType;

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

	private Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

	public Mvcc(DomainStore domainStore, DomainStoreDescriptor domainDescriptor,
			DetachedEntityCache cache) {
		Transactions.ensureInitialised();
		this.domainStore = domainStore;
		this.domainDescriptor = domainDescriptor;
		this.cache = cache;
		this.classTransformer = new ClassTransformer(this);
	}

	public <T extends HasIdAndLocalId> T create(Class<T> clazz) {
		return classTransformer.create(clazz);
	}

	public void init() {
		classTransformer.generateTransformedClasses();
	}

	public void testTransformer(Class<? extends HasIdAndLocalId> clazz) {
		// valid if we get an 'exception' result for each correctness type -
		// i.e. the tests are working
		logger.info("testTransformer :: {}", clazz.getSimpleName());
		for (MvccCorrectnessIssueType type : MvccCorrectnessIssueType
				.values()) {
			if (type.isUnknown()) {
				continue;
			}
			String result = classTransformer.testClassTransform(clazz, type);
			Preconditions.checkState(Ax.notBlank(result), Ax.format(
					"test did not pass (blank result) (type: %s)", type));
			logger.info("{} :: {}", type, result);
		}
		logger.info("(All tests passed)");
	}
}

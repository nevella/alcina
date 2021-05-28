package cc.alcina.framework.entity.persistence.mvcc;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.DomainStoreDescriptor;
import cc.alcina.framework.entity.persistence.mvcc.MvccCorrectnessIssue.MvccCorrectnessIssueType;

public class Mvcc {
	private static Logger logger = LoggerFactory.getLogger(Mvcc.class);

	public static void debugSourceNotFound(DomainTransformException e) {
		logger.warn("in vacuum?");
	}

	public static <T extends Entity> T getEntity(EntityManager entityManager,
			T t) {
		if (t instanceof MvccObject) {
			return (T) entityManager.find(t.entityClass(), t.getId());
		} else {
			return t;
		}
	}

	public static DomainStore getStore(Entity entity) {
		return DomainStore.stores().storeFor(entity.entityClass());
	}

	public static boolean isMvccObject(Entity entity) {
		return entity instanceof MvccObject;
	}

	public static boolean isVisible(Entity e) {
		MvccObjectVersions versions = ((MvccObject) e).__getMvccVersions__();
		return versions == null || versions.hasVisibleVersion();
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
	}

	public <T extends Entity> T create(Class<T> clazz) {
		return classTransformer.create(clazz);
	}

	public void init() {
		classTransformer.generateTransformedClasses();
	}

	public Class<? extends HasId>
			resolveMvccClass(Class<? extends Entity> clazz) {
		if (!MvccObject.class.isAssignableFrom(clazz)) {
			return classTransformer.getTransformedClass(clazz);
		}
		return clazz;
	}

	public void testTransformer(Class<? extends Entity> clazz,
			MvccCorrectnessToken token) {
		// valid if we get an 'exception' result for each correctness type -
		// i.e. the tests are working
		if (classTransformer.getTransformedClass(clazz) != null) {
			Preconditions.checkState(
					classTransformer.testClassTransform(clazz, token));
		} else {
			logger.info("testTransformer :: {}", clazz.getSimpleName());
			for (MvccCorrectnessIssueType type : MvccCorrectnessIssueType
					.values()) {
				if (type.isUnknown()) {
					continue;
				}
				String result = classTransformer.testClassTransform(clazz, type,
						new MvccCorrectnessToken());
				Preconditions.checkState(Ax.notBlank(result), Ax.format(
						"test did not pass (blank result) (type: %s)", type));
				logger.info("{} :: {}", type, result);
			}
			logger.info("(All tests passed)");
		}
	}
}

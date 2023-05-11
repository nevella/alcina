package cc.alcina.framework.entity.persistence.mvcc;

import java.util.List;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder.BplDelegateMapCreator;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.DomainStoreDescriptor;
import cc.alcina.framework.entity.persistence.mvcc.MvccCorrectnessIssue.MvccCorrectnessIssueType;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;

public class Mvcc {
	private static Logger logger = LoggerFactory.getLogger(Mvcc.class);

	public static void debugNotFound(Entity entity, RuntimeException e) {
		logger.warn("mvcc.debugNotFound :: {} :: {}/{}",
				e.getClass().getSimpleName(), entity.getClass().getSimpleName(),
				entity.getId());
		if (isMvccObject(entity)) {
			MvccObjectVersions versions = ((MvccObject) entity)
					.__getMvccVersions__();
			if (versions != null) {
				versions.debugNotResolved();
			}
		}
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

	public static void initialiseTransactionEnvironment() {
		Registry.register().singleton(TransactionEnvironment.class,
				new TransactionEnvironmentMvcc());
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
			DetachedEntityCache cache, Mvcc reuseMvcc) {
		Transactions.ensureInitialised();
		this.domainStore = domainStore;
		this.domainDescriptor = domainDescriptor;
		this.cache = cache;
		this.classTransformer = reuseMvcc != null ? reuseMvcc.classTransformer
				: new ClassTransformer(this);
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

	public static class TransactionEnvironmentMvcc
			implements TransactionEnvironment {
		public TransactionEnvironmentMvcc() {
			Registry.register().add(
					BaseProjectionSupportMvcc.BplDelegateMapCreatorTransactional.class,
					List.of(BplDelegateMapCreator.class),
					Registration.Implementation.INSTANCE,
					Registration.Priority.PREFERRED_LIBRARY);
			Registry.register().add(
					BaseProjectionSupportMvcc.TreeMapRevCreatorImpl.class,
					List.of(CollectionCreators.TreeMapRevCreator.class),
					Registration.Implementation.INSTANCE,
					Registration.Priority.PREFERRED_LIBRARY);
			Registry.register().add(
					BaseProjectionSupportMvcc.TreeMapCreatorImpl.class,
					List.of(CollectionCreators.TreeMapCreator.class),
					Registration.Implementation.INSTANCE,
					Registration.Priority.PREFERRED_LIBRARY);
			Registry.register().add(
					BaseProjectionSupportMvcc.MultiTrieCreatorImpl.class,
					List.of(CollectionCreators.MultiTrieCreator.class),
					Registration.Implementation.INSTANCE,
					Registration.Priority.PREFERRED_LIBRARY);
		}

		@Override
		public void begin() {
			Transaction.begin();
		}

		@Override
		public void commit() {
			Transaction.commit();
		}

		@Override
		public void commitWithBackoff() {
			TransformCommit.commitWithBackoff();
		}

		@Override
		public void end() {
			Transaction.end();
		}

		@Override
		public void endAndBeginNew() {
			Transaction.endAndBeginNew();
		}

		@Override
		public void ensureBegun() {
			Transaction.ensureBegun();
		}

		@Override
		public void ensureEnded() {
			Transaction.ensureEnded();
		}

		@Override
		public TransactionId getCurrentTxId() {
			return Transaction.current().getId();
		}

		@Override
		public boolean isInActiveTransaction() {
			return Transaction.isInActiveTransaction();
		}

		@Override
		public boolean isInNonSingleThreadedProjectionState() {
			return !Transaction.current().isBaseTransaction();
		}

		@Override
		public boolean isInTransaction() {
			return Transaction.isInTransaction();
		}

		@Override
		public boolean isMultiple() {
			return true;
		}

		@Override
		public boolean isToDomainCommitting() {
			return Transaction.current().isToDomainCommitting();
		}

		@Override
		public void waitUntilCurrentRequestsProcessed() {
			DomainStore.waitUntilCurrentRequestsProcessed();
		}

		@Override
		public void withDomainAccess0(Runnable runnable) {
			Preconditions.checkState(isInActiveTransaction());
			runnable.run();
		}

		@Override
		public <T> T withDomainAccess0(Supplier<T> supplier) {
			Preconditions.checkState(isInActiveTransaction());
			return supplier.get();
		}
	}
}

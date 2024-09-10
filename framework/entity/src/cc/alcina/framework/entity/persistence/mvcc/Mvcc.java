package cc.alcina.framework.entity.persistence.mvcc;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.DomainStoreDescriptor;
import cc.alcina.framework.entity.persistence.mvcc.MvccCorrectnessIssue.MvccCorrectnessIssueType;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionCommittedEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionsCreationEvent;

/**
 *
 * <h2>Transactional object</h2>
 * <ul>
 * <li>Every object implements MvccObject
 * <li>Every object has an MvccObjectVersions getter/setter (accessible via the
 * interface).
 * </ul>
 *
 *
 * <h2>MvccObject identity and debugging gotchas</h2>
 * <ul>
 * <li>An mvccobject (transactional entity) has essentially two roles: a store
 * of field values, and a 'domain identity'
 * <li>The 'domain identity' object is always the first instance of a given
 * entity (class/id or class/localid tuple) visible to the domain, and remains
 * the same object for the lifetime of the domain (i.e webapp or jvm). Even when
 * an entity is persisted to the db and assigned an id value, this relationship
 * holds (so there's no need to call entity.domain().domainVersion() after
 * persisting).
 * <li>If an entity has not been changed in any unvacuumed transaction,
 * MvccObjectVersions will be null and the property values returned from
 * getters/setters will be the field values of the 'domain identity' object.
 * <li>If an entity _has_ been changed, all visible methods will route to the
 * appropriate versioned instance ( which acts as a container of field values
 * for the transaction). There are some wrinkles to this logic (@see
 * MvccAccessType) but not for getters/setters.
 * <li>TLDR; For debugging, to view the field values of an object you're writing
 * to (if it has a non-zero id value), look at:
 * entity.__mvccObjectVersions__.__mostRecentWritable
 * </ul>
 *
 *
 * <h2>Transaction application (db tx -> domain store) (Nick's thoughts)</h2>
 * <ul>
 * <li>Transactions application is sequential, in db order
 * <li>Strategies to reduce application times...?
 * <li>...maybe have a speculative apply which can be rewound if collisions
 * occur. Only issue is indexing but most indicies - if not totally derived from
 * non-domain object fields - are derived from unchanging domain references
 * <li>Aha! When publishing (kafka) "persisting tx id", publish row-level
 * 'locks' (class.id tuples modified). If no conflicts, tx can be applied
 * out-of-order (to the object)
 * <li>application-level question about whether, then, we should wait for all
 * txs with prior db commit times
 * <li>Anyway, that's for the future - for the moment stick with sequential
 * commit and reduce time there where possible
 * <li>And then of course there's ... eventual consistency
 * </ul>
 * 
 * <h2>Process debugging and mvcc object lifecycle</h2>
 * <p>
 * Significant events in the mvcc object lifecycle are emitted as
 * {@link ProcessObservable} instances and can be viewed by the corresponding
 * servlet layer system (see package
 * cc.alcina.framework.servlet.process.observer.mvcc).
 * <p>
 * These events and their sequence also provide a guide to the lifecycle of an
 * mvcc entity - summarised here for a notional JobImpl object (with notional tx
 * ids):
 * <table>
 * <tr>
 * <th>Tx/Phase</th>
 * <th>Event/Observable</th>
 * <th>Code</th> *
 * <th>Notes</th>
 * </tr>
 * <tr>
 * <td>txid:1 - TO_DB_PREPARING</td>
 * <td>{@link VersionsCreationEvent}, {@link VersionCreationEvent}</td>
 * <td><code>Job job = new TaskListJobs().schedule(); ... </code></td> *
 * <td>A JobImpl entity is created with a local id, and its _mvccVersions__
 * field is initialised with a writeable version. All calls in this tx
 * (including the e.g. setTaskSerialized in schedule()) route to this
 * version</td>
 * </tr>
 * <tr>
 * <td>txid:1 - TO_DB_PERSISTING...TO_DB_PERSISTED</td>
 * <td>{@link VersionPersistedEvent}</td>
 * <td><code>The transaction is committed to the db -
 * TO_DB_PERSISTING, TO_DB_PERSISTED </code></td> *
 * <td>The VersionPersistedEvent contains the final field values (and the object
 * version will be the domainIdentity. Its fields will be reset to defaults
 * during the TO_DOMAIN_COMMITTED tx</td>
 * </tr>
 * <tr>
 * <td>txid:2 - TO_DOMAIN_PREPARING...TO_DOMAIN_COMMITTED</td>
 * <td>{@link VersionCreationEvent}, {@link VersionCommittedEvent}</td>
 * <td><code>The transaction is committed to the the committed graph - 
  TO_DOMAIN_PREPARING, TO_DOMAIN_COMMITTING, TO_DOMAIN_COMMITTED </code></td> *
 * <td>A new {@link ObjectVersion} is created during the TO_DOMAIN_COMMITTING
 * phase. It is then modified by application of the transforms, and the
 * domainIdentity fields are reset (since it will no longer be visible to any
 * tx)</td>
 * </tr>
 * </table>
 *
 * <h2>Further notes</h2>
 * <ul>
 * <li>Method access package-private is disallowed since the 'call super if
 * resolved' rewrite strategy can't be applied, and version correctness can't be
 * guaranteed
 * <li>Covariant methods are not allowed since it appears the javassist rewriter
 * doesn't rewrite both method versions
 * </ul>
 */
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
}

package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Vacuum.Vacuumable;

/**
 * Note that like a TransactionalMap, the owning MvccObject will not be
 * reachable by any transaction started before it is committed to the domain
 * graph (unless the object is created within that transaction).
 * 
 * synchronization - resolution of version is synchronized on the baseobject, as
 * is vacuum.
 * 
 * @author nick@alcina.cc
 * 
 * 
 *
 * @param <T>
 */
public class MvccObjectVersions<T extends HasIdAndLocalId>
		implements Vacuumable {
	static Logger logger = LoggerFactory.getLogger(MvccObjectVersions.class);

	// called in a synchronized block (synchronized on baseObject)
	static <T extends HasIdAndLocalId> MvccObjectVersions<T> ensure(
			T baseObject, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) baseObject;
		MvccObjectVersions<T> versions = null;
		versions = new MvccObjectVersions<T>(baseObject, transaction,
				initialObjectIsWriteable);
		mvccObject.__setMvccVersions__(versions);
		return versions;
	}

	private volatile ConcurrentHashMap<Transaction, ObjectVersion<T>> versions = new ConcurrentHashMap<>();

	// never changes (for a given class/id or class/clientinstance/localid
	// tuple). fields can be updated by vacuum if not accessible via any
	// transaction
	private volatile T baseObject;

	/*
	 * debugging aids
	 */
	@SuppressWarnings("unused")
	private T __mostRecentReffed;

	@SuppressWarnings("unused")
	private T __mostRecentWritable;

	/*
	 * synchronization - either a object t for this domainstore (so no sync
	 * needed), or accessed via a block synced on 't'
	 */
	MvccObjectVersions(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable) {
		/*
		 * If in the 'preparing' phase, and t is non-local, this is a
		 * modification of the initial transaction version.
		 * 
		 * In that case, create a copy (to be modified) from the base object
		 * 
		 * 
		 * TODO - explain logic here and in resolve0
		 */
		baseObject = t;
		if (initialTransaction.phase == TransactionPhase.TO_DB_PREPARING
				&& t.provideWasPersisted()) {
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = initialTransaction;
				version.object = (T) Transactions
						.copyObject((HasIdAndLocalId & MvccObject) t);
				((MvccObject) version.object).__setMvccVersions__(this);
				putVersion(version);
			}
		} else {
			/*
			 * transforms from another vm - or domain commit - or created this
			 * tx
			 */
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = initialTransaction;
				version.object = t;
				version.writeable = initialObjectIsWriteable;
				putVersion(version);
			}
		}
		// TODO - remove? It's a hit
		logger.trace("created object version: {} : {}", toString(), hashCode());
	}

	public T getBaseObject() {
		return this.baseObject;
	}

	@Override
	public String toString() {
		try {
			T object = baseObject;
			Transaction transaction = null;
			synchronized (baseObject) {
				Iterator<ObjectVersion<T>> itr = versions.values().iterator();
				if (itr.hasNext()) {
					ObjectVersion<T> firstVersion = itr.next();
					object = firstVersion.object;
					transaction = firstVersion.transaction;
				}
			}
			/*
			 * use field rather than getters to not resolve
			 */
			Field idField = SEUtilities.getFieldByName(object.getClass(), "id");
			Field localIdField = SEUtilities.getFieldByName(object.getClass(),
					"localId");
			Object id = idField.get(object);
			return Ax.format("versions: %s : base: %s/%s/%s : initial-tx: %s",
					versions.size(), object.getClass(), id,
					System.identityHashCode(object),
					transaction == null ? transaction : "base");
		} catch (Exception e) {
			return "exception..";
		}
	}

	@Override
	public void vacuum(Transaction transaction) {
		synchronized (baseObject) {
			ObjectVersion<T> version = versions.get(transaction);
			/*
			 * Must change the base before removing transaction (otherwise
			 * there's a narrow window where we'd return an older version)
			 */
			if (transaction
					.getPhase() == TransactionPhase.TO_DOMAIN_COMMITTED) {
				// baseObject fields will not be reachable here to app code (all
				// active txs will be
				// looking at version.object fields)
				Transactions.copyObjectFields(version.object, baseObject);
			}
			versions.remove(transaction);
			if (versions.isEmpty()) {
				((MvccObject) baseObject).__setMvccVersions__(null);
			}
		}
	}

	private void putVersion(ObjectVersion<T> version) {
		versions.put(version.transaction, version);
		Transactions.get().onAddedVacuumable(this);
	}

	/*
	 * Try and return a cached version if possible (if cached version is not
	 * 'writeable' and we're writing, we need to replace it for this tx with a
	 * writeable version). Note that all this is not hit unless the domain
	 * object is modified during the jvm lifetime -
	 */
	private T resolve0(boolean write) {
		Transaction transaction = Transaction.current();
		ObjectVersion<T> version = versions.get(transaction);
		if (version != null && version.isCorrectWriteableState(write)) {
			return version.object;
		}
		if (versions.isEmpty()) {
			return baseObject;
		}
		version = versions.get(transaction);
		if (version != null && version.isCorrectWriteableState(write)) {
			return version.object;
		}
		version = new ObjectVersion<>();
		version.transaction = transaction;
		Transaction mostRecentTransaction = transaction
				.mostRecentPriorTransaction(versions.keys(),
						DomainStore.stores().storeFor(
								versions.values().iterator().next().object
										.provideEntityClass()));
		T mostRecentObject = null;
		/*
		 * mostRecent will be null if just created
		 */
		ObjectVersion<T> mostRecentVersion = mostRecentTransaction == null
				? null
				: versions.get(mostRecentTransaction);
		if (mostRecentVersion != null) {
			mostRecentObject = mostRecentVersion.object;
		} else {
			mostRecentObject = baseObject;
		}
		if (write) {
			version.object = (T) Transactions.copyObject(
					(HasIdAndLocalId & MvccObject) mostRecentObject);
			version.writeable = true;
			// put before register (which will call resolve());
			putVersion(version);
			Domain.register(version.object);
			return version.object;
		} else {
			/*
			 * We could cache - by creating an ObjectVerson for this read call -
			 * but that would require a later vacuum
			 */
			return mostRecentObject;
		}
	}

	T resolve(boolean write) {
		T resolved = resolve0(write);
		__mostRecentReffed = resolved;
		if (write) {
			__mostRecentWritable = resolved;
		}
		return resolved;
	}
}

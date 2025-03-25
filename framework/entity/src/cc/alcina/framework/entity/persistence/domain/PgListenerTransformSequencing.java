package cc.alcina.framework.entity.persistence.domain;

/**
 * <p>
 * This class adds support for transform request ordering via published request
 * ids received on a pg_listener channel
 * 
 * <p>
 * This class is not required by the core loader (it's one of several sequencing
 * possibilities), so the loader and store communicate via topic/events rather
 * than direct calls.
 * 
 * @author nick@alcina.cc
 *
 */
public class PgListenerTransformSequencing {
	DomainStore domainStore;

	DomainStoreLoaderDatabase loaderDatabase;

	public PgListenerTransformSequencing(DomainStore domainStore) {
		this.domainStore = domainStore;
		loaderDatabase = (DomainStoreLoaderDatabase) domainStore.loader;
		/*
		 * * prior to warmup, ensure the trigger fires the dtr id
		 * 
		 * prior to warmup, ensure trigger is active
		 * 
		 * during warmup, collect published dtr ids
		 * 
		 * at warmup end, remove any visible, publish the rest
		 */
		/*
		 * during normal operation, a dtrid trigger just signals via
		 * store.getPersistenceEvents().getQueue().onTransformRequestCommitted(
		 * id);
		 */
	}

	/*
	 * Prior to db warmup (transaction/snapshot isolation), ensure the remote
	 * trigger emits the DTR id as a payload
	 */
	void ensureDtrTrigger() {
	}

	/*
	 * Prior to db warmup (transaction/snapshot isolation), ensure the remote
	 * trigger is active. This will collect published dtr ids
	 */
	void ensureDtrTriggerActive() {
	}

	/*
	 * At warmup end, remove any visible, publish the rest
	 */
	void removeSnapshotVisiblePublishedIds() {
	}
}

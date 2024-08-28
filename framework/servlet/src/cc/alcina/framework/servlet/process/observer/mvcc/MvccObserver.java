package cc.alcina.framework.servlet.process.observer.mvcc;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.RevertDomainIdentityEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionCommittedEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionCopiedToDomainIdentityEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionDbPersistedEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionRemovalEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionsCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionsRemovalEvent;

/**
 * <p>
 * This class gathers detailed logs of mutations to selected mvcc entities, via
 * observations of the several {@link ProcessObserver} types
 */
@Registration.Singleton
public class MvccObserver {
	static MvccObserver get() {
		return Registry.impl(MvccObserver.class);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void observe(ObservableEntityFilter filter) {
		get().observe0(filter);
	}

	public static MvccHistory getHistory(EntityLocator locator) {
		return get().getHistory0(locator);
	}

	MvccHistory getHistory0(EntityLocator locator) {
		return histories.get(locator.localId);
	}

	/*
	 * Since EntityLocator comparison is complex around promoted objects, use
	 * the localId only
	 */
	ConcurrentHashMap<Long, MvccHistory> histories = new ConcurrentHashMap<>();

	List<ObservableEntityFilter> filters = new CopyOnWriteArrayList<>();

	List<EvictionRecord> evictionRecords = new CopyOnWriteArrayList<>();

	class EvictionRecord {
		EvictionRecord(long localId) {
			this.localId = localId;
			this.evictAt = System.currentTimeMillis()
					+ TimeConstants.ONE_MINUTE_MS;
		}

		long localId;

		long evictAt;

		boolean evict() {
			if (evictAt <= System.currentTimeMillis()) {
				histories.remove(localId);
				return true;
			} else {
				return false;
			}
		}
	}

	void observe0(ObservableEntityFilter filter) {
		filters.add(filter);
		if (filters.size() == 1) {
			registerObservers();
		}
	}

	class VersionsCreation
			implements ProcessObserver<MvccObservable.VersionsCreationEvent> {
		@Override
		public void topicPublished(VersionsCreationEvent event) {
			if (filters.stream().anyMatch(f -> f.isBeginObservation(event))) {
				if (!isObserving(event)) {
					histories.put(event.event.locator.localId,
							new MvccHistory(event.event.domainIdentity));
				}
			}
			conditionallyRecord(event);
		}
	}

	class VersionsRemoval
			implements ProcessObserver<MvccObservable.VersionsRemovalEvent> {
		@Override
		public void topicPublished(VersionsRemovalEvent event) {
			if (filters.stream().anyMatch(f -> f.isEndObservation(event))) {
				evictionRecords.add(
						new EvictionRecord(event.event.locator.getLocalId()));
			}
			conditionallyRecord(event);
		}
	}

	class VersionRemoval
			implements ProcessObserver<MvccObservable.VersionRemovalEvent> {
		@Override
		public void topicPublished(VersionRemovalEvent event) {
			conditionallyRecord(event);
		}
	}

	class VersionCreation
			implements ProcessObserver<MvccObservable.VersionCreationEvent> {
		@Override
		public void topicPublished(VersionCreationEvent event) {
			conditionallyRecord(event);
		}
	}

	class VersionCommitted
			implements ProcessObserver<MvccObservable.VersionCommittedEvent> {
		@Override
		public void topicPublished(VersionCommittedEvent event) {
			conditionallyRecord(event);
		}
	}

	class VersionDbPersisted
			implements ProcessObserver<MvccObservable.VersionDbPersistedEvent> {
		@Override
		public void topicPublished(VersionDbPersistedEvent event) {
			conditionallyRecord(event);
		}
	}

	class VersionCopiedToDomainIdentity implements
			ProcessObserver<MvccObservable.VersionCopiedToDomainIdentityEvent> {
		@Override
		public void topicPublished(VersionCopiedToDomainIdentityEvent event) {
			conditionallyRecord(event);
		}
	}

	class RevertDomainIdentity implements
			ProcessObserver<MvccObservable.RevertDomainIdentityEvent> {
		@Override
		public void topicPublished(RevertDomainIdentityEvent event) {
			conditionallyRecord(event);
		}
	}

	void registerObservers() {
		new VersionsCreation().bind();
		new VersionsRemoval().bind();
		new VersionCreation().bind();
		new VersionRemoval().bind();
		new VersionCommitted().bind();
		new VersionDbPersisted().bind();
		new VersionCopiedToDomainIdentity().bind();
		new RevertDomainIdentity().bind();
	}

	boolean isObserving(MvccObservable event) {
		long localId = event.event.locator.localId;
		return localId != 0 && histories.containsKey(localId);
	}

	void conditionallyRecord(MvccObservable event) {
		MvccHistory history = histories.get(event.event.locator.localId);
		if (history != null) {
			history.add(event);
		}
		checkEviction();
	}

	void checkEviction() {
		while (evictionRecords.size() > 0) {
			EvictionRecord record = evictionRecords.get(0);
			if (record.evict()) {
				evictionRecords.remove(0);
			} else {
				break;
			}
		}
	}
}

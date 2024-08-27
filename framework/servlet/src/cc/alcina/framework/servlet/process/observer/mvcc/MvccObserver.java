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
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionCommittedEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionDbPersistedEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionRemovalEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionsCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionsRemovalEvent;

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
			implements ProcessObserver<MvccObservables.VersionsCreationEvent> {
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
			implements ProcessObserver<MvccObservables.VersionsRemovalEvent> {
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
			implements ProcessObserver<MvccObservables.VersionRemovalEvent> {
		@Override
		public void topicPublished(VersionRemovalEvent event) {
			conditionallyRecord(event);
		}
	}

	class VersionCreation
			implements ProcessObserver<MvccObservables.VersionCreationEvent> {
		@Override
		public void topicPublished(VersionCreationEvent event) {
			conditionallyRecord(event);
		}
	}

	class VersionCommitted
			implements ProcessObserver<MvccObservables.VersionCommittedEvent> {
		@Override
		public void topicPublished(VersionCommittedEvent event) {
			conditionallyRecord(event);
		}
	}

	class VersionDbPersisted implements
			ProcessObserver<MvccObservables.VersionDbPersistedEvent> {
		@Override
		public void topicPublished(VersionDbPersistedEvent event) {
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
	}

	boolean isObserving(MvccObservables.Observable event) {
		long localId = event.event.locator.localId;
		return localId != 0 && histories.containsKey(localId);
	}

	void conditionallyRecord(MvccObservables.Observable event) {
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
			}
		}
	}
}

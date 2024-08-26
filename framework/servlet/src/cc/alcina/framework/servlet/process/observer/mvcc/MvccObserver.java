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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables;
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
					Ax.out("observing: %s", event.event);
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
				histories.remove(event.event.locator.getLocalId());
			}
			conditionallyRecord(event);
		}
	}

	void registerObservers() {
		new VersionsCreation().bind();
		new VersionsRemoval().bind();
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
	}
}

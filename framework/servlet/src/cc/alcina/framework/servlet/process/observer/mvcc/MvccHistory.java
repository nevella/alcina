package cc.alcina.framework.servlet.process.observer.mvcc;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.persistence.mvcc.MvccEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObject;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable;
import cc.alcina.framework.servlet.component.sequence.Sequence;

/**
 * A detailed history of changes to the observed mvcc object
 */
public class MvccHistory {
	public static final String LOCAL_PATH = "/tmp/sequence/mvcc-event";

	public MvccObject domainIdentity;

	public List<MvccObservable> observables = new CopyOnWriteArrayList<>();

	public MvccHistory(MvccObject domainIdentity) {
		this.domainIdentity = domainIdentity;
	}

	void add(MvccObservable observable) {
		observables.add(observable);
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder();
		format.line("entity: %s", ((Entity) domainIdentity).toStringId());
		format.indent(1);
		observables.forEach(format::line);
		return format.toString();
	}

	void populateVersionIds() {
		Map<Integer, Integer> identityHashCodeEncounterIndex = new LinkedHashMap<>();
		observables.forEach(o -> {
			MvccEvent event = o.event;
			Integer versionId = identityHashCodeEncounterIndex.computeIfAbsent(
					event.versionObjectIdentityHashCode,
					hash -> identityHashCodeEncounterIndex.size());
			event.versionId = versionId;
		});
	}

	public class HistorySequence {
		public void exportLocal() {
			List<MvccEvent> events = getEvents();
			File folder = new File(LOCAL_PATH);
			Sequence.Loader.writeElements(folder, events);
		}

		public List<MvccEvent> getEvents() {
			populateVersionIds();
			List<MvccEvent> events = observables.stream()
					.map(MvccObservable::getEvent).collect(Collectors.toList());
			return events;
		}
	}

	/**
	 * 
	 * @return A Sequence instance, to export the history in a form to be
	 *         rendered by the Alcina sequence viewer
	 */
	public HistorySequence sequence() {
		return new HistorySequence();
	}
}

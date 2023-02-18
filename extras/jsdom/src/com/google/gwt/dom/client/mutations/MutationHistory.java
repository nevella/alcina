package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.DomNode;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.mutations.MutationHistory.Event.Type;
import com.google.gwt.dom.client.mutations.MutationNode.EquivalenceTest;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializerOptions;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.FormatBuilder;

/*
 * @formatter:off
 *
 * - list of MutationEvents
 * - controlled by ldm2.config.observe [init, events, check_dom]
 * - if init, intercept init (and get initial local, remote MutationNode trees)
 * - if events,
 *
 * @formatter:on
 */
@Bean
public class MutationHistory implements ProcessObserver<MutationHistory.Event> {
	// ensure we don't OOM with dom logging, but have playback info for
	// debugging
	private static int clearBack = 3;

	private List<MutationHistory.Event> events = new ArrayList<>();

	private LocalDomMutations mutations;

	SyncMutations currentMutations;

	public MutationHistory() {
	}

	public MutationHistory(LocalDomMutations mutations) {
		this.mutations = mutations;
		if (mutations.configuration.provideIsObserveHistory()) {
			ProcessObservers.observe(this, true);
		}
	}

	public List<MutationHistory.Event> getEvents() {
		return this.events;
	}

	@Override
	public Class<MutationHistory.Event> getObservableClass() {
		return MutationHistory.Event.class;
	}

	public boolean hadExceptions() {
		return events.stream().anyMatch(Event::isEquivalenceTestFailed);
	}

	public String serialize() {
		SerializerOptions options = new ReflectiveSerializer.SerializerOptions()
				.withElideDefaults(true).withTypeInfo(false).withPretty(true);
		return ReflectiveSerializer.serialize(this, options);
	}

	public void setEvents(List<MutationHistory.Event> events) {
		this.events = events;
	}

	@Override
	public void topicPublished(MutationHistory.Event event) {
		if (mutations.configuration.logEvents) {
			LocalDom.log(Level.INFO,
					"mutation event %s - %s - received - %s mutations",
					events.size(), event.type, event.records.size());
		}
		if (mutations.configuration.logDoms && !hadExceptions()) {
			if (events.size() >= clearBack) {
				events.get(events.size() - clearBack).clearDoms();
			}
			testEquivalence(event);
		}
		events.add(event);
	}

	void verifyDomEquivalence() {
		Event event = new Event(Type.TEST, new ArrayList<>());
		Preconditions.checkState(testEquivalence(event));
	}

	boolean testEquivalence(MutationHistory.Event event) {
		long start = System.currentTimeMillis();
		try {
			return testEquivalence0(event);
		} finally {
			LocalDom.log(Level.INFO, "mutations - testEquivalence - %s ms",
					System.currentTimeMillis() - start);
		}
	}

	boolean testEquivalence0(MutationHistory.Event event) {
		Element documentElement = Document.get().getDocumentElement();
		event.localDom = new MutationNode(documentElement, null,
				mutations.mutationsAccess, true, null);
		event.remoteDom = new MutationNode(
				mutations.mutationsAccess.typedRemote(documentElement), null,
				mutations.mutationsAccess, true, null);
		EquivalenceTest equivalenceTest = event.localDom
				.testEquivalence(event.remoteDom);
		event.equivalenceTest = equivalenceTest.toString();
		if (equivalenceTest.firstInequivalent != null) {
			event.equivalenceTestFailed = true;
			FormatBuilder issue = new FormatBuilder().separator("\n");
			issue.append("-----------------------------------");
			issue.append(event.equivalenceTest);
			issue.append("-----------------------------------");
			issue.append("");
			if (currentMutations != null) {
				DomNode triggeringRemote = equivalenceTest.firstInequivalent.right.domNode;
				MutationNode mutationNodeWithRecords = currentMutations.mutationNodes
						.get(triggeringRemote);
				MutationRecord mutationRecord = mutationNodeWithRecords.records
						.get(0);
				int indexOf = event.records.indexOf(mutationRecord);
				// TODO - add to fb
				int debug = 3;
			}
			LocalDom.log(Level.WARNING, issue.toString());
			event.records.forEach(
					record -> LocalDom.log(Level.WARNING, record.toString()));
			Scheduler.get().scheduleDeferred(() -> {
				mutations.setEnabled(false);
				mutations.mutationsAccess
						.reportException(new InequivalentDomException());
			});
			return false;
		} else {
			return true;
		}
	}

	@Bean
	@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
	public static class Event implements ProcessObservable {
		public static void publish(Event.Type type,
				List<MutationRecord> records) {
			ProcessObservers.publish(MutationHistory.Event.class,
					() -> new Event(type, records));
		}

		public boolean equivalenceTestFailed;

		private Type type;

		private List<MutationRecord> records;

		private String equivalenceTest;

		private MutationNode localDom;

		private MutationNode remoteDom;

		public Event() {
		}

		public Event(Event.Type type, List<MutationRecord> records) {
			this.type = type;
			this.records = records;
		}

		public String getEquivalenceTest() {
			return this.equivalenceTest;
		}

		public MutationNode getLocalDom() {
			return this.localDom;
		}

		public List<MutationRecord> getRecords() {
			return this.records;
		}

		public MutationNode getRemoteDom() {
			return this.remoteDom;
		}

		public Type getType() {
			return this.type;
		}

		public boolean isEquivalenceTestFailed() {
			return this.equivalenceTestFailed;
		}

		public void setEquivalenceTest(String equivalenceTest) {
			this.equivalenceTest = equivalenceTest;
		}

		public void setEquivalenceTestFailed(boolean inequivalent) {
			this.equivalenceTestFailed = inequivalent;
		}

		public void setLocalDom(MutationNode localDom) {
			this.localDom = localDom;
		}

		public void setRecords(List<MutationRecord> records) {
			this.records = records;
		}

		public void setRemoteDom(MutationNode remoteDom) {
			this.remoteDom = remoteDom;
		}

		public void setType(Type type) {
			this.type = type;
		}

		void clearDoms() {
			localDom = null;
			remoteDom = null;
			records = null;
		}

		@Reflected
		public enum Type {
			INIT, MUTATIONS, TEST
		}
	}

	public static class InequivalentDomException extends Exception {
	}
}

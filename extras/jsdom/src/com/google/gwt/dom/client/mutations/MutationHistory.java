package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
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
import cc.alcina.framework.common.client.util.Ax;
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
	private List<MutationHistory.Event> events = new ArrayList<>();

	private LocalDomMutations2 mutations;

	public MutationHistory() {
	}

	public MutationHistory(LocalDomMutations2 mutations) {
		this.mutations = mutations;
		if (mutations.configuration.provideIsObserveHistory()) {
			ProcessObservers.observe(this, true);
		}
	}

	public void dump() {
		SerializerOptions options = new ReflectiveSerializer.SerializerOptions()
				.withElideDefaults(true).withTypeInfo(false).withPretty(true);
		String string = ReflectiveSerializer.serialize(this, options);
		Ax.out(string);
	}

	public List<MutationHistory.Event> getEvents() {
		return this.events;
	}

	@Override
	public Class<MutationHistory.Event> getObservableClass() {
		return MutationHistory.Event.class;
	}

	public void setEvents(List<MutationHistory.Event> events) {
		this.events = events;
	}

	@Override
	public void topicPublished(MutationHistory.Event event) {
		if (mutations.configuration.logDoms) {
			Element documentElement = Document.get().getDocumentElement();
			event.localDom = new MutationNode(documentElement, null,
					mutations.mutationsAccess, true, null);
			event.remoteDom = new MutationNode(
					mutations.mutationsAccess.typedRemote(documentElement),
					null, mutations.mutationsAccess, true, null);
			EquivalenceTest equivalenceTest = event.localDom
					.testEquivalence(event.remoteDom);
			event.equivalenceTest = equivalenceTest.toString();
			if (equivalenceTest.firstInequivalent != null) {
				FormatBuilder issue = new FormatBuilder().separator("\n");
				issue.append("-----------------------------------");
				issue.append(event.equivalenceTest);
				issue.append("-----------------------------------");
				issue.append("");
				LocalDom.log(Level.WARNING, issue.toString());
				Scheduler.get().scheduleDeferred(() -> {
					throw new IllegalStateException(event.equivalenceTest);
				});
			} else {
				LocalDom.log(Level.INFO, "mutation event %s - verified correct",
						events.size());
			}
		}
		events.add(event);
	}

	@Bean
	@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
	public static class Event implements ProcessObservable {
		public static void publish(Event.Type type,
				List<MutationRecord> records) {
			ProcessObservers.publish(MutationHistory.Event.class,
					() -> new Event(type, records));
		}

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

		public void setEquivalenceTest(String equivalenceTest) {
			this.equivalenceTest = equivalenceTest;
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

		@Reflected
		public enum Type {
			INIT, MUTATIONS
		}
	}
}

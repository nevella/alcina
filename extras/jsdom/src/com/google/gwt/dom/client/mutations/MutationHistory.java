package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializerOptions;
import cc.alcina.framework.common.client.util.Ax;

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
				.withTypeInfo(false).withPretty(true);
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
					mutations.mutationsAccess, true);
			event.remoteDom = new MutationNode(
					mutations.mutationsAccess.typedRemote(documentElement),
					null, mutations.mutationsAccess, true);
			event.identicalDoms = event.localDom.equivalentTo(event.remoteDom);
		}
		events.add(event);
	}

	@Bean
	public static class Event implements ProcessObservable {
		public static void publish(Event.Type type,
				List<MutationRecord> records) {
			ProcessObservers.publish(MutationHistory.Event.class,
					() -> new Event(type, records));
		}

		private Type type;

		private List<MutationRecord> records;

		private boolean identicalDoms = true;

		private MutationNode localDom;

		private MutationNode remoteDom;

		public Event() {
		}

		public Event(Event.Type type, List<MutationRecord> records) {
			this.type = type;
			this.records = records;
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

		public boolean isIdenticalDoms() {
			return this.identicalDoms;
		}

		public void setIdenticalDoms(boolean identicalDoms) {
			this.identicalDoms = identicalDoms;
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

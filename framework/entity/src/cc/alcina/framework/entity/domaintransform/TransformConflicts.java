package cc.alcina.framework.entity.domaintransform;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.EntityLayerLocator;

/**
 * Can be plugged into TLTM to check for versioning conflicts
 * 
 * @author nick@alcina.cc
 * 
 */
public class TransformConflicts {
	private static final String CHECK_TRANSFORM_CONFLICTS_QUERY = "check transform conflicts query";

	public static final String TOPIC_CONFLICT_EVENT = TransformConflicts.class
			.getName() + "::TOPIC_CONFLICT_EVENT";

	private boolean ignoreConflicts;

	private TransformPersistenceToken transformPersistenceToken;

	public TransformConflicts() {
		ignoreConflicts = ResourceUtilities.getBoolean(
				TransformConflicts.class, "ignoreConflicts");
	}

	/*
	 * do all that's humanly possible to avoid a db query
	 */
	public void checkVersion(HasIdAndLocalId obj, DomainTransformEvent event)
			throws DomainTransformException {
		if (ignoreConflicts) {
			return;
		}
		if (!(obj instanceof HasVersionNumber)) {
			return;
		}
		int persistentVersionNumber = ((HasVersionNumber) obj)
				.getVersionNumber();
		Integer objectVersionNumber = event.getObjectVersionNumber();
		if (objectVersionNumber == null) {
			return;
		}
		int dteVersionNumber = objectVersionNumber.intValue();
		if (dteVersionNumber == persistentVersionNumber) {
			return;
		}
		switch (event.getTransformType()) {
		case CHANGE_PROPERTY_REF:
		case CHANGE_PROPERTY_SIMPLE_VALUE:
		case NULL_PROPERTY_REF:
			break;
		default:
			return;
		}
		// sigh
		EntityManager em = ThreadlocalTransformManager.get().getEntityManager();
		CommonPersistenceBase cpb = EntityLayerLocator.get()
				.commonPersistenceProvider()
				.getCommonPersistenceExTransaction();
		String eql = String
				.format("select dtep from %s dtep inner join fetch dtep.domainTransformRequestPersistent dtrp"
						+ " inner join fetch dtrp.clientInstance "
						+ "where  dtep.objectId= %s and dtep.objectClassRef.id = %s "
						+ " and dtep.objectVersionNumber >= %s and dtep.propertyName='%s'",
						cpb.getImplementationSimpleClassName(DomainTransformEventPersistent.class),
						obj.getId(), ClassRef.forClass(obj.getClass()).getId(),
						Math.min(dteVersionNumber, persistentVersionNumber),
						event.getPropertyName());
		MetricLogging.get().start(CHECK_TRANSFORM_CONFLICTS_QUERY);
		List<DomainTransformEventPersistent> dteps = em.createQuery(eql)
				.getResultList();
		MetricLogging.get().end(CHECK_TRANSFORM_CONFLICTS_QUERY);
		CollectionFilters.filterInPlace(dteps,
				new CollectionFilter<DomainTransformEventPersistent>() {
					@Override
					public boolean allow(DomainTransformEventPersistent o) {
						return o.getDomainTransformRequestPersistent()
								.getClientInstance().getId() != transformPersistenceToken
								.getRequest().getClientInstance().getId();
					}
				});
		if (!dteps.isEmpty()) {
			TransformConflictEvent conflictEvent = new TransformConflictEvent();
			conflictEvent.token = transformPersistenceToken;
			conflictEvent.members.add(new TransformConflictEventMember(event,
					transformPersistenceToken.getRequest()));
			for (DomainTransformEventPersistent dtep : dteps) {
				conflictEvent.members.add(new TransformConflictEventMember(
						dtep, dtep.getDomainTransformRequestPersistent()));
			}
			GlobalTopicPublisher.get().publishTopic(
					TransformConflicts.TOPIC_CONFLICT_EVENT, conflictEvent);
		}
	}

	public static class TransformConflictEvent {
		public List<TransformConflictEventMember> members = new ArrayList<TransformConflictEventMember>();

		public TransformPersistenceToken token;
	}

	public static class TransformConflictEventMember {
		public DomainTransformEvent event;

		public DomainTransformRequest request;

		public TransformConflictEventMember(DomainTransformEvent event,
				DomainTransformRequest request) {
			this.event = event;
			this.request = request;
		}
	}

	public static class TransformConflictEventLogger implements
			TopicListener<TransformConflictEvent> {
		StringBuilder builder = new StringBuilder();

		@Override
		public void topicPublished(String key, TransformConflictEvent event) {
			builder.append(">>> Transform conflict <<<\n\nThe first transform in the list conflicts"
					+ " with subsequent change(s) (same object and field)- but has been applied"
					+ " (simple conflict resolution - latest commit wins). \n\n");
			for (TransformConflictEventMember member : event.members) {
				add(member);
			}
			String message = builder.toString();
			event.token.getLogger().warn(message);
			CommonPersistenceLocal cpl = EntityLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence();
			cpl.log(message, LogMessageType.TRANSFORM_CONFLICT.toString());
		}

		private void add(TransformConflictEventMember member) {
			Object persistentRequestId = member.request instanceof DomainTransformRequestPersistent ? ((DomainTransformRequestPersistent) member.request)
					.getId() : "--";
			Date date = member.event instanceof DomainTransformEventPersistent ? ((DomainTransformEventPersistent) member.event)
					.getServerCommitDate() : member.event.getUtcDate();
			builder.append(String.format("Client instance: %s\n"
					+ "Request: %s [%s]\n" + "Date: %s\n"
					+ "Object version: %s\n\n" + "%s\n\n", member.request
					.getClientInstance().getId(),
					member.request.getRequestId(), persistentRequestId, date,
					member.event.getObjectVersionNumber(), member.event
							.toString()));
		}
	}

	public TransformPersistenceToken getTransformPersistenceToken() {
		return this.transformPersistenceToken;
	}

	public void setTransformPersistenceToken(
			TransformPersistenceToken transformPersistenceToken) {
		this.transformPersistenceToken = transformPersistenceToken;
	}
}

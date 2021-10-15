package cc.alcina.framework.entity.transform;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;

/**
 * Can be plugged into TLTM to check for versioning conflicts
 * 
 * @author nick@alcina.cc
 * 
 */
public class TransformConflicts {
	private static final String CHECK_TRANSFORM_CONFLICTS_QUERY = "check transform conflicts query";

	public static final String CONTEXT_OFFLINE_SUPPORT = TransformConflicts.class
			.getName() + ".CONTEXT_OFFLINE_SUPPORT";

	public static final String CONTEXT_IGNORE_TRANSFORM_CONFLICTS = TransformConflicts.class
			.getName() + ".CONTEXT_IGNORE_TRANSFORM_CONFLICTS";

	public static final String TOPIC_CONFLICT_EVENT = TransformConflicts.class
			.getName() + ".TOPIC_CONFLICT_EVENT";

	private boolean ignoreConflicts;

	private TransformPersistenceToken transformPersistenceToken;

	public TransformConflicts() {
		ignoreConflicts = ResourceUtilities.getBoolean(TransformConflicts.class,
				"ignoreConflicts")
				|| LooseContext.getBoolean(CONTEXT_IGNORE_TRANSFORM_CONFLICTS);
	}

	/*
	 * do all that's humanly possible to avoid a db query
	 */
	public void checkVersion(Entity obj, DomainTransformEvent event)
			throws DomainTransformException {
		if (ignoreConflicts) {
			return;
		}
		if (!(obj instanceof HasVersionNumber)) {
			return;
		}
		TransformConflictsFromOfflineSupport fromOfflineSupport = LooseContext
				.getContext().get(CONTEXT_OFFLINE_SUPPORT);
		// because offline dtrs are persisted as separate transactions, this
		// means we don't have
		// spurious version conflicts
		if (fromOfflineSupport != null) {
			if (fromOfflineSupport.wasChecked(obj)) {
				return;
			}
			fromOfflineSupport.checking(obj);
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
		String eql = String.format(
				"select dtep from %s dtep inner join fetch dtep.domainTransformRequestPersistent dtrp"
						+ " inner join fetch dtrp.clientInstance "
						+ "where  dtep.objectId= %s and dtep.objectClassRef.id = %s "
						+ " and dtep.objectVersionNumber >= %s and dtep.propertyName='%s'",
				PersistentImpl.getImplementationSimpleClassName(
						DomainTransformEventPersistent.class),
				obj.getId(), ClassRef.forClass(obj.getClass()).getId(),
				Math.min(dteVersionNumber, persistentVersionNumber),
				event.getPropertyName());
		MetricLogging.get().start(CHECK_TRANSFORM_CONFLICTS_QUERY);
		List<DomainTransformEventPersistent> dteps = em.createQuery(eql)
				.getResultList();
		MetricLogging.get().end(CHECK_TRANSFORM_CONFLICTS_QUERY);
		dteps.removeIf(o -> o.getDomainTransformRequestPersistent()
				.getClientInstance().getId() == transformPersistenceToken
						.getRequest().getClientInstance().getId());
		if (!dteps.isEmpty()) {
			TransformConflictEvent conflictEvent = new TransformConflictEvent();
			conflictEvent.token = transformPersistenceToken;
			conflictEvent.members.add(new TransformConflictEventMember(event,
					transformPersistenceToken.getRequest()));
			for (DomainTransformEventPersistent dtep : dteps) {
				conflictEvent.members.add(new TransformConflictEventMember(dtep,
						dtep.getDomainTransformRequestPersistent()));
			}
			GlobalTopicPublisher.get().publishTopic(
					TransformConflicts.TOPIC_CONFLICT_EVENT, conflictEvent);
		}
	}

	public TransformPersistenceToken getTransformPersistenceToken() {
		return this.transformPersistenceToken;
	}

	public void setTransformPersistenceToken(
			TransformPersistenceToken transformPersistenceToken) {
		this.transformPersistenceToken = transformPersistenceToken;
	}

	public static class TransformConflictEvent {
		public List<TransformConflictEventMember> members = new ArrayList<TransformConflictEventMember>();

		public TransformPersistenceToken token;
	}

	public static class TransformConflictEventLogger
			implements TopicListener<TransformConflictEvent> {
		StringBuilder builder = new StringBuilder();

		@Override
		public void topicPublished(String key, TransformConflictEvent event) {
			builder.append(
					">>> Transform conflict <<<\n\nThe transforms below (first is most recent) "
							+ " have conflicts - same object and field, but changes were made "
							+ "when the field value visible to the client was stale.  The most recent has been applied"
							+ " (simple conflict resolution - latest commit wins) - the notification is strictly informational.\n\n"
							+ " See the Alcina TransformConflicts java class if you need automatic resolution interceptors.\n\n"
							+ "");
			for (TransformConflictEventMember member : event.members) {
				add(member);
			}
			String message = builder.toString();
			event.token.getLogger().warn(message);
			CommonPersistenceLocal cpl = Registry
					.impl(CommonPersistenceProvider.class)
					.getCommonPersistence();
			cpl.log(message, LogMessageType.TRANSFORM_CONFLICT.toString());
		}

		private void add(TransformConflictEventMember member) {
			Object persistentRequestId = member.request instanceof DomainTransformRequestPersistent
					? ((DomainTransformRequestPersistent) member.request)
							.getId()
					: "--";
			Date date = member.event instanceof DomainTransformEventPersistent
					? ((DomainTransformEventPersistent) member.event)
							.getServerCommitDate()
					: member.event.getUtcDate();
			builder.append(String.format(
					"Client instance: %s\n" + "Request: %s [%s]\n"
							+ "Date: %s\n" + "Object version: %s\n\n"
							+ "%s\n\n",
					member.request.getClientInstance().getId(),
					member.request.getRequestId(), persistentRequestId, date,
					member.event.getObjectVersionNumber(),
					member.event.toString()));
		}
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

	public static class TransformConflictsFromOfflineSupport {
		private Set<EntityLocator> checked = new LinkedHashSet<EntityLocator>();

		public void checking(Entity entity) {
			checked.add(entity.toLocator());
		}

		public boolean wasChecked(Entity entity) {
			return checked.contains(entity.toLocator());
		}
	}
}

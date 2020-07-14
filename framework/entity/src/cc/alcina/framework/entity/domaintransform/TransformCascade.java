package cc.alcina.framework.entity.domaintransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.logic.EntityLayerObjects;

public class TransformCascade {
	private TransformPersistenceToken token;

	private boolean applied;

	private MultikeyMap<EntityCascade> perClass;

	private List<DomainTransformEvent> allEvents;

	public TransformCascade(
			TransformPersistenceToken transformPersistenceToken) {
		this.token = transformPersistenceToken;
		this.applied = transformPersistenceToken.isLocalToVm();
		this.allEvents = token.getRequest().allTransforms();
	}

	// this works because of transactions -
	public void ensureApplied() {
		if (!applied) {
			applied = true;
			ensureLookups();
			if (token.isLocalToVm() && PermissionsManager.get()
					.getClientInstance().equals(EntityLayerObjects.get()
							.getServerAsClientInstance())) {
				return;
			}
			try {
				TransformManager.get().setIgnorePropertyChanges(true);
				ThreadlocalTransformManager.cast()
						.setApplyingExternalTransforms(true);
				// FIXME - mvcc.3 - index
				for (DomainTransformEvent event : allEvents) {
					if (event
							.getTransformType() == TransformType.CREATE_OBJECT) {
						Entity instance = (Entity) ThreadlocalTransformManager
								.cast().newInstance(event.getObjectClass(),
										event.getObjectId(),
										event.getObjectLocalId());
						token.getTargetStore().putExternalLocal(instance);
					}
					TransformManager.get().apply(event);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				ThreadlocalTransformManager.cast()
						.setApplyingExternalTransforms(false);
				TransformManager.get().setIgnorePropertyChanges(false);
			}
		}
	}

	public List<DomainTransformEvent> getAllEvents() {
		return this.allEvents;
	}

	public <E extends Entity> boolean has(Class<E> clazz) {
		ensureLookups();
		return perClass.containsKey(clazz);
	}

	public <E extends Entity> Query query(Class<E> clazz) {
		return new Query(clazz);
	}

	private void ensureLookups() {
		if (perClass == null) {
			perClass = new UnsortedMultikeyMap<>(2);
			allEvents.forEach(event -> {
				EntityLocator locator = EntityLocator.objectLocator(event);
				perClass.ensure(() -> new EntityCascade(locator),
						event.getObjectClass(), locator).events.add(event);
			});
		}
	}

	public class EntityCascade {
		private EntityLocator locator;

		private List<DomainTransformEvent> events = new ArrayList<>();

		EntityCascade(EntityLocator locator) {
			this.locator = locator;
		}

		public <E extends Entity> E getObject() {
			return locator.getObject();
		}

		@Override
		public String toString() {
			return locator.toIdPairString();
		}
	}

	public class Query {
		private Class clazz;

		private String propertyName;

		public Query(Class clazz) {
			this.clazz = clazz;
		}

		public Stream<QueryResult> stream() {
			ensureLookups();
			if (!perClass.containsKey(clazz)) {
				return Stream.empty();
			}
			return perClass.allValues().stream().filter(this::matches)
					.map(ec -> new QueryResult(ec, getEvents(ec)));
		}

		public Query withPropertyName(String propertyName) {
			this.propertyName = propertyName;
			return this;
		}

		private List<DomainTransformEvent> getEvents(EntityCascade ec) {
			return ec.events.stream().filter(this::matches)
					.collect(Collectors.toList());
		}

		boolean matches(DomainTransformEvent event) {
			if (propertyName != null
					&& !Objects.equals(propertyName, event.getPropertyName())) {
				return false;
			}
			return true;
		}

		boolean matches(EntityCascade cascade) {
			return getEvents(cascade).size() > 0;
		}
	}

	public class QueryResult {
		public EntityCascade entityCascade;

		public List<DomainTransformEvent> events;

		public QueryResult(EntityCascade ec,
				List<DomainTransformEvent> events) {
			this.entityCascade = ec;
			this.events = events;
		}

		public void removeTransformsFromRequest() {
			Preconditions.checkState(token.getTransformResult() == null);
		}
	}
}

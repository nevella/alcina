package cc.alcina.framework.entity.domaintransform;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

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

	public void ensureApplied() {
		if (!applied) {
			applied = true;
			ensureLookups();
			try {
				TransformManager.get().setIgnorePropertyChanges(true);
				// FIXME - mvcc.3 - index
				for (DomainTransformEvent event : allEvents) {
					TransformManager.get().apply(event);
				}
				TransformManager.get().setIgnorePropertyChanges(false);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
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

	private class EntityCascade {
		private EntityLocator locator;

		private List<DomainTransformEvent> events = new ArrayList<>();

		EntityCascade(EntityLocator locator) {
			this.locator = locator;
		}

		@Override
		public String toString() {
			return locator.toIdPairString();
		}
	}
}

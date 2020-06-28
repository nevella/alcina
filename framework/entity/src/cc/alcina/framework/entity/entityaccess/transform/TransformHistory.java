package cc.alcina.framework.entity.entityaccess.transform;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEventView;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class TransformHistory {
	private static ThreadLocal<Map<EntityLocator, TransformHistory>> cache = new ThreadLocal<Map<EntityLocator, TransformHistory>>() {
		@Override
		protected Map<EntityLocator, TransformHistory> initialValue() {
			return new LinkedHashMap<>();
		}
	};

	public static void cache(Stream<? extends Entity>... streams) {
		for (Stream<? extends Entity> stream : streams) {
			List<? extends Entity> entities = stream.filter(Objects::nonNull)
					.collect(Collectors.toList());
			if (entities.size() > 0) {
				TransformHistory info = get(entities.get(0).entityClass(),
						EntityHelper.toIdSet(entities));
				for (Entity entity : entities) {
					cache.get().put(EntityLocator.instanceLocator(entity),
							info.filter(entity));
				}
			}
		}
	}

	public static TransformHistory get(Entity entity) {
		TransformHistory cached = cache.get()
				.get(EntityLocator.instanceLocator(entity));
		return cached != null ? cached
				: get(entity.getClass(), Collections.singleton(entity.getId()));
	}

	private static TransformHistory get(Class<?> clazz, Set<Long> ids) {
		TransformHistory history = new TransformHistory();
		history.transforms = TransformHistorySearcher.get().search(clazz, ids);
		return history;
	}

	public List<DomainTransformEventView> transforms;

	public DomainTransformEventView creationEvent() {
		return transforms.stream().filter(
				tr -> tr.getTransformType() == TransformType.CREATE_OBJECT)
				.findFirst().orElse(null);
	}

	public DomainTransformEventView eventForProperty(String propertyName) {
		return transforms.stream()
				.filter(tr -> propertyName.equals(tr.getPropertyName()))
				.findFirst().orElse(null);
	}

	public List<DomainTransformEventView>
			eventsForProperty(String propertyName) {
		return transforms.stream()
				.filter(tr -> propertyName.equals(tr.getPropertyName()))
				.sorted(Comparator.comparing(DomainTransformEventView::getId))
				.collect(Collectors.toList());
	}

	public <U extends IUser> U getCreationUser() {
		DomainTransformEventView transformEvent = creationEvent();
		return transformEvent == null ? null
				: (U) Domain.find(AlcinaPersistentEntityImpl.getImplementation(
						IUser.class), transformEvent.getUserId());
	}

	public <U extends IUser> U
			getUser(DomainTransformEventView transformEvent) {
		return transformEvent == null ? null
				: (U) Domain.find(AlcinaPersistentEntityImpl.getImplementation(
						IUser.class), transformEvent.getUserId());
	}

	private TransformHistory filter(Entity entity) {
		TransformHistory info = new TransformHistory();
		EntityLocator instanceLocator = EntityLocator.instanceLocator(entity);
		info.transforms = transforms.stream().filter(event -> EntityLocator
				.objectLocator(event).equals(instanceLocator))
				.collect(Collectors.toList());
		return info;
	}

	@RegistryLocation(registryPoint = TransformHistorySearcher.class, implementationType = ImplementationType.INSTANCE)
	public static abstract class TransformHistorySearcher {
		public static TransformHistory.TransformHistorySearcher get() {
			return Registry
					.impl(TransformHistory.TransformHistorySearcher.class);
		}

		protected abstract List<DomainTransformEventView> search(Class<?> clazz,
				Set<Long> ids);
	}
}
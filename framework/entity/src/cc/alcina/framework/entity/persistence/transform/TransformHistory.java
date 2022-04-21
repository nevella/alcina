package cc.alcina.framework.entity.persistence.transform;

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
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEventView;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.mvcc.Mvcc;

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
					cache.get().put(entity.toLocator(), info.filter(entity));
				}
			}
		}
	}

	public static TransformHistory get(Entity entity) {
		TransformHistory cached = cache.get().get(entity.toLocator());
		return cached != null ? cached
				: get(
						// If we get passed in an MVCC object,
						// resolve that back to its original class instead of
						// the MVCC generated
						// one
						entity.entityClass(),
						Collections.singleton(entity.getId()));
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
				: (U) Domain.find(
						(Class) PersistentImpl.getImplementation(IUser.class),
						transformEvent.getUserId());
	}

	public <U extends IUser> U
			getUser(DomainTransformEventView transformEvent) {
		return transformEvent == null ? null
				: (U) Domain.find(
						(Class) PersistentImpl.getImplementation(IUser.class),
						transformEvent.getUserId());
	}

	private TransformHistory filter(Entity entity) {
		TransformHistory info = new TransformHistory();
		info.transforms = transforms.stream()
				.filter(event -> event.toObjectLocator().matches(entity))
				.collect(Collectors.toList());
		return info;
	}

	@Registration(TransformHistorySearcher.class)
	public static abstract class TransformHistorySearcher {
		public static TransformHistory.TransformHistorySearcher get() {
			return Registry
					.impl(TransformHistory.TransformHistorySearcher.class);
		}

		protected abstract List<DomainTransformEventView> search(Class<?> clazz,
				Set<Long> ids);
	}
}

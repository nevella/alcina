package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

//FIXME - mvcc.jobs.2 - make query/queryresult typed?
public class TransformCollation {
	// class/locator/collation
	private MultikeyMap<EntityCollation> perClass;

	protected List<DomainTransformEvent> allEvents;

	private Map<EntityLocator, EntityCollation> perLocator;

	public TransformCollation(List<? extends DomainTransformEvent> allEvents) {
		this.allEvents = (List<DomainTransformEvent>) allEvents;
	}

	public TransformCollation(Set<? extends DomainTransformEvent> allEvents) {
		this(allEvents.stream().collect(Collectors.toList()));
	}

	public Stream<EntityCollation> allEntityCollations() {
		ensureLookups();
		return perLocator.values().stream();
	}

	public EntityCollation forLocator(EntityLocator locator) {
		ensureLookups();
		return perLocator.get(locator);
	}

	public List<DomainTransformEvent> getAllEvents() {
		return this.allEvents;
	}

	public Set<EntityLocator> getCreatedAndDeleted() {
		ensureLookups();
		return perLocator.values().stream()
				.filter(EntityCollation::isCreatedAndDeleted)
				.map(EntityCollation::getLocator).collect(Collectors.toSet());
	}

	public MultikeyMap<EntityCollation> getPerClass() {
		ensureLookups();
		return this.perClass;
	}

	public <E extends Entity> boolean has(Class<E>... classes) {
		ensureLookups();
		for (Class clazz : classes) {
			if (perClass.containsKey(clazz)) {
				return true;
			}
		}
		return false;
	}

	public boolean isCreatedAndDeleted(DomainTransformEvent event) {
		EntityCollation valueCollation = forLocator(event.toValueLocator());
		return forLocator(event.toObjectLocator()).isCreatedAndDeleted()
				|| (valueCollation != null
						&& valueCollation.isCreatedAndDeleted());
	}

	public <E extends Entity> Query query(Class<E> clazz) {
		return new Query(clazz);
	}

	public <E extends Entity> Query query(E entity) {
		return new Query(entity);
	}

	public void setPerClass(MultikeyMap<EntityCollation> perClass) {
		this.perClass = perClass;
	}

	protected void ensureLookups() {
		if (perLocator == null) {
			perClass = new UnsortedMultikeyMap<>(2);
			perLocator = new HashMap<>();
			allEvents.forEach(event -> {
				EntityLocator locator = event.toObjectLocator();
				EntityCollation collation = perClass.ensure(
						() -> new EntityCollation(locator), locator.clazz,
						locator);
				collation.transforms.add(event);
				perLocator.put(locator, collation);
			});
		}
	}

	protected void removeTransformsFromRequest(QueryResult queryResult) {
		throw new UnsupportedOperationException();
	}

	public class EntityCollation implements HasId {
		private EntityLocator locator;

		private List<DomainTransformEvent> transforms = new ArrayList<>();

		private Set<String> transformedPropertyNames;

		EntityCollation(EntityLocator locator) {
			this.locator = locator;
		}

		public DomainTransformEvent first() {
			return Ax.first(transforms);
		}

		@Override
		public long getId() {
			return first().getObjectId();
		}

		public EntityLocator getLocator() {
			return this.locator;
		}

		public <E extends Entity> E getObject() {
			return TransformManager.get().getObject(locator);
		}

		public Class<? extends Entity> getObjectClass() {
			return first().getObjectClass();
		}

		public Set<String> getTransformedPropertyNames() {
			if (transformedPropertyNames == null) {
				transformedPropertyNames = transforms.stream()
						.map(DomainTransformEvent::getPropertyName)
						.filter(Objects::nonNull).collect(Collectors.toSet());
			}
			return transformedPropertyNames;
		}

		public List<DomainTransformEvent> getTransforms() {
			return this.transforms;
		}

		public boolean isCreated() {
			return first().getTransformType() == TransformType.CREATE_OBJECT;
		}

		public boolean isDeleted() {
			return last().getTransformType() == TransformType.DELETE_OBJECT;
		}

		public DomainTransformEvent last() {
			return Ax.last(transforms);
		}

		@Override
		public void setId(long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return locator.toIdPairString();
		}

		boolean isCreatedAndDeleted() {
			return isCreated() && isDeleted();
		}
	}

	public class Query {
		private Class clazz;

		private String propertyName;

		private Predicate<DomainTransformEvent> predicate;

		public Query(Class clazz) {
			this.clazz = clazz;
		}

		public Query(Entity entity) {
			this(entity.entityClass());
			withFilter(e -> e.toObjectLocator().equals(entity.toLocator()));
		}

		public Stream<QueryResult> stream() {
			ensureLookups();
			if (!perClass.containsKey(clazz)) {
				return Stream.empty();
			}
			return perClass.allValues().stream().filter(this::matches)
					.map(ec -> new QueryResult(ec, getEvents(ec)));
		}

		public Query withFilter(Predicate<DomainTransformEvent> predicate) {
			if (this.predicate != null) {
				this.predicate = this.predicate.and(predicate);
			} else {
				this.predicate = predicate;
			}
			return this;
		}

		public Query withPropertyName(String propertyName) {
			this.propertyName = propertyName;
			return this;
		}

		private List<DomainTransformEvent> getEvents(EntityCollation ec) {
			return ec.transforms.stream().filter(this::matches)
					.collect(Collectors.toList());
		}

		boolean matches(DomainTransformEvent event) {
			if (event.getObjectClass() != clazz) {
				return false;
			}
			if (propertyName != null
					&& !Objects.equals(propertyName, event.getPropertyName())) {
				return false;
			}
			if (predicate != null && !predicate.test(event)) {
				return false;
			}
			return true;
		}

		boolean matches(EntityCollation collation) {
			return getEvents(collation).size() > 0;
		}
	}

	public class QueryResult {
		public EntityCollation entityCollation;

		public List<DomainTransformEvent> events;

		public QueryResult(EntityCollation ec,
				List<DomainTransformEvent> events) {
			this.entityCollation = ec;
			this.events = events;
		}

		public <E extends Entity> E getObject() {
			return entityCollation.getObject();
		}

		public boolean hasCreateTransform() {
			return events.stream().anyMatch(
					e -> e.getTransformType() == TransformType.CREATE_OBJECT);
		}

		public boolean hasDeleteTransform() {
			return events.stream().anyMatch(
					e -> e.getTransformType() == TransformType.DELETE_OBJECT);
		}

		public boolean hasNoDeleteTransform() {
			return !hasDeleteTransform();
		}

		public boolean hasPropertyName(String name) {
			return events.stream()
					.anyMatch(e -> Objects.equals(e.getPropertyName(), name));
		}

		public void removeTransformsFromRequest() {
			TransformCollation.this.removeTransformsFromRequest(this);
		}
	}
}
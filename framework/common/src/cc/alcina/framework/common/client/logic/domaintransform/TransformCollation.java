package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.UserProperty;
import cc.alcina.framework.common.client.logic.domain.UserPropertyPersistable;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

// FIXME - mvcc.jobs.2 - make query/queryresult typed?
public class TransformCollation {
	private static transient Class<? extends UserProperty> userPropertyPersistableImpl;

	// class/locator/collation
	protected MultikeyMap<EntityCollation> perClass;

	protected List<DomainTransformEvent> allEvents;

	protected Map<EntityLocator, EntityCollation> perLocator;

	public TransformCollation(List<? extends DomainTransformEvent> allEvents) {
		refresh(allEvents);
	}

	public TransformCollation(Set<? extends DomainTransformEvent> allEvents) {
		this(allEvents.stream().collect(Collectors.toList()));
	}

	public Stream<EntityCollation> allEntityCollations() {
		ensureLookups();
		return perLocator.values().stream();
	}

	public boolean conflictsWith(TransformCollation otherCollation) {
		ensureLookups();
		Set<EntityLocator> set1 = perLocator.keySet();
		otherCollation.ensureLookups();
		Set<EntityLocator> set2 = otherCollation.perLocator.keySet();
		return CommonUtils.hasIntersection(set1, set2);
	}

	protected void ensureLookups() {
		if (perLocator == null) {
			perClass = new UnsortedMultikeyMap<>(2);
			perLocator = new HashMap<>();
			allEvents.forEach(event -> {
				{
					EntityLocator locator = event.toObjectLocator();
					if (locator.provideIsZeroIdAndLocalId()) {
						// FIXME - mvcc.5 - DEVEX (probably on transform
						// creation)
					} else {
						EntityCollation collation = perClass.ensure(
								() -> new EntityCollation(locator),
								locator.clazz, locator);
						collation.transforms.add(event);
						perLocator.put(locator, collation);
					}
				}
				{
					EntityLocator locator = event.toValueLocator();
					if (locator == null
							|| locator.provideIsZeroIdAndLocalId()) {
					} else {
						EntityCollation collation = perClass.ensure(
								() -> new EntityCollation(locator),
								locator.clazz, locator);
						collation.valueTransforms.add(event);
						perLocator.put(locator, collation);
					}
				}
			});
		}
	}

	public void filterNonpersistentTransforms() {
		ensureLookups();
		Set<DomainTransformEvent> events = allEvents.stream()
				.collect(AlcinaCollectors.toLinkedHashSet());
		allEntityCollations().forEach(ec -> {
			if (ec.isCreatedAndDeleted()) {
				ec.transforms.forEach(events::remove);
			} else {
				ec.ensureByPropertyName().values().forEach(list -> {
					for (int idx = 0; idx < list.size() - 1; idx++) {
						DomainTransformEvent transform = list.get(idx);
						if (transform.getTransformType()
								.isNotCollectionTransform()) {
							events.remove(transform);
						}
					}
				});
			}
		});
		allEvents = events.stream().collect(Collectors.toList());
	}

	public EntityCollation forLocator(EntityLocator locator) {
		ensureLookups();
		return perLocator.get(locator);
	}

	public List<DomainTransformEvent> getAllEvents() {
		return this.allEvents;
	}

	public Stream<EntityCollation>
			getConflictingCollations(TransformCollation conflictingWith) {
		ensureLookups();
		conflictingWith.ensureLookups();
		Set<EntityLocator> set2 = conflictingWith.perLocator.keySet();
		Stream<EntityCollation> conflicting = allEntityCollations()
				.filter(ec -> set2.contains(ec.getLocator()));
		return conflicting;
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

	public boolean has(Class<? extends Entity>... classes) {
		ensureLookups();
		for (Class clazz : classes) {
			if (perClass.containsKey(clazz)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasPersistable(
			Class<? extends UserPropertyPersistable>... userPropertyPersistableClasses) {
		if (userPropertyPersistableImpl == null) {
			userPropertyPersistableImpl = PersistentImpl
					.getImplementation(UserProperty.class);
		}
		if (!has(userPropertyPersistableImpl)) {
			return false;
		}
		Set<Class> modifiedPersistableClasses = query(
				userPropertyPersistableImpl).stream()
						.filter(QueryResult::hasNoDeleteTransform)
						.<UserProperty> map(QueryResult::getEntity)
						.map(up -> up.ensureUserPropertySupport()
								.getPersistable())
						.filter(Objects::nonNull).map(Object::getClass)
						.collect(Collectors.toSet());
		List<UserProperty> list = (List) query(userPropertyPersistableImpl)
				.stream().map(QueryResult::getEntity)
				.collect(Collectors.toList());
		for (Class clazz : userPropertyPersistableClasses) {
			if (modifiedPersistableClasses.contains(clazz)) {
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

	public <T extends Entity> Stream<T> modified(Class<T> clazz) {
		return query(clazz).stream().<T> map(QueryResult::getEntity)
				.filter(Objects::nonNull);
	}

	public <T extends Entity> Stream<T>
			modifiedExcludingProperties(Class<T> clazz, Enum... excludes) {
		return query(clazz).stream()
				.filter(qr -> qr.hasPropertyNameExcluding(excludes))
				.<T> map(QueryResult::getEntity).filter(Objects::nonNull);
	}

	public <E extends Entity> Query query(Class<E> clazz) {
		return new Query(clazz);
	}

	public <E extends Entity> Query query(E entity) {
		return new Query(entity);
	}

	protected void refresh(List<? extends DomainTransformEvent> allEvents) {
		this.allEvents = (List<DomainTransformEvent>) allEvents.stream()
				.collect(Collectors.toList());
		perClass = null;
		perLocator = null;
	}

	public Set<DomainTransformEvent>
			removeConflictingTransforms(TransformCollation conflictingWith) {
		Set<DomainTransformEvent> result = new LinkedHashSet<>();
		Stream<EntityCollation> conflicting = getConflictingCollations(
				conflictingWith);
		conflicting.map(EntityCollation::getTransforms)
				.flatMap(Collection::stream).forEach(result::add);
		allEvents.removeIf(result::contains);
		return result;
	}

	public void removeTransformFromRequest(DomainTransformEvent event) {
		throw new UnsupportedOperationException();
	}

	protected void removeTransformsFromRequest(QueryResult queryResult) {
		queryResult.events.forEach(this::removeTransformFromRequest);
	}

	public void setPerClass(MultikeyMap<EntityCollation> perClass) {
		this.perClass = perClass;
	}

	public String toStatisticsString() {
		ensureLookups();
		CountingMap<String> statistics = new CountingMap<>();
		perClass.keySet().forEach(k -> statistics
				.add(((Class) k).getSimpleName(), perClass.items(k).size()));
		return statistics.toLinkedHashMap(true).entrySet().toString();
	}

	public String toIdsString() {
		ensureLookups();
		FormatBuilder builder = new FormatBuilder();
		perClass.keySet().forEach(k -> {
			Collection<? extends Entity> items = (Collection) perClass.items(k);
			builder.line("%s : %s", ((Class) k).getSimpleName(),
					EntityHelper.toIdString(items));
		});
		return builder.toString();
	}

	public class EntityCollation implements HasId {
		private EntityLocator locator;

		private List<DomainTransformEvent> transforms = new ArrayList<>();

		private List<DomainTransformEvent> valueTransforms = new ArrayList<>();

		Multimap<String, List<DomainTransformEvent>> transformsByPropertyName;

		private Entity entity;

		EntityCollation(EntityLocator locator) {
			this.locator = locator;
		}

		public boolean doesNotContainsNames(PropertyEnum... names) {
			Set<String> transformedPropertyNames = getTransformedPropertyNames();
			return Arrays.stream(names).map(PropertyEnum::name)
					.noneMatch(transformedPropertyNames::contains);
		}

		public boolean doesNotContainsNames(String... propertyNames) {
			Set<String> transformedPropertyNames = getTransformedPropertyNames();
			return Arrays.stream(propertyNames)
					.noneMatch(transformedPropertyNames::contains);
		}

		public Multimap<String, List<DomainTransformEvent>>
				ensureByPropertyName() {
			if (transformsByPropertyName == null) {
				transformsByPropertyName = transforms.stream()
						.filter(dte -> dte.getPropertyName() != null)
						.collect(AlcinaCollectors.toKeyMultimap(
								DomainTransformEvent::getPropertyName));
			}
			return transformsByPropertyName;
		}

		public DomainTransformEvent first() {
			return Ax.first(transforms);
		}

		public <E extends Entity> E getEntity() {
			if (entity == null) {
				entity = TransformManager.get().getObject(locator);
			}
			// handle deletion
			// return entity != null ? entity : (E)
			// transforms.get(0).getSource();
			// NO! - should be handled by deletion/tx aware code (including
			// in client)
			return (E) entity;
		}

		public Class<? extends Entity> getEntityClass() {
			return locator.getClazz();
		}

		@Override
		public long getId() {
			return locator.getId();
		}

		public EntityLocator getLocator() {
			return this.locator;
		}

		public Set<String> getTransformedPropertyNames() {
			return ensureByPropertyName().keySet();
		}

		public List<DomainTransformEvent> getTransforms() {
			return this.transforms;
		}

		public List<DomainTransformEvent> getValueTransforms() {
			return this.valueTransforms;
		}

		public boolean isCreated() {
			return transforms.size() > 0 && first()
					.getTransformType() == TransformType.CREATE_OBJECT;
		}

		public boolean isCreatedAndDeleted() {
			return isCreated() && isDeleted();
		}

		public boolean isDeleted() {
			return transforms.size() > 0
					&& last().getTransformType() == TransformType.DELETE_OBJECT;
		}

		public boolean isPropertyOnly() {
			return !isCreated() && !isDeleted();
		}

		public DomainTransformEvent last() {
			return Ax.last(transforms);
		}

		public boolean onlyContainsNames(PropertyEnum... names) {
			if (!isPropertyOnly()) {
				return false;
			}
			Set<String> transformedPropertyNames = getTransformedPropertyNames();
			return Arrays.stream(names).map(PropertyEnum::name)
					.allMatch(transformedPropertyNames::contains);
		}

		@Override
		public void setId(long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return locator.toParseableString();
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

		public Query withPropertyName(Enum propertyEnum) {
			this.propertyName = propertyEnum.name();
			return this;
		}

		public Query withPropertyName(String propertyName) {
			this.propertyName = propertyName;
			return this;
		}
	}

	public class QueryResult {
		public EntityCollation entityCollation;

		public List<DomainTransformEvent> events;

		private Set<String> propertyNames;

		public QueryResult(EntityCollation ec,
				List<DomainTransformEvent> events) {
			this.entityCollation = ec;
			this.events = events;
		}

		private Set<String> ensurePropertyNames() {
			if (propertyNames == null) {
				propertyNames = events.stream()
						.map(DomainTransformEvent::getPropertyName)
						.filter(Objects::nonNull).collect(Collectors.toSet());
			}
			return propertyNames;
		}

		public <E extends Entity> E getEntity() {
			return entityCollation.getEntity();
		}

		public List<DomainTransformEvent> getEvents() {
			return this.events;
		}

		public boolean hasCreateTransform() {
			return events.stream().anyMatch(
					e -> e.getTransformType() == TransformType.CREATE_OBJECT);
		}

		public boolean hasDeleteTransform() {
			return events.stream().anyMatch(
					e -> e.getTransformType() == TransformType.DELETE_OBJECT);
		}

		public boolean hasNoCreateTransform() {
			return !hasCreateTransform();
		}

		public boolean hasNoDeleteTransform() {
			return !hasDeleteTransform();
		}

		public boolean hasPropertyName(Enum e) {
			return hasPropertyName(e.name());
		}

		public boolean hasPropertyName(String name) {
			return ensurePropertyNames().contains(name);
		}

		public boolean hasPropertyNameExcluding(Enum... namesArray) {
			Set<String> names = Arrays.stream(namesArray).map(Enum::name)
					.collect(Collectors.toSet());
			return ensurePropertyNames().stream()
					.anyMatch(n -> !names.contains(n));
		}

		public boolean hasPropertyNames(Enum... namesArray) {
			Set<String> names = Arrays.stream(namesArray).map(Enum::name)
					.collect(Collectors.toSet());
			return ensurePropertyNames().stream()
					.anyMatch(n -> names.contains(n));
		}

		public boolean hasPropertyNames(String... namesArray) {
			Set<String> names = Arrays.stream(namesArray)
					.collect(Collectors.toSet());
			return ensurePropertyNames().stream()
					.anyMatch(n -> names.contains(n));
		}

		public boolean hasPropertyTransform() {
			return ensurePropertyNames().size() > 0;
		}

		public boolean hasTransformsOtherThan(Enum... names) {
			return events.stream()
					.anyMatch(e -> e.getPropertyName() == null
							|| Arrays.stream(names).noneMatch(name -> Objects
									.equals(e.getPropertyName(), name.name())));
		}

		public Set<String> propertyNames() {
			return ensurePropertyNames();
		}

		public void removeTransform(DomainTransformEvent event) {
			TransformCollation.this.removeTransformFromRequest(event);
		}

		public void removeTransformsFromRequest() {
			TransformCollation.this.removeTransformsFromRequest(this);
		}
	}
}
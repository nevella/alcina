package cc.alcina.framework.entity.transform.policy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.NotPersistentObjectCriteriaGroup;
import cc.alcina.framework.common.client.search.PersistentObjectCriterion;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.domain.DomainStore;

@Registration.Singleton(TransformPropagationPolicy.class)
public class TransformPropagationPolicy {
	public static final transient String CONTEXT_PROPAGATION_FILTER = TransformPropagationPolicy.class
			.getName() + ".CONTEXT_PROPAGATION_FILTER";

	public static TransformPropagationPolicy get() {
		return Registry.impl(TransformPropagationPolicy.class);
	}

	private List<NonPersistentData> nonPersistentData;

	public long
			getProjectedPersistentCount(Stream<DomainTransformEvent> events) {
		return events.filter(event -> {
			if (event.getObjectClass() == null) {
				return true;
			} else {
				return shouldPersistEventRecord(event);
			}
		}).count();
	}

	public boolean handlesEvent(DomainTransformEvent event) {
		return DomainStore.stores().storeFor(event.getObjectClass()) != null;
	}

	protected boolean isNonDomainStoreClass(Class<? extends Entity> clazz) {
		return !DomainStore.stores().storeFor(clazz).isCached(clazz);
	}

	/**
	 * Filter any transforms which by default would not have been persisted.
	 */
	public void populateCriteriaGroupFromNonPersistent(
			NotPersistentObjectCriteriaGroup criteriaGroup) {
		if (nonPersistentData == null) {
			// concurrent population harmless
			nonPersistentData = Registry.query(Entity.class).registrations()
					.filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
					.map(NonPersistentData::new).collect(Collectors.toList());
		}
		nonPersistentData.stream().map(NonPersistentData::toCriteria)
				.flatMap(Collection::stream)
				.forEach(criteriaGroup::addCriterion);
	}

	public DomainTransformPropagation
			resolvePropagation(DomainTransformEvent event) {
		// this could be improved to use TreeResolver and go via the DomainStore
		// (to allow imperative customization)
		// but that only gives a slight reduction in transform propagation
		// chatter - and the primary purpose of the DomainStore customisations
		// is to trim initial cache load as much as possible. So....
		// low-priority
		AnnotationLocation location = new AnnotationLocation(
				event.getObjectClass(),
				event.getPropertyName() == null ? null
						: Reflections.at(event.getObjectClass())
								.property(event.getPropertyName()));
		DomainTransformPropagation annotation = location
				.getAnnotation(DomainTransformPropagation.class);
		return annotation;
	}

	/*
	 * The *results* of the event (changes to the db) are always persisted - the
	 * name of this method is designed to indicate that we also persist a
	 * *record* of the event
	 */
	public boolean shouldPersistEventRecord(DomainTransformEvent event) {
		DomainTransformPropagation propagation = resolvePropagation(event);
		switch (propagation.value()) {
		case NONE:
			return false;
		case PERSISTENT:
			return true;
		case NON_PERSISTENT:
			/*
			 * Always persist non-root transforms (if not propogation::NONE)
			 */
			return propagation.persistNonRoot()
					&& !PermissionsManager.get().isRoot();
		default:
			throw new UnsupportedOperationException();
		}
	}

	public boolean shouldPropagate(DomainTransformEvent event) {
		DomainTransformPropagation propagation = resolvePropagation(event);
		if (isNonDomainStoreClass(event.getObjectClass())) {
			return false;
		}
		if (event.getValueClass() != null
				&& Entity.class.isAssignableFrom(event.getValueClass())) {
			if (isNonDomainStoreClass(event.getValueClass())) {
				return false;
			}
		}
		Predicate<DomainTransformEvent> propagationFilter = LooseContext
				.get(CONTEXT_PROPAGATION_FILTER);
		if (propagationFilter != null && !propagationFilter.test(event)) {
			return false;
		}
		return propagation.value() == PropagationType.PERSISTENT
				|| propagation.value() == PropagationType.NON_PERSISTENT;
	}

	class NonPersistentData {
		Class<? extends Entity> clazz;

		boolean classIsNonPersistent;

		List<Property> nonPersistentProperties = new ArrayList<>();

		public NonPersistentData(Class<? extends Entity> clazz) {
			this.clazz = clazz;
			{
				AnnotationLocation location = new AnnotationLocation(clazz,
						null);
				DomainTransformPropagation annotation = location
						.getAnnotation(DomainTransformPropagation.class);
				classIsNonPersistent = annotation != null
						&& annotation.value() != PropagationType.PERSISTENT;
			}
			Reflections.at(clazz).properties().stream()
					.filter(Property::isReadWrite).forEach(property -> {
						DomainTransformPropagation annotation = property
								.annotation(DomainTransformPropagation.class);
						if (annotation != null && annotation
								.value() != PropagationType.PERSISTENT) {
							nonPersistentProperties.add(property);
						}
					});
			;
		}

		List<PersistentObjectCriterion> toCriteria() {
			List<PersistentObjectCriterion> result = new ArrayList<>();
			if (classIsNonPersistent) {
				result.add(new PersistentObjectCriterion()
						.withValue(ClassRef.forClass(clazz)));
			}
			nonPersistentProperties.forEach(property -> {
				result.add(new PersistentObjectCriterion()
						.withValue(ClassRef.forClass(clazz))
						.withPropertyName(property.getName()));
			});
			return result;
		}
	}
}

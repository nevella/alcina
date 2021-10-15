package cc.alcina.framework.entity.transform.policy;

import java.util.function.Predicate;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.domain.DomainStore;

@RegistryLocation(registryPoint = TransformPropagationPolicy.class, implementationType = ImplementationType.INSTANCE)
public class TransformPropagationPolicy {
	public static final transient String CONTEXT_PROPAGATION_FILTER = TransformPropagationPolicy.class
			.getName() + ".CONTEXT_PROPAGATION_FILTER";

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

	private DomainTransformPropagation
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
						: Reflections.classLookup().getPropertyReflector(
								event.getObjectClass(),
								event.getPropertyName()));
		DomainTransformPropagation annotation = location
				.getAnnotation(DomainTransformPropagation.class);
		return annotation;
	}

	protected boolean isNonDomainStoreClass(Class<? extends Entity> clazz) {
		return !DomainStore.stores().storeFor(clazz).isCached(clazz);
	}
}

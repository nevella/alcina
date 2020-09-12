package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = TransformPropagationPolicy.class, implementationType = ImplementationType.INSTANCE)
public class TransformPropagationPolicy {
	public boolean shouldPersist(DomainTransformEvent event) {
		DomainTransformPropagation propagation = resolvePropagation(event);
		return propagation.value() == PropagationType.PERSISTENT;
	}

	public boolean shouldPropagate(DomainTransformEvent event) {
		DomainTransformPropagation propagation = resolvePropagation(event);
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
				Reflections.classLookup().getPropertyReflector(
						event.getObjectClass(), event.getPropertyName()));
		return location.getAnnotation(DomainTransformPropagation.class);
	}
}

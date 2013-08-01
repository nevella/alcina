package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.entity.util.GraphProjection.CollectionProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.PermissibleFieldFilter;

@RegistryLocations({
		@RegistryLocation(registryPoint = PermissibleFieldFilter.class, implementationType = ImplementationType.FACTORY),
		@RegistryLocation(registryPoint = CollectionProjectionFilter.class, implementationType = ImplementationType.FACTORY) })
public class StandardProjectionFilterFactory implements
		RegistryFactory<GraphProjectionFilter> {
	@Override
	public GraphProjectionFilter create(
			Class<? extends GraphProjectionFilter> registryPoint,
			Class targetObjectClass) {
		if (registryPoint == PermissibleFieldFilter.class) {
			return new PermissibleFieldFilter();
		} else {
			return new CollectionProjectionFilter();
		}
	}
}

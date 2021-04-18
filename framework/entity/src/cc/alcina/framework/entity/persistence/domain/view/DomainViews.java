package cc.alcina.framework.entity.persistence.domain.view;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@RegistryLocation(registryPoint = DomainViews.class, implementationType = ImplementationType.SINGLETON)
public class DomainViews {
	public static DomainViews get() {
		return Registry.impl(DomainViews.class);
	}
}

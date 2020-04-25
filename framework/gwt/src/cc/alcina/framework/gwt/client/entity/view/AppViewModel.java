package cc.alcina.framework.gwt.client.entity.view;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.entity.VersionableDomainBase;

@RegistryLocation(registryPoint = AppViewModel.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class AppViewModel {
	public synchronized static AppViewModel get() {
		return Registry.impl(AppViewModel.class);
	}

	private Map<Class, DomainStoreDataProvider> providers = new LinkedHashMap<>();

	public AppViewModel() {
	}

	public <T extends VersionableDomainBase> DomainStoreDataProvider<T>
			getDataProvider(Class<T> clazz) {
		if (!providers.containsKey(clazz)) {
			providers.put(clazz, createProvider(clazz));
		}
		return providers.get(clazz);
	}

	public void resetProviderFor(Class<? extends VersionableDomainBase> clazz) {
		providers.get(clazz).invalidate();
	}

	protected <T extends VersionableDomainBase> DomainStoreDataProvider<T>
			createProvider(Class<T> clazz) {
		return new DomainStoreDataProvider<>(clazz);
	}
}

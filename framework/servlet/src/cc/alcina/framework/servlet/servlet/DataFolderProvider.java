package cc.alcina.framework.servlet.servlet;

import java.io.File;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@RegistryLocation(registryPoint = DataFolderProvider.class, implementationType = ImplementationType.SINGLETON)
public abstract class DataFolderProvider {
	public static DataFolderProvider get() {
		return Registry.impl(DataFolderProvider.class);
	}

	public abstract File getDataFolder();
}

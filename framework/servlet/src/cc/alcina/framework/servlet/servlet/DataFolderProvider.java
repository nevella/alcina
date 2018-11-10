package cc.alcina.framework.servlet.servlet;

import java.io.File;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;

//@RegistryLocation(registryPoint = DataFolderProvider.class, implementationType = ImplementationType.SINGLETON)
//No registry annotation, register manually in AppLifecycleServlet.initBootstrapRegistry
public abstract class DataFolderProvider {
	public static DataFolderProvider get() {
		return Registry.impl(DataFolderProvider.class);
	}

	public File getChildFile(String childFileName) {
		return SEUtilities.getChildFile(getDataFolder(), childFileName);
	}

	public abstract File getDataFolder();
}

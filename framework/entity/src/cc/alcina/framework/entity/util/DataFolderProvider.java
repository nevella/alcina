package cc.alcina.framework.entity.util;

import java.io.File;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;

//
// No registry annotation, register manually in
// AppLifecycleServlet.initBootstrapRegistry
public abstract class DataFolderProvider {
	public static DataFolderProvider get() {
		return Registry.impl(DataFolderProvider.class);
	}

	public File getChildFile(String childFileName) {
		return SEUtilities.getChildFile(getDataFolder(), childFileName);
	}

	public File getClassDataFile(Object instance) {
		// call with an instance, not its class
		Preconditions.checkState(instance.getClass() != Class.class);
		return getSubFolderFile("class-data-file",
				Ax.format("%s.dat", instance.getClass().getCanonicalName()));
	}

	public abstract File getDataFolder();

	public File getSubFolder(String folderName) {
		File childFile = getChildFile(folderName);
		childFile.mkdirs();
		return childFile;
	}

	public File getSubFolderFile(String subFolderName, String fileName) {
		File subFolder = getSubFolder(subFolderName);
		return SEUtilities.getChildFile(subFolder, fileName);
	}
}

package cc.alcina.framework.entity.util;

import java.io.File;

import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.gwittir.widget.FileData;

public class FileUtils {
	/**
	 * For testing of client/html file rpc calls
	 */
	public static FileData fromFile(File file) {
		FileData fileData = new FileData();
		fileData.setBytes(Io.read().file(file).asBytes());
		fileData.setFileName(file.getName());
		return fileData;
	}

	public static FileData fromResource(Class clazz,
			String relativeResourcePath) {
		FileData fileData = new FileData();
		fileData.setBytes(Io.read().relativeTo(clazz)
				.resource(relativeResourcePath).asBytes());
		fileData.setFileName(
				relativeResourcePath.replaceFirst("(.+)/(.+)", "$2"));
		return fileData;
	}
}

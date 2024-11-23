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

	public static File child(File folder, String childFileName) {
		if (childFileName.startsWith("/")) {
			childFileName = childFileName.substring(1);
		}
		return new File(
				String.format("%s/%s", folder.getPath(), childFileName));
	}

	public static String fileUrlToDataUrl(String url) {
		if (!url.startsWith("file:/")) {
			return url;
		}
		return Io.read().url(url).asDataUrl();
	}

	public static File sibling(File file, String siblingFileName) {
		return child(file.getParentFile(), siblingFileName);
	}
}

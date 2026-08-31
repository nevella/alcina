package cc.alcina.framework.entity.util;

import java.io.File;

public class FileUtils {
	public static File child(File folder, String childFileName) {
		if (childFileName.startsWith("/")) {
			childFileName = childFileName.substring(1);
		}
		return new File(
				String.format("%s/%s", folder.getPath(), childFileName));
	}

	public static File sibling(File file, String siblingFileName) {
		return child(file.getParentFile(), siblingFileName);
	}
}

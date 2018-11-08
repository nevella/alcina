package cc.alcina.framework.entity.util;

import java.io.File;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.ResourceUtilities;

public class FsStringCache {
	private File root;

	private ThrowingFunction<String, String> pathToContent;

	public FsStringCache(File root,
			ThrowingFunction<String, String> pathToContent) {
		this.root = root;
		this.pathToContent = pathToContent;
	}

	public String get(String path, String content) {
		File cacheFile = getCacheFile(path);
		if (!cacheFile.exists()) {
			try {
				String value = pathToContent.apply(content);
				ResourceUtilities.write(value, cacheFile);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		} else {
		}
		return ResourceUtilities.read(cacheFile);
	}

	private File getCacheFile(String path) {
		return new File(Ax.format("%s/%s.dat", root.getPath(), path));
	}
}
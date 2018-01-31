package cc.alcina.framework.classmeta;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ClasspathScanner;

public class ClasspathScannerResolver {
	Map<URL, ClassMetadataCache> caches = new LinkedHashMap<>();

	public synchronized ClassMetadataCache handle(ClassMetaRequest typedRequest) {
		// await update queue
		ClassMetadataCache result = new ClassMetadataCache();
		for (URL url : typedRequest.classPaths) {
			if (!caches.containsKey(url)) {
				ensureCache(url);
			}
			result.merge(caches.get(url));
		}
		return result;
	}

	private void ensureCache(URL url) {
		try {
			ClasspathScanner scanner = new ClasspathScanner("*",true,true);
			scanner.invokeHandler(url);
			caches.put(url, scanner.getClasses());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

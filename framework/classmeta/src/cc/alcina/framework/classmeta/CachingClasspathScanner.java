package cc.alcina.framework.classmeta;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;

public class CachingClasspathScanner extends ServletClasspathScanner {
	public CachingClasspathScanner(String pkg, boolean subpackages,
			boolean ignoreJars, Object logger, String resourceName,
			List<String> ignorePathSegments) {
		this(pkg, subpackages, ignoreJars, logger, resourceName,
				ignorePathSegments, new ArrayList<>());
	}

	public CachingClasspathScanner(String pkg, boolean subpackages,
			boolean ignoreJars, Object logger, String resourceName,
			List<String> ignorePathSegments,
			List<String> ignorePackageSegments) {
		super(pkg, subpackages, ignoreJars, logger, resourceName,
				ignorePathSegments, ignorePackageSegments);
		usingRemoteScanner = ResourceUtilities.is(CachingClasspathScanner.class,
				"usingRemoteScanner");
	}

	List<URL> urls = new ArrayList<>();

	private boolean usingRemoteScanner;

	@Override
	public ClassMetadataCache getClasses() throws Exception {
		if (usingRemoteScanner) {
			super.getClasses();
			ClassMetaRequest metaRequest = new ClassMetaRequest();
			metaRequest.type = ClassMetaRequestType.Classes;
			metaRequest.classPaths = urls;
			ClassMetaResponse response = new ClassMetaInvoker()
					.invoke(metaRequest);
			return response.cache;
		} else {
			return super.getClasses();
		}
	}

	@Override
	public void invokeHandler(URL url) {
		if (usingRemoteScanner) {
			urls.add(url);
		} else {
			super.invokeHandler(url);
		}
	}
}

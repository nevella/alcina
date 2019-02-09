package cc.alcina.framework.classmeta;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;

public class CachingClasspathScanner extends ServletClasspathScanner {
    private static ClasspathUrlTranslator classpathUrlTranslator = url -> url;

    public static void installUrlTranslator(
            ClasspathUrlTranslator classpathUrlTranslator) {
        CachingClasspathScanner.classpathUrlTranslator = classpathUrlTranslator;
    }

    List<URL> urls = new ArrayList<>();

    private boolean usingRemoteScanner;

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
        try {
            usingRemoteScanner = ResourceUtilities
                    .is(CachingClasspathScanner.class, "usingRemoteScanner");
        } catch (Exception e) {
            Ax.out("No bundle properties configured for CachingClasspathScanner");
        }
    }

    @Override
    public ClassMetadataCache getClasses() throws Exception {
        if (usingRemoteScanner) {
            try {
                super.getClasses();
                ClassMetaRequest metaRequest = new ClassMetaRequest();
                metaRequest.type = ClassMetaRequestType.Classes;
                metaRequest.classPaths = urls.stream()
                        .map(classpathUrlTranslator::translateClasspathUrl)
                        .collect(Collectors.toList());
                ClassMetaResponse response = new ClassMetaInvoker()
                        .invoke(metaRequest);
                if (response == null) {
                    usingRemoteScanner = false;
                    return super.getClasses();
                }
                return response.cache;
            } catch (Exception e) {
                e.printStackTrace();
                usingRemoteScanner = false;
                return super.getClasses();
            }
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

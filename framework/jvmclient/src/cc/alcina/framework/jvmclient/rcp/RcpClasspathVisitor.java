package cc.alcina.framework.jvmclient.rcp;

import java.lang.reflect.Method;
import java.net.URL;

import cc.alcina.framework.entity.util.ClasspathScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ClasspathVisitor;

/**
 * Has an eclipse dependency - but we use reflection to avoid adding for just
 * one method
 *
 * @author nick@alcina.cc
 *
 */

public class RcpClasspathVisitor extends ClasspathVisitor {
	protected static final Object PROTOCOL_BUNDLE_RESOURCE = "bundleresource";

	protected static final Object PROTOCOL_VFSZIP = "vfszip";

	protected static final Object PROTOCOL_VFSFILE = "vfsfile";

	public RcpClasspathVisitor(ClasspathScanner scanner) {
		super(scanner);
	}

	@Override
	public void enumerateClasses(URL url) throws Exception {
		assert false;
	}

	@Override
	public boolean handles(URL url) {
		if (url.getProtocol().equals(PROTOCOL_BUNDLE_RESOURCE)) {
			assert false;
		}
		return false;
	}

	@Override
	public URL resolve(URL url) throws Exception {
		if (url.getProtocol().equals(PROTOCOL_BUNDLE_RESOURCE)) {
			Class fileLocatorClass = Class
					.forName("org.eclipse.core.runtime.FileLocator");
			Method resolveMethod = fileLocatorClass.getMethod("resolve",
					URL.class);
			Object resolvedUrl = resolveMethod.invoke(null, url);
			System.out.println("resolved classpath url - " + resolvedUrl);
			return (URL) resolvedUrl;
		}
		return super.resolve(url);
	}
}
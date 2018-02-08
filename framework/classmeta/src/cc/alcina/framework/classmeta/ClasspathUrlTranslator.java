package cc.alcina.framework.classmeta;

import java.net.URL;

@FunctionalInterface
public interface ClasspathUrlTranslator {
	URL translateClasspathUrl(URL in);
}

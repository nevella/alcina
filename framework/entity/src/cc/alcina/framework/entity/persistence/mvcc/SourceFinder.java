package cc.alcina.framework.entity.persistence.mvcc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;

public interface SourceFinder {
	static List<SourceFinder> sourceFinders = Collections
			.synchronizedList(new ArrayList<>());

	static String locateSource(Class clazz) throws Exception {
		clazz = clazz.getNestHost();
		for (SourceFinder finder : sourceFinders) {
			String source = finder.findSource(clazz);
			if (source != null) {
				return source;
			}
		}
		Ax.err("Warn - cannot find source:\n\t%s", clazz.getName());
		return null;
	}

	String findSource(Class clazz);
}
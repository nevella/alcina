package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface SourceFinder {
	static List<SourceFinder> sourceFinders = Collections
			.synchronizedList(new ArrayList<>());

	String findSource(Class clazz);
}
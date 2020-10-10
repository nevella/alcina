package cc.alcina.framework.entity.persistence.mvcc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface SourceFinder {
	static List<SourceFinder> sourceFinders = Collections
			.synchronizedList(new ArrayList<>());

	String findSource(Class clazz);
}